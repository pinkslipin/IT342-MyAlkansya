package com.example.myalkansyamobile.model

import com.google.gson.annotations.SerializedName

/**
 * Model class representing a popular currency with its exchange rate and change percentage
 */
data class PopularCurrency(
    @SerializedName("code")
    val code: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("rate")
    val rate: Double,
    
    @SerializedName("changePercent")
    val changePercent: Double
)
