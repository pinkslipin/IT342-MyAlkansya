package com.example.myalkansyamobile

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.myalkansyamobile.R
import com.example.myalkansyamobile.api.IncomeRequest
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.databinding.ActivityAddIncomeBinding
import com.example.myalkansyamobile.utils.CurrencyUtils
import com.example.myalkansyamobile.utils.DateUtils
import com.example.myalkansyamobile.utils.SessionManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

class AddIncomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddIncomeBinding
    private lateinit var sessionManager: SessionManager
    private var userDefaultCurrency: String = "PHP"
    private var originalAmount: Double? = null
    private var originalCurrency: String? = null
    private var conversionJob: Job? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddIncomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize sessionManager
        sessionManager = SessionManager(this)
        userDefaultCurrency = sessionManager.getCurrency() ?: "PHP"
        
        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Add Income"
        
        // Set up UI components
        setupDatePicker()
        setupCurrencySpinner()
        setupAmountListener()
        
        // Set default date to today
        val today = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        binding.etDate.setText(dateFormat.format(today))
        
        // Setup save button
        binding.btnSave.setOnClickListener {
            saveIncome()
        }
        
        // Show default currency in info text
        binding.tvCurrencyInfo.text = "Your default currency is $userDefaultCurrency. Income will be stored in this currency."
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    
    private fun setupDatePicker() {
        binding.etDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()
                
            datePicker.addOnPositiveButtonClickListener { selection ->
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = selection
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                binding.etDate.setText(dateFormat.format(calendar.time))
            }
            
            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }
    }
    
    private fun setupCurrencySpinner() {
        // Create currency display items
        val currencyItems = CurrencyUtils.currencyCodes.map { code ->
            val isDefault = code == userDefaultCurrency
            val displayText = CurrencyUtils.getCurrencyDisplayText(code) + if (isDefault) " (Default)" else ""
            displayText to code
        }
        
        // Create adapter
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            currencyItems.map { it.first }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        
        // Set adapter
        binding.spinnerCurrency.adapter = adapter
        
        // Set default selection
        val defaultPosition = currencyItems.indexOfFirst { it.second == userDefaultCurrency }
        if (defaultPosition >= 0) {
            binding.spinnerCurrency.setSelection(defaultPosition)
        }
        
        // Handle selection changes
        binding.spinnerCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCurrency = currencyItems[position].second
                
                // Handle currency change
                handleCurrencyChange(selectedCurrency)
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }
    
    private fun handleCurrencyChange(newCurrency: String) {
        // Get current amount
        val amountStr = binding.etAmount.text.toString()
        val currentAmount = amountStr.toDoubleOrNull()
        
        // If amount exists and currency is changed
        if (currentAmount != null && currentAmount > 0) {
            val selectedCurrency = newCurrency
            val currentCurrency = originalCurrency ?: getSelectedCurrency()
            
            if (selectedCurrency != currentCurrency) {
                // Save original values if this is first change
                if (originalAmount == null) {
                    originalAmount = currentAmount
                    originalCurrency = currentCurrency
                }
                
                // If changing back to original currency, restore original amount
                if (selectedCurrency == originalCurrency) {
                    binding.etAmount.setText(String.format("%.2f", originalAmount))
                    binding.tvConversionInfo.text = ""
                    binding.tvConversionInfo.visibility = View.GONE
                } else {
                    // Otherwise convert to new currency
                    convertAmount(currentAmount, currentCurrency, selectedCurrency)
                }
            }
        }
        
        // Update notification message
        updateConversionNotification(newCurrency)
    }
    
    private fun updateConversionNotification(selectedCurrency: String) {
        if (selectedCurrency != userDefaultCurrency) {
            binding.tvCurrencyWarning.text = 
                "Note: This income will be automatically converted to $userDefaultCurrency when saved."
            binding.tvCurrencyWarning.visibility = View.VISIBLE
        } else {
            binding.tvCurrencyWarning.visibility = View.GONE
        }
    }
    
    private fun setupAmountListener() {
        binding.etAmount.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val amountStr = binding.etAmount.text.toString()
                if (amountStr.isNotEmpty()) {
                    try {
                        // Format to two decimal places
                        val amount = amountStr.toDouble()
                        binding.etAmount.setText(String.format("%.2f", amount))
                    } catch (e: Exception) {
                        binding.etAmount.error = "Invalid amount"
                    }
                }
            }
        }
    }
    
    private fun getSelectedCurrency(): String {
        val position = binding.spinnerCurrency.selectedItemPosition
        return CurrencyUtils.currencyCodes[position]
    }
    
    private fun convertAmount(amount: Double, fromCurrency: String, toCurrency: String) {
        // Cancel any ongoing conversion
        conversionJob?.cancel()
        
        // Show loading indicator
        binding.progressBarConversion.visibility = View.VISIBLE
        binding.tvConversionInfo.text = "Converting..."
        binding.tvConversionInfo.visibility = View.VISIBLE
        
        conversionJob = lifecycleScope.launch {
            try {
                val authToken = sessionManager.getToken()
                if (authToken.isNullOrEmpty()) {
                    binding.tvConversionInfo.text = "Error: Not logged in"
                    binding.progressBarConversion.visibility = View.GONE
                    return@launch
                }
                
                val convertedAmount = CurrencyUtils.convertCurrency(
                    amount, 
                    fromCurrency, 
                    toCurrency, 
                    authToken
                )
                
                if (convertedAmount != null) {
                    binding.etAmount.setText(String.format("%.2f", convertedAmount))
                    binding.tvConversionInfo.text = "Converted ${CurrencyUtils.formatAmount(amount)} $fromCurrency to ${CurrencyUtils.formatAmount(convertedAmount)} $toCurrency"
                } else {
                    binding.tvConversionInfo.text = "Conversion failed. Please enter amount manually."
                }
            } catch (e: Exception) {
                binding.tvConversionInfo.text = "Error: ${e.message}"
            } finally {
                binding.progressBarConversion.visibility = View.GONE
            }
        }
    }
    
    private fun saveIncome() {
        val source = binding.etSource.text.toString()
        val amountStr = binding.etAmount.text.toString()
        val dateStr = binding.etDate.text.toString()
        val currency = getSelectedCurrency()
        
        // Validate inputs
        if (source.isEmpty()) {
            binding.etSource.error = "Source is required"
            return
        }
        
        if (amountStr.isEmpty()) {
            binding.etAmount.error = "Amount is required"
            return
        }
        
        val amount = try {
            amountStr.toDouble()
        } catch (e: Exception) {
            binding.etAmount.error = "Invalid amount"
            return
        }
        
        if (amount <= 0) {
            binding.etAmount.error = "Amount must be greater than zero"
            return
        }
        
        if (dateStr.isEmpty()) {
            binding.etDate.error = "Date is required"
            return
        }
        
        // Show loading state
        binding.btnSave.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        
        // If currency is different than default, actually convert the amount
        if (currency != userDefaultCurrency) {
            binding.tvCurrencyWarning.text = "Converting to $userDefaultCurrency before saving..."
            binding.tvCurrencyWarning.visibility = View.VISIBLE

            // Perform the conversion before saving
            lifecycleScope.launch {
                try {
                    val token = sessionManager.getToken()
                    if (token.isNullOrEmpty()) {
                        showError("You must be logged in to add income")
                        binding.btnSave.isEnabled = true
                        binding.progressBar.visibility = View.GONE
                        return@launch
                    }
                    
                    // Get the converted amount using the CurrencyUtils
                    val convertedAmount = withContext(Dispatchers.IO) {
                        CurrencyUtils.convertCurrency(amount, currency, userDefaultCurrency, token)
                    } ?: amount // Fallback to original if conversion fails

                    // Save both the original and converted amounts
                    saveIncomeWithConversion(source, dateStr, amount, currency, convertedAmount, userDefaultCurrency)
                    
                } catch (e: Exception) {
                    showError("Error during conversion: ${e.message}")
                    binding.btnSave.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                }
            }
        } else {
            // No conversion needed, save directly
            saveIncomeWithConversion(source, dateStr, amount, currency, null, null)
        }
    }
    
    private fun saveIncomeWithConversion(
        source: String,
        dateStr: String,
        originalAmount: Double, 
        originalCurrency: String,
        convertedAmount: Double?, 
        convertedCurrency: String?
    ) {
        // Use the target amount and currency for the main fields
        val finalAmount = convertedAmount ?: originalAmount
        val finalCurrency = convertedCurrency ?: originalCurrency
        
        // Create request with both original and converted values
        val incomeRequest = IncomeRequest(
            source = source,
            amount = BigDecimal(finalAmount).toString(),
            date = dateStr,
            currency = finalCurrency,
            originalAmount = if (convertedAmount != null) BigDecimal(originalAmount).toString() else null,
            originalCurrency = if (convertedAmount != null) originalCurrency else null
        )
        
        // Call API to save income
        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    showError("You must be logged in to add income")
                    return@launch
                }
                
                val response = RetrofitClient.incomeApiService.addIncome(
                    "Bearer $token", 
                    incomeRequest
                )
                
                if (response.isSuccessful && response.body() != null) {
                    // Show success message
                    Snackbar.make(
                        binding.root,
                        "Income added successfully",
                        Snackbar.LENGTH_SHORT
                    ).show()
                    
                    // Return to previous screen after a short delay
                    delay(1000)
                    setResult(RESULT_OK)
                    finish()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Unknown error occurred"
                    showError("Failed to add income: $errorMsg")
                }
            } catch (e: HttpException) {
                showError("Network error: ${e.message()}")
            } catch (e: Exception) {
                showError("Error: ${e.message}")
            } finally {
                binding.btnSave.isEnabled = true
                binding.progressBar.visibility = View.GONE
            }
        }
    }
    
    private fun showError(message: String) {
        binding.btnSave.isEnabled = true
        binding.progressBar.visibility = View.GONE
        
        Snackbar.make(
            binding.root,
            message,
            Snackbar.LENGTH_LONG
        ).setBackgroundTint(
            ContextCompat.getColor(this, R.color.error_color)
        ).show()
    }
}
