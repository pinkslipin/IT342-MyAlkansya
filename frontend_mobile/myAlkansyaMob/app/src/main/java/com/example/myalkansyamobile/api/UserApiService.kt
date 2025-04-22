package com.example.myalkansyamobile.api

import com.example.myalkansyamobile.model.ProfileModel
import com.example.myalkansyamobile.model.ProfileUpdateRequest
import com.example.myalkansyamobile.model.UserDTO
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*

interface UserApiService {
    @GET("users/{id}")
    fun getUserById(@Path("id") id: Int): Call<UserResponse>
    
    // Fix the endpoint path by adding the 'api/' prefix
    @GET("api/users/me")
    fun getUserProfile(@Header("Authorization") token: String): Call<ProfileModel>
    
    // Update these endpoints as well to include the 'api/' prefix
    @PUT("api/users/update")
    fun updateUser(
        @Header("Authorization") token: String,
        @Body profileUpdateRequest: ProfileUpdateRequest
    ): Call<ProfileModel>
    
    @POST("api/users/uploadProfilePicture")
    @Multipart
    fun uploadProfilePicture(
        @Header("Authorization") token: String,
        @Part profilePicture: MultipartBody.Part
    ): Call<ProfileModel>
    
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
