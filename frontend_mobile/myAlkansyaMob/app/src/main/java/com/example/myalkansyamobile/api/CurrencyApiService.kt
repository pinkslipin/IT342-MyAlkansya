package com.example.myalkansyamobile.api

import com.example.myalkansyamobile.model.CurrencyConversionRequest
import com.example.myalkansyamobile.model.PopularCurrency
import retrofit2.Response
import retrofit2.http.*

interface CurrencyApiService {
    @POST("api/currency/convert")
    suspend fun convertCurrency(
        @Body request: CurrencyConversionRequest,
        @Header("Authorization") authToken: String
    ): Response<Map<String, Any>>
    
    @GET("api/currency/rate")
    suspend fun getExchangeRate(
        @Query("from") fromCurrency: String,
        @Query("to") toCurrency: String,
        @Header("Authorization") authToken: String
    ): Response<Map<String, Double>>
    
    @GET("api/currency/rates/{currency}")
    suspend fun getAllRates(
        @Path("currency") baseCurrency: String,
        @Header("Authorization") authToken: String
    ): Response<Map<String, Double>>
    
    @GET("api/currency/popular")
    suspend fun getPopularCurrencies(
        @Header("Authorization") authToken: String
    ): Response<List<PopularCurrency>>
}
