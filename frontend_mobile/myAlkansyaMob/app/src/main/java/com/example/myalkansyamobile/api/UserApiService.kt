package com.example.myalkansyamobile.api

import com.example.myalkansyamobile.model.ProfileModel
import com.example.myalkansyamobile.model.ProfileUpdateRequest
import com.example.myalkansyamobile.model.UserDTO
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface UserApiService {
    @GET("users/{id}")
    fun getUserById(@Path("id") id: Int): Call<UserResponse>
    
    @GET("api/users/me")
    fun getUserProfile(@Header("Authorization") token: String): Call<ProfileModel>
    
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
    
    @POST("api/users/changeCurrency")
    suspend fun changeCurrency(
        @Header("Authorization") token: String,
        @Body request: ChangeCurrencyRequest
    ): Response<UserResponse>
    
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

data class ChangeCurrencyRequest(
    @SerializedName("newCurrency") val newCurrency: String,
    @SerializedName("oldCurrency") val oldCurrency: String
)
