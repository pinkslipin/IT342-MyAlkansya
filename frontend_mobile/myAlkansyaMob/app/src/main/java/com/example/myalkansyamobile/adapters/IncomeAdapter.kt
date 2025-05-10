package com.example.myalkansyamobile.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myalkansyamobile.R
import com.example.myalkansyamobile.model.Income
import com.example.myalkansyamobile.utils.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.*

class IncomeAdapter(
    private var incomeList: MutableList<Income>, // Change to mutableList parameter
    private val onEditClick: (Income) -> Unit,
    private val onDeleteClick: (Income) -> Unit,
    private var defaultCurrency: String = "PHP"
) : RecyclerView.Adapter<IncomeAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    // Modified ViewHolder class to extend RecyclerView.ViewHolder
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvSource: TextView = view.findViewById(R.id.tvSource)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        val btnActions: ImageButton = view.findViewById(R.id.btnActions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_income, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val income = incomeList[position]
        
        // Format date
        try {
            val date = dateFormat.parse(income.date)
            if (date != null) {
                holder.tvDate.text = dateFormat.format(date)
            } else {
                holder.tvDate.text = income.date
            }
        } catch (e: Exception) {
            // In case of parsing error, use the original date string
            holder.tvDate.text = income.date
        }
        
        // Set source and amount
        holder.tvSource.text = income.source
        
        // Display amount with proper currency format
        holder.tvAmount.text = CurrencyUtils.formatWithProperCurrency(income.amount, income.currency ?: defaultCurrency)
        
        // Set up the actions popup menu
        holder.btnActions.setOnClickListener { view ->
            val popup = PopupMenu(view.context, view)
            popup.inflate(R.menu.income_actions_menu)
            
            // Set up menu item click listener
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_edit_income -> {
                        onEditClick(income)
                        true
                    }
                    R.id.action_delete_income -> {
                        onDeleteClick(income)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    override fun getItemCount() = incomeList.size
    
    fun updateDefaultCurrency(currency: String) {
        defaultCurrency = currency
        notifyDataSetChanged()
    }

    fun updateList(newList: List<Income>) {
        // Safely update the list
        incomeList.clear()
        incomeList.addAll(newList)
        notifyDataSetChanged()
    }
}