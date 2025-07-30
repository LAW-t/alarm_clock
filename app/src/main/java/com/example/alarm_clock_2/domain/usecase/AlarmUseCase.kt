package com.example.alarm_clock_2.domain.usecase

import com.example.alarm_clock_2.data.AlarmRepository
import com.example.alarm_clock_2.data.AlarmTimeEntity
import com.example.alarm_clock_2.data.SettingsDataStore
import com.example.alarm_clock_2.data.model.AlarmDisplayItem
import com.example.alarm_clock_2.data.model.ShiftOption
import com.example.alarm_clock_2.shift.IdentityType
import com.example.alarm_clock_2.util.Constants
import com.example.alarm_clock_2.util.Result
import com.example.alarm_clock_2.util.AlarmError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 闹钟业务逻辑用例
 * 封装闹钟相关的所有业务操作，提供统一的接口给ViewModel使用
 */
@Singleton
class AlarmUseCase @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val settingsDataStore: SettingsDataStore
) {
    
    /**
     * 获取当前身份对应的闹钟显示列表
     */
    fun getAlarmsForCurrentIdentity(): Flow<List<AlarmDisplayItem>> {
        return combine(
            alarmRepository.getAlarms(),
            settingsDataStore.identityFlow
        ) { alarms, identityStr ->
            val identity = parseIdentity(identityStr)
            val filteredAlarms = alarms.filter { it.identity == identity.name }
            
            filteredAlarms.map { alarm ->
                val label = Constants.SHIFT_CODE_LABELS[alarm.shift] 
                    ?: alarm.displayName 
                    ?: alarm.shift
                val isCustom = alarm.displayName != null
                AlarmDisplayItem(alarm, label, isCustom)
            }
        }
    }
    
    /**
     * 获取所有闹钟
     */
    fun getAllAlarms(): Flow<List<AlarmTimeEntity>> {
        return alarmRepository.getAlarms()
    }
    
    /**
     * 添加新闹钟
     */
    suspend fun addAlarm(
        time: String,
        shift: String,
        displayName: String? = null,
        snoozeCount: Int = Constants.DEFAULT_SNOOZE_COUNT,
        snoozeInterval: Int = Constants.DEFAULT_SNOOZE_INTERVAL,
        identity: String? = null
    ): Result<AlarmTimeEntity> {
        return Result.runCatchingSuspend {
            // 输入验证
            validateTimeFormat(time)
            validateShiftCode(shift)
            
            // 获取当前身份
            val currentIdentity = identity ?: settingsDataStore.identityFlow.first()
            
            // 创建闹钟实体
            val alarm = AlarmTimeEntity(
                time = time,
                shift = shift,
                displayName = displayName,
                snoozeCount = snoozeCount,
                snoozeInterval = snoozeInterval,
                identity = currentIdentity
            )
            
            // 保存到数据库
            alarmRepository.upsert(alarm)
            alarm
        }
    }
    
    /**
     * 更新闹钟
     */
    suspend fun updateAlarm(
        alarm: AlarmTimeEntity,
        newTime: String? = null,
        newShift: String? = null,
        newDisplayName: String? = null,
        newSnoozeCount: Int? = null,
        newSnoozeInterval: Int? = null
    ): Result<AlarmTimeEntity> {
        return Result.runCatchingSuspend {
            // 输入验证
            newTime?.let { validateTimeFormat(it) }
            newShift?.let { validateShiftCode(it) }
            
            // 创建更新后的闹钟
            val updatedAlarm = alarm.copy(
                time = newTime ?: alarm.time,
                shift = newShift ?: alarm.shift,
                displayName = newDisplayName ?: alarm.displayName,
                snoozeCount = newSnoozeCount ?: alarm.snoozeCount,
                snoozeInterval = newSnoozeInterval ?: alarm.snoozeInterval
            )
            
            // 保存到数据库
            alarmRepository.upsert(updatedAlarm)
            updatedAlarm
        }
    }
    
    /**
     * 删除闹钟
     */
    suspend fun deleteAlarm(alarm: AlarmTimeEntity): Result<Unit> {
        return Result.runCatchingSuspend {
            alarmRepository.delete(alarm)
        }
    }
    
    /**
     * 切换闹钟启用状态
     */
    suspend fun toggleAlarmEnabled(alarm: AlarmTimeEntity): Result<AlarmTimeEntity> {
        return Result.runCatchingSuspend {
            val updatedAlarm = alarm.copy(enabled = !alarm.enabled)
            alarmRepository.upsert(updatedAlarm)
            updatedAlarm
        }
    }
    
    /**
     * 为指定身份创建默认闹钟
     */
    suspend fun createDefaultAlarmsForIdentity(identity: IdentityType): Result<List<AlarmTimeEntity>> {
        return Result.runCatchingSuspend {
            val requiredShiftCodes = getRequiredShiftCodes(identity)
            val createdAlarms = mutableListOf<AlarmTimeEntity>()
            
            requiredShiftCodes.forEach { shiftCode ->
                val defaultTime = Constants.DEFAULT_ALARM_TIMES[shiftCode] ?: "08:00"
                val displayName = Constants.SHIFT_CODE_LABELS[shiftCode]
                
                val alarm = AlarmTimeEntity(
                    time = defaultTime,
                    shift = shiftCode,
                    displayName = displayName,
                    identity = identity.name
                )
                
                alarmRepository.upsert(alarm)
                createdAlarms.add(alarm)
            }
            
            createdAlarms
        }
    }
    
    /**
     * 检查是否需要创建默认闹钟
     */
    suspend fun shouldCreateDefaultAlarms(): Boolean {
        val currentIdentity = parseIdentity(settingsDataStore.identityFlow.first())
        val existingAlarms = alarmRepository.getAlarms().first()
        val currentIdentityAlarms = existingAlarms.filter { it.identity == currentIdentity.name }
        
        return currentIdentityAlarms.isEmpty()
    }
    
    /**
     * 获取可用的班次选项
     */
    suspend fun getAvailableShiftOptions(): List<ShiftOption> {
        val identityStr = settingsDataStore.identityFlow.first()
        val identity = parseIdentity(identityStr)
        return ShiftOption.getAvailableOptions(identity)
    }
    
    /**
     * 验证时间格式
     */
    private fun validateTimeFormat(time: String) {
        try {
            java.time.LocalTime.parse(time)
        } catch (e: Exception) {
            throw AlarmError.InvalidTimeFormat
        }
    }
    
    /**
     * 验证班次代码
     */
    private fun validateShiftCode(shift: String) {
        if (shift.isBlank()) {
            throw AlarmError.Unknown("班次代码不能为空")
        }
    }
    
    /**
     * 解析身份类型
     */
    private fun parseIdentity(identityStr: String): IdentityType {
        return runCatching { 
            IdentityType.valueOf(identityStr) 
        }.getOrDefault(IdentityType.LONG_DAY)
    }
    
    /**
     * 获取身份对应的必需班次代码
     */
    private fun getRequiredShiftCodes(identity: IdentityType): List<String> {
        return when (identity) {
            IdentityType.LONG_DAY -> listOf("DAY")
            IdentityType.FOUR_THREE -> listOf("MORNING", "AFTERNOON", "NIGHT")
            IdentityType.FOUR_TWO -> listOf("MORNING", "NIGHT")
        }
    }
}
