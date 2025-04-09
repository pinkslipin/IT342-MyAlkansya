package com.example.myalkansyamobile.api

import com.example.myalkansyamobile.model.Expense
import retrofit2.Response
import retrofit2.http.*

interface ExpenseApiService {
    @POST("expenses/postExpense")
    suspend fun createExpense(
        @Header("Authorization") token: String,
        @Body expenseRequest: ExpenseRequest
    ): Response<Expense>

    @GET("expenses/getExpenses")
    suspend fun getExpenses(
        @Header("Authorization") token: String
    ): Response<List<Expense>>

    @GET("expenses/getExpense/{expenseId}")
    suspend fun getExpenseById(
        @Header("Authorization") token: String,
        @Path("expenseId") expenseId: Int
    ): Response<Expense>

    @PUT("expenses/putExpense/{expenseId}")
    suspend fun updateExpense(
        @Header("Authorization") token: String,
        @Path("expenseId") expenseId: Int,
        @Body expenseRequest: ExpenseRequest
    ): Response<Expense>

    @DELETE("expenses/deleteExpense/{expenseId}")
    suspend fun deleteExpense(
        @Header("Authorization") token: String,
        @Path("expenseId") expenseId: Int
    ): Response<String>
}

// Response DTO to match backend entity structure
data class ExpenseResponse(
    val id: Int,
    val subject: String,
    val category: String,
    val date: String,
    val amount: Double,
    val currency: String
)


