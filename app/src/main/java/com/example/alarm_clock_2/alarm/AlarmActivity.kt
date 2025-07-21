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

class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show even if device is locked
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        // AlarmService 已由 AlarmReceiver 启动，这里无需再次启动，避免重复播放

        setContent {
            MaterialTheme {
                val shift = intent?.getStringExtra("shift")
                AlarmScreen(shift, onDismiss = {
                    stopService(Intent(this, AlarmService::class.java))
                    finish()
                })
            }
        }
    }
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
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = prompt, style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(onClick = {
                    // Pause current ring
                    context.startForegroundService(Intent(context, AlarmService::class.java).apply {
                        action = AlarmService.ACTION_PAUSE
                        putExtra("alarm_id", alarmId)
                        putExtra("shift", shiftArg)
                        putExtra("snooze_remaining", snoozeRemaining)
                    })
                    onDismiss()
                }) {
                    Text("暂停")
                }
                Button(onClick = {
                    context.startForegroundService(Intent(context, AlarmService::class.java).apply {
                        action = AlarmService.ACTION_STOP
                    })
                    onDismiss()
                }) {
                    Text("停止")
                }
            }
        }
    }
} 