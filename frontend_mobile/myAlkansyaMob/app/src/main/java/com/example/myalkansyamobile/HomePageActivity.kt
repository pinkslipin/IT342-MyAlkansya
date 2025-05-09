package com.example.myalkansyamobile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myalkansyamobile.utils.SessionManager
import com.example.myalkansyamobile.databinding.ActivityHomepageBinding
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.http.GET
import retrofit2.http.Header
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.myalkansyamobile.ui.profile.ProfileActivity

// API interfaces
interface UserApiService {
    @GET("api/users/me")
    suspend fun getCurrentUser(@Header("Authorization") token: String): UserResponse
}

interface ExpenseApiService {
    @GET("api/expenses/getExpenses")
    suspend fun getExpenses(@Header("Authorization") token: String): List<ExpenseResponse>
}

interface BudgetApiService {
    @GET("api/budgets/getCurrentMonthBudgets")
    suspend fun getCurrentMonthBudgets(@Header("Authorization") token: String): List<BudgetResponse>
}

// Response data classes
data class UserResponse(
    val userId: Int,
    val firstname: String,
    val lastname: String,
    val email: String,
    val currency: String?,
    val totalSavings: Double
)

data class ExpenseResponse(
    val id: Int,
    val amount: Double,
    val category: String,
    val description: String?,
    val date: String,
    val currency: String = "PHP",
    val subject: String = "",
    val savingsGoalId: Int? = null  // Added field
)

data class BudgetResponse(
    val id: Int,
    val category: String,
    val monthlyBudget: Double,
    val totalSpent: Double = 0.0,
    val budgetMonth: Int,
    val budgetYear: Int
)

class HomePageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomepageBinding
    private lateinit var sessionManager: SessionManager
    private val baseUrl = "https://myalkansya-sia.as.r.appspot.com/" // Production server URL

    private var allExpenses: List<ExpenseResponse> = listOf()
    private var allBudgets: List<BudgetResponse> = listOf()
    private var budgetCategories: List<String> = listOf()
    private var userCurrency: String = "PHP" // Default currency

    private val SETTINGS_REQUEST_CODE = 1001

    // Create API services
    private val userApiService: UserApiService
    private val expenseApiService: ExpenseApiService
    private val budgetApiService: BudgetApiService

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val httpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        userApiService = retrofit.create(UserApiService::class.java)
        expenseApiService = retrofit.create(ExpenseApiService::class.java)
        budgetApiService = retrofit.create(BudgetApiService::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomepageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // Set up the category spinner with default item
        setupCategorySpinner()

        // Fetch and display user data
        fetchUserData()

        // Set up navigation card click listeners
        binding.btnIncome.setOnClickListener {
            navigateTo(IncomeActivity::class.java)
        }

        binding.btnExpense.setOnClickListener {
            navigateTo(ExpenseActivity::class.java)
        }

        binding.btnBudget.setOnClickListener {
            navigateTo(BudgetActivity::class.java)
        }

        binding.btnSavingsGoal.setOnClickListener {
            navigateTo(SavingsGoalsActivity::class.java)
        }

        // Add Currency Converter navigation
        binding.btnCurrencyConverter.setOnClickListener {
            navigateTo(CurrencyConverterActivity::class.java)
        }

        binding.btnLogout.setOnClickListener {
            logoutUser()
        }

        // Add this with the other button click listeners
        binding.imgProfile.setOnClickListener {
            navigateTo(com.example.myalkansyamobile.ui.profile.ProfileActivity::class.java)
        }

        // Add Dashboard navigation with a clearer description
        binding.btnDashboard.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
            Toast.makeText(this, "View detailed financial analytics", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        // Add Dashboard item
        menu.add(Menu.NONE, MENU_DASHBOARD, Menu.NONE, "Analytics Dashboard")
            .setIcon(R.drawable.ic_dashboard) // Add this icon to your drawable resources
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            MENU_DASHBOARD -> {
                startActivity(Intent(this, DashboardActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        
        // Check if stored currency has changed
        val currentCurrency = sessionManager.getCurrency() ?: "PHP"
        if (currentCurrency != userCurrency) {
            Log.d("HomePageActivity", "Currency changed from $userCurrency to $currentCurrency - refreshing data")
            userCurrency = currentCurrency
            fetchUserData()  // Refresh all data with new currency
        } else {
            // Check if the user's name might have changed
            val storedName = sessionManager.getUserName()
            if (storedName != null && !binding.txtWelcomeMessage.text.contains(storedName)) {
                Log.d("HomePageActivity", "User name appears to have changed - refreshing data")
                fetchUserData()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SETTINGS_REQUEST_CODE && resultCode == RESULT_OK) {
            // Currency was changed - reload all data
            fetchUserData()
        }
    }

    private fun setupCategorySpinner() {
        // Set up listener for category selection
        binding.spinnerBudgetCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                updateDisplayForSelectedCategory(parent.getItemAtPosition(position).toString())
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }
    }

    private fun fetchUserData() {
        lifecycleScope.launch {
            try {
                // Changed to use basic validation which only checks user endpoints
                val authToken = try {
                    sessionManager.getBasicValidatedToken()
                } catch (e: Exception) {
                    Log.e("HomePageActivity", "Error validating token", e)
                    null
                }
                
                if (authToken.isNullOrEmpty()) {
                    Toast.makeText(this@HomePageActivity, "Authentication token not found or expired. Please log in again.", Toast.LENGTH_SHORT).show()
                    logoutUser()
                    return@launch
                }

                val bearerToken = "Bearer $authToken"

                try {
                    // Fetch user data
                    val userResponse = withContext(Dispatchers.IO) {
                        userApiService.getCurrentUser(bearerToken)
                    }

                    // Store user currency
                    userCurrency = userResponse.currency ?: "USD"
                    sessionManager.saveCurrency(userCurrency)
                    Log.d("HomePageActivity", "User preferred currency: $userCurrency")

                    // Fetch expenses
                    allExpenses = withContext(Dispatchers.IO) {
                        expenseApiService.getExpenses(bearerToken)
                    }
                    val totalExpenses = allExpenses.sumOf { it.amount }

                    // Fetch budgets
                    allBudgets = withContext(Dispatchers.IO) {
                        budgetApiService.getCurrentMonthBudgets(bearerToken)
                    }
                    val totalBudget = allBudgets.sumOf { it.monthlyBudget }

                    // Extract unique budget categories and populate spinner
                    updateBudgetCategories()

                    // Get total savings from user data
                    val totalSavings = userResponse.totalSavings

                    // Display user data
                    val userName = "${userResponse.firstname} ${userResponse.lastname}".trim()
                    binding.txtWelcomeMessage.text = "Welcome, $userName!"
                    binding.txtEmail.text = userResponse.email

                    // Format and display financial data with the user's preferred currency
                    val formatter = getCurrencyFormatter(userCurrency)
                    binding.txtTotalBudget.text = formatter.format(totalBudget)
                    binding.txtTotalExpenses.text = formatter.format(totalExpenses)
                    binding.txtTotalSavings.text = formatter.format(totalSavings)

                    // Save updated user info to session if needed
                    sessionManager.saveUsername(userName)
                    sessionManager.saveEmail(userResponse.email)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@HomePageActivity, "Error fetching data: ${e.message}", Toast.LENGTH_SHORT).show()

                    // Display whatever user info we have from session
                    val userName = sessionManager.getUserName() ?: "User"
                    userCurrency = sessionManager.getCurrency() ?: "USD"
                    val formatter = getCurrencyFormatter(userCurrency)
                    binding.txtTotalBudget.text = formatter.format(0.0)
                    binding.txtTotalExpenses.text = formatter.format(0.0)
                    binding.txtTotalSavings.text = formatter.format(0.0)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@HomePageActivity, "Failed to load user data", Toast.LENGTH_SHORT).show()
                binding.txtWelcomeMessage.text = "Welcome!"
                binding.txtEmail.text = ""

                userCurrency = sessionManager.getCurrency() ?: "USD"
                val formatter = getCurrencyFormatter(userCurrency)
                binding.txtTotalBudget.text = formatter.format(0.0)
                binding.txtTotalExpenses.text = formatter.format(0.0)
                binding.txtTotalSavings.text = formatter.format(0.0)
            }
        }
    }

    // Helper method to get a formatter for the specified currency
    private fun getCurrencyFormatter(currencyCode: String): NumberFormat {
        val formatter = NumberFormat.getCurrencyInstance()
        try {
            formatter.currency = Currency.getInstance(currencyCode)
        } catch (e: Exception) {
            Log.w("HomePageActivity", "Invalid currency code: $currencyCode, defaulting to USD")
            formatter.currency = Currency.getInstance("USD")
        }
        return formatter
    }

    private fun updateBudgetCategories() {
        // Extract unique categories from budgets
        val categories = allBudgets.map { it.category }.distinct().sorted()
        // Create a new list with "All Categories" as the first option
        budgetCategories = listOf("All Categories") + categories

        // Create an adapter for the spinner
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            budgetCategories
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // Apply the adapter to the spinner
        binding.spinnerBudgetCategory.adapter = adapter
    }

    private fun updateDisplayForSelectedCategory(category: String) {
        val formatter = getCurrencyFormatter(userCurrency)
        // Make sure to update the category label to indicate selected category
        binding.txtCategoryLabel.text = category

        if (category == "All Categories") {
            // Display totals for all categories
            val totalBudget = allBudgets.sumOf { it.monthlyBudget }
            val totalExpenses = allExpenses.sumOf { it.amount }
            binding.txtTotalBudget.text = formatter.format(totalBudget)
            binding.txtTotalExpenses.text = formatter.format(totalExpenses)
            // Hide remaining budget and warning when showing all categories
            binding.txtRemainingBudget.visibility = View.GONE
            binding.txtBudgetWarning.visibility = View.GONE
        } else {
            // Filter budget and expenses for the selected category
            val categoryBudget = allBudgets.find { it.category == category }?.monthlyBudget ?: 0.0
            val categoryExpenses = allExpenses.filter { it.category == category }.sumOf { it.amount }
            binding.txtTotalBudget.text = formatter.format(categoryBudget)
            binding.txtTotalExpenses.text = formatter.format(categoryExpenses)
            // Show remaining budget
            binding.txtRemainingBudget.visibility = View.VISIBLE
            val remaining = categoryBudget - categoryExpenses
            binding.txtRemainingBudget.text = "Remaining: ${formatter.format(remaining)}"

            // Show or hide warning if over budget
            if (categoryExpenses > categoryBudget && categoryBudget > 0) {
                binding.txtBudgetWarning.visibility = View.VISIBLE
                binding.txtBudgetWarning.text = "⚠️ Over budget by ${formatter.format(categoryExpenses - categoryBudget)}"
            } else {
                binding.txtBudgetWarning.visibility = View.GONE
            }
        }
    }

    private fun navigateTo(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
    }

    private fun logoutUser() {
        lifecycleScope.launch {
            sessionManager.clearSession()
            val intent = Intent(this@HomePageActivity, SignInActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    companion object {
        private const val MENU_DASHBOARD = 1001 // Choose any unique ID
    }
}