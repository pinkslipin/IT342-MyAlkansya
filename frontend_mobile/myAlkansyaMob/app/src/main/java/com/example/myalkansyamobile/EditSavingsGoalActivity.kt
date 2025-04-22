package com.example.myalkansyamobile

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.api.SavingsGoalRequest
import com.example.myalkansyamobile.databinding.ActivityEditSavingsGoalBinding
import com.example.myalkansyamobile.utils.SessionManager
import kotlinx.coroutines.Dispatchers
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

    companion object {
        const val EXTRA_GOAL_ID = "extra_goal_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditSavingsGoalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        goalId = intent.getIntExtra(EXTRA_GOAL_ID, -1)

        if (goalId == -1) {
            Toast.makeText(this, "Invalid savings goal", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupClickListeners()
        setupDatePicker()
        fetchSavingsGoalDetails()
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

        // Set minimum date to today
        datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
        datePickerDialog.show()
    }

    private fun fetchSavingsGoalDetails() {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    Toast.makeText(this@EditSavingsGoalActivity, "You must be logged in", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                val bearerToken = "Bearer $token"
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.savingsGoalApiService
                        .getSavingsGoalById(goalId, bearerToken)
                }

                // Parse the date
                try {
                    val date = dateFormat.parse(response.targetDate)
                    if (date != null) {
                        calendar.time = date
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Set the values to the form
                binding.editTextGoalName.setText(response.goal)
                binding.editTextTargetAmount.setText(response.targetAmount.toString())
                binding.editTextCurrentAmount.setText(response.currentAmount.toString())
                binding.editTextTargetDate.setText(displayDateFormat.format(calendar.time))

                // Set currency spinner
                val currencies = resources.getStringArray(R.array.currency_options)
                val currencyIndex = currencies.indexOf(response.currency)
                if (currencyIndex != -1) {
                    binding.spinnerCurrency.setSelection(currencyIndex)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@EditSavingsGoalActivity,
                    "Error loading savings goal: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun validateAndUpdateGoal() {
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

        // Update the goal
        updateGoal(goalName, targetAmount, currentAmount, calendar.time, currency)
    }

    private fun updateGoal(
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
                        this@EditSavingsGoalActivity,
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
                e.printStackTrace()
                Toast.makeText(
                    this@EditSavingsGoalActivity,
                    "Error updating savings goal: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
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
        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    Toast.makeText(
                        this@EditSavingsGoalActivity,
                        "You must be logged in",
                        Toast.LENGTH_SHORT
                    ).show()
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
                e.printStackTrace()
                Toast.makeText(
                    this@EditSavingsGoalActivity,
                    "Error deleting savings goal: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
