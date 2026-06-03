package com.example.alarm_clock_2.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.alarm_clock_2.MainActivity
import com.example.alarm_clock_2.R
import com.example.alarm_clock_2.alarm.AlarmScheduler
import com.example.alarm_clock_2.data.AlarmRepository
import com.example.alarm_clock_2.data.SettingsDataStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class NextAlarmWidget : AppWidgetProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntry {
        fun repository(): AlarmRepository
        fun scheduler(): AlarmScheduler
        fun settings(): SettingsDataStore
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
        val entry = EntryPointAccessors.fromApplication(context, WidgetEntry::class.java)

        var title = "暂无闹钟"
        var subtitle = "点击打开应用"
        var timeText = "--:--"

        try {
            runBlocking {
                val alarms = entry.repository().getAlarms().first()
                val enabled = alarms.filter { it.enabled }
                if (enabled.isNotEmpty()) {
                    // 找最近的下一次触发
                    val now = System.currentTimeMillis()
                    val upcoming = enabled.mapNotNull { alarm ->
                        val trigger = entry.scheduler().nextTriggerMillis(alarm)
                        if (trigger != null && trigger > now) alarm to trigger else null
                    }.minByOrNull { it.second }

                    if (upcoming != null) {
                        val (alarm, trigger) = upcoming
                        val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(trigger), ZoneId.systemDefault())
                        timeText = dt.format(DateTimeFormatter.ofPattern("HH:mm"))
                        val shiftLabel = when (alarm.shift) {
                            "MORNING" -> "早班"
                            "AFTERNOON" -> "中班"
                            "NIGHT" -> "晚班"
                            "DAY" -> "白班"
                            else -> alarm.shift
                        }
                        title = "下次闹钟"
                        subtitle = "${dt.monthValue}月${dt.dayOfMonth}日 · $shiftLabel"
                    } else {
                        title = "已全部关闭"
                        subtitle = "暂无启用的闹钟"
                    }
                }
            }
        } catch (_: Exception) {
            // 读取失败时使用默认文案
        }

        val views = RemoteViews(context.packageName, R.layout.widget_next_alarm).apply {
            setTextViewText(R.id.widget_title, title)
            setTextViewText(R.id.widget_subtitle, subtitle)
            setTextViewText(R.id.widget_time, timeText)
        }

        // 点击打开应用
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pending = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pending)

        appWidgetManager.updateAppWidget(widgetId, views)
    }
}
