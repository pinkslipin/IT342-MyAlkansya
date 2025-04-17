package com.example.myalkansyamobile.ui.profile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.myalkansyamobile.HomePageActivity
import com.example.myalkansyamobile.R
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.databinding.ActivityProfileBinding
import com.example.myalkansyamobile.model.ProfileModel
import com.example.myalkansyamobile.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize session manager
        sessionManager = SessionManager(this)

        // Set up the toolbar - handle possible null case
        try {
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "My Profile"
        } catch (e: Exception) {
            Log.e("ProfileActivity", "Error setting up toolbar: ${e.message}")
            // Continue without toolbar if it fails
        }

        // Set up back button
        binding.btnBackToHome.setOnClickListener {
            navigateToHome()
        }

        // Load profile data
        loadProfileData()
    }

    private fun loadProfileData() {
        // Show loading state
        try {
            binding.progressBar.visibility = View.VISIBLE
            binding.profileContent.visibility = View.GONE
            binding.tvError.visibility = View.GONE
        } catch (e: Exception) {
            Log.e("ProfileActivity", "Error accessing view elements: ${e.message}")
        }

        // Use session data first (this doesn't require API calls)
        displaySessionProfileData()
        
        // Then try to get fresh data from API
        fetchProfileFromApi()
    }

    private fun displaySessionProfileData() {
        try {
            // Use data from SessionManager for immediate display
            val firstName = sessionManager.getFirstName() ?: ""
            val lastName = sessionManager.getLastName() ?: ""
            binding.tvName.text = "$firstName $lastName"
            binding.tvEmail.text = sessionManager.getUserEmail() ?: ""
            binding.tvCurrency.text = "Preferred Currency: ${sessionManager.getCurrency() ?: "USD"}"

            // Load profile picture if available
            val profilePicUrl = sessionManager.getProfilePicture()
            if (!profilePicUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(profilePicUrl)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .into(binding.ivProfileImage)
            }

            // Show content once session data is loaded
            binding.progressBar.visibility = View.GONE
            binding.profileContent.visibility = View.VISIBLE
        } catch (e: Exception) {
            Log.e("ProfileActivity", "Error displaying session data: ${e.message}")
            showErrorMessage("Error loading profile information")
        }
    }

    private fun fetchProfileFromApi() {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    Log.w("ProfileActivity", "No authentication token found")
                    return@launch
                }

                // Fetch profile data from API
                val authToken = "Bearer $token"
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.userApiService.getUserProfile(authToken).execute()
                }

                if (response.isSuccessful && response.body() != null) {
                    // Display fresh data from API
                    response.body()?.let { displayProfileData(it) }
                } else {
                    Log.e("ProfileActivity", "Failed to get profile: ${response.code()}, ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("ProfileActivity", "Error fetching profile from API", e)
                // We already have session data displayed, so just log the error
            }
        }
    }

    private fun displayProfileData(profile: ProfileModel) {
        try {
            // Update UI with profile data
            binding.tvName.text = "${profile.firstname} ${profile.lastname}"
            binding.tvEmail.text = profile.email
            binding.tvCurrency.text = "Preferred Currency: ${profile.currency ?: "USD"}"

            // Load profile picture if available
            if (!profile.profilePicture.isNullOrEmpty()) {
                Glide.with(this)
                    .load(profile.profilePicture)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .into(binding.ivProfileImage)
            }

            // Save updated profile info to session
            sessionManager.saveFirstName(profile.firstname)
            sessionManager.saveLastName(profile.lastname)
            sessionManager.saveEmail(profile.email)
            sessionManager.saveCurrency(profile.currency ?: "USD")
            if (!profile.profilePicture.isNullOrEmpty()) {
                sessionManager.saveProfilePicture(profile.profilePicture)
            }
        } catch (e: Exception) {
            Log.e("ProfileActivity", "Error updating profile UI: ${e.message}")
        }
    }

    private fun showErrorMessage(message: String) {
        try {
            binding.progressBar.visibility = View.GONE
            binding.tvError.text = message
            binding.tvError.visibility = View.VISIBLE
            binding.profileContent.visibility = View.GONE
        } catch (e: Exception) {
            Log.e("ProfileActivity", "Error showing error message: ${e.message}")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            navigateToHome()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomePageActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        // Override back button to navigate to home
        navigateToHome()
    }
}
