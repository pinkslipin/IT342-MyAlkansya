package com.example.myalkansyamobile.model

import com.google.gson.annotations.SerializedName

data class AuthResponse(
    @SerializedName("token")
    val token: String,

    @SerializedName("user")
    val user: UserDTO? // Changed to nullable and using UserDTO instead of GoogleUserDTO
)


