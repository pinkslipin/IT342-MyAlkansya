package com.example.myalkansyamobile

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.databinding.ActivityEditBudgetBinding
import com.example.myalkansyamobile.model.Budget
import com.example.myalkansyamobile.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

class EditBudgetActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditBudgetBinding
    private lateinit var sessionManager: SessionManager
    
    private val categories = arrayOf(
        "Food", "Transportation", "Housing", "Utilities", 
        "Entertainment", "Healthcare", "Education", "Shopping", "Other"
    )
    
    private val currencies = arrayOf("PHP", "USD", "EUR")
    
    private val months = arrayOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    
    private val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private val years = arrayOf(
        (currentYear - 1).toString(),
        currentYear.toString(),
        (currentYear + 1).toString(),
        (currentYear + 2).toString()
    )
    
    private var budgetId: Int = 0
    private lateinit var currentBudget: Budget

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBudgetBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        sessionManager = SessionManager(this)
        
        // Get budget ID from intent
        budgetId = intent.getIntExtra("BUDGET_ID", 0)
        if (budgetId == 0) {
            Toast.makeText(this, "Invalid budget ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        setupSpinners()
        setupButtons()
        fetchBudgetDetails()
    }
    
    private fun setupSpinners() {
        // Category spinner
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = categoryAdapter
        
        // Month spinner
        val monthAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, months)
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMonth.adapter = monthAdapter
        
        // Year spinner
        val yearAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, years)
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerYear.adapter = yearAdapter
        
        // Currency spinner
        val currencyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencies)
        currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCurrency.adapter = currencyAdapter
    }
    
    private fun setupButtons() {
        binding.btnSaveChanges.setOnClickListener {
            updateBudget()
        }
        
        binding.btnCancel.setOnClickListener {
            finish()
        }
        
        binding.btnBack.setOnClickListener {
            finish()
        }
    }
    
    private fun fetchBudgetDetails() {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val token = sessionManager.fetchAuthToken() ?: ""
                if (token.isBlank()) {
                    showError("Please login again")
                    finish()
                    return@launch
                }
                
                val response = RetrofitClient.budgetApiService.getBudgetById(
                    budgetId,
                    "Bearer $token"
                )
                
                if (response.isSuccessful && response.body() != null) {
                    currentBudget = response.body()!!
                    populateForm(currentBudget)
                    showLoading(false)
                } else {
                    showError("Failed to fetch budget details")
                    finish()
                }
            } catch (e: Exception) {
                showError("Error: ${e.message}")
                finish()
            }
        }
    }
    
    private fun populateForm(budget: Budget) {
        // Set category
        val categoryIndex = categories.indexOf(budget.category)
        if (categoryIndex != -1) {
            binding.spinnerCategory.setSelection(categoryIndex)
        }
        
        // Set month
        val monthIndex = budget.budgetMonth - 1
        if (monthIndex in 0..11) {
            binding.spinnerMonth.setSelection(monthIndex)
        }
        
        // Set year
        val yearIndex = years.indexOf(budget.budgetYear.toString())
        if (yearIndex != -1) {
            binding.spinnerYear.setSelection(yearIndex)
        }
        
        // Set budget amount
        binding.etBudgetAmount.setText(budget.monthlyBudget.toString())
        
        // Set currency
        val currencyIndex = currencies.indexOf(budget.currency)
        if (currencyIndex != -1) {
            binding.spinnerCurrency.setSelection(currencyIndex)
        }
        
        // Set total spent (read-only)
        val currencyFormat = NumberFormat.getCurrencyInstance()
        currencyFormat.currency = Currency.getInstance(budget.currency)
        binding.tvTotalSpent.text = currencyFormat.format(budget.totalSpent)
    }
    
    private fun updateBudget() {
        // Validate input
        val budgetAmountText = binding.etBudgetAmount.text.toString()
        if (budgetAmountText.isEmpty()) {
            showError("Please enter a budget amount")
            return
        }
        
        val budgetAmount = try {
            budgetAmountText.toDouble()
        } catch (e: NumberFormatException) {
            showError("Please enter a valid number for the budget amount")
            return
        }
        
        if (budgetAmount <= 0) {
            showError("Budget amount must be greater than zero")
            return
        }
        
        // Get values from spinners
        val category = categories[binding.spinnerCategory.selectedItemPosition]
        val month = binding.spinnerMonth.selectedItemPosition + 1 // 1-indexed
        val year = years[binding.spinnerYear.selectedItemPosition].toInt()
        val currency = currencies[binding.spinnerCurrency.selectedItemPosition]
        
        // Create updated budget object
        val updatedBudget = Budget(
            id = budgetId,
            category = category,
            monthlyBudget = budgetAmount,
            totalSpent = currentBudget.totalSpent,
            currency = currency,
            budgetMonth = month,
            budgetYear = year
        )
        
        // Submit to API
        lifecycleScope.launch {
            showLoading(true)
            
            try {
                val token = sessionManager.fetchAuthToken() ?: ""
                if (token.isBlank()) {
                    showError("Please login again")
                    return@launch
                }
                
                val response = RetrofitClient.budgetApiService.updateBudget(
                    budgetId,
                    updatedBudget,
                    "Bearer $token"
                )
                
                showLoading(false)
                
                if (response.isSuccessful) {
                    Toast.makeText(this@EditBudgetActivity, "Budget updated successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val errorMessage = response.errorBody()?.string() ?: "Unknown error occurred"
                    showError("Failed to update budget: $errorMessage")
                }
            } catch (e: Exception) {
                showLoading(false)
                showError("Error: ${e.message}")
            }
        }
    }
    
    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun showLoading(isLoading: Boolean) {
        binding.progressLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.cardForm.visibility = if (isLoading) View.GONE else View.VISIBLE
    }
}
