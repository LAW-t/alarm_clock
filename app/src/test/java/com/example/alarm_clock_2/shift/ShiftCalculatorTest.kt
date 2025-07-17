package com.example.alarm_clock_2.shift

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ShiftCalculatorTest {
    @Test
    fun `long day weekdays and weekend`() {
        val config = ShiftConfig(identity = IdentityType.LONG_DAY)
        val monday = LocalDate.of(2024, 6, 10) // Monday
        val sunday = LocalDate.of(2024, 6, 9) // Sunday
        assertEquals(Shift.DAY, ShiftCalculator.calculate(monday, config))
        assertEquals(Shift.OFF, ShiftCalculator.calculate(sunday, config))
    }

    @Test
    fun `four three pattern cycle`() {
        val baseDate = LocalDate.of(2024, 6, 1) // index 0 -> OFF (休)
        val config = ShiftConfig(IdentityType.FOUR_THREE, baseDate, 0)
        assertEquals(Shift.OFF, ShiftCalculator.calculate(baseDate, config))
        assertEquals(Shift.OFF, ShiftCalculator.calculate(baseDate.plusDays(1), config))
        assertEquals(Shift.MORNING, ShiftCalculator.calculate(baseDate.plusDays(2), config))
        assertEquals(Shift.MORNING, ShiftCalculator.calculate(baseDate.plusDays(3), config))
        assertEquals(Shift.AFTERNOON, ShiftCalculator.calculate(baseDate.plusDays(4), config))
        assertEquals(Shift.NIGHT, ShiftCalculator.calculate(baseDate.plusDays(6), config))
    }

    @Test
    fun `four two pattern cycle`() {
        val baseDate = LocalDate.of(2024, 6, 1) // index 0 -> MORNING
        val config = ShiftConfig(IdentityType.FOUR_TWO, baseDate, 0)
        assertEquals(Shift.MORNING, ShiftCalculator.calculate(baseDate, config))
        assertEquals(Shift.NIGHT, ShiftCalculator.calculate(baseDate.plusDays(1), config))
        assertEquals(Shift.OFF, ShiftCalculator.calculate(baseDate.plusDays(2), config))
        assertEquals(Shift.OFF, ShiftCalculator.calculate(baseDate.plusDays(3), config))
        // cycle repeats
        assertEquals(Shift.MORNING, ShiftCalculator.calculate(baseDate.plusDays(4), config))
    }
} 