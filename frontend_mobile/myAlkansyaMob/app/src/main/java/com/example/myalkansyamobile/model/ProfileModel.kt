package com.example.myalkansyamobile.model

import com.google.gson.annotations.SerializedName

data class ProfileModel(
    @SerializedName("userId")
    val userId: Int,
    
    @SerializedName("firstname")
    val firstname: String,
    
    @SerializedName("lastname")
    val lastname: String,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("currency")
    val currency: String?,
    
    @SerializedName("profilePicture")
    val profilePicture: String?,
    
    @SerializedName("authProvider")
    val authProvider: String?, // Added to identify OAuth vs LOCAL users
    
    @SerializedName("providerId")
    val providerId: String?,    // Additional field to detect OAuth accounts
    
    @SerializedName("totalSavings")
    val totalSavings: Double
)

data class ProfileUpdateRequest(
    @SerializedName("firstname")
    val firstname: String,
    
    @SerializedName("lastname")
    val lastname: String,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("currency")
    val currency: String
)
