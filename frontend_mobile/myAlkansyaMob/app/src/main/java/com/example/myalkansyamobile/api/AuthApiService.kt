package com.example.myalkansyamobile.api

import com.example.myalkansyamobile.model.AuthResponse
import com.example.myalkansyamobile.model.GoogleAuthRequest
import com.example.myalkansyamobile.model.LoginRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {

    @POST("/api/users/google")
    suspend fun googleLogin(@Body request: GoogleAuthRequest): Response<AuthResponse>

    @POST("/api/users/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
}
