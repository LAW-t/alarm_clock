package com.example.alarm_clock_2.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object NotificationPermission {

    private const val REQUEST_CODE = 0x1033

    /** Returns true if we are on Android 13+ and the notification permission is NOT yet granted. */
    fun needsToRequest(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    }

    /** Request runtime notification permission if required (Android 13+). */
    fun request(activity: Activity) {
        if (!needsToRequest(activity)) return
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_CODE
        )
    }
} 