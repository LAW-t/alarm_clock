package com.example.alarm_clock_2.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import com.example.alarm_clock_2.data.SettingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import com.example.alarm_clock_2.shift.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import com.example.alarm_clock_2.data.AlarmTimeEntity
import android.os.Build
import android.util.Log
import com.example.alarm_clock_2.MainActivity

/**
 * Wraps [AlarmManager] to schedule/cancel alarms associated with [AlarmTimeEntity].
 */
@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsDataStore
) {

    companion object {
        private const val TAG = "AlarmScheduler"
    }

    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(entity: AlarmTimeEntity) {
        if (!entity.enabled) {
            Log.d(TAG, "Skip schedule: alarm ${entity.id} disabled")
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w(TAG, "Exact alarm permission denied; cannot schedule id=${entity.id}")
            return
        }
        val localTime = parseTime(entity.time) ?: return
        val triggerAt = computeNextTriggerMillisForShift(localTime, entity.shift)
        val humanTime = java.time.Instant.ofEpochMilli(triggerAt)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDateTime()
        Log.d(TAG, "Scheduling alarm id=${entity.id} shift=${entity.shift} at ${humanTime} (millis=${triggerAt})")

        val showIntent = buildActivityPendingIntent()
        val operation = buildBroadcastPendingIntent(entity)
        try {
            val info = android.app.AlarmManager.AlarmClockInfo(triggerAt, showIntent)
            alarmManager.setAlarmClock(info, operation)
            Log.d(TAG, "Alarm set via AlarmManager for id=${entity.id}")
        } catch (e: SecurityException) {
            // Permission not granted; skip scheduling
            Log.e(TAG, "SecurityException scheduling alarm id=${entity.id}: $e")
        }
    }

    fun cancel(entity: AlarmTimeEntity) {
        val pi = buildBroadcastPendingIntent(entity)
        alarmManager.cancel(pi)
        Log.d(TAG, "Cancelled alarm id=${entity.id}")
    }

    /**
     * Returns epoch millis of the next time this alarm will fire, or null if unable to compute
     * (e.g. invalid time format or alarm disabled).
     */
    fun nextTriggerMillis(entity: AlarmTimeEntity): Long? {
        if (!entity.enabled) return null
        val localTime = parseTime(entity.time) ?: return null
        return computeNextTriggerMillisForShift(localTime, entity.shift)
    }

    /** The PendingIntent that fires when alarm is triggered. */
    private fun buildBroadcastPendingIntent(entity: AlarmTimeEntity): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", entity.id)
            putExtra("shift", entity.shift)
        }
        val requestCode = if (entity.id != 0) entity.id else (entity.time + entity.shift).hashCode()
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** The PendingIntent to open UI when user clicks alarm icon in status bar. */
    private fun buildActivityPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context,
            -1, // fixed request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun parseTime(time: String): LocalTime? = runCatching {
        LocalTime.parse(time)
    }.getOrNull()

    private fun computeNextTriggerMillisForShift(time: LocalTime, shiftStr: String): Long {
        val desiredShift = runCatching { Shift.valueOf(shiftStr) }.getOrElse { return computeTomorrow(time) }

        // Fetch current settings synchronously
        val identityStr: String
        val idx43: Int
        val base43: String
        val idx42: Int
        val base42: String
        runBlocking {
            identityStr = settings.identityFlow.first()
            idx43 = settings.fourThreeIndexFlow.first()
            base43 = settings.fourThreeBaseDateFlow.first()
            idx42 = settings.fourTwoIndexFlow.first()
            base42 = settings.fourTwoBaseDateFlow.first()
        }
        val identity = runCatching { IdentityType.valueOf(identityStr) }.getOrDefault(IdentityType.LONG_DAY)
        val (baseIndex, baseDateStr) = when (identity) {
            IdentityType.FOUR_THREE -> idx43 to base43
            IdentityType.FOUR_TWO -> idx42 to base42
            else -> 0 to LocalDate.now().toString()
        }
        val baseDate = runCatching { LocalDate.parse(baseDateStr) }.getOrElse { LocalDate.now() }
        val config = ShiftConfig(identity, baseDate, baseIndex)

        // search next 60 days for matching shift
        for (offset in 0..60) {
            val date = LocalDate.now().plusDays(offset.toLong())
            if (ShiftCalculator.calculate(date, config) == desiredShift) {
                var triggerDate = date
                if (desiredShift == Shift.NIGHT) {
                    triggerDate = date.minusDays(1)
                }
                val dt = LocalDateTime.of(triggerDate, time)
                if (dt.isAfter(LocalDateTime.now())) {
                    return dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                }
            }
        }
        // fallback tomorrow
        return computeTomorrow(time)
    }

    private fun computeTomorrow(time: LocalTime): Long {
        val tomorrow = LocalDate.now().plusDays(1)
        val dt = LocalDateTime.of(tomorrow, time)
        return dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}