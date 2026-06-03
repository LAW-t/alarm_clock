@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.alarm_clock_2.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import android.view.WindowManager
import android.util.DisplayMetrics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch
import com.example.alarm_clock_2.ui.modifiers.pressClickEffect

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel = hiltViewModel()) {
    // 固定密度：使用物理屏幕密度（不受系统"显示大小"设置影响），
    // 确保日历内所有 dp/sp 值在修改显示大小时保持不变
    val context = LocalContext.current
    val fixedDensity = remember {
        val wm = context.getSystemService(android.content.Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        Density(metrics.density, fontScale = 1f)
    }
    CompositionLocalProvider(LocalDensity provides fixedDensity) {

    val baseMonth = remember { java.time.YearMonth.now() }
    val totalPages = 240 // ±10 years window
    val initialPage = totalPages / 2
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { totalPages })
    val scope = rememberCoroutineScope()

    // 当前 pager 页面对应的 YearMonth
    val currentMonth = baseMonth.plusMonths((pagerState.currentPage - initialPage).toLong())

    // 根据 WindowSizeClass 自适应 padding，避免大显示尺寸下布局溢出
    val windowSizeClass = LocalWindowSizeClass.current
    val isCompactWidth = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact

    val outerHorizontalPad = if (isCompactWidth) 6.dp else 12.dp
    val outerVerticalPad = if (isCompactWidth) 4.dp else 8.dp
    val surfaceTopPad = if (isCompactWidth) 12.dp else 20.dp
    val surfaceSidePad = if (isCompactWidth) 12.dp else 20.dp
    val surfaceBottomPad = if (isCompactWidth) 8.dp else 12.dp
    val headerToWeekdaySpacer = if (isCompactWidth) 8.dp else 16.dp
    val weekdayToGridSpacer = if (isCompactWidth) 6.dp else 12.dp

    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val backgroundColor = MaterialTheme.colorScheme.background
    val backgroundBrush = remember(primary, primaryContainer, backgroundColor) {
        Brush.verticalGradient(
            colors = listOf(
                primary.copy(alpha = 0.15f),
                primaryContainer.copy(alpha = 0.08f),
                backgroundColor
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(horizontal = outerHorizontalPad, vertical = outerVerticalPad)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            tonalElevation = 4.dp,
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = surfaceTopPad, start = surfaceSidePad, end = surfaceSidePad, bottom = surfaceBottomPad)
            ) {
                // ----- Header -----
                val canGoPrevious = pagerState.currentPage > 0
                val canGoNext = pagerState.currentPage < totalPages - 1

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0)) }
                        },
                        modifier = Modifier.pressClickEffect(),
                        enabled = canGoPrevious
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChevronLeft,
                            contentDescription = "上一个月"
                        )
                    }

                    // 年月按钮：点击回到本月
                    Surface(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(initialPage) }
                        },
                        modifier = Modifier.pressClickEffect(),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        tonalElevation = 1.dp,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Today,
                                contentDescription = "回到本月"
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${currentMonth.year}年${currentMonth.monthValue}月",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(totalPages - 1)) }
                        },
                        modifier = Modifier.pressClickEffect(),
                        enabled = canGoNext
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = "下一个月"
                        )
                    }
                }

                Spacer(Modifier.height(headerToWeekdaySpacer))

                // ----- Day of week labels -----
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val maxGridWidth = 600.dp
                    val gridWidth = if (maxWidth > maxGridWidth) maxGridWidth else maxWidth
                    val horizontalPad = (maxWidth - gridWidth) / 2

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = horizontalPad, end = horizontalPad),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            DayOfWeek.values().forEach { dayOfWeek ->
                                val isWeekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY
                                Text(
                                    text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINESE),
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isWeekend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(weekdayToGridSpacer))

                // ----- Pager with Calendar grids -----
                HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                    val month = baseMonth.plusMonths((page - initialPage).toLong())
                    MonthGrid(month = month, viewModel = viewModel, isCompact = isCompactWidth)
                }

                // ----- Legend -----
                ShiftLegendRow()
            }
        }
    }

    } // CompositionLocalProvider(fixedDensity)

}

