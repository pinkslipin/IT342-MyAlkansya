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
import com.example.myalkansyamobile.databinding.ActivityEditBudgetBinding
import com.example.myalkansyamobile.model.Budget
import com.example.myalkansyamobile.utils.CurrencyUtils
import com.example.myalkansyamobile.utils.SessionManager
import kotlinx.coroutines.Job
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
    private var userDefaultCurrency: String = "PHP"
    
    private var originalAmount: Double? = null
    private var originalCurrency: String? = null
    private var conversionJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBudgetBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        sessionManager = SessionManager(this)
        userDefaultCurrency = sessionManager.getCurrency() ?: "PHP"
        
        budgetId = intent.getIntExtra("BUDGET_ID", 0)
        if (budgetId == 0) {
            Toast.makeText(this, "Invalid budget ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        setupSpinners()
        setupButtons()
        setupAmountListener()
        fetchBudgetDetails()
    }
    
    private fun setupSpinners() {
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = categoryAdapter
        
        val monthAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, months)
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMonth.adapter = monthAdapter
        
        val yearAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, years)
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerYear.adapter = yearAdapter
        
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
        
        binding.spinnerCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCurrency = CurrencyUtils.currencyCodes[position]
                handleCurrencyChange(selectedCurrency)
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun handleCurrencyChange(newCurrency: String) {
        val amountStr = binding.etBudgetAmount.text.toString()
        val currentAmount = amountStr.toDoubleOrNull()
        
        if (currentAmount != null && currentAmount > 0) {
            val selectedCurrency = newCurrency
            val currentCurrency = originalCurrency ?: userDefaultCurrency
            
            if (selectedCurrency != currentCurrency) {
                if (originalAmount == null) {
                    originalAmount = currentAmount
                    originalCurrency = currentCurrency
                }
                
                if (selectedCurrency == originalCurrency) {
                    binding.etBudgetAmount.setText(String.format("%.2f", originalAmount))
                } else {
                    convertAmount(currentAmount, currentCurrency, selectedCurrency)
                }
            }
        }
        
        if (binding.tvCurrencyWarning != null && newCurrency != userDefaultCurrency) {
            binding.tvCurrencyWarning.text = 
                "Note: This budget will be automatically converted to $userDefaultCurrency when saved."
            binding.tvCurrencyWarning.visibility = View.VISIBLE
        } else if (binding.tvCurrencyWarning != null) {
            binding.tvCurrencyWarning.visibility = View.GONE
        }
    }
    
    private fun setupAmountListener() {
        binding.etBudgetAmount.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val amountStr = binding.etBudgetAmount.text.toString()
                if (amountStr.isNotEmpty()) {
                    try {
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
        conversionJob?.cancel()
        
        binding.progressLoading.visibility = View.VISIBLE
        
        conversionJob = lifecycleScope.launch {
            try {
                val authToken = sessionManager.getToken()
                if (authToken.isNullOrEmpty()) {
                    Toast.makeText(
                        this@EditBudgetActivity,
                        "Error: Not logged in",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.progressLoading.visibility = View.GONE
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
                        this@EditBudgetActivity,
                        "Converted ${CurrencyUtils.formatAmount(amount)} $fromCurrency to ${CurrencyUtils.formatAmount(convertedAmount)} $toCurrency",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@EditBudgetActivity,
                        "Conversion failed. Please enter amount manually.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@EditBudgetActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.progressLoading.visibility = View.GONE
            }
        }
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
        val categoryIndex = categories.indexOf(budget.category)
        if (categoryIndex != -1) {
            binding.spinnerCategory.setSelection(categoryIndex)
        }
        
        val monthIndex = budget.budgetMonth - 1
        if (monthIndex in 0..11) {
            binding.spinnerMonth.setSelection(monthIndex)
        }
        
        val yearIndex = years.indexOf(budget.budgetYear.toString())
        if (yearIndex != -1) {
            binding.spinnerYear.setSelection(yearIndex)
        }
        
        if (budget.originalAmount != null && budget.originalCurrency != null) {
            binding.etBudgetAmount.setText(String.format("%.2f", budget.originalAmount))
            originalAmount = budget.originalAmount
            originalCurrency = budget.originalCurrency
            
            val currencyPosition = CurrencyUtils.currencyCodes.indexOf(budget.originalCurrency)
            if (currencyPosition >= 0) {
                binding.spinnerCurrency.setSelection(currencyPosition)
            }
        } else {
            binding.etBudgetAmount.setText(String.format("%.2f", budget.monthlyBudget))
            originalAmount = budget.monthlyBudget
            originalCurrency = budget.currency
            
            val currencyPosition = CurrencyUtils.currencyCodes.indexOf(budget.currency)
            if (currencyPosition >= 0) {
                binding.spinnerCurrency.setSelection(currencyPosition)
            }
        }
        
        val currencyFormat = NumberFormat.getCurrencyInstance()
        try {
            currencyFormat.currency = Currency.getInstance(budget.currency)
        } catch (e: Exception) {
            currencyFormat.currency = Currency.getInstance("USD")
        }
        binding.tvTotalSpent.text = currencyFormat.format(budget.totalSpent)
    }
    
    private fun updateBudget() {
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
        
        val category = categories[binding.spinnerCategory.selectedItemPosition]
        val month = binding.spinnerMonth.selectedItemPosition + 1
        val year = years[binding.spinnerYear.selectedItemPosition].toInt()
        val selectedCurrency = CurrencyUtils.currencyCodes[binding.spinnerCurrency.selectedItemPosition]
        
        val token = sessionManager.fetchAuthToken()
        if (token.isNullOrEmpty()) {
            showError("Please login again")
            return
        }
        
        binding.progressLoading.visibility = View.VISIBLE
        binding.btnSaveChanges.isEnabled = false
        
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
                        val updatedBudget = Budget(
                            id = budgetId,
                            category = category,
                            monthlyBudget = convertedAmount,
                            totalSpent = currentBudget.totalSpent,
                            currency = userDefaultCurrency,
                            budgetMonth = month,
                            budgetYear = year,
                            originalAmount = budgetAmount,
                            originalCurrency = selectedCurrency
                        )
                        
                        submitBudgetUpdate(updatedBudget, token)
                    } else {
                        binding.progressLoading.visibility = View.GONE
                        binding.btnSaveChanges.isEnabled = true
                        showError("Currency conversion failed. Try again or use your default currency.")
                    }
                } catch (e: Exception) {
                    binding.progressLoading.visibility = View.GONE
                    binding.btnSaveChanges.isEnabled = true
                    showError("Error: ${e.message}")
                }
            }
        } else {
            val updatedBudget = Budget(
                id = budgetId,
                category = category,
                monthlyBudget = budgetAmount,
                totalSpent = currentBudget.totalSpent,
                currency = selectedCurrency,
                budgetMonth = month,
                budgetYear = year
            )
            lifecycleScope.launch {
                submitBudgetUpdate(updatedBudget, token)
            }
        }
    }
    
    private suspend fun submitBudgetUpdate(budget: Budget, token: String) {
        try {
            val response = RetrofitClient.budgetApiService.updateBudget(
                budgetId,
                budget,
                "Bearer $token"
            )
            
            binding.progressLoading.visibility = View.GONE
            binding.btnSaveChanges.isEnabled = true
            
            if (response.isSuccessful) {
                Toast.makeText(this@EditBudgetActivity, "Budget updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Unknown error occurred"
                showError("Failed to update budget: $errorMessage")
            }
        } catch (e: Exception) {
            binding.progressLoading.visibility = View.GONE
            binding.btnSaveChanges.isEnabled = true
            showError("Error: ${e.message}")
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
