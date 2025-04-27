package com.example.myalkansyamobile.api

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

/**
 * Data class representing an income request to the API
 */
data class IncomeRequest(
    @SerializedName("source")
    val source: String,
    
    @SerializedName("amount")
    val amount: String, // Using String to maintain precision from BigDecimal toString()
    
    @SerializedName("date")
    val date: String,
    
    @SerializedName("currency")
    val currency: String,
    
    @SerializedName("originalAmount")
    val originalAmount: String? = null,
    
    @SerializedName("originalCurrency")
    val originalCurrency: String? = null
)
