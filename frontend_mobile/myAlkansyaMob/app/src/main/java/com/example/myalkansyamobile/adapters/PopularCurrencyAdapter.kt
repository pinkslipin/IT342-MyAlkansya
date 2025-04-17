package com.example.myalkansyamobile.adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.myalkansyamobile.R
import com.example.myalkansyamobile.model.PopularCurrency
import java.text.NumberFormat

class PopularCurrencyAdapter(
    context: Context,
    currencies: List<PopularCurrency>
) : ArrayAdapter<PopularCurrency>(context, 0, currencies) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var itemView = convertView
        
        if (itemView == null) {
            itemView = LayoutInflater.from(context).inflate(
                R.layout.item_popular_currency, parent, false
            )
        }
        
        val currency = getItem(position) ?: return itemView!!
        
        // Get views
        val codeTextView = itemView!!.findViewById<TextView>(R.id.tvCurrencyCode)
        val nameTextView = itemView.findViewById<TextView>(R.id.tvCurrencyName)
        val rateTextView = itemView.findViewById<TextView>(R.id.tvExchangeRate)
        val changeTextView = itemView.findViewById<TextView>(R.id.tvChangePercent)
        
        // Set currency code and name
        codeTextView.text = currency.code
        nameTextView.text = currency.name
        
        // Format the rate
        val formatter = NumberFormat.getNumberInstance()
        formatter.minimumFractionDigits = 4
        formatter.maximumFractionDigits = 4
        rateTextView.text = formatter.format(currency.rate)
        
        // Format the change percentage and set appropriate color
        val changeText = if (currency.changePercent > 0) "↑" else if (currency.changePercent < 0) "↓" else "="
        val changePercent = String.format("%.2f%%", Math.abs(currency.changePercent))
        changeTextView.text = "$changeText $changePercent"
        
        val changeColor = when {
            currency.changePercent > 0 -> Color.GREEN
            currency.changePercent < 0 -> Color.RED
            else -> Color.GRAY
        }
        changeTextView.setTextColor(changeColor)
        
        return itemView
    }
}
