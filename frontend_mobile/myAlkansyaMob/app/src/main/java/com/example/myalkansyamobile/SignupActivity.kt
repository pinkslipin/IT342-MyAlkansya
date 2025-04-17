package com.example.myalkansyamobile

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myalkansyamobile.api.AuthRepository
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.utils.SessionManager
import com.example.myalkansyamobile.databinding.ActivitySignupBinding
import com.example.myalkansyamobile.model.RegisterRequest
import com.example.myalkansyamobile.utils.Resource
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding
    private lateinit var authRepository: AuthRepository
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize repository
        val apiService = RetrofitClient.authApiService
        authRepository = AuthRepository(apiService)
        sessionManager = SessionManager(this)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Email sign up
        binding.signUpButton.setOnClickListener {
            if (validateSignUpForm()) {
                registerUser()
            }
        }

        // Text link to sign in
        binding.txtSignIn.setOnClickListener {
            navigateToSignIn()
        }
    }

    private fun registerUser() {
        val firstName = binding.txtFirstName.text.toString()
        val lastName = binding.txtLastName.text.toString()
        val email = binding.txtEmail.text.toString()
        val password = binding.txtPassword.text.toString()
        val currency = binding.currencySpinner.selectedItem.toString() // Add a currency spinner to the layout
        
        val registerRequest = RegisterRequest(
            firstname = firstName,
            lastname = lastName,
            email = email,
            password = password,
            currency = currency
        )
        
        lifecycleScope.launch {
            // Show loading indicator
            showLoading(true)
            
            try {
                when (val result = authRepository.register(registerRequest)) {
                    is Resource.Success -> {
                        showLoading(false)
                        // Show success dialog
                        showSuccessDialog()
                    }
                    is Resource.Error -> {
                        showLoading(false)
                        Toast.makeText(this@SignUpActivity, result.message, Toast.LENGTH_LONG).show()
                    }
                    is Resource.Loading -> {
                        // Handle loading state if needed
                    }
                    else -> {
                        showLoading(false)
                        Toast.makeText(this@SignUpActivity, "Unexpected error occurred", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(this@SignUpActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showSuccessDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Success!")
            .setMessage("Your account has been created successfully.")
            .setPositiveButton("Sign In") { _, _ -> navigateToSignIn() }
            .setIcon(R.drawable.ic_success) // Make sure you have this drawable or replace it
            .setCancelable(false)
            .create()
            
        dialog.show()
        
        // Auto dismiss after 2 seconds and navigate to sign in
        Handler(Looper.getMainLooper()).postDelayed({
            dialog.dismiss()
            navigateToSignIn()
        }, 2000)
    }

    private fun validateSignUpForm(): Boolean {
        val firstName = binding.txtFirstName.text.toString()
        val lastName = binding.txtLastName.text.toString()
        val email = binding.txtEmail.text.toString()
        val password = binding.txtPassword.text.toString()
        val confirmPassword = binding.txtconfirmPassword.text.toString()

        var isValid = true

        if (firstName.isEmpty()) {
            binding.txtFirstName.error = "First name is required"
            isValid = false
        }

        if (lastName.isEmpty()) {
            binding.txtLastName.error = "Last name is required"
            isValid = false
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.txtEmail.error = "Valid email is required"
            isValid = false
        }

        if (password.length < 6) {
            binding.txtPassword.error = "Password must be at least 6 characters"
            isValid = false
        }

        if (password != confirmPassword) {
            binding.txtconfirmPassword.error = "Passwords don't match"
            isValid = false
        }

        return isValid
    }

    private fun navigateToSignIn() {
        startActivity(Intent(this, SignInActivity::class.java))
        finish() // Remove SignUpActivity from back stack
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.signUpButton.isEnabled = !isLoading
        binding.signUpButton.text = if (isLoading) "Registering..." else "Sign Up"
    }
}