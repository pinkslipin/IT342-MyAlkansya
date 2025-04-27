package com.example.myalkansyamobile

import android.app.ProgressDialog
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myalkansyamobile.utils.CurrencyUtils
import com.example.myalkansyamobile.utils.SessionManager
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager
    private lateinit var currencySpinner: Spinner
    private lateinit var btnSaveSettings: Button
    
    // Current user settings
    private var currentCurrency: String = "PHP"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        // Set up action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"
        
        // Initialize SessionManager
        sessionManager = SessionManager(this)
        currentCurrency = sessionManager.getCurrency() ?: "PHP"
        
        // Initialize UI components
        initializeUI()
        
        // Set up the currency dropdown
        setupCurrencySpinner()
        
        // Set up save button
        btnSaveSettings.setOnClickListener {
            saveSettings()
        }
    }
    
    private fun initializeUI() {
        try {
            currencySpinner = findViewById(R.id.spinnerCurrency)
            btnSaveSettings = findViewById(R.id.btnSaveSettings)
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing UI: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun setupCurrencySpinner() {
        // Create the currency options
        val currencyCodes = arrayOf("PHP", "USD", "EUR", "GBP", "JPY", "CNY", "CAD", "AUD")
        val currencyOptions = currencyCodes.map { code ->
            "$code - ${getCurrencyName(code)}"
        }.toTypedArray()
        
        // Create and set the adapter
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencyOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        currencySpinner.adapter = adapter
        
        // Set the current selection
        val currentPosition = currencyCodes.indexOf(currentCurrency)
        if (currentPosition >= 0) {
            currencySpinner.setSelection(currentPosition)
        }
    }
    
    private fun getCurrencyName(code: String): String {
        return when (code) {
            "PHP" -> "Philippine Peso"
            "USD" -> "US Dollar"
            "EUR" -> "Euro"
            "GBP" -> "British Pound"
            "JPY" -> "Japanese Yen"
            "CNY" -> "Chinese Yuan"
            "CAD" -> "Canadian Dollar"
            "AUD" -> "Australian Dollar"
            else -> code
        }
    }
    
    private fun saveSettings() {
        // Get the selected currency code
        val currencyCodes = arrayOf("PHP", "USD", "EUR", "GBP", "JPY", "CNY", "CAD", "AUD")
        val position = currencySpinner.selectedItemPosition
        if (position < 0 || position >= currencyCodes.size) {
            Toast.makeText(this, "Please select a valid currency", Toast.LENGTH_SHORT).show()
            return
        }
        
        val selectedCurrency = currencyCodes[position]
        
        // If currency hasn't changed, just return
        if (selectedCurrency == currentCurrency) {
            Toast.makeText(this, "No changes to save", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Show a progress dialog
        handleCurrencySelection(selectedCurrency)
    }
    
    private fun handleCurrencySelection(selectedCurrency: String) {
        val progressDialog = ProgressDialog(this).apply {
            setTitle("Updating Currency")
            setMessage("Converting all your financial data to $selectedCurrency. This may take a moment...")
            setCancelable(false)
            show()
        }
        
        lifecycleScope.launch {
            val success = sessionManager.updateCurrency(selectedCurrency)
            
            progressDialog.dismiss()
            
            if (success) {
                Toast.makeText(
                    this@SettingsActivity,
                    "Currency changed to $selectedCurrency. All your financial data has been converted.",
                    Toast.LENGTH_LONG
                ).show()
                
                // Let other activities know to refresh their data
                setResult(RESULT_OK)
                finish()
            } else {
                Toast.makeText(
                    this@SettingsActivity,
                    "Failed to update currency. Please try again.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}