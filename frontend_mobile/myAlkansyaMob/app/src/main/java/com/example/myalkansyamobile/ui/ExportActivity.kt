package com.example.myalkansyamobile.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myalkansyamobile.R
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.databinding.ActivityExportBinding
import com.example.myalkansyamobile.utils.ExportService
import com.example.myalkansyamobile.utils.GoogleSheetsHelper
import com.example.myalkansyamobile.utils.SessionManager
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.google.api.services.sheets.v4.SheetsScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException

class ExportActivity : AppCompatActivity() {
    private lateinit var binding: ActivityExportBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var exportService: ExportService
    
    private val TAG = "ExportActivity"
    
    // Activity result launcher for Sheets API permission intent
    private val sheetsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            showMessage("Permission granted, retrying export...")
            fetchDataAndExportToGoogleSheets()
        } else {
            showMessage("Google Sheets permission not granted")
            binding.progressBar.visibility = View.GONE
            binding.btnExportToSheets.isEnabled = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Export Data"
        
        sessionManager = SessionManager(this)
        exportService = ExportService(this)
        
        setupUI()
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    
    private fun setupUI() {
        // Update account info
        // Fix for the getEmail() unresolved reference error
        val accountInfo = getUserEmailFromSession() ?: "your Google account"
        binding.tvAccountInfo.text = "You'll use $accountInfo for Sheets export"
        
        // Export to Google Sheets button
        binding.btnExportToSheets.setOnClickListener {
            fetchDataAndExportToGoogleSheets()
        }
        
        // Export to CSV button
        binding.btnExportToCSV.setOnClickListener {
            exportToCSV()
        }
    }
    
    // Helper method to get email from session
    private fun getUserEmailFromSession(): String? {
        // Replace with the actual method your SessionManager uses to get the user's email
        return sessionManager.fetchUserEmail()
    }
    
    private fun fetchDataAndExportToGoogleSheets() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnExportToSheets.isEnabled = false
        
        // Show loading dialog
        val dialog = AlertDialog.Builder(this)
            .setTitle("Exporting Data")
            .setMessage("Please wait...\nFetching your financial data...")
            .setCancelable(false)
            .create()
        dialog.show()
        
        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken() ?: ""
                if (token.isEmpty()) {
                    dialog.dismiss()
                    showMessage("Please log in to your account first")
                    binding.progressBar.visibility = View.GONE
                    binding.btnExportToSheets.isEnabled = true
                    return@launch
                }

                val bearerToken = "Bearer $token"
                
                // Fetch real user data for export
                dialog.setMessage("Fetching income data...")
                val incomes = try {
                    withContext(Dispatchers.IO) {
                        RetrofitClient.incomeApiService.getIncomes(bearerToken)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching incomes for export: ${e.message}")
                    emptyList<com.example.myalkansyamobile.model.Income>()
                }
                
                dialog.setMessage("Fetching expense data...")
                val expenses = try {
                    withContext(Dispatchers.IO) {
                        RetrofitClient.expenseApiService.getExpenses(bearerToken)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching expenses for export: ${e.message}")
                    emptyList<com.example.myalkansyamobile.api.ExpenseResponse>()
                }
                
                dialog.setMessage("Fetching budget data...")
                val budgets = try {
                    withContext(Dispatchers.IO) {
                        RetrofitClient.budgetApiService.getUserBudgets(bearerToken)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching budgets for export: ${e.message}")
                    emptyList<com.example.myalkansyamobile.BudgetResponse>()
                }
                
                dialog.setMessage("Fetching savings goals...")
                val savingsGoals = try {
                    withContext(Dispatchers.IO) {
                        RetrofitClient.savingsGoalApiService.getAllSavingsGoals(bearerToken)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching savings goals for export: ${e.message}")
                    emptyList<com.example.myalkansyamobile.api.SavingsGoalResponse>()
                }

                dialog.setMessage("Fetching financial summary...")
                val financialSummary = try {
                    withContext(Dispatchers.IO) {
                        val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1
                        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                        RetrofitClient.analyticsApiService.getFinancialSummary(
                            bearerToken,
                            currentMonth,
                            currentYear
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching financial summary for export: ${e.message}")
                    null
                }
                
                dialog.setMessage("Creating Google Sheet...")
                
                // Now attempt to export to Google Sheets
                // Fix for type inference issues - explicitly specify types
                try {
                    val sheetsUrl = exportService.exportToGoogleSheets(
                        incomes = incomes as Any,
                        expenses = expenses as Any,
                        budgets = budgets as Any,
                        savingsGoals = savingsGoals as Any,
                        financialSummary = financialSummary
                    )
                    
                    dialog.dismiss()
                    
                    if (sheetsUrl != null) {
                        // Success! Open the Google Sheet
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = android.net.Uri.parse(sheetsUrl)
                        startActivity(intent)
                        
                        showMessage("Data successfully exported to Google Sheets")
                    } else {
                        showMessage("Failed to create Google Sheet")
                    }
                } catch (e: GoogleSheetsHelper.SheetsPermissionRequiredException) {
                    dialog.dismiss()
                    showMessage("Additional permissions required for Google Sheets")
                    // Launch the permission intent using the launcher
                    sheetsPermissionLauncher.launch(e.permissionIntent)
                } catch (e: Exception) {
                    dialog.dismiss()
                    Log.e(TAG, "Error exporting to Google Sheets", e)
                    showMessage("Export to Google Sheets failed: ${e.message}")
                    
                    // Offer CSV export as fallback
                    AlertDialog.Builder(this@ExportActivity)
                        .setTitle("Google Sheets Export Failed")
                        .setMessage("Would you like to export as CSV file instead?")
                        .setPositiveButton("Yes") { _, _ -> exportToCSV() }
                        .setNegativeButton("No", null)
                        .show()
                }
                
            } catch (e: SocketTimeoutException) {
                dialog.dismiss()
                showMessage("Connection timed out. Please check your internet connection.")
            } catch (e: Exception) {
                dialog.dismiss()
                Log.e(TAG, "Export failed", e)
                showMessage("Export failed: ${e.message}")
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnExportToSheets.isEnabled = true
            }
        }
    }
    
    private fun exportToCSV() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnExportToCSV.isEnabled = false
        
        // Show loading dialog
        val dialog = AlertDialog.Builder(this)
            .setTitle("Creating CSV Export")
            .setMessage("Please wait...")
            .setCancelable(false)
            .create()
        dialog.show()
        
        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken() ?: ""
                if (token.isEmpty()) {
                    dialog.dismiss()
                    showMessage("Please log in to your account first")
                    binding.progressBar.visibility = View.GONE
                    binding.btnExportToCSV.isEnabled = true
                    return@launch
                }

                val bearerToken = "Bearer $token"
                
                // Fetch user data (same as before)
                val incomes = try {
                    withContext(Dispatchers.IO) {
                        RetrofitClient.incomeApiService.getIncomes(bearerToken)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching incomes", e)
                    emptyList<com.example.myalkansyamobile.model.Income>()
                }
                
                val expenses = try {
                    withContext(Dispatchers.IO) {
                        RetrofitClient.expenseApiService.getExpenses(bearerToken)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching expenses", e)
                    emptyList<com.example.myalkansyamobile.api.ExpenseResponse>()
                }
                
                val budgets = try {
                    withContext(Dispatchers.IO) {
                        RetrofitClient.budgetApiService.getUserBudgets(bearerToken)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching budgets", e)
                    emptyList<com.example.myalkansyamobile.BudgetResponse>()
                }
                
                val savingsGoals = try {
                    withContext(Dispatchers.IO) {
                        RetrofitClient.savingsGoalApiService.getAllSavingsGoals(bearerToken)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching savings goals", e)
                    emptyList<com.example.myalkansyamobile.api.SavingsGoalResponse>()
                }
                
                val financialSummary = try {
                    val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1
                    val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                    withContext(Dispatchers.IO) {
                        RetrofitClient.analyticsApiService.getFinancialSummary(
                            bearerToken, currentMonth, currentYear
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching financial summary", e)
                    null
                }
                
                // Fix for type inference issues - explicitly specify types
                val uri = exportService.exportToCSV(
                    incomes = incomes as Any,
                    expenses = expenses as Any,
                    budgets = budgets as Any, 
                    savingsGoals = savingsGoals as Any,
                    financialSummary = financialSummary
                )
                
                dialog.dismiss()
                
                if (uri != null) {
                    exportService.shareCSVFile(uri)
                } else {
                    showMessage("Failed to create CSV file")
                }
                
            } catch (e: Exception) {
                dialog.dismiss()
                Log.e(TAG, "CSV Export failed", e)
                showMessage("CSV Export failed: ${e.message}")
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnExportToCSV.isEnabled = true
            }
        }
    }
    
    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
}
