package com.example.alarm_clock_2.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarm_times")
    fun getAll(): Flow<List<AlarmTimeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AlarmTimeEntity)

    @Delete
    suspend fun delete(entity: AlarmTimeEntity)
} 