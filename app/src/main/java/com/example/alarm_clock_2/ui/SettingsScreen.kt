package com.example.alarm_clock_2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*  // 导入 remember, mutableStateOf, getValue, setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alarm_clock_2.shift.IdentityType
import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import com.example.alarm_clock_2.data.AlarmPlayMode
import androidx.compose.material3.CircularProgressIndicator
import com.example.alarm_clock_2.util.Updater
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.font.FontWeight
// BuildConfig will be referenced with full package name
import android.widget.Toast

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val identity = viewModel.uiState.collectAsState().value
    val ui = identity

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(text = "身份选择", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        IdentityRadioGroup(selected = ui.identity, onSelect = viewModel::onIdentitySelected)

        Spacer(Modifier.height(16.dp))

        when (ui.identity) {
            IdentityType.FOUR_THREE -> FourThreeShiftPicker(ui.fourThreeIndex, viewModel::onFourThreeIndexChanged)
            IdentityType.FOUR_TWO -> FourTwoShiftPicker(ui.fourTwoIndex, viewModel::onFourTwoIndexChanged)
            else -> {}
        }

        Spacer(Modifier.height(16.dp))

        // 铃声选择
        RingtonePickerRow(ui.ringtoneUri) { viewModel.onRingtoneSelected(it) }

        Spacer(Modifier.height(16.dp))

        // 播放模式选择
        PlayModeRadioGroup(selected = ui.playMode, onSelect = viewModel::onPlayModeSelected)

        Spacer(Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "法定节假日休息")
            Spacer(Modifier.width(8.dp))
            Switch(checked = ui.holidayRest, onCheckedChange = viewModel::onHolidayRestChanged)
        }

        Spacer(Modifier.height(24.dp))

        // 当前版本 & 更新
        VersionRow(viewModel = viewModel)

        // 开发者信息
        DeveloperInfoRow()
    }
}

@Composable
private fun IdentityRadioGroup(
    selected: IdentityType,
    onSelect: (IdentityType) -> Unit
) {
    Column {
        IdentityType.values().forEach { type ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                RadioButton(
                    selected = type == selected,
                    onClick = { onSelect(type) }
                )
                Spacer(Modifier.width(8.dp))
                Text(text = when (type) {
                    IdentityType.LONG_DAY -> "长白班"
                    IdentityType.FOUR_THREE -> "四班三运转"
                    IdentityType.FOUR_TWO -> "四班两运转"
                })
            }
        }
    }
}

@Composable
private fun FourThreeShiftPicker(selectedIndex: Int, onSelect: (Int) -> Unit) {
    val pattern = listOf("休", "休", "晚", "晚", "早", "早", "中", "中")
    val size = pattern.size

    // 本地状态：今天/明天班次
    var today by remember(selectedIndex) { mutableStateOf(pattern[selectedIndex % size]) }
    var tomorrow by remember(selectedIndex) { mutableStateOf(pattern[(selectedIndex + 1) % size]) }

    val options = listOf("休", "早", "中", "晚")

    // 根据另一日班次限制可选项
    val allowedToday = remember(tomorrow) { precedingSet(pattern, tomorrow) }
    val allowedTomorrow = remember(today) { followingSet(pattern, today) }

    Text(text = "今天班次")
    Spacer(Modifier.height(4.dp))
    ShiftOptionRow(options, today, allowedToday) { newToday ->
        today = newToday
        // 若明天班次不再合法，则自动调整为首个合法值
        if (tomorrow !in followingSet(pattern, newToday)) {
            tomorrow = followingSet(pattern, newToday).first()
        }
        tryCommit(pattern, today, tomorrow, onSelect)
    }

    Spacer(Modifier.height(8.dp))

    Text(text = "明天班次")
    Spacer(Modifier.height(4.dp))
    ShiftOptionRow(options, tomorrow, allowedTomorrow) { newTomorrow ->
        tomorrow = newTomorrow
        // 若今天班次不合法，自动调整
        if (today !in precedingSet(pattern, newTomorrow)) {
            today = precedingSet(pattern, newTomorrow).first()
        }
        tryCommit(pattern, today, tomorrow, onSelect)
    }
}

@Composable
private fun FourTwoShiftPicker(selectedIndex: Int, onSelect: (Int) -> Unit) {
    val labels = listOf("早", "晚", "休(一)", "休(二)")

    fun indexToLabel(idx: Int): String = when (idx % 4) {
        0 -> "早"
        1 -> "晚"
        2 -> "休(一)"
        else -> "休(二)"
    }

    fun labelToIndex(label: String): Int = when (label) {
        "早" -> 0
        "晚" -> 1
        "休(一)" -> 2
        else -> 3 // "休(二)"
    }

    var todayLabel by remember(selectedIndex) { mutableStateOf(indexToLabel(selectedIndex)) }

    Text(text = "今天班次")
    Spacer(Modifier.height(4.dp))
    ShiftOptionRow(options = labels, selected = todayLabel, allowed = labels) { newLabel ->
        todayLabel = newLabel
        onSelect(labelToIndex(newLabel))
    }
}

private fun tryCommit(pattern: List<String>, today: String, tomorrow: String, commit: (Int)->Unit) {
    val idx = computeIndex(pattern, today, tomorrow)
    if (idx != -1) {
        commit(idx)
    }
}

