package com.example.myalkansyamobile.model

import com.google.gson.annotations.SerializedName

data class GoogleUserDTO(
    @SerializedName("firstname")
    val firstname: String,

    @SerializedName("lastname")
    val lastname: String?,

    @SerializedName("email")
    val email: String,

    @SerializedName("profilePicture")
    val profilePicture: String?,

    @SerializedName("providerId")
    val providerId: String,
    
    @SerializedName("userId")
    val userId: String?,
    
    @SerializedName("username")
    val username: String?
)

