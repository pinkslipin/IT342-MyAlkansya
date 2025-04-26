package com.example.myalkansyamobile.model

import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime

data class CurrencyConversionResponse(
    @SerializedName("fromCurrency")
    val fromCurrency: String,
    
    @SerializedName("toCurrency")
    val toCurrency: String,
    
    @SerializedName("amount")
    val amount: Double,
    
    @SerializedName("convertedAmount")
    val convertedAmount: Double,
    
    @SerializedName("exchangeRate")
    val exchangeRate: Double,
    
    @SerializedName("timestamp")
    val timestamp: String
)

data class ConversionResult(
    @SerializedName("conversion")
    val conversion: CurrencyConversionResponse,
    
    @SerializedName("trends")
    val trends: Map<String, TrendData>
)

data class TrendData(
    @SerializedName("historicalRate")
    val historicalRate: Double,
    
    @SerializedName("changePercent")
    val changePercent: Double,
    
    @SerializedName("trendPoints")
    val trendPoints: List<Double>
)

