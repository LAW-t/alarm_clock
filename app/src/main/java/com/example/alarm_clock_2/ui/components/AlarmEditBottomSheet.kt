@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
package com.example.alarm_clock_2.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm_clock_2.data.AlarmTimeEntity
import java.time.LocalTime
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/**
 * 美化后的闹钟编辑底部弹窗
 */
@Composable
fun AlarmEditBottomSheet(
    alarm: AlarmTimeEntity?,
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

    // 初始贪睡状态逻辑：如果是编辑且snoozeCount>0，则开启；如果是新建，默认关闭（即snoozeCount=0）
    // 或者保持原逻辑：默认开启？通常闹钟默认开启贪睡比较合理，但也看用户习惯。
    // 这里改为：如果有初始值且>0，则开启。新建时默认为 true (3次, 5分)
    var isSnoozeEnabled by remember(alarm) {
        mutableStateOf(if (isEditing) alarm!!.snoozeCount > 0 else true)
    }

    var snoozeCount by remember(alarm) {
        mutableStateOf(if (isEditing && alarm!!.snoozeCount > 0) alarm!!.snoozeCount else 3)
    }

    var snoozeInterval by remember(alarm) {
        mutableStateOf(if (isEditing && alarm!!.snoozeInterval > 0) alarm!!.snoozeInterval else 5)
    }

    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp), // 底部留白
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", style = MaterialTheme.typography.bodyLarge)
                }

                Text(
                    text = if (isEditing) "编辑闹钟" else "添加闹钟",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // 保存按钮 (右上角)
                TextButton(
                    onClick = {
                        val timeString = "%02d:%02d".format(timePickerState.hour, timePickerState.minute)
                        val finalSnoozeCount = if (isSnoozeEnabled) snoozeCount else 0
                        val finalSnoozeInterval = if (isSnoozeEnabled) snoozeInterval else 5
                        onConfirm(timeString, selectedShift, finalSnoozeCount, finalSnoozeInterval)
                    },
                    enabled = selectedShift.isNotBlank()
                ) {
                    Text(
                        "保存", 
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (selectedShift.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Unspecified
                    )
                }
            }

            // 防止 TimeInput 自动获取焦点：延迟清除 + 收起键盘
            val focusManager = LocalFocusManager.current
            val view = androidx.compose.ui.platform.LocalView.current
            LaunchedEffect(Unit) {
                focusManager.clearFocus()
                // 二次清除：TimeInput 内部可能在下一帧请求焦点
                kotlinx.coroutines.delay(100)
                focusManager.clearFocus()
                // 同时通过 View API 收起输入法
                val imm = view.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                    as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            }

            // 时间选择器部分
            var useDial by remember { mutableStateOf(false) }
            
            // 切换按钮容器，与时间输入组件对齐
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                 IconButton(onClick = { useDial = !useDial }) {
                    Icon(
                        imageVector = if (useDial) Icons.Outlined.Keyboard else Icons.Outlined.Schedule,
                        contentDescription = "切换输入模式",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (useDial) {
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            selectorColor = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.surface,
                            timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                } else {
                    TimeInput(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 班次选择卡片
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "适用班次",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    if (availableShiftOptions.isEmpty()) {
                        Text(
                            text = "当前身份下无可用班次",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableShiftOptions.forEach { (code, label) ->
                                FilterChip(
                                    selected = selectedShift == code,
                                    onClick = { selectedShift = code },
                                    label = { Text(label) },
                                    leadingIcon = if (selectedShift == code) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selectedShift == code,
                                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        selectedBorderColor = Color.Transparent
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 贪睡设置卡片
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isSnoozeEnabled = !isSnoozeEnabled }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "贪睡模式",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isSnoozeEnabled) "响铃 ${snoozeCount} 次，间隔 ${snoozeInterval} 分钟" else "已关闭",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isSnoozeEnabled,
                            onCheckedChange = { isSnoozeEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    // 展开的贪睡详细设置
                    AnimatedVisibility(
                        visible = isSnoozeEnabled,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            
                            // 次数
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("重复次数", style = MaterialTheme.typography.bodyMedium)
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ) {
                                        Text(
                                            "${snoozeCount} 次",
                                            modifier = Modifier.padding(horizontal = 4.dp),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                                Slider(
                                    value = snoozeCount.toFloat(),
                                    onValueChange = { snoozeCount = it.roundToInt() },
                                    valueRange = 1f..5f, 
                                    steps = 3, // 1, 2, 3, 4, 5 (5个点，中间3个)
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                            
                            // 间隔
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("间隔时间", style = MaterialTheme.typography.bodyMedium)
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ) {
                                        Text(
                                            "${snoozeInterval} 分钟",
                                            modifier = Modifier.padding(horizontal = 4.dp),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                                Slider(
                                    value = snoozeInterval.toFloat(),
                                    onValueChange = { snoozeInterval = it.roundToInt() },
                                    valueRange = 5f..30f,
                                    steps = 4, // 5, 10, 15, 20, 25, 30 (6个点，中间4个)
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
