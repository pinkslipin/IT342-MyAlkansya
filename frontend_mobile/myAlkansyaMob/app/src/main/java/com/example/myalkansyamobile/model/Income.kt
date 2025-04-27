package com.example.myalkansyamobile.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Income(
    @SerializedName("id")
    val id: Int = 0,
    
    @SerializedName("source")
    val source: String = "",
    
    @SerializedName("date") 
    val date: String = "",
    
    @SerializedName("amount")
    val amount: Double = 0.0,
    
    @SerializedName("currency")
    val currency: String? = null,
    
    @SerializedName("originalAmount")
    val originalAmount: Double? = null,
    
    @SerializedName("originalCurrency")
    val originalCurrency: String? = null
) : Serializable