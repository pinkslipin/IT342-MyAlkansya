package com.example.myalkansyamobile.api

import android.util.Log
import com.example.myalkansyamobile.model.AuthResponse
import com.example.myalkansyamobile.model.GoogleAuthRequest
import com.example.myalkansyamobile.model.LoginRequest
import com.example.myalkansyamobile.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class AuthRepository(private val apiService: AuthApiService) {

    suspend fun authenticateWithGoogle(idToken: String): Resource<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = GoogleAuthRequest(idToken)
                val response = apiService.googleLogin(request)

                if (response.isSuccessful) {
                    response.body()?.let {
                        Resource.Success(it)
                    } ?: Resource.Error("Empty response from server")
                } else {
                    Resource.Error("Google login failed: ${response.errorBody()?.string() ?: "Unknown error"}")
                }
            } catch (e: HttpException) {
                Resource.Error("Server Error: ${e.message()}")
            } catch (e: IOException) {
                Resource.Error("Network error: Please check your internet connection.")
            } catch (e: Exception) {
                Resource.Error("Unexpected error: ${e.localizedMessage}")
            }
        }
    }

    suspend fun login(request: LoginRequest): Resource<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.login(request)

            if (response.isSuccessful) {
                response.body()?.let {
                    Resource.Success(it)
                } ?: Resource.Error("Empty response from server")
            } else {
                Resource.Error("Login failed: ${response.errorBody()?.string() ?: "Unknown error"}")
            }
        } catch (e: HttpException) {
            Resource.Error("Server Error: ${e.message()}")
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: Exception) {
            Resource.Error("Unexpected error: ${e.localizedMessage}")
        }
    }
}
