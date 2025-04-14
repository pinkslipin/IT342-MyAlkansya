package com.example.myalkansyamobile

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.databinding.ActivityAddBudgetBinding
import com.example.myalkansyamobile.model.Budget
import com.example.myalkansyamobile.utils.SessionManager
import kotlinx.coroutines.launch
import java.util.*

class AddBudgetActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddBudgetBinding
    private lateinit var sessionManager: SessionManager
    
    private val categories = arrayOf(
        "Food", "Transportation", "Housing", "Utilities", 
        "Entertainment", "Healthcare", "Education", "Shopping", "Other"
    )
    
    private val currencies = arrayOf("PHP", "USD", "EUR")
    
    private val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1 // January is 0
    
    private var selectedCategory = ""
    private var selectedMonth = currentMonth
    private var selectedYear = currentYear
    private var selectedCurrency = "PHP"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddBudgetBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        sessionManager = SessionManager(this)
        
        setupCategorySpinner()
        setupMonthYearSpinners()
        setupCurrencySpinner()
        setupButtons()
    }
    
    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter
        
        binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCategory = categories[position]
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun setupMonthYearSpinners() {
        // Month spinner
        val months = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        
        val monthAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, months)
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMonth.adapter = monthAdapter
        binding.spinnerMonth.setSelection(currentMonth - 1) // Set to current month (0-indexed)
        
        binding.spinnerMonth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedMonth = position + 1 // Months are 1-indexed
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
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
        binding.spinnerYear.setSelection(1) // Current year is the second item (index 1)
        
        binding.spinnerYear.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedYear = years[position].toInt()
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun setupCurrencySpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCurrency.adapter = adapter
        
        binding.spinnerCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCurrency = currencies[position]
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun setupButtons() {
        binding.btnAddBudget.setOnClickListener {
            submitBudget()
        }
        
        binding.btnCancel.setOnClickListener {
            finish()
        }
        
        binding.btnBack.setOnClickListener {
            finish()
        }
    }
    
    private fun submitBudget() {
        // Validate input
        val budgetAmountText = binding.etBudgetAmount.text.toString()
        if (selectedCategory.isEmpty() || budgetAmountText.isEmpty()) {
            showError("Please select a category and enter a budget amount")
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
        
        // Create budget object
        val budget = Budget(
            category = selectedCategory,
            monthlyBudget = budgetAmount,
            currency = selectedCurrency,
            budgetMonth = selectedMonth,
            budgetYear = selectedYear
        )
        
        // Submit to API
        lifecycleScope.launch {
            try {
                val token = sessionManager.fetchAuthToken() ?: ""
                if (token.isBlank()) {
                    showError("Please login again")
                    return@launch
                }
                
                val response = RetrofitClient.budgetApiService.createBudget(
                    budget,
                    "Bearer $token"
                )
                
                if (response.isSuccessful) {
                    Toast.makeText(this@AddBudgetActivity, "Budget added successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val errorMessage = response.errorBody()?.string() ?: "Unknown error occurred"
                    showError("Failed to add budget: $errorMessage")
                }
            } catch (e: Exception) {
                showError("Error: ${e.message}")
            }
        }
    }
    
    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
