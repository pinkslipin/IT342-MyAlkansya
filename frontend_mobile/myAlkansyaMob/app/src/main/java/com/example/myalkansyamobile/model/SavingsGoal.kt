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
    val currency: String = "PHP",
    val createdDate: Date = Date(),  // Added creation date for filtering
    // Added fields to track original values before currency conversion
    val originalTargetAmount: Double? = null,
    val originalCurrentAmount: Double? = null,
    val originalCurrency: String? = null
) {
    // Status enum for filtering
    enum class GoalStatus {
        IN_PROGRESS,
        COMPLETED,
        OVERDUE
    }
    
    // Computed status based on current state
    fun getComputedStatus(): GoalStatus {
        return when {
            currentAmount >= targetAmount -> GoalStatus.COMPLETED
            getRemainingDays() < 0 -> GoalStatus.OVERDUE
            else -> GoalStatus.IN_PROGRESS
        }
    }
    
    // Display status as string
    fun getStatusText(): String {
        return when (getComputedStatus()) {
            GoalStatus.COMPLETED -> "Completed"
            GoalStatus.OVERDUE -> "Overdue"
            GoalStatus.IN_PROGRESS -> "In Progress"
        }
    }
    
    // Get month from target date (for filtering)
    fun getTargetMonth(): Int {
        val calendar = Calendar.getInstance()
        calendar.time = targetDate
        return calendar.get(Calendar.MONTH)
    }
    
    // Get year from target date (for filtering)
    fun getTargetYear(): Int {
        val calendar = Calendar.getInstance()
        calendar.time = targetDate
        return calendar.get(Calendar.YEAR)
    }
    
    // Get creation month (for filtering)
    fun getCreationMonth(): Int {
        val calendar = Calendar.getInstance()
        calendar.time = createdDate
        return calendar.get(Calendar.MONTH)
    }
    
    // Get creation year (for filtering)
    fun getCreationYear(): Int {
        val calendar = Calendar.getInstance()
        calendar.time = createdDate
        return calendar.get(Calendar.YEAR)
    }

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
    
    fun getFormattedCreationDate(): String {
        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return formatter.format(createdDate)
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
    
    // Helper method to get the original amounts if available
    fun getEffectiveTargetAmount(): Double = originalTargetAmount ?: targetAmount
    
    fun getEffectiveCurrentAmount(): Double = originalCurrentAmount ?: currentAmount
    
    fun getEffectiveCurrency(): String = originalCurrency ?: currency
    
    companion object {
        // Sort methods for the filter system
        fun sortByTargetDate(goals: List<SavingsGoal>): List<SavingsGoal> {
            return goals.sortedBy { it.targetDate }
        }
        
        fun sortByProgress(goals: List<SavingsGoal>): List<SavingsGoal> {
            return goals.sortedBy { it.getProgressPercentage() }
        }
        
        fun sortByAmount(goals: List<SavingsGoal>): List<SavingsGoal> {
            return goals.sortedBy { it.targetAmount }
        }
        
        fun sortByName(goals: List<SavingsGoal>): List<SavingsGoal> {
            return goals.sortedBy { it.goal }
        }
        
        // Filter methods for the filter system
        fun filterByStatus(goals: List<SavingsGoal>, status: GoalStatus?): List<SavingsGoal> {
            return if (status == null) goals else goals.filter { it.getComputedStatus() == status }
        }
    }
}
