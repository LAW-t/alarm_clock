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
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel = hiltViewModel()) {
    // 首次安装时节假日数据在后台加载，去掉阻塞式 Dialog

    val baseMonth = remember { java.time.YearMonth.now() }
    val totalPages = 240 // ±10 years window
    val initialPage = totalPages / 2
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { totalPages })
    val scope = rememberCoroutineScope()

    // 当前 pager 页面对应的 YearMonth
    val currentMonth = baseMonth.plusMonths((pagerState.currentPage - initialPage).toLong())

    // YearMonth Picker 弹窗
    var showYmPicker by remember { mutableStateOf(false) }

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
            .padding(horizontal = 12.dp, vertical = 8.dp)
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
                    .padding(top = 24.dp, start = 20.dp, end = 20.dp, bottom = 16.dp)
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
                        enabled = canGoPrevious
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChevronLeft,
                            contentDescription = "上一个月"
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            onClick = { showYmPicker = true },
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
                                    imageVector = Icons.Rounded.CalendarMonth,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${currentMonth.year}年${currentMonth.monthValue}月",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        AssistChip(
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(initialPage) }
                            },
                            label = { Text("回到本月") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Today,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }

                    IconButton(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(totalPages - 1)) }
                        },
                        enabled = canGoNext
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = "下一个月"
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

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

                Spacer(Modifier.height(12.dp))

                // ----- Pager with Calendar grids -----
                HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                    val month = baseMonth.plusMonths((page - initialPage).toLong())
                    MonthGrid(month = month, viewModel = viewModel)
                }

                // ----- Legend -----
                ShiftLegendRow()
            }
        }
    }

    if (showYmPicker) {
        YearMonthPickerDialog(currentMonth = currentMonth, onConfirm = { ym ->
            val diff = ChronoUnit.MONTHS.between(baseMonth, ym).toInt()
            val targetPage = (initialPage + diff).coerceIn(0, totalPages - 1)
            scope.launch { pagerState.animateScrollToPage(targetPage) }
            showYmPicker = false
        }, onDismiss = { showYmPicker = false })
    }
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            com.example.alarm_clock_2.shift.Shift.values().forEach { shift ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(color = shiftAccent(shift), shape = CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = shiftLabel(shift),
                        style = MaterialTheme.typography.labelMedium,
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

    // 使用 Surface 承载点击与圆角，这样涟漪与点击区域与视觉圆角完全一致
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = cellHeight)
            .padding(all = 4.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
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
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
        // Date number with unified circle wrapper
        val fontScale = LocalConfiguration.current.fontScale
        val circleSize = (56.dp * fontScale).coerceIn(14.dp, 26.dp)

        Surface(
            modifier = Modifier.size(circleSize),
            shape = CircleShape,
            color = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
            tonalElevation = if (isToday) 3.dp else 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = day.date.dayOfMonth.toString(),
                    style = if (isToday) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                    color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )

                if (holiday != null && (day.isOffDay || day.workdayOverrideApplied)) {
                    val indicatorBorder = if (isToday) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    }
                    val dotColor = if (day.isOffDay) {
                        // Off-day (休) -> red dot
                        MaterialTheme.colorScheme.error
                    } else {
                        // Adjusted workday (上班) -> primary dot
                        MaterialTheme.colorScheme.primary
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-2).dp, y = 2.dp) // move inward to avoid clipping
                            .size(8.dp)
                            .zIndex(1f)
                            .background(dotColor, CircleShape)
                            .border(0.75.dp, indicatorBorder, CircleShape)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val subText = when {
            holiday == null -> day.lunarDay
            day.isOffDay -> holiday.name.take(2)
            day.workdayOverrideApplied -> "上班"
            else -> day.lunarDay
        }
        Text(
            text = subText,
            style = MaterialTheme.typography.labelSmall,
            color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(3.dp))

        // Shift label chip
        ShiftChip(shift = day.shift)
            }
        }
    }
}

