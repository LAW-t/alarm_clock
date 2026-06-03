@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.alarm_clock_2.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
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
 
import com.example.alarm_clock_2.ui.components.AlarmEditBottomSheet
import com.example.alarm_clock_2.shift.IdentityType
import com.example.alarm_clock_2.ui.modifiers.bounceClick
import com.example.alarm_clock_2.ui.modifiers.pressClickEffect
import com.example.alarm_clock_2.data.AlarmTimeEntity
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
    // 这里直接使用导入类型，避免全类名，提高可读性
    var editingAlarm by remember { mutableStateOf<AlarmTimeEntity?>(null) }
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
            IdentityType.CUSTOM -> listOf(
                "DAY" to "白班",
                "MORNING" to "早班",
                "AFTERNOON" to "中班",
                "NIGHT" to "晚班"
            )
        }
    }

    // 显示所有闹钟，不区分默认和自定义
    data class AlarmDisplayItem(
        val entity: AlarmTimeEntity
    )

    // 身份隔离：只显示当前身份对应的闹钟
    val currentIdentityAlarms = alarmsDb.filter { alarm ->
        alarm.identity == uiState.identity.name
    }

    val allAlarms = currentIdentityAlarms.map { alarm ->
        AlarmDisplayItem(alarm)
    }
    val shiftLabels = getAvailableShiftOptions(uiState.identity).associate { it.first to it.second }

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

    val snackbarHostState = remember { SnackbarHostState() }
    // 监听撤销删除事件
    LaunchedEffect(Unit) {
        viewModel.undoDeleteFlow.collect { (msg, alarm) ->
            val result = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = "撤销",
                duration = androidx.compose.material3.SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDeleteAlarm(alarm)
            }
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
                    modifier = Modifier
                        .size(60.dp)
                        .pressClickEffect(),
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
                        AlarmItemWithLongPress(
                            alarm = item.entity,
                            shiftLabel = shiftLabels[item.entity.shift] ?: item.entity.shift,
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

        // 顶部撤销 Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp, start = 16.dp, end = 16.dp),
            snackbar = { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    actionColor = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.padding(0.dp)
                )
            }
        )
    }

    // 添加闹钟对话框
    if (showAddDialog) {
        AlarmEditBottomSheet(
            alarm = null,
            availableShiftOptions = getAvailableShiftOptions(uiState.identity),
            onDismiss = { showAddDialog = false },
            onConfirm = { time, shift, snoozeCount, snoozeInterval ->
                viewModel.addAlarm(time, shift, null, snoozeCount, snoozeInterval)
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
                viewModel.updateAlarm(editingAlarm!!, time, shift, null, snoozeCount, snoozeInterval)
                showEditDialog = false
                editingAlarm = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun noop() {}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun AlarmItemWithLongPress(
    alarm: AlarmTimeEntity,
    shiftLabel: String,
    isUpcoming: Boolean,
    nextTriggerMillis: Long?,
    currentTimeMillis: Long,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pressClickEffect()
            .combinedClickable(
                onClick = onEdit,
                onLongClick = { showDeleteDialog = true }
            )
    ) {
        ModernAlarmCard(
            alarm = alarm,
            shiftLabel = shiftLabel,
            isUpcoming = isUpcoming,
            nextTriggerMillis = nextTriggerMillis,
            currentTimeMillis = currentTimeMillis,
            onToggle = onToggle
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除闹钟") },
            text = { Text("确定要删除该闹钟吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun ModernAlarmCard(
    alarm: AlarmTimeEntity,
    shiftLabel: String,
    isUpcoming: Boolean,
    nextTriggerMillis: Long?,
    currentTimeMillis: Long,
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

    // 卡片容器颜色：启用时使用 Surface，禁用时透明度降低
    val containerColor = MaterialTheme.colorScheme.surface
    
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isEnabled) 3.dp else 0.5.dp),
        border = if (!isEnabled) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 时间和信息区域
                Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = timeDisplay,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = 42.sp,
                            fontWeight = if (isEnabled) FontWeight.Medium else FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isEnabled) 1f else 0.38f)
                    )
                    
                    // "即将响铃" 标签
                    if (isUpcoming && isEnabled) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "即将响铃",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 班次信息
                if (shiftLabel.isNotBlank()) {
                    Text(
                        text = "班次：$shiftLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = if (isEnabled) 0.9f else 0.4f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 副标题信息
                val subtitleText = buildString {
                    if (isEnabled) {
                        remainingDescription?.let { append(it + " 后") }
                    } else {
                        append("已关闭")
                    }
                }
                
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isEnabled) 1f else 0.38f)
                )

                // 贪睡信息
                if (alarm.snoozeCount > 0 && isEnabled) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "贪睡开启: ${alarm.snoozeCount}次, 间隔${alarm.snoozeInterval}分",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }
            }

            // 开关
            Switch(
                checked = isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
    }
}


@Composable
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
