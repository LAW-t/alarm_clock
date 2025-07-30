package com.example.alarm_clock_2.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import com.example.alarm_clock_2.update.UpdateRepository
import com.example.alarm_clock_2.update.UpdateUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class UpdateCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val updateRepository: UpdateRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val WORK_NAME = "update_check_work"
        private const val CHECK_INTERVAL_HOURS = 24L // Check once per day

        /**
         * Enqueue periodic update checking
         */
        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
                CHECK_INTERVAL_HOURS, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        /**
         * Cancel update checking
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        return try {
            // Check if auto-check is enabled
            val preferences = updateRepository.getUpdatePreferences().first()
            if (!preferences.autoCheckEnabled) {
                return Result.success()
            }

            // Get current app version
            val currentVersion = UpdateUtils.getCurrentVersion(applicationContext)

            // Check for updates
            val result = updateRepository.checkForUpdates(currentVersion)
            
            result.fold(
                onSuccess = { updateInfo ->
                    if (updateInfo != null) {
                        // Update found - the repository will handle showing the notification
                        // through the ViewModel when the app is next opened
                        Result.success()
                    } else {
                        // No update available
                        Result.success()
                    }
                },
                onFailure = { exception ->
                    // Log error but don't fail the work - we'll try again next time
                    Result.success()
                }
            )
        } catch (e: Exception) {
            // Don't retry on failure to avoid excessive network usage
            Result.success()
        }
    }
}
