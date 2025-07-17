package com.example.alarm_clock_2.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HolidayDao {
    @Query("SELECT * FROM holiday_days WHERE date = :iso")
    suspend fun getByDate(iso: String): HolidayDayEntity?

    @Query("SELECT * FROM holiday_days")
    fun getAllFlow(): Flow<List<HolidayDayEntity>>

    @Query("SELECT COUNT(*) FROM holiday_days WHERE CAST(substr(date,1,4) AS INT) = :year")
    suspend fun countYear(year: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<HolidayDayEntity>)

    @Query("DELETE FROM holiday_days WHERE CAST(substr(date,1,4) AS INT) = :year")
    suspend fun deleteYear(year: Int)
} 