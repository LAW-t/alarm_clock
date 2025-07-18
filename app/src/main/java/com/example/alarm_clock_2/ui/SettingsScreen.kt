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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.font.FontWeight
// BuildConfig will be referenced with full package name
import android.widget.Toast
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val identity = viewModel.uiState.collectAsState().value
    val ui = identity

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp) // Use spacedBy for consistent spacing
    ) {
        Column {
            Text(text = "身份选择", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            IdentityRadioGroup(selected = ui.identity, onSelect = viewModel::onIdentitySelected)
        }

        when (ui.identity) {
            IdentityType.FOUR_THREE -> FourThreeShiftPicker(ui.fourThreeIndex, viewModel::onFourThreeIndexChanged)
            IdentityType.FOUR_TWO -> FourTwoShiftPicker(ui.fourTwoIndex, viewModel::onFourTwoIndexChanged)
            else -> {} // No extra spacer needed here
        }

        // 铃声选择
        RingtonePickerRow(ui.ringtoneUri) { viewModel.onRingtoneSelected(it) }

        // 播放模式选择
        PlayModeRadioGroup(selected = ui.playMode, onSelect = viewModel::onPlayModeSelected)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "法定节假日休息")
            Spacer(Modifier.width(8.dp))
            Switch(checked = ui.holidayRest, onCheckedChange = viewModel::onHolidayRestChanged)
        }

        // 当前版本 & 更新
        VersionRow(viewModel = viewModel)

        // 开发者信息
        DeveloperInfoRow()

        // Add extra space at the bottom
        Spacer(Modifier.height(24.dp))
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
    // 顺序：休(一)0 休(二)1 晚(一)2 晚(二)3 早(一)4 早(二)5 中(一)6 中(二)7
    val labels = listOf("休(一)", "休(二)", "晚(一)", "晚(二)", "早(一)", "早(二)", "中(一)", "中(二)")

    fun indexToLabel(idx: Int): String = labels[idx % 8]
    fun labelToIndex(label: String): Int = labels.indexOf(label).coerceAtLeast(0)

    var selectedLabel by remember(selectedIndex) { mutableStateOf(indexToLabel(selectedIndex)) }

    Column {
        Text(text = "今天班次")
        Spacer(Modifier.height(4.dp))
        ShiftOptionRow(options = labels, selected = selectedLabel, allowed = labels) { newLabel ->
            selectedLabel = newLabel
            onSelect(labelToIndex(newLabel))
        }
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

    Column {
        Text(text = "今天班次")
        Spacer(Modifier.height(4.dp))
        ShiftOptionRow(options = labels, selected = todayLabel, allowed = labels) { newLabel ->
            todayLabel = newLabel
            onSelect(labelToIndex(newLabel))
        }
    }
}

// 旧两日校准函数已废弃

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShiftOptionRow(
    options: List<String>,
    selected: String,
    allowed: List<String>,
    onOptionSelect: (String) -> Unit
) {
    FlowRow {
        options.forEach { label ->
            val enabled = label in allowed
            AssistChip(
                onClick = { if (enabled) onOptionSelect(label) },
                label = { Text(label) },
                enabled = enabled,
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (label == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    labelColor = if (label == selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(Modifier.width(4.dp))
        }
    }
}

// 旧 now/tomorrow 逻辑已废弃

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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlayModeRadioGroup(selected: AlarmPlayMode, onSelect: (AlarmPlayMode) -> Unit) {
    Column {
        Text("播放模式")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                AlarmPlayMode.SOUND to "仅响铃",
                AlarmPlayMode.VIBRATE to "仅震动",
                AlarmPlayMode.BOTH to "响铃+震动"
            ).forEach { (mode, label) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
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