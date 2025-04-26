package com.example.myalkansyamobile.model

import com.google.gson.annotations.SerializedName

/**
 * Request model for currency conversion API
 */
data class CurrencyConversionRequest(
    @SerializedName("fromCurrency")
    val from: String,
    
    @SerializedName("toCurrency")
    val to: String,
    
    @SerializedName("amount")
    val amount: Double
)
