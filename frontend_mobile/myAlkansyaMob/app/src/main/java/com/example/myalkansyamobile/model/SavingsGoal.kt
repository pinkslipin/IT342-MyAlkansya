package com.example.myalkansyamobile.model

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

data class SavingsGoal(
    val id: Int,
    val goal: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val targetDate: Date,
    val currency: String = "PHP"
) {
    fun getFormattedTargetAmount(): String {
        val formatter = NumberFormat.getCurrencyInstance()
        formatter.currency = Currency.getInstance(currency)
        return formatter.format(targetAmount)
    }
    
    fun getFormattedCurrentAmount(): String {
        val formatter = NumberFormat.getCurrencyInstance()
        formatter.currency = Currency.getInstance(currency)
        return formatter.format(currentAmount)
    }
    
    fun getFormattedTargetDate(): String {
        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return formatter.format(targetDate)
    }
    
    fun getRemainingDays(): Int {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        
        val targetCalendar = Calendar.getInstance().apply {
            time = targetDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val diffInMillis = targetCalendar.timeInMillis - today.time
        return (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
    }
    
    fun getDaysRemainingText(): String {
        val days = getRemainingDays()
        return if (days < 0) {
            "${abs(days)} days overdue"
        } else {
            "$days days left"
        }
    }
    
    fun getProgressPercentage(): Int {
        if (targetAmount <= 0) return 0
        val progress = (currentAmount / targetAmount) * 100
        return progress.toInt().coerceIn(0, 100)
    }
    
    fun getRemainingAmount(): Double {
        return (targetAmount - currentAmount).coerceAtLeast(0.0)
    }
    
    fun getFormattedRemainingAmount(): String {
        val formatter = NumberFormat.getCurrencyInstance()
        formatter.currency = Currency.getInstance(currency)
        return formatter.format(getRemainingAmount())
    }
}
