package com.example.myalkansyamobile.api

import com.example.myalkansyamobile.model.Expense
import com.google.gson.annotations.SerializedName
import java.time.format.DateTimeFormatter

data class ExpenseRequest(
    val subject: String,
    val category: String,
    val amount: Double,
    val currency: String,
    val date: String,
    
    @SerializedName("savingsGoalId")
    val savingsGoalId: Int? = null, // This field is optional
    
    @SerializedName("originalAmount")
    val originalAmount: Double? = null,
    
    @SerializedName("originalCurrency")
    val originalCurrency: String? = null
) {
    companion object {
        fun fromExpense(expense: Expense): ExpenseRequest {
            return ExpenseRequest(
                subject = expense.subject,
                category = expense.category,
                amount = expense.amount,
                currency = expense.currency,
                date = expense.date.toString(),
                savingsGoalId = expense.savingsGoalId,
                originalAmount = expense.originalAmount,
                originalCurrency = expense.originalCurrency
            )
        }
    }
}