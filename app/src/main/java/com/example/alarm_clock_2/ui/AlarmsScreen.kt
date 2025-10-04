@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.alarm_clock_2.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
 
import com.example.alarm_clock_2.shift.IdentityType
import kotlinx.coroutines.delay
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

    val currentTime by produceState(initialValue = LocalTime.now()) {
        while (true) {
            value = LocalTime.now()
            delay(10_000)
        }
    }

    val nowMillis by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(10_000)
        }
    }

    val refreshTick = nowMillis / 600_000L
    LaunchedEffect(refreshTick) {
        viewModel.refreshNextTriggerTimes()
    }

    val nextTriggerMap = viewModel.nextTriggerMap.collectAsState().value

    val upcomingAlarmId = remember(nextTriggerMap, nowMillis) {
        nextTriggerMap
            .filterValues { it != null && it > nowMillis }
            .minByOrNull { it.value!! }
            ?.key
    }

    val colorScheme = MaterialTheme.colorScheme
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            colorScheme.primary.copy(alpha = 0.12f),
            colorScheme.background
        )
    )

    val greetingText = remember(currentTime) {
        when (currentTime.hour) {
            in 5..10 -> "早安，开启充满活力的一天"
            in 11..13 -> "午间小憩也别忘了定闹钟"
            in 14..18 -> "午后时光，保持专注"
            else -> "夜深了，别忘了好好休息"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "闹钟",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                greetingText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            floatingActionButton = {
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
            containerColor = Color.Transparent
        ) { innerPadding ->
            if (allAlarms.isEmpty()) {
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
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    contentPadding = PaddingValues(vertical = 24.dp)
                ) {
                    items(items = allAlarms, key = { it.entity.id }) { item ->
                        SwipeToDeleteAlarmCard(
                            alarm = item.entity,
                            label = item.label,
                            isUpcoming = item.entity.id == upcomingAlarmId,
                            nextTriggerMillis = nextTriggerMap[item.entity.id],
                            currentTimeMillis = nowMillis,
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

                    item("alarms_bottom_spacer") {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
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
                viewModel.addAlarm(time, shift, displayName, snoozeCount, snoozeInterval)
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
                viewModel.updateAlarm(editingAlarm!!, time, shift, displayName, snoozeCount, snoozeInterval)
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
    isUpcoming: Boolean,
    nextTriggerMillis: Long?,
    currentTimeMillis: Long,
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
            isUpcoming = isUpcoming,
            nextTriggerMillis = nextTriggerMillis,
            currentTimeMillis = currentTimeMillis,
            onEdit = onEdit,
            onToggle = onToggle
        )
    }
}

@Composable
private fun ModernAlarmCard(
    alarm: com.example.alarm_clock_2.data.AlarmTimeEntity,
    label: String,
    isUpcoming: Boolean,
    nextTriggerMillis: Long?,
    currentTimeMillis: Long,
    onEdit: () -> Unit,
    onToggle: () -> Unit
) {
    val timeDisplay = alarm.time
    val isEnabled = alarm.enabled
    val remainingMillis = if (isEnabled) {
        nextTriggerMillis?.minus(currentTimeMillis)?.takeIf { it > 0 }
    } else {
        null
    }
    val remainingDescription = remainingMillis?.let { formatDuration(it) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(20.dp), // 更大的圆角
        colors = CardDefaults.cardColors(
            containerColor = if (isUpcoming) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isUpcoming) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        }
                    )
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                AnimatedVisibility(visible = isUpcoming) {
                    Text(
                        text = "下一个闹钟",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (isUpcoming) {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = timeDisplay,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp
                    ),
                    color = if (isEnabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isEnabled) {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.Alarm,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isEnabled) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            }
                        )
                    }
                }

                AnimatedVisibility(visible = alarm.snoozeCount > 0) {
                    Column {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "重复 ${alarm.snoozeCount} 次，间隔 ${alarm.snoozeInterval} 分钟",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 13.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                AnimatedVisibility(visible = remainingDescription != null) {
                    Column {
                        Spacer(modifier = Modifier.height(6.dp))
                        remainingDescription?.let { description ->
                            Text(
                                text = "距离下一次响铃 ${description}后",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = !isEnabled) {
                    Column {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "闹钟已暂停",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

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


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AlarmEditBottomSheet(
    alarm: com.example.alarm_clock_2.data.AlarmTimeEntity?,
    availableShiftOptions: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int, Int) -> Unit
) {
    val isEditing = alarm != null
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val defaultTime = remember { LocalTime.now().withSecond(0).withNano(0) }
    val initialTime = remember(alarm) {
        if (isEditing && alarm!!.time.matches(Regex("\\d{2}:\\d{2}"))) {
            val parts = alarm.time.split(":")
            LocalTime.of(parts[0].toInt(), parts[1].toInt())
        } else {
            defaultTime
        }
    }
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true
    )

    var selectedShift by remember(alarm, availableShiftOptions) {
        mutableStateOf(
            if (isEditing) {
                alarm!!.shift
            } else {
                availableShiftOptions.firstOrNull()?.first.orEmpty()
            }
        )
    }

    var snoozeCount by remember(alarm) {
        mutableStateOf(if (isEditing) alarm!!.snoozeCount else 3)
    }

    var snoozeInterval by remember(alarm) {
        mutableStateOf(if (isEditing) alarm!!.snoozeInterval else 5)
    }

    
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .size(width = 40.dp, height = 5.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isEditing) "编辑闹钟" else "添加闹钟",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // 时间选择放在最上面
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ) {
                var inputMode by remember { mutableStateOf(true) }
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "选择时间",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        TextButton(onClick = { inputMode = !inputMode }) {
                            Icon(
                                imageVector = if (inputMode) Icons.Outlined.AccessTime else Icons.Outlined.Edit,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (inputMode) "拨盘" else "键入")
                        }
                    }

                    if (inputMode) {
                        TimeInput(state = timePickerState)
                    } else {
                        TimePicker(state = timePickerState)
                    }
                }
            }

            if (availableShiftOptions.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "提醒班次",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        availableShiftOptions.forEach { (code, name) ->
                            FilterChip(
                                selected = selectedShift == code,
                                onClick = { selectedShift = code },
                                label = {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                leadingIcon = if (selectedShift == code) {
                                    {
                                        Icon(
                                            imageVector = Icons.Outlined.AccessTime,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selectedShift == code,
                                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                    selectedBorderColor = MaterialTheme.colorScheme.secondary,
                                    borderWidth = 1.dp,
                                    selectedBorderWidth = 1.5.dp
                                )
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "重复提醒",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "调节贪睡次数与间隔",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "重复响铃次数",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Slider(
                                value = snoozeCount.toFloat(),
                                onValueChange = { snoozeCount = it.roundToInt() },
                                valueRange = 0f..5f,
                                steps = 4,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "$snoozeCount 次",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "重复间隔时间",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Slider(
                                value = snoozeInterval.toFloat(),
                                onValueChange = { snoozeInterval = it.roundToInt() },
                                valueRange = 1f..10f,
                                steps = 8,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "$snoozeInterval 分钟",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(
                        text = "取消",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Button(
                    onClick = {
                        val timeString = "%02d:%02d".format(timePickerState.hour, timePickerState.minute)
                        onConfirm(timeString, selectedShift, snoozeCount, snoozeInterval)
                    },
                    enabled = selectedShift.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(
                        text = if (isEditing) "保存" else "确定",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

private fun formatDuration(remainingMillis: Long): String {
    val totalMinutes = remainingMillis / 60_000
    if (totalMinutes <= 0) return "不到 1 分钟"

    val days = totalMinutes / (24 * 60)
    val hours = (totalMinutes % (24 * 60)) / 60
    val minutes = totalMinutes % 60

    val parts = mutableListOf<String>()
    if (days > 0) {
        parts += "${days} 天"
    }
    if (hours > 0) {
        parts += "${hours} 小时"
    }
    if (minutes > 0) {
        parts += "${minutes} 分钟"
    }

    return parts.joinToString(separator = " ")
}
