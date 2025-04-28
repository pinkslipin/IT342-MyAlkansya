package com.example.myalkansyamobile

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.api.SavingsGoalRequest
import com.example.myalkansyamobile.databinding.ActivityEditSavingsGoalBinding
import com.example.myalkansyamobile.utils.CurrencyUtils
import com.example.myalkansyamobile.utils.SessionManager
import com.example.myalkansyamobile.api.SavingsGoalResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class EditSavingsGoalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditSavingsGoalBinding
    private lateinit var sessionManager: SessionManager
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private var goalId: Int = -1
    private lateinit var currentGoal: SavingsGoalResponse
    
    private var userDefaultCurrency: String = "PHP"
    private var selectedCurrency: String = "PHP"
    private var originalTargetAmount: Double? = null
    private var originalCurrentAmount: Double? = null
    private var originalCurrency: String? = null
    private var conversionJob: Job? = null

    companion object {
        const val EXTRA_GOAL_ID = "extra_goal_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditSavingsGoalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        userDefaultCurrency = sessionManager.getCurrency() ?: "PHP"
        goalId = intent.getIntExtra(EXTRA_GOAL_ID, -1)

        if (goalId == -1) {
            Toast.makeText(this, "Invalid savings goal", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupCurrencySpinner()
        setupClickListeners()
        setupDatePicker()
        setupAmountListeners()
        fetchSavingsGoalDetails()
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
                        this@EditSavingsGoalActivity,
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
                        this@EditSavingsGoalActivity,
                        "Converted ${CurrencyUtils.formatAmount(amount)} $fromCurrency to ${CurrencyUtils.formatAmount(convertedAmount)} $toCurrency",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@EditSavingsGoalActivity,
                        "Conversion failed. Please enter amount manually.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@EditSavingsGoalActivity,
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
            validateAndUpdateGoal()
        }

        binding.btnDeleteGoal.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun setupDatePicker() {
        binding.editTextTargetDate.setOnClickListener {
            showDatePickerDialog()
        }
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

    private fun fetchSavingsGoalDetails() {
        binding.progressLoading.visibility = View.VISIBLE
        binding.cardForm.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    showError("You must be logged in")
                    finish()
                    return@launch
                }

                val bearerToken = "Bearer $token"
                currentGoal = withContext(Dispatchers.IO) {
                    RetrofitClient.savingsGoalApiService
                        .getSavingsGoalById(goalId, bearerToken)
                }

                try {
                    val date = dateFormat.parse(currentGoal.targetDate)
                    if (date != null) {
                        calendar.time = date
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                if (currentGoal.originalCurrency != null) {
                    originalCurrency = currentGoal.originalCurrency
                    
                    if (currentGoal.originalTargetAmount != null) {
                        originalTargetAmount = currentGoal.originalTargetAmount
                    }
                    
                    if (currentGoal.originalCurrentAmount != null) {
                        originalCurrentAmount = currentGoal.originalCurrentAmount
                    }
                    
                    val currencyPosition = CurrencyUtils.currencyCodes.indexOf(originalCurrency)
                    if (currencyPosition >= 0) {
                        binding.spinnerCurrency.setSelection(currencyPosition)
                        selectedCurrency = originalCurrency ?: currentGoal.currency
                    }
                    
                    binding.editTextTargetAmount.setText(
                        String.format("%.2f", originalTargetAmount ?: currentGoal.targetAmount)
                    )
                    
                    binding.editTextCurrentAmount.setText(
                        String.format("%.2f", originalCurrentAmount ?: currentGoal.currentAmount)
                    )
                } else {
                    binding.editTextTargetAmount.setText(currentGoal.targetAmount.toString())
                    binding.editTextCurrentAmount.setText(currentGoal.currentAmount.toString())
                    
                    val currencyPosition = CurrencyUtils.currencyCodes.indexOf(currentGoal.currency)
                    if (currencyPosition >= 0) {
                        binding.spinnerCurrency.setSelection(currencyPosition)
                        selectedCurrency = currentGoal.currency
                    }
                }
                
                binding.editTextGoalName.setText(currentGoal.goal)
                binding.editTextTargetDate.setText(displayDateFormat.format(calendar.time))
                
                binding.progressLoading.visibility = View.GONE
                binding.cardForm.visibility = View.VISIBLE

            } catch (e: Exception) {
                e.printStackTrace()
                showError("Error loading savings goal: ${e.message}")
                finish()
            }
        }
    }

    private fun validateAndUpdateGoal() {
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
                        updateGoal(
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
            updateGoal(goalName, targetAmount, currentAmount, calendar.time, selectedCurrency)
        }
    }

    private fun updateGoal(
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
                        .updateSavingsGoal(goalId, savingsGoalRequest, bearerToken)
                }

                Toast.makeText(
                    this@EditSavingsGoalActivity,
                    "Savings goal updated successfully",
                    Toast.LENGTH_SHORT
                ).show()
                finish()

            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.btnSaveGoal.isEnabled = true
                showError("Error updating savings goal: ${e.message}")
            }
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Savings Goal")
            .setMessage("Are you sure you want to delete this savings goal?")
            .setPositiveButton("Delete") { dialog, _ ->
                deleteGoal()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteGoal() {
        binding.progressLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    showError("You must be logged in")
                    finish()
                    return@launch
                }

                val bearerToken = "Bearer $token"
                withContext(Dispatchers.IO) {
                    RetrofitClient.savingsGoalApiService
                        .deleteSavingsGoal(goalId, bearerToken)
                }

                Toast.makeText(
                    this@EditSavingsGoalActivity,
                    "Savings goal deleted successfully",
                    Toast.LENGTH_SHORT
                ).show()
                finish()

            } catch (e: Exception) {
                binding.progressLoading.visibility = View.GONE
                showError("Error deleting savings goal: ${e.message}")
            }
        }
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
