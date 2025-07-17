package com.example.alarm_clock_2.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.example.alarm_clock_2.data.AlarmRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: AlarmRepository

    @Inject
    lateinit var scheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule all enabled alarms
            runBlocking {
                val list = repository.getAlarms().first()
                list.filter { it.enabled }.forEach { scheduler.schedule(it) }
            }
        }
    }
} 