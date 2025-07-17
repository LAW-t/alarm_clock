package com.example.alarm_clock_2.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.EntryPointAccessors
import com.example.alarm_clock_2.data.HolidayRepository
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class HolidaySyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    /**
     * Use Hilt [EntryPoint] to obtain [HolidayRepository] since the Worker may be instantiated
     * by WorkManager via reflection (i.e. without going through Hilt's generated factory).
     */
    private val repo: HolidayRepository by lazy {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            HolidayRepositoryEntryPoint::class.java
        )
        entryPoint.holidayRepository()
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface HolidayRepositoryEntryPoint {
        fun holidayRepository(): HolidayRepository
    }

    override suspend fun doWork(): Result {
        val year = LocalDate.now().year
        try {
            repo.syncYear(year)
            repo.syncYear(year + 1)
            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "holiday-sync"

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<HolidaySyncWorker>(24, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
} 