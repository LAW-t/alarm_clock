package com.example.alarm_clock_2.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmRepository @Inject constructor(
    private val dao: AlarmDao
) {
    fun getAlarms(): Flow<List<AlarmTimeEntity>> = dao.getAll()

    suspend fun upsert(alarm: AlarmTimeEntity) = dao.upsert(alarm)

    suspend fun delete(alarm: AlarmTimeEntity) = dao.delete(alarm)
} 