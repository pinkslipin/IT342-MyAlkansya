package com.example.myalkansyamobile.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myalkansyamobile.R
import com.example.myalkansyamobile.model.Income

class IncomeAdapter(private val incomeList: List<Income>, private val onItemClick: (Income) -> Unit) : RecyclerView.Adapter<IncomeAdapter.IncomeViewHolder>() {

    inner class IncomeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sourceTextView: TextView = itemView.findViewById(R.id.txtSource)
        val dateTextView: TextView = itemView.findViewById(R.id.txtDate)
        val amountTextView: TextView = itemView.findViewById(R.id.txtAmount)

        init {
            itemView.setOnClickListener {
                onItemClick(incomeList[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IncomeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.income_item, parent, false)
        return IncomeViewHolder(view)
    }

    override fun onBindViewHolder(holder: IncomeViewHolder, position: Int) {
        val income = incomeList[position]
        holder.sourceTextView.text = income.source
        holder.dateTextView.text = income.date.toString()
        holder.amountTextView.text = String.format("%.2f %s", income.amount, income.currency)
    }

    override fun getItemCount(): Int {
        return incomeList.size
    }
}