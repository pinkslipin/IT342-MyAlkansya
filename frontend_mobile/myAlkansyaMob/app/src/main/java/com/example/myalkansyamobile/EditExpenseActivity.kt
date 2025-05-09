package com.example.myalkansyamobile

import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myalkansyamobile.model.Expense
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.api.ExpenseRequest
import com.example.myalkansyamobile.utils.CurrencyUtils
import com.example.myalkansyamobile.utils.SessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.LocalDate
import java.util.*
import androidx.appcompat.app.AlertDialog

class EditExpenseActivity : AppCompatActivity() {
    private lateinit var etSubject: EditText
    private lateinit var tvDate: TextView
    private lateinit var spinnerCategory: Spinner
    private lateinit var etAmount: EditText
    private lateinit var spinnerCurrency: Spinner
    private lateinit var btnPickDate: ImageView
    private lateinit var btnSaveChanges: Button
    private lateinit var btnCancel: Button
    private lateinit var btnDeleteExpense: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var progressBarConversion: ProgressBar
    private lateinit var tvConversionInfo: TextView
    private lateinit var tvCurrencyWarning: TextView
    private lateinit var tvError: TextView
    private lateinit var manualCategoryLayout: LinearLayout
    private lateinit var etManualCategory: EditText

    private lateinit var sessionManager: SessionManager

    private var expenseId: Int = 0
    private var selectedDate = LocalDate.now()
    private var userDefaultCurrency: String = "PHP"
    private var originalAmount: Double? = null
    private var originalCurrency: String? = null
    private var conversionJob: Job? = null

    private val currencies = CurrencyUtils.currencyCodes.toTypedArray()

    private val categories = arrayOf(
        "Food", "Transportation", "Housing", "Utilities",
        "Entertainment", "Healthcare", "Education", "Shopping",
        "Personal Care", "Debt Payment", "Savings", "Other"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_expense)

        sessionManager = SessionManager(this)
        userDefaultCurrency = sessionManager.getCurrency() ?: "PHP"

        initializeUI()

