package com.example.myalkansyamobile.model

import com.google.gson.annotations.SerializedName
import com.example.myalkansyamobile.api.ExpenseResponse
import java.io.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class Expense(
    val id: Int,
    val subject: String,
    val category: String,
    val date: LocalDate,
    val amount: Double,
    val currency: String = "PHP",
    
    // Add these fields to support preferred currency conversion
    @SerializedName("originalAmount")
    val originalAmount: Double? = null,
    
    @SerializedName("originalCurrency")
    val originalCurrency: String? = null,
    
    // Make savingsGoalId optional and not a primary constructor parameter
    @SerializedName("savingsGoalId")
    val savingsGoalId: Int? = null
) : Serializable {
    // Helper method to check if this is a savings goal expense
    fun isLinkedToSavingsGoal(): Boolean {
        return category == "Savings Goal" || savingsGoalId != null
    }

    // For API serialization
    fun getDateAsString(): String {
        return date.toString()
    }
    
    // For API deserialization
    companion object {
        fun fromResponse(response: ExpenseResponse): Expense {
            return Expense(
                id = response.id,
                subject = response.subject,
                category = response.category,
                date = LocalDate.parse(response.date),
                amount = response.amount,
                currency = response.currency,
                savingsGoalId = response.savingsGoalId
            )
        }
    }
}