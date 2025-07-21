package com.example.alarm_clock_2.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.alarm_clock_2.data.AlarmRepository
import com.example.alarm_clock_2.alarm.AlarmScheduler
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import com.example.alarm_clock_2.data.SettingsDataStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint
import android.os.PowerManager
import android.util.Log

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    // Inject repository & scheduler so we can reschedule the following alarm occurrence
    @Inject
    lateinit var repository: AlarmRepository

    @Inject
    lateinit var scheduler: AlarmScheduler

    // Access DataStore via entry point (cannot inject into receiver constructor)
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Entry {
        fun settings(): SettingsDataStore
    }

    override fun onReceive(context: Context, intent: Intent) {
        val settings: SettingsDataStore = EntryPointAccessors.fromApplication(context, Entry::class.java).settings()
        val defaultCount = runBlocking { settings.snoozeCountFlow.first() }

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AlarmClock:WakeLock")
        wakeLock.acquire(10 * 60 * 1000L /* 10 minutes */)
        Log.d("AlarmReceiver", "WakeLock acquired")

        // Start foreground service to play alarm sound in case UI cannot be shown
        val alarmId = intent.getIntExtra("alarm_id", 0)
        val shift = intent.getStringExtra("shift")
        val remaining = intent.getIntExtra("snooze_remaining", defaultCount)

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("alarm_id", alarmId)
            putExtra("shift", shift)
            putExtra("snooze_remaining", remaining)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        val activityIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("shift", shift)
            putExtra("alarm_id", alarmId)
            putExtra("snooze_remaining", remaining)
        }
        context.startActivity(activityIntent)

        // After the current alarm is triggered, immediately schedule the next upcoming
        // occurrence of all enabled alarms. This guarantees that alarms keep working
        // even if the user never re-opens the app again.
        runBlocking {
            val list = repository.getAlarms().first()
            list.filter { it.enabled }.forEach { scheduler.schedule(it) }
        }

        wakeLock.release()
        Log.d("AlarmReceiver", "WakeLock released")
    }
} 