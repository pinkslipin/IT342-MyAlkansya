package com.example.myalkansyamobile.api

import retrofit2.Call
import retrofit2.http.*

interface UserApiService {
    @GET("users/{id}")
    fun getUserById(@Path("id") id: Int): Call<UserResponse>
    
    @GET("users/profile")
    fun getUserProfile(@Header("Authorization") token: String): Call<UserResponse>
    
    @PUT("users/{id}")
    fun updateUser(@Path("id") id: Int, @Body user: UserRequest): Call<UserResponse>
    
    @GET("users")
    fun getAllUsers(): Call<List<UserResponse>>
}

data class UserRequest(
    val username: String,
    val email: String,
    val password: String
)

data class UserResponse(
    val id: Int,
    val username: String,
    val email: String
)
