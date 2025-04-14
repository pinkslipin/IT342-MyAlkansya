package com.example.myalkansyamobile.api

import com.example.myalkansyamobile.model.Expense
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class ExpenseRequest(
    val subject: String,
    val category: String,
    val date: String,
    val amount: Double,
    val currency: String = "PHP"
) {
    companion object {
        fun fromExpense(expense: com.example.myalkansyamobile.model.Expense): ExpenseRequest {
            return ExpenseRequest(
                subject = expense.subject,
                category = expense.category,
                date = expense.date.toString(),
                amount = expense.amount,
                currency = expense.currency
            )
        }
    }
}