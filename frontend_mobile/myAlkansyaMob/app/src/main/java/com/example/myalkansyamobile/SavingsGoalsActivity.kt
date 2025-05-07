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
import com.example.myalkansyamobile.adapters.SavingsGoalAdapter
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.api.SavingsGoalResponse
import com.example.myalkansyamobile.databinding.ActivitySavingsGoalsBinding
import com.example.myalkansyamobile.model.SavingsGoal
import com.example.myalkansyamobile.utils.CurrencyUtils
import com.example.myalkansyamobile.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
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
    }
    
    private fun setupRecyclerView() {
        adapter = SavingsGoalAdapter(
            displayGoalsList,
            { goal -> // Click listener for goal item
                val intent = Intent(this, EditSavingsGoalActivity::class.java)
                intent.putExtra(EditSavingsGoalActivity.EXTRA_GOAL_ID, goal.id)
                startActivity(intent)
            }
        )
        binding.recyclerSavingsGoals.layoutManager = LinearLayoutManager(this)
        binding.recyclerSavingsGoals.adapter = adapter
    }

    private fun setupFilterSpinners() {
        // Status spinner setup
        val statusAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, STATUS_OPTIONS)
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStatus.adapter = statusAdapter
        
        // Sort by spinner setup
        val sortAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, SORT_OPTIONS)
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSortBy.adapter = sortAdapter
        
        // Add listeners
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
        currentPage = 1 // Reset to first page when applying filters
        filterAndDisplayGoals()
        
        // Show active filters indicator if any filters are applied
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
        // Reset spinner selections
        binding.spinnerStatus.setSelection(0)
        binding.spinnerSortBy.setSelection(0)
        
        // Clear filter variables
        selectedStatus = null
        selectedSortBy = null
        
        // Hide active filters indicator
        binding.activeFiltersText.visibility = View.GONE
        
        // Reset to page 1
        currentPage = 1
        
        // Apply the reset (show all goals)
        filterAndDisplayGoals()
    }
    
    private fun filterAndDisplayGoals() {
        filteredGoalsList.clear()
        filteredGoalsList.addAll(displayGoalsList)
        
        // Apply status filter
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
            "Progress" -> filteredGoalsList.sortBy { it.getProgressPercentage() }
            "Amount" -> filteredGoalsList.sortBy { it.targetAmount }
        }
        
        // Calculate total pages
        totalPages = Math.ceil(filteredGoalsList.size.toDouble() / itemsPerPage).toInt()
        if (totalPages == 0) totalPages = 1
        
        // Update pagination info
        updatePaginationControls()
        
        // Apply pagination
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
        
        // Enable/disable pagination buttons
        binding.btnPrevPage.isEnabled = currentPage > 1
        binding.btnNextPage.isEnabled = currentPage < totalPages
    }
    
    override fun onResume() {
        super.onResume()
        
        // Check if currency preference changed
        val currentCurrency = sessionManager.getCurrency() ?: "PHP"
        if (currentCurrency != userDefaultCurrency) {
            userDefaultCurrency = currentCurrency
            // Re-fetch and convert data with new currency
            fetchSavingsGoals()
        } else {
            // Just refresh the list
            fetchSavingsGoals()
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
                
                // Process goals for display with correct currency
                processGoalsForDisplay(token)
                
                // Apply filters (if any) 
                filterAndDisplayGoals()
                
                binding.progressBar.visibility = View.GONE
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@SavingsGoalsActivity, "Failed to load savings goals: ${e.message}", Toast.LENGTH_SHORT).show()
                updateEmptyState()
            }
        }
    }
    
    private suspend fun processGoalsForDisplay(token: String) {
        // Clear the main display list first (not the filtered one, we'll create that next)
        displayGoalsList.clear()
        
        try {
            for (apiGoal in originalGoalsList) {
                val targetDate = try {
                    dateFormat.parse(apiGoal.targetDate) ?: Date()
                } catch (e: Exception) {
                    Date()
                }
                
                // Check if we need to convert currencies
                if (apiGoal.currency != userDefaultCurrency) {
                    try {
                        // Try to convert amounts
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
                            // Use converted amounts with original values preserved
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
                            // Conversion failed, use original
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
                        // On error, use original
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
                    // No conversion needed
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
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.recyclerSavingsGoals.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.recyclerSavingsGoals.visibility = View.VISIBLE
        }
    }
}
