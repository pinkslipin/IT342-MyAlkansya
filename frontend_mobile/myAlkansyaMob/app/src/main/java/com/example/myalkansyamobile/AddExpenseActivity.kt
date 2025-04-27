package com.example.myalkansyamobile

import android.app.DatePickerDialog
import android.app.ProgressDialog
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
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.HttpException
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
    
    private val currencies = CurrencyUtils.currencyCodes.toTypedArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)
        
        sessionManager = SessionManager(this)
        userDefaultCurrency = sessionManager.getCurrency() ?: "PHP"
        
        etSubject = findViewById(R.id.etSubject)
        tvDate = findViewById(R.id.tvDate)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        etAmount = findViewById(R.id.etAmount)
        spinnerCurrency = findViewById(R.id.spinnerCurrency)
        btnAddExpense = findViewById(R.id.btnAddExpense)
        btnCancel = findViewById(R.id.btnCancel)
        btnPickDate = findViewById(R.id.btnPickDate)
        
        tvConversionInfo = findViewById(R.id.tvConversionInfo) ?: TextView(this).also { it.visibility = View.GONE }
        tvCurrencyWarning = findViewById(R.id.tvCurrencyWarning) ?: TextView(this).also { it.visibility = View.GONE }
        
        tvDate.text = selectedDate.toString()
        
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spinnerCategory.adapter = categoryAdapter
        
        setupCurrencySpinner()
        setupAmountListener()
        
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
                    if (tvConversionInfo != null) {
                        tvConversionInfo.visibility = View.GONE
                    }
                } else {
                    convertAmount(currentAmount, currentCurrency, selectedCurrency)
                }
            }
        }
        
        updateConversionNotification(newCurrency)
    }
    
    private fun updateConversionNotification(selectedCurrency: String) {
        if (tvCurrencyWarning != null) {
            if (selectedCurrency != userDefaultCurrency) {
                tvCurrencyWarning.text = 
                    "Note: This expense will be automatically converted to $userDefaultCurrency when saved."
                tvCurrencyWarning.visibility = View.VISIBLE
            } else {
                tvCurrencyWarning.visibility = View.GONE
            }
        }
    }
    
    private fun convertAmount(amount: Double, fromCurrency: String, toCurrency: String) {
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Converting currency...")
        progressDialog.setCancelable(false)
        progressDialog.show()
        
        conversionJob?.cancel()
        
        conversionJob = lifecycleScope.launch {
            try {
                val authToken = sessionManager.getToken()
                if (authToken.isNullOrEmpty()) {
                    progressDialog.dismiss()
                    Toast.makeText(this@AddExpenseActivity, "Error: Not logged in", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                val convertedAmount = CurrencyUtils.convertCurrency(
                    amount, 
                    fromCurrency, 
                    toCurrency, 
                    authToken
                )
                
                progressDialog.dismiss()
                
                if (convertedAmount != null) {
                    etAmount.setText(String.format("%.2f", convertedAmount))
                    Toast.makeText(
                        this@AddExpenseActivity,
                        "Converted ${CurrencyUtils.formatAmount(amount)} $fromCurrency to ${CurrencyUtils.formatAmount(convertedAmount)} $toCurrency",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@AddExpenseActivity,
                        "Conversion failed. Please enter amount manually.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(this@AddExpenseActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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
        val category = spinnerCategory.selectedItem.toString()
        val amountString = etAmount.text.toString().trim()
        val currency = getSelectedCurrency()
        
        if (subject.isEmpty()) {
            etSubject.error = "Subject cannot be empty"
            return
        }
        
        if (amountString.isEmpty()) {
            etAmount.error = "Amount cannot be empty"
            return
        }
        
        val amount = try {
            amountString.toDouble()
        } catch (e: NumberFormatException) {
            etAmount.error = "Invalid amount format"
            return
        }
        
        if (amount <= 0) {
            etAmount.error = "Amount must be greater than zero"
            return
        }
        
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Adding expense...")
        progressDialog.setCancelable(false)
        progressDialog.show()
        
        if (currency != userDefaultCurrency) {
            lifecycleScope.launch {
                try {
                    val token = sessionManager.getToken()
                    if (token == null) {
                        progressDialog.dismiss()
                        Toast.makeText(this@AddExpenseActivity, "Authentication required", Toast.LENGTH_SHORT).show()
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
                        
                        submitExpense(expense, progressDialog)
                    } else {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@AddExpenseActivity,
                            "Currency conversion failed. Using original values.",
                            Toast.LENGTH_LONG
                        ).show()
                        
                        val expense = Expense(
                            id = 0,
                            subject = subject,
                            category = category,
                            date = selectedDate,
                            amount = amount,
                            currency = currency
                        )
                        
                        submitExpense(expense, progressDialog)
                    }
                } catch (e: Exception) {
                    progressDialog.dismiss()
                    Toast.makeText(this@AddExpenseActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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
            
            submitExpense(expense, progressDialog)
        }
    }
    
    private fun submitExpense(expense: Expense, progressDialog: ProgressDialog) {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken()
                if (token == null) {
                    progressDialog.dismiss()
                    Toast.makeText(this@AddExpenseActivity, "Authentication required", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                Log.d("AddExpenseActivity", "Sending expense with category: ${expense.category}, amount: ${expense.amount}")
                
                val expenseRequest = ExpenseRequest.fromExpense(expense)
                val response = RetrofitClient.expenseApiService.createExpense("Bearer $token", expenseRequest)
                
                progressDialog.dismiss()
                
                if (response.isSuccessful) {
                    Toast.makeText(this@AddExpenseActivity, "Expense added successfully", Toast.LENGTH_SHORT).show()
                    
                    val intent = Intent()
                    intent.putExtra("EXPENSE_ADDED", true)
                    intent.putExtra("CATEGORY", expense.category)
                    intent.putExtra("AMOUNT", expense.amount)
                    setResult(RESULT_OK, intent)
                    
                    finish()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                    Toast.makeText(this@AddExpenseActivity, "Server error: $errorMsg", Toast.LENGTH_LONG).show()
                    Log.e("AddExpenseActivity", "Server error: $errorMsg")
                }
            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(this@AddExpenseActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("AddExpenseActivity", "Exception during API call", e)
            }
        }
    }
}
