package com.example.myalkansyamobile

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myalkansyamobile.model.Expense
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.api.ExpenseRequest
import com.example.myalkansyamobile.utils.CurrencyUtils
import com.example.myalkansyamobile.utils.SessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.*

class AddExpenseActivity : AppCompatActivity() {
    private lateinit var etSubject: EditText
    private lateinit var tvDate: TextView
    private lateinit var spinnerCategory: Spinner
    private lateinit var etAmount: EditText
    private lateinit var spinnerCurrency: Spinner
    private lateinit var btnAddExpense: Button
    private lateinit var btnCancel: Button
    private lateinit var btnPickDate: ImageView
    private lateinit var tvConversionInfo: TextView
    private lateinit var tvCurrencyWarning: TextView
    private lateinit var tvError: TextView
    private lateinit var manualCategoryLayout: LinearLayout
    private lateinit var etManualCategory: EditText
    private lateinit var progressBar: ProgressBar
    
    private lateinit var sessionManager: SessionManager
    
    private var selectedDate = LocalDate.now()
    private var userDefaultCurrency: String = "PHP"
    private var originalAmount: Double? = null
    private var originalCurrency: String? = null
    private var conversionJob: Job? = null
    
    private val categories = arrayOf(
        "Food", "Transportation", "Housing", "Utilities", 
        "Entertainment", "Healthcare", "Education", "Shopping", 
        "Personal Care", "Debt Payment", "Savings", "Other"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)
        
        sessionManager = SessionManager(this)
        userDefaultCurrency = sessionManager.getCurrency() ?: "PHP"
        
        initializeUI()
        setupCategoryListener()
        setupCurrencySpinner()
        setupAmountListener()
        setupClickListeners()
        
