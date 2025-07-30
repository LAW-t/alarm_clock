package com.example.alarm_clock_2.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.alarm_clock_2.util.Constants

/**
 * 空状态组件
 * 用于显示没有数据时的提示界面
 */
@Composable
fun EmptyState(
    icon: ImageVector = Icons.Default.Alarm,
    title: String = Constants.EMPTY_STATE_TITLE,
    subtitle: String = Constants.EMPTY_STATE_SUBTITLE,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = Constants.SPACING_EXTRA_LARGE)
        ) {
            // 大号图标
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(Constants.ICON_SIZE_EXTRA_LARGE),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Constants.ALPHA_DISABLED)
            )
            
            Spacer(modifier = Modifier.height(Constants.SPACING_LARGE))
            
            // 标题
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = Constants.SUBTITLE_FONT_SIZE
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 副标题
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = Constants.BODY_FONT_SIZE
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Constants.ALPHA_SECONDARY),
                textAlign = TextAlign.Center,
                lineHeight = Constants.LINE_HEIGHT
            )
            
            // 可选的操作按钮
            if (actionText != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(Constants.SPACING_LARGE))
                
                Button(
                    onClick = onActionClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = actionText,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

/**
 * 加载状态组件
 */
@Composable
fun LoadingState(
    message: String = "加载中...",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(Constants.SPACING_STANDARD))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 错误状态组件
 */
@Composable
fun ErrorState(
    message: String = "出现错误",
    actionText: String = "重试",
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = Constants.SPACING_EXTRA_LARGE)
        ) {
            // 错误图标
            Icon(
                imageVector = Icons.Default.Alarm, // 可以替换为错误图标
                contentDescription = null,
                modifier = Modifier.size(Constants.ICON_SIZE_EXTRA_LARGE),
                tint = MaterialTheme.colorScheme.error.copy(alpha = Constants.ALPHA_DISABLED)
            )
            
            Spacer(modifier = Modifier.height(Constants.SPACING_LARGE))
            
            // 错误消息
            Text(
                text = message,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(Constants.SPACING_LARGE))
            
            // 重试按钮
            OutlinedButton(
                onClick = onActionClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.error
                    ).brush
                )
            ) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}
