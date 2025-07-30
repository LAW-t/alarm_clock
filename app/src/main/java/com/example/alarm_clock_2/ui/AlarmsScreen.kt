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
import androidx.compose.material.icons.filled.Alarm
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
                        "闹钟",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            // iOS风格的浮动按钮
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(60.dp),
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp
                )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "添加闹钟",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (allAlarms.isEmpty()) {
            // iOS风格的空状态
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    // 大号图标
                    Icon(
                        Icons.Default.Alarm,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "暂无闹钟",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "点击右下角的 + 按钮\n添加您的第一个闹钟",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 17.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp), // iOS风格的边距
                verticalArrangement = Arrangement.spacedBy(16.dp), // 增加间距
                contentPadding = PaddingValues(vertical = 20.dp) // 增加上下边距
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
            // iOS风格的删除背景
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.error,
                        RoundedCornerShape(20.dp) // 与卡片圆角一致
                    )
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "删除",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color.White
                    )
                }
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

    // iOS风格的卡片设计
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(20.dp), // 更大的圆角
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp), // 增加内边距
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 时间显示 - 更大更醒目
                Text(
                    text = timeDisplay,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp // iOS风格的大字体
                    ),
                    color = if (isEnabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))

                // 班次标签
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = if (isEnabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        }
                    )
                }

                // 如果有重复设置，显示额外信息
                if (alarm.snoozeCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "重复 ${alarm.snoozeCount} 次，间隔 ${alarm.snoozeInterval} 分钟",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // iOS风格的开关
            Switch(
                checked = isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
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
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            // iOS风格的拖拽指示器
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(5.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(2.5.dp)
                    )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // 标题
            Text(
                text = if (isEditing) "编辑闹钟" else "添加闹钟",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            // iOS风格的班次类型选择
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.padding(4.dp)
                ) {
                    OutlinedTextField(
                        value = availableShiftOptions.find { it.first == selectedShift }?.second ?: "",
                        onValueChange = { },
                        readOnly = true,
                        label = {
                            Text(
                                "班次类型",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        availableShiftOptions.forEach { (code, name) ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        name,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                },
                                onClick = {
                                    selectedShift = code
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            // iOS风格的重复设置卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    // 重复响铃次数设置
                    Text(
                        text = "重复响铃次数",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Slider(
                            value = snoozeCount.toFloat(),
                            onValueChange = { snoozeCount = it.roundToInt() },
                            valueRange = 0f..5f,
                            steps = 4,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "$snoozeCount 次",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(50.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 重复间隔时间设置
                    Text(
                        text = "重复间隔时间",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Slider(
                            value = snoozeInterval.toFloat(),
                            onValueChange = { snoozeInterval = it.roundToInt() },
                            valueRange = 1f..10f,
                            steps = 8,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "$snoozeInterval 分钟",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(60.dp)
                        )
                    }
                }
            }

            // iOS风格的时间选择器
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "选择时间",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val pickerWidth = screenWidth * 0.8f
                    WheelTimePicker(
                        modifier = Modifier.width(pickerWidth),
                        size = DpSize(width = pickerWidth, height = 260.dp),
                        startTime = selectedTime,
                        selectorProperties = WheelPickerDefaults.selectorProperties(
                            enabled = true,
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        ),
                        onSnappedTime = { time -> selectedTime = time }
                    )
                }
            }

            // iOS风格的操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 取消按钮
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp
                    )
                ) {
                    Text(
                        "取消",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp
                        )
                    )
                }

                // 确定按钮
                Button(
                    onClick = {
                        val timeString = "%02d:%02d".format(selectedTime.hour, selectedTime.minute)
                        onConfirm(timeString, selectedShift, snoozeCount, snoozeInterval)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 4.dp
                    )
                ) {
                    Text(
                        "确定",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp
                        )
                    )
                }
            }

            // 底部间距
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

}