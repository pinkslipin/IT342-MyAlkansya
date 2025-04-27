package com.example.myalkansyamobile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.utils.SessionManager
import com.example.myalkansyamobile.databinding.ActivitySigninBinding
import com.example.myalkansyamobile.api.AuthRepository
import com.example.myalkansyamobile.model.AuthResponse
import com.example.myalkansyamobile.model.LoginRequest
import com.example.myalkansyamobile.model.UserDTO
import com.example.myalkansyamobile.utils.Resource
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.gson.Gson
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

        // Use the authApiService property directly
        authRepository = AuthRepository(RetrofitClient.authApiService)
        sessionManager = SessionManager(this)

        if (sessionManager.fetchAuthToken() != null && sessionManager.isLoggedIn()) {
            navigateToHome()
            return
        }

        // Initialize Google Sign-In
        setupGoogleSignIn()
        
        // Initialize Facebook Login
        callbackManager = CallbackManager.Factory.create()
        
        // Initialize Activity Result Launchers
        setupActivityResultLaunchers()
        
        setupClickListeners()
    }
    
    private fun setupGoogleSignIn() {
        // Request both authentication and Google Sheets scopes together
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.server_client_id))
            .requestEmail()
            .requestProfile()
            // Add Google Sheets scope to initial sign-in
            .requestScopes(Scope(SheetsScopes.SPREADSHEETS))
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        
        binding.btnGoogle2.setOnClickListener {
            showLoading(true)
            signInWithGoogle()
        }
    }

    private fun setupActivityResultLaunchers() {
        googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> 
            if (result.resultCode == RESULT_OK) {
                try {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    handleGoogleSignInResult(task)
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
                    handleSuccessfulAuth(result)
                }
                is Resource.Error -> {
                    showLoading(false)
                    Log.e("LoginActivity", "Login Error: ${result.message}")
                    Toast.makeText(this@SignInActivity, "Login failed: ${result.message}", Toast.LENGTH_LONG).show()
                }
                is Resource.Loading -> {
                    // Handle loading state
                }
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
                    handleSuccessfulAuth(result)
                }
                is Resource.Error -> {
                    // Check for specific error indicating the user doesn't exist
                    if (result.message?.contains("User not registered", ignoreCase = true) == true) {
                        Log.d("FacebookAuth", "User not found, attempting to register")
                        registerWithFacebook(accessToken)
                    } else {
                        showLoading(false)
                        Log.e("FacebookAuth", "Error: ${result.message}")
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

    private suspend fun registerWithFacebook(accessToken: String) {
        try {
            when (val result = authRepository.registerWithFacebook(accessToken)) {
                is Resource.Success -> {
                    showLoading(false)
                    handleSuccessfulAuth(result)
                    Toast.makeText(this@SignInActivity, "Account created and signed in with Facebook", 
                        Toast.LENGTH_SHORT).show()
                }
                is Resource.Error -> {
                    showLoading(false)
                    Log.e("FacebookAuth", "Registration error: ${result.message}")
                    Toast.makeText(this@SignInActivity, "Facebook registration failed: ${result.message}", 
                        Toast.LENGTH_LONG).show()
                }
                is Resource.Loading -> {}
            }
        } catch (e: Exception) {
            showLoading(false)
            Log.e("FacebookAuth", "Exception during Facebook registration", e)
            Toast.makeText(this, "Facebook registration error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private suspend fun authenticateWithGoogle(idToken: String) {
        showLoading(true)
        Log.d("GoogleAuth", "Authenticating with Google...")

        when (val result = authRepository.authenticateWithGoogle(idToken)) {
            is Resource.Success -> {
                showLoading(false)
                handleSuccessfulAuth(result)
            }
            is Resource.Error -> {
                // Check for specific error indicating the user doesn't exist
                if (result.message?.contains("User not registered", ignoreCase = true) == true) {
                    Log.d("GoogleAuth", "User not found, attempting to register")
                    registerWithGoogle(idToken)
                } else {
                    showLoading(false)
                    Log.e("GoogleAuth", "Error: ${result.message}")
                    Toast.makeText(this@SignInActivity, "Google authentication failed: ${result.message}", 
                        Toast.LENGTH_LONG).show()
                }
            }
            is Resource.Loading -> {}
        }
    }

    private suspend fun registerWithGoogle(idToken: String) {
        Log.d("GoogleAuth", "Registering with Google...")
        when (val result = authRepository.registerWithGoogle(idToken)) {
            is Resource.Success -> {
                showLoading(false)
                handleSuccessfulAuth(result)
                Toast.makeText(this@SignInActivity, "Account created and signed in with Google", 
                    Toast.LENGTH_SHORT).show()
            }
            is Resource.Error -> {
                showLoading(false)
                Log.e("GoogleAuth", "Registration error: ${result.message}")
                Toast.makeText(this@SignInActivity, "Google registration failed: ${result.message}", 
                    Toast.LENGTH_LONG).show()
            }
            is Resource.Loading -> {}
        }
    }

    private fun handleGoogleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            
            // Check if user granted all requested scopes including Sheets
            val hasSheetScope = GoogleSignIn.hasPermissions(
                account,
                Scope(SheetsScopes.SPREADSHEETS)
            )
            
            // Log the granted scopes for debugging
            Log.d("GoogleAuth", "Google sign-in successful. Has Sheets scope: $hasSheetScope")
            
            // Save the account info for use in GoogleSheetsHelper
            val accountJson = Gson().toJson(account)
            val sharedPrefs = getSharedPreferences("google_sign_in", Context.MODE_PRIVATE)
            sharedPrefs.edit().putString("last_account", accountJson).apply()
            
            // Launch a coroutine to call the suspend function
            lifecycleScope.launch {
                // Continue with backend authentication in a coroutine context
                authenticateWithGoogle(account.idToken!!)
            }
        } catch (e: ApiException) {
            showLoading(false)
            Log.e("GoogleAuth", "Google sign-in failed", e)
            Toast.makeText(this, "Google sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleLoginSuccess(token: String, user: UserDTO) {
        // Store user token and other data in SessionManager
        sessionManager.clearSession() // Clear any existing session first
        
        // Convert the userId from String? to Int
        val userId = user.userId?.toIntOrNull() ?: -1
        
        sessionManager.createLoginSession(
            token = token,
            userId = userId, // Now passing an Int instead of String?
            username = "${user.firstname} ${user.lastname}".trim(),
            email = user.email
        )
        
        // Store additional user data
        user.firstname?.let { sessionManager.saveFirstName(it) }
        user.lastname?.let { sessionManager.saveLastName(it) }

        
        // Navigate to the home screen
        val intent = Intent(this@SignInActivity, HomePageActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun handleSuccessfulAuth(result: Resource.Success<AuthResponse>) {
        val authResponse = result.data
        if (authResponse == null) {
            Log.e("Auth", "Auth response is NULL!")
            Toast.makeText(this@SignInActivity, "Authentication failed: Empty response", Toast.LENGTH_LONG).show()
            return
        }
        
        val user = authResponse.user
        if (user == null) {
            Log.e("Auth", "User is NULL in AuthResponse!")
            Toast.makeText(this@SignInActivity, "Authentication failed: User data is missing", Toast.LENGTH_LONG).show()
            return
        }

        if (user.email.isNullOrEmpty()) {
            Log.e("Auth", "Email is NULL or empty in user data!")
            Toast.makeText(this@SignInActivity, "Authentication failed: Email is missing", Toast.LENGTH_LONG).show()
            return
        }
        
        // Convert userId to Int if possible, or use -1 as default
        val userId = user.userId?.toIntOrNull() ?: -1
        
        // Get firstname and lastname
        val firstname = user.firstname ?: "User"
        val lastname = user.lastname ?: ""
        
        // Create session with full name (firstname + lastname)
        sessionManager.createLoginSession(
            token = authResponse.token,
            userId = userId,
            username = "$firstname $lastname".trim(),
            email = user.email
        )
        
        // Save the individual name components for more flexibility
        sessionManager.saveFirstName(firstname)
        sessionManager.saveLastName(lastname)
        
        // Explicitly set PHP as default currency for all users (especially important for OAuth)
        sessionManager.saveCurrency("PHP")
        
        // For OAuth users (Google or Facebook), ensure currency is set to PHP
        if (user.authProvider != null || user.providerId != null) {
            Log.d("Auth", "OAuth user detected, enforcing PHP as default currency")
            sessionManager.saveCurrency("PHP")
        }
        
        Log.d("Auth", "User successfully authenticated: $firstname $lastname")
        navigateToHome()
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
