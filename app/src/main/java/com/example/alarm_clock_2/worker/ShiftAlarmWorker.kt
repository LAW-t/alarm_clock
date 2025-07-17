package com.example.alarm_clock_2.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import com.example.alarm_clock_2.data.AlarmRepository
import com.example.alarm_clock_2.data.AlarmTimeEntity
import com.example.alarm_clock_2.data.SettingsDataStore
import com.example.alarm_clock_2.alarm.AlarmScheduler
import com.example.alarm_clock_2.shift.*
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class ShiftAlarmWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Entry {
        fun alarmRepository(): AlarmRepository
        fun settingsDataStore(): SettingsDataStore
        fun alarmScheduler(): AlarmScheduler
    }

    private val entry: Entry by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            Entry::class.java
        )
    }

    private val repo: AlarmRepository by lazy { entry.alarmRepository() }
    private val settings: SettingsDataStore by lazy { entry.settingsDataStore() }
    private val scheduler: AlarmScheduler by lazy { entry.alarmScheduler() }

    override suspend fun doWork(): Result {
        return try {
            syncTodayAlarms()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun syncTodayAlarms() {
        // 1. 计算今天应上的班次
        val identityStr = settings.identityFlow.first()
        val idx43 = settings.fourThreeIndexFlow.first()
        val base43 = settings.fourThreeBaseDateFlow.first()
        val idx42 = settings.fourTwoIndexFlow.first()
        val base42 = settings.fourTwoBaseDateFlow.first()

        val identity = runCatching { IdentityType.valueOf(identityStr) }.getOrDefault(IdentityType.LONG_DAY)
        val (baseIndex, baseDateStr) = when (identity) {
            IdentityType.FOUR_THREE -> idx43 to base43
            IdentityType.FOUR_TWO -> idx42 to base42
            else -> 0 to LocalDate.now().toString()
        }
        val baseDate = runCatching { LocalDate.parse(baseDateStr) }.getOrElse { LocalDate.now() }
        val config = ShiftConfig(identity, baseDate, baseIndex)

        val todayShift = ShiftCalculator.calculate(LocalDate.now(), config)

        // 2. 更新闹钟表
        val alarms = repo.getAlarms().first()
        alarms.forEach { alarm ->
            if (alarm.shift == todayShift.name) {
                ensureEnabledAndScheduled(alarm)
            } else {
                ensureDisabledAndCancelled(alarm)
            }
        }
    }

    private suspend fun ensureEnabledAndScheduled(alarm: AlarmTimeEntity) {
        val entity = if (!alarm.enabled) {
            val updated = alarm.copy(enabled = true)
            repo.upsert(updated)
            updated
        } else alarm
        scheduler.schedule(entity)
    }

    private suspend fun ensureDisabledAndCancelled(alarm: AlarmTimeEntity) {
        scheduler.cancel(alarm)
        if (alarm.enabled) {
            repo.upsert(alarm.copy(enabled = false))
        }
    }

    companion object {
        private const val WORK_NAME = "shift_alarm_daily"

        fun enqueue(context: Context) {
            // 24 小时一次即可，WorkManager 会在系统认为合适的时间执行，可能会略有延迟
            val request = PeriodicWorkRequestBuilder<ShiftAlarmWorker>(24, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
} 