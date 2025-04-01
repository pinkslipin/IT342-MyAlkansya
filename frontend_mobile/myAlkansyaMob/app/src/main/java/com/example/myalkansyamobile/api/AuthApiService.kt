package com.example.myalkansyamobile.api

import com.example.myalkansyamobile.model.AuthResponse
import com.example.myalkansyamobile.model.FacebookAuthRequest
import com.example.myalkansyamobile.model.GoogleAuthRequest
import com.example.myalkansyamobile.model.LoginRequest
import com.example.myalkansyamobile.model.RegisterRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {

    @POST("/api/users/google/login")
    suspend fun googleLogin(@Body request: GoogleAuthRequest): Response<AuthResponse>
    
    @POST("/api/users/google/register")
    suspend fun googleRegister(@Body request: GoogleAuthRequest): Response<AuthResponse>

    @POST("/api/users/facebook/login")
    suspend fun facebookLogin(@Body request: FacebookAuthRequest): Response<AuthResponse>
    
    @POST("/api/users/facebook/register")
    suspend fun facebookRegister(@Body request: FacebookAuthRequest): Response<AuthResponse>

    @POST("/api/users/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
    
    @POST("/api/users/register")
    suspend fun register(@Body request: RegisterRequest): Response<ResponseBody>
}
