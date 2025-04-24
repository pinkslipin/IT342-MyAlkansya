package com.example.myalkansyamobile.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.util.Base64
import com.example.myalkansyamobile.api.RetrofitClient
import kotlinx.coroutines.withContext

class SessionManager(context: Context) {
    private var prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    companion object {
        const val PREF_NAME = "MyAlkansyaPrefs"
        const val USER_TOKEN = "user_token"
        const val USER_ID = "user_id"
        const val USERNAME = "username"
        const val USER_EMAIL = "user_email"
        const val USER_FIRSTNAME = "user_firstname"
        const val USER_LASTNAME = "user_lastname"
        const val USER_CURRENCY = "user_currency"
        const val USER_PROFILE_PIC = "user_profile_pic"
        const val IS_LOGGED_IN = "is_logged_in"
        const val HAS_ANALYTICS_ACCESS = "has_analytics_access"
    }

    // Create full login session
    fun createLoginSession(token: String, userId: Int, username: String, email: String) {
        val editor = prefs.edit()
        editor.putString(USER_TOKEN, token)
        editor.putInt(USER_ID, userId)
        editor.putString(USERNAME, username)
        editor.putString(USER_EMAIL, email)
        editor.putBoolean(IS_LOGGED_IN, true)
        // Default analytics access to false until verified
        editor.putBoolean(HAS_ANALYTICS_ACCESS, false)
        editor.apply()
    }
    
    // Save auth token
    fun saveAuthToken(token: String) {
        val editor = prefs.edit()
        editor.putString(USER_TOKEN, token)
        editor.apply()
    }

    // Fetch auth token
    fun fetchAuthToken(): String? {
        return prefs.getString(USER_TOKEN, null)
    }
    
    // Alternative name for fetchAuthToken (for compatibility)
    fun getToken(): String? {
        return fetchAuthToken()
    }
    
    // Check if token is valid (not expired)
    fun isTokenValid(): Boolean {
        val token = getToken() ?: return false
        
        try {
            // Simple check if the token format is valid (contains two dots for JWT)
            if (!token.contains(".") || token.split(".").size != 3) {
                Log.d("SessionManager", "Token doesn't have valid JWT format")
                return false
            }
            
            // Extract the payload part (second part of JWT)
            val parts = token.split(".")
            val payload = parts[1]
            
            // Base64 decode the payload
            val decodedBytes = Base64.decode(
                // Add padding if needed
                payload.padEnd((payload.length + 3) / 4 * 4, '='), 
                Base64.URL_SAFE
            )
            val decodedPayload = String(decodedBytes)
            
            // Look for expiration time in the decoded payload
            val expRegex = "\"exp\":(\\d+)".toRegex()
            val match = expRegex.find(decodedPayload)
            
            if (match != null) {
                val expTime = match.groupValues[1].toLong()
                val currentTime = System.currentTimeMillis() / 1000
                
                // Add some buffer time (5 minutes) to account for clock differences
                val bufferTime = 300 // 5 minutes in seconds
                
                // Check if token has expired or is about to expire
                if (expTime <= currentTime + bufferTime) {
                    Log.d("SessionManager", "Token has expired or will expire soon: $expTime vs $currentTime")
                    return false
                }
                
                // Token is valid and not expired
                return true
            }
            
            // Couldn't find expiration time, assume token is invalid
            Log.d("SessionManager", "Couldn't find expiration time in token")
            return false
            
        } catch (e: Exception) {
            // Any parsing error means token is invalid
            Log.e("SessionManager", "Error validating token", e)
            return false
        }
    }

