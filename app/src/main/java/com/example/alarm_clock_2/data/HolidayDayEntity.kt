package com.example.alarm_clock_2.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "holiday_days")
data class HolidayDayEntity(
    @PrimaryKey val date: String, // ISO yyyy-MM-dd
    val name: String,
    val isOffDay: Boolean
) 