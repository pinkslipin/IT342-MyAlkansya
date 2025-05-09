package com.example.myalkansyamobile.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*
import okhttp3.ResponseBody

interface SavingsGoalApiService {
    @GET("api/savings-goals/getSavingsGoals")
    suspend fun getAllSavingsGoals(@Header("Authorization") token: String): List<SavingsGoalResponse>
    
    @GET("api/savings-goals/getSavingsGoal/{id}")
    suspend fun getSavingsGoalById(@Path("id") id: Int, @Header("Authorization") token: String): SavingsGoalResponse
    
    @POST("api/savings-goals/postSavingsGoal")
    suspend fun createSavingsGoal(
        @Body savingsGoal: SavingsGoalRequest,
        @Header("Authorization") token: String
    ): SavingsGoalResponse
    
    @PUT("api/savings-goals/putSavingsGoal/{id}")
    suspend fun updateSavingsGoal(
        @Path("id") id: Int,
        @Body savingsGoal: SavingsGoalRequest,
        @Header("Authorization") token: String
    ): SavingsGoalResponse
    
    @DELETE("api/savings-goals/deleteSavingsGoal/{id}")
    suspend fun deleteSavingsGoal(
        @Path("id") id: Int, 
        @Header("Authorization") token: String
    ): Response<ResponseBody>
}

data class SavingsGoalRequest(
    val goal: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val targetDate: String,
    val currency: String = "PHP",

    @SerializedName("originalTargetAmount")
    val originalTargetAmount: Double? = null,

    @SerializedName("originalCurrentAmount")
    val originalCurrentAmount: Double? = null,

    @SerializedName("originalCurrency")
    val originalCurrency: String? = null
)

data class SavingsGoalResponse(
    val id: Int,
    val goal: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val targetDate: String,
    val currency: String,
    val originalTargetAmount: Double? = null,
    val originalCurrentAmount: Double? = null,
    val originalCurrency: String? = null
)
