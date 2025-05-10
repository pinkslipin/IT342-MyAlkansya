package com.example.myalkansyamobile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myalkansyamobile.adapters.IncomeAdapter
import com.example.myalkansyamobile.api.IncomeRepository
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.databinding.ActivityIncomeBinding
import com.example.myalkansyamobile.model.Income
import com.example.myalkansyamobile.utils.CurrencyUtils
import com.example.myalkansyamobile.utils.Resource
import com.example.myalkansyamobile.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext   
import java.text.SimpleDateFormat
import java.util.*

class IncomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityIncomeBinding
    private lateinit var incomeAdapter: IncomeAdapter
    private val incomeList = mutableListOf<Income>()
    private val displayIncomeList = mutableListOf<Income>()
    private lateinit var sessionManager: SessionManager
    private lateinit var incomeRepository: IncomeRepository
    private var userDefaultCurrency: String = "PHP"
    
    // Add filter-related fields
    private val months = arrayOf(
        "All Months", "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    
    private val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1 // Calendar months are 0-indexed
    
    private val years = arrayOf(
        "All Years",
        (currentYear - 2).toString(),
        (currentYear - 1).toString(),
        currentYear.toString(),
        (currentYear + 1).toString()
    )
    
    private var filterMonth = 0 // 0 means all months
    private var filterYear = 0 // 0 means all years
    
    // Pagination variables
    private var currentPage = 1
    private var totalPages = 1
    private var itemsPerPage = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIncomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        incomeRepository = IncomeRepository(RetrofitClient.incomeApiService)
        userDefaultCurrency = sessionManager.getCurrency() ?: "PHP"

        setupRecyclerView()
        setupFilterSpinners()
        setupPagination()
        fetchIncomes()

        binding.btnAddIncome.setOnClickListener {
            val intent = Intent(this, AddIncomeActivity::class.java)
            startActivity(intent)
        }
        
        // Add filter button click listeners
        binding.btnApplyFilter.setOnClickListener {
            applyFilters()
        }
        
        binding.btnResetFilters.setOnClickListener {
            resetFilters()
        }
        
        // Add menu navigation
        binding.topBar.setOnClickListener {
            val intent = Intent(this, HomePageActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Check if currency preference changed
        val currentCurrency = sessionManager.getCurrency() ?: "PHP"
        if (currentCurrency != userDefaultCurrency) {
            userDefaultCurrency = currentCurrency
            // Update adapter with new currency
            incomeAdapter.updateDefaultCurrency(userDefaultCurrency)
            // Re-fetch and convert incomes with new currency
            fetchIncomes()
        } else {
            // Just refresh the income list when returning to this activity
            fetchIncomes()
        }
    }

    private fun setupRecyclerView() {
        // Initialize with mutableList
        incomeAdapter = IncomeAdapter(
            displayIncomeList.toMutableList(), // Pass as mutableList 
            { income -> navigateToEditIncome(income) },
            { income -> showDeleteConfirmation(income) },
            userDefaultCurrency
        )
        binding.recyclerViewIncome.apply {
            layoutManager = LinearLayoutManager(this@IncomeActivity)
            adapter = incomeAdapter
        }
    }

    private fun setupFilterSpinners() {
        // Setup month spinner
        val monthAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, months)
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMonth.adapter = monthAdapter
        
        // Default to current month
        binding.spinnerMonth.setSelection(currentMonth)
        filterMonth = currentMonth
        
        binding.spinnerMonth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterMonth = position
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        // Setup year spinner
        val yearAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, years)
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerYear.adapter = yearAdapter
        
        // Default to current year (index 3)
        binding.spinnerYear.setSelection(3)
        filterYear = currentYear
        
        binding.spinnerYear.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterYear = if (position == 0) 0 else years[position].toInt()
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun setupPagination() {
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
        
        updatePageDisplay()
    }
    
    private fun calculateTotalPages() {
        totalPages = if (displayIncomeList.isEmpty()) 1 else (displayIncomeList.size + itemsPerPage - 1) / itemsPerPage
    }
    
    private fun updatePageDisplay() {
        binding.tvPagination.text = "$currentPage out of $totalPages"
        binding.btnPrevPage.isEnabled = currentPage > 1
        binding.btnNextPage.isEnabled = currentPage < totalPages
        
        // Display current page content
        displayCurrentPage()
    }
    
    private fun displayCurrentPage() {
        try {
            val start = (currentPage - 1) * itemsPerPage
            val end = minOf(start + itemsPerPage, displayIncomeList.size)
            
            if (displayIncomeList.isEmpty()) {
                Log.d("IncomeActivity", "No incomes to display")
                incomeAdapter.updateList(emptyList())
                return
            }
            
            if (start >= displayIncomeList.size) {
                Log.e("IncomeActivity", "Pagination error: start index ($start) >= list size (${displayIncomeList.size})")
                incomeAdapter.updateList(emptyList())
                return
            }
            
            val currentPageItems = displayIncomeList.subList(start, end)
            Log.d("IncomeActivity", "Displaying page $currentPage with ${currentPageItems.size} items")
            
            incomeAdapter.updateList(currentPageItems)
        } catch (e: Exception) {
            Log.e("IncomeActivity", "Error displaying current page: ${e.message}", e)
            incomeAdapter.updateList(emptyList())
        }
    }
    
    private fun navigateToEditIncome(income: Income) {
        val intent = Intent(this, EditIncomeActivity::class.java)
        intent.putExtra("incomeId", income.id)
        startActivity(intent)
    }
    
    private fun showDeleteConfirmation(income: Income) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_income))
            .setMessage("Are you sure you want to delete this income: ${income.source}? This action cannot be undone.")
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                deleteIncome(income.id)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun deleteIncome(incomeId: Int) {
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val token = sessionManager.fetchAuthToken()
                if (token.isNullOrEmpty()) {
                    Toast.makeText(this@IncomeActivity, "Authentication required", Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                    return@launch
                }

                // Call the API to delete the income
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.incomeApiService.deleteIncome(incomeId, "Bearer $token")
                }

                binding.progressBar.visibility = View.GONE

                if (response.isSuccessful) {
                    Toast.makeText(this@IncomeActivity, "Income deleted successfully", Toast.LENGTH_SHORT).show()
                    // Refresh the income list
                    fetchIncomes()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                    Toast.makeText(this@IncomeActivity, "Error: $errorMsg", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@IncomeActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun applyFilters() {
        val filteredList = incomeList.filter { income ->
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = try {
                dateFormat.parse(income.date)
            } catch (e: Exception) {
                null
            }
            
            if (date == null) return@filter false
            
            val calendar = Calendar.getInstance()
            calendar.time = date
            
            val incomeMonth = calendar.get(Calendar.MONTH) + 1
            val incomeYear = calendar.get(Calendar.YEAR)
            
            val monthMatches = filterMonth == 0 || incomeMonth == filterMonth
            val yearMatches = filterYear == 0 || incomeYear == filterYear
            
            monthMatches && yearMatches
        }
        
        // Update the display list
        displayIncomeList.clear()
        displayIncomeList.addAll(filteredList)
        
        // Reset pagination
        currentPage = 1
        calculateTotalPages()
        updatePageDisplay()
        
        // Update active filters text
        updateActiveFiltersText()
        
        // Show empty state if needed
        binding.txtEmptyState.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerViewIncome.visibility = if (filteredList.isEmpty()) View.GONE else View.VISIBLE
        binding.txtEmptyState.text = getString(R.string.no_filtered_income)
    }
    
    private fun resetFilters() {
        binding.spinnerMonth.setSelection(0) // "All Months"
        binding.spinnerYear.setSelection(0) // "All Years"
        filterMonth = 0
        filterYear = 0
        
        // Reset to show all incomes
        displayIncomeList.clear()
        displayIncomeList.addAll(incomeList)
        
        // Reset pagination
        currentPage = 1
        calculateTotalPages()
        updatePageDisplay()
        
        // Hide active filters text
        binding.activeFiltersText.visibility = View.GONE
        
        // Update empty state visibility
        binding.txtEmptyState.visibility = if (incomeList.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerViewIncome.visibility = if (incomeList.isEmpty()) View.GONE else View.VISIBLE
        binding.txtEmptyState.text = getString(R.string.no_income_records)
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

    private fun fetchIncomes() {
        val token = sessionManager.fetchAuthToken()
        if (token.isNullOrEmpty()) {
            // If no token, redirect to login
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerViewIncome.visibility = View.GONE // Hide recycler while loading
        
        lifecycleScope.launch {
            try {
                when (val result = incomeRepository.getIncomes(token)) {
                    is Resource.Success -> {
                        Log.d("IncomeActivity", "Success: Received ${result.data.size} income records")
                        
                        // Store original list
                        incomeList.clear()
                        incomeList.addAll(result.data)
                        
                        // Process incomes for display with correct currency
                        processIncomesForDisplay(token)
                        
                        // Update UI visibility
                        binding.txtEmptyState.visibility = if (incomeList.isEmpty()) View.VISIBLE else View.GONE
                        binding.recyclerViewIncome.visibility = if (incomeList.isEmpty()) View.GONE else View.VISIBLE
                    }
                    is Resource.Error -> {
                        Log.e("IncomeActivity", "Error: ${result.message}")
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this@IncomeActivity, "Error: ${result.message}", Toast.LENGTH_SHORT).show()
                        binding.txtEmptyState.visibility = View.VISIBLE
                        binding.recyclerViewIncome.visibility = View.GONE
                    }
                    is Resource.Loading -> {
                        // Keep progress bar visible during loading
                        binding.progressBar.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                Log.e("IncomeActivity", "Exception during fetchIncomes: ${e.message}", e)
                binding.progressBar.visibility = View.GONE
                binding.txtEmptyState.visibility = View.VISIBLE
                binding.recyclerViewIncome.visibility = View.GONE
                Toast.makeText(this@IncomeActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private suspend fun processIncomesForDisplay(token: String) {
        displayIncomeList.clear()
        
        try {
            withContext(Dispatchers.Default) {
                for (income in incomeList) {
                    // Create a display copy of the income
                    val displayIncome = if (income.currency != userDefaultCurrency) {
                        try {
                            // Try to convert the amount
                            val convertedAmount = CurrencyUtils.convertCurrency(
                                income.amount,
                                income.currency ?: "PHP",
                                userDefaultCurrency,
                                token
                            )
                            
                            if (convertedAmount != null) {
                                // Create a new income with converted amount
                                // IMPORTANT: Preserve true original values when available
                                val trueOriginalAmount = income.originalAmount ?: income.amount
                                val trueOriginalCurrency = income.originalCurrency ?: income.currency
                                
                                Income(
                                    id = income.id,
                                    source = income.source,
                                    date = income.date,
                                    amount = convertedAmount,
                                    currency = userDefaultCurrency,
                                    originalAmount = trueOriginalAmount,
                                    originalCurrency = trueOriginalCurrency
                                )
                            } else {
                                // Conversion failed, use original
                                income
                            }
                        } catch (e: Exception) {
                            Log.e("IncomeActivity", "Currency conversion error: ${e.message}")
                            // On error, use original
                            income
                        }
                    } else {
                        // Same currency as default, but still need to check if we should preserve original data
                        if (income.originalAmount != null && income.originalCurrency != null) {
                            income // Keep original values intact
                        } else {
                            // No conversion needed and no original values
                            income
                        }
                    }
                    
                    displayIncomeList.add(displayIncome) // Make sure income is actually added
                }
                
                Log.d("IncomeActivity", "Processed ${displayIncomeList.size} incomes for display")
            }
            
            // Sort incomes by date (most recent first)
            displayIncomeList.sortByDescending { it.date }
            
            // Update pagination
            calculateTotalPages()
            
            // Update the UI on main thread
            withContext(Dispatchers.Main) {
                Log.d("IncomeActivity", "Updating UI with ${displayIncomeList.size} incomes")
                
                if (displayIncomeList.isEmpty()) {
                    binding.txtEmptyState.visibility = View.VISIBLE
                    binding.recyclerViewIncome.visibility = View.GONE
                } else {
                    binding.txtEmptyState.visibility = View.GONE
                    binding.recyclerViewIncome.visibility = View.VISIBLE
                    updatePageDisplay()
                }
                
                binding.progressBar.visibility = View.GONE
            }
        } catch (e: Exception) {
            Log.e("IncomeActivity", "Error processing incomes: ${e.message}", e)
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                binding.txtEmptyState.visibility = View.VISIBLE
                binding.recyclerViewIncome.visibility = View.GONE
                Toast.makeText(this@IncomeActivity, "Error processing incomes: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}