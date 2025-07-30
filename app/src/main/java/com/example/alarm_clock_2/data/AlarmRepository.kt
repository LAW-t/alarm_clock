package com.example.alarm_clock_2.data

import com.example.alarm_clock_2.util.Constants
import com.example.alarm_clock_2.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmRepository @Inject constructor(
    private val dao: AlarmDao
) {

    // 用于同步数据库操作的互斥锁
    private val mutex = Mutex()

    /**
     * 获取所有闹钟
     */
    fun getAlarms(): Flow<List<AlarmTimeEntity>> = dao.getAll()
        .catch { exception ->
            // 记录错误但不中断流
            android.util.Log.e(Constants.LOG_TAG_PREFIX, "获取闹钟列表失败", exception)
            emit(emptyList())
        }

    /**
     * 根据身份获取闹钟
     */
    fun getAlarmsByIdentity(identity: String): Flow<List<AlarmTimeEntity>> =
        dao.getByIdentity(identity)
            .catch { exception ->
                android.util.Log.e(Constants.LOG_TAG_PREFIX, "根据身份获取闹钟失败", exception)
                emit(emptyList())
            }

    /**
     * 根据班次获取闹钟
     */
    fun getAlarmsByShift(shift: String): Flow<List<AlarmTimeEntity>> =
        dao.getByShift(shift)
            .catch { exception ->
                android.util.Log.e(Constants.LOG_TAG_PREFIX, "根据班次获取闹钟失败", exception)
                emit(emptyList())
            }

    /**
     * 获取启用的闹钟
     */
    fun getEnabledAlarms(): Flow<List<AlarmTimeEntity>> =
        dao.getEnabled()
            .catch { exception ->
                android.util.Log.e(Constants.LOG_TAG_PREFIX, "获取启用闹钟失败", exception)
                emit(emptyList())
            }

    /**
     * 根据身份和启用状态获取闹钟
     */
    fun getAlarmsByIdentityAndEnabled(identity: String, enabled: Boolean): Flow<List<AlarmTimeEntity>> =
        dao.getByIdentityAndEnabled(identity, enabled)
            .catch { exception ->
                android.util.Log.e(Constants.LOG_TAG_PREFIX, "根据身份和状态获取闹钟失败", exception)
                emit(emptyList())
            }

    /**
     * 插入或更新闹钟
     * 使用互斥锁确保线程安全
     */
    suspend fun upsert(alarm: AlarmTimeEntity): Result<AlarmTimeEntity> = mutex.withLock {
        try {
            // 验证闹钟数据
            validateAlarm(alarm)

            // 执行数据库操作
            dao.upsert(alarm)

            android.util.Log.d(Constants.LOG_TAG_PREFIX, "闹钟保存成功: ${alarm.id}")
            Result.success(alarm)
        } catch (e: Exception) {
            android.util.Log.e(Constants.LOG_TAG_PREFIX, "保存闹钟失败", e)
            Result.error(e, "保存闹钟失败: ${e.message}")
        }
    }

    /**
     * 删除闹钟
     */
    suspend fun delete(alarm: AlarmTimeEntity): Result<Unit> = mutex.withLock {
        try {
            dao.delete(alarm)
            android.util.Log.d(Constants.LOG_TAG_PREFIX, "闹钟删除成功: ${alarm.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(Constants.LOG_TAG_PREFIX, "删除闹钟失败", e)
            Result.error(e, "删除闹钟失败: ${e.message}")
        }
    }

    /**
     * 根据ID删除闹钟
     */
    suspend fun deleteById(id: Int): Result<Unit> = mutex.withLock {
        try {
            dao.deleteById(id)
            android.util.Log.d(Constants.LOG_TAG_PREFIX, "闹钟删除成功: $id")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(Constants.LOG_TAG_PREFIX, "删除闹钟失败", e)
            Result.error(e, "删除闹钟失败: ${e.message}")
        }
    }

    /**
     * 批量插入或更新闹钟
     */
    suspend fun upsertAll(alarms: List<AlarmTimeEntity>): Result<List<AlarmTimeEntity>> = mutex.withLock {
        try {
            // 验证所有闹钟数据
            alarms.forEach { validateAlarm(it) }

            // 批量操作
            dao.upsertAll(alarms)

            android.util.Log.d(Constants.LOG_TAG_PREFIX, "批量保存闹钟成功: ${alarms.size}个")
            Result.success(alarms)
        } catch (e: Exception) {
            android.util.Log.e(Constants.LOG_TAG_PREFIX, "批量保存闹钟失败", e)
            Result.error(e, "批量保存闹钟失败: ${e.message}")
        }
    }

    /**
     * 清空指定身份的所有闹钟
     */
    suspend fun clearAlarmsByIdentity(identity: String): Result<Unit> = mutex.withLock {
        try {
            dao.deleteByIdentity(identity)
            android.util.Log.d(Constants.LOG_TAG_PREFIX, "清空身份闹钟成功: $identity")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(Constants.LOG_TAG_PREFIX, "清空身份闹钟失败", e)
            Result.error(e, "清空身份闹钟失败: ${e.message}")
        }
    }

    /**
     * 验证闹钟数据
     */
    private fun validateAlarm(alarm: AlarmTimeEntity) {
        // 验证时间格式
        try {
            java.time.LocalTime.parse(alarm.time)
        } catch (e: Exception) {
            throw IllegalArgumentException("时间格式无效: ${alarm.time}")
        }

        // 验证班次代码
        if (alarm.shift.isBlank()) {
            throw IllegalArgumentException("班次代码不能为空")
        }

        // 验证身份
        if (alarm.identity.isBlank()) {
            throw IllegalArgumentException("身份标识不能为空")
        }

        // 验证贪睡设置
        if (alarm.snoozeCount < 0 || alarm.snoozeCount > 10) {
            throw IllegalArgumentException("贪睡次数必须在0-10之间")
        }

        if (alarm.snoozeInterval < 1 || alarm.snoozeInterval > 60) {
            throw IllegalArgumentException("贪睡间隔必须在1-60分钟之间")
        }
    }
}