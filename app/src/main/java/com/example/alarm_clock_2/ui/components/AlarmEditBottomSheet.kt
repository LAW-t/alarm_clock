package com.example.alarm_clock_2.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.commandiron.wheel_picker_compose.WheelTimePicker
import com.commandiron.wheel_picker_compose.core.WheelPickerDefaults
import com.example.alarm_clock_2.data.model.ShiftOption
import com.example.alarm_clock_2.util.Constants
import java.time.LocalTime

/**
 * 闹钟编辑底部弹窗组件
 * 用于添加和编辑闹钟
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditBottomSheet(
    isVisible: Boolean,
    initialTime: String = "08:00",
    initialShift: String = "",
    initialDisplayName: String? = null,
    shiftOptions: List<ShiftOption> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (time: String, shift: String, displayName: String?) -> Unit,
    isEditMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            dragHandle = {
                Surface(
                    modifier = Modifier
                        .padding(vertical = Constants.SPACING_SMALL)
                        .size(width = 32.dp, height = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(2.dp)
                ) {}
            }
        ) {
            AlarmEditContent(
                initialTime = initialTime,
                initialShift = initialShift,
                initialDisplayName = initialDisplayName,
                shiftOptions = shiftOptions,
                onDismiss = onDismiss,
                onConfirm = onConfirm,
                isEditMode = isEditMode
            )
        }
    }
}

/**
 * 闹钟编辑内容
 */
@Composable
private fun AlarmEditContent(
    initialTime: String,
    initialShift: String,
    initialDisplayName: String?,
    shiftOptions: List<ShiftOption>,
    onDismiss: () -> Unit,
    onConfirm: (time: String, shift: String, displayName: String?) -> Unit,
    isEditMode: Boolean
) {
    var selectedTime by remember { mutableStateOf(LocalTime.parse(initialTime)) }
    var selectedShift by remember { mutableStateOf(initialShift) }
    var customDisplayName by remember { mutableStateOf(initialDisplayName ?: "") }
    var isCustomShift by remember { mutableStateOf(initialDisplayName != null) }
    
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Constants.SPACING_STANDARD)
            .padding(bottom = Constants.SPACING_LARGE),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 标题
        Text(
            text = if (isEditMode) "编辑闹钟" else "添加闹钟",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = Constants.SPACING_STANDARD)
        )
        
        // 操作按钮（顶部）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Constants.SPACING_LARGE),
            horizontalArrangement = Arrangement.spacedBy(Constants.SPACING_STANDARD)
        ) {
            // 取消按钮
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text(
                    text = "取消",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            
            // 确定按钮
            Button(
                onClick = {
                    val timeString = selectedTime.toString()
                    val finalDisplayName = if (isCustomShift && customDisplayName.isNotBlank()) {
                        customDisplayName
                    } else null
                    onConfirm(timeString, selectedShift, finalDisplayName)
                },
                modifier = Modifier.weight(1f),
                enabled = selectedShift.isNotBlank() && (!isCustomShift || customDisplayName.isNotBlank()),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = if (isEditMode) "保存" else "添加",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
        
        // 班次选择
        if (shiftOptions.isNotEmpty()) {
            ShiftSelector(
                options = shiftOptions,
                selectedShift = selectedShift,
                onShiftSelected = { shift ->
                    selectedShift = shift
                    isCustomShift = false
                },
                modifier = Modifier.padding(bottom = Constants.SPACING_STANDARD)
            )
        }
        
        // 自定义班次选项
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Constants.SPACING_STANDARD),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isCustomShift,
                onCheckedChange = { checked ->
                    isCustomShift = checked
                    if (checked) {
                        selectedShift = "CUSTOM"
                    } else {
                        selectedShift = ""
                        customDisplayName = ""
                    }
                }
            )
            Text(
                text = "自定义班次",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = Constants.SPACING_SMALL)
            )
        }
        
        // 自定义班次名称输入
        if (isCustomShift) {
            OutlinedTextField(
                value = customDisplayName,
                onValueChange = { customDisplayName = it },
                label = { Text("班次名称") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Constants.SPACING_STANDARD),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                )
            )
        }
        
        // 时间选择器
        WheelTimePicker(
            startTime = selectedTime,
            onSnappedTime = { time ->
                selectedTime = time
            },
            size = DpSize(
                width = (screenWidth * 0.8f).coerceAtMost(300.dp),
                height = 180.dp
            ),
            selectorProperties = WheelPickerDefaults.selectorProperties(
                shape = RoundedCornerShape(Constants.SMALL_CORNER_RADIUS),
                color = MaterialTheme.colorScheme.primary.copy(alpha = Constants.ALPHA_SELECTOR),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = Constants.ALPHA_BORDER)
                )
            )
        )
    }
}

/**
 * 班次选择器
 */
@Composable
private fun ShiftSelector(
    options: List<ShiftOption>,
    selectedShift: String,
    onShiftSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "选择班次",
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = Constants.SPACING_SMALL)
        )
        
        // 班次选项
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedShift == option.code,
                    onClick = { onShiftSelected(option.code) },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = Constants.SPACING_SMALL)
                )
            }
        }
    }
}
