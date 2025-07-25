package com.example.alarm_clock_2.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

object Updater {
    private const val LATEST_API = "https://api.github.com/repos/LAW-t/alarm_clock/releases/latest"

    /** in-memory cache; clears after [CACHE_WINDOW_MS] */
    private var cachedInfo: ReleaseInfo? = null
    private var lastFetch: Long = 0L
    private const val CACHE_WINDOW_MS = 6 * 60 * 60 * 1000L // 6 hours

    data class ReleaseInfo(val version:String, val apkUrl:String)

    suspend fun fetchLatest(): ReleaseInfo? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (cachedInfo != null && now - lastFetch < CACHE_WINDOW_MS) {
            return@withContext cachedInfo
        }

        return@withContext try {
            val conn = URL(LATEST_API).openConnection() as java.net.HttpURLConnection
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "alarm-clock-app")
            conn.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            if (conn.responseCode != 200) return@withContext null
            val json = conn.inputStream.bufferedReader().use { it.readText() }
            val obj = JSONObject(json)
            val version = obj.getString("tag_name")
            val assets = obj.getJSONArray("assets")
            var apkUrl = ""
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.getString("name")
                if (name.endsWith(".apk")) {
                    apkUrl = asset.getString("browser_download_url")
                    break
                }
            }
            val info = if (apkUrl.isBlank()) null else ReleaseInfo(version, apkUrl)
            if (info != null) {
                cachedInfo = info
                lastFetch = now
            }
            info
        } catch (e: Exception) {
            null
        }
    }

    fun startDownload(context: Context, url: String, onEnqueued: ()->Unit) {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = Uri.parse(url)
        val request = DownloadManager.Request(uri)
            .setTitle("下载更新")
            .setDescription("正在下载最新版本…")
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "alarm_clock_update.apk")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
        val id = dm.enqueue(request)
        onEnqueued()

        // Register receiver to listen for download completion
        val completeReceiver = object : BroadcastReceiver() {
            override fun onReceive(ct: Context, intent: Intent) {
                val completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (completedId == id) {
                    context.unregisterReceiver(this)
                    val apkUri = dm.getUriForDownloadedFile(id)
                    if (apkUri != null) {
                        val install = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(apkUri, "application/vnd.android.package-archive")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(install)
                    } else {
                        Toast.makeText(context, "下载失败", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        // Use ContextCompat to supply the required exported/not-exported flag
        androidx.core.content.ContextCompat.registerReceiver(
            context,
            completeReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }
} 