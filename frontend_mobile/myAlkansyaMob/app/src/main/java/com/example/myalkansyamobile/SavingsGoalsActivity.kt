package com.example.myalkansyamobile

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myalkansyamobile.adapters.PaymentHistoryAdapter
import com.example.myalkansyamobile.adapters.SavingsGoalAdapter
import com.example.myalkansyamobile.api.ExpenseRequest
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.api.SavingsGoalResponse
import com.example.myalkansyamobile.databinding.ActivitySavingsGoalsBinding
import com.example.myalkansyamobile.databinding.DialogAddPaymentBinding
import com.example.myalkansyamobile.databinding.DialogPaymentHistoryBinding
import com.example.myalkansyamobile.model.Expense
import com.example.myalkansyamobile.model.SavingsGoal
import com.example.myalkansyamobile.utils.CurrencyUtils
import com.example.myalkansyamobile.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*

class SavingsGoalsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySavingsGoalsBinding
    private lateinit var adapter: SavingsGoalAdapter
    private lateinit var sessionManager: SessionManager
    private var userDefaultCurrency = "PHP"
    
    private val originalGoalsList = mutableListOf<SavingsGoalResponse>()
    private val filteredGoalsList = mutableListOf<SavingsGoal>()
    private val displayGoalsList = mutableListOf<SavingsGoal>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val expensesForGoals = mutableMapOf<Int, List<Expense>>()
    
    // Pagination properties
    private var currentPage = 1
    private var itemsPerPage = 10
    private var totalPages = 1
    
    // Filter properties
    private var selectedStatus: String? = null
    private var selectedSortBy: String? = null

    companion object {
        // Status options for filter
        private val STATUS_OPTIONS = listOf("All", "In Progress", "Completed", "Overdue")
        
        // Sort options
        private val SORT_OPTIONS = listOf("Name", "Target Date", "Progress", "Amount")
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavingsGoalsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        sessionManager = SessionManager(this)
        userDefaultCurrency = sessionManager.getCurrency() ?: "PHP"
        
        setupRecyclerView()
        setupFilterSpinners()
        setupClickListeners()
        updatePaginationControls()
        fetchSavingsGoals()
        fetchExpensesForSavingsGoals()
    }
    
    private fun setupRecyclerView() {
        adapter = SavingsGoalAdapter(
            displayGoalsList,
            { goal, action -> 
                when (action) {
                    SavingsGoalAdapter.ACTION_EDIT -> {
                        val intent = Intent(this, EditSavingsGoalActivity::class.java)
                        intent.putExtra(EditSavingsGoalActivity.EXTRA_GOAL_ID, goal.id)
                        startActivity(intent)
                    }
                    SavingsGoalAdapter.ACTION_ADD_PAYMENT -> {
                        showAddPaymentDialog(goal)
                    }
                    SavingsGoalAdapter.ACTION_VIEW_HISTORY -> {
                        showPaymentHistoryDialog(goal)
                    }
                }
            }
        )
        binding.recyclerSavingsGoals.layoutManager = LinearLayoutManager(this)
        binding.recyclerSavingsGoals.adapter = adapter
    }

    private fun setupFilterSpinners() {
        val statusAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, STATUS_OPTIONS)
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStatus.adapter = statusAdapter
        
        val sortAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, SORT_OPTIONS)
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSortBy.adapter = sortAdapter
        
        binding.spinnerStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedStatus = if (position == 0) null else STATUS_OPTIONS[position]
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedStatus = null
            }
        }
        
        binding.spinnerSortBy.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedSortBy = SORT_OPTIONS[position]
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedSortBy = null
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        binding.btnAddSavingsGoal.setOnClickListener {
            startActivity(Intent(this, AddSavingsGoalActivity::class.java))
        }

        binding.btnApplyFilter.setOnClickListener {
            applyFilters()
        }

        binding.btnResetFilters.setOnClickListener {
            resetFilters()
        }

        binding.btnPrevPage.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                updatePageDisplay()
            }
        }

        binding.btnNextPage.setOnClickListener {
            if (currentPage < totalPages) {
                currentPage++
                updatePageDisplay()
            }
        }
    }

    private fun applyFilters() {
        currentPage = 1
        filterAndDisplayGoals()
        
        val hasFilters = selectedStatus != null && selectedStatus != "All" || selectedSortBy != null
        
        if (hasFilters) {
            var filterText = "Active filters: "
            if (selectedStatus != null && selectedStatus != "All") {
                filterText += "Status: $selectedStatus"
            }
            
            if (selectedSortBy != null) {
                if (selectedStatus != null && selectedStatus != "All") {
                    filterText += ", "
                }
                filterText += "Sort: $selectedSortBy"
            }
            
            binding.activeFiltersText.text = filterText
            binding.activeFiltersText.visibility = View.VISIBLE
        } else {
            binding.activeFiltersText.visibility = View.GONE
        }
    }

    private fun resetFilters() {
        binding.spinnerStatus.setSelection(0)
        binding.spinnerSortBy.setSelection(0)
        
        selectedStatus = null
        selectedSortBy = null
        
        binding.activeFiltersText.visibility = View.GONE
        
        currentPage = 1
        
        filterAndDisplayGoals()
    }

    private fun filterAndDisplayGoals() {
        filteredGoalsList.clear()

        // Always start with the complete list of savings goals from the original data
        val completeGoalsList = mutableListOf<SavingsGoal>()

        // Process all API goals into our app model
        for (apiGoal in originalGoalsList) {
            // Create SavingsGoal object with the data from API
            val targetDate = try {
                // Return the Date object directly instead of its milliseconds
                dateFormat.parse(apiGoal.targetDate) ?: Date()
            } catch (e: Exception) {
                Date() // Create a new Date for current time
            }

            val savingsGoal = SavingsGoal(
                id = apiGoal.id,
                goal = apiGoal.goal,
                targetAmount = apiGoal.targetAmount,
                currentAmount = apiGoal.currentAmount,
                currency = apiGoal.currency,
                targetDate = targetDate // Now correctly passing a Date object
            )

            completeGoalsList.add(savingsGoal)
        }

        // Now add all goals to the filtered list
        filteredGoalsList.addAll(completeGoalsList)

        // Apply status filter if selected
        if (selectedStatus != null && selectedStatus != "All") {
            val statusEnum = when (selectedStatus) {
                "In Progress" -> SavingsGoal.GoalStatus.IN_PROGRESS
                "Completed" -> SavingsGoal.GoalStatus.COMPLETED
                "Overdue" -> SavingsGoal.GoalStatus.OVERDUE
                else -> null
            }

            if (statusEnum != null) {
                filteredGoalsList.removeAll { it.getComputedStatus() != statusEnum }
            }
        }

        // Apply sorting
        when (selectedSortBy) {
            "Name" -> filteredGoalsList.sortBy { it.goal }
            "Target Date" -> filteredGoalsList.sortBy { it.targetDate }
            "Progress" -> filteredGoalsList.sortByDescending { it.getProgressPercentage() }
            "Amount" -> filteredGoalsList.sortByDescending { it.targetAmount }
        }

        // Update pagination info
        totalPages = Math.ceil(filteredGoalsList.size.toDouble() / itemsPerPage).toInt()
        if (totalPages == 0) totalPages = 1

        // Make sure current page is valid
        if (currentPage > totalPages) {
            currentPage = totalPages
        }

        updatePaginationControls()
        updatePageDisplay()
    }
    private fun updatePageDisplay() {
        displayGoalsList.clear()
        
        val startIndex = (currentPage - 1) * itemsPerPage
        val endIndex = Math.min(startIndex + itemsPerPage, filteredGoalsList.size)
        
        if (startIndex < filteredGoalsList.size) {
            for (i in startIndex until endIndex) {
                displayGoalsList.add(filteredGoalsList[i])
            }
        }
        
        adapter.notifyDataSetChanged()
        updateEmptyState()
        binding.tvPagination.text = "$currentPage out of $totalPages"
    }
    
    private fun updatePaginationControls() {
        binding.tvPagination.text = "$currentPage out of $totalPages"
        
        binding.btnPrevPage.isEnabled = currentPage > 1
        binding.btnNextPage.isEnabled = currentPage < totalPages
    }
    
    override fun onResume() {
        super.onResume()
        
        val currentCurrency = sessionManager.getCurrency() ?: "PHP"
        if (currentCurrency != userDefaultCurrency) {
            userDefaultCurrency = currentCurrency
            fetchSavingsGoals()
        } else {
            fetchSavingsGoals()
        }
        
        fetchExpensesForSavingsGoals()
    }
    
    private fun showAddPaymentDialog(goal: SavingsGoal) {
        val dialogBinding = DialogAddPaymentBinding.inflate(layoutInflater)
        val dialog = Dialog(this)
        dialog.setContentView(dialogBinding.root)
        
        dialogBinding.tvGoalName.text = "Goal: ${goal.goal}"
        dialogBinding.tvCurrencyInfo.text = "Amount will be added in ${goal.currency}"
        
        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        dialogBinding.btnConfirm.setOnClickListener {
            val amountText = dialogBinding.etPaymentAmount.text.toString()
            if (amountText.isBlank()) {
                dialogBinding.etPaymentAmount.error = "Please enter an amount"
                return@setOnClickListener
            }
            
            val amount = amountText.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                dialogBinding.etPaymentAmount.error = "Please enter a valid amount"
                return@setOnClickListener
            }
            
            addPaymentToGoal(goal, amount)
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun addPaymentToGoal(goal: SavingsGoal, amount: Double) {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    Toast.makeText(this@SavingsGoalsActivity, "Authentication required", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                val bearerToken = "Bearer $token"
                
                val currentDate = LocalDate.now().toString()
                
                // Create a proper ExpenseRequest object instead of using a Map
                val expenseRequest = ExpenseRequest(
                    subject = goal.goal,
                    category = "Savings Goal", 
                    amount = amount,
                    currency = goal.currency,
                    date = currentDate,
                    savingsGoalId = goal.id
                )
                
                // Pass the ExpenseRequest object to the API
                val expenseResponse = RetrofitClient.expenseApiService.createExpense(
                    expenseRequest,
                    bearerToken
                )
                
                val updatedCurrentAmount = goal.currentAmount + amount
                
                // Create a proper SavingsGoalRequest object instead of using a Map
                val savingsGoalRequest = com.example.myalkansyamobile.api.SavingsGoalRequest(
                    goal = goal.goal,
                    targetAmount = goal.targetAmount,
                    currentAmount = updatedCurrentAmount,
                    targetDate = dateFormat.format(goal.targetDate),
                    currency = goal.currency,
                    originalTargetAmount = goal.originalTargetAmount,
                    originalCurrentAmount = goal.originalCurrentAmount,
                    originalCurrency = goal.originalCurrency
                )
                
                val updateResponse = RetrofitClient.savingsGoalApiService.updateSavingsGoal(
                    goal.id,
                    savingsGoalRequest,
                    bearerToken
                )
                
                Toast.makeText(this@SavingsGoalsActivity, "Payment added successfully!", Toast.LENGTH_SHORT).show()
                fetchSavingsGoals()
                fetchExpensesForSavingsGoals()
                
            } catch (e: Exception) {
                Log.e("SavingsGoalsActivity", "Error adding payment: ${e.message}", e)
                Toast.makeText(this@SavingsGoalsActivity, "Error adding payment: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showPaymentHistoryDialog(goal: SavingsGoal) {
        val dialogBinding = DialogPaymentHistoryBinding.inflate(layoutInflater)
        val dialog = Dialog(this, R.style.DialogTheme)
        dialog.setContentView(dialogBinding.root)
        
        // Set dialog width to match parent with margins
        val window = dialog.window
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        // Set goal name in the header
        dialogBinding.tvGoalName.text = "Goal: ${goal.goal}"
        
        // Default state - show loading
        dialogBinding.progressBar.visibility = View.VISIBLE
        dialogBinding.rvPaymentHistory.visibility = View.GONE
        dialogBinding.tvEmptyHistory.visibility = View.GONE
        dialogBinding.tvHistoryTitle.visibility = View.GONE
        dialogBinding.historyHeaderLayout.visibility = View.GONE
        
        // Get existing payments for this goal (don't wait for fetch to complete)
        val existingPayments = expensesForGoals[goal.id] ?: emptyList()
        
        Log.d("PaymentHistory", "Initial payment count for goal ${goal.id}: ${existingPayments.size}")
        
        // Setup RecyclerView
        val layoutManager = LinearLayoutManager(this)
        dialogBinding.rvPaymentHistory.layoutManager = layoutManager
        
        // Create adapter with existing payments
        val historyAdapter = PaymentHistoryAdapter(existingPayments)
        dialogBinding.rvPaymentHistory.adapter = historyAdapter
        
        // Show UI if we already have payments
        if (existingPayments.isNotEmpty()) {
            dialogBinding.progressBar.visibility = View.GONE
            dialogBinding.tvHistoryTitle.visibility = View.VISIBLE
            dialogBinding.historyHeaderLayout.visibility = View.VISIBLE
            dialogBinding.rvPaymentHistory.visibility = View.VISIBLE
        }
        
        // Show the dialog immediately
        dialog.show()
        
        // Fetch fresh data in background
        lifecycleScope.launch {
            try {
                // Fetch latest expenses to make sure we have the most recent data
                fetchExpensesForSavingsGoals()
                
                // Get payments for this specific goal
                val goalExpenses = expensesForGoals[goal.id] ?: emptyList()
                
                // Debug logs
                Log.d("PaymentHistory", "Found ${goalExpenses.size} expenses for goal ID: ${goal.id}")
                if (goalExpenses.isNotEmpty()) {
                    Log.d("PaymentHistory", "First payment: ${goalExpenses[0].date} - ${goalExpenses[0].amount}")
                }
                
                // Hide progress indicator
                dialogBinding.progressBar.visibility = View.GONE
                
                if (goalExpenses.isEmpty()) {
                    // No payments - show empty state
                    dialogBinding.tvEmptyHistory.visibility = View.VISIBLE
                    dialogBinding.tvHistoryTitle.visibility = View.GONE
                    dialogBinding.historyHeaderLayout.visibility = View.GONE
                    dialogBinding.rvPaymentHistory.visibility = View.GONE
                } else {
                    // Show transaction history
                    dialogBinding.tvEmptyHistory.visibility = View.GONE
                    dialogBinding.tvHistoryTitle.visibility = View.VISIBLE
                    dialogBinding.historyHeaderLayout.visibility = View.VISIBLE
                    dialogBinding.rvPaymentHistory.visibility = View.VISIBLE
                    
                    // Update adapter with fresh data
                    historyAdapter.updatePayments(goalExpenses)
                }
                
            } catch (e: Exception) {
                Log.e("SavingsGoalsActivity", "Error fetching payment history: ${e.message}", e)
                dialogBinding.progressBar.visibility = View.GONE
                dialogBinding.tvEmptyHistory.visibility = View.VISIBLE
                dialogBinding.tvEmptyHistory.text = "Error loading payment history"
            }
        }
        
        dialogBinding.btnClose.setOnClickListener {
            dialog.dismiss()
        }
    }
    
    private fun fetchExpensesForSavingsGoals() {
        val token = sessionManager.fetchAuthToken()
        if (token.isNullOrEmpty()) {
            return
        }
        
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.expenseApiService.getExpenses("Bearer $token")
                
                // Clear the existing map
                expensesForGoals.clear()
                
                // Process all expenses
                response.forEach { expenseResponse ->
                    if (expenseResponse.savingsGoalId != null && expenseResponse.savingsGoalId > 0) {
                        // Use the actual savingsGoalId field if available
                        val goalId = expenseResponse.savingsGoalId
                        
                        val expense = Expense(
                            id = expenseResponse.id,
                            subject = expenseResponse.subject,
                            category = expenseResponse.category,
                            date = LocalDate.parse(expenseResponse.date),
                            amount = expenseResponse.amount,
                            currency = expenseResponse.currency ?: userDefaultCurrency,
                            savingsGoalId = goalId
                        )
                        
                        val existingList = expensesForGoals[goalId] ?: emptyList()
                        expensesForGoals[goalId] = existingList + expense
                    }
                    else if (expenseResponse.category == "Savings Goal") {
                        // For backward compatibility with older data that might not have savingsGoalId
                        // Find matching goal by name
                        val matchingGoal = originalGoalsList.find { it.goal == expenseResponse.subject }
                        
                        if (matchingGoal != null) {
                            val expense = Expense(
                                id = expenseResponse.id,
                                subject = expenseResponse.subject,
                                category = expenseResponse.category,
                                date = LocalDate.parse(expenseResponse.date),
                                amount = expenseResponse.amount,
                                currency = expenseResponse.currency ?: userDefaultCurrency,
                                savingsGoalId = matchingGoal.id
                            )
                            
                            val existingList = expensesForGoals[matchingGoal.id] ?: emptyList()
                            expensesForGoals[matchingGoal.id] = existingList + expense
                        }
                    }
                }
                
                // Debug log
                Log.d("SavingsGoalsActivity", "Fetched expenses for ${expensesForGoals.size} goals")
                
                if (::adapter.isInitialized) {
                    adapter.updateExpensesForGoals(expensesForGoals)
                }
                
            } catch (e: Exception) {
                Log.e("SavingsGoalsActivity", "Error fetching expenses: ${e.message}")
            }
        }
    }
    
    private fun fetchSavingsGoals() {
        binding.progressBar.visibility = View.VISIBLE
        
        val token = sessionManager.fetchAuthToken()
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }
        
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.savingsGoalApiService.getAllSavingsGoals("Bearer $token")
                originalGoalsList.clear()
                originalGoalsList.addAll(response)
                
                processGoalsForDisplay(token)
                
                filterAndDisplayGoals()
                
                binding.progressBar.visibility = View.GONE
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Log.e("SavingsGoalsActivity", "Error fetching savings goals", e)
                Toast.makeText(this@SavingsGoalsActivity, "Failed to load savings goals: ${e.message}", Toast.LENGTH_SHORT).show()
                updateEmptyState()
            }
        }
    }
    
    private suspend fun processGoalsForDisplay(token: String) {
        displayGoalsList.clear()
        
        try {
            for (apiGoal in originalGoalsList) {
                val targetDate = try {
                    dateFormat.parse(apiGoal.targetDate) ?: Date()
                } catch (e: Exception) {
                    Date()
                }
                
                if (apiGoal.currency != userDefaultCurrency) {
                    try {
                        val convertedTargetAmount = CurrencyUtils.convertCurrency(
                            apiGoal.targetAmount,
                            apiGoal.currency,
                            userDefaultCurrency,
                            token
                        )
                        
                        val convertedCurrentAmount = CurrencyUtils.convertCurrency(
                            apiGoal.currentAmount,
                            apiGoal.currency,
                            userDefaultCurrency,
                            token
                        )
                        
                        if (convertedTargetAmount != null && convertedCurrentAmount != null) {
                            val trueOriginalTargetAmount = apiGoal.originalTargetAmount ?: apiGoal.targetAmount
                            val trueOriginalCurrentAmount = apiGoal.originalCurrentAmount ?: apiGoal.currentAmount
                            val trueOriginalCurrency = apiGoal.originalCurrency ?: apiGoal.currency
                            
                            val goal = SavingsGoal(
                                id = apiGoal.id,
                                goal = apiGoal.goal,
                                targetAmount = convertedTargetAmount,
                                currentAmount = convertedCurrentAmount,
                                targetDate = targetDate,
                                currency = userDefaultCurrency,
                                originalTargetAmount = trueOriginalTargetAmount,
                                originalCurrentAmount = trueOriginalCurrentAmount,
                                originalCurrency = trueOriginalCurrency
                            )
                            displayGoalsList.add(goal)
                        } else {
                            val goal = SavingsGoal(
                                id = apiGoal.id,
                                goal = apiGoal.goal,
                                targetAmount = apiGoal.targetAmount,
                                currentAmount = apiGoal.currentAmount,
                                targetDate = targetDate,
                                currency = apiGoal.currency,
                                originalTargetAmount = apiGoal.originalTargetAmount,
                                originalCurrentAmount = apiGoal.originalCurrentAmount,
                                originalCurrency = apiGoal.originalCurrency
                            )
                            displayGoalsList.add(goal)
                        }
                    } catch (e: Exception) {
                        Log.e("SavingsGoalsActivity", "Currency conversion error: ${e.message}")
                        val goal = SavingsGoal(
                            id = apiGoal.id,
                            goal = apiGoal.goal,
                            targetAmount = apiGoal.targetAmount,
                            currentAmount = apiGoal.currentAmount,
                            targetDate = targetDate,
                            currency = apiGoal.currency,
                            originalTargetAmount = apiGoal.originalTargetAmount,
                            originalCurrentAmount = apiGoal.originalCurrentAmount,
                            originalCurrency = apiGoal.originalCurrency
                        )
                        displayGoalsList.add(goal)
                    }
                } else {
                    val goal = SavingsGoal(
                        id = apiGoal.id,
                        goal = apiGoal.goal,
                        targetAmount = apiGoal.targetAmount,
                        currentAmount = apiGoal.currentAmount,
                        targetDate = targetDate,
                        currency = apiGoal.currency,
                        originalTargetAmount = apiGoal.originalTargetAmount,
                        originalCurrentAmount = apiGoal.originalCurrentAmount,
                        originalCurrency = apiGoal.originalCurrency
                    )
                    displayGoalsList.add(goal)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this@SavingsGoalsActivity, "Error processing goals: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateEmptyState() {
        if (displayGoalsList.isEmpty()) {
            binding.recyclerSavingsGoals.visibility = View.GONE
            binding.emptyStateLayout.visibility = View.VISIBLE
        } else {
            binding.recyclerSavingsGoals.visibility = View.VISIBLE
            binding.emptyStateLayout.visibility = View.GONE
        }
    }
}