        expenseId = intent.getIntExtra("EXPENSE_ID", 0)
        if (expenseId == 0) {
            Toast.makeText(this, "Invalid expense ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spinnerCategory.adapter = categoryAdapter

        setupCurrencySpinner()
        setupAmountListener()
        setupCategoryListener()

        btnPickDate.setOnClickListener {
            showDatePicker()
        }

        loadExpenseDetails()

        btnSaveChanges.setOnClickListener {
            updateExpense()
        }

        btnCancel.setOnClickListener {
            finish()
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        btnDeleteExpense.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun initializeUI() {
        try {
            etSubject = findViewById(R.id.etSubjectEdit)
            tvDate = findViewById(R.id.tvDateEdit)
            spinnerCategory = findViewById(R.id.spinnerCategoryEdit)
            etAmount = findViewById(R.id.etAmountEdit)
            spinnerCurrency = findViewById(R.id.spinnerCurrencyEdit)
            btnPickDate = findViewById(R.id.btnPickDateEdit)
            btnSaveChanges = findViewById(R.id.btnSaveExpense)
            btnCancel = findViewById(R.id.btnCancelEdit)
            btnDeleteExpense = findViewById(R.id.btnDeleteExpense)
            progressBar = findViewById(R.id.progressBar)
            progressBarConversion = findViewById(R.id.progressBarConversion)
            tvConversionInfo = findViewById(R.id.tvConversionInfo)
            tvCurrencyWarning = findViewById(R.id.tvCurrencyWarning)
            tvError = findViewById(R.id.tvError)
            manualCategoryLayout = findViewById(R.id.manualCategoryLayoutEdit)
            etManualCategory = findViewById(R.id.etManualCategoryEdit)

        } catch (e: Exception) {
            Log.e("EditExpenseActivity", "Error finding views: ${e.message}")
            Toast.makeText(this, "Failed to initialize UI: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupCurrencySpinner() {
        try {
            val currencyItems = CurrencyUtils.currencyCodes.map { code ->
                val isDefault = code == userDefaultCurrency
                val displayText = CurrencyUtils.getCurrencyDisplayText(code) + if (isDefault) " (Default)" else ""
                displayText
            }

            val currencyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencyItems)
            currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCurrency.adapter = currencyAdapter

            val defaultPosition = CurrencyUtils.currencyCodes.indexOf(userDefaultCurrency)
            if (defaultPosition >= 0) {
                spinnerCurrency.setSelection(defaultPosition)
            }

            spinnerCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedCurrency = CurrencyUtils.currencyCodes[position]
                    handleCurrencyChange(selectedCurrency)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        } catch (e: Exception) {
            Log.e("EditExpenseActivity", "Error setting up currency spinner: ${e.message}")
        }
    }

    private fun setupAmountListener() {
        etAmount.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val amountStr = etAmount.text.toString()
                if (amountStr.isNotEmpty()) {
                    try {
                        val amount = amountStr.toDouble()
                        etAmount.setText(String.format("%.2f", amount))
                    } catch (e: Exception) {
                        etAmount.error = "Invalid amount"
                    }
                }
            }
        }
    }

    private fun setupCategoryListener() {
        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (categories[position] == "Other") {
                    manualCategoryLayout.visibility = View.VISIBLE
                } else {
                    manualCategoryLayout.visibility = View.GONE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                manualCategoryLayout.visibility = View.GONE
            }
        }
    }

    private fun getSelectedCurrency(): String {
        val position = spinnerCurrency.selectedItemPosition
        return CurrencyUtils.currencyCodes[position]
    }

    private fun handleCurrencyChange(newCurrency: String) {
        val amountStr = etAmount.text.toString()
        val currentAmount = amountStr.toDoubleOrNull()

        if (currentAmount != null && currentAmount > 0) {
            val selectedCurrency = newCurrency
            val currentCurrency = originalCurrency ?: getSelectedCurrency()

            if (selectedCurrency != currentCurrency) {
                if (originalAmount == null) {
                    originalAmount = currentAmount
                    originalCurrency = currentCurrency
                }

                if (selectedCurrency == originalCurrency) {
                    etAmount.setText(String.format("%.2f", originalAmount))
                    tvConversionInfo.visibility = View.GONE
                } else {
                    convertAmount(currentAmount, currentCurrency, selectedCurrency)
                }
            }
        }

        updateConversionNotification(newCurrency)
    }

    private fun updateConversionNotification(selectedCurrency: String) {
        if (tvCurrencyWarning != null) {
            if (selectedCurrency != userDefaultCurrency) {
                tvCurrencyWarning.text =
                    "Note: This expense will be automatically converted to $userDefaultCurrency when saved."
                tvCurrencyWarning.visibility = View.VISIBLE
            } else {
                tvCurrencyWarning.visibility = View.GONE
            }
        }
    }

    private fun convertAmount(amount: Double, fromCurrency: String, toCurrency: String) {
        conversionJob?.cancel()

        progressBar.visibility = View.VISIBLE
        tvConversionInfo.text = "Converting..."
        tvConversionInfo.visibility = View.VISIBLE

        conversionJob = lifecycleScope.launch {
            try {
                val authToken = sessionManager.getToken()
                if (authToken.isNullOrEmpty()) {
                    tvConversionInfo.text = "Error: Not logged in"
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
                    tvConversionInfo.text = "Converted ${CurrencyUtils.formatAmount(amount)} $fromCurrency to ${CurrencyUtils.formatAmount(convertedAmount)} $toCurrency"
                    tvConversionInfo.visibility = View.VISIBLE
                } else {
                    tvConversionInfo.text = "Conversion failed. Please enter amount manually."
                    tvConversionInfo.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                tvConversionInfo.text = "Error: ${e.message}"
                tvConversionInfo.visibility = View.VISIBLE
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            calendar.set(Calendar.YEAR, selectedDate.year)
            calendar.set(Calendar.MONTH, selectedDate.monthValue - 1)
            calendar.set(Calendar.DAY_OF_MONTH, selectedDate.dayOfMonth)
        } else {
            val dateParts = selectedDate.toString().split("-")
            if (dateParts.size == 3) {
                try {
                    val year = dateParts[0].toInt()
                    val month = dateParts[1].toInt() - 1
                    val day = dateParts[2].toInt()
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, day)
                } catch (e: Exception) {
                    Log.e("EditExpenseActivity", "Error parsing date: ${e.message}")
                }
            }
        }

        val datePickerDialog = DatePickerDialog(this,
            { _, year, month, day ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    selectedDate = LocalDate.of(year, month + 1, day)
                } else {
                    selectedDate = LocalDate.parse(String.format("%04d-%02d-%02d", year, month + 1, day))
                }
                tvDate.text = selectedDate.toString()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadExpenseDetails() {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken()
                if (token == null) {
                    Toast.makeText(this@EditExpenseActivity, "Authentication required", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                val expense = RetrofitClient.expenseApiService.getExpenseById(expenseId, "Bearer $token")

                etSubject.setText(expense.subject)
                tvDate.text = expense.date
                selectedDate = LocalDate.parse(expense.date)
                etAmount.setText(expense.amount.toString())

                originalCurrency = expense.currency
                originalAmount = expense.amount

                val categoryPosition = categories.indexOf(expense.category)
                if (categoryPosition >= 0) {
                    spinnerCategory.setSelection(categoryPosition)
                    manualCategoryLayout.visibility = View.GONE
                } else {
                    spinnerCategory.setSelection(categories.indexOf("Other"))
                    etManualCategory.setText(expense.category)
                    manualCategoryLayout.visibility = View.VISIBLE
                }

                val currencyPosition = CurrencyUtils.currencyCodes.indexOf(expense.currency)
                if (currencyPosition >= 0) {
                    spinnerCurrency.setSelection(currencyPosition)
                }

                progressBar.visibility = View.GONE

            } catch (e: HttpException) {
                progressBar.visibility = View.GONE
                if (e.code() == 401) {
                    Toast.makeText(this@EditExpenseActivity, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
                    sessionManager.clearSession()
                    finish()
                } else {
                    Toast.makeText(this@EditExpenseActivity, "Failed to load expense details: ${e.message()}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@EditExpenseActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun updateExpense() {
        val subject = etSubject.text.toString().trim()
        val selectedCategoryPosition = spinnerCategory.selectedItemPosition
        var category = categories[selectedCategoryPosition]

        if (category == "Other") {
            val manualCategory = etManualCategory.text.toString().trim()
            if (manualCategory.isEmpty()) {
                showError("Please specify the category")
                return
            }
            category = manualCategory
        }

        val amountStr = etAmount.text.toString().trim()
        val currency = getSelectedCurrency()

        if (subject.isEmpty()) {
            showError("Subject cannot be empty")
            return
        }

        if (amountStr.isEmpty()) {
            showError("Amount cannot be empty")
            return
        }

        val amount = try {
            amountStr.toDouble()
        } catch (e: NumberFormatException) {
            showError("Invalid amount format")
            return
        }

        if (amount <= 0) {
            showError("Amount must be greater than zero")
            return
        }

        progressBar.visibility = View.VISIBLE

        if (currency != userDefaultCurrency) {
            if (tvCurrencyWarning != null) {
                tvCurrencyWarning.text = "Converting to $userDefaultCurrency before saving..."
                tvCurrencyWarning.visibility = View.VISIBLE
            }

            lifecycleScope.launch {
                try {
                    val token = sessionManager.getToken()
                    if (token == null) {
                        Toast.makeText(this@EditExpenseActivity, "Authentication required", Toast.LENGTH_SHORT).show()
                        progressBar.visibility = View.GONE
                        return@launch
                    }

                    val convertedAmount = CurrencyUtils.convertCurrency(
                        amount,
                        currency,
                        userDefaultCurrency,
                        token
                    )

                    if (convertedAmount != null) {
                        val updatedExpense = Expense(
                            id = expenseId,
                            subject = subject,
                            category = category,
                            date = selectedDate,
                            amount = convertedAmount,
                            currency = userDefaultCurrency,
                            originalAmount = amount,
                            originalCurrency = currency
                        )
                        saveExpenseToServer(updatedExpense)
                    } else {
                        val updatedExpense = Expense(
                            id = expenseId,
                            subject = subject,
                            category = category,
                            date = selectedDate,
                            amount = amount,
                            currency = currency
                        )
                        saveExpenseToServer(updatedExpense)
                    }
                } catch (e: Exception) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@EditExpenseActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            val updatedExpense = Expense(
                id = expenseId,
                subject = subject,
                category = category,
                date = selectedDate,
                amount = amount,
                currency = currency
            )
            saveExpenseToServer(updatedExpense)
        }
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }

    private fun saveExpenseToServer(updatedExpense: Expense) {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken()
                if (token == null) {
                    Toast.makeText(this@EditExpenseActivity, "Authentication required", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                    return@launch
                }

                val expenseRequest = ExpenseRequest.fromExpense(updatedExpense)
                val response = RetrofitClient.expenseApiService.updateExpense(
                    expenseId, 
                    expenseRequest, 
                    "Bearer $token"
                )

                progressBar.visibility = View.GONE

                if (response.isSuccessful && response.body() != null) {
                    Toast.makeText(this@EditExpenseActivity, "Expense updated successfully", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                    Toast.makeText(this@EditExpenseActivity, "Error: $errorMsg", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@EditExpenseActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Delete Expense")
            .setMessage("Are you sure you want to delete this expense? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteExpense()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteExpense() {
        progressBar.visibility = View.VISIBLE
        tvError.visibility = View.GONE  // Hide any previous errors

        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken()
                if (token == null) {
                    Toast.makeText(this@EditExpenseActivity, "Authentication required", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                    return@launch
                }

                val response = RetrofitClient.expenseApiService.deleteExpense(
                    expenseId, 
                    "Bearer $token"
                )

                progressBar.visibility = View.GONE

                if (response.isSuccessful) {
                    // The deletion was successful
                    Toast.makeText(this@EditExpenseActivity, "Expense deleted successfully", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                    showError("Error: $errorMsg")
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                showError("Error: ${e.message}")
                Log.e("EditExpenseActivity", "Delete expense error", e)
            }
        }
    }
}
