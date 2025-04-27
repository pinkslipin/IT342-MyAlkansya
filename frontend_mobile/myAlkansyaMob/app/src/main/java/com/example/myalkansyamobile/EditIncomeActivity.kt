package com.example.myalkansyamobile

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myalkansyamobile.api.IncomeRepository
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.databinding.ActivityEditIncomeBinding
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
    private lateinit var binding: ActivityEditIncomeBinding
    private var incomeId: Int = -1
    private val calendar = Calendar.getInstance()
    private lateinit var sessionManager: SessionManager
    private lateinit var incomeRepository: IncomeRepository
    private var userDefaultCurrency: String = "PHP"
    private var originalAmount: Double? = null
    private var originalCurrency: String? = null
    private var conversionJob: Job? = null
    
    private val currencies = arrayOf("PHP", "USD", "EUR", "GBP", "JPY", "CNY", "CAD", "AUD")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditIncomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        userDefaultCurrency = sessionManager.getCurrency() ?: "PHP"
        incomeRepository = IncomeRepository(RetrofitClient.incomeApiService)

        // Setup currency spinner
        setupCurrencySpinner()
        
        // Setup date picker for the date field
        binding.editTextDate.setOnClickListener {
            showDatePickerDialog()
        }
        
        // Setup amount focus listener for formatting
        setupAmountListener()

        incomeId = intent.getIntExtra("incomeId", -1)
        if (incomeId == -1) {
            Toast.makeText(this, "Invalid income ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        fetchIncomeDetails()

        binding.btnSave.setOnClickListener {
            updateIncome()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        // Add delete button click listener
        binding.btnDelete.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun setupCurrencySpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, currencies)
        binding.spinnerCurrency.adapter = adapter
        
        // Set default selection
        val defaultPosition = currencies.indexOf(userDefaultCurrency)
        if (defaultPosition >= 0) {
            binding.spinnerCurrency.setSelection(defaultPosition)
        }
        
        // Handle selection changes
        binding.spinnerCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCurrency = currencies[position]
                handleCurrencyChange(selectedCurrency)
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }
    
    private fun handleCurrencyChange(newCurrency: String) {
        // Get current amount
        val amountStr = binding.editTextAmount.text.toString()
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
                    binding.editTextAmount.setText(String.format("%.2f", originalAmount))
                } else {
                    // Otherwise convert to new currency
                    convertAmount(currentAmount, currentCurrency, selectedCurrency)
                }
            }
        }
    }
    
    private fun setupAmountListener() {
        binding.editTextAmount.setOnFocusChangeListener { _, hasFocus -> 
            if (!hasFocus) {
                val amountStr = binding.editTextAmount.text.toString()
                if (amountStr.isNotEmpty()) {
                    try {
                        // Format to two decimal places
                        val amount = amountStr.toDouble()
                        binding.editTextAmount.setText(String.format("%.2f", amount))
                    } catch (e: Exception) {
                        binding.editTextAmount.error = "Invalid amount"
                    }
                }
            }
        }
    }
    
    private fun getSelectedCurrency(): String {
        val position = binding.spinnerCurrency.selectedItemPosition
        return currencies[position]
    }
    
    private fun convertAmount(amount: Double, fromCurrency: String, toCurrency: String) {
        // Cancel any ongoing conversion
        conversionJob?.cancel()
        
        // Show loading indicator
        binding.progressBar.visibility = View.VISIBLE
        
        conversionJob = lifecycleScope.launch {
            try {
                val authToken = sessionManager.getToken()
                if (authToken.isNullOrEmpty()) {
                    Toast.makeText(
                        this@EditIncomeActivity,
                        "Error: Not logged in",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.progressBar.visibility = View.GONE
                    return@launch
                }
                
                val convertedAmount = CurrencyUtils.convertCurrency(
                    amount, 
                    fromCurrency, 
                    toCurrency, 
                    authToken
                )
                
                if (convertedAmount != null) {
                    binding.editTextAmount.setText(String.format("%.2f", convertedAmount))
                    Toast.makeText(
                        this@EditIncomeActivity,
                        "Converted ${CurrencyUtils.formatAmount(amount)} $fromCurrency to ${CurrencyUtils.formatAmount(convertedAmount)} $toCurrency",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@EditIncomeActivity,
                        "Conversion failed. Please enter amount manually.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@EditIncomeActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
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
        binding.editTextDate.setText(sdf.format(calendar.time))
    }

    private fun fetchIncomeDetails() {
        val token = sessionManager.fetchAuthToken()
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            when (val result = incomeRepository.getIncomeById(incomeId, token)) {
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val income = result.data
                    binding.editTextSource.setText(income.source)
                    binding.editTextDate.setText(income.date)
                    
                    // Always prioritize showing the true original values if they exist
                    if (income.originalAmount != null && income.originalCurrency != null) {
                        // If we have original values, show those instead
                        binding.editTextAmount.setText(income.originalAmount.toString())
                        originalAmount = income.originalAmount
                        originalCurrency = income.originalCurrency
                        
                        // Set spinner to original currency
                        val originalPosition = currencies.indexOf(income.originalCurrency)
                        if (originalPosition >= 0) {
                            binding.spinnerCurrency.setSelection(originalPosition)
                        }
                    } else {
                        // No original values available, use the stored ones
                        binding.editTextAmount.setText(income.amount.toString())
                        originalAmount = income.amount
                        originalCurrency = income.currency
                        
                        val position = currencies.indexOf(income.currency ?: userDefaultCurrency)
                        if (position >= 0) {
                            binding.spinnerCurrency.setSelection(position)
                        }
                    }
                    
                    // Log for debugging
                    Log.d("EditIncomeActivity", "Income: amount=${income.amount}, currency=${income.currency}")
                    Log.d("EditIncomeActivity", "Original: amount=${income.originalAmount}, currency=${income.originalCurrency}")
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@EditIncomeActivity, "Error: ${result.message}", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is Resource.Loading -> {
                    // Keep progress bar visible during loading
                    binding.progressBar.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun updateIncome() {
        val source = binding.editTextSource.text.toString()
        val date = binding.editTextDate.text.toString()
        val amountStr = binding.editTextAmount.text.toString()
        val currency = getSelectedCurrency()

        if (source.isEmpty() || date.isEmpty() || amountStr.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
            return
        }
        
        val amount = try {
            amountStr.toDouble()
        } catch (e: Exception) {
            binding.editTextAmount.error = "Invalid amount"
            return
        }

        val token = sessionManager.fetchAuthToken()
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false
        
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
                        binding.progressBar.visibility = View.GONE
                        binding.btnSave.isEnabled = true
                        Toast.makeText(
                            this@EditIncomeActivity,
                            "Currency conversion failed. Try again or use your default currency.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                    Toast.makeText(
                        this@EditIncomeActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
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
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@EditIncomeActivity, "Income updated successfully", Toast.LENGTH_SHORT).show()
                finish() // Go back to previous screen
            }
            is Resource.Error -> {
                binding.progressBar.visibility = View.GONE
                binding.btnSave.isEnabled = true
                Toast.makeText(this@EditIncomeActivity, "Error: ${result.message}", Toast.LENGTH_SHORT).show()
            }
            is Resource.Loading -> {
                // Keep progress bar visible during loading
                binding.progressBar.visibility = View.VISIBLE
            }
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Income")
            .setMessage("Are you sure you want to delete this income record? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteIncome()
            }
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun deleteIncome() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false
        binding.btnDelete.isEnabled = false

        lifecycleScope.launch {
            val token = sessionManager.fetchAuthToken() ?: ""
            when (val result = incomeRepository.deleteIncome(incomeId, token)) {
                is Resource.Success -> {
                    Toast.makeText(this@EditIncomeActivity, "Income deleted successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                    binding.btnDelete.isEnabled = true
                    Toast.makeText(this@EditIncomeActivity, "Error: ${result.message}", Toast.LENGTH_SHORT).show()
                }
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
            }
        }
    }
}