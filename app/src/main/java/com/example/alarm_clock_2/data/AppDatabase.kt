package com.example.alarm_clock_2.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [AlarmTimeEntity::class, HolidayDayEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun holidayDao(): HolidayDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE alarm_times ADD COLUMN display_name TEXT")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE alarm_times ADD COLUMN snooze_count INTEGER NOT NULL DEFAULT 3")
                database.execSQL("ALTER TABLE alarm_times ADD COLUMN snooze_interval INTEGER NOT NULL DEFAULT 5")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE alarm_times ADD COLUMN identity TEXT NOT NULL DEFAULT 'LONG_DAY'")
            }
        }
    }
}