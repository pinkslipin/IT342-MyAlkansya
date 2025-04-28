package com.example.myalkansyamobile.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myalkansyamobile.R
import com.example.myalkansyamobile.model.Budget
import java.text.NumberFormat
import java.util.*

class BudgetAdapter(
    private var budgetList: List<Budget>,
    private val onItemClick: (Budget) -> Unit,
    private var defaultCurrency: String = "PHP"
) : RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder>() {

    class BudgetViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Fixed the ID references to match the actual IDs in item_budget.xml
        val textCategory: TextView = view.findViewById(R.id.tvCategory)
        val textBudget: TextView = view.findViewById(R.id.tvBudgetAmount)
        val textSpent: TextView = view.findViewById(R.id.tvSpentAmount)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
        val percentageText: TextView = view.findViewById(R.id.tvPercentage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_budget, parent, false)
        return BudgetViewHolder(view)
    }

    override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
        val budget = budgetList[position]
        
        holder.textCategory.text = budget.category
        
        // Format amounts with appropriate currency symbols
        val formatter = getCurrencyFormatter(budget.currency)
        holder.textBudget.text = formatter.format(budget.monthlyBudget)
        holder.textSpent.text = formatter.format(budget.totalSpent)
        
        // Set progress bar value
        val progressPercentage = budget.getSpendingPercentage()
        holder.progressBar.progress = progressPercentage
        
        // Set percentage text
        holder.percentageText.text = "$progressPercentage%"
        
        // Set text color based on spending behavior
        val context = holder.itemView.context
        if (budget.totalSpent > budget.monthlyBudget) {
            holder.textSpent.setTextColor(context.getColor(R.color.red))
            holder.percentageText.setTextColor(context.getColor(R.color.red))
        } else {
            holder.textSpent.setTextColor(context.getColor(R.color.green))
            holder.percentageText.setTextColor(context.getColor(R.color.green))
        }
        
        // Set click listener
        holder.itemView.setOnClickListener {
            onItemClick(budget)
        }
    }

    override fun getItemCount() = budgetList.size
    
    fun updateData(newList: List<Budget>) {
        budgetList = newList
        notifyDataSetChanged()
    }
    
    fun updateDefaultCurrency(newCurrency: String) {
        defaultCurrency = newCurrency
        notifyDataSetChanged()
    }
    
    private fun getCurrencyFormatter(currencyCode: String): NumberFormat {
        val formatter = NumberFormat.getCurrencyInstance()
        try {
            formatter.currency = Currency.getInstance(currencyCode)
        } catch (e: Exception) {
            formatter.currency = Currency.getInstance(defaultCurrency)
        }
        return formatter
    }
}
