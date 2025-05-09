package com.example.myalkansyamobile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myalkansyamobile.adapters.ExpandableBudgetAdapter
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.databinding.ActivityBudgetListBinding
import com.example.myalkansyamobile.model.Budget
import com.example.myalkansyamobile.model.Expense
import com.example.myalkansyamobile.utils.CurrencyUtils
import com.example.myalkansyamobile.utils.SessionManager
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.*

class BudgetActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBudgetListBinding
    private lateinit var adapter: ExpandableBudgetAdapter
    private lateinit var sessionManager: SessionManager
    private var userDefaultCurrency = "PHP"
    
    private val budgetList = mutableListOf<Budget>()
    private val displayBudgetList = mutableListOf<Budget>()
    private val allExpenses = mutableListOf<Expense>()
    
    private val months = arrayOf(
        "All Months", "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    
    private val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private val years = arrayOf(
        "All Years",
        (currentYear - 1).toString(),
        currentYear.toString(),
        (currentYear + 1).toString()
    )
    
    private var filterMonth = 0 // 0 means all months
    private var filterYear = 0 // 0 means all years

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBudgetListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        sessionManager = SessionManager(this)
        userDefaultCurrency = sessionManager.getCurrency() ?: "PHP"
        
        setupUI()
        setupRecyclerView()
        fetchBudgets()
        fetchExpenses() // New: fetch expenses for each category
    }
    
    private fun setupUI() {
        // Set up back button in topBar
        binding.topBar.setOnClickListener {
            finish()
        }
        
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        // Set up add budget button
        binding.btnAddBudget.setOnClickListener {
            startActivity(Intent(this, AddBudgetActivity::class.java))
        }
        
        // Hide the header layout since we're using expandable items with their own headers
        binding.layoutHeaders.visibility = View.GONE

        // Set up month spinner
        val monthAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, months)
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMonth.adapter = monthAdapter
        
        // Default to current month
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
        binding.spinnerMonth.setSelection(currentMonth)
        filterMonth = currentMonth
        
        binding.spinnerMonth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterMonth = position
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        // Set up year spinner
        val yearAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, years)
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerYear.adapter = yearAdapter
        
        // Default to current year
        binding.spinnerYear.setSelection(2) // Current year is at index 2
        filterYear = currentYear
        
        binding.spinnerYear.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterYear = if (position == 0) 0 else years[position].toInt()
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        // Setup apply and reset filter buttons
        binding.btnApplyFilter.setOnClickListener {
            applyFilters()
            updateActiveFiltersText()
        }
        
        binding.btnResetFilters.setOnClickListener {
            // Reset spinners to default values
            binding.spinnerMonth.setSelection(0) // All months
            binding.spinnerYear.setSelection(0) // All years
            filterMonth = 0
            filterYear = 0
            applyFilters()
            binding.activeFiltersText.visibility = View.GONE
        }
    }
    
    private fun updateActiveFiltersText() {
        val activeFilters = mutableListOf<String>()
        
        if (filterMonth > 0) {
            activeFilters.add("Month: ${months[filterMonth]}")
        }
        
        if (filterYear > 0) {
            activeFilters.add("Year: $filterYear")
        }
        
        if (activeFilters.isNotEmpty()) {
            binding.activeFiltersText.text = getString(R.string.active_filters, activeFilters.joinToString(", "))
            binding.activeFiltersText.visibility = View.VISIBLE
        } else {
            binding.activeFiltersText.visibility = View.GONE
        }
    }
    
    private fun setupRecyclerView() {
        // Use the new expandable adapter
        adapter = ExpandableBudgetAdapter(
            displayBudgetList,
            { budget -> // Click listener for budget
                val intent = Intent(this, EditBudgetActivity::class.java)
                intent.putExtra("BUDGET_ID", budget.id)
                startActivity(intent)
            },
            userDefaultCurrency,
            { expense -> // Click listener for expense
                val intent = Intent(this, ExpenseActivity::class.java)
                intent.putExtra("EXPENSE_ID", expense.id)
                startActivity(intent)
            }
        )
        
        binding.recyclerBudgets.layoutManager = LinearLayoutManager(this)
        binding.recyclerBudgets.adapter = adapter
    }
    
    override fun onResume() {
        super.onResume()
        
        // Check if currency preference changed
        val currentCurrency = sessionManager.getCurrency() ?: "PHP"
        if (currentCurrency != userDefaultCurrency) {
            userDefaultCurrency = currentCurrency
            // Update adapter with new currency
            adapter.updateDefaultCurrency(userDefaultCurrency)
            // Re-fetch budgets with new currency
            fetchBudgets()
        } else {
            // Just refresh the budget list and expenses when returning
            fetchBudgets()
            fetchExpenses()
        }
    }
    
    private fun fetchExpenses() {
        val token = sessionManager.fetchAuthToken()
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }
        
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.expenseApiService.getExpenses("Bearer $token")
                
                allExpenses.clear()
                // Convert the API response to Expense model
                response.forEach { expenseResponse ->
                    val expense = Expense(
                        id = expenseResponse.id,
                        subject = expenseResponse.subject,
                        category = expenseResponse.category,
                        date = LocalDate.parse(expenseResponse.date),
                        amount = expenseResponse.amount,
                        currency = expenseResponse.currency ?: userDefaultCurrency
                    )
                    allExpenses.add(expense)
                }
                
                // Filter expenses based on current filters
                val filteredExpenses = if (filterMonth == 0 && filterYear == 0) {
                    allExpenses
                } else {
                    allExpenses.filter { expense ->
                        val monthMatches = filterMonth == 0 || expense.date.monthValue == filterMonth
                        val yearMatches = filterYear == 0 || expense.date.year == filterYear
                        monthMatches && yearMatches
                    }
                }
                
                // Update the adapter with all filtered expenses
                adapter.updateAllExpenses(filteredExpenses)
                
            } catch (e: Exception) {
                Log.e("BudgetActivity", "Error fetching expenses: ${e.message}", e)
                Toast.makeText(this@BudgetActivity, "Failed to load expense details", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun fetchBudgets() {
        val token = sessionManager.fetchAuthToken()
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }
        
        binding.progressLoading.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.budgetApiService.getUserBudgets("Bearer $token")
                
                if (response.isSuccessful && response.body() != null) {
                    // Store original list
                    budgetList.clear()
                    budgetList.addAll(response.body()!!)
                    
                    // Process budgets for display with correct currency
                    processBudgetsForDisplay(token)
                    
                    // Apply filters
                    applyFilters()
                    
                    binding.progressLoading.visibility = View.GONE
                } else {
                    binding.progressLoading.visibility = View.GONE
                    Toast.makeText(this@BudgetActivity, "Failed to fetch budgets", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.progressLoading.visibility = View.GONE
                Toast.makeText(this@BudgetActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private suspend fun processBudgetsForDisplay(token: String) {
        displayBudgetList.clear()
        
        try {
            for (budget in budgetList) {
                // Create a display copy of the budget
                val displayBudget = if (budget.currency != userDefaultCurrency) {
                    try {
                        // Try to convert the amount
                        val convertedBudget = CurrencyUtils.convertCurrency(
                            budget.monthlyBudget,
                            budget.currency,
                            userDefaultCurrency,
                            token
                        )
                        
                        val convertedSpent = CurrencyUtils.convertCurrency(
                            budget.totalSpent,
                            budget.currency,
                            userDefaultCurrency,
                            token
                        )
                        
                        if (convertedBudget != null && convertedSpent != null) {
                            // Create a new budget with converted amounts
                            // IMPORTANT: Preserve true original values when available
                            val trueOriginalAmount = budget.originalAmount ?: budget.monthlyBudget
                            val trueOriginalCurrency = budget.originalCurrency ?: budget.currency
                            
                            Budget(
                                id = budget.id,
                                category = budget.category,
                                monthlyBudget = convertedBudget,
                                totalSpent = convertedSpent,
                                currency = userDefaultCurrency,
                                budgetMonth = budget.budgetMonth,
                                budgetYear = budget.budgetYear,
                                originalAmount = trueOriginalAmount,
                                originalCurrency = trueOriginalCurrency
                            )
                        } else {
                            // Conversion failed, use original
                            budget
                        }
                    } catch (e: Exception) {
                        Log.e("BudgetActivity", "Currency conversion error: ${e.message}")
                        // On error, use original
                        budget
                    }
                } else {
                    // Same currency as default, no conversion needed
                    budget
                }
                
                displayBudgetList.add(displayBudget)
            }
            
        } catch (e: Exception) {
            Toast.makeText(this@BudgetActivity, "Error processing budgets: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun applyFilters() {
        val filteredList = if (filterMonth == 0 && filterYear == 0) {
            // No filters applied, show all budgets
            displayBudgetList
        } else {
            // Apply filters
            displayBudgetList.filter { budget ->
                val monthMatches = filterMonth == 0 || budget.budgetMonth == filterMonth
                val yearMatches = filterYear == 0 || budget.budgetYear == filterYear
                monthMatches && yearMatches
            }
        }
        
        adapter.updateData(filteredList)
        
        // Also update expenses based on the same filters
        fetchExpenses()
        
        // Show empty state if needed
        if (filteredList.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.tvEmptyState.text = if (filterMonth > 0 || filterYear > 0) {
                getString(R.string.no_filtered_budget)
            } else {
                getString(R.string.no_budget_records)
            }
        } else {
            binding.tvEmptyState.visibility = View.GONE
        }
    }
}
