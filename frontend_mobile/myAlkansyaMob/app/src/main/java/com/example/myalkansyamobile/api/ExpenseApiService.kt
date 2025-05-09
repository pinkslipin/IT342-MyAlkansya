package com.example.myalkansyamobile.api

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*
import java.time.LocalDate

interface ExpenseApiService {
    @GET("api/expenses/getExpenses")
    suspend fun getExpenses(@Header("Authorization") token: String): List<ExpenseResponse>
    
    @GET("api/expenses/getExpense/{id}")
    suspend fun getExpenseById(@Path("id") id: Int, @Header("Authorization") token: String): ExpenseResponse
    
    @POST("api/expenses/postExpense")
    suspend fun createExpense(
        @Body expenseRequest: ExpenseRequest, 
        @Header("Authorization") token: String
    ): Response<ExpenseResponse>
    
    @PUT("api/expenses/putExpense/{id}")
    suspend fun updateExpense(
        @Path("id") id: Int,
        @Body expenseRequest: ExpenseRequest, 
        @Header("Authorization") token: String
    ): Response<ExpenseResponse>
    
    @DELETE("api/expenses/deleteExpense/{id}")
    suspend fun deleteExpense(
        @Path("id") id: Int, 
        @Header("Authorization") token: String
    ): Response<ResponseBody>
}

data class ExpenseResponse(
    val id: Int,
    val amount: Double,
    val category: String,
    val description: String?,
    val date: String,
    val currency: String = "PHP",
    val subject: String = "",
    val savingsGoalId: Int? = null
)


