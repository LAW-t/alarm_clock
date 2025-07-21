package com.example.alarm_clock_2.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.content.pm.ServiceInfo
import android.media.ToneGenerator
import android.media.AudioManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.net.Uri
import android.os.Vibrator
import android.os.VibrationEffect
import com.example.alarm_clock_2.data.SettingsDataStore
import com.example.alarm_clock_2.data.AlarmPlayMode
import kotlinx.coroutines.runBlocking
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import android.app.PendingIntent

class AlarmService : Service() {

    private val channelId = "alarm_playback_channel"

    companion object {
        const val ACTION_STOP = "com.example.alarm_clock_2.alarm.ACTION_STOP_ALARM"
        const val ACTION_PAUSE = "com.example.alarm_clock_2.alarm.ACTION_PAUSE_ALARM"
    }
    private var ringtone: Ringtone? = null
    private var toneGen: ToneGenerator? = null
    private var prevAlarmVolume: Int? = null
    private var prevMusicVolume: Int? = null
    private var prevRingVolume: Int? = null
    private var prevRingerMode: Int? = null
    private var prevInterruptionFilter: Int? = null
    private lateinit var audioManager: AudioManager
    private var vibrator: Vibrator? = null
    private var playMode: AlarmPlayMode = AlarmPlayMode.SOUND
    private var ringtoneUriStr: String = ""
    private lateinit var notificationManager: NotificationManager

    // Extras from triggering intent
    private var alarmId: Int = 0
    private var snoozeRemaining: Int = 0
    private var shift: String? = null

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Entry {
        fun settings(): SettingsDataStore
    }

    private val settings: SettingsDataStore by lazy {
        EntryPointAccessors.fromApplication(applicationContext, Entry::class.java).settings()
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        notificationManager = getSystemService(NotificationManager::class.java)
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        createChannel()

        // Post a silent foreground notification placeholder immediately to comply with 5-second rule
        // (will be updated with full content in onStartCommand)
        val placeholder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("闹钟启动中…")
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, placeholder, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(1, placeholder)
        }

        // Load settings synchronously (small IO on start):
        runBlocking {
            playMode = AlarmPlayMode.from(settings.playModeFlow.first())
            ringtoneUriStr = settings.ringtoneUriFlow.first()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_PAUSE -> {
                handlePause(intent)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        // Normal start after alarm trigger
        // Cache extras for building notification & pause handling
        alarmId = intent?.getIntExtra("alarm_id", 0) ?: 0
        snoozeRemaining = intent?.getIntExtra("snooze_remaining", 0) ?: 0
        shift = intent?.getStringExtra("shift")

        val notif = buildForegroundNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(1, notif)
        }

        // === 播放逻辑 ===
        if (playMode == AlarmPlayMode.SOUND || playMode == AlarmPlayMode.BOTH) {
            startRingtone()
        }
        if (playMode == AlarmPlayMode.VIBRATE || playMode == AlarmPlayMode.BOTH) {
            startVibration()
        }

        // 若两者都失败则 fallback tone
        if (ringtone == null && (playMode == AlarmPlayMode.SOUND || playMode == AlarmPlayMode.BOTH)) {
            fallbackTone()
        }

        return START_STICKY
    }

    private fun startRingtone() {
        var uri: Uri? = if (ringtoneUriStr.isNotBlank()) Uri.parse(ringtoneUriStr) else null
        if (uri == null) {
            uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            if (uri == null) {
                uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
        }
        ringtone = RingtoneManager.getRingtone(this, uri)
        ringtone?.let { rt ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                rt.isLooping = true
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                rt.audioAttributes = android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            }
            try {
                rt.play()

                // === 处理静音 / 勿扰模式 ===
                // 记录当前铃声模式
                prevRingerMode = audioManager.ringerMode
                if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                }

                // 若拥有勿扰策略权限，则记录并放开全部拦截
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M &&
                    notificationManager.isNotificationPolicyAccessGranted
                ) {
                    prevInterruptionFilter = notificationManager.currentInterruptionFilter
                    if (prevInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) {
                        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                    }
                }

                // 记录并将三大相关流（ALARM/MUSIC/RING）提升至最大，确保外放足够响
                val alarmMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                val musicMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val ringMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)

                prevAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
                prevMusicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                prevRingVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)

                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, alarmMax, 0)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, musicMax, 0)
                audioManager.setStreamVolume(AudioManager.STREAM_RING, ringMax, 0)

                // 若启动后依旧未播放，则立即使用 ToneGenerator 兜底
                GlobalScope.launch {
                    delay(500)
                    if (!rt.isPlaying) {
                        fallbackTone()
                    }
                }
            } catch (e: Exception) {
                // play failed
                ringtone = null
            }
        }
    }

    private fun startVibration() {
        vibrator?.let { vib ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 500), 0))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(longArrayOf(0, 500, 500), 0)
            }
        }
    }

    override fun onDestroy() {
        ringtone?.stop()
        toneGen?.stopTone()
        toneGen?.release()
        vibrator?.cancel()
        // 恢复原闹钟音量
        prevAlarmVolume?.let { audioManager.setStreamVolume(AudioManager.STREAM_ALARM, it, 0) }
        prevMusicVolume?.let { audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, it, 0) }
        prevRingVolume?.let { audioManager.setStreamVolume(AudioManager.STREAM_RING, it, 0) }
        // 恢复铃声模式和勿扰策略
        prevRingerMode?.let { audioManager.ringerMode = it }
        prevInterruptionFilter?.let { notificationManager.setInterruptionFilter(it) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(channelId) == null) {
                val ch = NotificationChannel(channelId, "Alarm Playback", NotificationManager.IMPORTANCE_HIGH)
                nm.createNotificationChannel(ch)
            }
        }
    }

    private fun buildForegroundNotification(): Notification {
        val pauseIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_PAUSE
            putExtra("alarm_id", alarmId)
            putExtra("shift", shift)
            putExtra("snooze_remaining", snoozeRemaining)
        }
        val pausePending = PendingIntent.getService(
            this, 2, pauseIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = Intent(this, AlarmService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Full-screen intent so that it can wake the screen / launch UI when the phone is locked
        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPending = PendingIntent.getActivity(
            this,
            1,
            fullScreenIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("闹钟响铃")
            .setContentText("闹钟正在响铃…")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .addAction(android.R.drawable.ic_media_pause, "暂停", pausePending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "停止", stopPending)
            // Make the whole notification clickable as well, opening the full-screen alarm UI
            .setContentIntent(fullScreenPending)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPending, true)
            .setOngoing(true)
            .build()
    }

    private fun fallbackTone() {
        if (toneGen == null) {
            toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            toneGen?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD)
            // repeat after 1s to simulate looping loud tone
            GlobalScope.launch {
                repeat(30) {
                    delay(1000)
                    toneGen?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD)
                }
            }
        }
    }

    private fun handlePause(intent: Intent) {
        val remaining = intent.getIntExtra("snooze_remaining", 0)
        if (remaining <= 0) return

        // Load interval from settings
        val intervalMin = runBlocking { settings.snoozeIntervalFlow.first() }
        val delayMillis = intervalMin * 60_000L

        val alarmManager = getSystemService(ALARM_SERVICE) as android.app.AlarmManager

        val nextIntent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", intent.getIntExtra("alarm_id", 0))
            putExtra("shift", intent.getStringExtra("shift"))
            putExtra("snooze_remaining", remaining - 1)
        }
        val pending = PendingIntent.getBroadcast(
            this,
            intent.getIntExtra("alarm_id", 0),
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAt = System.currentTimeMillis() + delayMillis
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }
} 