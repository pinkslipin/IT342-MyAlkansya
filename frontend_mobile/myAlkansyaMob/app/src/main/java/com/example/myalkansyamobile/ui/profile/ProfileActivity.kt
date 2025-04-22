package com.example.myalkansyamobile.ui.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.myalkansyamobile.HomePageActivity
import com.example.myalkansyamobile.R
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.databinding.ActivityProfileBinding
import com.example.myalkansyamobile.model.ProfileModel
import com.example.myalkansyamobile.utils.SessionManager
import com.example.myalkansyamobile.viewmodel.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: ProfileViewModel
    private var isEditMode = false
    private var selectedImageUri: Uri? = null

    // Result launcher for image picking
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                Glide.with(this)
                    .load(uri)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .into(binding.ivProfileImage)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize session manager
        sessionManager = SessionManager(this)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]

        // Set up the toolbar
        try {
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "My Profile"
        } catch (e: Exception) {
            Log.e("ProfileActivity", "Error setting up toolbar: ${e.message}")
        }

        // Set up button click listeners
        setupClickListeners()

        // Set up observers
        setupObservers()

        // Load profile data
        loadProfileData()
    }

    private fun setupClickListeners() {
        // Back button
        binding.btnBackToHome.setOnClickListener {
            navigateToHome()
        }

        // Edit profile button
        binding.btnEditProfile.setOnClickListener {
            toggleEditMode(true)
        }

        // Save profile button
        binding.btnSaveProfile.setOnClickListener {
            saveProfileChanges()
        }

        // Cancel edit button
        binding.btnCancelEdit.setOnClickListener {
            toggleEditMode(false)
        }

        // Change profile picture button
        binding.btnChangePicture.setOnClickListener {
            openImagePicker()
        }
    }

    private fun setupObservers() {
        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe error messages
        viewModel.error.observe(this) { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) {
                binding.tvError.text = errorMessage
                binding.tvError.visibility = View.VISIBLE
                binding.profileContent.visibility = View.GONE
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            } else {
                binding.tvError.visibility = View.GONE
            }
        }

        // Observe profile data
        viewModel.profileData.observe(this) { profileData ->
            displayProfileData(profileData)
        }

        // Observe update success
        viewModel.updateSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                toggleEditMode(false)
                
                // If we have a selected image, upload it
                selectedImageUri?.let { uri ->
                    viewModel.uploadProfilePicture(sessionManager, uri, this)
                    selectedImageUri = null
                }
            }
        }
    }

    private fun loadProfileData() {
        // Show loading state
        binding.progressBar.visibility = View.VISIBLE
        binding.profileContent.visibility = View.GONE
        binding.tvError.visibility = View.GONE

        // Use session data first for immediate display
        displaySessionProfileData()
        
        // Then fetch fresh data from API
        viewModel.fetchUserProfile(sessionManager)
    }

    private fun displaySessionProfileData() {
        try {
            // Use data from SessionManager for immediate display
            val firstName = sessionManager.getFirstName() ?: ""
            val lastName = sessionManager.getLastName() ?: ""
            binding.tvName.text = "$firstName $lastName"
            binding.tvEmail.text = sessionManager.getUserEmail() ?: ""
            binding.tvCurrency.text = "Preferred Currency: ${sessionManager.getCurrency() ?: "USD"}"

            // Also populate the edit fields
            binding.etFirstName.setText(firstName)
            binding.etLastName.setText(lastName)
            binding.etEmail.setText(sessionManager.getUserEmail() ?: "")
            binding.etCurrency.setText(sessionManager.getCurrency() ?: "USD")

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

    private fun displayProfileData(profile: ProfileModel) {
        try {
            // Update UI with profile data - view mode
            binding.tvName.text = "${profile.firstname} ${profile.lastname}"
            binding.tvEmail.text = profile.email
            binding.tvCurrency.text = "Preferred Currency: ${profile.currency ?: "USD"}"

            // Update edit fields
            binding.etFirstName.setText(profile.firstname)
            binding.etLastName.setText(profile.lastname)
            binding.etEmail.setText(profile.email)
            binding.etCurrency.setText(profile.currency ?: "USD")

            // Load profile picture if available
            if (!profile.profilePicture.isNullOrEmpty()) {
                Glide.with(this)
                    .load(profile.profilePicture)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .into(binding.ivProfileImage)
                
                // Update session with the profile picture URL
                sessionManager.saveProfilePicture(profile.profilePicture)
            }

            // Save updated profile info to session
            sessionManager.saveFirstName(profile.firstname)
            sessionManager.saveLastName(profile.lastname)
            sessionManager.saveEmail(profile.email)
            sessionManager.saveCurrency(profile.currency ?: "USD")
        } catch (e: Exception) {
            Log.e("ProfileActivity", "Error updating profile UI: ${e.message}")
        }
    }

    private fun toggleEditMode(isEdit: Boolean) {
        isEditMode = isEdit
        
        // Toggle visibility between view and edit modes
        binding.viewModeLayout.visibility = if (isEdit) View.GONE else View.VISIBLE
        binding.editModeLayout.visibility = if (isEdit) View.VISIBLE else View.GONE
        binding.btnEditProfile.visibility = if (isEdit) View.GONE else View.VISIBLE
        binding.btnChangePicture.visibility = if (isEdit) View.VISIBLE else View.GONE
    }

    private fun saveProfileChanges() {
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val currency = binding.etCurrency.text.toString().trim()

        // Validate inputs
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Update profile via ViewModel
        viewModel.updateProfile(
            sessionManager = sessionManager,
            firstname = firstName,
            lastname = lastName,
            email = email,
            currency = currency
        )
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
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
            // If in edit mode, cancel edits instead of navigating back
            if (isEditMode) {
                toggleEditMode(false)
                return true
            }
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
        // If in edit mode, cancel edits instead of going back
        if (isEditMode) {
            toggleEditMode(false)
            return
        }
        // Otherwise, navigate to home
        navigateToHome()
    }
}
