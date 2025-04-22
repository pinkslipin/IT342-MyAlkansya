package com.example.myalkansyamobile.api

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // Base URL for API requests - Make sure there's no trailing /api/
    private const val BASE_URL = "http://10.0.2.2:8080/"  // Android emulator URL for localhost
    
    // Create logging interceptor
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    // Configure OkHttpClient with timeout and logging
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    // Configure Gson to handle various date formats and null values
    private val gson = GsonBuilder()
        .setLenient()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        .create()
    
    // Create Retrofit instance
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
    
    // Create API services
    val authApiService: AuthApiService = retrofit.create(AuthApiService::class.java)
    val userApiService: UserApiService = retrofit.create(UserApiService::class.java)
    val expenseApiService: ExpenseApiService = retrofit.create(ExpenseApiService::class.java)
    val incomeApiService: IncomeApiService = retrofit.create(IncomeApiService::class.java)
    val budgetApiService: BudgetApiService = retrofit.create(BudgetApiService::class.java)
    val savingsGoalApiService: SavingsGoalApiService = retrofit.create(SavingsGoalApiService::class.java)
    val currencyApiService: CurrencyApiService = retrofit.create(CurrencyApiService::class.java)
}