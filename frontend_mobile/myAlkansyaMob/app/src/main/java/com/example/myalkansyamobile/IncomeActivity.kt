package com.example.myalkansyamobile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myalkansyamobile.adapters.IncomeAdapter
import com.example.myalkansyamobile.api.IncomeRepository
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.databinding.ActivityIncomeBinding
import com.example.myalkansyamobile.model.Income
import com.example.myalkansyamobile.utils.Resource
import com.example.myalkansyamobile.utils.SessionManager
import kotlinx.coroutines.launch

class IncomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityIncomeBinding
    private lateinit var incomeAdapter: IncomeAdapter
    private val incomeList = mutableListOf<Income>()
    private lateinit var sessionManager: SessionManager
    private lateinit var incomeRepository: IncomeRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIncomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        incomeRepository = IncomeRepository(RetrofitClient.incomeApiService)

        setupRecyclerView()
        fetchIncomes()

        binding.btnAddIncome.setOnClickListener {
            val intent = Intent(this, AddIncomeActivity::class.java)
            startActivity(intent)
        }
        
        // Add menu navigation
        binding.topBar.setOnClickListener {
            val intent = Intent(this, HomePageActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh the income list when returning to this activity
        fetchIncomes()
    }

    private fun setupRecyclerView() {
        incomeAdapter = IncomeAdapter(incomeList) { income ->
            val intent = Intent(this, EditIncomeActivity::class.java)
            intent.putExtra("incomeId", income.id)
            startActivity(intent)
        }
        binding.recyclerViewIncome.apply {
            layoutManager = LinearLayoutManager(this@IncomeActivity)
            adapter = incomeAdapter
        }
    }

    private fun fetchIncomes() {
        val token = sessionManager.fetchAuthToken()
        if (token.isNullOrEmpty()) {
            // If no token, redirect to login
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            when (val result = incomeRepository.getIncomes(token)) {
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    incomeList.clear()
                    incomeList.addAll(result.data)
                    incomeAdapter.notifyDataSetChanged()

                    binding.txtEmptyState.visibility = if (incomeList.isEmpty()) View.VISIBLE else View.GONE
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@IncomeActivity, "Error: ${result.message}", Toast.LENGTH_SHORT).show()
                    binding.txtEmptyState.visibility = View.VISIBLE
                }
                is Resource.Loading -> {
                    // Keep progress bar visible during loading
                    binding.progressBar.visibility = View.VISIBLE
                }
            }
        }
    }
}