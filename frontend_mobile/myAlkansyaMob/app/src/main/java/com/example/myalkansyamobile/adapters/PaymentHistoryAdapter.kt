package com.example.myalkansyamobile.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myalkansyamobile.R
import com.example.myalkansyamobile.model.Expense
import com.example.myalkansyamobile.utils.CurrencyUtils
import java.time.format.DateTimeFormatter

class PaymentHistoryAdapter(
    private var payments: List<Expense>
) : RecyclerView.Adapter<PaymentHistoryAdapter.PaymentViewHolder>() {

    class PaymentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateText: TextView = view.findViewById(R.id.tvDate)
        val descriptionText: TextView = view.findViewById(R.id.tvDescription)
        val amountText: TextView = view.findViewById(R.id.tvAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_payment_history, parent, false)
        return PaymentViewHolder(view)
    }

    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) {
        val payment = payments[position]
        
        // Format date
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
        holder.dateText.text = payment.date.format(formatter)
        
        // Set description
        holder.descriptionText.text = payment.subject
        
        // Format amount with currency using the correct method
        holder.amountText.text = CurrencyUtils.formatWithCurrencySymbol(
            payment.amount,
            payment.currency
        )
    }

    override fun getItemCount(): Int = payments.size

    fun updatePayments(newPayments: List<Expense>) {
        payments = newPayments
        notifyDataSetChanged()
    }
}
