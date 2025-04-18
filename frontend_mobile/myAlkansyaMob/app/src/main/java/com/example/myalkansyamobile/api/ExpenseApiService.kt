package com.example.myalkansyamobile.api

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*
import java.time.LocalDate

interface ExpenseApiService {
    @GET("expenses/getExpenses")
    suspend fun getExpenses(@Header("Authorization") token: String): List<ExpenseResponse>
    
    @GET("expenses/getExpensesByCategory/{category}")
    suspend fun getExpensesByCategory(
        @Header("Authorization") token: String,
        @Path("category") category: String
    ): List<ExpenseResponse>
    
    @GET("expenses/getExpense/{id}")
    suspend fun getExpenseById(@Path("id") id: Int, @Header("Authorization") token: String): ExpenseResponse
    
    @POST("expenses/postExpense")
    suspend fun createExpense(@Header("Authorization") token: String, @Body expense: ExpenseRequest): Response<ExpenseResponse>
    
    @PUT("expenses/putExpense/{id}")
    suspend fun updateExpense(@Path("id") id: Int, @Body expense: ExpenseRequest, @Header("Authorization") token: String): Response<ExpenseResponse>
    
    @DELETE("expenses/deleteExpense/{id}")
    suspend fun deleteExpense(@Path("id") id: Int, @Header("Authorization") token: String): Response<String>
}

data class ExpenseResponse(
    val id: Int = 0,
    val subject: String = "",
    val category: String = "",
    val date: String = "",
    val amount: Double = 0.0,
    val currency: String = "PHP"
)


