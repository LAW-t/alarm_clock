package com.example.alarm_clock_2.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File

object UpdateUtils {
    
    /**
     * Install APK file
     */
    fun installApk(context: Context, filePath: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                throw Exception("APK file not found: $filePath")
            }

            val intent = Intent(Intent.ACTION_VIEW)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Use FileProvider for Android 7.0+
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else {
                Uri.fromFile(file)
            }

            intent.setDataAndType(uri, "application/vnd.android.package-archive")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            context.startActivity(intent)
        } catch (e: Exception) {
            throw Exception("Failed to install APK: ${e.message}")
        }
    }

    /**
     * Check if app can install packages from unknown sources
     */
    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true // Pre-Android 8.0 doesn't need this permission
        }
    }

    /**
     * Open settings to allow installing from unknown sources
     */
    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
            intent.data = Uri.parse("package:${context.packageName}")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }

    /**
     * Get current app version name
     */
    fun getCurrentVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "0.0.0"
        } catch (e: Exception) {
            "0.0.0"
        }
    }

    /**
     * Clean up downloaded APK files
     */
    fun cleanupDownloadedFiles(context: Context) {
        try {
            val downloadDir = File(context.getExternalFilesDir(null), "downloads")
            if (downloadDir.exists()) {
                downloadDir.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".apk")) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }

    /**
     * Check if device has enough storage space for download
     */
    fun hasEnoughStorage(context: Context, requiredBytes: Long): Boolean {
        return try {
            val downloadDir = context.getExternalFilesDir(null)
            downloadDir?.usableSpace?.let { availableBytes ->
                availableBytes > requiredBytes + (10 * 1024 * 1024) // 10MB buffer
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get download directory
     */
    fun getDownloadDirectory(context: Context): File {
        val downloadDir = File(context.getExternalFilesDir(null), "downloads")
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
        return downloadDir
    }
}
