package com.example.myalkansyamobile

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myalkansyamobile.api.IncomeRepository
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.model.Income
import com.example.myalkansyamobile.utils.CurrencyUtils
import com.example.myalkansyamobile.utils.Resource
import com.example.myalkansyamobile.utils.SessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditIncomeActivity : AppCompatActivity() {
    private lateinit var etSource: EditText
    private lateinit var etDate: EditText
    private lateinit var etAmount: EditText
    private lateinit var spinnerCurrency: Spinner
    private lateinit var btnPickDate: ImageView
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var btnDelete: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvCurrencyWarning: TextView
    private lateinit var tvError: TextView
    
    private val calendar = Calendar.getInstance()
    private lateinit var sessionManager: SessionManager
    private lateinit var incomeRepository: IncomeRepository
    
    private var incomeId: Int = -1
    private var userDefaultCurrency: String = "PHP"
    private var originalAmount: Double? = null
    private var originalCurrency: String? = null
    private var conversionJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_income)

        sessionManager = SessionManager(this)
        userDefaultCurrency = sessionManager.getCurrency() ?: "PHP"
        incomeRepository = IncomeRepository(RetrofitClient.incomeApiService)

        // Initialize UI components
        initializeUI()
        
        incomeId = intent.getIntExtra("incomeId", -1)
        if (incomeId == -1) {
            showError("Invalid income ID")
            finish()
            return
        }

        setupUI()
        fetchIncomeDetails()
    }

    private fun initializeUI() {
        try {
            etSource = findViewById(R.id.editTextSource)
            etDate = findViewById(R.id.editTextDate)
            etAmount = findViewById(R.id.editTextAmount)
            spinnerCurrency = findViewById(R.id.spinnerCurrency)
            btnPickDate = findViewById(R.id.btnPickDate)
            btnSave = findViewById(R.id.btnSave)
            btnCancel = findViewById(R.id.btnCancel)
            btnDelete = findViewById(R.id.btnDelete)
            progressBar = findViewById(R.id.progressBar)
            tvCurrencyWarning = findViewById(R.id.tvCurrencyWarning)
            tvError = findViewById(R.id.tvError)
            
        } catch (e: Exception) {
            Log.e("EditIncomeActivity", "Error finding views: ${e.message}")
            Toast.makeText(this, "Failed to initialize UI: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupUI() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
        
        btnCancel.setOnClickListener {
            finish()
        }
        
        btnSave.setOnClickListener {
            updateIncome()
        }
        
        btnDelete.setOnClickListener {
            showDeleteConfirmationDialog()
        }
        
        setupCurrencySpinner()
        setupDatePicker()
        setupAmountListener()
    }
    
    private fun setupCurrencySpinner() {
        val currencyItems = CurrencyUtils.currencyCodes.map { code ->
            val isDefault = code == userDefaultCurrency
            val displayText = CurrencyUtils.getCurrencyDisplayText(code) + if (isDefault) " (Default)" else ""
            displayText
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencyItems)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCurrency.adapter = adapter
        
        // Set default selection
        val defaultPosition = CurrencyUtils.currencyCodes.indexOf(userDefaultCurrency)
        if (defaultPosition >= 0) {
            spinnerCurrency.setSelection(defaultPosition)
        }
        
        // Handle selection changes
        spinnerCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCurrency = CurrencyUtils.currencyCodes[position]
                handleCurrencyChange(selectedCurrency)
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }
    
    private fun setupDatePicker() {
        etDate.setOnClickListener {
            showDatePickerDialog()
        }
        
        btnPickDate.setOnClickListener {
            showDatePickerDialog()
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
                    tvCurrencyWarning.visibility = View.GONE
                } else {
                    // Otherwise convert to new currency
                    convertAmount(currentAmount, currentCurrency, selectedCurrency)
                }
            }
        }
        
        updateCurrencyWarning(newCurrency)
    }
    
    private fun updateCurrencyWarning(selectedCurrency: String) {
        if (selectedCurrency != userDefaultCurrency) {
            tvCurrencyWarning.text = 
                "Note: This income will be automatically converted to $userDefaultCurrency when saved."
            tvCurrencyWarning.visibility = View.VISIBLE
        } else {
            tvCurrencyWarning.visibility = View.GONE
        }
    }
    
    private fun getSelectedCurrency(): String {
        val position = spinnerCurrency.selectedItemPosition
        return CurrencyUtils.currencyCodes[position]
    }

    private fun showDatePickerDialog() {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, monthOfYear, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, monthOfYear)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateInView()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun updateDateInView() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        etDate.setText(sdf.format(calendar.time))
    }
    
    private fun convertAmount(amount: Double, fromCurrency: String, toCurrency: String) {
        // Cancel any ongoing conversion
        conversionJob?.cancel()
        
        // Show loading indicator
        progressBar.visibility = View.VISIBLE
        
        conversionJob = lifecycleScope.launch {
            try {
                val authToken = sessionManager.getToken()
                if (authToken.isNullOrEmpty()) {
                    showError("Error: Not logged in")
                    progressBar.visibility = View.GONE
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
                    Toast.makeText(
                        this@EditIncomeActivity,
                        "Converted ${CurrencyUtils.formatAmount(amount)} $fromCurrency to ${CurrencyUtils.formatAmount(convertedAmount)} $toCurrency",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    showError("Conversion failed. Please enter amount manually.")
                }
            } catch (e: Exception) {
                showError("Error: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun fetchIncomeDetails() {
        showLoading(true)
        
        val token = sessionManager.fetchAuthToken()
        if (token.isNullOrEmpty()) {
            showError("Please login first")
            finish()
            return
        }
        
        lifecycleScope.launch {
            when (val result = incomeRepository.getIncomeById(incomeId, token)) {
                is Resource.Success -> {
                    val income = result.data
                    etSource.setText(income.source)
                    etDate.setText(income.date)
                    
                    // Parse date string to set the calendar
                    try {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val date = sdf.parse(income.date)
                        if (date != null) {
                            calendar.time = date
                        }
                    } catch (e: Exception) {
                        Log.e("EditIncomeActivity", "Error parsing date: ${e.message}")
                    }
                    
                    // Always prioritize showing the true original values if they exist
                    if (income.originalAmount != null && income.originalCurrency != null) {
                        // If we have original values, show those instead
                        etAmount.setText(String.format("%.2f", income.originalAmount))
                        originalAmount = income.originalAmount
                        originalCurrency = income.originalCurrency
                        
                        // Set spinner to original currency
                        val originalPosition = CurrencyUtils.currencyCodes.indexOf(income.originalCurrency)
                        if (originalPosition >= 0) {
                            spinnerCurrency.setSelection(originalPosition)
                        }
                    } else {
                        // No original values available, use the stored ones
                        etAmount.setText(String.format("%.2f", income.amount))
                        originalAmount = income.amount
                        originalCurrency = income.currency
                        
                        val position = CurrencyUtils.currencyCodes.indexOf(income.currency ?: userDefaultCurrency)
                        if (position >= 0) {
                            spinnerCurrency.setSelection(position)
                        }
                    }
                    
                    // Update currency warning if needed
                    updateCurrencyWarning(getSelectedCurrency())
                    
                    showLoading(false)
                }
                is Resource.Error -> {
                    showError("Error: ${result.message}")
                    finish()
                }
                is Resource.Loading -> {
                    // Keep loading state active
                }
            }
        }
    }

    private fun updateIncome() {
        val source = etSource.text.toString()
        val date = etDate.text.toString()
        val amountStr = etAmount.text.toString()
        val currency = getSelectedCurrency()

        if (source.isEmpty()) {
            etSource.error = "Source cannot be empty"
            return
        }
        
        if (date.isEmpty()) {
            showError("Please select a date")
            return
        }
        
        if (amountStr.isEmpty()) {
            etAmount.error = "Amount cannot be empty"
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

        val token = sessionManager.fetchAuthToken()
        if (token.isNullOrEmpty()) {
            showError("Please login first")
            return
        }
        
        showLoading(true)
        btnSave.isEnabled = false
        btnDelete.isEnabled = false
        btnCancel.isEnabled = false
        
        // If selected currency is different from user's default, convert it
        if (currency != userDefaultCurrency) {
            // First try to convert to user's preferred currency
            lifecycleScope.launch {
                try {
                    val convertedAmount = CurrencyUtils.convertCurrency(
                        amount,
                        currency,
                        userDefaultCurrency,
                        token
                    )
                    
                    if (convertedAmount != null) {
                        // Create income with both original and converted values
                        val updatedIncome = Income(
                            id = incomeId,
                            source = source,
                            date = date,
                            amount = convertedAmount,
                            currency = userDefaultCurrency,
                            originalAmount = amount,
                            originalCurrency = currency
                        )
                        
                        submitIncomeUpdate(updatedIncome, token)
                    } else {
                        // Conversion failed
                        showLoading(false)
                        btnSave.isEnabled = true
                        btnDelete.isEnabled = true
                        btnCancel.isEnabled = true
                        showError("Currency conversion failed. Try again or use your default currency.")
                    }
                } catch (e: Exception) {
                    showLoading(false)
                    btnSave.isEnabled = true
                    btnDelete.isEnabled = true
                    btnCancel.isEnabled = true
                    showError("Error: ${e.message}")
                }
            }
        } else {
            // Currency matches user's default, no need to convert
            val updatedIncome = Income(
                id = incomeId,
                source = source,
                date = date,
                amount = amount,
                currency = currency
            )
            lifecycleScope.launch {
                submitIncomeUpdate(updatedIncome, token)
            }
        }
    }
    
    private suspend fun submitIncomeUpdate(updatedIncome: Income, token: String) {
        when (val result = incomeRepository.updateIncome(incomeId, updatedIncome, token)) {
            is Resource.Success -> {
                showLoading(false)
                Toast.makeText(this@EditIncomeActivity, "Income updated successfully", Toast.LENGTH_SHORT).show()
                finish() // Go back to previous screen
            }
            is Resource.Error -> {
                showLoading(false)
                btnSave.isEnabled = true
                btnDelete.isEnabled = true
                btnCancel.isEnabled = true
                showError("Error: ${result.message}")
            }
            is Resource.Loading -> {
                // Keep loading state active
            }
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_income)
            .setMessage("Are you sure you want to delete this income record? This action cannot be undone.")
            .setPositiveButton(R.string.delete) { _, _ ->
                deleteIncome()
            }
            .setNegativeButton(R.string.cancel, null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun deleteIncome() {
        showLoading(true)
        btnSave.isEnabled = false
        btnDelete.isEnabled = false
        btnCancel.isEnabled = false

        lifecycleScope.launch {
            val token = sessionManager.fetchAuthToken() ?: ""
            when (val result = incomeRepository.deleteIncome(incomeId, token)) {
                is Resource.Success -> {
                    Toast.makeText(this@EditIncomeActivity, "Income deleted successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is Resource.Error -> {
                    showLoading(false)
                    btnSave.isEnabled = true
                    btnDelete.isEnabled = true
                    btnCancel.isEnabled = true
                    showError("Error: ${result.message}")
                }
                is Resource.Loading -> {
                    // Keep loading state active
                }
            }
        }
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}