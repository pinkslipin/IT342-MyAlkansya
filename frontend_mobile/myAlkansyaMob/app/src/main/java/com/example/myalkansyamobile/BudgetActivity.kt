package com.example.myalkansyamobile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myalkansyamobile.adapters.BudgetAdapter
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.databinding.ActivityBudgetListBinding
import com.example.myalkansyamobile.model.Budget
import com.example.myalkansyamobile.utils.SessionManager
import kotlinx.coroutines.launch
import java.util.*

class BudgetActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBudgetListBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var budgetAdapter: BudgetAdapter
    private var currentPage = 1
    private var itemsPerPage = 10
    private val budgets = mutableListOf<Budget>()
    
    private val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1 // January is 0
    
    private var selectedMonth = currentMonth
    private var selectedYear = currentYear

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBudgetListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        sessionManager = SessionManager(this)
        
        setupRecyclerView()
        setupFilterSpinners()
        setupAddButton()
        setupPagination()
        setupBackNavigation()
        
        // Initial data load
        fetchBudgets()
    }
    
    private fun setupRecyclerView() {
        budgetAdapter = BudgetAdapter(budgets) { budget ->
            // Handle edit click
            val intent = Intent(this, EditBudgetActivity::class.java)
            intent.putExtra("BUDGET_ID", budget.id)
            startActivity(intent)
        }
        
        binding.recyclerBudgets.apply {
            layoutManager = LinearLayoutManager(this@BudgetActivity)
            adapter = budgetAdapter
        }
    }
    
    private fun setupFilterSpinners() {
        // Month spinner
        val months = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        
        val monthAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, months)
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMonth.adapter = monthAdapter
        binding.spinnerMonth.setSelection(currentMonth - 1) // Set to current month (0-indexed)
        
        // Year spinner
        val years = arrayOf(
            (currentYear - 1).toString(),
            currentYear.toString(),
            (currentYear + 1).toString(),
            (currentYear + 2).toString()
        )
        
        val yearAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, years)
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerYear.adapter = yearAdapter
        binding.spinnerYear.setSelection(1) // Set to current year
        
        // Set listeners
        binding.spinnerMonth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedMonth = position + 1 // Months are 1-indexed
                fetchBudgets()
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        binding.spinnerYear.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedYear = years[position].toInt()
                fetchBudgets()
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun setupAddButton() {
        binding.btnAddBudget.setOnClickListener {
            val intent = Intent(this, AddBudgetActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun setupPagination() {
        binding.btnPrevPage.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                updatePaginationUI()
                displayCurrentPageItems()
            }
        }
        
        binding.btnNextPage.setOnClickListener {
            val totalPages = calculateTotalPages()
            if (currentPage < totalPages) {
                currentPage++
                updatePaginationUI()
                displayCurrentPageItems()
            }
        }
    }
    
    private fun setupBackNavigation() {
        binding.btnBack.setOnClickListener {
            // Navigate back to homepage
            val intent = Intent(this, HomePageActivity::class.java)
            startActivity(intent)
            finish() // Optional: finish this activity to avoid back stack issues
        }
    }
    
    private fun fetchBudgets() {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val token = sessionManager.fetchAuthToken() ?: ""
                if (token.isBlank()) {
                    Toast.makeText(this@BudgetActivity, "Please login again", Toast.LENGTH_SHORT).show()
                    navigateToLogin()
                    return@launch
                }
                
                val response = RetrofitClient.budgetApiService.getBudgetsByMonth(
                    selectedMonth,
                    selectedYear,
                    "Bearer $token"
                )
                
                showLoading(false)
                
                if (response.isSuccessful && response.body() != null) {
                    budgets.clear()
                    budgets.addAll(response.body()!!)
                    
                    // Reset to page 1 when filter changes
                    currentPage = 1
                    updatePaginationUI()
                    displayCurrentPageItems()
                    
                    if (budgets.isEmpty()) {
                        showEmptyState(true)
                    } else {
                        showEmptyState(false)
                    }
                } else {
                    Toast.makeText(this@BudgetActivity, "Failed to fetch budgets", Toast.LENGTH_SHORT).show()
                    showEmptyState(true)
                }
            } catch (e: Exception) {
                showLoading(false)
                showEmptyState(true)
                Toast.makeText(this@BudgetActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun displayCurrentPageItems() {
        val startIndex = (currentPage - 1) * itemsPerPage
        val endIndex = minOf(startIndex + itemsPerPage, budgets.size)
        
        if (startIndex <= endIndex) {
            val currentPageItems = budgets.subList(startIndex, endIndex)
            budgetAdapter.updateData(currentPageItems)
        }
    }
    
    private fun calculateTotalPages(): Int {
        return (budgets.size + itemsPerPage - 1) / itemsPerPage
    }
    
    private fun updatePaginationUI() {
        val totalPages = calculateTotalPages().coerceAtLeast(1)
        binding.tvPagination.text = "$currentPage of $totalPages"
        binding.btnPrevPage.isEnabled = currentPage > 1
        binding.btnNextPage.isEnabled = currentPage < totalPages
    }
    
    private fun showLoading(isLoading: Boolean) {
        binding.progressLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.recyclerBudgets.visibility = if (isLoading) View.GONE else View.VISIBLE
        binding.layoutHeaders.visibility = if (isLoading) View.GONE else View.VISIBLE
        binding.layoutPagination.visibility = if (isLoading) View.GONE else View.VISIBLE
    }
    
    private fun showEmptyState(isEmpty: Boolean) {
        binding.tvEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerBudgets.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.layoutHeaders.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.layoutPagination.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
    
    private fun showNoBudgetsMessage() {
        showEmptyState(true)
    }
    
    private fun hideNoBudgetsMessage() {
        showEmptyState(false)
    }
    
    private fun getMonthName(month: Int): String {
        val months = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        return months[month - 1]
    }
    
    private fun navigateToLogin() {
        val intent = Intent(this, SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh data when returning to activity
        fetchBudgets()
    }
}
