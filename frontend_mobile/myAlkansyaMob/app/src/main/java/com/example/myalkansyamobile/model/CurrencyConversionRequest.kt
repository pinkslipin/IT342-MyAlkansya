package com.example.myalkansyamobile.model

data class CurrencyConversionRequest(
    val fromCurrency: String,
    val toCurrency: String,
    val amount: Double
)
