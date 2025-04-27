package com.example.myalkansyamobile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myalkansyamobile.adapters.IncomeAdapter
import com.example.myalkansyamobile.api.IncomeRepository
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.databinding.ActivityIncomeBinding
import com.example.myalkansyamobile.model.Income
import com.example.myalkansyamobile.utils.CurrencyUtils
import com.example.myalkansyamobile.utils.Resource
import com.example.myalkansyamobile.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext   

class IncomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityIncomeBinding
    private lateinit var incomeAdapter: IncomeAdapter
    private val incomeList = mutableListOf<Income>()
    private val displayIncomeList = mutableListOf<Income>()
    private lateinit var sessionManager: SessionManager
    private lateinit var incomeRepository: IncomeRepository
    private var userDefaultCurrency: String = "PHP"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIncomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        incomeRepository = IncomeRepository(RetrofitClient.incomeApiService)
        userDefaultCurrency = sessionManager.getCurrency() ?: "PHP"

        setupRecyclerView()
        fetchIncomes()

        binding.btnAddIncome.setOnClickListener {
            val intent = Intent(this, AddIncomeActivity::class.java)
            startActivity(intent)
        }
        
        // Add menu navigation
        binding.topBar.setOnClickListener {
            val intent = Intent(this, HomePageActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Check if currency preference changed
        val currentCurrency = sessionManager.getCurrency() ?: "PHP"
        if (currentCurrency != userDefaultCurrency) {
            userDefaultCurrency = currentCurrency
            // Update adapter with new currency
            incomeAdapter.updateDefaultCurrency(userDefaultCurrency)
            // Re-fetch and convert incomes with new currency
            fetchIncomes()
        } else {
            // Just refresh the income list when returning to this activity
            fetchIncomes()
        }
    }

    private fun setupRecyclerView() {
        // Pass displayIncomeList instead of incomeList
        incomeAdapter = IncomeAdapter(
            displayIncomeList, 
            { income ->
                val intent = Intent(this, EditIncomeActivity::class.java)
                intent.putExtra("incomeId", income.id)
                startActivity(intent)
            },
            userDefaultCurrency
        )
        binding.recyclerViewIncome.apply {
            layoutManager = LinearLayoutManager(this@IncomeActivity)
            adapter = incomeAdapter
        }
    }

    private fun fetchIncomes() {
        val token = sessionManager.fetchAuthToken()
        if (token.isNullOrEmpty()) {
            // If no token, redirect to login
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            when (val result = incomeRepository.getIncomes(token)) {
                is Resource.Success -> {
                    // Store original list
                    incomeList.clear()
                    incomeList.addAll(result.data)
                    
                    // Process incomes for display with correct currency
                    processIncomesForDisplay(token)
                    
                    binding.txtEmptyState.visibility = if (incomeList.isEmpty()) View.VISIBLE else View.GONE
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@IncomeActivity, "Error: ${result.message}", Toast.LENGTH_SHORT).show()
                    binding.txtEmptyState.visibility = View.VISIBLE
                }
                is Resource.Loading -> {
                    // Keep progress bar visible during loading
                    binding.progressBar.visibility = View.VISIBLE
                }
            }
        }
    }
    
    private suspend fun processIncomesForDisplay(token: String) {
        displayIncomeList.clear()
        
        try {
            withContext(Dispatchers.Default) {
                for (income in incomeList) {
                    // Create a display copy of the income
                    val displayIncome = if (income.currency != userDefaultCurrency) {
                        try {
                            // Try to convert the amount
                            val convertedAmount = CurrencyUtils.convertCurrency(
                                income.amount,
                                income.currency ?: "PHP",
                                userDefaultCurrency,
                                token
                            )
                            
                            if (convertedAmount != null) {
                                // Create a new income with converted amount
                                // IMPORTANT: Preserve true original values when available
                                val trueOriginalAmount = income.originalAmount ?: income.amount
                                val trueOriginalCurrency = income.originalCurrency ?: income.currency
                                
                                Income(
                                    id = income.id,
                                    source = income.source,
                                    date = income.date,
                                    amount = convertedAmount,
                                    currency = userDefaultCurrency,
                                    originalAmount = trueOriginalAmount,
                                    originalCurrency = trueOriginalCurrency
                                )
                            } else {
                                // Conversion failed, use original
                                income
                            }
                        } catch (e: Exception) {
                            Log.e("IncomeActivity", "Currency conversion error: ${e.message}")
                            // On error, use original
                            income
                        }
                    } else {
                        // Same currency as default, but still need to check if we should preserve original data
                        if (income.originalAmount != null && income.originalCurrency != null) {
                            income // Keep original values intact
                        } else {
                            // No conversion needed and no original values
                            income
                        }
                    }
                    
                    displayIncomeList.add(displayIncome)
                }
            }
            
            // Update the UI on main thread
            withContext(Dispatchers.Main) {
                incomeAdapter.notifyDataSetChanged()
                binding.progressBar.visibility = View.GONE
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@IncomeActivity, "Error processing incomes: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}