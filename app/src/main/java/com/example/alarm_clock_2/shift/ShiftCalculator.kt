package com.example.alarm_clock_2.shift

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 支持的身份类型
 */
enum class IdentityType {
    /** 长白班（周一~周五上班、周末休） */
    LONG_DAY,

    /** 四班三运转（休-早-中-晚，每个班次两天，共 8 天周期） */
    FOUR_THREE,

    /** 四班两运转（早-晚-休-休，4 天周期） */
    FOUR_TWO,

    /** 自定义排班（用户自行定义周期和班次类型） */
    CUSTOM
}

/**
 * 班次枚举
 */
enum class Shift {
    /** 长白班白班/早班 */
    DAY,

    /** 四班制早班 */
    MORNING,

    /** 四班三运转中班 */
    AFTERNOON,

    /** 晚班 */
    NIGHT,

    /** 休息 */
    OFF
}

/**
 * 计算所需配置。
 * @param identity 身份类型
 * @param baseDate 参考日期（index = [baseShiftIndex] 对应此日期）
 * @param baseShiftIndex 周期起始索引，0 表示周期数组第 0 位
 */
data class ShiftConfig(
    val identity: IdentityType,
    val baseDate: LocalDate = LocalDate.now(),
    val baseShiftIndex: Int = 0,
    /** 自定义班次模式，逗号分隔的 Shift 枚举名，如 "MORNING,NIGHT,OFF,OFF" */
    val customPattern: String = ""
)

object ShiftCalculator {

    // 顺序：休、休、晚、晚、早、早、中、中
    private val patternFourThree = listOf(
        Shift.OFF, Shift.OFF,
        Shift.NIGHT, Shift.NIGHT,
        Shift.MORNING, Shift.MORNING,
        Shift.AFTERNOON, Shift.AFTERNOON
    )

    private val patternFourTwo = listOf(
        Shift.MORNING,
        Shift.NIGHT,
        Shift.OFF,
        Shift.OFF
    )

    /**
     * 计算指定日期的班次
     */
    fun calculate(date: LocalDate, config: ShiftConfig): Shift = when (config.identity) {
        IdentityType.LONG_DAY -> {
            val dow = date.dayOfWeek
            if (dow.value in 1..5) Shift.DAY else Shift.OFF
        }

        IdentityType.FOUR_THREE -> {
            val days = ChronoUnit.DAYS.between(config.baseDate, date).toInt()
            val idx = positiveMod(config.baseShiftIndex + days, patternFourThree.size)
            patternFourThree[idx]
        }

        IdentityType.FOUR_TWO -> {
            val days = ChronoUnit.DAYS.between(config.baseDate, date).toInt()
            val idx = positiveMod(config.baseShiftIndex + days, patternFourTwo.size)
            patternFourTwo[idx]
        }

        IdentityType.CUSTOM -> {
            val pattern = parseCustomPattern(config.customPattern)
            if (pattern.isEmpty()) Shift.OFF
            else {
                val days = ChronoUnit.DAYS.between(config.baseDate, date).toInt()
                val idx = positiveMod(config.baseShiftIndex + days, pattern.size)
                pattern[idx]
            }
        }
    }

    /** 解析自定义班次字符串为 Shift 列表 */
    fun parseCustomPattern(raw: String): List<Shift> {
        if (raw.isBlank()) return emptyList()
        return raw.split(",")
            .mapNotNull { name ->
                runCatching { Shift.valueOf(name.trim()) }.getOrNull()
            }
    }

    /** 计算周期天数 */
    fun customCycleDays(raw: String): Int = parseCustomPattern(raw).size

    private fun positiveMod(value: Int, mod: Int): Int {
        val r = value % mod
        return if (r < 0) r + mod else r
    }
} 