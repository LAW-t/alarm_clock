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

    private val userSettingsFlow: Flow<UserSettings> = combine(
        settings.identityFlow,
        settings.holidayRestFlow,
        settings.fourThreeBaseDateFlow,
        settings.fourThreeIndexFlow,
        settings.fourTwoBaseDateFlow,
        settings.fourTwoIndexFlow
    ) { arr ->
        val identityStr = arr[0] as String
        val holidayRest = arr[1] as Boolean
        val base43DateStr = arr[2] as String
        val idx43 = arr[3] as Int
        val base42DateStr = arr[4] as String
        val idx42 = arr[5] as Int

        val identity = runCatching { IdentityType.valueOf(identityStr) }.getOrDefault(IdentityType.LONG_DAY)
        val base43Date = runCatching { LocalDate.parse(base43DateStr) }.getOrElse { LocalDate.now() }
        val base42Date = runCatching { LocalDate.parse(base42DateStr) }.getOrElse { LocalDate.now() }

        UserSettings(identity, holidayRest, base43Date, idx43, base42Date, idx42)
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
        // Check cache first (if settings or holidays haven't changed the entry might still be valid)
        cache[month]?.let { return@withContext it }

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

        val baseDate: LocalDate
        val baseIdx: Int
        when (us.identity) {
            IdentityType.FOUR_THREE -> {
                baseDate = us.baseDate43
                baseIdx = us.baseIndex43
            }
            IdentityType.FOUR_TWO -> {
                baseDate = us.baseDate42
                baseIdx = us.baseIndex42
            }
            else -> {
                baseDate = LocalDate.now()
                baseIdx = 0
            }
        }

        val config = ShiftConfig(us.identity, baseDate, baseIdx)

        val days = (0 until total).map { offset ->
            val date = first.plusDays(offset.toLong())
            var shift = ShiftCalculator.calculate(date, config)

            val lunar = com.nlf.calendar.Solar.fromYmd(date.year, date.monthValue, date.dayOfMonth).lunar
            val lunarStr = lunar.dayInChinese

            val hd = holidayMap[date.toString()]
            val holiday = hd?.let { Holiday(it.name, it.isOffDay) }
            val isOff = hd?.isOffDay ?: false

            if (us.holidayRest && isOff) {
                shift = Shift.OFF
            }

            DayInfo(date, shift, lunarStr, holiday, isOff)
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

        val (baseDate, baseIdx) = when (us.identity) {
            IdentityType.FOUR_THREE -> us.baseDate43 to us.baseIndex43
            IdentityType.FOUR_TWO -> us.baseDate42 to us.baseIndex42
            else -> LocalDate.now() to 0
        }

        val config = ShiftConfig(us.identity, baseDate, baseIdx)

        val days = (0 until total).map { offset ->
            val date = first.plusDays(offset.toLong())
            var shift = ShiftCalculator.calculate(date, config)

            val lunar = com.nlf.calendar.Solar.fromYmd(date.year, date.monthValue, date.dayOfMonth).lunar
            val lunarStr = lunar.dayInChinese

            val hd = holidayMap[date.toString()]
            val holiday = hd?.let { Holiday(it.name, it.isOffDay) }
            val isOff = hd?.isOffDay ?: false

            if (us.holidayRest && isOff) shift = Shift.OFF

            DayInfo(date, shift, lunarStr, holiday, isOff)
        }

        return MonthInfo(month, days)
    }
} 