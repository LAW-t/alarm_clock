package com.example.alarm_clock_2.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.alarm_clock_2.data.model.AlarmDisplayItem
import com.example.alarm_clock_2.util.Constants

/**
 * 闹钟卡片组件
 * 显示单个闹钟的信息，支持点击编辑和滑动删除
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmCard(
    alarm: AlarmDisplayItem,
    onToggleEnabled: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    canDelete: Boolean = true
) {
    // 动画颜色
    val containerColor by animateColorAsState(
        targetValue = if (alarm.enabled) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(Constants.ANIMATION_DURATION_STANDARD),
        label = "container_color"
    )
    
    val contentColor by animateColorAsState(
        targetValue = if (alarm.enabled) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Constants.ALPHA_DISABLED)
        },
        animationSpec = tween(Constants.ANIMATION_DURATION_STANDARD),
        label = "content_color"
    )
    
    // 滑动删除功能
    if (canDelete) {
        SwipeToDismissBox(
            state = rememberSwipeToDismissBoxState(
                confirmValueChange = { dismissValue ->
                    if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                        onDelete()
                        true
                    } else {
                        false
                    }
                }
            ),
            backgroundContent = {
                // 删除背景
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.errorContainer,
                            RoundedCornerShape(Constants.CARD_CORNER_RADIUS)
                        )
                        .padding(horizontal = Constants.SPACING_STANDARD),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除闹钟",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(Constants.ICON_SIZE_STANDARD)
                    )
                }
            },
            enableDismissFromEndToStart = true,
            enableDismissFromStartToEnd = false,
            modifier = modifier
        ) {
            AlarmCardContent(
                alarm = alarm,
                containerColor = containerColor,
                contentColor = contentColor,
                onToggleEnabled = onToggleEnabled,
                onEdit = onEdit
            )
        }
    } else {
        AlarmCardContent(
            alarm = alarm,
            containerColor = containerColor,
            contentColor = contentColor,
            onToggleEnabled = onToggleEnabled,
            onEdit = onEdit,
            modifier = modifier
        )
    }
}

/**
 * 闹钟卡片内容
 */
@Composable
private fun AlarmCardContent(
    alarm: AlarmDisplayItem,
    containerColor: Color,
    contentColor: Color,
    onToggleEnabled: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(Constants.LIST_ITEM_HEIGHT)
            .clickable { onEdit() },
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        shape = RoundedCornerShape(Constants.CARD_CORNER_RADIUS),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(Constants.SPACING_STANDARD),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 时间和标签
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // 时间显示
                Text(
                    text = alarm.time,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = Constants.TITLE_FONT_SIZE
                    ),
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 班次标签
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = alarm.getFullLabel(),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = Constants.BODY_FONT_SIZE
                        ),
                        color = contentColor.copy(alpha = Constants.ALPHA_SECONDARY),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // 自定义标识
                    if (alarm.isCustom) {
                        Spacer(modifier = Modifier.width(Constants.SPACING_SMALL))
                        Surface(
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(Constants.SMALL_CORNER_RADIUS),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "自定义",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            
            // 开关
            Switch(
                checked = alarm.enabled,
                onCheckedChange = { onToggleEnabled() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

/**
 * 闹钟卡片预览组件
 */
@Composable
fun AlarmCardPreview(
    time: String = "08:00",
    label: String = "长白班",
    enabled: Boolean = true,
    isCustom: Boolean = false
) {
    val sampleAlarm = AlarmDisplayItem(
        entity = com.example.alarm_clock_2.data.AlarmTimeEntity(
            id = 1,
            time = time,
            shift = "DAY",
            enabled = enabled,
            displayName = if (isCustom) label else null
        ),
        label = label,
        isCustom = isCustom
    )
    
    AlarmCard(
        alarm = sampleAlarm,
        onToggleEnabled = { },
        onEdit = { },
        onDelete = { },
        canDelete = isCustom
    )
}
