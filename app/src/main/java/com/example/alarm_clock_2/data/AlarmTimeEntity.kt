package com.example.alarm_clock_2.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarm_times")
data class AlarmTimeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "shift") val shift: String,
    @ColumnInfo(name = "time") val time: String,
    @ColumnInfo(name = "enabled") val enabled: Boolean = true
) 