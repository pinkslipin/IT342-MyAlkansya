package com.example.myalkansyamobile

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myalkansyamobile.model.Expense
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.api.ExpenseRequest
import com.example.myalkansyamobile.utils.SessionManager
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.LocalDate
import java.util.*

class EditExpenseActivity : AppCompatActivity() {
    private lateinit var etSubject: EditText
    private lateinit var tvDate: TextView
    private lateinit var spinnerCategory: Spinner
    private lateinit var etAmount: EditText
    private lateinit var spinnerCurrency: Spinner
    private lateinit var btnPickDate: ImageView
    private lateinit var btnSaveChanges: Button
    private lateinit var btnCancel: Button
    private lateinit var progressBar: ProgressBar
    
    private lateinit var sessionManager: SessionManager
    
    private var expenseId: Int = 0
    private var selectedDate = LocalDate.now()
    private val categories = arrayOf("Food", "Transportation", "Housing", "Utilities", 
        "Entertainment", "Healthcare", "Education", "Shopping", "Other")
    private val currencies = arrayOf("PHP", "USD", "EUR", "GBP", "JPY")
    private var selectedCurrency = "PHP"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_expense)
        
        // Initialize session manager
        sessionManager = SessionManager(this)
        
        // Add error handling for findViewById operations
        try {
            // Initialize UI elements
            etSubject = findViewById(R.id.etSubjectEdit)
            tvDate = findViewById(R.id.tvDateEdit)
            spinnerCategory = findViewById(R.id.spinnerCategoryEdit)
            etAmount = findViewById(R.id.etAmountEdit)
            
            // Try to find the currency spinner with possible different IDs
            spinnerCurrency = findViewById(R.id.spinnerCurrencyEdit) 
                ?: findViewById(R.id.spinnerCurrency) 
                ?: throw NullPointerException("Currency spinner not found in layout")
            
            btnPickDate = findViewById(R.id.btnPickDateEdit)
            btnSaveChanges = findViewById(R.id.btnSaveExpense)
            btnCancel = findViewById(R.id.btnCancelEdit)
            
            // Add the progress bar programmatically since it's missing from the XML
            progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
            progressBar.visibility = View.GONE
            
            val layout = findViewById<LinearLayout>(R.id.editExpenseLayout) // You'll need to add this ID to the root LinearLayout in XML
            layout.addView(progressBar, 0)

        } catch (e: Exception) {
            Log.e("EditExpenseActivity", "Error finding views: ${e.message}")
            Toast.makeText(this, "Failed to initialize UI: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        // Get expense ID from intent
        expenseId = intent.getIntExtra("EXPENSE_ID", 0)
        if (expenseId == 0) {
            Toast.makeText(this, "Invalid expense ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Setup category spinner
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spinnerCategory.adapter = categoryAdapter
        
        // Setup currency spinner - now safely after view initialization
        setupCurrencySpinner()
        
        // Setup date picker
        btnPickDate.setOnClickListener {
            showDatePicker()
        }
        
        // Load expense details
        loadExpenseDetails()
        
        // Setup button click listeners
        btnSaveChanges.setOnClickListener {
            updateExpense()
        }
        
        btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun setupCurrencySpinner() {
        try {
            val currencyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencies)
            currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCurrency.adapter = currencyAdapter
        } catch (e: Exception) {
            Log.e("EditExpenseActivity", "Error setting up currency spinner: ${e.message}")
            // Non-fatal error, app can still work with default selection
        }
    }

    // New data class to represent a date for pre-API 26 devices
    data class DateCompat(val year: Int, val month: Int, val day: Int) {
        override fun toString(): String {
            return String.format("%04d-%02d-%02d", year, month, day)
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()

        // Handle API compatibility for LocalDate
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // API level 26+ implementation
            calendar.set(Calendar.YEAR, selectedDate.year)
            calendar.set(Calendar.MONTH, selectedDate.monthValue - 1)
            calendar.set(Calendar.DAY_OF_MONTH, selectedDate.dayOfMonth)
        } else {
            // Pre-API level 26 implementation
            // Extract date components manually using string parsing
            val dateParts = selectedDate.toString().split("-")
            if (dateParts.size == 3) {
                try {
                    val year = dateParts[0].toInt()
                    val month = dateParts[1].toInt() - 1
                    val day = dateParts[2].toInt()
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, day)
                } catch (e: Exception) {
                    Log.e("EditExpenseActivity", "Error parsing date: ${e.message}")
                    // Use current date as fallback
                }
            }
        }

        val datePickerDialog = DatePickerDialog(this,
            { _, year, month, day ->
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    // API level 26+ implementation
                    selectedDate = LocalDate.of(year, month + 1, day)
                } else {
                    // Pre-API level 26 implementation
                    // Create a string representation that can be parsed later
                    selectedDate = parseLocalDateCompat(year, month + 1, day)
                }
                tvDate.text = selectedDate.toString()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    // Helper function to manage the DateCompat class for pre-API 26
    private fun parseLocalDateCompat(year: Int, month: Int, day: Int): LocalDate {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            return LocalDate.of(year, month, day)
        } else {
            // Creates a string that will be parsed in the loadExpenseDetails
            return LocalDate.parse(String.format("%04d-%02d-%02d", year, month, day))
        }
    }
    
    private fun loadExpenseDetails() {
        progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken()
                if (token == null) {
                    Toast.makeText(this@EditExpenseActivity, "Authentication required", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }
                
                val response = RetrofitClient.expenseApiService.getExpenseById("Bearer $token", expenseId)
                
                if (response.isSuccessful) {
                    // Extract the expense body from the response
                    val expenseBody = response.body()
                    if (expenseBody != null) {
                        // Use the expense directly since it's already an Expense object
                        etSubject.setText(expenseBody.subject)
                        tvDate.text = expenseBody.date.toString()
                        selectedDate = expenseBody.date
                        etAmount.setText(expenseBody.amount.toString())
                        
                        val categoryPosition = categories.indexOf(expenseBody.category)
                        if (categoryPosition >= 0) {
                            spinnerCategory.setSelection(categoryPosition)
                        }
                        
                        selectedCurrency = expenseBody.currency
                        try {
                            val currencyPosition = currencies.indexOf(expenseBody.currency)
                            if (currencyPosition >= 0) {
                                spinnerCurrency.setSelection(currencyPosition)
                            }
                        } catch (e: Exception) {
                            Log.e("EditExpenseActivity", "Error setting currency: ${e.message}")
                        }
                    } else {
                        Toast.makeText(this@EditExpenseActivity, "Received empty response", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this@EditExpenseActivity, "Failed to load expense: ${response.code()}", Toast.LENGTH_SHORT).show()
                    finish()
                }
                
                progressBar.visibility = View.GONE
                
            } catch (e: HttpException) {
                progressBar.visibility = View.GONE
                if (e.code() == 401) {
                    Toast.makeText(this@EditExpenseActivity, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
                    sessionManager.clearSession()
                    finish()
                } else {
                    Toast.makeText(this@EditExpenseActivity, "Failed to load expense details: ${e.message()}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@EditExpenseActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    
    private fun updateExpense() {
        val subject = etSubject.text.toString().trim()
        val category = spinnerCategory.selectedItem.toString()
        val amountString = etAmount.text.toString().trim()
        val currency = try {
            spinnerCurrency.selectedItem?.toString() ?: "PHP"  // Default to PHP if null
        } catch (e: Exception) {
            Log.e("EditExpenseActivity", "Error getting selected currency: ${e.message}")
            "PHP"  // Default to PHP on error
        }
        
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
        
        val updatedExpense = Expense(
            id = expenseId,
            subject = subject,
            category = category,
            date = selectedDate,
            amount = amount,
            currency = currency
        )
        
        progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken()
                if (token == null) {
                    Toast.makeText(this@EditExpenseActivity, "Authentication required", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                    finish()
                    return@launch
                }
                
                val expenseRequest = ExpenseRequest.fromExpense(updatedExpense)
                RetrofitClient.expenseApiService.updateExpense("Bearer $token", expenseId, expenseRequest)
                
                progressBar.visibility = View.GONE
                Toast.makeText(this@EditExpenseActivity, "Expense updated successfully", Toast.LENGTH_SHORT).show()
                finish()
                
            } catch (e: HttpException) {
                progressBar.visibility = View.GONE
                if (e.code() == 401) {
                    Toast.makeText(this@EditExpenseActivity, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
                    sessionManager.clearSession()
                    finish()
                } else {
                    Toast.makeText(this@EditExpenseActivity, "Failed to update expense: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@EditExpenseActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
