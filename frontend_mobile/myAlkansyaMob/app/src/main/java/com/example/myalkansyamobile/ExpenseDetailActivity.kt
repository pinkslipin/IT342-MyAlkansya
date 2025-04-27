package com.example.myalkansyamobile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myalkansyamobile.model.Expense
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class ExpenseDetailActivity : AppCompatActivity() {
    private lateinit var tvSubject: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvCategory: TextView
    private lateinit var tvAmount: TextView
    private lateinit var btnEdit: Button
    private lateinit var btnBack: Button
    private lateinit var btnDelete: Button
    
    private lateinit var sessionManager: SessionManager
    
    private var expenseId: Int = 0
    private lateinit var expense: Expense
    
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    
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
        btnDelete = findViewById(R.id.btnDelete)
        
        // Load expense details
        loadExpenseDetails()
        
        // Setup button click listeners
        btnEdit.setOnClickListener {
            val intent = Intent(this, EditExpenseActivity::class.java)
            intent.putExtra("EXPENSE_ID", expenseId)
            startActivityForResult(intent, EDIT_EXPENSE_REQUEST_CODE)
        }
        
        btnBack.setOnClickListener {
            finish()
        }
        
        btnDelete.setOnClickListener {
            showDeleteConfirmationDialog()
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
                
                // Get expense from API using the direct return type
                val expenseResponse = withContext(Dispatchers.IO) {
                    RetrofitClient.expenseApiService.getExpenseById(expenseId, "Bearer $token")
                }
                
                // Convert to Expense model
                expense = Expense(
                    id = expenseResponse.id,
                    subject = expenseResponse.subject,
                    category = expenseResponse.category,
                    date = LocalDate.parse(expenseResponse.date),
                    amount = expenseResponse.amount,
                    currency = expenseResponse.currency
                )
                
                // Display expense details
                displayExpenseDetails()
                
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
        tvSubject.text = expense.subject
        
        // Format date for better display
        val formattedDate = expense.date.format(dateFormatter)
        tvDate.text = formattedDate
        tvCategory.text = expense.category
        
        // Format amount with currency
        val formattedAmount = NumberFormat.getCurrencyInstance(Locale.getDefault())
            .format(expense.amount)
            .replace("$", "${expense.currency} ")
        tvAmount.text = formattedAmount
    }
    
    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Expense")
            .setMessage("Are you sure you want to delete this expense? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteExpense()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteExpense() {
        // Show a loading dialog first
        val loadingDialog = AlertDialog.Builder(this)
            .setTitle("Deleting Expense")
            .setMessage("Please wait...")
            .setCancelable(false)
            .create()
        loadingDialog.show()
        
        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken()
                if (token == null) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@ExpenseDetailActivity, "Authentication required", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.expenseApiService.deleteExpense(expenseId, "Bearer $token")
                }
                
                // Immediately dismiss the dialog
                loadingDialog.dismiss()
                
                if (response.isSuccessful) {
                    // Show success toast
                    Toast.makeText(this@ExpenseDetailActivity, "Expense deleted successfully", Toast.LENGTH_SHORT).show()
                    
                    // Set result and immediately finish the activity
                    setResult(RESULT_OK)
                    finish()
                } else {
                    // Handle error case with better message extraction
                    try {
                        val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                        Toast.makeText(this@ExpenseDetailActivity, "Error: $errorMsg", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@ExpenseDetailActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                
            } catch (e: Exception) {
                loadingDialog.dismiss()
                Toast.makeText(this@ExpenseDetailActivity, "Error deleting expense: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == EDIT_EXPENSE_REQUEST_CODE && resultCode == RESULT_OK) {
            loadExpenseDetails()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Only reload expense details if we didn't just delete the expense
        // and if we have a valid expense ID
        if (expenseId != 0) {
            // We need to be careful not to try loading a deleted expense
            try {
                loadExpenseDetails()
            } catch (e: Exception) {
                // If this fails, don't crash - just finish the activity
                Log.e("ExpenseDetailActivity", "Error loading expense details: ${e.message}")
                finish()
            }
        }
    }
    
    companion object {
        const val EDIT_EXPENSE_REQUEST_CODE = 201
    }
}
