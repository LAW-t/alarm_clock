package com.example.alarm_clock_2.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm_clock_2.calendar.CalendarRepository
import com.example.alarm_clock_2.calendar.DayInfo
import com.example.alarm_clock_2.calendar.MonthInfo
import com.example.alarm_clock_2.calendar.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repo: CalendarRepository
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())

    data class CalendarUiState(
        val currentMonth: YearMonth,
        val monthInfo: MonthInfo,
        val loading: Boolean = false
    )

    val uiState: StateFlow<CalendarUiState> = _currentMonth
        .flatMapLatest { month ->
            repo.monthInfoFlow(month)
                .map { mi -> CalendarUiState(month, mi, loading = false) }
                .onStart {
                    val cached = repo.peek(month)
                    emit(
                        CalendarUiState(
                            currentMonth = month,
                            monthInfo = cached ?: MonthInfo(month, emptyList()),
                            loading = cached == null
                        )
                    )
        }
    }
        .stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
            CalendarUiState(YearMonth.now(), MonthInfo(YearMonth.now(), emptyList()), loading = true)
        )

    fun nextMonth() { _currentMonth.value = _currentMonth.value.plusMonths(1) }
    fun prevMonth() { _currentMonth.value = _currentMonth.value.minusMonths(1) }

    // --- helper methods for legacy UI ---
    fun peekMonthDays(month: YearMonth): List<DayInfo> = repo.peek(month)?.days ?: emptyList()
    suspend fun getMonthDays(month: YearMonth): List<DayInfo> = repo.monthInfoFlow(month).first().days

    fun monthInfoFlow(month: YearMonth): Flow<MonthInfo> = repo.monthInfoFlow(month)
} 