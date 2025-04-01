package com.example.myalkansyamobile

import android.app.Application
import android.util.Log
import com.example.myalkansyamobile.utils.FacebookKeyHashUtil
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Facebook SDK
        FacebookSdk.sdkInitialize(applicationContext)
        AppEventsLogger.activateApp(this)
        
        // Generate and log the key hash for Facebook
        FacebookKeyHashUtil.logKeyHash(this)
        Log.d("FacebookSDK", "Facebook SDK initialized")
    }
}
