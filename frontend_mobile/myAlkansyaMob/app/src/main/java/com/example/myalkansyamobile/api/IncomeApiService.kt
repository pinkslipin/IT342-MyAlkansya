package com.example.myalkansyamobile.api

import com.example.myalkansyamobile.model.Income
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface IncomeApiService {
    @GET("api/incomes/getIncomes")
    suspend fun getIncomes(@Header("Authorization") token: String): Response<List<Income>>

    @GET("api/incomes/getIncome/{incomeId}")
    suspend fun getIncomeById(
        @Path("incomeId") incomeId: Int,
        @Header("Authorization") token: String
    ): Response<Income>

    @POST("/api/incomes/postIncome")
    suspend fun addIncome(
        @Body income: Income,
        @Header("Authorization") token: String
    ): Response<Income>

    @PUT("/api/incomes/putIncome/{incomeId}")
    suspend fun updateIncome(
        @Path("incomeId") incomeId: Int,
        @Body income: Income,
        @Header("Authorization") token: String
    ): Response<Income>

    @DELETE("/api/incomes/deleteIncome/{incomeId}")
    suspend fun deleteIncome(
        @Path("incomeId") incomeId: Int,
        @Header("Authorization") token: String
    ): Response<ResponseBody>
}
