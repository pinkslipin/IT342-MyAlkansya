package com.example.myalkansyamobile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myalkansyamobile.adapters.SavingsGoalAdapter
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.api.SavingsGoalResponse
import com.example.myalkansyamobile.databinding.ActivitySavingsGoalsBinding
import com.example.myalkansyamobile.model.SavingsGoal
import com.example.myalkansyamobile.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class SavingsGoalsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySavingsGoalsBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: SavingsGoalAdapter
    private val savingsGoalsList = mutableListOf<SavingsGoal>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavingsGoalsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        setupRecyclerView()
        setupClickListeners()
        fetchSavingsGoals()
    }

    private fun setupRecyclerView() {
        adapter = SavingsGoalAdapter(savingsGoalsList) { savingsGoal: SavingsGoal ->
            // Handle click on edit button
            val intent = Intent(this, EditSavingsGoalActivity::class.java).apply {
                putExtra(EditSavingsGoalActivity.EXTRA_GOAL_ID, savingsGoal.id)
            }
            startActivity(intent)
        }

        binding.recyclerSavingsGoals.apply {
            layoutManager = LinearLayoutManager(this@SavingsGoalsActivity)
            adapter = this@SavingsGoalsActivity.adapter
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnAddSavingsGoal.setOnClickListener {
            val intent = Intent(this, AddSavingsGoalActivity::class.java)
            startActivity(intent)
        }
    }

    private fun fetchSavingsGoals() {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    Toast.makeText(this@SavingsGoalsActivity, "You must be logged in", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                val bearerToken = "Bearer $token"
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.savingsGoalApiService
                        .getAllSavingsGoals(bearerToken)
                }

                // Map API response to our model
                savingsGoalsList.clear()
                savingsGoalsList.addAll(response.map { mapToSavingsGoal(it) })
                adapter.updateData(savingsGoalsList)

                // Show empty state if no goals
                if (savingsGoalsList.isEmpty()) {
                    binding.txtNoGoals.visibility = View.VISIBLE
                    binding.recyclerSavingsGoals.visibility = View.GONE
                } else {
                    binding.txtNoGoals.visibility = View.GONE
                    binding.recyclerSavingsGoals.visibility = View.VISIBLE
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@SavingsGoalsActivity,
                    "Error loading savings goals: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                binding.txtNoGoals.visibility = View.VISIBLE
                binding.recyclerSavingsGoals.visibility = View.GONE
            }
        }
    }

    private fun mapToSavingsGoal(response: SavingsGoalResponse): SavingsGoal {
        val targetDate = try {
            dateFormat.parse(response.targetDate) ?: Date()
        } catch (e: Exception) {
            Date()
        }

        return SavingsGoal(
            id = response.id,
            goal = response.goal,
            targetAmount = response.targetAmount,
            currentAmount = response.currentAmount,
            targetDate = targetDate,
            currency = response.currency
        )
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when coming back to this activity
        fetchSavingsGoals()
    }
}
