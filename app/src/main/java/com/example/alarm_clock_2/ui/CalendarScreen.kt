@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.alarm_clock_2.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel = hiltViewModel()) {
    // 首次安装时节假日数据在后台加载，去掉阻塞式 Dialog

    val baseMonth = remember { java.time.YearMonth.now() }
    val totalPages = 240 // ±10 years window
    val initialPage = totalPages / 2
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { totalPages })
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    // 当前 pager 页面对应的 YearMonth
    val currentMonth = baseMonth.plusMonths((pagerState.currentPage - initialPage).toLong())

    // YearMonth Picker 弹窗
    var showYmPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp, start = 8.dp, end = 8.dp),
    ) {
        // ----- Header -----
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "<", modifier = Modifier.clickable {
                scope.launch { pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0)) }
            })

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${currentMonth.year}年${currentMonth.monthValue}月",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.clickable { showYmPicker = true }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "今", style = MaterialTheme.typography.bodyMedium, modifier = Modifier
                    .clickable {
                        scope.launch { pagerState.animateScrollToPage(initialPage) }
                    })
            }

            Text(text = ">", modifier = Modifier.clickable {
                scope.launch { pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(totalPages - 1)) }
            })
        }

        Spacer(Modifier.height(8.dp))

        // ----- Day of week labels -----
        // 宽度与下方日期网格保持一致
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val maxGridWidth = 600.dp
            val gridWidth = if (maxWidth > maxGridWidth) maxGridWidth else maxWidth
            val horizontalPad = (maxWidth - gridWidth) / 2

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = horizontalPad, end = horizontalPad)
            ) {
                DayOfWeek.values().forEach {
                    Text(
                        text = it.getDisplayName(TextStyle.SHORT, Locale.CHINESE),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // ----- Pager with Calendar grids -----
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            val month = baseMonth.plusMonths((page - initialPage).toLong())
            MonthGrid(month = month, viewModel = viewModel)
        }

        // ----- Legend -----
        ShiftLegendRow()
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        com.example.alarm_clock_2.shift.Shift.values().forEach { shift ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(color = shiftColor(shift), shape = androidx.compose.foundation.shape.CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = shiftLabel(shift), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

// Extracted color logic for reusability
private fun shiftColor(shift: com.example.alarm_clock_2.shift.Shift): Color = when (shift) {
    com.example.alarm_clock_2.shift.Shift.MORNING -> Color(0xFF3B82F6) // blue-500
    com.example.alarm_clock_2.shift.Shift.AFTERNOON -> Color(0xFF059669) // emerald-600
    com.example.alarm_clock_2.shift.Shift.NIGHT -> Color(0xFFF59E0B) // amber-500
    com.example.alarm_clock_2.shift.Shift.DAY -> Color(0xFF4F46E5) // indigo-600
    com.example.alarm_clock_2.shift.Shift.OFF -> Color(0xFF94A3B8) // slate-400
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
    if(day==null){ Spacer(modifier = Modifier.height(cellHeight)) ; return }
    val context = androidx.compose.ui.platform.LocalContext.current
    val today = java.time.LocalDate.now()
    val isToday = day.date == today

    val clickModifier = remember(day.date) {
        Modifier.combinedClickable(onClick = {
            day.holiday?.name?.let { name ->
                android.widget.Toast.makeText(context, name, android.widget.Toast.LENGTH_SHORT).show()
            }
        })
    }

    Column(
        modifier = Modifier
            .fillMaxWidth() // 撑满列宽，解决列间隙和对齐问题
            // 使用最小高度，但允许内容撑开，避免 Chip 被裁剪
            .heightIn(min = cellHeight)
            .padding(vertical = 2.dp)   // 仅垂直内边距，避免列之间产生可见空隙
            // 不再高亮节假日
            .then(clickModifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Date number with unified circle wrapper
        val fontScale = LocalConfiguration.current.fontScale
        val circleSize = (56.dp * fontScale).coerceIn(14.dp, 26.dp)

        Box(
            modifier = Modifier
                .size(circleSize)
                .then(
                    if (isToday) Modifier.border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.error,
                        shape = androidx.compose.foundation.shape.CircleShape
                    ) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = if (isToday) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                color = if (isToday) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )

            // Holiday dot indicator (top-right)
            if (day.isOffDay) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(6.dp)
                        .background(MaterialTheme.colorScheme.error, shape = androidx.compose.foundation.shape.CircleShape)
                )
            }
        }

        // 数字与农历/节假日文本之间的间距（稍微缩小）
        Spacer(modifier = Modifier.height(1.dp))

        // Lunar day text (replace with first 2 chars of holiday name if present)
        val lunarText = day.holiday?.name?.take(2) ?: day.lunarDay
        Text(
            text = lunarText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 农历/节假日文本与班次标签之间的间距
        Spacer(modifier = Modifier.height(2.dp))

        // Shift label chip
        ShiftChip(shift = day.shift)
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

            // 2. 用剩余空间计算单元格高度
            val heightForCells = maxHeight - totalGapHeight
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

            val scrollEnabled = (needHeight + totalGapHeight) > maxHeight

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = horizontalPad, end = horizontalPad),
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

@Composable
private fun ShiftChip(shift: com.example.alarm_clock_2.shift.Shift) {
    // Color palette inspired by index.html tags
    val bgColor = when (shift) {
        com.example.alarm_clock_2.shift.Shift.MORNING -> Color(0xFF3B82F6) // blue-500
        com.example.alarm_clock_2.shift.Shift.AFTERNOON -> Color(0xFF059669) // emerald-600
        com.example.alarm_clock_2.shift.Shift.NIGHT -> Color(0xFFF59E0B) // amber-500
        com.example.alarm_clock_2.shift.Shift.DAY -> Color(0xFF4F46E5) // indigo-600
        com.example.alarm_clock_2.shift.Shift.OFF -> Color(0xFF94A3B8) // slate-400
    }
    val textColor = Color.White

    Box(
        modifier = Modifier
            .wrapContentWidth()
            .background(color = bgColor, shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = shiftLabel(shift),
            color = textColor,
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