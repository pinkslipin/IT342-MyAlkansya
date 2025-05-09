package com.example.myalkansyamobile.api

import com.android.installreferrer.BuildConfig
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.lang.reflect.Type
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

data class MonthlySummaryResponse(
    val month: String = "", // Default value to avoid errors
    val income: Double = 0.0, // Default value to avoid errors
    val expenses: Double = 0.0 // Default value to avoid errors
)

data class CategoryExpenseResponse(
    val category: String = "", // Default value to avoid errors
    val amount: Double = 0.0 // Default value to avoid errors
)

data class FinancialSummaryResponse(
    val totalIncome: Double = 0.0, // Default value to avoid errors
    val totalExpenses: Double = 0.0, // Default value to avoid errors
    val totalBudget: Double = 0.0, // Default value to avoid errors
    val totalSavings: Double = 0.0, // Default value to avoid errors
    val netCashflow: Double = 0.0, // Default value to avoid errors
    val budgetUtilization: Double = 0.0, // Default value to avoid errors
    val savingsRate: Double = 0.0, // Default value to avoid errors
    val currency: String = "" // Default value to avoid errors
)

data class SavingsGoalProgressResponse(
    val goal: String = "", // Default value to avoid errors
    val currentAmount: Double = 0.0, // Default value to avoid errors
    val targetAmount: Double = 0.0, // Default value to avoid errors
    val progress: Double = 0.0, // Default value to avoid errors
    val savings: Double = 0.0, // Default value to avoid errors
    val percentage: Double = 0.0, // Default value to avoid errors
    val targetDate: String = "", // Default value to avoid errors
    val daysRemaining: Int = 0 // Default value to avoid errors
)

interface AnalyticsApiService {
    @GET("api/analytics/monthly-summary")
    suspend fun getMonthlySummary(
        @Header("Authorization") token: String,
        @Query("year") year: Int
    ): List<MonthlySummaryResponse>
    
    @GET("api/analytics/expense-categories")
    suspend fun getExpenseCategories(
        @Header("Authorization") token: String,
        @Query("month") month: Int,
        @Query("year") year: Int
    ): List<CategoryExpenseResponse>
    
    @GET("api/analytics/financial-summary")
    suspend fun getFinancialSummary(
        @Header("Authorization") token: String,
        @Query("month") month: Int,
        @Query("year") year: Int
    ): FinancialSummaryResponse
    
    @GET("api/analytics/savings-goals-progress")
    suspend fun getSavingsGoalsProgress(
        @Header("Authorization") token: String
    ): List<SavingsGoalProgressResponse>
}

object RetrofitClient {
    // Base URL for API requests - Make sure there's no trailing /api/
    private val BASE_URL = ApiConstants.BASE_URL
    // private const val BASE_URL = "http://192.168.1.x:8080/" // For physical device
    
    // Create logging interceptor
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
        
