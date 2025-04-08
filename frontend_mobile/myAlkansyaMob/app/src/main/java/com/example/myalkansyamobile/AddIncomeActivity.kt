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
import com.example.myalkansyamobile.databinding.ActivityAddIncomeBinding
import com.example.myalkansyamobile.model.Income
import com.example.myalkansyamobile.utils.Resource
import com.example.myalkansyamobile.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddIncomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddIncomeBinding
    private val calendar = Calendar.getInstance()
    private lateinit var sessionManager: SessionManager
    private lateinit var incomeRepository: IncomeRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddIncomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        incomeRepository = IncomeRepository(RetrofitClient.incomeApiService)

        // Setup currency spinner
        setupCurrencySpinner()
        
        // Setup date picker
        binding.btnPickDate.setOnClickListener {
            showDatePickerDialog()
        }

        binding.btnAddIncome.setOnClickListener {
            addIncome()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun setupCurrencySpinner() {
        val currencies = arrayOf("USD", "EUR", "GBP", "JPY", "PHP")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCurrency.adapter = adapter
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
        binding.inputDate.setText(sdf.format(calendar.time))
    }

    private fun addIncome() {
        val source = binding.inputSource.text.toString()
        val date = binding.inputDate.text.toString()
        val amount = binding.inputAmount.text.toString().toDoubleOrNull()
        val currency = binding.spinnerCurrency.selectedItem.toString()

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
        val income = Income(source = source, date = date, amount = amount, currency = currency)
        
        lifecycleScope.launch {
            when (val result = incomeRepository.addIncome(income, token)) {
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@AddIncomeActivity, "Income added successfully", Toast.LENGTH_SHORT).show()
                    finish() // Go back to previous screen
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@AddIncomeActivity, "Error: ${result.message}", Toast.LENGTH_SHORT).show()
                }
                is Resource.Loading -> {
                    // Keep progress bar visible during loading
                    binding.progressBar.visibility = View.VISIBLE
                }
            }
        }
    }
}