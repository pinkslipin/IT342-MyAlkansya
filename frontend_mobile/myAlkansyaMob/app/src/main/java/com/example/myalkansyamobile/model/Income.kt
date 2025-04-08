package com.example.myalkansyamobile.model

import com.google.gson.annotations.SerializedName

data class Income(
    val id: Int = 0,
    val source: String,
    val date: String,
    val amount: Double,
    val currency: String
    // Note: User property is handled by the backend
)