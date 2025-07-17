package com.example.alarm_clock_2.alarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.view.WindowManager

class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show even if device is locked
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        // Start foreground service to play alarm sound (required from API 26+)
        val serviceIntent = Intent(this, AlarmService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

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
            Button(onClick = onDismiss) {
                Text("关闭")
            }
        }
    }
} 