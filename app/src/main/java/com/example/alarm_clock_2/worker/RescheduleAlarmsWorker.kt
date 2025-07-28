package com.example.alarm_clock_2.worker

import android.app.AlarmManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import com.example.alarm_clock_2.data.AlarmRepository
import com.example.alarm_clock_2.alarm.AlarmScheduler
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class RescheduleAlarmsWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Entry {
        fun alarmRepository(): AlarmRepository
        fun alarmScheduler(): AlarmScheduler
    }

    private val entry: Entry by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            Entry::class.java
        )
    }

    private val repo: AlarmRepository by lazy { entry.alarmRepository() }
    private val scheduler: AlarmScheduler by lazy { entry.alarmScheduler() }

    override suspend fun doWork(): Result {
        return try {
            // Check if we have the exact alarm permission
            val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                // If we don't have the permission, we can't schedule alarms
                // This is not an error, but we should log it
                return Result.success()
            }
            
            // Reschedule all enabled alarms
            val alarms = repo.getAlarms().first()
            alarms.filter { it.enabled }.forEach { scheduler.schedule(it) }
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "reschedule_alarms"

        fun enqueue(context: Context) {
            // Schedule the work to run immediately
            val request = OneTimeWorkRequestBuilder<RescheduleAlarmsWorker>()
                .setInitialDelay(5, TimeUnit.SECONDS) // Give some time for the system to fully boot
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}