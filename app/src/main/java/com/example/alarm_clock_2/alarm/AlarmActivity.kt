package com.example.alarm_clock_2.alarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.view.WindowManager
import androidx.compose.ui.platform.LocalContext
import com.example.alarm_clock_2.ui.theme.AppTheme
import com.example.alarm_clock_2.alarm.AlarmService

class AlarmActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: android.content.Context) {
        val configuration = newBase.resources.configuration
        configuration.fontScale = 1f
        val context = newBase.createConfigurationContext(configuration)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 在锁屏和息屏状态下自动亮屏并置顶显示
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // AlarmService 已由 AlarmReceiver 启动，这里无需再次启动，避免重复播放

        setContent {
            AppTheme {
                val shift = intent?.getStringExtra("shift")
                AlarmScreen(shift, onDismiss = {
                    stopService(Intent(this, AlarmService::class.java))
                    finish()
                })
            }
        }
    }
}

/**
 * 贪睡操作：同 AlarmService.handlePause 逻辑但传入自定义延迟分钟数
 */
private fun snoozeAlarm(context: android.content.Context, alarmId: Int, shift: String?, snoozeRemaining: Int, delayMinutes: Int) {
    val intent = Intent(context, AlarmService::class.java).apply {
        action = AlarmService.ACTION_PAUSE
        putExtra("alarm_id", alarmId)
        putExtra("shift", shift)
        putExtra("snooze_remaining", snoozeRemaining)
        putExtra("snooze_delay_minutes", delayMinutes)
    }
    context.startForegroundService(intent)
}

@Composable
private fun AlarmScreen(shift: String?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val alarmId = remember { (context as? ComponentActivity)?.intent?.getIntExtra("alarm_id", 0) ?: 0 }
    val snoozeRemaining = remember { (context as? ComponentActivity)?.intent?.getIntExtra("snooze_remaining", 0) ?: 0 }
    val shiftArg = shift

    val prompt = when (shift) {
        "MORNING", "DAY" -> "上早班"
        "AFTERNOON" -> "上中班"
        "NIGHT" -> "上晚班"
        else -> "闹钟响铃！"
    }
    // 贪睡时长选项（分钟）
    val snoozeOptions = listOf(5, 10, 15, 20)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = prompt, style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "选择稍后提醒时长",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                snoozeOptions.forEach { mins ->
                    OutlinedButton(onClick = {
                        snoozeAlarm(context, alarmId, shiftArg, snoozeRemaining, mins)
                        onDismiss()
                    }) {
                        Text("${mins}分")
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                context.startForegroundService(Intent(context, AlarmService::class.java).apply {
                    action = AlarmService.ACTION_STOP
                })
                onDismiss()
            }) {
                Text("关闭闹钟")
            }
        }
    }
} 