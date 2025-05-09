package com.example.myalkansyamobile

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.time.LocalDate
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
    private lateinit var resetFilterButton: Button
    private lateinit var categorySpinner: Spinner
    private lateinit var progressBar: ProgressBar
    private lateinit var txtEmptyState: TextView
    private lateinit var activeFiltersText: TextView
    
    // Pagination variables
    private var currentPage = 1
    private var totalPages = 1
    private var itemsPerPage = 10
    private lateinit var btnPrevPage: Button
    private lateinit var btnNextPage: Button
    private lateinit var tvPagination: TextView
    
    // Updated categories list matching with backend and web version
    private val categories = arrayOf(
        "All Categories",
        "Food", "Transportation", "Housing", "Utilities", 
        "Entertainment", "Healthcare", "Education", "Shopping", 
        "Personal Care", "Debt Payment", "Savings", "Other"
    )

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_list)

        // Initialize session manager
        sessionManager = SessionManager(this)

        // Initialize UI elements
        initializeUiElements()
        
        // Setup filter spinners
        setupFilterSpinners()

        // Setup RecyclerView
        setupRecyclerView()

        // Load expenses
        loadExpenses()

        // Setup button click listeners
        setupClickListeners()
    }
    
    private fun initializeUiElements() {
        recyclerView = findViewById(R.id.expenseRecyclerView)
        progressBar = findViewById(R.id.progressBar)
        txtEmptyState = findViewById(R.id.txtEmptyState)
        activeFiltersText = findViewById(R.id.activeFiltersText)
        
        // Initialize filter UI elements
        monthSpinner = findViewById(R.id.monthSpinner)
        yearSpinner = findViewById(R.id.yearSpinner)
        filterButton = findViewById(R.id.filterButton)
        resetFilterButton = findViewById(R.id.resetFilterButton)
        categorySpinner = findViewById(R.id.categorySpinner)
        
        // Initialize pagination controls
        btnPrevPage = findViewById(R.id.btnPrevPage)
        btnNextPage = findViewById(R.id.btnNextPage)
        tvPagination = findViewById(R.id.tvPagination)
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
        
        // Default to current month
        val currentMonth = LocalDate.now().monthValue
        monthSpinner.setSelection(currentMonth)
        
        // Setup year spinner
        val currentYear = LocalDate.now().year
        val years = mutableListOf("All Years")
        for (year in currentYear downTo currentYear - 5) {
            years.add(year.toString())
        }
        val yearAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, years)
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        yearSpinner.adapter = yearAdapter
        
        // Setup category spinner
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter
    }
    
    private fun setupRecyclerView() {
        expenseList = mutableListOf()
        allExpenseList = mutableListOf()
        expenseAdapter = ExpenseAdapter(expenseList) { expense ->
            val intent = Intent(this, EditExpenseActivity::class.java)
            intent.putExtra("EXPENSE_ID", expense.id)
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = expenseAdapter
    }
    
    private fun setupClickListeners() {
        findViewById<Button>(R.id.addExpenseButton).setOnClickListener {
            val intent = Intent(this, AddExpenseActivity::class.java)
            startActivityForResult(intent, ADD_EXPENSE_REQUEST_CODE)
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
        
        filterButton.setOnClickListener {
            applyFilter()
        }
        
        resetFilterButton.setOnClickListener {
            resetFilters()
        }
        
        btnPrevPage.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                updatePageDisplay()
            }
        }
        
        btnNextPage.setOnClickListener {
            if (currentPage < totalPages) {
                currentPage++
                updatePageDisplay()
            }
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    private fun applyFilter() {
        val selectedMonth = monthSpinner.selectedItemPosition
        val selectedYear = yearSpinner.selectedItem.toString()
        val selectedCategory = categorySpinner.selectedItem.toString()
        
        // Reset to show all expenses
        expenseList.clear()
        expenseList.addAll(allExpenseList)
        
        // Apply month filter if not "All Months"
        if (selectedMonth > 0) {
            expenseList.retainAll { expense ->
                val expenseMonth = expense.date.monthValue
                expenseMonth == selectedMonth
            }
        }
        
        // Apply year filter if not "All Years"
        if (selectedYear != "All Years") {
            expenseList.retainAll { expense ->
                val expenseYear = expense.date.year
                expenseYear == selectedYear.toInt()
            }
        }
        
        // Apply category filter if not "All Categories"
        if (selectedCategory != "All Categories") {
            expenseList.retainAll { expense ->
                expense.category == selectedCategory
            }
        }
        
        // Update active filters text
        updateActiveFiltersText(selectedMonth, selectedYear, selectedCategory)
        
        // Reset pagination
        currentPage = 1
        calculateTotalPages()
        updatePageDisplay()
        
        // Update UI
        updateEmptyState()
    }
    
    private fun updateActiveFiltersText(monthPosition: Int, year: String, category: String) {
        val activeFilters = mutableListOf<String>()
        
        if (monthPosition > 0) {
            val monthName = monthSpinner.getItemAtPosition(monthPosition).toString()
            activeFilters.add("Month: $monthName")
        }
        
        if (year != "All Years") {
            activeFilters.add("Year: $year")
        }
        
        if (category != "All Categories") {
            activeFilters.add("Category: $category")
        }
        
        if (activeFilters.isNotEmpty()) {
            val filterText = getString(R.string.active_filters, activeFilters.joinToString(", "))
            activeFiltersText.text = filterText
            activeFiltersText.visibility = View.VISIBLE
        } else {
            activeFiltersText.visibility = View.GONE
        }
    }
    
    private fun resetFilters() {
        monthSpinner.setSelection(0)
        yearSpinner.setSelection(0)
        categorySpinner.setSelection(0)
        
        activeFiltersText.visibility = View.GONE
        
        // Reset list to show all expenses
        expenseList.clear()
        expenseList.addAll(allExpenseList)
        
        // Reset pagination
        currentPage = 1
        calculateTotalPages()
        updatePageDisplay()
        
        // Update UI
        updateEmptyState()
    }
    
    private fun calculateTotalPages() {
        totalPages = if (expenseList.isEmpty()) 1 else (expenseList.size + itemsPerPage - 1) / itemsPerPage
    }
    
    private fun updatePageDisplay() {
        tvPagination.text = "$currentPage out of $totalPages"
        btnPrevPage.isEnabled = currentPage > 1
        btnNextPage.isEnabled = currentPage < totalPages
        
        // Display the current page of items
        displayCurrentPage()
    }
    
    private fun displayCurrentPage() {
        val start = (currentPage - 1) * itemsPerPage
        val end = minOf(start + itemsPerPage, expenseList.size)
        
        val currentPageItems = if (expenseList.isNotEmpty() && start < expenseList.size) {
            expenseList.subList(start, end)
        } else {
            emptyList()
        }
        
        // Update adapter with current page items
        expenseAdapter.updateList(currentPageItems)
    }
    
    private fun updateEmptyState() {
        if (expenseList.isEmpty()) {
            recyclerView.visibility = View.GONE
            txtEmptyState.visibility = View.VISIBLE
            
            // Set appropriate message based on whether filters are applied
            if (monthSpinner.selectedItemPosition > 0 || 
                yearSpinner.selectedItem.toString() != "All Years" ||
                categorySpinner.selectedItem.toString() != "All Categories") {
                txtEmptyState.text = getString(R.string.no_filtered_expense)
            } else {
                txtEmptyState.text = getString(R.string.no_expense_records)
            }
        } else {
            recyclerView.visibility = View.VISIBLE
            txtEmptyState.visibility = View.GONE
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

                // Show loading indicator
                progressBar.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
                txtEmptyState.visibility = View.GONE

                // Get expenses from API with proper error handling
                val responseList = withContext(Dispatchers.IO) {
                    RetrofitClient.expenseApiService.getExpenses("Bearer $token")
                }
                
                // Map API responses to our Expense model
                val expenses = responseList.map { response ->
                    Expense(
                        id = response.id,
                        subject = response.subject,
                        category = response.category,
                        date = LocalDate.parse(response.date),
                        amount = response.amount,
                        currency = response.currency ?: "PHP",
                        savingsGoalId = response.savingsGoalId
                    )
                }
                
                // Sort by date (most recent first)
                val sortedExpenses = expenses.sortedByDescending { it.date }
                
                // Update lists
                allExpenseList = sortedExpenses.toMutableList()
                expenseList = sortedExpenses.toMutableList()
                
                // Setup pagination
                currentPage = 1
                calculateTotalPages()
                updatePageDisplay()
                
                // Hide loading indicator
                progressBar.visibility = View.GONE
                
                // Update UI based on results
                updateEmptyState()
                
            } catch (e: HttpException) {
                // Handle HTTP errors
                handleHttpException(e)
            } catch (e: Exception) {
                // Handle generic errors
                handleGenericException(e)
            }
        }
    }
    
    private fun handleHttpException(e: HttpException) {
        progressBar.visibility = View.GONE
        recyclerView.visibility = View.GONE
        txtEmptyState.visibility = View.VISIBLE
        txtEmptyState.text = "Error loading expenses"
        
        if (e.code() == 401) {
            Toast.makeText(this@ExpenseActivity, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
            sessionManager.clearSession()
            val intent = Intent(this@ExpenseActivity, SignInActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        } else {
            Toast.makeText(this@ExpenseActivity, "Failed to load expenses: ${e.message()}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun handleGenericException(e: Exception) {
        progressBar.visibility = View.GONE
        recyclerView.visibility = View.GONE
        txtEmptyState.visibility = View.VISIBLE
        txtEmptyState.text = "Error loading expenses"
        
        Toast.makeText(this@ExpenseActivity, "Error loading expenses: ${e.message}", Toast.LENGTH_SHORT).show()
        e.printStackTrace()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        // Refresh the expense list when coming back to this activity
        loadExpenses()
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == ADD_EXPENSE_REQUEST_CODE && resultCode == RESULT_OK) {
            // Reload expenses after adding a new one
            loadExpenses()
            
            // Show success message
            Toast.makeText(this, "Expense added successfully", Toast.LENGTH_SHORT).show()
        }
    }
    
    companion object {
        const val ADD_EXPENSE_REQUEST_CODE = 101
    }
}
