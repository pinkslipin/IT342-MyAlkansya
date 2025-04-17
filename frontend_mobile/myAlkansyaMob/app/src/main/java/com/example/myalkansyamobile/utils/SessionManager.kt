package com.example.myalkansyamobile.utils

import android.content.Context
import android.content.SharedPreferences

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
    }

    // Create full login session
    fun createLoginSession(token: String, userId: Int, username: String, email: String) {
        val editor = prefs.edit()
        editor.putString(USER_TOKEN, token)
        editor.putInt(USER_ID, userId)
        editor.putString(USERNAME, username)
        editor.putString(USER_EMAIL, email)
        editor.putBoolean(IS_LOGGED_IN, true)
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
