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
                Log.d("AuthRepository", "Sending Google auth request with token: ${idToken.take(20)}...")
                val response = apiService.googleLogin(request)

                if (response.isSuccessful) {
                    response.body()?.let {
                        Log.d("AuthRepository", "Google auth successful")
                        Resource.Success(it)
                    } ?: Resource.Error("Empty response from server")
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e("AuthRepository", "Google login failed: $errorBody")
                    
                    if (errorBody.contains("No user found", ignoreCase = true) || 
                        errorBody.contains("not registered", ignoreCase = true) ||
                        response.code() == 401) {
                        Resource.Error("User not registered", code = response.code())
                    } else {
                        Resource.Error("Google login failed: $errorBody", code = response.code())
                    }
                }
            } catch (e: HttpException) {
                Log.e("AuthRepository", "HTTP error during Google auth: ${e.code()}: ${e.message()}")
                Resource.Error("Server Error: ${e.message()}", code = e.code())
            } catch (e: IOException) {
                Log.e("AuthRepository", "Network error during Google auth: ${e.message}")
                Resource.Error("Network error: Please check your internet connection.")
            } catch (e: Exception) {
                Log.e("AuthRepository", "Unexpected error during Google auth", e)
                Resource.Error("Unexpected error: ${e.localizedMessage}")
            }
        }
    }
    
    suspend fun registerWithGoogle(idToken: String): Resource<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = GoogleAuthRequest(idToken)
                Log.d("AuthRepository", "Sending Google registration request with token: ${idToken.take(20)}...")
                val response = apiService.googleRegister(request)

                if (response.isSuccessful) {
                    response.body()?.let {
                        Log.d("AuthRepository", "Google registration successful")
                        Resource.Success(it)
                    } ?: Resource.Error("Empty response from server")
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e("AuthRepository", "Google registration failed: $errorBody")
                    Resource.Error("Google registration failed: $errorBody", code = response.code())
                }
            } catch (e: HttpException) {
                Log.e("AuthRepository", "HTTP error during Google registration: ${e.code()}: ${e.message()}")
                Resource.Error("Server Error: ${e.message()}", code = e.code())
            } catch (e: IOException) {
                Log.e("AuthRepository", "Network error during Google registration: ${e.message}")
                Resource.Error("Network error: Please check your internet connection.")
            } catch (e: Exception) {
                Log.e("AuthRepository", "Unexpected error during Google registration", e)
                Resource.Error("Unexpected error: ${e.localizedMessage}")
            }
        }
    }

    suspend fun authenticateWithFacebook(accessToken: String): Resource<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = FacebookAuthRequest(accessToken)
                Log.d("AuthRepository", "Sending Facebook auth request with token: ${accessToken.take(20)}...")
                val response = apiService.facebookLogin(request)

                if (response.isSuccessful) {
                    response.body()?.let {
                        Log.d("AuthRepository", "Facebook auth successful")
                        Resource.Success(it)
                    } ?: Resource.Error("Empty response from server")
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e("AuthRepository", "Facebook login failed: $errorBody")
                    
                    if (errorBody.contains("No user found", ignoreCase = true) || 
                        errorBody.contains("not registered", ignoreCase = true) ||
                        response.code() == 401) {
                        Resource.Error("User not registered", code = response.code())
                    } else {
                        Resource.Error("Facebook login failed: $errorBody", code = response.code())
                    }
                }
            } catch (e: HttpException) {
                Log.e("AuthRepository", "HTTP error during Facebook auth: ${e.code()}: ${e.message()}")
                Resource.Error("Server Error: ${e.message()}", code = e.code())
            } catch (e: IOException) {
                Log.e("AuthRepository", "Network error during Facebook auth: ${e.message}")
                Resource.Error("Network error: Please check your internet connection.")
            } catch (e: Exception) {
                Log.e("AuthRepository", "Unexpected error during Facebook auth", e)
                Resource.Error("Unexpected error: ${e.localizedMessage}")
            }
        }
    }
    
    suspend fun registerWithFacebook(accessToken: String): Resource<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = FacebookAuthRequest(accessToken)
                Log.d("AuthRepository", "Sending Facebook registration request with token: ${accessToken.take(20)}...")
                val response = apiService.facebookRegister(request)

                if (response.isSuccessful) {
                    response.body()?.let {
                        Log.d("AuthRepository", "Facebook registration successful")
                        Resource.Success(it)
                    } ?: Resource.Error("Empty response from server")
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e("AuthRepository", "Facebook registration failed: $errorBody")
                    Resource.Error("Facebook registration failed: $errorBody", code = response.code())
                }
            } catch (e: HttpException) {
                Log.e("AuthRepository", "HTTP error during Facebook registration: ${e.code()}: ${e.message()}")
                Resource.Error("Server Error: ${e.message()}", code = e.code())
            } catch (e: IOException) {
                Log.e("AuthRepository", "Network error during Facebook registration: ${e.message}")
                Resource.Error("Network error: Please check your internet connection.")
            } catch (e: Exception) {
                Log.e("AuthRepository", "Unexpected error during Facebook registration", e)
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
