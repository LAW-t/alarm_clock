package com.example.alarm_clock_2.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alarm_clock_2.shift.IdentityType
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpSize
import com.commandiron.wheel_picker_compose.WheelTimePicker
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsScreen(viewModel: AlarmsViewModel = hiltViewModel()) {
    val alarmsDb = viewModel.alarms.collectAsState().value

    // Toast notifications for next alarm time
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.toastFlow.collect { msg ->
            Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
        }
    }

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
    val context = LocalContext.current

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
            initialTime = LocalTime.of(initialHour, initialMinute),
            onConfirm = { newTime ->
                onTimeChange(newTime)
                showPicker = false
            }
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
                onCheckedChange = {
                    if (alarm == null) {
                        Toast.makeText(context, "请先选择闹钟时间", Toast.LENGTH_SHORT).show()
                    } else {
                        onToggle()
                    }
                }
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeWheelBottomSheet(
    initialTime: LocalTime,
    onConfirm: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var selectedTime by remember { mutableStateOf(initialTime) }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    ModalBottomSheet(
        onDismissRequest = {
            val newTime = "%02d:%02d".format(selectedTime.hour, selectedTime.minute)
            scope.launch { sheetState.hide() }.invokeOnCompletion { onConfirm(newTime) }
        },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
            .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "选择时间",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            val pickerWidth = screenWidth * 0.7f
            WheelTimePicker(
                modifier = Modifier.width(pickerWidth),
                size = DpSize(width = pickerWidth, height = 128.dp),
                startTime = initialTime,
                onSnappedTime = { time -> selectedTime = time }
            )
        }
    }
} 