        // Add special handling for deleted resources
        if (BuildConfig.DEBUG) {
            // Add custom response handling
            redactHeader("Authorization")
            redactHeader("Cookie")
        }
    }
    
    // Custom interceptor to properly handle error responses
    private val errorInterceptor = Interceptor { chain ->
        val request = chain.request()
        val response = chain.proceed(request)
        
        // Log response details
        val url = response.request.url.toString()
        val code = response.code
        val contentType = response.header("Content-Type") ?: "unknown"
        
        // If it's a failed DELETE and then we try to GET the same resource, we shouldn't log out
        // This is a special case where we're handling the "resource not found after delete" situation
        val isDeleteAftermath = request.method == "GET" && url.contains("/get") && 
                              (code == 401 || code == 403 || code == 404)
        
        // Use safe call operator with  elvis operator to handle nullable return value
        val extractedId = extractIdFromUrl(url)
        if (isDeleteAftermath && extractedId != null && url.endsWith(extractedId)) {
            // This is likely just attempting to access a deleted resource 
            // Don't treat as an auth error
            android.util.Log.d("RetrofitClient", "Resource not found after deletion: $url")
        } else if (code == 401 || code == 403) {
            // Only log true auth errors for debugging
            android.util.Log.e("RetrofitClient", "Authentication issue detected - Code: $code, Content-Type: $contentType, URL: $url")
        }
        
        response
    }
    
    // Helper to extract ID from a URL like "/api/expenses/getExpense/3"
    private fun extractIdFromUrl(url: String): String? {
        val segments = url.split("/")
        return if (segments.isNotEmpty()) segments.last() else null
    }
    
    // Configure OkHttpClient with timeout and logging
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(errorInterceptor)
        .addInterceptor { chain ->
            val request = chain.request()
            
            // Log the request details for debugging
            android.util.Log.d("RetrofitClient", "Sending request: ${request.method} ${request.url}")
            
            val response = chain.proceed(request)
            
            // Log response details
            android.util.Log.d("RetrofitClient", "Received response: ${response.code} for ${request.url}")
            
            // Check for authentication issues:
            // 1. If response code is 401 or 403 - direct auth failure
            // 2. If response is 200 but content-type is HTML (indicates redirect to login page)
            // 3. If response body contains login keywords
            val contentType = response.header("Content-Type")
            val isHtmlResponse = contentType?.contains("text/html") == true
            val isAuthError = response.code == 401 || response.code == 403
            
            if (isHtmlResponse || isAuthError) {
                // Get the body text to see if it contains login-related content
                val responseBodyCopy = response.peekBody(Long.MAX_VALUE)
                val bodyString = responseBodyCopy.string()
                val containsLoginKeywords = bodyString.contains("login", ignoreCase = true) || 
                                            bodyString.contains("sign in", ignoreCase = true) ||
                                            bodyString.contains("auth", ignoreCase = true)
                
                // Debug details for troubleshooting
                android.util.Log.e("RetrofitClient", 
                    "Authentication issue detected - Code: ${response.code}, Content-Type: $contentType, " +
                    "URL: ${request.url}, Contains login keywords: $containsLoginKeywords")
                
                // Create error response with a proper body instead of no body
                val responseBody = """{"error":"Authentication required","message":"Session expired or invalid token"}"""
                    .toResponseBody("application/json".toMediaType())
                
                return@addInterceptor response.newBuilder()
                    .code(401)
                    .message("Authentication required")
                    .body(responseBody)
                    .build()
            }
            
            response
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    // Create Retrofit instance for analytics endpoints
    private val analyticsOkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(errorInterceptor)
        .addInterceptor { chain ->
            val request = chain.request()
            
            android.util.Log.d("RetrofitClient", "Sending request: ${request.method} ${request.url}")
            
            val response = chain.proceed(request)
            
            android.util.Log.d("RetrofitClient", "Received response: ${response.code} for ${request.url}")
            
            // Check for authentication issues
            val contentType = response.header("Content-Type")
            val isHtmlResponse = contentType?.contains("text/html") == true
            val isAuthError = response.code == 401 || response.code == 403
            
            if (isAuthError) {
                // Standard auth error
                return@addInterceptor response // Keep the original error response
            } else if (isHtmlResponse && response.code == 200) {
                // This is HTML with 200 status - likely a login page redirect
                val responseBodyCopy = response.peekBody(Long.MAX_VALUE)
                val bodyString = responseBodyCopy.string()
                
                if (bodyString.contains("login", ignoreCase = true) || 
                    bodyString.contains("sign in", ignoreCase = true)) {
                    
                    android.util.Log.d("RetrofitClient", "HTML login page detected, returning empty data")
                    
                    // Return empty data based on the endpoint
                    val emptyDataJson = when {
                        request.url.toString().contains("monthly-summary") -> "[]"
                        request.url.toString().contains("expense-categories") -> "[]"
                        request.url.toString().contains("financial-summary") -> 
                            """{"totalIncome":0,"totalExpenses":0,"totalBudget":0,"totalSavings":0,
                               "netCashflow":0,"budgetUtilization":0,"savingsRate":0,"currency":"USD"}"""
                        request.url.toString().contains("savings-goals-progress") -> "[]"
                        else -> "[]"
                    }
                    
                    return@addInterceptor response.newBuilder()
                        .code(200) // Keep status 200 for empty data
                        .removeHeader("Content-Type")
                        .header("Content-Type", "application/json")
                        .body(emptyDataJson.toResponseBody("application/json".toMediaType()))
                        .build()
                }
            }
            
            response
        }
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
        .addConverterFactory(NullOnEmptyConverterFactory())
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
    
    // Create a separate analytics Retrofit instance
    private val analyticsRetrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(analyticsOkHttpClient)
        .addConverterFactory(NullOnEmptyConverterFactory())
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
    // Use the analytics-specific client for analytics endpoints
    val analyticsApiService: AnalyticsApiService = analyticsRetrofit.create(AnalyticsApiService::class.java)
    
    /**
     * Handles empty responses by returning null instead of failing
     */
    private class NullOnEmptyConverterFactory : Converter.Factory() {
        override fun responseBodyConverter(
            type: Type,
            annotations: Array<Annotation>,
            retrofit: Retrofit
        ): Converter<ResponseBody, *> {
            val delegate = retrofit.nextResponseBodyConverter<Any>(this, type, annotations)
            return Converter<ResponseBody, Any> { body ->
                if (body.contentLength() == 0L) null
                else delegate.convert(body)
            }
        }
    }
}