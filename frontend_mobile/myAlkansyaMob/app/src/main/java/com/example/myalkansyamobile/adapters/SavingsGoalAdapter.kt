package com.example.myalkansyamobile.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myalkansyamobile.R
import com.example.myalkansyamobile.model.SavingsGoal

class SavingsGoalAdapter(
    private var savingsGoals: List<SavingsGoal>,
    private val onEditClicked: (savingsGoal: SavingsGoal) -> Unit
) : RecyclerView.Adapter<SavingsGoalAdapter.SavingsGoalViewHolder>() {

    class SavingsGoalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtGoalName: TextView = view.findViewById(R.id.txtGoalName)
        val txtTargetAmount: TextView = view.findViewById(R.id.txtTargetAmount)
        val txtCurrentAmount: TextView = view.findViewById(R.id.txtCurrentAmount)
        val txtTargetDate: TextView = view.findViewById(R.id.txtTargetDate)
        val txtDaysRemaining: TextView = view.findViewById(R.id.txtDaysRemaining)
        val progressBarSavings: ProgressBar = view.findViewById(R.id.progressBarSavings)
        val txtProgressPercentage: TextView = view.findViewById(R.id.txtProgressPercentage)
        val txtAmountRemaining: TextView = view.findViewById(R.id.txtAmountRemaining)
        val btnEditGoal: ImageButton = view.findViewById(R.id.btnEditGoal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavingsGoalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_savings_goal, parent, false)
        return SavingsGoalViewHolder(view)
    }

    override fun onBindViewHolder(holder: SavingsGoalViewHolder, position: Int) {
        val savingsGoal = savingsGoals[position]
        
        holder.txtGoalName.text = savingsGoal.goal
        holder.txtTargetAmount.text = savingsGoal.getFormattedTargetAmount()
        holder.txtCurrentAmount.text = savingsGoal.getFormattedCurrentAmount()
        holder.txtTargetDate.text = savingsGoal.getFormattedTargetDate()
        holder.txtDaysRemaining.text = savingsGoal.getDaysRemainingText()
        
        val progressPercentage = savingsGoal.getProgressPercentage()
        holder.progressBarSavings.progress = progressPercentage
        holder.txtProgressPercentage.text = "$progressPercentage% Complete"
        
        if (savingsGoal.currentAmount >= savingsGoal.targetAmount) {
            holder.txtAmountRemaining.text = "Goal reached! 🎉"
        } else {
            holder.txtAmountRemaining.text = "${savingsGoal.getFormattedRemainingAmount()} to go"
        }
        
        holder.btnEditGoal.setOnClickListener {
            onEditClicked(savingsGoal)
        }
    }

    override fun getItemCount() = savingsGoals.size

    fun updateData(newSavingsGoals: List<SavingsGoal>) {
        savingsGoals = newSavingsGoals
        notifyDataSetChanged()
    }
}
