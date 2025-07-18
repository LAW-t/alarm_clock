package com.example.alarm_clock_2.calendar

import com.example.alarm_clock_2.shift.IdentityType
import com.example.alarm_clock_2.shift.Shift
import java.time.LocalDate
import java.time.YearMonth

/**
 * 节假日数据（来自 HolidayRepository）
 */
data class Holiday(
    val name: String,
    val isOffDay: Boolean
)

/** 单日信息，用于 UI 渲染 */
data class DayInfo(
    val date: LocalDate,
    val shift: Shift,
    val lunarDay: String,
    val holiday: Holiday?,
    val isOffDay: Boolean
)

/** 整月信息 */
data class MonthInfo(
    val month: YearMonth,
    val days: List<DayInfo>
)

/** 用户排班相关设置快照 */
data class UserSettings(
    val identity: IdentityType,
    val holidayRest: Boolean,
    val baseDate43: LocalDate,
    val baseIndex43: Int,
    val baseDate42: LocalDate,
    val baseIndex42: Int
) 