package com.example.alarm_clock_2.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GitHub Release API response model
 */
@Serializable
data class GitHubRelease(
    @SerialName("tag_name")
    val tagName: String,
    @SerialName("name")
    val name: String,
    @SerialName("body")
    val body: String,
    @SerialName("prerelease")
    val prerelease: Boolean = false,
    @SerialName("assets")
    val assets: List<GitHubAsset>
)

@Serializable
data class GitHubAsset(
    @SerialName("name")
    val name: String,
    @SerialName("browser_download_url")
    val browserDownloadUrl: String,
    @SerialName("size")
    val size: Long
)

/**
 * Update information processed from GitHub release
 */
data class UpdateInfo(
    val version: String,
    val versionName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val fileSize: Long,
    val isNewerVersion: Boolean
)

/**
 * Update state management
 */
sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    object NoUpdateAvailable : UpdateState()
    data class UpdateAvailable(val updateInfo: UpdateInfo) : UpdateState()
    data class Downloading(val progress: Int, val downloadId: Long) : UpdateState()
    data class DownloadComplete(val filePath: String) : UpdateState()
    data class Error(val message: String, val canRetry: Boolean = true) : UpdateState()
}

/**
 * Download progress information
 */
data class DownloadProgress(
    val downloadId: Long,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val progress: Int,
    val status: Int
)

/**
 * User preferences for updates
 */
data class UpdatePreferences(
    val lastCheckTime: Long = 0L,
    val postponedVersion: String? = null,
    val postponedUntil: Long = 0L,
    val autoCheckEnabled: Boolean = true,
    val wifiOnlyDownload: Boolean = true
)
