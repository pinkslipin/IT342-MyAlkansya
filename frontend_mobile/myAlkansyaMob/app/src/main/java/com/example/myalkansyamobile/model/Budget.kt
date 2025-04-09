package com.example.myalkansyamobile.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Budget(
    val id: Int = 0,
    val category: String = "",
    val monthlyBudget: Double = 0.0,
    val totalSpent: Double = 0.0,
    val currency: String = "PHP",
    val budgetMonth: Int = 0,  // 1-12 for January-December
    val budgetYear: Int = 0,
    // User relationship is ignored here as it's not needed on the mobile side
    // We'll get only budgets for the logged-in user
    
    // Used to display month names in UI
    @Transient
    val monthNames: Array<String> = arrayOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
) : Serializable {
    // Helper methods
    fun getMonthName(): String {
        return if (budgetMonth in 1..12) {
            monthNames[budgetMonth - 1]
        } else {
            "Unknown Month"
        }
    }
    
    fun getSpendingPercentage(): Int {
        return if (monthlyBudget > 0) {
            ((totalSpent / monthlyBudget) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }
    }
    
    fun getRemainingBudget(): Double {
        return monthlyBudget - totalSpent
    }
}
