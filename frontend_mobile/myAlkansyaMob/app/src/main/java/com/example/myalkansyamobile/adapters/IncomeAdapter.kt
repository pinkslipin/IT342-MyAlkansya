package com.example.myalkansyamobile.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myalkansyamobile.databinding.ItemIncomeBinding
import com.example.myalkansyamobile.model.Income
import com.example.myalkansyamobile.utils.CurrencyUtils
import com.example.myalkansyamobile.utils.DateUtils
import java.text.DecimalFormat

class IncomeAdapter(
    private val incomes: MutableList<Income>,
    private val onItemClick: (Income) -> Unit,
    private var userDefaultCurrency: String
) : RecyclerView.Adapter<IncomeAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemIncomeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemIncomeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val income = incomes[position]
        
        // Set source and date
        holder.binding.tvSource.text = income.source
        holder.binding.tvDate.text = DateUtils.formatDate(income.date)
        
        // Format the amount with currency
        val amount = income.amount
        val currency = income.currency ?: userDefaultCurrency
        
        // Check if this is a converted income (has original values)
        if (income.originalCurrency != null && income.originalAmount != null) {
            // Show converted amount as primary
            holder.binding.tvAmount.text = CurrencyUtils.formatWithCurrency(amount, currency)
            
            // Show original amount as secondary
            holder.binding.tvConvertedAmount.text = 
                "(${CurrencyUtils.formatAmount(income.originalAmount)} ${income.originalCurrency})"
            holder.binding.tvConvertedAmount.visibility = android.view.View.VISIBLE
        } else {
            // Regular income with single currency
            holder.binding.tvAmount.text = CurrencyUtils.formatWithCurrency(amount, currency)
            holder.binding.tvConvertedAmount.visibility = android.view.View.GONE
        }
        
        // Set click listener
        holder.binding.root.setOnClickListener {
            onItemClick(income)
        }
    }
    
    // Add function to update default currency
    fun updateDefaultCurrency(newCurrency: String) {
        userDefaultCurrency = newCurrency
        notifyDataSetChanged()
    }

    override fun getItemCount() = incomes.size
}