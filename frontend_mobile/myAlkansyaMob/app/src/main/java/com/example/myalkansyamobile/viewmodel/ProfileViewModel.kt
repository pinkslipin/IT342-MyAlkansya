package com.example.myalkansyamobile.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.model.ProfileModel
import com.example.myalkansyamobile.model.ProfileUpdateRequest
import com.example.myalkansyamobile.utils.SessionManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ProfileViewModel : ViewModel() {
    // Use the RetrofitClient that already exists in the project
    private val userApiService = RetrofitClient.userApiService
    
    private val _profileData = MutableLiveData<ProfileModel>()
    val profileData: LiveData<ProfileModel> = _profileData
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error
    
    private val _updateSuccess = MutableLiveData<Boolean>()
    val updateSuccess: LiveData<Boolean> = _updateSuccess
    
    fun fetchUserProfile(sessionManager: SessionManager) {
        _isLoading.value = true
        val tokenValue = sessionManager.getToken()
        
        if (tokenValue.isNullOrEmpty()) {
            _isLoading.value = false
            _error.value = "Authentication token is missing"
            return
        }
        
        val token = "Bearer $tokenValue"
        
        userApiService.getUserProfile(token).enqueue(object : Callback<ProfileModel> {
            override fun onResponse(call: Call<ProfileModel>, response: Response<ProfileModel>) {
                _isLoading.value = false
                if (response.isSuccessful && response.body() != null) {
                    _profileData.value = response.body()
                } else {
                    _error.value = "Failed to load profile: ${response.code()} ${response.message()}"
                }
            }
            
            override fun onFailure(call: Call<ProfileModel>, t: Throwable) {
                _isLoading.value = false
                _error.value = "Network error: ${t.message}"
            }
        })
    }
    
    fun updateProfile(
        sessionManager: SessionManager,
        firstname: String,
        lastname: String,
        email: String,
        currency: String
    ) {
        _isLoading.value = true
        val tokenValue = sessionManager.getToken()
        
        if (tokenValue.isNullOrEmpty()) {
            _isLoading.value = false
            _error.value = "Authentication token is missing"
            _updateSuccess.value = false
            return
        }
        
        val token = "Bearer $tokenValue"
        val updateRequest = ProfileUpdateRequest(firstname, lastname, email, currency)
        
        userApiService.updateUser(token, updateRequest).enqueue(object : Callback<ProfileModel> {
            override fun onResponse(call: Call<ProfileModel>, response: Response<ProfileModel>) {
                _isLoading.value = false
                if (response.isSuccessful && response.body() != null) {
                    _profileData.value = response.body()
                    _updateSuccess.value = true
                } else {
                    _error.value = "Failed to update profile: ${response.code()} ${response.message()}"
                    _updateSuccess.value = false
                }
            }
            
            override fun onFailure(call: Call<ProfileModel>, t: Throwable) {
                _isLoading.value = false
                _error.value = "Network error: ${t.message}"
                _updateSuccess.value = false
            }
        })
    }
    
    fun uploadProfilePicture(sessionManager: SessionManager, imageUri: Uri, context: Context) {
        _isLoading.value = true
        val tokenValue = sessionManager.getToken()
        
        if (tokenValue.isNullOrEmpty()) {
            _isLoading.value = false
            _error.value = "Authentication token is missing"
            _updateSuccess.value = false
            return
        }
        
        val token = "Bearer $tokenValue"
        
        try {
            // Convert Uri to File
            val file = uriToFile(imageUri, context)
            
            // Create MultipartBody.Part
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("profilePicture", file.name, requestFile)
            
            userApiService.uploadProfilePicture(token, body).enqueue(object : Callback<ProfileModel> {
                override fun onResponse(call: Call<ProfileModel>, response: Response<ProfileModel>) {
                    _isLoading.value = false
                    if (response.isSuccessful && response.body() != null) {
                        _profileData.value = response.body()
                        _updateSuccess.value = true
                    } else {
                        _error.value = "Failed to upload profile picture: ${response.code()} ${response.message()}"
                        _updateSuccess.value = false
                    }
                }
                
                override fun onFailure(call: Call<ProfileModel>, t: Throwable) {
                    _isLoading.value = false
                    _error.value = "Network error: ${t.message}"
                    _updateSuccess.value = false
                }
            })
            
        } catch (e: Exception) {
            _isLoading.value = false
            _error.value = "Error processing image: ${e.message}"
            _updateSuccess.value = false
        }
    }
    
    @Throws(IOException::class)
    private fun uriToFile(uri: Uri, context: Context): File {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Failed to open input stream")
        
        val tempFile = File.createTempFile("profile_picture", ".jpg", context.cacheDir)
        tempFile.deleteOnExit()
        
        FileOutputStream(tempFile).use { fileOut ->
            inputStream.use { input ->
                input.copyTo(fileOut)
            }
        }
        
        return tempFile
    }
}
