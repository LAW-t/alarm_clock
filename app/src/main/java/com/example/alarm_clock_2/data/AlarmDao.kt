package com.example.alarm_clock_2.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {

    // ==================== 查询方法 ====================

    @Query("SELECT * FROM alarm_times ORDER BY time ASC")
    fun getAll(): Flow<List<AlarmTimeEntity>>

    @Query("SELECT * FROM alarm_times WHERE id = :id")
    suspend fun getById(id: Int): AlarmTimeEntity?

    @Query("SELECT * FROM alarm_times WHERE identity = :identity ORDER BY time ASC")
    fun getByIdentity(identity: String): Flow<List<AlarmTimeEntity>>

    @Query("SELECT * FROM alarm_times WHERE shift = :shift ORDER BY time ASC")
    fun getByShift(shift: String): Flow<List<AlarmTimeEntity>>

    @Query("SELECT * FROM alarm_times WHERE enabled = :enabled ORDER BY time ASC")
    fun getEnabled(enabled: Boolean = true): Flow<List<AlarmTimeEntity>>

    @Query("SELECT * FROM alarm_times WHERE identity = :identity AND enabled = :enabled ORDER BY time ASC")
    fun getByIdentityAndEnabled(identity: String, enabled: Boolean): Flow<List<AlarmTimeEntity>>

    @Query("SELECT * FROM alarm_times WHERE identity = :identity AND shift = :shift")
    suspend fun getByIdentityAndShift(identity: String, shift: String): List<AlarmTimeEntity>

    @Query("SELECT COUNT(*) FROM alarm_times WHERE identity = :identity")
    suspend fun countByIdentity(identity: String): Int

    @Query("SELECT COUNT(*) FROM alarm_times WHERE enabled = 1")
    suspend fun countEnabled(): Int

    // ==================== 插入和更新方法 ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AlarmTimeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<AlarmTimeEntity>): List<Long>

    @Update
    suspend fun update(entity: AlarmTimeEntity)

    @Update
    suspend fun updateAll(entities: List<AlarmTimeEntity>)

    // ==================== 删除方法 ====================

    @Delete
    suspend fun delete(entity: AlarmTimeEntity)

    @Query("DELETE FROM alarm_times WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM alarm_times WHERE identity = :identity")
    suspend fun deleteByIdentity(identity: String)

    @Query("DELETE FROM alarm_times WHERE shift = :shift")
    suspend fun deleteByShift(shift: String)

    @Query("DELETE FROM alarm_times WHERE enabled = 0")
    suspend fun deleteDisabled()

    @Query("DELETE FROM alarm_times")
    suspend fun deleteAll()

    // ==================== 批量操作方法 ====================

    @Query("UPDATE alarm_times SET enabled = :enabled WHERE identity = :identity")
    suspend fun updateEnabledByIdentity(identity: String, enabled: Boolean)

    @Query("UPDATE alarm_times SET enabled = :enabled WHERE shift = :shift")
    suspend fun updateEnabledByShift(shift: String, enabled: Boolean)

    @Query("UPDATE alarm_times SET snooze_count = :snoozeCount, snooze_interval = :snoozeInterval WHERE identity = :identity")
    suspend fun updateSnoozeSettingsByIdentity(identity: String, snoozeCount: Int, snoozeInterval: Int)
}