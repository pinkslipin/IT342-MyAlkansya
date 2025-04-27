package com.example.myalkansyamobile.model

data class RegisterRequest(
    val firstname: String,
    val lastname: String,
    val email: String,
    val password: String,
    val currency: String = "PHP"  // Default currency
)
