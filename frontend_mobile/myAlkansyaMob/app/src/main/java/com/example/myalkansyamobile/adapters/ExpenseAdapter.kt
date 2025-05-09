package com.example.myalkansyamobile.adapters

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.example.myalkansyamobile.R
import com.example.myalkansyamobile.model.Expense
import com.example.myalkansyamobile.utils.CurrencyUtils
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

class ExpenseAdapter(
    private var expenses: List<Expense>,
    private val onItemClick: (Expense) -> Unit
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

    class ExpenseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvSubject: TextView = view.findViewById(R.id.tvSubject)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
        val tvSavingsGoalIndicator: TextView = view.findViewById(R.id.tvSavingsGoalIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = expenses[position]
        
        // Format and display date
        holder.tvDate.text = expense.date.format(dateFormatter)
        
        // Display subject and category
        holder.tvSubject.text = expense.subject
        holder.tvCategory.text = expense.category
        
        // Format and display amount with proper currency
        holder.tvAmount.text = CurrencyUtils.formatWithProperCurrency(expense.amount, expense.currency)
        
        // Check if this expense is linked to a savings goal
        if (expense.category == "Savings Goal") {
            holder.tvSavingsGoalIndicator.visibility = View.VISIBLE
        } else {
            holder.tvSavingsGoalIndicator.visibility = View.GONE
        }
        
        // Set click listener for edit button
        holder.btnEdit.setOnClickListener {
            onItemClick(expense)
        }
        
        // Set click listener for the entire item
        holder.itemView.setOnClickListener {
            onItemClick(expense)
        }
    }

    override fun getItemCount() = expenses.size
    
    fun updateList(newExpenses: List<Expense>) {
        expenses = newExpenses
        notifyDataSetChanged()
    }
}
