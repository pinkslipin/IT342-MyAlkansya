package com.example.myalkansyamobile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myalkansyamobile.api.AuthApiService
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.auth.SessionManager
import com.example.myalkansyamobile.databinding.ActivitySigninBinding
import com.example.myalkansyamobile.model.GoogleAuthRequest
import com.example.myalkansyamobile.model.LoginRequest
import com.example.myalkansyamobile.api.AuthRepository
import com.example.myalkansyamobile.model.AuthResponse
import com.example.myalkansyamobile.utils.Resource
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySigninBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    private lateinit var authRepository: AuthRepository
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySigninBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authRepository = AuthRepository(RetrofitClient.instance.create(AuthApiService::class.java))
        sessionManager = SessionManager(this)

        if (sessionManager.getAuthToken() != null) {
            navigateToHome()
            return
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.server_client_id))
            .requestEmail()
            .requestProfile()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.signInButton.setOnClickListener {
            val email = binding.emailInput.text.toString()
            val password = binding.passwordInput.text.toString()

            if (validateInput(email, password)) {
                loginUser(email, password)
            }
        }

        binding.btnGoogle2.setOnClickListener {
            signInWithGoogle()
        }

        binding.txtDontHaveAccount.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    private fun loginUser(email: String, password: String) {
        showLoading(true)
        lifecycleScope.launch {
            val request = LoginRequest(email, password)
            when (val result = authRepository.login(request)) {
                is Resource.Success -> {
                    showLoading(false)
                    result.data?.let { authResponse ->
                        sessionManager.saveAuthToken(authResponse.token)
                        sessionManager.saveUserDetails(authResponse.user.email, authResponse.user.email) // FIXED: Access email inside `user`
                        navigateToHome()
                    }
                }
                is Resource.Error -> {
                    showLoading(false)
                    Log.e("LoginActivity", "Login Error: ${result.message}")
                    Toast.makeText(this@SignInActivity, "Login failed: ${result.message}", Toast.LENGTH_LONG).show()
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        var isValid = true

        if (email.isEmpty()) {
            binding.emailInput.error = "Email is required"
            isValid = false
        }

        if (password.isEmpty()) {
            binding.passwordInput.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            binding.passwordInput.error = "Password must be at least 6 characters"
            isValid = false
        }

        return isValid
    }

    private fun signInWithGoogle() {
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
            } catch (e: ApiException) {
                Log.e("GoogleLogin", "Google sign-in failed", e)
                if (e.statusCode == 12502) {
                    Log.e("GoogleLogin", "Specific handling for error code 12502")
                }
                Toast.makeText(this, "Google sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
                return
            }
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            Log.d("GoogleLogin", "ID Token received: $idToken")
            if (idToken != null) {
                lifecycleScope.launch {
                    authenticateWithGoogle(idToken)
                }
            }
        }
    }

    private suspend fun authenticateWithGoogle(idToken: String) {
        showLoading(true)
        Log.d("GoogleAuth", "Authenticating with Google...")

        when (val result = authRepository.authenticateWithGoogle(idToken)) {
            is Resource.Success -> {
                showLoading(false)
                result.data?.let { authResponse ->
                    if (authResponse.user?.email.isNullOrEmpty()) { // FIXED: Properly access `user.email`
                        Log.e("GoogleAuth", "Email is NULL in AuthResponse!")
                        Toast.makeText(this@SignInActivity, "Google authentication failed: Email is missing", Toast.LENGTH_LONG).show()
                        return
                    }
                    sessionManager.saveAuthToken(authResponse.token) // FIXED: Use `token`
                    sessionManager.saveUserDetails(authResponse.user.email, authResponse.user.email)
                    navigateToHome()
                }
            }
            is Resource.Error -> {
                showLoading(false)
                Log.e("GoogleAuth", "Error: ${result.message}")
                
                // Handle case where user hasn't signed up yet
                if (result.message?.contains("No user found", ignoreCase = true) == true || 
                    result.message?.contains("not registered", ignoreCase = true) == true) {
                    Toast.makeText(this, "Please sign up with Google first before attempting to sign in", 
                        Toast.LENGTH_LONG).show()
                    
                    // Optionally navigate to sign up page
                    startActivity(Intent(this, SignUpActivity::class.java))
                } else {
                    Toast.makeText(this@SignInActivity, "Google authentication failed: ${result.message}", 
                        Toast.LENGTH_LONG).show()
                }
            }
            is Resource.Loading -> {}
        }
    }

    private fun navigateToHome() {
        startActivity(Intent(this, HomePageActivity::class.java))
        finish()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.signInButton.isEnabled = !isLoading
        binding.btnGoogle2.isEnabled = !isLoading
    }
}
