package com.example.myalkansyamobile

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myalkansyamobile.adapters.ExpenseAdapter
import com.example.myalkansyamobile.model.Expense
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.utils.SessionManager
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.util.*

class ExpenseActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var expenseAdapter: ExpenseAdapter
    private lateinit var expenseList: MutableList<Expense>
    private lateinit var allExpenseList: MutableList<Expense>
    private lateinit var sessionManager: SessionManager
    
    private lateinit var monthSpinner: Spinner
    private lateinit var yearSpinner: Spinner
    private lateinit var filterButton: Button

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_list)

        // Initialize session manager
        sessionManager = SessionManager(this)

        // Initialize UI elements
        recyclerView = findViewById(R.id.expenseRecyclerView)
        val addExpenseButton = findViewById<Button>(R.id.addExpenseButton)
        val menuButton = findViewById<ImageButton>(R.id.menuButton)
        
        // Initialize filter UI elements
        monthSpinner = findViewById(R.id.monthSpinner)
        yearSpinner = findViewById(R.id.yearSpinner)
        filterButton = findViewById(R.id.filterButton)

        // Setup filter spinners
        setupFilterSpinners()

        // Setup RecyclerView
        expenseList = mutableListOf()
        allExpenseList = mutableListOf()
        expenseAdapter = ExpenseAdapter(expenseList) { expense ->
            // Handle item click - navigate to detail view
            val intent = Intent(this, ExpenseDetailActivity::class.java)
            intent.putExtra("EXPENSE_ID", expense.id)
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = expenseAdapter

        // Load expenses
        loadExpenses()

        // Setup button click listeners
        addExpenseButton.setOnClickListener {
            val intent = Intent(this, AddExpenseActivity::class.java)
            startActivity(intent)
        }

        menuButton.setOnClickListener {
            // Navigate back to home
            finish()
        }
        
        // Setup filter button click listener
        filterButton.setOnClickListener {
            applyFilter()
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupFilterSpinners() {
        // Setup month spinner
        val months = arrayOf(
            "All Months", "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        val monthAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, months)
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        monthSpinner.adapter = monthAdapter
        
        // Setup year spinner
        val currentYear = LocalDate.now().year
        val years = mutableListOf("All Years")
        for (year in currentYear downTo currentYear - 5) {
            years.add(year.toString())
        }
        val yearAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, years)
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        yearSpinner.adapter = yearAdapter
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    private fun applyFilter() {
        val selectedMonth = monthSpinner.selectedItemPosition
        val selectedYear = yearSpinner.selectedItem.toString()
        
        // Reset to show all expenses
        expenseList.clear()
        expenseList.addAll(allExpenseList)
        
        // Apply month filter if not "All Months"
        if (selectedMonth > 0) {
            expenseList.retainAll { expense ->
                expense.date.month == Month.of(selectedMonth)
            }
        }
        
        // Apply year filter if not "All Years"
        if (selectedYear != "All Years") {
            expenseList.retainAll { expense ->
                expense.date.year == selectedYear.toInt()
            }
        }
        
        // Update the adapter
        expenseAdapter.notifyDataSetChanged()
        
        // Show message if no expenses match the filter
        if (expenseList.isEmpty()) {
            Toast.makeText(this, "No expenses found for the selected filter", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadExpenses() {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken()
                if (token == null) {
                    Toast.makeText(this@ExpenseActivity, "Authentication required", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                // Get expenses from API
                val response = RetrofitClient.expenseApiService.getExpenses("Bearer $token")
                
                if (response.isSuccessful) {
                    val expensesList = response.body()
                    if (expensesList != null) {
                        // Clear existing list and add new items
                        expenseList.clear()
                        allExpenseList.clear()
                        expenseList.addAll(expensesList)
                        allExpenseList.addAll(expensesList)
                        expenseAdapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(this@ExpenseActivity, "Received empty response", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@ExpenseActivity, "Failed to load expenses: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    Toast.makeText(this@ExpenseActivity, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
                    sessionManager.clearSession()
                    val intent = Intent(this@ExpenseActivity, SignInActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } else {
                    Toast.makeText(this@ExpenseActivity, "Failed to load expenses: ${e.message()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ExpenseActivity, "Error loading expenses: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh the expense list when coming back to this activity
        loadExpenses()
    }
}
