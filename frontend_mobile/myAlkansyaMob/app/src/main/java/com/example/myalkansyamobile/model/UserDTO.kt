package com.example.myalkansyamobile.model

import com.google.gson.annotations.SerializedName

data class UserDTO(
    @SerializedName("userId")
    val userId: String? = null,
    
    @SerializedName("id")
    val id: String? = null, // Some APIs return id instead of userId
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("firstname")
    val firstname: String? = null,
    
    @SerializedName("lastname")
    val lastname: String? = null,
    
    @SerializedName("username")
    val username: String? = null,
    
    @SerializedName("picture")
    val picture: String? = null,
    
    @SerializedName("profilePicture")
    val profilePicture: String? = null,
    
    @SerializedName("providerId")
    val providerId: String? = null,
    
    @SerializedName("authProvider")
    val authProvider: String? = null
) {
    // Helper method to get the effective userId from either userId or id
    fun getEffectiveUserId(): String? {
        return userId ?: id
    }
    
    // Helper method to get the user's display name
    fun getDisplayName(): String {
        return when {
            !firstname.isNullOrEmpty() && !lastname.isNullOrEmpty() -> "$firstname $lastname"
            !username.isNullOrEmpty() -> username
            !email.isNullOrEmpty() -> email.substringBefore("@")
            else -> "User"
        }
    }
    
    // Helper method to get profile image URL
    fun getProfileImage(): String? {
        return picture ?: profilePicture
    }
}
