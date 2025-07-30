package com.example.alarm_clock_2.update

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private val Context.updateDataStore: DataStore<Preferences> by preferencesDataStore(name = "update_preferences")

@Singleton
class UpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    // DataStore keys
    private val LAST_CHECK_TIME = longPreferencesKey("last_check_time")
    private val POSTPONED_VERSION = stringPreferencesKey("postponed_version")
    private val POSTPONED_UNTIL = longPreferencesKey("postponed_until")
    private val AUTO_CHECK_ENABLED = booleanPreferencesKey("auto_check_enabled")
    private val WIFI_ONLY_DOWNLOAD = booleanPreferencesKey("wifi_only_download")

    companion object {
        private const val GITHUB_API_URL = "https://api.github.com/repos/LAW-t/alarm_clock/releases/latest"
        private const val APK_FILE_NAME = "alarm_clock_update.apk"
        private const val POSTPONE_DURATION_HOURS = 24L // Postpone for 24 hours
    }

    /**
     * Check for app updates from GitHub releases
     */
    suspend fun checkForUpdates(currentVersion: String): Result<UpdateInfo?> {
        return try {
            val request = Request.Builder()
                .url(GITHUB_API_URL)
                .addHeader("Accept", "application/vnd.github.v3+json")
                .build()

            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return Result.failure(Exception("API request failed: ${response.code}"))
            }

            val responseBody = response.body?.string()
                ?: return Result.failure(Exception("Empty response body"))

            val release = json.decodeFromString<GitHubRelease>(responseBody)
            
            // Skip pre-release versions
            if (release.prerelease) {
                return Result.success(null)
            }

            // Find APK asset
            val apkAsset = release.assets.find { it.name.endsWith(".apk") }
                ?: return Result.failure(Exception("No APK found in release"))

            val latestVersion = release.tagName.removePrefix("v")
            val isNewer = isVersionNewer(currentVersion, latestVersion)

            if (!isNewer) {
                return Result.success(null)
            }

            // Check if user has postponed this version
            val preferences = getUpdatePreferences().first()
            if (preferences.postponedVersion == latestVersion && 
                System.currentTimeMillis() < preferences.postponedUntil) {
                return Result.success(null)
            }

            val updateInfo = UpdateInfo(
                version = latestVersion,
                versionName = release.name,
                releaseNotes = release.body,
                downloadUrl = apkAsset.browserDownloadUrl,
                fileSize = apkAsset.size,
                isNewerVersion = true
            )

            // Update last check time
            updateLastCheckTime()

            Result.success(updateInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Download APK file using DownloadManager
     */
    fun downloadUpdate(updateInfo: UpdateInfo): Long {
        val request = DownloadManager.Request(Uri.parse(updateInfo.downloadUrl))
            .setTitle("倒班闹钟更新")
            .setDescription("正在下载版本 ${updateInfo.version}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, APK_FILE_NAME)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)

        return downloadManager.enqueue(request)
    }

    /**
     * Get download progress
     */
    fun getDownloadProgress(downloadId: Long): DownloadProgress? {
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)
        
        return if (cursor.moveToFirst()) {
            val bytesDownloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val totalBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            
            val progress = if (totalBytes > 0) {
                ((bytesDownloaded * 100) / totalBytes).toInt()
            } else 0

            cursor.close()
            DownloadProgress(downloadId, bytesDownloaded, totalBytes, progress, status)
        } else {
            cursor.close()
            null
        }
    }

    /**
     * Get downloaded APK file path
     */
    fun getDownloadedFilePath(downloadId: Long): String? {
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)
        
        return if (cursor.moveToFirst()) {
            val uri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
            cursor.close()
            uri?.let { Uri.parse(it).path }
        } else {
            cursor.close()
            null
        }
    }

    /**
     * Cancel download
     */
    fun cancelDownload(downloadId: Long) {
        downloadManager.remove(downloadId)
    }

    /**
     * Postpone update for specified version
     */
    suspend fun postponeUpdate(version: String) {
        context.updateDataStore.edit { preferences ->
            preferences[POSTPONED_VERSION] = version
            preferences[POSTPONED_UNTIL] = System.currentTimeMillis() + (POSTPONE_DURATION_HOURS * 60 * 60 * 1000)
        }
    }

    /**
     * Clear postponed update
     */
    suspend fun clearPostponedUpdate() {
        context.updateDataStore.edit { preferences ->
            preferences.remove(POSTPONED_VERSION)
            preferences.remove(POSTPONED_UNTIL)
        }
    }

    /**
     * Update last check time
     */
    private suspend fun updateLastCheckTime() {
        context.updateDataStore.edit { preferences ->
            preferences[LAST_CHECK_TIME] = System.currentTimeMillis()
        }
    }

    /**
     * Get update preferences
     */
    fun getUpdatePreferences(): Flow<UpdatePreferences> {
        return context.updateDataStore.data.map { preferences ->
            UpdatePreferences(
                lastCheckTime = preferences[LAST_CHECK_TIME] ?: 0L,
                postponedVersion = preferences[POSTPONED_VERSION],
                postponedUntil = preferences[POSTPONED_UNTIL] ?: 0L,
                autoCheckEnabled = preferences[AUTO_CHECK_ENABLED] ?: true,
                wifiOnlyDownload = preferences[WIFI_ONLY_DOWNLOAD] ?: true
            )
        }
    }

    /**
     * Compare version strings to determine if new version is newer
     */
    private fun isVersionNewer(currentVersion: String, newVersion: String): Boolean {
        val current = parseVersion(currentVersion)
        val new = parseVersion(newVersion)
        
        return when {
            new.major > current.major -> true
            new.major < current.major -> false
            new.minor > current.minor -> true
            new.minor < current.minor -> false
            new.patch > current.patch -> true
            else -> false
        }
    }

    private fun parseVersion(version: String): Version {
        val parts = version.split(".")
        return Version(
            major = parts.getOrNull(0)?.toIntOrNull() ?: 0,
            minor = parts.getOrNull(1)?.toIntOrNull() ?: 0,
            patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
        )
    }

    private data class Version(val major: Int, val minor: Int, val patch: Int)
}
