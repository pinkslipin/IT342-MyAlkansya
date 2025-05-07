package com.example.myalkansyamobile

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.api.SavingsGoalRequest
import com.example.myalkansyamobile.databinding.ActivityAddSavingsGoalBinding
import com.example.myalkansyamobile.utils.CurrencyUtils
import com.example.myalkansyamobile.api.SavingsGoalResponse
import com.example.myalkansyamobile.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AddSavingsGoalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddSavingsGoalBinding
    private lateinit var sessionManager: SessionManager
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    private var userDefaultCurrency: String = "PHP"
    private var selectedCurrency: String = "PHP"
    private var originalTargetAmount: Double? = null
    private var originalCurrentAmount: Double? = null
    private var originalCurrency: String? = null
    private var conversionJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddSavingsGoalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        userDefaultCurrency = sessionManager.getCurrency() ?: "PHP"
        selectedCurrency = userDefaultCurrency

        setupCurrencySpinner()
        setupClickListeners()
        setupDatePicker()
        setupAmountListeners()
        setupCurrencyInfo()
    }

    private fun setupCurrencyInfo() {
        binding.tvCurrencyInfo.text = "Your default currency is $userDefaultCurrency. Goals will be stored in this currency."
    }

    private fun setupCurrencySpinner() {
        val currencyAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            CurrencyUtils.currencyCodes.map { code ->
                val isDefault = code == userDefaultCurrency
                CurrencyUtils.getCurrencyDisplayText(code) + if (isDefault) " (Default)" else ""
            }
        )
        currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCurrency.adapter = currencyAdapter
        
        val defaultPosition = CurrencyUtils.currencyCodes.indexOf(userDefaultCurrency)
        if (defaultPosition >= 0) {
            binding.spinnerCurrency.setSelection(defaultPosition)
        }
        
        binding.spinnerCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val newCurrency = CurrencyUtils.currencyCodes[position]
                handleCurrencyChange(newCurrency)
                selectedCurrency = newCurrency
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun handleCurrencyChange(newCurrency: String) {
        if (newCurrency != userDefaultCurrency) {
            binding.tvCurrencyWarning.text = 
                "Note: This goal will be automatically converted to $userDefaultCurrency when saved."
            binding.tvCurrencyWarning.visibility = View.VISIBLE
        } else {
            binding.tvCurrencyWarning.visibility = View.GONE
        }
        
        val targetAmountStr = binding.editTextTargetAmount.text.toString()
        val targetAmount = targetAmountStr.toDoubleOrNull()
        
        if (targetAmount != null && targetAmount > 0) {
            val oldCurrency = originalCurrency ?: userDefaultCurrency
            
            if (newCurrency != oldCurrency) {
                if (originalTargetAmount == null) {
                    originalTargetAmount = targetAmount
                    originalCurrency = oldCurrency
                }
                
                if (newCurrency == originalCurrency) {
                    binding.editTextTargetAmount.setText(String.format("%.2f", originalTargetAmount))
                } else {
                    convertAmount(targetAmount, oldCurrency, newCurrency, isTarget = true)
                }
            }
        }
        
        val currentAmountStr = binding.editTextCurrentAmount.text.toString()
        val currentAmount = currentAmountStr.toDoubleOrNull()
        
        if (currentAmount != null && currentAmount > 0) {
            val oldCurrency = originalCurrency ?: userDefaultCurrency
            
            if (newCurrency != oldCurrency) {
                if (originalCurrentAmount == null) {
                    originalCurrentAmount = currentAmount
                }
                
                if (newCurrency == originalCurrency) {
                    binding.editTextCurrentAmount.setText(String.format("%.2f", originalCurrentAmount))
                } else {
                    convertAmount(currentAmount, oldCurrency, newCurrency, isTarget = false)
                }
            }
        }
    }

    private fun setupAmountListeners() {
        binding.editTextTargetAmount.setOnFocusChangeListener { _, hasFocus -> 
            if (!hasFocus) {
                val amountStr = binding.editTextTargetAmount.text.toString()
                if (amountStr.isNotEmpty()) {
                    try {
                        val amount = amountStr.toDouble()
                        binding.editTextTargetAmount.setText(String.format("%.2f", amount))
                    } catch (e: Exception) {
                        binding.editTextTargetAmount.error = "Invalid amount"
                    }
                }
            }
        }
        
        binding.editTextCurrentAmount.setOnFocusChangeListener { _, hasFocus -> 
            if (!hasFocus) {
                val amountStr = binding.editTextCurrentAmount.text.toString()
                if (amountStr.isNotEmpty()) {
                    try {
                        val amount = amountStr.toDouble()
                        binding.editTextCurrentAmount.setText(String.format("%.2f", amount))
                    } catch (e: Exception) {
                        binding.editTextCurrentAmount.error = "Invalid amount"
                    }
                }
            }
        }
    }

    private fun convertAmount(amount: Double, fromCurrency: String, toCurrency: String, isTarget: Boolean) {
        conversionJob?.cancel()
        
        binding.progressBar.visibility = View.VISIBLE
        
        conversionJob = lifecycleScope.launch {
            try {
                val authToken = sessionManager.getToken()
                if (authToken.isNullOrEmpty()) {
                    Toast.makeText(
                        this@AddSavingsGoalActivity,
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
                    if (isTarget) {
                        binding.editTextTargetAmount.setText(String.format("%.2f", convertedAmount))
                    } else {
                        binding.editTextCurrentAmount.setText(String.format("%.2f", convertedAmount))
                    }
                    
                    Toast.makeText(
                        this@AddSavingsGoalActivity,
                        "Converted ${CurrencyUtils.formatAmount(amount)} $fromCurrency to ${CurrencyUtils.formatAmount(convertedAmount)} $toCurrency",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@AddSavingsGoalActivity,
                        "Conversion failed. Please enter amount manually.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@AddSavingsGoalActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }

        binding.btnSaveGoal.setOnClickListener {
            validateAndSaveGoal()
        }
        
        binding.btnPickDate.setOnClickListener {
            showDatePickerDialog()
        }
    }

    private fun setupDatePicker() {
        binding.editTextTargetDate.setOnClickListener {
            showDatePickerDialog()
        }
        
        // Set default text
        binding.editTextTargetDate.setText("Select Date")
    }

    private fun showDatePickerDialog() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                val formattedDate = displayDateFormat.format(calendar.time)
                binding.editTextTargetDate.setText(formattedDate)
            },
            year,
            month,
            day
        )

        datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
        datePickerDialog.show()
    }

    private fun validateAndSaveGoal() {
        val goalName = binding.editTextGoalName.text.toString().trim()
        val targetAmountText = binding.editTextTargetAmount.text.toString().trim()
        val currentAmountText = binding.editTextCurrentAmount.text.toString().trim()
        val targetDate = binding.editTextTargetDate.text.toString().trim()

        if (goalName.isEmpty()) {
            binding.editTextGoalName.error = "Please enter a goal name"
            binding.editTextGoalName.requestFocus()
            return
        }

        if (targetAmountText.isEmpty()) {
            binding.editTextTargetAmount.error = "Please enter a target amount"
            binding.editTextTargetAmount.requestFocus()
            return
        }

        if (targetDate.isEmpty()) {
            binding.editTextTargetDate.error = "Please select a target date"
            binding.editTextTargetDate.requestFocus()
            return
        }

        val targetAmount = targetAmountText.toDoubleOrNull() ?: 0.0
        val currentAmount = currentAmountText.toDoubleOrNull() ?: 0.0

        if (targetAmount <= 0) {
            binding.editTextTargetAmount.error = "Target amount must be greater than 0"
            binding.editTextTargetAmount.requestFocus()
            return
        }

        if (currentAmount < 0) {
            binding.editTextCurrentAmount.error = "Current amount cannot be negative"
            binding.editTextCurrentAmount.requestFocus()
            return
        }

        if (currentAmount > targetAmount) {
            binding.editTextCurrentAmount.error = "Current amount cannot exceed target amount"
            binding.editTextCurrentAmount.requestFocus()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSaveGoal.isEnabled = false

        if (selectedCurrency != userDefaultCurrency) {
            lifecycleScope.launch {
                try {
                    val token = sessionManager.getToken() ?: ""
                    
                    val convertedTargetAmount = CurrencyUtils.convertCurrency(
                        targetAmount,
                        selectedCurrency,
                        userDefaultCurrency,
                        token
                    )
                    
                    val convertedCurrentAmount = CurrencyUtils.convertCurrency(
                        currentAmount,
                        selectedCurrency,
                        userDefaultCurrency,
                        token
                    )
                    
                    if (convertedTargetAmount != null && convertedCurrentAmount != null) {
                        saveGoal(
                            goalName, 
                            convertedTargetAmount, 
                            convertedCurrentAmount, 
                            calendar.time, 
                            userDefaultCurrency,
                            originalTargetAmount = targetAmount,
                            originalCurrentAmount = currentAmount,
                            originalCurrency = selectedCurrency
                        )
                    } else {
                        binding.progressBar.visibility = View.GONE
                        binding.btnSaveGoal.isEnabled = true
                        showError("Currency conversion failed. Try again or use your default currency.")
                    }
                } catch (e: Exception) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSaveGoal.isEnabled = true
                    showError("Error: ${e.message}")
                }
            }
        } else {
            saveGoal(goalName, targetAmount, currentAmount, calendar.time, selectedCurrency)
        }
    }

    private fun saveGoal(
        goalName: String,
        targetAmount: Double,
        currentAmount: Double,
        targetDate: Date,
        currency: String,
        originalTargetAmount: Double? = null,
        originalCurrentAmount: Double? = null,
        originalCurrency: String? = null
    ) {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    showError("You must be logged in")
                    finish()
                    return@launch
                }

                val dateString = dateFormat.format(targetDate)
                val savingsGoalRequest = SavingsGoalRequest(
                    goal = goalName,
                    targetAmount = targetAmount,
                    currentAmount = currentAmount,
                    targetDate = dateString,
                    currency = currency,
                    originalTargetAmount = originalTargetAmount,
                    originalCurrentAmount = originalCurrentAmount,
                    originalCurrency = originalCurrency
                )

                val bearerToken = "Bearer $token"
                withContext(Dispatchers.IO) {
                    RetrofitClient.savingsGoalApiService
                        .createSavingsGoal(savingsGoalRequest, bearerToken)
                }

                Toast.makeText(
                    this@AddSavingsGoalActivity,
                    "Savings goal created successfully",
                    Toast.LENGTH_SHORT
                ).show()
                finish()

            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.btnSaveGoal.isEnabled = true
                showError("Error creating savings goal: ${e.message}")
            }
        }
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
