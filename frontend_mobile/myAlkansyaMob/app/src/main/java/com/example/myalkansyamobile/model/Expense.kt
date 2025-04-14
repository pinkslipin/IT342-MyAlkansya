package com.example.myalkansyamobile.model

import com.example.myalkansyamobile.api.ExpenseResponse
import java.io.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class Expense(
    val id: Int = 0,
    var subject: String = "",
    var category: String = "",
    var date: LocalDate = LocalDate.now(),
    var amount: Double = 0.0,
    var currency: String = "PHP"
) : Serializable {
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
                currency = response.currency
            )
        }
    }
}