package com.example.alarm_clock_2.ui

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alarm_clock_2.data.AlarmPlayMode
import com.example.alarm_clock_2.shift.IdentityType
import com.example.alarm_clock_2.update.UpdateState
import com.example.alarm_clock_2.update.UpdateUtils
import com.example.alarm_clock_2.update.UpdateViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val ui = viewModel.uiState.collectAsState().value
    val listState = rememberLazyListState()

    // 身份切换确认对话框
    var pendingIdentity by remember { mutableStateOf<IdentityType?>(null) }

    if (pendingIdentity != null) {
        AlertDialog(
            onDismissRequest = { pendingIdentity = null },
            title = { Text("切换身份", fontWeight = FontWeight.Bold) },
            text = {
                Text("切换到${pendingIdentity!!.displayName()}后，当前身份的闹钟将暂时隐藏（数据保留不会删除）。\n\n确定要切换吗？")
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingIdentity?.let { viewModel.onIdentitySelected(it) }
                    pendingIdentity = null
                }) { Text("确定切换", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { pendingIdentity = null }) { Text("取消") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "设置",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 身份选择部分
            item {
                SettingsSection(
                    title = "您的身份",
                    icon = Icons.Outlined.Person
                ) {
                    IdentitySelectionCards(
                        selected = ui.identity,
                        onSelect = { pendingIdentity = it }
                    )
                }
            }

            // 自定义班次配置
            if (ui.identity == IdentityType.CUSTOM) {
                item {
                    SettingsSection(
                        title = "自定义排班",
                        icon = Icons.Outlined.FormatListBulleted
                    ) {
                        CustomShiftConfig(
                            pattern = ui.customPattern,
                            currentIndex = ui.customIndex,
                            onPatternChange = viewModel::onCustomPatternChanged,
                            onIndexChange = viewModel::onCustomIndexChanged
                        )
                    }
                }
            }

            // 班次配置部分（根据身份显示）
            if (ui.identity != IdentityType.LONG_DAY && ui.identity != IdentityType.CUSTOM) {
                item {
                    SettingsSection(
                        title = "班次设置",
                        icon = Icons.Outlined.Schedule
                    ) {
                        ModernSettingsCard {
                            when (ui.identity) {
                                IdentityType.FOUR_THREE -> {
                                    ShiftPickerContent(
                                        title = "三班倒当前班次",
                                        selectedIndex = ui.fourThreeIndex,
                                        onSelect = viewModel::onFourThreeIndexChanged,
                                        labels = listOf("休(一)", "休(二)", "晚(一)", "晚(二)", "早(一)", "早(二)", "中(一)", "中(二)")
                                    )
                                }
                                IdentityType.FOUR_TWO -> {
                                    ShiftPickerContent(
                                        title = "两班倒当前班次",
                                        selectedIndex = ui.fourTwoIndex,
                                        onSelect = viewModel::onFourTwoIndexChanged,
                                        labels = listOf("早", "晚", "休(一)", "休(二)")
                                    )
                                }
                                else -> Unit
                            }
                        }
                    }
                }
            }

            // 闹钟全局设置
            item {
                SettingsSection(
                    title = "闹钟偏好",
                    icon = Icons.Outlined.NotificationsActive
                ) {
                    ModernSettingsCard {
                        Column {
                            RingtonePreferenceRow(ui.ringtoneUri, viewModel::onRingtoneSelected)
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            PlayModePreferenceRow(ui.playMode, viewModel::onPlayModeSelected)
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            SnoozePreferenceRow(
                                count = ui.snoozeCount,
                                interval = ui.snoozeInterval,
                                onCountChange = viewModel::onSnoozeCountChanged,
                                onIntervalChange = viewModel::onSnoozeIntervalChanged
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            TogglePreferenceRow(
                                title = "节假日自动静音",
                                description = "仅在长白班模式下生效",
                                checked = ui.holidayRest,
                                enabled = ui.identity == IdentityType.LONG_DAY,
                                onCheckedChange = viewModel::onHolidayRestChanged
                            )
                        }
                    }
                }
            }

            // 关于与更新
            item {
                SettingsSection(
                    title = "关于",
                    icon = Icons.Outlined.Info
                ) {
                    ModernSettingsCard {
                        Column {
                            VersionPreferenceRow()
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            DeveloperPreferenceRow()
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomShiftConfig(
    pattern: String,
    currentIndex: Int,
    onPatternChange: (String) -> Unit,
    onIndexChange: (Int) -> Unit
) {
    val shifts = remember(pattern) {
        com.example.alarm_clock_2.shift.ShiftCalculator.parseCustomPattern(pattern)
    }
    val cycleDays = shifts.size
    // 可选的班次类型
    val allShifts = com.example.alarm_clock_2.shift.Shift.values()

    ModernSettingsCard {
        Column(modifier = Modifier.padding(16.dp)) {
            // 周期配置：每个位置可点选切换班次
            Text("排班周期（点击切换班次）", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                shifts.forEachIndexed { idx, shift ->
                    FilterChip(
                        selected = idx == currentIndex,
                        onClick = {
                            // 轮回切换班次
                            val currentShiftIdx = allShifts.indexOf(shift)
                            val nextShift = allShifts[(currentShiftIdx + 1) % allShifts.size]
                            val newPattern = shifts.toMutableList().also { it[idx] = nextShift }
                                .joinToString(",") { it.name }
                            onPatternChange(newPattern)
                        },
                        label = { Text(customShiftLabel(shift)) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // 添加/删除周期天数
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = {
                    // 增加一天（默认 OFF）
                    onPatternChange("$pattern,OFF")
                }) {
                    Text("+ 增加一天", style = MaterialTheme.typography.labelMedium)
                }
                TextButton(
                    onClick = {
                        if (cycleDays > 2) {
                            val newPattern = shifts.dropLast(1).joinToString(",") { it.name }
                            onPatternChange(newPattern)
                            if (currentIndex >= cycleDays - 1) onIndexChange(0)
                        }
                    },
                    enabled = cycleDays > 2
                ) {
                    Text("- 减少一天", style = MaterialTheme.typography.labelMedium)
                }
            }

            if (cycleDays > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
                Text("当前是周期第几天", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (0 until cycleDays).forEach { idx ->
                        FilterChip(
                            selected = idx == currentIndex,
                            onClick = { onIndexChange(idx) },
                            label = { Text("第${idx + 1}天 · ${customShiftLabel(shifts[idx])}") },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun customShiftLabel(shift: com.example.alarm_clock_2.shift.Shift): String = when (shift) {
    com.example.alarm_clock_2.shift.Shift.DAY -> "班"
    com.example.alarm_clock_2.shift.Shift.MORNING -> "早"
    com.example.alarm_clock_2.shift.Shift.AFTERNOON -> "中"
    com.example.alarm_clock_2.shift.Shift.NIGHT -> "晚"
    com.example.alarm_clock_2.shift.Shift.OFF -> "休"
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        }
        content()
    }
}

@Composable
private fun ModernSettingsCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun IdentitySelectionCards(
    selected: IdentityType,
    onSelect: (IdentityType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IdentityCard(
            modifier = Modifier.weight(1f),
            title = "长白班",
            description = "按星期排",
            icon = Icons.Outlined.Work,
            isSelected = selected == IdentityType.LONG_DAY,
            onClick = { onSelect(IdentityType.LONG_DAY) }
        )
        IdentityCard(
            modifier = Modifier.weight(1f),
            title = "三班倒",
            description = "8天周期",
            icon = Icons.Outlined.Schedule,
            isSelected = selected == IdentityType.FOUR_THREE,
            onClick = { onSelect(IdentityType.FOUR_THREE) }
        )
        IdentityCard(
            modifier = Modifier.weight(1f),
            title = "两班倒",
            description = "4天周期",
            icon = Icons.Outlined.History,
            isSelected = selected == IdentityType.FOUR_TWO,
            onClick = { onSelect(IdentityType.FOUR_TWO) }
        )
        IdentityCard(
            modifier = Modifier.weight(1f),
            title = "自定义",
            description = "自由排班",
            icon = Icons.Outlined.FormatListBulleted,
            isSelected = selected == IdentityType.CUSTOM,
            onClick = { onSelect(IdentityType.CUSTOM) }
        )
    }
}

@Composable
private fun IdentityCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }

    Surface(
        modifier = modifier
            .height(110.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = contentColor,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShiftPickerContent(
    title: String,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    labels: List<String>
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            labels.forEachIndexed { index, label ->
                val isSelected = index == selectedIndex
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(index) },
                    label = { Text(label) },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = null
                )
            }
        }
    }
}

@Composable
private fun RingtonePreferenceRow(
    currentUri: String,
    onUriSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val ringtoneName = remember(currentUri, context) { resolveRingtoneName(context, currentUri) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) onUriSelected(uri.toString())
        }
    }

    PreferenceRow(
        title = "闹钟铃声",
        subtitle = ringtoneName,
        icon = Icons.Outlined.MusicNote,
        onClick = {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                if (currentUri.isNotBlank()) {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(currentUri))
                }
            }
            launcher.launch(intent)
        }
    )
}

@Composable
private fun PlayModePreferenceRow(
    selected: AlarmPlayMode,
    onSelect: (AlarmPlayMode) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    
    PreferenceRow(
        title = "播放模式",
        subtitle = selected.displayName(),
        icon = Icons.Outlined.Vibration,
        onClick = { showDialog = true }
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("选择播放模式") },
            text = {
                Column {
                    listOf(
                        AlarmPlayMode.SOUND to "仅响铃",
                        AlarmPlayMode.VIBRATE to "仅震动",
                        AlarmPlayMode.BOTH to "响铃+震动"
                    ).forEach { (mode, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelect(mode)
                                    showDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (selected == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (selected == mode) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun SnoozePreferenceRow(
    count: Int,
    interval: Int,
    onCountChange: (Int) -> Unit,
    onIntervalChange: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    
    PreferenceRow(
        title = "再响设置",
        subtitle = "次数: ${count}次, 间隔: ${interval}分钟",
        icon = Icons.Outlined.Snooze,
        onClick = { showDialog = true }
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("稍后提醒设置") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text("重复次数", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(1, 3, 5, 10).forEach { c ->
                                FilterChip(
                                    selected = count == c,
                                    onClick = { onCountChange(c) },
                                    label = { Text("${c}次") }
                                )
                            }
                        }
                    }
                    Column {
                        Text("间隔时间", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(5, 10, 15, 20).forEach { i ->
                                FilterChip(
                                    selected = interval == i,
                                    onClick = { onIntervalChange(i) },
                                    label = { Text("${i}分") }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("确定")
                }
            }
        )
    }
}

@Composable
private fun PreferenceRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun TogglePreferenceRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (enabled) MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.FormatListBulleted,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun VersionPreferenceRow() {
    val context = LocalContext.current
    val updateViewModel: UpdateViewModel = hiltViewModel()
    val updateState by updateViewModel.updateState.collectAsState()
    val currentVersion = remember { UpdateUtils.getCurrentVersion(context) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                updateViewModel.clearPostponedUpdate()
                updateViewModel.checkForUpdates(currentVersion, showProgress = true)
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Update,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "检查更新",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )
            Text(
                text = "当前版本: $currentVersion",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        when (updateState) {
            is UpdateState.Checking -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            }
            is UpdateState.UpdateAvailable -> {
                Text(
                    text = "有新版本",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun DeveloperPreferenceRow() {
    var showDialog by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = "开发者信息",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("确定")
                }
            },
            title = {
                Text(
                    text = "关于开发者",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("作者: TWH", style = MaterialTheme.typography.bodyLarge)
                    
                    val url = "https://github.com/LAW-t/alarm_clock"
                    val annotated = buildAnnotatedString {
                        append("项目主页: ")
                        pushStringAnnotation(tag = "URL", annotation = url)
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append("GitHub")
                        }
                        pop()
                    }
                    androidx.compose.foundation.text.ClickableText(
                        text = annotated,
                        onClick = { offset ->
                            annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                .firstOrNull()?.let { uriHandler.openUri(it.item) }
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        )
    }
}

private fun resolveRingtoneName(context: android.content.Context, uriString: String): String {
    val fallback = "系统默认"

    if (uriString.isBlank()) {
        return runCatching {
            val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            RingtoneManager.getRingtone(context, defaultUri)?.getTitle(context)
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: fallback
    }

    return runCatching {
        val uri = Uri.parse(uriString)
        RingtoneManager.getRingtone(context, uri)?.getTitle(context)
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: fallback
}

private fun IdentityType.displayName(): String = when (this) {
    IdentityType.LONG_DAY -> "长白班"
    IdentityType.FOUR_THREE -> "三班倒"
    IdentityType.FOUR_TWO -> "两班倒"
    IdentityType.CUSTOM -> "自定义排班"
}

private fun AlarmPlayMode.displayName(): String = when (this) {
    AlarmPlayMode.SOUND -> "仅响铃"
    AlarmPlayMode.VIBRATE -> "仅震动"
    AlarmPlayMode.BOTH -> "响铃+震动"
}