@Composable
private fun ShiftLegendRow() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        // 使用 FlowRow 替代 Row，窄屏时自动换行避免溢出
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            maxItemsInEachRow = 5
        ) {
            com.example.alarm_clock_2.shift.Shift.values().forEach { shift ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(color = shiftAccent(shift), shape = CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = shiftLabel(shift),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private fun shiftAccent(shift: com.example.alarm_clock_2.shift.Shift): Color = when (shift) {
    com.example.alarm_clock_2.shift.Shift.MORNING -> Color(0xFF2563EB)
    com.example.alarm_clock_2.shift.Shift.AFTERNOON -> Color(0xFF059669)
    com.example.alarm_clock_2.shift.Shift.NIGHT -> Color(0xFFF59E0B)
    com.example.alarm_clock_2.shift.Shift.DAY -> Color(0xFF4F46E5)
    com.example.alarm_clock_2.shift.Shift.OFF -> Color(0xFF64748B)
}

// Shift to simplified Chinese label
private fun shiftLabel(shift: com.example.alarm_clock_2.shift.Shift): String = when (shift) {
    com.example.alarm_clock_2.shift.Shift.DAY -> "班"
    com.example.alarm_clock_2.shift.Shift.MORNING -> "早"
    com.example.alarm_clock_2.shift.Shift.AFTERNOON -> "中"
    com.example.alarm_clock_2.shift.Shift.NIGHT -> "晚"
    com.example.alarm_clock_2.shift.Shift.OFF -> "休"
}

/**
 * 根据可用 cellHeight 自适应内容密度。
 * 当显示大小调到最大时 cellHeight 可能只有 50dp，需要缩小/隐藏部分内容以避免溢出。
 */
@Composable
private fun DayCell(day: com.example.alarm_clock_2.calendar.DayInfo?, cellHeight: androidx.compose.ui.unit.Dp) {
    if (day == null) {
        Spacer(modifier = Modifier.height(cellHeight))
        return
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    val today = java.time.LocalDate.now()
    val isToday = day.date == today

    val dayBackground = when {
        isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        day.isOffDay -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else -> Color.Transparent
    }

    val holiday = day.holiday

    // 根据 cellHeight 分三档自适应：< 58dp 紧凑 / < 70dp 中等 / >= 70dp 完整
    val density = when {
        cellHeight < 58.dp -> CellDensity.COMPACT
        cellHeight < 70.dp -> CellDensity.MEDIUM
        else -> CellDensity.FULL
    }

    val circleSize = when (density) {
        CellDensity.COMPACT -> 22.dp
        CellDensity.MEDIUM -> 26.dp
        CellDensity.FULL -> 28.dp
    }
    val dateStyle = when {
        density == CellDensity.COMPACT -> MaterialTheme.typography.bodySmall
        isToday -> MaterialTheme.typography.titleMedium
        else -> MaterialTheme.typography.titleSmall
    }
    val subTextStyle = when (density) {
        CellDensity.COMPACT -> MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)
        CellDensity.MEDIUM -> MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
        CellDensity.FULL -> MaterialTheme.typography.labelSmall
    }
    val verticalPad = when (density) {
        CellDensity.COMPACT -> 1.dp
        CellDensity.MEDIUM -> 1.dp
        CellDensity.FULL -> 2.dp
    }
    val spacerH = when (density) {
        CellDensity.COMPACT -> 1.dp
        CellDensity.MEDIUM -> 1.dp
        CellDensity.FULL -> 1.dp
    }
    val showShiftChip = density != CellDensity.COMPACT
    val holidayDotSize = when (density) {
        CellDensity.COMPACT -> 6.dp
        else -> 8.dp
    }

    // 使用 Surface 承载点击与圆角，这样涟漪与点击区域与视觉圆角完全一致
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = cellHeight)
            .padding(all = 1.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().pressClickEffect(),
            onClick = {
                day.holiday?.name?.let { name ->
                    android.widget.Toast.makeText(context, name, android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            shape = RoundedCornerShape(14.dp),
            color = dayBackground,
            tonalElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = verticalPad),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
        // Date number with unified circle wrapper
        Surface(
            modifier = Modifier.size(circleSize),
            shape = CircleShape,
            color = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
            tonalElevation = if (isToday && density != CellDensity.COMPACT) 3.dp else 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = day.date.dayOfMonth.toString(),
                    style = dateStyle,
                    color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )

                if (holiday != null && (day.isOffDay || day.workdayOverrideApplied)) {
                    val indicatorBorder = if (isToday) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    }
                    val dotColor = if (day.isOffDay) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-2).dp, y = 2.dp)
                            .size(holidayDotSize)
                            .zIndex(1f)
                            .background(dotColor, CircleShape)
                            .border(0.75.dp, indicatorBorder, CircleShape)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(spacerH))

        val subText = when {
            holiday == null -> day.lunarDay
            day.isOffDay -> holiday.name.take(2)
            day.workdayOverrideApplied -> "上班"
            else -> day.lunarDay
        }
        Text(
            text = subText,
            style = subTextStyle,
            maxLines = 1,
            color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (showShiftChip) {
            Spacer(modifier = Modifier.height(spacerH))
            ShiftChip(shift = day.shift, density = density)
        }
            }
        }
    }
}

