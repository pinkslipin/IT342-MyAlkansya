package com.example.myalkansyamobile

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.api.SavingsGoalRequest
import com.example.myalkansyamobile.databinding.ActivityAddSavingsGoalBinding
import com.example.myalkansyamobile.utils.SessionManager
import kotlinx.coroutines.Dispatchers
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddSavingsGoalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        setupClickListeners()
        setupDatePicker()
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

        // Set minimum date to today
        datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
        datePickerDialog.show()
    }

    private fun validateAndSaveGoal() {
        val goalName = binding.editTextGoalName.text.toString().trim()
        val targetAmountText = binding.editTextTargetAmount.text.toString().trim()
        val currentAmountText = binding.editTextCurrentAmount.text.toString().trim()
        val targetDate = binding.editTextTargetDate.text.toString().trim()
        val currency = binding.spinnerCurrency.selectedItem.toString()

        // Validate inputs
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

        // Create request and save
        saveGoal(goalName, targetAmount, currentAmount, calendar.time, currency)
    }

    private fun saveGoal(
        goalName: String,
        targetAmount: Double,
        currentAmount: Double,
        targetDate: Date,
        currency: String
    ) {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    Toast.makeText(
                        this@AddSavingsGoalActivity,
                        "You must be logged in",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                    return@launch
                }

                val dateString = dateFormat.format(targetDate)
                val savingsGoalRequest = SavingsGoalRequest(
                    goal = goalName,
                    targetAmount = targetAmount,
                    currentAmount = currentAmount,
                    targetDate = dateString,
                    currency = currency
                )

                val bearerToken = "Bearer $token"
                withContext(Dispatchers.IO) {
                    RetrofitClient.instance.create(com.example.myalkansyamobile.api.SavingsGoalApiService::class.java)
                        .createSavingsGoal(savingsGoalRequest, bearerToken)
                }

                Toast.makeText(
                    this@AddSavingsGoalActivity,
                    "Savings goal created successfully",
                    Toast.LENGTH_SHORT
                ).show()
                finish()

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@AddSavingsGoalActivity,
                    "Error creating savings goal: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
