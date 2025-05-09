package com.example.myalkansyamobile.adapters

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.example.myalkansyamobile.R
import com.example.myalkansyamobile.model.Expense
import com.example.myalkansyamobile.utils.CurrencyUtils
import java.time.format.DateTimeFormatter
import java.util.Locale

class ExpenseAdapter(
    private var expenses: List<Expense>,
    private val onEditClick: (Expense) -> Unit,
    private val onDeleteClick: (Expense) -> Unit
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

    class ExpenseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvSubject: TextView = view.findViewById(R.id.tvSubject)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        val btnActions: ImageButton = view.findViewById(R.id.btnActions)
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
        if (expense.isLinkedToSavingsGoal()) {
            holder.tvSavingsGoalIndicator.visibility = View.VISIBLE
        } else {
            holder.tvSavingsGoalIndicator.visibility = View.GONE
        }
        
        // Set up the actions popup menu
        holder.btnActions.setOnClickListener { view ->
            val popup = PopupMenu(view.context, view)
            popup.inflate(R.menu.expense_actions_menu)
            
            // Set up menu item click listener
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_edit_expense -> {
                        onEditClick(expense)
                        true
                    }
                    R.id.action_delete_expense -> {
                        onDeleteClick(expense)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    override fun getItemCount() = expenses.size
    
    fun updateList(newExpenses: List<Expense>) {
        expenses = newExpenses
        notifyDataSetChanged()
    }
}
