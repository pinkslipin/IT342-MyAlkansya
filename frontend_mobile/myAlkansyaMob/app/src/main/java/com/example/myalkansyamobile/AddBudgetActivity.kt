package com.example.myalkansyamobile

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.databinding.ActivityAddBudgetBinding
import com.example.myalkansyamobile.model.Budget
import com.example.myalkansyamobile.utils.CurrencyUtils
import com.example.myalkansyamobile.utils.SessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

class AddBudgetActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddBudgetBinding
    private lateinit var sessionManager: SessionManager
    
    private val categories = arrayOf(
        "Food", "Transportation", "Housing", "Utilities", 
        "Entertainment", "Healthcare", "Education", "Shopping", "Other"
    )
    
    private val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1 // January is 0
    
    private var selectedCategory = ""
    private var selectedMonth = currentMonth
    private var selectedYear = currentYear
    private var selectedCurrency = "PHP"
    private var userDefaultCurrency = "PHP"
    
    private var originalAmount: Double? = null
    private var originalCurrency: String? = null
    private var conversionJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddBudgetBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        sessionManager = SessionManager(this)
        userDefaultCurrency = sessionManager.getCurrency() ?: "PHP"
        
        setupCategorySpinner()
        setupMonthYearSpinners()
        setupCurrencySpinner()
        setupButtons()
        setupAmountListener()
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
        // Use CurrencyUtils.currencyCodes instead of hardcoded currencies
        val currencyAdapter = ArrayAdapter(
            this, 
            android.R.layout.simple_spinner_item,
            CurrencyUtils.currencyCodes.map { code ->
                val isDefault = code == userDefaultCurrency
                CurrencyUtils.getCurrencyDisplayText(code) + if (isDefault) " (Default)" else ""
            }
        )
        currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCurrency.adapter = currencyAdapter
        
        // Set default selection to user's preferred currency
        val defaultPosition = CurrencyUtils.currencyCodes.indexOf(userDefaultCurrency)
        if (defaultPosition >= 0) {
            binding.spinnerCurrency.setSelection(defaultPosition)
        }
        
        binding.spinnerCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val newCurrency = CurrencyUtils.currencyCodes[position]
                handleCurrencyChange(newCurrency)
                selectedCurrency = newCurrency
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun handleCurrencyChange(newCurrency: String) {
        // Get current amount
        val amountStr = binding.etBudgetAmount.text.toString()
        val currentAmount = amountStr.toDoubleOrNull()
        
        // If amount exists and currency is changed
        if (currentAmount != null && currentAmount > 0) {
            val selectedCurrency = newCurrency
            val currentCurrency = originalCurrency ?: userDefaultCurrency
            
            if (selectedCurrency != currentCurrency) {
                // Save original values if this is first change
                if (originalAmount == null) {
                    originalAmount = currentAmount
                    originalCurrency = currentCurrency
                }
                
                // If changing back to original currency, restore original amount
                if (selectedCurrency == originalCurrency) {
                    binding.etBudgetAmount.setText(String.format("%.2f", originalAmount))
                } else {
                    // Otherwise convert to new currency
                    convertAmount(currentAmount, currentCurrency, selectedCurrency)
                }
            }
        }
        
        // Show notification if selected currency is different from user's default
        if (newCurrency != userDefaultCurrency) {
            binding.tvCurrencyWarning.text = 
                "Note: This budget will be automatically converted to $userDefaultCurrency when saved."
            binding.tvCurrencyWarning.visibility = View.VISIBLE
        } else {
            binding.tvCurrencyWarning.visibility = View.GONE
        }
    }
    
    private fun setupAmountListener() {
        binding.etBudgetAmount.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val amountStr = binding.etBudgetAmount.text.toString()
                if (amountStr.isNotEmpty()) {
                    try {
                        // Format to two decimal places
                        val amount = amountStr.toDouble()
                        binding.etBudgetAmount.setText(String.format("%.2f", amount))
                    } catch (e: Exception) {
                        binding.etBudgetAmount.error = "Invalid amount"
                    }
                }
            }
        }
    }
    
    private fun convertAmount(amount: Double, fromCurrency: String, toCurrency: String) {
        // Cancel any ongoing conversion
        conversionJob?.cancel()
        
        // Show loading indicator
        binding.progressBar.visibility = View.VISIBLE
        
        conversionJob = lifecycleScope.launch {
            try {
                val authToken = sessionManager.getToken()
                if (authToken.isNullOrEmpty()) {
                    Toast.makeText(
                        this@AddBudgetActivity,
                        "Error: Not logged in",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.progressBar.visibility = View.GONE
                    return@launch
                }
                
                val convertedAmount = CurrencyUtils.convertCurrency(
                    amount, 
                    fromCurrency, 
                    toCurrency, 
                    authToken
                )
                
                if (convertedAmount != null) {
                    binding.etBudgetAmount.setText(String.format("%.2f", convertedAmount))
                    Toast.makeText(
                        this@AddBudgetActivity,
                        "Converted ${CurrencyUtils.formatAmount(amount)} $fromCurrency to ${CurrencyUtils.formatAmount(convertedAmount)} $toCurrency",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@AddBudgetActivity,
                        "Conversion failed. Please enter amount manually.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@AddBudgetActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
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
        
        val token = sessionManager.fetchAuthToken()
        if (token.isNullOrEmpty()) {
            showError("Please login again")
            return
        }
        
        binding.progressBar.visibility = View.VISIBLE
        binding.btnAddBudget.isEnabled = false
        
        // If selected currency is different from user's default, convert it
        if (selectedCurrency != userDefaultCurrency) {
            lifecycleScope.launch {
                try {
                    val convertedAmount = CurrencyUtils.convertCurrency(
                        budgetAmount,
                        selectedCurrency,
                        userDefaultCurrency,
                        token
                    )
                    
                    if (convertedAmount != null) {
                        // Create budget with both original and converted values
                        val budget = Budget(
                            category = selectedCategory,
                            monthlyBudget = convertedAmount,
                            currency = userDefaultCurrency,
                            budgetMonth = selectedMonth,
                            budgetYear = selectedYear,
                            originalAmount = budgetAmount,
                            originalCurrency = selectedCurrency
                        )
                        
                        submitBudgetToServer(budget, token)
                    } else {
                        // Conversion failed
                        binding.progressBar.visibility = View.GONE
                        binding.btnAddBudget.isEnabled = true
                        showError("Currency conversion failed. Try again or use your default currency.")
                    }
                } catch (e: Exception) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnAddBudget.isEnabled = true
                    showError("Error: ${e.message}")
                }
            }
        } else {
            // Currency matches user's default, no need to convert
            val budget = Budget(
                category = selectedCategory,
                monthlyBudget = budgetAmount,
                currency = selectedCurrency,
                budgetMonth = selectedMonth,
                budgetYear = selectedYear
            )
            lifecycleScope.launch {
                submitBudgetToServer(budget, token)
            }
        }
    }
    
    private suspend fun submitBudgetToServer(budget: Budget, token: String) {
        try {
            val response = RetrofitClient.budgetApiService.createBudget(
                budget,
                "Bearer $token"
            )
            
            binding.progressBar.visibility = View.GONE
            binding.btnAddBudget.isEnabled = true
            
            if (response.isSuccessful) {
                Toast.makeText(this@AddBudgetActivity, "Budget added successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Unknown error occurred"
                showError("Failed to add budget: $errorMessage")
            }
        } catch (e: Exception) {
            binding.progressBar.visibility = View.GONE
            binding.btnAddBudget.isEnabled = true
            showError("Error: ${e.message}")
        }
    }
    
    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
