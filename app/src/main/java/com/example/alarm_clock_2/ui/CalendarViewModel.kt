package com.example.alarm_clock_2.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm_clock_2.data.SettingsDataStore
import com.example.alarm_clock_2.shift.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val holidayRepository: com.example.alarm_clock_2.data.HolidayRepository
) : ViewModel() {

    init {
        // 首次进入应用时，如果节假日数据尚未加载，则触发同步并在期间显示加载弹窗
        viewModelScope.launch {
            val holidaysLoaded = settingsDataStore.holidayLoadedFlow.first()
            val nowYear = LocalDate.now().year
            val yearRange = (nowYear - 10)..(nowYear + 10)

            if (!holidaysLoaded) {
                // 首次安装：后台逐年下载，完成后写入标记
                yearRange.forEach { y ->
                    runCatching { holidayRepository.syncYear(y) }
                }
                settingsDataStore.setHolidayLoaded(true)
            } else {
                // 后续启动：确保内存标记即可，无网络消耗
                yearRange.forEach { y -> holidayRepository.ensureYear(y) }
            }
        }
    }

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth

    // 首次加载节假日改为后台异步，无需 UI 显示加载对话框

    /** In-memory cache to avoid recomputing days for months already visited during current app session */
    private val monthCache = mutableMapOf<YearMonth, List<DayInfo>>()

    data class DayInfo(
        val date: LocalDate,
        val shift: Shift,
        val lunarDay: String,
        val holidayName: String?,
        val isOffDay: Boolean
    )

    private data class SettingBundle(
        val identityStr: String,
        val holidayRest: Boolean,
        val idx43: Int,
        val base43: String,
        val idx42: Int,
        val base42: String
    )

    // Kotlin Flow combine 对 6 个流使用 vararg 版本，其 transform 只有一个 Array 参数
    private val settingBundleFlow: Flow<SettingBundle> = combine(
        settingsDataStore.identityFlow,
        settingsDataStore.holidayRestFlow,
        settingsDataStore.fourThreeIndexFlow,
        settingsDataStore.fourThreeBaseDateFlow,
        settingsDataStore.fourTwoIndexFlow,
        settingsDataStore.fourTwoBaseDateFlow
    ) { values ->
        // 参数顺序与传入流顺序一致
        val identityStr = values[0] as String
        val holidayRest = values[1] as Boolean
        val idx43 = values[2] as Int
        val base43 = values[3] as String
        val idx42 = values[4] as Int
        val base42 = values[5] as String
        SettingBundle(identityStr, holidayRest, idx43, base43, idx42, base42)
    }

    /** Emits an incrementing integer each time holiday table changes, used as recomposition key */
    val holidayVersion: StateFlow<Int> = holidayRepository.holidaysFlow
        .map { it.hashCode() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private fun identityStrToEnum(value: String): IdentityType =
        runCatching { IdentityType.valueOf(value) }.getOrDefault(IdentityType.LONG_DAY)

    private fun generateMonthDays(
        month: YearMonth,
        identity: IdentityType,
        holidayRest: Boolean,
        baseDate: LocalDate,
        baseIndex: Int,
        holidayMap: Map<String, com.example.alarm_clock_2.data.HolidayDayEntity>
    ): List<DayInfo> {
        val first = month.atDay(1)
        val total = month.lengthOfMonth()
        val config = ShiftConfig(identity, baseDate, baseIndex)

        return (0 until total).map { offset ->
            val date = first.plusDays(offset.toLong())
            var shift = ShiftCalculator.calculate(date, config)

            val lunar = com.nlf.calendar.Solar.fromYmd(date.year, date.monthValue, date.dayOfMonth).lunar
            val lunarDayStr = lunar.dayInChinese

            val holidayEntity = holidayMap[date.toString()]
            val holidayName = holidayEntity?.name
            val isOffDay = holidayEntity?.isOffDay ?: false

            if (holidayRest && isOffDay) {
                shift = Shift.OFF
            }

            DayInfo(date, shift, lunarDayStr, holidayName, isOffDay)
        }
    }

    private val daysInternal: Flow<List<DayInfo>> = combine(
        _currentMonth,
        settingBundleFlow,
        holidayRepository.holidaysFlow
    ) { month, settings, holidays ->
        val identity = identityStrToEnum(settings.identityStr)
        val (baseIndex, baseDateStr) = when (identity) {
            IdentityType.FOUR_THREE -> settings.idx43 to settings.base43
            IdentityType.FOUR_TWO -> settings.idx42 to settings.base42
            else -> 0 to LocalDate.now().toString()
        }

        val baseDate = runCatching { LocalDate.parse(baseDateStr) }.getOrElse { LocalDate.now() }

        val holidayMap = holidays.associateBy { it.date }

        val first = month.atDay(1)
        val total = month.lengthOfMonth()
        val config = ShiftConfig(identity, baseDate, baseIndex)

        (0 until total).map { offset ->
            val date = first.plusDays(offset.toLong())
            var shift = ShiftCalculator.calculate(date, config)

            val lunar = com.nlf.calendar.Solar.fromYmd(date.year, date.monthValue, date.dayOfMonth).lunar
            val lunarDayStr = lunar.dayInChinese

            val holidayEntity = holidayMap[date.toString()]
            val holidayName = holidayEntity?.name
            val isOffDay = holidayEntity?.isOffDay ?: false

            if (settings.holidayRest && isOffDay) {
                shift = Shift.OFF
            }

            DayInfo(date, shift, lunarDayStr, holidayName, isOffDay)
        }
    }

    val days: StateFlow<List<DayInfo>> = daysInternal.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        emptyList()
    )

    /** Return cached calendar if available without suspension; empty list otherwise */
    fun peekMonthDays(month: YearMonth): List<DayInfo> = monthCache[month] ?: emptyList()

    /** Public helper */
    suspend fun getMonthDays(month: YearMonth): List<DayInfo> {
        monthCache[month]?.let { return it }

        val identityStr = settingsDataStore.identityFlow.first()
        val identity = identityStrToEnum(identityStr)
        val holidayRest = settingsDataStore.holidayRestFlow.first()

        val idx43 = settingsDataStore.fourThreeIndexFlow.first()
        val idx42 = settingsDataStore.fourTwoIndexFlow.first()
        val baseDateStr = when (identity) {
            IdentityType.FOUR_THREE -> settingsDataStore.fourThreeBaseDateFlow.first()
            IdentityType.FOUR_TWO -> settingsDataStore.fourTwoBaseDateFlow.first()
            else -> LocalDate.now().toString()
        }

        val baseDate = runCatching { LocalDate.parse(baseDateStr) }.getOrElse { LocalDate.now() }

        val baseIndex = when (identity) {
            IdentityType.FOUR_THREE -> idx43
            IdentityType.FOUR_TWO -> idx42
            else -> 0
        }

        // 确保节假日数据就绪（若用户跳到未来/过去年份）
        // 异步触发节假日下载，不阻塞班次计算
        viewModelScope.launch {
            runCatching {
                holidayRepository.ensureYear(month.year)
                holidayRepository.ensureYear(month.year + 1)
            }
        }

        // 在后台线程计算，避免阻塞主线程
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            computeMonthInternal(month, identity, holidayRest, baseDate, baseIndex)
        }.also { result ->
            monthCache[month] = result
        }
    }

    private suspend fun computeMonthInternal(
        month: YearMonth,
        identity: IdentityType,
        holidayRest: Boolean,
        baseDate: LocalDate,
        baseIndex: Int
    ): List<DayInfo> {
        val holidayMap = holidayRepository.holidaysFlow.first().associateBy { it.date }
        return generateMonthDays(month, identity, holidayRest, baseDate, baseIndex, holidayMap)
    }

    fun prevMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }
} 