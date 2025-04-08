package com.example.myalkansyamobile

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myalkansyamobile.api.IncomeRepository
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.databinding.ActivityEditIncomeBinding
import com.example.myalkansyamobile.model.Income
import com.example.myalkansyamobile.utils.Resource
import com.example.myalkansyamobile.utils.SessionManager
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditIncomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        incomeRepository = IncomeRepository(RetrofitClient.incomeApiService)

        // Setup currency spinner
        setupCurrencySpinner()
        
        // Setup date picker
        binding.btnEditPickDate.setOnClickListener {
            showDatePickerDialog()
        }

        incomeId = intent.getIntExtra("incomeId", -1)
        if (incomeId == -1) {
            Toast.makeText(this, "Invalid income ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        fetchIncomeDetails()

        binding.btnSaveChanges.setOnClickListener {
            updateIncome()
        }

        binding.btnCancelEdit.setOnClickListener {
            finish()
        }
    }

    private fun setupCurrencySpinner() {
        val currencies = arrayOf("USD", "EUR", "GBP", "JPY", "PHP")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.editCurrency.adapter = adapter
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
        binding.editDate.setText(sdf.format(calendar.time))
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
                    binding.editSource.setText(income.source)
                    binding.editDate.setText(income.date)
                    binding.editAmount.setText(income.amount.toString())
                    
                    // Set currency spinner selection
                    val currencies = arrayOf("USD", "EUR", "GBP", "JPY", "PHP")
                    val currencyPosition = currencies.indexOf(income.currency).coerceAtLeast(0)
                    binding.editCurrency.setSelection(currencyPosition)
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
        val source = binding.editSource.text.toString()
        val date = binding.editDate.text.toString()
        val amount = binding.editAmount.text.toString().toDoubleOrNull()
        val currency = binding.editCurrency.selectedItem.toString()

        if (source.isEmpty() || date.isEmpty() || amount == null) {
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val token = sessionManager.fetchAuthToken()
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        binding.progressBar.visibility = View.VISIBLE
        val updatedIncome = Income(id = incomeId, source = source, date = date, amount = amount, currency = currency)
        
        lifecycleScope.launch {
            when (val result = incomeRepository.updateIncome(incomeId, updatedIncome, token)) {
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@EditIncomeActivity, "Income updated successfully", Toast.LENGTH_SHORT).show()
                    finish() // Go back to previous screen
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@EditIncomeActivity, "Error: ${result.message}", Toast.LENGTH_SHORT).show()
                }
                is Resource.Loading -> {
                    // Keep progress bar visible during loading
                    binding.progressBar.visibility = View.VISIBLE
                }
            }
        }
    }
}