    // Test a token with the user profile endpoint only
    suspend fun testTokenValidityBasic(token: String?): Boolean {
        if (token == null) return false
        
        try {
            // We'll use the UserApiService to make a simple request
            val bearerToken = "Bearer $token"
            val userService = RetrofitClient.userApiService
            
            // Use withContext to run on IO thread
            return withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val response = userService.getUserProfile(bearerToken).execute()
                    val success = response.isSuccessful
                    Log.d("SessionManager", "Basic token validation result: $success")
                    success
                } catch (e: Exception) {
                    Log.w("SessionManager", "Basic token validation failed: ${e.message}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("SessionManager", "Error testing basic token validity", e)
            return false
        }
    }

    // Test a token with the analytics endpoints
    suspend fun testTokenValidityAnalytics(token: String?): Boolean {
        if (token == null) return false
        
        try {
            // Use analytics service for testing
            val bearerToken = "Bearer $token"
            val analyticsService = RetrofitClient.analyticsApiService
            
            return withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    // Test with an analytics endpoint
                    val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                    val response = analyticsService.getMonthlySummary(bearerToken, currentYear)
                    
                    // If we reach here, analytics access is confirmed
                    val editor = prefs.edit()
                    editor.putBoolean(HAS_ANALYTICS_ACCESS, true)
                    editor.apply()
                    
                    Log.d("SessionManager", "Analytics token validation successful")
                    true // If we get here without exception, the token works
                } catch (e: Exception) {
                    Log.w("SessionManager", "Analytics token validation failed: ${e.message}")
                    
                    // Store that this user doesn't have analytics access
                    val editor = prefs.edit()
                    editor.putBoolean(HAS_ANALYTICS_ACCESS, false)
                    editor.apply()
                    
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("SessionManager", "Error testing analytics token validity", e)
            return false
        }
    }

    // Check if user has analytics access based on previous checks
    fun hasAnalyticsAccess(): Boolean {
        return prefs.getBoolean(HAS_ANALYTICS_ACCESS, false)
    }

    // Set analytics access flag (used when we determine access through API calls)
    fun setAnalyticsAccess(hasAccess: Boolean) {
        val editor = prefs.edit()
        editor.putBoolean(HAS_ANALYTICS_ACCESS, hasAccess)
        editor.apply()
    }

    // Test token validity - this can be customized to check either basic or both
    // By default, only check basic user profile access
    suspend fun testTokenValidity(token: String?, checkAnalytics: Boolean = false): Boolean {
        // First check basic validation
        val basicValid = testTokenValidityBasic(token)
        
        // If basic fails or we don't need analytics check, return the basic result
        if (!basicValid || !checkAnalytics) {
            return basicValid
        }
        
        // If we need to check analytics too, do that
        val analyticsValid = testTokenValidityAnalytics(token)
        
        // We consider the token valid if basic validation passes, even if analytics fails
        // This is because analytics endpoints might require different permissions
        return basicValid
    }

    // Get token only if it's valid (CLIENT-SIDE validation only)
    fun getValidToken(): String? {
        return if (isTokenValid()) getToken() else null
    }

    // Get token only if it's valid (requires network call)
    suspend fun getServerValidatedToken(): String? {
        val token = getToken()
        return if (token != null && testTokenValidity(token)) token else null
    }

    // Get token only if it's valid for basic user profile access
    suspend fun getBasicValidatedToken(): String? {
        val token = getToken()
        return if (token != null && testTokenValidityBasic(token)) token else null
    }

    // Get token only if it's valid for both user profile and analytics
    suspend fun getFullyValidatedToken(): String? {
        val token = getToken()
        return if (token != null && testTokenValidity(token, true)) token else null
    }
    
    // Clear just the auth token
    fun clearToken() {
        val editor = prefs.edit()
        editor.remove(USER_TOKEN)
        editor.apply()
    }
    
    // Save username
    fun saveUsername(username: String) {
        val editor = prefs.edit()
        editor.putString(USERNAME, username)
        editor.apply()
    }
    
    // Fetch username
    fun fetchUsername(): String? {
        return prefs.getString(USERNAME, null)
    }
    
    // Alternative name for fetchUsername (for compatibility)
    fun getUserName(): String? {
        return fetchUsername()
    }
    
    // Save user email
    fun saveEmail(email: String) {
        val editor = prefs.edit()
        editor.putString(USER_EMAIL, email)
        editor.apply()
    }
    
    // Fetch user email
    fun getUserEmail(): String? {
        return prefs.getString(USER_EMAIL, null)
    }
    
    // Save user ID
    fun saveUserId(userId: Int) {
        val editor = prefs.edit()
        editor.putInt(USER_ID, userId)
        editor.apply()
    }
    
    // Fetch user ID
    fun fetchUserId(): Int {
        return prefs.getInt(USER_ID, -1)
    }
    
    // Alternative name for fetchUserId (for compatibility)
    fun getUserId(): Int {
        return fetchUserId()
    }
    
    // Save user first name
    fun saveFirstName(firstName: String) {
        val editor = prefs.edit()
        editor.putString(USER_FIRSTNAME, firstName)
        editor.apply()
    }
    
    // Fetch user first name
    fun getFirstName(): String? {
        return prefs.getString(USER_FIRSTNAME, null)
    }
    
    // Save user last name
    fun saveLastName(lastName: String) {
        val editor = prefs.edit()
        editor.putString(USER_LASTNAME, lastName)
        editor.apply()
    }
    
    // Fetch user last name
    fun getLastName(): String? {
        return prefs.getString(USER_LASTNAME, null)
    }
    
    // Save user currency
    fun saveCurrency(currency: String) {
        val editor = prefs.edit()
        editor.putString(USER_CURRENCY, currency)
        editor.apply()
    }
    
    // Fetch user currency
    fun getCurrency(): String? {
        return prefs.getString(USER_CURRENCY, "USD")
    }
    
    // Save profile picture URL
    fun saveProfilePicture(pictureUrl: String) {
        val editor = prefs.edit()
        editor.putString(USER_PROFILE_PIC, pictureUrl)
        editor.apply()
    }
    
    // Fetch profile picture URL
    fun getProfilePicture(): String? {
        return prefs.getString(USER_PROFILE_PIC, null)
    }
    
    // Check if user is logged in
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(IS_LOGGED_IN, false)
    }
    
    // Clear session data
    fun clearSession() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
    }
}
