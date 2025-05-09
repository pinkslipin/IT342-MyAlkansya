package com.example.myalkansyamobile.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myalkansyamobile.R
import com.example.myalkansyamobile.model.Expense
import com.example.myalkansyamobile.model.SavingsGoal
import java.text.NumberFormat
import java.util.*
import kotlin.math.abs

class SavingsGoalAdapter(
    private var goals: List<SavingsGoal>,
    private val onItemClick: (SavingsGoal, Int) -> Unit
) : RecyclerView.Adapter<SavingsGoalAdapter.SavingsGoalViewHolder>() {
    
    private var expensesForGoals: Map<Int, List<Expense>> = emptyMap()
    
    // Action constants
    companion object {
        const val ACTION_EDIT = 1
        const val ACTION_ADD_PAYMENT = 2
        const val ACTION_VIEW_HISTORY = 3
    }

    class SavingsGoalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val goalNameText: TextView = view.findViewById(R.id.txtGoalName)
        val targetAmountText: TextView = view.findViewById(R.id.txtTargetAmount)
        val currentAmountText: TextView = view.findViewById(R.id.txtCurrentAmount)
        val targetDateText: TextView = view.findViewById(R.id.txtTargetDate)
        val daysRemainingText: TextView = view.findViewById(R.id.txtDaysRemaining)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBarSavings)
        val progressText: TextView = view.findViewById(R.id.txtProgressPercentage)
        val amountRemainingText: TextView = view.findViewById(R.id.txtAmountRemaining)
        val moreButton: ImageButton = view.findViewById(R.id.btnMoreOptions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavingsGoalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_savings_goal, parent, false)
        return SavingsGoalViewHolder(view)
    }

    override fun onBindViewHolder(holder: SavingsGoalViewHolder, position: Int) {
        val goal = goals[position]
        
        // Set basic goal data
        holder.goalNameText.text = goal.goal
        
        // Format currency amounts
        val formatter = NumberFormat.getCurrencyInstance()
        try {
            formatter.currency = Currency.getInstance(goal.currency)
        } catch (e: Exception) {
            formatter.currency = Currency.getInstance("PHP")
        }
        
        holder.targetAmountText.text = formatter.format(goal.targetAmount)
        holder.currentAmountText.text = formatter.format(goal.currentAmount)
        
        // Format dates
        holder.targetDateText.text = goal.getFormattedTargetDate()
        
        // Calculate and display days remaining
        val daysRemaining = goal.getRemainingDays()
        holder.daysRemainingText.text = if (daysRemaining < 0) {
            "(${abs(daysRemaining)} days overdue)"
        } else {
            "($daysRemaining days left)"
        }
        
        // Setup progress bar and text
        val progress = goal.getProgressPercentage()
        holder.progressBar.progress = progress
        holder.progressText.text = "$progress% Complete"
        
        // Calculate and format remaining amount
        val remaining = goal.getRemainingAmount()
        holder.amountRemainingText.text = if (remaining > 0) {
            "${formatter.format(remaining)} to go"
        } else {
            "Goal achieved! 🎉"
        }
        
        // Set progress bar color based on progress
        val context = holder.itemView.context
        when {
            progress >= 100 -> {
                holder.progressBar.progressTintList = context.getColorStateList(R.color.green_primary)
            }
            progress >= 75 -> {
                holder.progressBar.progressTintList = context.getColorStateList(R.color.yellow_primary)
            }
            progress >= 50 -> {
                holder.progressBar.progressTintList = context.getColorStateList(R.color.yellow_primary)
            }
            else -> {
                if (daysRemaining < 0) {
                    holder.progressBar.progressTintList = context.getColorStateList(R.color.red)
                } else {
                    holder.progressBar.progressTintList = context.getColorStateList(R.color.green_primary)
                }
            }
        }
        
        // Setup more button with popup menu
        holder.moreButton.setOnClickListener { view ->
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.savings_goal_actions_menu, popup.menu)
            
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_edit_goal -> {
                        onItemClick(goal, ACTION_EDIT)
                        true
                    }
                    R.id.action_add_payment -> {
                        onItemClick(goal, ACTION_ADD_PAYMENT)
                        true
                    }
                    R.id.action_view_history -> {
                        onItemClick(goal, ACTION_VIEW_HISTORY)
                        true
                    }
                    else -> false
                }
            }
            
            popup.show()
        }
        
        // Set click listener for the whole item (optional - you might want to remove this)
        holder.itemView.setOnClickListener {
            onItemClick(goal, ACTION_EDIT)
        }
    }

    override fun getItemCount(): Int = goals.size

    fun updateData(newGoals: List<SavingsGoal>) {
        goals = newGoals
        notifyDataSetChanged()
    }
    
    fun updateExpensesForGoals(newExpensesMap: Map<Int, List<Expense>>) {
        expensesForGoals = newExpensesMap
        notifyDataSetChanged()
    }
}
