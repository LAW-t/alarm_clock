@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.alarm_clock_2.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.commandiron.wheel_picker_compose.WheelTimePicker
import com.commandiron.wheel_picker_compose.core.WheelPickerDefaults
import com.example.alarm_clock_2.shift.IdentityType
import kotlinx.coroutines.launch
import java.time.LocalTime
import android.widget.Toast
import kotlin.math.roundToInt
import androidx.compose.foundation.BorderStroke

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsScreen(viewModel: AlarmsViewModel = hiltViewModel()) {
    val alarmsDb = viewModel.alarms.collectAsState().value
    var showAddDialog by remember { mutableStateOf(false) }

    // 闹钟编辑状态
    var editingAlarm by remember { mutableStateOf<com.example.alarm_clock_2.data.AlarmTimeEntity?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

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

    // 获取当前用户身份可用的班次类型选项
    fun getAvailableShiftOptions(identity: IdentityType): List<Pair<String, String>> {
        return when (identity) {
            IdentityType.LONG_DAY -> listOf("DAY" to "长白班")
            IdentityType.FOUR_THREE -> listOf(
                "MORNING" to "早班",
                "AFTERNOON" to "中班",
                "NIGHT" to "晚班"
            )
            IdentityType.FOUR_TWO -> listOf(
                "MORNING" to "早班",
                "NIGHT" to "晚班"
            )
        }
    }

    // 显示所有闹钟，不区分默认和自定义
    data class AlarmDisplayItem(
        val entity: com.example.alarm_clock_2.data.AlarmTimeEntity,
        val label: String
    )

    // 身份隔离：只显示当前身份对应的闹钟
    val currentIdentityAlarms = alarmsDb.filter { alarm ->
        alarm.identity == uiState.identity.name
    }

    val allAlarms = currentIdentityAlarms.map { alarm ->
        val label = labelMap[alarm.shift] ?: alarm.displayName ?: alarm.shift
        AlarmDisplayItem(alarm, label)
    }



    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "闹钟管理",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "添加闹钟",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { innerPadding ->
        if (allAlarms.isEmpty()) {
            // 空状态
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "暂无闹钟",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "点击右下角的 + 按钮添加闹钟",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(items = allAlarms, key = { it.entity.id }) { item ->
                    SwipeToDeleteAlarmCard(
                        alarm = item.entity,
                        label = item.label,
                        onEdit = {
                            editingAlarm = item.entity
                            showEditDialog = true
                        },
                        onToggle = {
                            viewModel.toggleEnabled(item.entity)
                        },
                        onDelete = {
                            viewModel.deleteAlarm(item.entity)
                        }
                    )
                }
            }
        }
    }

    // 添加闹钟对话框
    if (showAddDialog) {
        AlarmEditBottomSheet(
            alarm = null,
            availableShiftOptions = getAvailableShiftOptions(uiState.identity),
            onDismiss = { showAddDialog = false },
            onConfirm = { time, shift, snoozeCount, snoozeInterval ->
                val displayName = labelMap[shift]
                viewModel.addAlarm(time, shift, displayName, snoozeCount, snoozeInterval, uiState.identity.name)
                showAddDialog = false
            }
        )
    }

    // 编辑闹钟对话框
    if (showEditDialog && editingAlarm != null) {
        AlarmEditBottomSheet(
            alarm = editingAlarm,
            availableShiftOptions = getAvailableShiftOptions(uiState.identity),
            onDismiss = {
                showEditDialog = false
                editingAlarm = null
            },
            onConfirm = { time, shift, snoozeCount, snoozeInterval ->
                val displayName = labelMap[shift]
                viewModel.updateAlarm(editingAlarm!!, time, shift, displayName, snoozeCount, snoozeInterval, uiState.identity.name)
                showEditDialog = false
                editingAlarm = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteAlarmCard(
    alarm: com.example.alarm_clock_2.data.AlarmTimeEntity,
    label: String,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    // 当前时间展示
    val timeDisplay = alarm.time

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color.Red,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    ) {
        ModernAlarmCard(
            alarm = alarm,
            label = label,
            onEdit = onEdit,
            onToggle = onToggle
        )
    }
}

@Composable
private fun ModernAlarmCard(
    alarm: com.example.alarm_clock_2.data.AlarmTimeEntity,
    label: String,
    onEdit: () -> Unit,
    onToggle: () -> Unit
) {
    val timeDisplay = alarm.time
    val context = LocalContext.current
    val isEnabled = alarm.enabled

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = timeDisplay,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isEnabled) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isEnabled) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        }
                    )
                }
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmEditBottomSheet(
    alarm: com.example.alarm_clock_2.data.AlarmTimeEntity?,
    availableShiftOptions: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int, Int) -> Unit
) {
    val isEditing = alarm != null
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // 初始化状态
    var selectedTime by remember {
        mutableStateOf(
            if (isEditing && alarm!!.time.matches(Regex("\\d{2}:\\d{2}"))) {
                val parts = alarm.time.split(":")
                LocalTime.of(parts[0].toInt(), parts[1].toInt())
            } else {
                LocalTime.of(8, 0)
            }
        )
    }

    var selectedShift by remember {
        mutableStateOf(
            if (isEditing) alarm!!.shift else availableShiftOptions.firstOrNull()?.first ?: ""
        )
    }

    var snoozeCount by remember {
        mutableStateOf(if (isEditing) alarm!!.snoozeCount else 3)
    }

    var snoozeInterval by remember {
        mutableStateOf(if (isEditing) alarm!!.snoozeInterval else 5)
    }

    var expanded by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // 班次类型选择 - 移到顶部（按钮下方）
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = availableShiftOptions.find { it.first == selectedShift }?.second ?: "",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("班次类型") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    availableShiftOptions.forEach { (code, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                selectedShift = code
                                expanded = false
                            }
                        )
                    }
                }
            }

            // 重复响铃次数设置
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "重复响铃次数：$snoozeCount 次",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = snoozeCount.toFloat(),
                    onValueChange = { snoozeCount = it.roundToInt() },
                    valueRange = 0f..5f,
                    steps = 4,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            // 重复间隔时间设置
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "重复间隔：$snoozeInterval 分钟",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = snoozeInterval.toFloat(),
                    onValueChange = { snoozeInterval = it.roundToInt() },
                    valueRange = 1f..10f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            // 时间选择器 - 移到下方，增加高度
            val pickerWidth = screenWidth * 0.85f
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "选择时间",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(12.dp))
                WheelTimePicker(
                    modifier = Modifier.width(pickerWidth),
                    size = DpSize(width = pickerWidth, height = 240.dp),
                    startTime = selectedTime,
                    selectorProperties = WheelPickerDefaults.selectorProperties(
                        enabled = true,
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ),
                    onSnappedTime = { time -> selectedTime = time }
                )
            }

            // 操作按钮 - 移到底部
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ElevatedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    elevation = ButtonDefaults.elevatedButtonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("取消", fontWeight = FontWeight.Medium)
                }

                ElevatedButton(
                    onClick = {
                        val timeString = "%02d:%02d".format(selectedTime.hour, selectedTime.minute)
                        onConfirm(timeString, selectedShift, snoozeCount, snoozeInterval)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.elevatedButtonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("确定", fontWeight = FontWeight.Medium)
                }
            }

            // 底部间距
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

}