package com.example.myalkansyamobile.adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myalkansyamobile.R
import com.example.myalkansyamobile.databinding.ItemBudgetBinding
import com.example.myalkansyamobile.model.Budget
import java.text.NumberFormat
import java.util.*

class BudgetAdapter(
    private var budgets: List<Budget>,
    private val onEditClick: (Budget) -> Unit
) : RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder>() {

    fun updateData(newBudgets: List<Budget>) {
        budgets = newBudgets
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
        val binding = ItemBudgetBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BudgetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
        val budget = budgets[position]
        holder.bind(budget)

        // Set click listener on edit button
        holder.binding.btnEdit.setOnClickListener {
            onEditClick(budget)
        }

        // Optionally set click listener on the whole item too
        holder.itemView.setOnClickListener {
            onEditClick(budget)
        }
    }

    override fun getItemCount(): Int = budgets.size

    inner class BudgetViewHolder(val binding: ItemBudgetBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(budget: Budget) {
            // Format currency based on the budget's currency
            val currencyFormat = NumberFormat.getCurrencyInstance()
            currencyFormat.currency = Currency.getInstance(budget.currency)

            binding.tvCategory.text = budget.category
            binding.tvBudgetAmount.text = currencyFormat.format(budget.monthlyBudget)
            binding.tvSpentAmount.text = currencyFormat.format(budget.totalSpent)
            
            // Calculate percentage (extend Budget class if needed)
            val percentage = calculateSpendingPercentage(budget)
            
            // Set color based on percentage (only if progress views exist)
            val progressColor = when {
                percentage > 90 -> Color.RED
                percentage > 70 -> Color.parseColor("#FFC107") // Yellow
                else -> Color.parseColor("#18864F") // Green
            }
            
            // Check if these views exist before using them
            try {
                binding.progressBar?.progress = percentage
                binding.progressBar?.progressTintList = android.content.res.ColorStateList.valueOf(progressColor)
                binding.tvPercentage?.text = "$percentage%"
                binding.tvPercentage?.setTextColor(progressColor)
            } catch (e: Exception) {
                // Views not present in layout
            }
        }
        
        private fun calculateSpendingPercentage(budget: Budget): Int {
            // Add this method as a fallback if Budget doesn't have getSpendingPercentage()
            return if (budget.monthlyBudget > 0)
                (budget.totalSpent * 100 / budget.monthlyBudget).toInt().coerceIn(0, 100)
            else
                0
        }
    }
}
