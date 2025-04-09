package com.example.myalkansyamobile

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myalkansyamobile.model.Expense
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.utils.SessionManager
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

class ExpenseDetailActivity : AppCompatActivity() {
    private lateinit var tvSubject: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvCategory: TextView
    private lateinit var tvAmount: TextView
    private lateinit var btnEdit: Button
    private lateinit var btnBack: Button
    
    private lateinit var sessionManager: SessionManager
    
    private var expenseId: Int = 0
    private lateinit var expense: Expense
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_detail)
        
        // Initialize session manager
        sessionManager = SessionManager(this)
        
        // Get expense ID from intent
        expenseId = intent.getIntExtra("EXPENSE_ID", 0)
        if (expenseId == 0) {
            Toast.makeText(this, "Invalid expense", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Initialize UI elements
        tvSubject = findViewById(R.id.tvSubject)
        tvDate = findViewById(R.id.tvDate)
        tvCategory = findViewById(R.id.tvCategory)
        tvAmount = findViewById(R.id.tvAmount)
        btnEdit = findViewById(R.id.btnEdit)
        btnBack = findViewById(R.id.btnBack)
        
        // Load expense details
        loadExpenseDetails()
        
        // Setup button click listeners
        btnEdit.setOnClickListener {
            val intent = Intent(this, EditExpenseActivity::class.java)
            intent.putExtra("EXPENSE_ID", expenseId)
            startActivity(intent)
        }
        
        btnBack.setOnClickListener {
            finish()
        }
    }
    
    private fun loadExpenseDetails() {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken()
                if (token == null) {
                    Toast.makeText(this@ExpenseDetailActivity, "Authentication required", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }
                
                // Get expense from API
                val response = RetrofitClient.expenseApiService.getExpenseById("Bearer $token", expenseId)
                
                if (response.isSuccessful) {
                    // Extract the expense body from the response
                    val expenseBody = response.body()
                    if (expenseBody != null) {
                        // Use the expense directly
                        expense = expenseBody
                        
                        // Display expense details
                        displayExpenseDetails()
                    } else {
                        Toast.makeText(this@ExpenseDetailActivity, "Received empty response", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this@ExpenseDetailActivity, "Failed to load expense: ${response.code()}", Toast.LENGTH_SHORT).show()
                    finish()
                }
                
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    Toast.makeText(this@ExpenseDetailActivity, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
                    sessionManager.clearSession()
                    val intent = Intent(this@ExpenseDetailActivity, SignInActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } else {
                    Toast.makeText(this@ExpenseDetailActivity, "Failed to load expense details: ${e.message()}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ExpenseDetailActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    
    private fun displayExpenseDetails() {
        tvSubject.text = "Subject: ${expense.subject}"
        tvDate.text = "Date: ${expense.date}"
        tvCategory.text = "Category: ${expense.category}"
        
        // Format amount with currency
        val formattedAmount = NumberFormat.getCurrencyInstance(Locale.US)
            .format(expense.amount)
            .replace("$", "${expense.currency} ")
        tvAmount.text = "Amount: $formattedAmount"
    }
    
    override fun onResume() {
        super.onResume()
        // Reload expense details when coming back to this activity
        if (expenseId != 0) {
            loadExpenseDetails()
        }
    }
}
