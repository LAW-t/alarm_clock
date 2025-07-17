package com.example.alarm_clock_2

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.alarm_clock_2.worker.HolidaySyncWorker
import javax.inject.Inject

@HiltAndroidApp
class MyApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        HolidaySyncWorker.enqueue(this)
        // 每日更新班次闹钟
        com.example.alarm_clock_2.worker.ShiftAlarmWorker.enqueue(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
} 