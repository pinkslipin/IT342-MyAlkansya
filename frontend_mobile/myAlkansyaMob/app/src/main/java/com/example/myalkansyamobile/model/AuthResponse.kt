package com.example.myalkansyamobile.model

import com.google.gson.annotations.SerializedName

data class AuthResponse(
    @SerializedName("token")
    val token: String,

    @SerializedName("user")
    val user: GoogleUserDTO
)


