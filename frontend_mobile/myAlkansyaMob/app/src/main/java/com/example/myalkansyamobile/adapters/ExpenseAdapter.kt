package com.example.myalkansyamobile.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myalkansyamobile.R
import com.example.myalkansyamobile.model.Expense
import java.text.NumberFormat
import java.util.Locale

class ExpenseAdapter(private val expenseList: List<Expense>, private val onItemClick: (Expense) -> Unit) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    inner class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val subjectTextView: TextView = itemView.findViewById(R.id.txtSubject)
        val categoryTextView: TextView = itemView.findViewById(R.id.txtCategory)
        val dateTextView: TextView = itemView.findViewById(R.id.txtDate)
        val amountTextView: TextView = itemView.findViewById(R.id.txtAmount)

        init {
            itemView.setOnClickListener {
                onItemClick(expenseList[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.expense_item, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = expenseList[position]
        holder.subjectTextView.text = expense.subject
        holder.categoryTextView.text = expense.category
        holder.dateTextView.text = expense.date.toString()
        
        // Format the amount with currency
        val formattedAmount = NumberFormat.getCurrencyInstance(Locale.US).format(expense.amount)
            .replace("$", "${expense.currency} ")  // Replace $ with the actual currency
        holder.amountTextView.text = formattedAmount
    }

    override fun getItemCount(): Int {
        return expenseList.size
    }
}
