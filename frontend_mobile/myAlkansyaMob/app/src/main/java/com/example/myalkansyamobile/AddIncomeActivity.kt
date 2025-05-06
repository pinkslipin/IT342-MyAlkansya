package com.example.myalkansyamobile

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.myalkansyamobile.api.IncomeRequest
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.utils.CurrencyUtils
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
    // UI elements
    private lateinit var etSource: EditText
    private lateinit var etDate: EditText
    private lateinit var btnPickDate: ImageView
    private lateinit var etAmount: EditText
    private lateinit var spinnerCurrency: Spinner
    private lateinit var tvCurrencyWarning: TextView
    private lateinit var progressBarConversion: ProgressBar
    private lateinit var tvConversionInfo: TextView
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var btnBack: ImageButton
    private lateinit var tvCurrencyInfo: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView

    // Other variables
    private lateinit var sessionManager: SessionManager
    private var userDefaultCurrency: String = "PHP"
    private var originalAmount: Double? = null
    private var originalCurrency: String? = null
    private var conversionJob: Job? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_income)
        
        // Initialize sessionManager
        sessionManager = SessionManager(this)
        userDefaultCurrency = sessionManager.getCurrency() ?: "PHP"
        
        // Initialize UI components
        initializeViews()
        
        // Set up UI interaction
        setupBackButton()
        setupDatePicker()
        setupCurrencySpinner()
        setupAmountListener()
        setupButtons()
        
        // Set default date to today
        val today = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        etDate.setText(dateFormat.format(today))
        
        // Show default currency in info text
        tvCurrencyInfo.text = "Your default currency is $userDefaultCurrency. Income will be stored in this currency."
    }
    
    private fun initializeViews() {
        // Find all views
        etSource = findViewById(R.id.etSource)
        etDate = findViewById(R.id.etDate)
        btnPickDate = findViewById(R.id.btnPickDate)
        etAmount = findViewById(R.id.etAmount)
        spinnerCurrency = findViewById(R.id.spinnerCurrency)
        tvCurrencyWarning = findViewById(R.id.tvCurrencyWarning)
        progressBarConversion = findViewById(R.id.progressBarConversion)
        tvConversionInfo = findViewById(R.id.tvConversionInfo)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)
        btnBack = findViewById(R.id.btnBack)
        tvCurrencyInfo = findViewById(R.id.tvCurrencyInfo)
        progressBar = findViewById(R.id.progressBar)
        tvError = findViewById(R.id.tvError)
    }
    
    private fun setupBackButton() {
        btnBack.setOnClickListener {
            finish()
        }
    }
    
    private fun setupButtons() {
        btnSave.setOnClickListener {
            saveIncome()
        }
        
        btnCancel.setOnClickListener {
            finish()
        }
    }
    
    private fun setupDatePicker() {
        val dateClickListener = View.OnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()
                
            datePicker.addOnPositiveButtonClickListener { selection ->
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = selection
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                etDate.setText(dateFormat.format(calendar.time))
            }
            
            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }
        
        etDate.setOnClickListener(dateClickListener)
        btnPickDate.setOnClickListener(dateClickListener)
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
        spinnerCurrency.adapter = adapter
        
        // Set default selection
        val defaultPosition = currencyItems.indexOfFirst { it.second == userDefaultCurrency }
        if (defaultPosition >= 0) {
            spinnerCurrency.setSelection(defaultPosition)
        }
        
        // Handle selection changes
        spinnerCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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
        val amountStr = etAmount.text.toString()
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
                    etAmount.setText(String.format("%.2f", originalAmount))
                    tvConversionInfo.text = ""
                    tvConversionInfo.visibility = View.GONE
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
            tvCurrencyWarning.text = 
                "Note: This income will be automatically converted to $userDefaultCurrency when saved."
            tvCurrencyWarning.visibility = View.VISIBLE
        } else {
            tvCurrencyWarning.visibility = View.GONE
        }
    }
    
    private fun setupAmountListener() {
        etAmount.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val amountStr = etAmount.text.toString()
                if (amountStr.isNotEmpty()) {
                    try {
                        // Format to two decimal places
                        val amount = amountStr.toDouble()
                        etAmount.setText(String.format("%.2f", amount))
                    } catch (e: Exception) {
                        etAmount.error = "Invalid amount"
                    }
                }
            }
        }
    }
    
    private fun getSelectedCurrency(): String {
        val position = spinnerCurrency.selectedItemPosition
        return CurrencyUtils.currencyCodes[position]
    }
    
    private fun convertAmount(amount: Double, fromCurrency: String, toCurrency: String) {
        // Cancel any ongoing conversion
        conversionJob?.cancel()
        
        // Show loading indicator
        progressBarConversion.visibility = View.VISIBLE
        tvConversionInfo.text = "Converting..."
        tvConversionInfo.visibility = View.VISIBLE
        
        conversionJob = lifecycleScope.launch {
            try {
                val authToken = sessionManager.getToken()
                if (authToken.isNullOrEmpty()) {
                    tvConversionInfo.text = "Error: Not logged in"
                    progressBarConversion.visibility = View.GONE
                    return@launch
                }
                
                val convertedAmount = CurrencyUtils.convertCurrency(
                    amount, 
                    fromCurrency, 
                    toCurrency, 
                    authToken
                )
                
                if (convertedAmount != null) {
                    etAmount.setText(String.format("%.2f", convertedAmount))
                    tvConversionInfo.text = "Converted ${CurrencyUtils.formatAmount(amount)} $fromCurrency to ${CurrencyUtils.formatAmount(convertedAmount)} $toCurrency"
                } else {
                    tvConversionInfo.text = "Conversion failed. Please enter amount manually."
                }
            } catch (e: Exception) {
                tvConversionInfo.text = "Error: ${e.message}"
            } finally {
                progressBarConversion.visibility = View.GONE
            }
        }
    }
    
    private fun saveIncome() {
        val source = etSource.text.toString()
        val amountStr = etAmount.text.toString()
        val dateStr = etDate.text.toString()
        val currency = getSelectedCurrency()
        
        // Validate inputs
        if (source.isEmpty()) {
            etSource.error = "Source is required"
            return
        }
        
        if (amountStr.isEmpty()) {
            etAmount.error = "Amount is required"
            return
        }
        
        val amount = try {
            amountStr.toDouble()
        } catch (e: Exception) {
            etAmount.error = "Invalid amount"
            return
        }
        
        if (amount <= 0) {
            etAmount.error = "Amount must be greater than zero"
            return
        }
        
        if (dateStr.isEmpty()) {
            etDate.error = "Date is required"
            return
        }
        
        // Show loading state
        btnSave.isEnabled = false
        progressBar.visibility = View.VISIBLE
        
        // If currency is different than default, actually convert the amount
        if (currency != userDefaultCurrency) {
            tvCurrencyWarning.text = "Converting to $userDefaultCurrency before saving..."
            tvCurrencyWarning.visibility = View.VISIBLE

            // Perform the conversion before saving
            lifecycleScope.launch {
                try {
                    val token = sessionManager.getToken()
                    if (token.isNullOrEmpty()) {
                        showError("You must be logged in to add income")
                        btnSave.isEnabled = true
                        progressBar.visibility = View.GONE
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
                    btnSave.isEnabled = true
                    progressBar.visibility = View.GONE
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
                        findViewById(android.R.id.content),
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
                btnSave.isEnabled = true
                progressBar.visibility = View.GONE
            }
        }
    }
    
    private fun showError(message: String) {
        btnSave.isEnabled = true
        progressBar.visibility = View.GONE
        
        // Display error in the designated TextView
        tvError.text = message
        tvError.visibility = View.VISIBLE
        
        // Also show a Snackbar for immediate feedback
        Snackbar.make(
            findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_LONG
        ).setBackgroundTint(
            ContextCompat.getColor(this, R.color.error_color)
        ).show()
    }
}
