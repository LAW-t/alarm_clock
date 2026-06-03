package com.example.alarm_clock_2.calendar

import com.example.alarm_clock_2.data.HolidayRepository
import com.example.alarm_clock_2.data.SettingsDataStore
import com.example.alarm_clock_2.shift.IdentityType
import com.example.alarm_clock_2.shift.Shift
import com.example.alarm_clock_2.shift.ShiftCalculator
import com.example.alarm_clock_2.shift.ShiftConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarRepository @Inject constructor(
    private val settings: SettingsDataStore,
    private val holidayRepo: HolidayRepository
) {

    private val cache = object : LinkedHashMap<YearMonth, MonthInfo>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<YearMonth, MonthInfo>?): Boolean {
            return size > 36 // keep up to 3 years in memory
        }
    }

    private val scope = CoroutineScope(SupervisorJob())

    @Suppress("UNCHECKED_CAST")
    private val userSettingsFlow: Flow<UserSettings> = combine(
        combine(
            settings.identityFlow,
            settings.holidayRestFlow,
            settings.fourThreeBaseDateFlow,
            settings.fourThreeIndexFlow,
            settings.fourTwoBaseDateFlow
        ) { id, hr, b43, i43, b42 -> listOf(id, hr, b43, i43, b42) },
        combine(
            settings.fourTwoIndexFlow,
            settings.customPatternFlow,
            settings.customIndexFlow,
            settings.customBaseDateFlow
        ) { i42, cp, ci, cb -> listOf(i42, cp, ci, cb) }
    ) { g1, g2 ->
        val identityStr = g1[0] as String
        val holidayRest = g1[1] as Boolean
        val base43DateStr = g1[2] as String
        val idx43 = g1[3] as Int
        val base42DateStr = g1[4] as String
        val idx42 = g2[0] as Int
        val customPat = g2[1] as String
        val customIdx = g2[2] as Int
        val customBaseStr = g2[3] as String

        val identity = runCatching { IdentityType.valueOf(identityStr) }.getOrDefault(IdentityType.LONG_DAY)
        val base43Date = runCatching { LocalDate.parse(base43DateStr) }.getOrElse { LocalDate.now() }
        val base42Date = runCatching { LocalDate.parse(base42DateStr) }.getOrElse { LocalDate.now() }
        val customBaseDate = runCatching { LocalDate.parse(customBaseStr) }.getOrElse { LocalDate.now() }

        UserSettings(identity, holidayRest, base43Date, idx43, base42Date, idx42, customPat, customBaseDate, customIdx)
    }.distinctUntilChanged()

    init {
        userSettingsFlow.onEach { cache.clear() }.launchIn(scope)
        holidayRepo.holidaysFlow.onEach { cache.clear() }.launchIn(scope)
    }

    // ---- public API ----
    fun monthInfoFlow(target: YearMonth): Flow<MonthInfo> {
        val monthKey = target
        return combine(userSettingsFlow, holidayRepo.holidaysFlow) { settingsSnap, holidays ->
            computeMonth(monthKey, settingsSnap, holidays)
        }.flowOn(Dispatchers.Default)
    }

    fun peek(month: YearMonth): MonthInfo? = cache[month]

    // ---- internal helpers ----

    private suspend fun computeMonth(
        month: YearMonth,
        us: UserSettings,
        holidays: List<com.example.alarm_clock_2.data.HolidayDayEntity>
    ): MonthInfo = withContext(Dispatchers.Default) {
        // Fire-and-forget download of holiday data，不阻塞首帧
        scope.launch {
            runCatching {
                holidayRepo.ensureYear(month.year)
                holidayRepo.ensureYear(month.year + 1)
            }
        }

        val holidayMap = holidays.associateBy { it.date }

        val total = month.lengthOfMonth()
        val first = month.atDay(1)

        val config = buildShiftConfig(us)

        val days = (0 until total).map { offset ->
            val date = first.plusDays(offset.toLong())
            var shift = ShiftCalculator.calculate(date, config)

            val lunar = com.nlf.calendar.Solar.fromYmd(date.year, date.monthValue, date.dayOfMonth).lunar
            val jieQi = lunar.jieQi
            val lunarStr = if (jieQi.isNotEmpty()) jieQi else lunar.dayInChinese

            val hd = holidayMap[date.toString()]
            val holiday = hd?.let { Holiday(it.name, it.isOffDay) }
            val isOff = hd?.isOffDay ?: false
            val holidayRestActive = us.holidayRest && us.identity == IdentityType.LONG_DAY
            val workdayOverrideApplied = holidayRestActive && hd != null && !isOff

            // Holiday overrides (only for long-day identity)
            if (holidayRestActive && isOff) {
                shift = Shift.OFF
            } else if (workdayOverrideApplied) {
                // Adjusted working day (make-up work on weekend) treated as DAY for long-day
                shift = Shift.DAY
            }

            DayInfo(date, shift, lunarStr, holiday, isOff, workdayOverrideApplied)
        }

        val result = MonthInfo(month, days)
        cache[month] = result

        // 预计算整年（月度缓存暖场）
        scope.launch {
            (1..12).map { YearMonth.of(month.year, it) }.filter { it !in cache }.forEach { m ->
                val mi = computeMonthSync(m, us, holidays)
                cache[m] = mi
            }
        }
        result
    }

    // 同步版供预计算使用（不递归预取）
    private fun computeMonthSync(
        month: YearMonth,
        us: UserSettings,
        holidays: List<com.example.alarm_clock_2.data.HolidayDayEntity>
    ): MonthInfo {
        val holidayMap = holidays.associateBy { it.date }

        val total = month.lengthOfMonth()
        val first = month.atDay(1)

        val config = buildShiftConfig(us)

        val days = (0 until total).map { offset ->
            val date = first.plusDays(offset.toLong())
            var shift = ShiftCalculator.calculate(date, config)

            val lunar = com.nlf.calendar.Solar.fromYmd(date.year, date.monthValue, date.dayOfMonth).lunar
            val jieQi = lunar.jieQi
            val lunarStr = if (jieQi.isNotEmpty()) jieQi else lunar.dayInChinese

            val hd = holidayMap[date.toString()]
            val holiday = hd?.let { Holiday(it.name, it.isOffDay) }
            val isOff = hd?.isOffDay ?: false
            val holidayRestActive = us.holidayRest && us.identity == IdentityType.LONG_DAY
            val workdayOverrideApplied = holidayRestActive && hd != null && !isOff

            if (holidayRestActive && isOff) {
                shift = Shift.OFF
            } else if (workdayOverrideApplied) {
                shift = Shift.DAY
            }

            DayInfo(date, shift, lunarStr, holiday, isOff, workdayOverrideApplied)
        }

        return MonthInfo(month, days)
    }

    private fun buildShiftConfig(us: UserSettings): ShiftConfig {
        return when (us.identity) {
            IdentityType.FOUR_THREE -> ShiftConfig(us.identity, us.baseDate43, us.baseIndex43)
            IdentityType.FOUR_TWO -> ShiftConfig(us.identity, us.baseDate42, us.baseIndex42)
            IdentityType.CUSTOM -> ShiftConfig(us.identity, us.baseDateCustom, us.baseIndexCustom, us.customPattern)
            else -> ShiftConfig(us.identity, LocalDate.now(), 0)
        }
    }
} 
