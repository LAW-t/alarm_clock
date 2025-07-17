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

    override fun onReceive(context: Context, intent: Intent) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AlarmClock:WakeLock")
        wakeLock.acquire(10 * 60 * 1000L /* 10 minutes */)
        Log.d("AlarmReceiver", "WakeLock acquired")

        val activityIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("shift", intent.getStringExtra("shift"))
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