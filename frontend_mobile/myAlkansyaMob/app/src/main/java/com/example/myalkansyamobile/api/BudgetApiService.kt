package com.example.myalkansyamobile.api

import com.example.myalkansyamobile.model.Budget
import retrofit2.Response
import retrofit2.http.*

interface BudgetApiService {
    @GET("api/budgets/user")
    suspend fun getUserBudgets(@Header("Authorization") token: String): Response<List<Budget>>

    @GET("api/budgets/{id}")
    suspend fun getBudgetById(
        @Path("id") id: Int,
        @Header("Authorization") token: String
    ): Response<Budget>

    @GET("api/budgets/getBudgetsByMonth/{month}/{year}")
    suspend fun getBudgetsByMonth(
        @Path("month") month: Int,
        @Path("year") year: Int,
        @Header("Authorization") token: String
    ): Response<List<Budget>>

    @GET("api/budgets/getCurrentMonthBudgets")
    suspend fun getCurrentMonthBudgets(
        @Header("Authorization") token: String
    ): Response<List<Budget>>

    @POST("api/budgets/create")
    suspend fun createBudget(
        @Body budget: Budget,
        @Header("Authorization") token: String
    ): Response<Budget>

    @PUT("api/budgets/update/{id}")
    suspend fun updateBudget(
        @Path("id") id: Int,
        @Body budget: Budget,
        @Header("Authorization") token: String
    ): Response<Budget>

    @DELETE("api/budgets/delete/{id}")
    suspend fun deleteBudget(
        @Path("id") id: Int,
        @Header("Authorization") token: String
    ): Response<String>
}