/** 单元格内容密度等级，用于适配不同显示大小 */
private enum class CellDensity { COMPACT, MEDIUM, FULL }

// ------- 月份网格 -------
@Composable
private fun MonthGrid(
    month: java.time.YearMonth,
    viewModel: CalendarViewModel = hiltViewModel(),
    isCompact: Boolean = false
) {
    val monthInfo by viewModel.monthInfoFlow(month).collectAsState(initial = com.example.alarm_clock_2.calendar.MonthInfo(month, viewModel.peekMonthDays(month)))
    val days = monthInfo.days

    if (days.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // 紧凑模式下降低最小单元格高度与 padding，释放更多空间给内容
            val minHeight = if (isCompact) 38.dp else 44.dp
            val maxHeightCap = 120.dp

            // 1. 决定行间距大小 (自适应，并限制在 1dp-4dp)
            val desiredGap = (maxHeight * 0.015f).coerceIn(1.dp, 4.dp)
            val lazyColumnPadding = if (isCompact) 6.dp else 10.dp

            // 2. 用剩余空间计算单元格高度（扣除 LazyColumn 内边距与行间距）
            val totalGapHeight = desiredGap * 5 // 6行之间有5个间距
            val heightForCells = (maxHeight - totalGapHeight - lazyColumnPadding * 2).coerceAtLeast(0.dp)
            val rawHeight = if (heightForCells > 0.dp) heightForCells / 6 else 0.dp

            // 3. 应用最大/最小高度约束
            val cellHeight = rawHeight.coerceIn(minHeight, maxHeightCap)

            // 始终允许滚动作为安全网：当格子内容因 heightIn(min) 超出计算高度时不会裁剪末行

            // prepare weeks — 补前导空白使1号对齐到正确的星期列
            val firstDayOfWeekIdx = days.firstOrNull()?.date?.dayOfWeek?.value?.minus(1) ?: 0 // 0=Mon, 6=Sun
            val totalSlots = 7 * 6
            val padded: List<com.example.alarm_clock_2.calendar.DayInfo?> =
                List(firstDayOfWeekIdx) { null } +
                        days.map { it } +
                        List(totalSlots - firstDayOfWeekIdx - days.size) { null }
            val weeks = padded.chunked(7)

            // 屏幕过宽时限制网格最大宽度并居中
            val maxGridWidth = 600.dp
            val gridWidth = if (maxWidth > maxGridWidth) maxGridWidth else maxWidth
            val horizontalPad = (maxWidth - gridWidth) / 2

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = horizontalPad, end = horizontalPad),
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 1.dp,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = lazyColumnPadding, vertical = lazyColumnPadding),
                    verticalArrangement = Arrangement.spacedBy(desiredGap),
                    userScrollEnabled = true // 始终可滚动，避免末行裁剪
                ) {
                    items(weeks) { week ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            week.forEach { d ->
                                Box(modifier = Modifier.weight(1f)) {
                                    DayCell(day = d, cellHeight = cellHeight)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShiftChip(shift: com.example.alarm_clock_2.shift.Shift, density: CellDensity = CellDensity.FULL) {
    val accent = shiftAccent(shift)
    val containerColor = accent.copy(alpha = 0.18f)

    val hPad = when (density) {
        CellDensity.COMPACT -> 3.dp
        CellDensity.MEDIUM -> 4.dp
        CellDensity.FULL -> 6.dp
    }
    val vPad = when (density) {
        CellDensity.COMPACT -> 1.dp
        else -> 2.dp
    }
    val textStyle = when (density) {
        CellDensity.COMPACT -> MaterialTheme.typography.labelSmall
        CellDensity.MEDIUM -> MaterialTheme.typography.labelMedium
        CellDensity.FULL -> MaterialTheme.typography.labelMedium
    }

    Box(
        modifier = Modifier
            .wrapContentWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .padding(horizontal = hPad, vertical = vPad),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = shiftLabel(shift),
            color = accent,
            style = textStyle
        )
    }
}

 
