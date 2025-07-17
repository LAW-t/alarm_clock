package com.example.alarm_clock_2.util

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

/**
 * Utility helpers for Android 12+ precise alarm permission.
 */
object ExactAlarmPermission {

    /** Returns true if we are on Android 12+ and the permission is NOT yet granted. */
    fun needsToRequest(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
    }

    /**
     * Launches the system settings page asking user to grant the *Precise alarms* permission.
     * Does nothing on pre-S devices or when permission already granted.
     */
    fun request(context: Context) {
        if (!needsToRequest(context)) return
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        // From activity context we can start directly, otherwise needs FLAG_ACTIVITY_NEW_TASK.
        if (context !is android.app.Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
} 