package com.example.myalkansyamobile

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myalkansyamobile.auth.SessionManager
import com.example.myalkansyamobile.databinding.ActivityHomepageBinding
import kotlinx.coroutines.launch

class HomePageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomepageBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomepageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // Set welcome message if you want
        binding.txtWelcometoHomePage.text = "Welcome to Home Page"

        binding.btnLogout.setOnClickListener {
            logoutUser()
        }
    }

    private fun logoutUser() {
        lifecycleScope.launch {
            sessionManager.clearSession()
            val intent = Intent(this@HomePageActivity, SignInActivity::class.java)
            // Clear back stack so user can't go back after logout
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}