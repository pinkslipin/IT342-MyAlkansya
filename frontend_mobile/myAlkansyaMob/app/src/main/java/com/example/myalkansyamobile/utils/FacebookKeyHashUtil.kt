package com.example.myalkansyamobile.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import android.util.Log
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object FacebookKeyHashUtil {
    private const val TAG = "KeyHash"

    /**
     * Generates and logs the key hash for Facebook authentication
     */
    fun logKeyHash(context: Context) {
        try {
            val info: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName, 
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                info.signingInfo?.apkContentsSigners?.forEach { signature ->
                    val md = MessageDigest.getInstance("SHA")
                    md.update(signature.toByteArray())
                    val keyHash = Base64.encodeToString(md.digest(), Base64.DEFAULT)
                    Log.d(TAG, "Key hash for Facebook: $keyHash")
                }
            } else {
                @Suppress("DEPRECATION")
                info.signatures?.forEach { signature ->
                    val md = MessageDigest.getInstance("SHA")
                    md.update(signature.toByteArray())
                    val keyHash = Base64.encodeToString(md.digest(), Base64.DEFAULT)
                    Log.d(TAG, "Key hash for Facebook: $keyHash")
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Package name not found", e)
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, "No such algorithm", e)
        }
    }
}