@Composable
private fun ShiftOptionRow(
    options: List<String>,
    selected: String,
    allowed: List<String>,
    onOptionSelect: (String) -> Unit
) {
    Row {
        options.forEach { label ->
            val enabled = label in allowed
            FilterChip(label = label, selected = label == selected, enabled = enabled) {
                if (enabled) onOptionSelect(label)
            }
            Spacer(Modifier.width(4.dp))
        }
    }
}

// 计算符合今明班次组合的周期起始下标，若无匹配返回 0
private fun computeIndex(pattern: List<String>, today: String, tomorrow: String): Int {
    val size = pattern.size
    for (i in 0 until size) {
        if (pattern[i] == today && pattern[(i + 1) % size] == tomorrow) return i
    }
    return -1 // -1 表示未找到匹配组合
}

// 支持禁用
@Composable
private fun FilterChip(label: String, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = null,
        enabled = enabled,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            labelColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    )
}

// 根据 pattern 计算合法下一天班次集合
private fun followingSet(pattern: List<String>, today: String): List<String> {
    val size = pattern.size
    val set = mutableSetOf<String>()
    for (i in 0 until size) if (pattern[i] == today) set.add(pattern[(i + 1) % size])
    return if (set.isNotEmpty()) set.toList() else pattern // fallback 全部
}

// 根据 pattern 计算合法前一天班次集合
private fun precedingSet(pattern: List<String>, tomorrow: String): List<String> {
    val size = pattern.size
    val set = mutableSetOf<String>()
    for (i in 0 until size) if (pattern[i] == tomorrow) set.add(pattern[(i - 1 + size) % size])
    return if (set.isNotEmpty()) set.toList() else pattern
}

@Composable
private fun RingtonePickerRow(currentUri: String, onUriSelected: (String) -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) onUriSelected(uri.toString())
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "铃声")
        Spacer(Modifier.width(8.dp))
        Button(onClick = {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                if (currentUri.isNotBlank()) {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(currentUri))
                }
            }
            launcher.launch(intent)
        }) {
            Text("选择")
        }
    }
}

@Composable
private fun PlayModeRadioGroup(selected: AlarmPlayMode, onSelect: (AlarmPlayMode) -> Unit) {
    Column {
        Text("播放模式")
        Row(verticalAlignment = Alignment.CenterVertically) {
            listOf(
                AlarmPlayMode.SOUND to "仅响铃",
                AlarmPlayMode.VIBRATE to "仅震动",
                AlarmPlayMode.BOTH to "响铃+震动"
            ).forEach { (mode, label) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                    RadioButton(selected = mode == selected, onClick = { onSelect(mode) })
                    Text(label)
                }
            }
        }
    }
}

@Composable
private fun VersionRow(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    // No need to collect latest separately
    // val latest by viewModel.latestVersion.collectAsState(initial = null)
    val uiState by viewModel.uiState.collectAsState()
    val current: String = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "N/A"
        } catch (e: Exception) {
            "N/A"
        }
    }

    Row(modifier = Modifier
        .fillMaxWidth()
        .clickable { showDialog = true }
        .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text("应用版本")
        Spacer(Modifier.weight(1f))
        Text(current)
    }

    if (showDialog) {
        var downloading by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!downloading) showDialog = false },
            confirmButton = {
                Row {
                    if (uiState.latestVersion != null && uiState.latestVersion != current) {
                        Button(enabled = !downloading, onClick = {
                            scope.launch {
                                downloading = true
                                val info = Updater.fetchLatest()
                                if (info != null) {
                                    Updater.startDownload(context, info.apkUrl) {
                                        Toast.makeText(context, "已开始下载，请查看通知", Toast.LENGTH_LONG).show()
                                    }
                                }
                                downloading = false
                                showDialog = false
                            }
                        }) { Text("更新到最新版本") }
                        Spacer(Modifier.width(8.dp))
                    }
                    TextButton(enabled = !downloading, onClick = { showDialog = false }) { Text("关闭") }
                }
            },
            title = { Text("应用版本") },
            text = {
                Column {
                    Text("当前版本：$current")
                    Text("最新版本：${uiState.latestVersion ?: "检查中..."}")
                    if (downloading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("正在下载更新…")
                        }
                    }
                }
            }
        )
    }
}

// 开发者信息
@Composable
private fun DeveloperInfoRow() {
    var showDialog by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("开发者信息")
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { Text("确定") }
            },
            title = { Text("开发者信息", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.Start) {
                    // Text("德铜信息中心制作")
                    Spacer(Modifier.height(8.dp))
                    Text("作者: TWH")
                    Spacer(Modifier.height(8.dp))

                    val url = "https://github.com/LAW-t/alarm_clock"
                    val annotated: AnnotatedString = buildAnnotatedString {
                        append("GitHub: ")
                        pushStringAnnotation(tag = "URL", annotation = url)
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                            append(url)
                        }
                        pop()
                    }
                    ClickableText(text = annotated, onClick = { offset ->
                        annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                            .firstOrNull()?.let { uriHandler.openUri(it.item) }
                    })
                }
            }
        )
    }
} 