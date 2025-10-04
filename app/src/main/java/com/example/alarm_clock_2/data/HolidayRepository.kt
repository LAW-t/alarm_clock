package com.example.alarm_clock_2.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HolidayRepository @Inject constructor(private val dao: HolidayDao) {

    /** 已经同步过的年份集合，避免重复网络请求 */
    private val syncedYears = mutableSetOf<Int>()

    suspend fun isOffDay(isoDate: String): Boolean = dao.getByDate(isoDate)?.isOffDay ?: false

    /**
     * Returns true if the date is an adjusted working day (has holiday record and isOffDay == false).
     * Normal weekdays without a holiday record return false.
     */
    suspend fun isWorkdayOverride(isoDate: String): Boolean {
        val hd = dao.getByDate(isoDate)
        return hd != null && !hd.isOffDay
    }

    /**
     * Return [HolidayDayEntity] for the given ISO date string (yyyy-MM-dd) or null if not found.
     */
    suspend fun getHoliday(isoDate: String): HolidayDayEntity? = dao.getByDate(isoDate)

    /** Flow emitting complete holiday list, used for UI reactive updates */
    val holidaysFlow: kotlinx.coroutines.flow.Flow<List<HolidayDayEntity>> get() = dao.getAllFlow()

    /**
     * 确保指定年份的节假日数据已在本地数据库。
     * 如未同步则尝试下载，失败时不会抛出致命异常。
     */
    suspend fun ensureYear(year: Int) {
        if (syncedYears.contains(year)) return

        // 若数据库已存在该年份数据则直接标记为已同步
        val localCount = dao.countYear(year)
        if (localCount > 0) {
            syncedYears.add(year)
            return
        }

        runCatching { syncYear(year) }.onSuccess { syncedYears.add(year) }
    }

    suspend fun syncYear(year: Int) {
        val json = Json { ignoreUnknownKeys = true }

        // 官方 raw + 两个 jsDelivr CDN 作为备份
        val urls = listOf(
            "https://raw.githubusercontent.com/NateScarlet/holiday-cn/master/${year}.json",
            "https://cdn.jsdelivr.net/gh/NateScarlet/holiday-cn@master/${year}.json",
            "https://fastly.jsdelivr.net/gh/NateScarlet/holiday-cn@master/${year}.json"
        )

        var dto: HolidaysDto? = null
        var lastError: Exception? = null
        for (u in urls) {
            val jsonStr = runCatching {
                withContext(Dispatchers.IO) { URL(u).readText() }
            }.getOrElse { e ->
                lastError = e as? Exception ?: IOException(e.message)
                null
            } ?: continue

            dto = runCatching { json.decodeFromString<HolidaysDto>(jsonStr) }.getOrElse { e ->
                lastError = e as? Exception ?: IOException(e.message)
                null
            }
            if (dto != null) break
        }

        if (dto == null) {
            throw lastError ?: IOException("Failed to download holiday file for $year")
        }

        val entities = dto.days.map { HolidayDayEntity(it.date, it.name, it.isOffDay) }
        dao.deleteYear(year)
        dao.upsertAll(entities)
    }
}

@Serializable
data class HolidaysDto(val year: Int, val papers: List<String> = emptyList(), val days: List<DayDto>)

@Serializable
data class DayDto(val name: String, val date: String, val isOffDay: Boolean) 
