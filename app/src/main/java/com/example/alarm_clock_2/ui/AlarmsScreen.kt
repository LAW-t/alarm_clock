package com.example.alarm_clock_2.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alarm_clock_2.shift.IdentityType
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import android.widget.NumberPicker
import androidx.core.content.ContextCompat
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.background

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsScreen(viewModel: AlarmsViewModel = hiltViewModel()) {
    val alarmsDb = viewModel.alarms.collectAsState().value

    // 读取设置，用于确定应显示的班次数量
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val uiState = settingsViewModel.uiState.collectAsState().value

    // shift code -> 中文标签 映射
    val labelMap = mapOf(
        "DAY" to "长白班",
        "MORNING" to "早班",
        "AFTERNOON" to "中班",
        "NIGHT" to "晚班"
    )

    // 根据身份生成需要展示的班次代码
    val requiredShiftCodes: List<String> = when (uiState.identity) {
        IdentityType.LONG_DAY -> listOf("DAY")
        IdentityType.FOUR_THREE -> listOf("MORNING", "AFTERNOON", "NIGHT")
        IdentityType.FOUR_TWO -> listOf("MORNING", "NIGHT")
    }

    // 合并数据库实体与需求
    data class TempAlarm(val entity: com.example.alarm_clock_2.data.AlarmTimeEntity?, val code: String)
    val merged = requiredShiftCodes.map { code ->
        val found = alarmsDb.firstOrNull { it.shift == code }
        TempAlarm(found, code)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("闹钟列表") }) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items = merged, key = { it.code }) { temp ->
                AlarmRow(
                    alarm = temp.entity,
                    label = labelMap[temp.code] ?: temp.code,
                    onTimeChange = { newTime ->
                        val ent = temp.entity
                        if (ent == null) {
                            viewModel.addAlarm(newTime, temp.code)
                        } else {
                            viewModel.updateTime(ent, newTime)
                        }
                    },
                    onToggle = {
                        temp.entity?.let { viewModel.toggleEnabled(it) }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmRow(
    alarm: com.example.alarm_clock_2.data.AlarmTimeEntity?,
    label: String,
    onTimeChange: (String) -> Unit,
    onToggle: () -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }

    // 当前时间展示
    val timeDisplay = alarm?.time ?: "--:--"

    if (showPicker) {
        val initialHour: Int
        val initialMinute: Int
        if (alarm != null && alarm.time.matches(Regex("\\d{2}:\\d{2}"))) {
            val parts = alarm.time.split(":")
            initialHour = parts[0].toInt()
            initialMinute = parts[1].toInt()
        } else {
            initialHour = 8
            initialMinute = 0
        }

        TimeWheelBottomSheet(
            initialHour = initialHour,
            initialMinute = initialMinute,
            onConfirm = { newTime ->
                onTimeChange(newTime)
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }

    val clickModifier = remember {
        Modifier.clickable { showPicker = true }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickModifier)
            .padding(horizontal = 16.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = timeDisplay,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Switch(
                checked = alarm?.enabled ?: false,
                onCheckedChange = { onToggle() }
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeWheelBottomSheet(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var hour by remember { mutableIntStateOf(initialHour) }
    var minute by remember { mutableIntStateOf(initialMinute) }

    ModalBottomSheet(
        onDismissRequest = {
            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
        },
        sheetState = sheetState
    ) {
        Text(
            text = "选择时间",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)  // Reduced height to roughly one-third of the screen
        ) {
            // Highlight overlay at center
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.8f)
                    .height(36.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp)
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalArrangement = Arrangement.Center
            ) {
                AndroidView(
                    modifier = Modifier.height(140.dp),  // Adjusted height for NumberPicker
                    factory = { context ->
                    NumberPicker(context).apply {
                        minValue = 0
                        maxValue = 23
                        value = hour
                        wrapSelectorWheel = true
                        setFormatter { String.format("%02d", it) }
                        setOnValueChangedListener { _, _, newVal -> hour = newVal }

                        // Style adjustments
                        setTextColor(ContextCompat.getColor(context, android.R.color.black))
                        textSize = 40f // Increased from 30f to make numbers even larger
                        
                        // Remove divider lines completely
                        try {
                            val divField = NumberPicker::class.java.getDeclaredField("mSelectionDivider")
                            divField.isAccessible = true
                            divField.set(this, ColorDrawable(android.graphics.Color.TRANSPARENT))
                            
                            // Increase the number of displayed values to enhance wheel feeling
                            val selectorWheelPaintField = NumberPicker::class.java.getDeclaredField("mSelectorWheelPaint")
                            selectorWheelPaintField.isAccessible = true
                            val paint = selectorWheelPaintField.get(this) as android.graphics.Paint
                            paint.textSize = resources.displayMetrics.density * 36 // Match with textSize
                            
                            // Adjust the displayed values count
                            val countField = NumberPicker::class.java.getDeclaredField("mShownCount")
                            if (countField != null) {
                                countField.isAccessible = true
                                countField.set(this, 5) // Show 5 items (more wheel-like)
                            }
                        } catch (_: Throwable) {}
                    }
                })

                Spacer(modifier = Modifier.width(12.dp))

                AndroidView(
                    modifier = Modifier.height(140.dp),  // Adjusted height for NumberPicker
                    factory = { context ->
                    NumberPicker(context).apply {
                        minValue = 0
                        maxValue = 59
                        value = minute
                        wrapSelectorWheel = true
                        setFormatter { String.format("%02d", it) }
                        setOnValueChangedListener { _, _, newVal -> minute = newVal }

                        setTextColor(ContextCompat.getColor(context, android.R.color.black))
                        textSize = 36f // Increased from 30f to make numbers even larger

                        try {
                            val divField = NumberPicker::class.java.getDeclaredField("mSelectionDivider")
                            divField.isAccessible = true
                            divField.set(this, ColorDrawable(android.graphics.Color.TRANSPARENT))
                            
                            // Increase the number of displayed values to enhance wheel feeling
                            val selectorWheelPaintField = NumberPicker::class.java.getDeclaredField("mSelectorWheelPaint")
                            selectorWheelPaintField.isAccessible = true
                            val paint = selectorWheelPaintField.get(this) as android.graphics.Paint
                            paint.textSize = resources.displayMetrics.density * 36 // Match with textSize
                            
                            // Adjust the displayed values count
                            val countField = NumberPicker::class.java.getDeclaredField("mShownCount")
                            if (countField != null) {
                                countField.isAccessible = true
                                countField.set(this, 5) // Show 5 items (more wheel-like)
                            }
                        } catch (_: Throwable) {}
                    }
                })
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = {
                scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
            }) { Text("取消") }

            Spacer(Modifier.width(16.dp))

            TextButton(onClick = {
                val newTime = "%02d:%02d".format(hour, minute)
                scope.launch { sheetState.hide() }.invokeOnCompletion { onConfirm(newTime) }
            }) { Text("确定") }
        }

        Spacer(Modifier.height(16.dp))
    }
} 