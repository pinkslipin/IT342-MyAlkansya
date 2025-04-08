package com.example.myalkansyamobile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myalkansyamobile.utils.SessionManager
import com.example.myalkansyamobile.databinding.ActivityHomepageBinding
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

class HomePageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomepageBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomepageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // Fetch and display user data
        fetchUserData()

        // Set up button click listeners
        binding.btnIncome.setOnClickListener {
            navigateTo(IncomeActivity::class.java)
        }

//        binding.btnExpense.setOnClickListener {
//            navigateTo(ExpenseActivity::class.java)
//        }
//
//        binding.btnBudget.setOnClickListener {
//            navigateTo(BudgetActivity::class.java)
//        }
//
//        binding.btnSavingsGoal.setOnClickListener {
//            navigateTo(SavingsGoalActivity::class.java)
//        }
//
        binding.btnLogout.setOnClickListener {
            logoutUser()
        }
    }

    private fun fetchUserData() {
        lifecycleScope.launch {
            try {
                // Get user info from session manager
                val userName = sessionManager.getUserName() ?: "User"
                val userEmail = sessionManager.getUserEmail() ?: ""
                
                // Try to fetch updated user data including financial information
                val userId = sessionManager.getUserId()
                if (userId != null) {
                    try {
                        // For now, show zero as default since we don't have actual data
                        val totalSavings = 0.0
                        
                        // Display user data
                        binding.txtWelcomeMessage.text = "Welcome, $userName!"
                        binding.txtEmail.text = userEmail

                        // Format and display total savings
                        val formattedSavings = NumberFormat.getCurrencyInstance(Locale.US).format(totalSavings)
                        binding.txtTotalSavings.text = formattedSavings
                    } catch (e: Exception) {
                        // If API call fails, still show basic user info
                        binding.txtWelcomeMessage.text = "Welcome, $userName!"
                        binding.txtEmail.text = userEmail
                        binding.txtTotalSavings.text = NumberFormat.getCurrencyInstance(Locale.US).format(0.0)
                        Toast.makeText(this@HomePageActivity, "Failed to load financial data", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Fallback if user ID is not available
                    binding.txtWelcomeMessage.text = "Welcome, $userName!"
                    binding.txtEmail.text = userEmail
                    binding.txtTotalSavings.text = NumberFormat.getCurrencyInstance(Locale.US).format(0.0)
                }
            } catch (e: Exception) {
                Toast.makeText(this@HomePageActivity, "Failed to load user data", Toast.LENGTH_SHORT).show()
                // Set default values if we can't get user data
                binding.txtWelcomeMessage.text = "Welcome!"
                binding.txtEmail.text = ""
                binding.txtTotalSavings.text = NumberFormat.getCurrencyInstance(Locale.US).format(0.0)
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
}