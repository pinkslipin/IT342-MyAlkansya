package com.example.myalkansyamobile

import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myalkansyamobile.model.Expense
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.api.ExpenseRequest
import com.example.myalkansyamobile.utils.SessionManager
import com.google.gson.JsonSyntaxException
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
    
    private lateinit var sessionManager: SessionManager
    
    private var selectedDate = LocalDate.now()
    private val categories = arrayOf("Food", "Transportation", "Housing", "Utilities", 
        "Entertainment", "Healthcare", "Education", "Shopping", "Other")
    private val currencies = arrayOf("PHP", "USD", "EUR", "GBP", "JPY")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)
        
        // Initialize session manager
        sessionManager = SessionManager(this)
        
        // Initialize UI elements
        etSubject = findViewById(R.id.etSubject)
        tvDate = findViewById(R.id.tvDate)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        etAmount = findViewById(R.id.etAmount)
        spinnerCurrency = findViewById(R.id.spinnerCurrency)
        btnAddExpense = findViewById(R.id.btnAddExpense)
        btnCancel = findViewById(R.id.btnCancel)
        btnPickDate = findViewById(R.id.btnPickDate)
        
        // Setup date display
        tvDate.text = selectedDate.toString()
        
        // Setup category spinner
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spinnerCategory.adapter = categoryAdapter
        
        // Setup currency spinner
        setupCurrencySpinner()
        
        // Setup date picker
        btnPickDate.setOnClickListener {
            showDatePicker()
        }
        
        // Setup add expense button
        btnAddExpense.setOnClickListener {
            addExpense()
        }
        
        // Setup cancel button
        btnCancel.setOnClickListener {
            finish()
        }
    }
    
    private fun setupCurrencySpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCurrency.adapter = adapter
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
        
        // Validation
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
        
        // Create expense object
        val expense = Expense(
            subject = subject,
            category = category,
            date = selectedDate,
            amount = amount,
            currency = spinnerCurrency.selectedItem.toString()  
        )
        
        // Show loading indicator
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Adding expense...")
        progressDialog.setCancelable(false)
        progressDialog.show()
        
        // Send to API
         lifecycleScope.launch {
        try {
            val token = sessionManager.getToken()
            if (token == null) {
                Toast.makeText(this@AddExpenseActivity, "Authentication required", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            
            Log.d("AddExpenseActivity", "Sending expense with category: ${expense.category}, amount: ${expense.amount}")
            
            // Create expense via API using ExpenseRequest for proper serialization
            val expenseRequest = ExpenseRequest.fromExpense(expense)
            val response = RetrofitClient.expenseApiService.createExpense("Bearer $token", expenseRequest)
            
            progressDialog.dismiss()
            
            // Check response body type
            if (response.isSuccessful) {
                Toast.makeText(this@AddExpenseActivity, "Expense added successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Toast.makeText(this@AddExpenseActivity, "Server error: $errorMsg", Toast.LENGTH_LONG).show()
                Log.e("AddExpenseActivity", "Server error: $errorMsg")
            }
            
        } catch (e: JsonSyntaxException) {
            progressDialog.dismiss()
            Log.e("AddExpenseActivity", "JSON parsing error", e)
            Toast.makeText(this@AddExpenseActivity, "Error parsing server response", Toast.LENGTH_SHORT).show()
        } catch (e: HttpException) {
            progressDialog.dismiss()
            // ...existing HttpException handling...
        } catch (e: Exception) {
            progressDialog.dismiss()
            Log.e("AddExpenseActivity", "Exception during API call", e)
            Toast.makeText(this@AddExpenseActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    }
}
