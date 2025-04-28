package com.example.myalkansyamobile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myalkansyamobile.api.SavingsGoalResponse
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myalkansyamobile.adapters.SavingsGoalAdapter
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.databinding.ActivitySavingsGoalsBinding
import com.example.myalkansyamobile.model.SavingsGoal
import com.example.myalkansyamobile.utils.CurrencyUtils
import com.example.myalkansyamobile.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SavingsGoalsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySavingsGoalsBinding
    private lateinit var adapter: SavingsGoalAdapter
    private lateinit var sessionManager: SessionManager
    private var userDefaultCurrency = "PHP"
    
    private val originalGoalsList = mutableListOf<SavingsGoalResponse>()
    private val displayGoalsList = mutableListOf<SavingsGoal>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavingsGoalsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        sessionManager = SessionManager(this)
        userDefaultCurrency = sessionManager.getCurrency() ?: "PHP"
        
        setupRecyclerView()
        setupClickListeners()
        fetchSavingsGoals()
    }
    
    private fun setupRecyclerView() {
        adapter = SavingsGoalAdapter(
            displayGoalsList,
            { goal -> // Click listener for goal item
                val intent = Intent(this, EditSavingsGoalActivity::class.java)
                intent.putExtra(EditSavingsGoalActivity.EXTRA_GOAL_ID, goal.id)
                startActivity(intent)
            }
        )
        binding.recyclerSavingsGoals.layoutManager = LinearLayoutManager(this)
        binding.recyclerSavingsGoals.adapter = adapter
    }
    
    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        binding.btnAddSavingsGoal.setOnClickListener {
            startActivity(Intent(this, AddSavingsGoalActivity::class.java))
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // Check if currency preference changed
        val currentCurrency = sessionManager.getCurrency() ?: "PHP"
        if (currentCurrency != userDefaultCurrency) {
            userDefaultCurrency = currentCurrency
            // Re-fetch and convert data with new currency
            fetchSavingsGoals()
        } else {
            // Just refresh the list
            fetchSavingsGoals()
        }
    }
    
    private fun fetchSavingsGoals() {
        val token = sessionManager.fetchAuthToken()
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }
        
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.savingsGoalApiService.getAllSavingsGoals("Bearer $token")
                originalGoalsList.clear()
                originalGoalsList.addAll(response)
                
                // Process goals for display with correct currency
                processGoalsForDisplay(token)
                
                updateEmptyState()
            } catch (e: Exception) {
                Toast.makeText(this@SavingsGoalsActivity, "Failed to load savings goals: ${e.message}", Toast.LENGTH_SHORT).show()
                updateEmptyState()
            }
        }
    }
    
    private suspend fun processGoalsForDisplay(token: String) {
        displayGoalsList.clear()
        
        try {
            for (apiGoal in originalGoalsList) {
                val targetDate = try {
                    dateFormat.parse(apiGoal.targetDate) ?: Date()
                } catch (e: Exception) {
                    Date()
                }
                
                // Check if we need to convert currencies
                if (apiGoal.currency != userDefaultCurrency) {
                    try {
                        // Try to convert amounts
                        val convertedTargetAmount = CurrencyUtils.convertCurrency(
                            apiGoal.targetAmount,
                            apiGoal.currency,
                            userDefaultCurrency,
                            token
                        )
                        
                        val convertedCurrentAmount = CurrencyUtils.convertCurrency(
                            apiGoal.currentAmount,
                            apiGoal.currency,
                            userDefaultCurrency,
                            token
                        )
                        
                        if (convertedTargetAmount != null && convertedCurrentAmount != null) {
                            // Use converted amounts with original values preserved
                            val trueOriginalTargetAmount = apiGoal.originalTargetAmount ?: apiGoal.targetAmount
                            val trueOriginalCurrentAmount = apiGoal.originalCurrentAmount ?: apiGoal.currentAmount
                            val trueOriginalCurrency = apiGoal.originalCurrency ?: apiGoal.currency
                            
                            val goal = SavingsGoal(
                                id = apiGoal.id,
                                goal = apiGoal.goal,
                                targetAmount = convertedTargetAmount,
                                currentAmount = convertedCurrentAmount,
                                targetDate = targetDate,
                                currency = userDefaultCurrency,
                                originalTargetAmount = trueOriginalTargetAmount,
                                originalCurrentAmount = trueOriginalCurrentAmount,
                                originalCurrency = trueOriginalCurrency
                            )
                            displayGoalsList.add(goal)
                        } else {
                            // Conversion failed, use original
                            val goal = SavingsGoal(
                                id = apiGoal.id,
                                goal = apiGoal.goal,
                                targetAmount = apiGoal.targetAmount,
                                currentAmount = apiGoal.currentAmount,
                                targetDate = targetDate,
                                currency = apiGoal.currency,
                                originalTargetAmount = apiGoal.originalTargetAmount,
                                originalCurrentAmount = apiGoal.originalCurrentAmount,
                                originalCurrency = apiGoal.originalCurrency
                            )
                            displayGoalsList.add(goal)
                        }
                    } catch (e: Exception) {
                        Log.e("SavingsGoalsActivity", "Currency conversion error: ${e.message}")
                        // On error, use original
                        val goal = SavingsGoal(
                            id = apiGoal.id,
                            goal = apiGoal.goal,
                            targetAmount = apiGoal.targetAmount,
                            currentAmount = apiGoal.currentAmount,
                            targetDate = targetDate,
                            currency = apiGoal.currency,
                            originalTargetAmount = apiGoal.originalTargetAmount,
                            originalCurrentAmount = apiGoal.originalCurrentAmount,
                            originalCurrency = apiGoal.originalCurrency
                        )
                        displayGoalsList.add(goal)
                    }
                } else {
                    // No conversion needed
                    val goal = SavingsGoal(
                        id = apiGoal.id,
                        goal = apiGoal.goal,
                        targetAmount = apiGoal.targetAmount,
                        currentAmount = apiGoal.currentAmount,
                        targetDate = targetDate,
                        currency = apiGoal.currency,
                        originalTargetAmount = apiGoal.originalTargetAmount,
                        originalCurrentAmount = apiGoal.originalCurrentAmount,
                        originalCurrency = apiGoal.originalCurrency
                    )
                    displayGoalsList.add(goal)
                }
            }
            
            adapter.notifyDataSetChanged()
        } catch (e: Exception) {
            Toast.makeText(this@SavingsGoalsActivity, "Error processing goals: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateEmptyState() {
        if (displayGoalsList.isEmpty()) {
            binding.emptyStateCard.visibility = View.VISIBLE
            binding.recyclerSavingsGoals.visibility = View.GONE
        } else {
            binding.emptyStateCard.visibility = View.GONE
            binding.recyclerSavingsGoals.visibility = View.VISIBLE
        }
    }
}
