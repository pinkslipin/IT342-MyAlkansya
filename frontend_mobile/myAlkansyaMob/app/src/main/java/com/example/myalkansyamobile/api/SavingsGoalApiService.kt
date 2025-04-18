package com.example.myalkansyamobile.api

import retrofit2.Call
import retrofit2.http.*
import java.time.LocalDate

interface SavingsGoalApiService {
    @GET("savings-goals/getSavingsGoals")
    suspend fun getAllSavingsGoals(@Header("Authorization") token: String): List<SavingsGoalResponse>
    
    @GET("savings-goals/getSavingsGoal/{goalId}")
    suspend fun getSavingsGoalById(
        @Path("goalId") goalId: Int,
        @Header("Authorization") token: String
    ): SavingsGoalResponse
    
    @POST("savings-goals/postSavingsGoal")
    suspend fun createSavingsGoal(
        @Body savingsGoal: SavingsGoalRequest,
        @Header("Authorization") token: String
    ): SavingsGoalResponse
    
    @PUT("savings-goals/putSavingsGoal/{goalId}")
    suspend fun updateSavingsGoal(
        @Path("goalId") goalId: Int,
        @Body savingsGoal: SavingsGoalRequest,
        @Header("Authorization") token: String
    ): SavingsGoalResponse
    
    @DELETE("savings-goals/deleteSavingsGoal/{goalId}")
    suspend fun deleteSavingsGoal(
        @Path("goalId") goalId: Int,
        @Header("Authorization") token: String
    ): String
}

data class SavingsGoalRequest(
    val goal: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val targetDate: String,
    val currency: String = "PHP"
)

data class SavingsGoalResponse(
    val id: Int,
    val goal: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val targetDate: String,
    val currency: String
)
