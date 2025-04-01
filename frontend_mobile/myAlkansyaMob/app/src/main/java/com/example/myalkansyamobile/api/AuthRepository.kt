package com.example.myalkansyamobile.api

import android.util.Log
import com.example.myalkansyamobile.model.AuthResponse
import com.example.myalkansyamobile.model.FacebookAuthRequest
import com.example.myalkansyamobile.model.GoogleAuthRequest
import com.example.myalkansyamobile.model.LoginRequest
import com.example.myalkansyamobile.model.RegisterRequest
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
    
    suspend fun registerWithGoogle(idToken: String): Resource<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = GoogleAuthRequest(idToken)
                val response = apiService.googleRegister(request)

                if (response.isSuccessful) {
                    response.body()?.let {
                        Resource.Success(it)
                    } ?: Resource.Error("Empty response from server")
                } else {
                    Resource.Error("Google registration failed: ${response.errorBody()?.string() ?: "Unknown error"}")
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

    suspend fun authenticateWithFacebook(accessToken: String): Resource<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = FacebookAuthRequest(accessToken)
                val response = apiService.facebookLogin(request)

                if (response.isSuccessful) {
                    response.body()?.let {
                        Resource.Success(it)
                    } ?: Resource.Error("Empty response from server")
                } else {
                    Resource.Error("Facebook login failed: ${response.errorBody()?.string() ?: "Unknown error"}")
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
    
    suspend fun registerWithFacebook(accessToken: String): Resource<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = FacebookAuthRequest(accessToken)
                val response = apiService.facebookRegister(request)

                if (response.isSuccessful) {
                    response.body()?.let {
                        Resource.Success(it)
                    } ?: Resource.Error("Empty response from server")
                } else {
                    Resource.Error("Facebook registration failed: ${response.errorBody()?.string() ?: "Unknown error"}")
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
    
    suspend fun register(request: RegisterRequest): Resource<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.register(request)
            
            if (response.isSuccessful) {
                // Just return a success message without parsing the body
                val responseBody = response.body()?.string() ?: "Registration successful"
                Resource.Success("Registration successful: $responseBody")
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Resource.Error("Registration failed: $errorBody")
            }
        } catch (e: HttpException) {
            Resource.Error("Server Error: ${e.message()}")
        } catch (e: IOException) {
            Resource.Error("Network error: Please check your internet connection.")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Registration exception", e)
            Resource.Error("Unexpected error: ${e.localizedMessage}")
        }
    }
}
