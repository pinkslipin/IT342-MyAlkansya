package com.example.myalkansyamobile.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.util.Base64
import com.example.myalkansyamobile.api.RetrofitClient
import com.example.myalkansyamobile.api.ChangeCurrencyRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.util.Calendar

class SessionManager(context: Context) {
    private var prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val editor = prefs.edit()

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
        const val TOKEN_EXPIRY = "token_expiry"
        const val KEY_EMAIL = "email"
        const val KEY_AUTH_SESSION = "authenticated_this_session"
    }

    // Create full login session
    fun createLoginSession(token: String, userId: Int, username: String, email: String) {
        editor.putString(USER_TOKEN, token)
        editor.putInt(USER_ID, userId)
        editor.putString(USERNAME, username)
        editor.putString(USER_EMAIL, email)
        editor.putBoolean(IS_LOGGED_IN, true)
        // Default analytics access to false until verified
        editor.putBoolean(HAS_ANALYTICS_ACCESS, false)
        // Set token expiry time (24 hours from now)
        val expiryTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000)
        editor.putLong(TOKEN_EXPIRY, expiryTime)
        // Set default currency to PHP for all logins
        editor.putString(USER_CURRENCY, "PHP")
        editor.apply()
    }

    // Save auth token
    fun saveAuthToken(token: String) {
        editor.putString(USER_TOKEN, token)
        // Set token expiry time (24 hours from now)
        val expiryTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000)
        editor.putLong(TOKEN_EXPIRY, expiryTime)
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
            return withContext(Dispatchers.IO) {
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
            
            // Get current month and year
            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH) + 1 // Calendar months are 0-based
            val currentYear = calendar.get(Calendar.YEAR)
            
            // Use withContext to run on IO thread
            return withContext(Dispatchers.IO) {
                try {
                    // For suspend functions, we don't use execute()
                    analyticsService.getFinancialSummary(bearerToken, currentMonth, currentYear)
                    // If we get here without exception, the call was successful
                    Log.d("SessionManager", "Analytics token validation result: success")
                    true
                } catch (e: Exception) {
                    Log.w("SessionManager", "Analytics token validation failed: ${e.message}")
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
        editor.remove(USER_TOKEN)
        editor.remove(TOKEN_EXPIRY)
        editor.apply()
    }

    // Save username
    fun saveUsername(username: String) {
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
        editor.putString(USER_EMAIL, email)
        editor.apply()
    }

    // Fetch user email
    fun getUserEmail(): String? {
        return prefs.getString(USER_EMAIL, null)
    }

    /**
     * Retrieve the user's email from shared preferences
     * @return The user's email or null if not found
     */
    fun fetchUserEmail(): String? {
        return prefs.getString(KEY_EMAIL, null)
    }

    // Save user ID
    fun saveUserId(userId: Int) {
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
        editor.putString(USER_FIRSTNAME, firstName)
        editor.apply()
    }

    // Fetch user first name
    fun getFirstName(): String? {
        return prefs.getString(USER_FIRSTNAME, null)
    }

    // Save user last name
    fun saveLastName(lastName: String) {
        editor.putString(USER_LASTNAME, lastName)
        editor.apply()
    }

    // Fetch user last name
    fun getLastName(): String? {
        return prefs.getString(USER_LASTNAME, null)
    }

    // Save user currency
    fun saveCurrency(currency: String) {
        editor.putString(USER_CURRENCY, currency)
        editor.apply()
    }

    // Fetch user currency
    fun getCurrency(): String? {
        return prefs.getString(USER_CURRENCY, "PHP")  // Default to PHP instead of USD
    }

    /**
     * Updates the user's preferred currency and triggers a backend currency conversion
     * 
     * @param newCurrency The new currency code
     * @return true if successful, false otherwise
     */
    suspend fun updateCurrency(newCurrency: String): Boolean {
        val oldCurrency = getCurrency() ?: "USD"
        // Skip if no change
        if (oldCurrency == newCurrency) return true
        
        try {
            val token = getToken() ?: return false
            
            // Call backend to convert all financial data
            val response = withContext(Dispatchers.IO) {
                RetrofitClient.userApiService.changeCurrency(
                    "Bearer $token",
                    ChangeCurrencyRequest(newCurrency, oldCurrency)
                )
            }
            
            if (response.isSuccessful) {
                // Update locally stored currency preference
                prefs.edit().putString(USER_CURRENCY, newCurrency).apply()
                return true
            }
            return false
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    // Save profile picture URL
    fun saveProfilePicture(pictureUrl: String) {
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

    // Check if user has authenticated in the current session
    fun isAuthenticatedThisSession(): Boolean {
        return prefs.getBoolean(KEY_AUTH_SESSION, false)
    }

    // Set authentication state for the current session
    fun setAuthenticatedThisSession(authenticated: Boolean) {
        editor.putBoolean(KEY_AUTH_SESSION, authenticated).apply()
    }

    // Clear session data
    fun clearSession() {
        editor.clear()
        editor.apply()
        editor.putBoolean(KEY_AUTH_SESSION, false).apply()
    }

    /**
     * Determines if an error should be treated as an authentication error
     */
    fun isAuthError(response: Response<*>): Boolean {
        // Check for 401 Unauthorized or 403 Forbidden
        if (response.code() == 401 || response.code() == 403) {
            return true
        }
        
        // Check error message for auth-related keywords
        val errorBody = response.errorBody()?.string()?.lowercase() ?: ""
        return errorBody.contains("authentication required") || 
               errorBody.contains("unauthorized") ||
               errorBody.contains("unauthenticated") ||
               errorBody.contains("session expired") ||
               errorBody.contains("invalid token") ||
               errorBody.contains("not logged in")
    }

    /**
     * Handle 404 Not Found errors differently from authentication errors 
     */
    fun isResourceNotFound(response: Response<*>): Boolean {
        return response.code() == 404
    }
}