// ------- 月份网格 -------
@Composable
private fun MonthGrid(month: java.time.YearMonth, viewModel: CalendarViewModel = hiltViewModel()) {
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
            val fontScale = LocalConfiguration.current.fontScale
            val baseMin = 48.dp
            val minHeight = baseMin * fontScale.coerceIn(1f, 1.4f)
            val maxHeightCap = 80.dp * fontScale.coerceIn(1f, 1.4f)

            // 修正：先为行间距预留空间，再计算单元格高度
            // 1. 决定行间距大小 (自适应，并限制在 2dp-8dp)
            val desiredGap = (maxHeight * 0.02f).coerceIn(2.dp, 8.dp)
            val totalGapHeight = desiredGap * 5 // 6行之间有5个间距
            val verticalContainerPadding = 24.dp

            // 2. 用剩余空间计算单元格高度
            val heightForCells = (maxHeight - verticalContainerPadding - totalGapHeight).coerceAtLeast(0.dp)
            val rawHeight = if (heightForCells > 0.dp) heightForCells / 6 else 0.dp

            // 3. 应用最大/最小高度约束
            val cellHeight = rawHeight.coerceIn(minHeight, maxHeightCap)

            // 视图所需总高度按实际 cellHeight 重新计算
            val needHeight = cellHeight * 6

            // prepare weeks
            // Insert leading blanks so the first day aligns with its weekday column
            val firstDayOfWeekIdx = days.firstOrNull()?.date?.dayOfWeek?.value?.minus(1) ?: 0 // 0=Mon, 6=Sun

            val totalSlots = 7 * 6
            val padded: List<com.example.alarm_clock_2.calendar.DayInfo?> =
                List(firstDayOfWeekIdx) { null } +
                        days.map { it } +
                        List(totalSlots - firstDayOfWeekIdx - days.size) { null }
            val weeks = padded.chunked(7)

            // ----------- 适配大尺寸 -----------
            // 屏幕过宽时，限制网格最大宽度并居中；保持等宽 7 列
            val maxGridWidth = 600.dp
            val gridWidth = if (maxWidth > maxGridWidth) maxGridWidth else maxWidth
            val horizontalPad = (maxWidth - gridWidth) / 2

            val scrollEnabled = (needHeight + totalGapHeight + verticalContainerPadding) > maxHeight

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
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(desiredGap),
                    userScrollEnabled = scrollEnabled
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
private fun ShiftChip(shift: com.example.alarm_clock_2.shift.Shift) {
    val accent = shiftAccent(shift)
    val containerColor = accent.copy(alpha = 0.18f)

    Box(
        modifier = Modifier
            .wrapContentWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = shiftLabel(shift),
            color = accent,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YearMonthPickerDialog(currentMonth: java.time.YearMonth, onConfirm: (java.time.YearMonth)->Unit, onDismiss:()->Unit) {
    var yearExpanded by remember { mutableStateOf(false) }
    var monthExpanded by remember { mutableStateOf(false) }

    var year by remember { mutableStateOf(currentMonth.year) }
    var month by remember { mutableStateOf(currentMonth.monthValue) }

    val years = (currentMonth.year-10)..(currentMonth.year+10)
    val months = (1..12)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(java.time.YearMonth.of(year, month)) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        title = { Text("选择年月") },
        text = {
            Column {
                ExposedDropdownMenuBox(expanded = yearExpanded, onExpandedChange = { yearExpanded = !yearExpanded }) {
                    OutlinedTextField(
                        value = "${year}年",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("年份") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = yearExpanded, onDismissRequest = { yearExpanded = false }) {
                        years.forEach { y ->
                            DropdownMenuItem(text = { Text("$y 年") }, onClick = { year = y; yearExpanded = false })
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                ExposedDropdownMenuBox(expanded = monthExpanded, onExpandedChange = { monthExpanded = !monthExpanded }) {
                    OutlinedTextField(
                        value = "${month}月",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("月份") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = monthExpanded, onDismissRequest = { monthExpanded = false }) {
                        months.forEach { m ->
                            DropdownMenuItem(text = { Text("$m 月") }, onClick = { month = m; monthExpanded = false })
                        }
                    }
                }
            }
        }
    )
} 
