package com.example.myalkansyamobile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myalkansyamobile.api.AuthApiService
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.auth.SessionManager
import com.example.myalkansyamobile.databinding.ActivitySigninBinding
import com.example.myalkansyamobile.api.AuthRepository
import com.example.myalkansyamobile.model.LoginRequest
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

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySigninBinding
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
        
        binding = ActivitySigninBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authRepository = AuthRepository(RetrofitClient.instance.create(AuthApiService::class.java))
        sessionManager = SessionManager(this)

        if (sessionManager.getAuthToken() != null) {
            navigateToHome()
            return
        }

        // Initialize Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.server_client_id))
            .requestEmail()
            .requestProfile()
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
                    val idToken = account.idToken
                    if (idToken != null) {
                        lifecycleScope.launch {
                            authenticateWithGoogle(idToken)
                        }
                    }
                } catch (e: ApiException) {
                    showLoading(false)
                    Log.e("GoogleLogin", "Google sign-in failed", e)
                    Toast.makeText(this, "Google sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                showLoading(false)
                Log.d("GoogleLogin", "Sign in canceled or failed")
            }
        }
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
        
        binding.btnFacebook2.setOnClickListener {
            signInWithFacebook()
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
        showLoading(true)
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private fun signInWithFacebook() {
        showLoading(true)
        
        // Clear previous login state
        LoginManager.getInstance().logOut()
        
        // Set up Facebook callback
        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                Log.d("FacebookAuth", "Facebook login success")
                val token = result.accessToken.token
                
                lifecycleScope.launch {
                    authenticateWithFacebook(token)
                }
            }

            override fun onCancel() {
                Log.d("FacebookAuth", "Facebook login canceled")
                showLoading(false)
                Toast.makeText(this@SignInActivity, "Facebook login canceled", Toast.LENGTH_SHORT).show()
            }

            override fun onError(error: FacebookException) {
                Log.e("FacebookAuth", "Facebook login error", error)
                showLoading(false)
                Toast.makeText(this@SignInActivity, "Facebook login error: ${error.message}", Toast.LENGTH_SHORT).show()
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

    private suspend fun authenticateWithFacebook(accessToken: String) {
        Log.d("FacebookAuth", "Authenticating with Facebook...")

        try {
            when (val result = authRepository.authenticateWithFacebook(accessToken)) {
                is Resource.Success -> {
                    showLoading(false)
                    result.data?.let { authResponse ->
                        if (authResponse.user?.email.isNullOrEmpty()) {
                            Log.e("FacebookAuth", "Email is NULL in AuthResponse!")
                            Toast.makeText(this@SignInActivity, "Facebook authentication failed: Email is missing", Toast.LENGTH_LONG).show()
                            return
                        }
                        sessionManager.saveAuthToken(authResponse.token)
                        sessionManager.saveUserDetails(authResponse.user.email, authResponse.user.email)
                        navigateToHome()
                    }
                }
                is Resource.Error -> {
                    showLoading(false)
                    Log.e("FacebookAuth", "Error: ${result.message}")
                    
                    // Handle case where user hasn't signed up yet
                    if (result.message?.contains("No user found", ignoreCase = true) == true || 
                        result.message?.contains("not registered", ignoreCase = true) == true) {
                        Toast.makeText(this, "Please sign up with Facebook first before attempting to sign in", 
                            Toast.LENGTH_LONG).show()
                        
                        // Navigate to sign up page
                        startActivity(Intent(this, SignUpActivity::class.java))
                    } else {
                        Toast.makeText(this@SignInActivity, "Facebook authentication failed: ${result.message}", 
                            Toast.LENGTH_LONG).show()
                    }
                }
                is Resource.Loading -> {}
            }
        } catch (e: Exception) {
            showLoading(false)
            Log.e("FacebookAuth", "Exception during Facebook auth", e)
            Toast.makeText(this, "Facebook authentication error: ${e.message}", Toast.LENGTH_LONG).show()
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
        binding.btnFacebook2.isEnabled = !isLoading
    }
}
