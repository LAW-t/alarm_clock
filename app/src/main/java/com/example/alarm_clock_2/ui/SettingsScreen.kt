package com.example.alarm_clock_2.ui

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alarm_clock_2.data.AlarmPlayMode
import com.example.alarm_clock_2.shift.IdentityType
import com.example.alarm_clock_2.update.UpdateState
import com.example.alarm_clock_2.update.UpdateUtils
import com.example.alarm_clock_2.update.UpdateViewModel
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.text.style.TextAlign

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val ui = viewModel.uiState.collectAsState().value
    val listState = rememberLazyListState()

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            MaterialTheme.colorScheme.background
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                SettingsCard(
                    title = "身份选择",
                    subtitle = "根据排班选择最适合的身份配置"
                ) {
                    IdentityRadioGroup(selected = ui.identity, onSelect = viewModel::onIdentitySelected)
                }
            }

            when (ui.identity) {
                IdentityType.FOUR_THREE -> {
                    item {
                        SettingsCard(
                            title = "四班三运转配置",
                            subtitle = "选择今天您所在的班次"
                        ) {
                            FourThreeShiftPicker(ui.fourThreeIndex, viewModel::onFourThreeIndexChanged)
                        }
                    }
                }
                IdentityType.FOUR_TWO -> {
                    item {
                        SettingsCard(
                            title = "四班两运转配置",
                            subtitle = "选择今天您所在的班次"
                        ) {
                            FourTwoShiftPicker(ui.fourTwoIndex, viewModel::onFourTwoIndexChanged)
                        }
                    }
                }
                else -> Unit
            }

            item {
                SettingsCard(
                    title = "闹钟设置",
                    subtitle = "个性化响铃方式与节假日策略"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        RingtonePickerRow(ui.ringtoneUri) { viewModel.onRingtoneSelected(it) }
                        PlayModeChipGroup(selected = ui.playMode, onSelect = viewModel::onPlayModeSelected)
                        SettingsToggleRow(
                            title = "法定节假日休息",
                            description = "节假日期间自动静音闹钟",
                            checked = ui.holidayRest,
                            onCheckedChange = viewModel::onHolidayRestChanged
                        )
                    }
                }
            }

            item {
                SettingsCard(
                    title = "应用信息",
                    subtitle = "保持更新，与开发者保持联系"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        VersionRow()
                        DeveloperInfoRow()
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IdentityRadioGroup(
    selected: IdentityType,
    onSelect: (IdentityType) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IdentityType.values().forEach { type ->
            val label = type.displayName()
            val selectedState by rememberUpdatedState(newValue = selected == type)
            FilterChip(
                selected = selectedState,
                onClick = { onSelect(type) },
                label = { Text(label) },
                leadingIcon = if (selectedState) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null
                        )
                    }
                } else {
                    null
                },
                shape = RoundedCornerShape(16.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
private fun FourThreeShiftPicker(selectedIndex: Int, onSelect: (Int) -> Unit) {
    val labels = listOf("休(一)", "休(二)", "晚(一)", "晚(二)", "早(一)", "早(二)", "中(一)", "中(二)")

    fun indexToLabel(idx: Int): String = labels[idx % labels.size]
    fun labelToIndex(label: String): Int = labels.indexOf(label).coerceAtLeast(0)

    var selectedLabel by remember(selectedIndex) { mutableStateOf(indexToLabel(selectedIndex)) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "今天班次",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ShiftOptionRow(options = labels, selected = selectedLabel, allowed = labels) { newLabel ->
            selectedLabel = newLabel
            onSelect(labelToIndex(newLabel))
        }
    }
}

@Composable
private fun FourTwoShiftPicker(selectedIndex: Int, onSelect: (Int) -> Unit) {
    val labels = listOf("早", "晚", "休(一)", "休(二)")

    fun indexToLabel(idx: Int): String = when (idx % labels.size) {
        0 -> "早"
        1 -> "晚"
        2 -> "休(一)"
        else -> "休(二)"
    }

    fun labelToIndex(label: String): Int = when (label) {
        "早" -> 0
        "晚" -> 1
        "休(一)" -> 2
        else -> 3
    }

    var todayLabel by remember(selectedIndex) { mutableStateOf(indexToLabel(selectedIndex)) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "今天班次",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ShiftOptionRow(options = labels, selected = todayLabel, allowed = labels) { newLabel ->
            todayLabel = newLabel
            onSelect(labelToIndex(newLabel))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShiftOptionRow(
    options: List<String>,
    selected: String,
    allowed: List<String>,
    onOptionSelect: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        options.forEach { label ->
            val enabled = label in allowed
            val isSelected = label == selected
            FilterChip(
                selected = isSelected,
                onClick = { if (enabled) onOptionSelect(label) },
                label = { Text(label) },
                enabled = enabled,
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null
                        )
                    }
                } else {
                    null
                },
                shape = RoundedCornerShape(14.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RingtonePickerRow(currentUri: String, onUriSelected: (String) -> Unit) {
    val context = LocalContext.current
    val ringtoneName = remember(currentUri, context) { resolveRingtoneName(context, currentUri) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) onUriSelected(uri.toString())
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
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
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Alarm,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "铃声",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "当前：$ringtoneName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "更改",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlayModeChipGroup(selected: AlarmPlayMode, onSelect: (AlarmPlayMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "播放模式",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf(
                AlarmPlayMode.SOUND to "仅响铃",
                AlarmPlayMode.VIBRATE to "仅震动",
                AlarmPlayMode.BOTH to "响铃+震动"
            ).forEach { (mode, label) ->
                val isSelected = mode == selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(mode) },
                    label = { Text(label) },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null
                            )
                        }
                    } else null,
                    shape = RoundedCornerShape(16.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
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
private fun VersionRow() {
    val context = LocalContext.current
    val updateViewModel: UpdateViewModel = hiltViewModel()
    val updateState by updateViewModel.updateState.collectAsState()

    val currentVersion = remember { UpdateUtils.getCurrentVersion(context) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
        onClick = {
            updateViewModel.clearPostponedUpdate()
            updateViewModel.checkForUpdates(currentVersion, showProgress = true)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Update,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "检查更新",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
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
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is UpdateState.NoUpdateAvailable -> {
                    Text(
                        text = "已是最新",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is UpdateState.Error -> {
                    Text(
                        text = "检查失败",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> {
                    Text(
                        text = "点击检查",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeveloperInfoRow() {
    var showDialog by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
        onClick = { showDialog = true }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "开发者信息",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
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
                    text = "开发者信息",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("作者: TWH")

                    val url = "https://github.com/LAW-t/alarm_clock"
                    val annotated: AnnotatedString = buildAnnotatedString {
                        append("GitHub: ")
                        pushStringAnnotation(tag = "URL", annotation = url)
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append(url)
                        }
                        pop()
                    }
                    androidx.compose.foundation.text.ClickableText(
                        text = annotated,
                        onClick = { offset ->
                            annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                .firstOrNull()?.let { uriHandler.openUri(it.item) }
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Start)
                    )
                }
            }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            content()
        }
    }
}

private fun IdentityType.displayName(): String = when (this) {
    IdentityType.LONG_DAY -> "长白班"
    IdentityType.FOUR_THREE -> "四班三运转"
    IdentityType.FOUR_TWO -> "四班两运转"
}

private fun AlarmPlayMode.displayName(): String = when (this) {
    AlarmPlayMode.SOUND -> "仅响铃"
    AlarmPlayMode.VIBRATE -> "仅震动"
    AlarmPlayMode.BOTH -> "响铃+震动"
}