        // Set initial date
        tvDate.text = selectedDate.toString()
    }
    
    private fun initializeUI() {
        try {
            etSubject = findViewById(R.id.etSubject)
            tvDate = findViewById(R.id.tvDate)
            spinnerCategory = findViewById(R.id.spinnerCategory)
            etAmount = findViewById(R.id.etAmount)
            spinnerCurrency = findViewById(R.id.spinnerCurrency)
            btnAddExpense = findViewById(R.id.btnAddExpense)
            btnCancel = findViewById(R.id.btnCancel)
            btnPickDate = findViewById(R.id.btnPickDate)
            tvConversionInfo = findViewById(R.id.tvConversionInfo)
            tvCurrencyWarning = findViewById(R.id.tvCurrencyWarning)
            tvError = findViewById(R.id.tvError)
            manualCategoryLayout = findViewById(R.id.manualCategoryLayout)
            etManualCategory = findViewById(R.id.etManualCategory)
            progressBar = findViewById(R.id.progressBar)
            
            // Set up category adapter
            val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
            spinnerCategory.adapter = categoryAdapter
            
        } catch (e: Exception) {
            Log.e("AddExpenseActivity", "Error finding views: ${e.message}")
            Toast.makeText(this, "Failed to initialize UI: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun setupClickListeners() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
        
        btnPickDate.setOnClickListener {
            showDatePicker()
        }
        
        btnAddExpense.setOnClickListener {
            addExpense()
        }
        
        btnCancel.setOnClickListener {
            finish()
        }
    }
    
    private fun setupCategoryListener() {
        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (categories[position] == "Other") {
                    manualCategoryLayout.visibility = View.VISIBLE
                } else {
                    manualCategoryLayout.visibility = View.GONE
                    etManualCategory.text.clear()
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                manualCategoryLayout.visibility = View.GONE
            }
        }
    }
    
    private fun setupCurrencySpinner() {
        try {
            val currencyItems = CurrencyUtils.currencyCodes.map { code -> 
                val isDefault = code == userDefaultCurrency
                val displayText = CurrencyUtils.getCurrencyDisplayText(code) + if (isDefault) " (Default)" else ""
                displayText
            }
            
            val currencyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencyItems)
            currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCurrency.adapter = currencyAdapter
            
            val defaultPosition = CurrencyUtils.currencyCodes.indexOf(userDefaultCurrency)
            if (defaultPosition >= 0) {
                spinnerCurrency.setSelection(defaultPosition)
            }
            
            spinnerCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedCurrency = CurrencyUtils.currencyCodes[position]
                    handleCurrencyChange(selectedCurrency)
                }
                
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        } catch (e: Exception) {
            Log.e("AddExpenseActivity", "Error setting up currency spinner: ${e.message}")
        }
    }
    
    private fun setupAmountListener() {
        etAmount.setOnFocusChangeListener { _, hasFocus -> 
            if (!hasFocus) {
                val amountStr = etAmount.text.toString()
                if (amountStr.isNotEmpty()) {
                    try {
                        val amount = amountStr.toDouble()
                        etAmount.setText(String.format("%.2f", amount))
                    } catch (e: Exception) {
                        etAmount.error = "Invalid amount"
                    }
                }
            }
        }
    }
    
    private fun getSelectedCurrency(): String {
        val position = spinnerCurrency.selectedItemPosition
        return CurrencyUtils.currencyCodes[position]
    }
    
    private fun handleCurrencyChange(newCurrency: String) {
        val amountStr = etAmount.text.toString()
        val currentAmount = amountStr.toDoubleOrNull()
        
        if (currentAmount != null && currentAmount > 0) {
            val selectedCurrency = newCurrency
            val currentCurrency = originalCurrency ?: getSelectedCurrency()
            
            if (selectedCurrency != currentCurrency) {
                if (originalAmount == null) {
                    originalAmount = currentAmount
                    originalCurrency = currentCurrency
                }
                
                if (selectedCurrency == originalCurrency) {
                    etAmount.setText(String.format("%.2f", originalAmount))
                    tvConversionInfo.visibility = View.GONE
                } else {
                    convertAmount(currentAmount, currentCurrency, selectedCurrency)
                }
            }
        }
        
        updateConversionNotification(newCurrency)
    }
    
    private fun updateConversionNotification(selectedCurrency: String) {
        if (selectedCurrency != userDefaultCurrency) {
            tvCurrencyWarning.text = 
                "Note: This expense will be automatically converted to $userDefaultCurrency when saved."
            tvCurrencyWarning.visibility = View.VISIBLE
        } else {
            tvCurrencyWarning.visibility = View.GONE
        }
    }
    
    private fun convertAmount(amount: Double, fromCurrency: String, toCurrency: String) {
        progressBar.visibility = View.VISIBLE
        tvConversionInfo.text = "Converting..."
        tvConversionInfo.visibility = View.VISIBLE
        
        conversionJob?.cancel()
        
        conversionJob = lifecycleScope.launch {
            try {
                val authToken = sessionManager.getToken()
                if (authToken.isNullOrEmpty()) {
                    progressBar.visibility = View.GONE
                    tvConversionInfo.text = "Error: Not logged in"
                    return@launch
                }
                
                val convertedAmount = CurrencyUtils.convertCurrency(
                    amount, 
                    fromCurrency, 
                    toCurrency, 
                    authToken
                )
                
                progressBar.visibility = View.GONE
                
                if (convertedAmount != null) {
                    etAmount.setText(String.format("%.2f", convertedAmount))
                    tvConversionInfo.text = "Converted ${CurrencyUtils.formatAmount(amount)} $fromCurrency to ${CurrencyUtils.formatAmount(convertedAmount)} $toCurrency"
                    tvConversionInfo.visibility = View.VISIBLE
                } else {
                    tvConversionInfo.text = "Conversion failed. Please enter amount manually."
                    tvConversionInfo.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                tvConversionInfo.text = "Error: ${e.message}"
                tvConversionInfo.visibility = View.VISIBLE
            }
        }
    }
    
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        
        val datePickerDialog = DatePickerDialog(this,
            { _, year, month, day -> 
                selectedDate = LocalDate.of(year, month + 1, day)
                tvDate.text = selectedDate.toString()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }
    
    private fun addExpense() {
        val subject = etSubject.text.toString().trim()
        val selectedCategoryPosition = spinnerCategory.selectedItemPosition
        var category = categories[selectedCategoryPosition]
        val amountString = etAmount.text.toString().trim()
        val currency = getSelectedCurrency()
        
        if (subject.isEmpty()) {
            showError("Subject cannot be empty")
            return
        }
        
        if (amountString.isEmpty()) {
            showError("Amount cannot be empty")
            return
        }
        
        // Handle manual category if "Other" is selected
        if (category == "Other") {
            val manualCategory = etManualCategory.text.toString().trim()
            if (manualCategory.isEmpty()) {
                showError("Please specify the category")
                return
            }
            category = manualCategory
        }
        
        val amount = try {
            amountString.toDouble()
        } catch (e: NumberFormatException) {
            showError("Invalid amount format")
            return
        }
        
        if (amount <= 0) {
            showError("Amount must be greater than zero")
            return
        }
        
        progressBar.visibility = View.VISIBLE
        tvError.visibility = View.GONE
        
        if (currency != userDefaultCurrency) {
            tvCurrencyWarning.text = "Converting to $userDefaultCurrency before saving..."
            tvCurrencyWarning.visibility = View.VISIBLE

            lifecycleScope.launch {
                try {
                    val token = sessionManager.getToken()
                    if (token == null) {
                        progressBar.visibility = View.GONE
                        showError("Authentication required")
                        return@launch
                    }
                    
                    val convertedAmount = CurrencyUtils.convertCurrency(
                        amount, 
                        currency, 
                        userDefaultCurrency, 
                        token
                    )
                    
                    if (convertedAmount != null) {
                        val expense = Expense(
                            id = 0,
                            subject = subject,
                            category = category,
                            date = selectedDate,
                            amount = convertedAmount,
                            currency = userDefaultCurrency,
                            originalAmount = amount,
                            originalCurrency = currency
                        )
                        
                        submitExpense(expense)
                    } else {
                        progressBar.visibility = View.GONE
                        showError("Currency conversion failed. Using original values.")
                        
                        val expense = Expense(
                            id = 0,
                            subject = subject,
                            category = category,
                            date = selectedDate,
                            amount = amount,
                            currency = currency
                        )
                        
                        submitExpense(expense)
                    }
                } catch (e: Exception) {
                    progressBar.visibility = View.GONE
                    showError("Error: ${e.message}")
                }
            }
        } else {
            val expense = Expense(
                id = 0,
                subject = subject,
                category = category,
                date = selectedDate,
                amount = amount,
                currency = currency
            )
            
            submitExpense(expense)
        }
    }
    
    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }
    
    private fun submitExpense(expense: Expense) {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken()
                if (token == null) {
                    progressBar.visibility = View.GONE
                    showError("Authentication required")
                    return@launch
                }
                
                Log.d("AddExpenseActivity", "Sending expense with category: ${expense.category}, amount: ${expense.amount}")
                
                val expenseRequest = ExpenseRequest.fromExpense(expense)
                val response = RetrofitClient.expenseApiService.createExpense(
                    expenseRequest,
                    "Bearer $token"
                )
                
                progressBar.visibility = View.GONE
                
                if (response.isSuccessful && response.body() != null) {
                    Toast.makeText(this@AddExpenseActivity, "Expense added successfully", Toast.LENGTH_SHORT).show()
                    
                    val intent = Intent()
                    intent.putExtra("EXPENSE_ADDED", true)
                    intent.putExtra("CATEGORY", expense.category)
                    intent.putExtra("AMOUNT", expense.amount)
                    setResult(RESULT_OK, intent)
                    
                    finish()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                    showError("Server error: $errorMsg")
                    Log.e("AddExpenseActivity", "Server error: $errorMsg")
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                showError("Error: ${e.message}")
                Log.e("AddExpenseActivity", "Exception during API call", e)
            }
        }
    }
}
