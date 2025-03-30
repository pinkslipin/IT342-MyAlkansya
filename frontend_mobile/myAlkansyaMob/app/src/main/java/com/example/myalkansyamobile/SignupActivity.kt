package com.example.myalkansyamobile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myalkansyamobile.databinding.ActivitySignupBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9002 // Different request code from SignInActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.server_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Email sign up
        binding.signUpButton.setOnClickListener {
            if (validateSignUpForm()) {
                // TODO: Implement actual sign up logic
                Toast.makeText(this, "Sign up successful!", Toast.LENGTH_SHORT).show()
                navigateToSignIn()
            }
        }

        // Text link to sign in
        binding.txtSignIn.setOnClickListener {
            navigateToSignIn()
        }

        // Google sign up
        binding.btnGoogle.setOnClickListener {
            signInWithGoogle()
        }

        // Facebook sign up
        binding.btnFacebook.setOnClickListener {
            Toast.makeText(this, "Facebook sign up will be implemented", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateSignUpForm(): Boolean {
        // Implement your validation logic
        val name = binding.inputName.text.toString()
        val email = binding.inputEmail.text.toString()
        val password = binding.inputPassword.text.toString()
        val confirmPassword = binding.confirmPassword.text.toString()

        var isValid = true

        if (name.isEmpty()) {
            binding.inputName.error = "Name is required"
            isValid = false
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.inputEmail.error = "Valid email is required"
            isValid = false
        }

        if (password.length < 6) {
            binding.inputPassword.error = "Password must be at least 6 characters"
            isValid = false
        }

        if (password != confirmPassword) {
            binding.confirmPassword.error = "Passwords don't match"
            isValid = false
        }

        return isValid
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { token ->
                    // TODO: Send token to your backend
                    Toast.makeText(this, "Google authentication successful", Toast.LENGTH_SHORT).show()
                    navigateToHome()
                }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToSignIn() {
        startActivity(Intent(this, SignInActivity::class.java))
        finish() // Remove SignUpActivity from back stack
    }

    private fun navigateToHome() {
        startActivity(Intent(this, HomePageActivity::class.java))
        finish()
    }
}