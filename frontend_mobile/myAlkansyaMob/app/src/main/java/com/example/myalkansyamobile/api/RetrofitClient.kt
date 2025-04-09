package com.example.myalkansyamobile.api

import com.google.gson.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // Update the BASE_URL to include /api path which matches backend
    private const val BASE_URL = "http://10.0.2.2:8080/api/" // For Android emulator

    // 🔹 Custom TypeAdapter for LocalDate
    private class LocalDateAdapter : JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
        override fun serialize(src: LocalDate, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            return JsonPrimitive(src.toString()) // ISO-8601 format (yyyy-MM-dd)
        }

        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): LocalDate {
            return LocalDate.parse(json.asString)
        }
    }

    // 🔹 Explicit Gson Configuration with LocalDate adapter
    private val gson: Gson = GsonBuilder()
        .setLenient() // Helps avoid JSON parsing issues
        .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
        .create()

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val authApiService: AuthApiService = retrofit.create(AuthApiService::class.java)
    val incomeApiService: IncomeApiService = retrofit.create(IncomeApiService::class.java)
    val userApiService: UserApiService = retrofit.create(UserApiService::class.java)
    val expenseApiService: ExpenseApiService = retrofit.create(ExpenseApiService::class.java)

    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
}