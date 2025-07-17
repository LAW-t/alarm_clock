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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel = hiltViewModel()) {
    val isLoading by viewModel.isLoading.collectAsState()

    val baseMonth = remember { java.time.YearMonth.now() }
    val totalPages = 240 // ±10 years window
    val initialPage = totalPages / 2
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { totalPages })
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    // 当前 pager 页面对应的 YearMonth
    val currentMonth = baseMonth.plusMonths((pagerState.currentPage - initialPage).toLong())

    // DatePicker 弹窗控制
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

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

            Text(
                text = "${currentMonth.year}年${currentMonth.monthValue}月",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.clickable { showDatePicker = true }
            )

            Text(text = ">", modifier = Modifier.clickable {
                scope.launch { pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(totalPages - 1)) }
            })
        }

        Spacer(Modifier.height(8.dp))

        // ----- Day of week labels -----
        Row(modifier = Modifier.fillMaxWidth()) {
            DayOfWeek.values().forEach {
                Text(
                    text = it.getDisplayName(TextStyle.SHORT, Locale.CHINESE),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall
                )
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

    // ----- DatePickerDialog -----
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val selectedDate = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        val targetMonth = java.time.YearMonth.from(selectedDate)
                        val diff = ChronoUnit.MONTHS.between(baseMonth, targetMonth).toInt()
                        val targetPage = (initialPage + diff).coerceIn(0, totalPages - 1)
                        scope.launch {
                            pagerState.animateScrollToPage(targetPage)
                        }
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // ----- First-time loading dialog -----
    if (isLoading) {
        Dialog(onDismissRequest = {}) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .background(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium)
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "正在加载节假日数据…")
            }
        }
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
private fun DayCell(day: CalendarViewModel.DayInfo, cellHeight: androidx.compose.ui.unit.Dp) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val today = java.time.LocalDate.now()
    val isToday = day.date == today

    Column(
        modifier = Modifier
            .height(cellHeight)
            .padding(4.dp)
            // 不再高亮节假日
            .combinedClickable(onClick = {
                day.holidayName?.let { name ->
                    android.widget.Toast.makeText(context, name, android.widget.Toast.LENGTH_SHORT).show()
                }
            }),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Date number with unified circle wrapper
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                    shape = androidx.compose.foundation.shape.CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = if (isToday) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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

        // 数字与农历/节假日文本之间的间距
        Spacer(modifier = Modifier.height(4.dp))

        // Lunar day text (replace with first 2 chars of holiday name if present)
        val lunarText = day.holidayName?.take(2) ?: day.lunarDay
        Text(
            text = lunarText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 农历/节假日文本与班次标签之间的间距
        Spacer(modifier = Modifier.height(4.dp))

        // Shift label chip
        ShiftChip(shift = day.shift)
    }
}

// ------- 月份网格 -------
@Composable
private fun MonthGrid(month: java.time.YearMonth, viewModel: CalendarViewModel = hiltViewModel()) {
    // Trigger recomposition when holiday table updated
    val holidayVer by viewModel.holidayVersion.collectAsState()

    val days by produceState<List<CalendarViewModel.DayInfo>>(initialValue = emptyList(), month, holidayVer) {
        value = viewModel.getMonthDays(month)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
    ) {
        val cellHeight = maxHeight / 6
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(4.dp)
        ) {
            items(days) { day ->
                DayCell(day = day, cellHeight = cellHeight)
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
            .fillMaxWidth(0.9f)
            .background(color = bgColor, shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = shiftLabel(shift),
            color = textColor,
            style = MaterialTheme.typography.labelMedium
        )
    }
} 