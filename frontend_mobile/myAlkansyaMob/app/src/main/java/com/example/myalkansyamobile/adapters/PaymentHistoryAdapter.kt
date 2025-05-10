package com.example.myalkansyamobile.adapters

import android.util.Log
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

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

    class PaymentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
        Log.d("PaymentAdapter", "Creating view holder")
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_payment_history, parent, false)
        return PaymentViewHolder(view)
    }

    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) {
        val payment = payments[position]
        Log.d("PaymentAdapter", "Binding payment at position $position: ${payment.date} - ${payment.amount}")
        
        // Format date
        holder.tvDate.text = payment.date.format(dateFormatter)
        
        // Format amount with currency
        holder.tvAmount.text = CurrencyUtils.formatWithProperCurrency(payment.amount, payment.currency)
    }

    override fun getItemCount(): Int {
        Log.d("PaymentAdapter", "Payment count: ${payments.size}")
        return payments.size
    }

    fun updatePayments(newPayments: List<Expense>) {
        Log.d("PaymentAdapter", "Updating payments: count=${newPayments.size}")
        this.payments = newPayments
        notifyDataSetChanged()
    }
}
