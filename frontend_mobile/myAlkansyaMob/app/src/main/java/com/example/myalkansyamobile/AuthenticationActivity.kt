package com.example.myalkansyamobile

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.myalkansyamobile.databinding.ActivityAuthenticationBinding
import com.example.myalkansyamobile.utils.SessionManager
import java.util.concurrent.Executor

class AuthenticationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthenticationBinding
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthenticationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // Check if user is already authenticated in this session
        if (sessionManager.isAuthenticatedThisSession()) {
            proceedToNextScreen()
            return
        }

        setupBiometricAuthentication()
        checkBiometricSupportAndAuthenticate()
    }

    private fun setupBiometricAuthentication() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(this@AuthenticationActivity, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                    finish()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    markAsAuthenticated()
                    Handler(Looper.getMainLooper()).postDelayed({
                        proceedToNextScreen()
                    }, 500)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(this@AuthenticationActivity, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Authentication")
            .setSubtitle("Authenticate to access MyAlkansya")
            .setNegativeButtonText("Cancel")
            .build()
    }

    private fun checkBiometricSupportAndAuthenticate() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> biometricPrompt.authenticate(promptInfo)
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Toast.makeText(this, "No biometric hardware available", Toast.LENGTH_SHORT).show()
                proceedToNextScreen()
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Toast.makeText(this, "Biometric hardware unavailable", Toast.LENGTH_SHORT).show()
                proceedToNextScreen()
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Toast.makeText(this, "No biometric credentials enrolled", Toast.LENGTH_SHORT).show()
                proceedToNextScreen()
            }
            else -> {
                Toast.makeText(this, "Biometric authentication not supported", Toast.LENGTH_SHORT).show()
                proceedToNextScreen()
            }
        }
    }

    private fun markAsAuthenticated() {
        sessionManager.setAuthenticatedThisSession(true)
    }

    private fun proceedToNextScreen() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
