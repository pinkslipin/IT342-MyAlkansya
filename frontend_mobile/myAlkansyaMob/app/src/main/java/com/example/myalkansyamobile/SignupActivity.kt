package com.example.myalkansyamobile

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myalkansyamobile.api.AuthRepository
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.auth.SessionManager
import com.example.myalkansyamobile.databinding.ActivitySignupBinding
import com.example.myalkansyamobile.model.RegisterRequest
import com.example.myalkansyamobile.utils.Resource
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager
    private lateinit var authRepository: AuthRepository
    private lateinit var sessionManager: SessionManager
    
    // Activity Result Launchers
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Facebook SDK
        FacebookSdk.sdkInitialize(applicationContext)
        
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize repository
        val apiService = RetrofitClient.authApiService
        authRepository = AuthRepository(apiService)
        sessionManager = SessionManager(this)

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.server_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        
        // Initialize Facebook Login
        callbackManager = CallbackManager.Factory.create()
        
        // Initialize Activity Result Launchers
        setupActivityResultLaunchers()

        setupClickListeners()
    }
    
    private fun setupActivityResultLaunchers() {
        googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                try {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    val account = task.getResult(ApiException::class.java)
                    account.idToken?.let { token ->
                        // Register with Google
                        lifecycleScope.launch {
                            registerWithGoogle(token)
                        }
                    }
                } catch (e: ApiException) {
                    Toast.makeText(this, "Google sign up failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("GoogleSignUp", "Google sign up failed", e)
                }
            }
        }
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

        // Google sign up
        binding.btnGoogle.setOnClickListener {
            signUpWithGoogle()
        }

        // Facebook sign up
        binding.btnFacebook.setOnClickListener {
            signUpWithFacebook()
        }
    }

    private fun registerUser() {
        val firstName = binding.txtFirstName.text.toString()
        val lastName = binding.txtLastName.text.toString()
        val email = binding.txtEmail.text.toString()
        val password = binding.txtPassword.text.toString()
        
        val registerRequest = RegisterRequest(
            firstname = firstName,
            lastname = lastName,
            email = email,
            password = password
        )
        
        lifecycleScope.launch {
            // Show loading indicator if you have one
            binding.signUpButton.isEnabled = false
            binding.signUpButton.text = "Registering..."
            
            try {
                when (val result = authRepository.register(registerRequest)) {
                    is Resource.Success -> {
                        // Show success dialog
                        showSuccessDialog()
                    }
                    is Resource.Error -> {
                        binding.signUpButton.isEnabled = true
                        binding.signUpButton.text = "Sign Up"
                        Toast.makeText(this@SignUpActivity, result.message, Toast.LENGTH_LONG).show()
                    }
                    is Resource.Loading -> {
                        // Handle loading state if needed
                    }
                    else -> {
                        binding.signUpButton.isEnabled = true
                        binding.signUpButton.text = "Sign Up"
                        Toast.makeText(this@SignUpActivity, "Unexpected error occurred", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                binding.signUpButton.isEnabled = true
                binding.signUpButton.text = "Sign Up"
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

    private fun signUpWithGoogle() {
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }
    
    private fun signUpWithFacebook() {
        // Clear any previous login state
        LoginManager.getInstance().logOut()
        
        // Request permissions and start Facebook login flow
        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                Log.d("FacebookAuth", "Facebook login success")
                val token = result.accessToken.token
                
                lifecycleScope.launch {
                    registerWithFacebook(token)
                }
            }

            override fun onCancel() {
                Log.d("FacebookAuth", "Facebook login canceled")
                Toast.makeText(this@SignUpActivity, "Facebook signup canceled", Toast.LENGTH_SHORT).show()
            }

            override fun onError(error: FacebookException) {
                Log.e("FacebookAuth", "Facebook login error", error)
                Toast.makeText(this@SignUpActivity, "Facebook signup error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
        
        // Start the Facebook login flow with read permissions
        LoginManager.getInstance().logInWithReadPermissions(
            this,
            callbackManager,
            listOf("email", "public_profile")
        )
    }

    // This is still needed for Facebook SDK compatibility
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    private suspend fun registerWithGoogle(idToken: String) {
        binding.signUpButton.isEnabled = false
        binding.btnGoogle.isEnabled = false

        try {
            when (val result = authRepository.registerWithGoogle(idToken)) {
                is Resource.Success -> {
                    result.data?.let { authResponse ->
                        sessionManager.saveAuthToken(authResponse.token)
                        sessionManager.saveUserDetails(authResponse.user.email, authResponse.user.email)
                        showSuccessDialog()
                    }
                }
                is Resource.Error -> {
                    binding.signUpButton.isEnabled = true
                    binding.btnGoogle.isEnabled = true
                    Toast.makeText(this, "Google registration failed: ${result.message}", Toast.LENGTH_LONG).show()
                }
                is Resource.Loading -> {}
            }
        } catch (e: Exception) {
            binding.signUpButton.isEnabled = true
            binding.btnGoogle.isEnabled = true
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private suspend fun registerWithFacebook(accessToken: String) {
        binding.signUpButton.isEnabled = false
        binding.btnFacebook.isEnabled = false

        try {
            Log.d("FacebookAuth", "Starting Facebook registration with token: $accessToken")
            when (val result = authRepository.registerWithFacebook(accessToken)) {
                is Resource.Success -> {
                    result.data?.let { authResponse ->
                        sessionManager.saveAuthToken(authResponse.token)
                        sessionManager.saveUserDetails(authResponse.user.email, authResponse.user.email)
                        showSuccessDialog()
                    } ?: run {
                        binding.signUpButton.isEnabled = true
                        binding.btnFacebook.isEnabled = true
                        Toast.makeText(this, "Facebook registration failed: Empty response data", Toast.LENGTH_LONG).show()
                    }
                }
                is Resource.Error -> {
                    binding.signUpButton.isEnabled = true
                    binding.btnFacebook.isEnabled = true
                    Toast.makeText(this, "Facebook registration failed: ${result.message}", Toast.LENGTH_LONG).show()
                }
                is Resource.Loading -> {}
            }
        } catch (e: Exception) {
            binding.signUpButton.isEnabled = true
            binding.btnFacebook.isEnabled = true
            Log.e("FacebookSignUp", "Facebook registration error", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
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