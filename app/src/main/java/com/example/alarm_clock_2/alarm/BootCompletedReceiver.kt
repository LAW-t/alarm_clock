package com.example.alarm_clock_2.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.alarm_clock_2.worker.RescheduleAlarmsWorker

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Enqueue the RescheduleAlarmsWorker to reschedule alarms after boot
            RescheduleAlarmsWorker.enqueue(context)
        }
    }
}