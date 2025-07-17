package com.example.alarm_clock_2.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [AlarmTimeEntity::class, HolidayDayEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun holidayDao(): HolidayDao
} 