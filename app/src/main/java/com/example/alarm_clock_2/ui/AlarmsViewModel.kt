package com.example.alarm_clock_2.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm_clock_2.data.AlarmTimeEntity
import com.example.alarm_clock_2.data.model.AlarmDisplayItem
import com.example.alarm_clock_2.data.model.AlarmListUiState
import com.example.alarm_clock_2.data.model.AlarmState
import com.example.alarm_clock_2.data.model.ShiftOption
import com.example.alarm_clock_2.domain.usecase.AlarmUseCase
import com.example.alarm_clock_2.domain.usecase.AlarmScheduleUseCase
import com.example.alarm_clock_2.util.Constants
import com.example.alarm_clock_2.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmsViewModel @Inject constructor(
    private val alarmUseCase: AlarmUseCase,
    private val scheduleUseCase: AlarmScheduleUseCase
) : ViewModel() {

    // ==================== UI状态管理 ====================

    private val _uiState = MutableStateFlow(AlarmListUiState())
    val uiState: StateFlow<AlarmListUiState> = _uiState.asStateFlow()

    private val _toastFlow = MutableSharedFlow<String>()
    val toastFlow: SharedFlow<String> = _toastFlow

    private val _errorFlow = MutableSharedFlow<String>()
    val errorFlow: SharedFlow<String> = _errorFlow

    // ==================== 数据流 ====================

    /** 当前身份对应的闹钟列表（显示项） */
    val alarmDisplayItems: StateFlow<List<AlarmDisplayItem>> = alarmUseCase.getAlarmsForCurrentIdentity()
        .catch { exception ->
            handleError("获取闹钟列表失败", exception)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** 当前身份对应的闹钟实体列表（向后兼容） */
    val alarms: StateFlow<List<AlarmTimeEntity>> = alarmDisplayItems
        .map { displayItems -> displayItems.map { it.entity } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        // 监听闹钟列表变化，更新UI状态
        viewModelScope.launch {
            alarmDisplayItems.collect { alarmList ->
                _uiState.value = _uiState.value.copy(
                    alarms = alarmList,
                    state = if (alarmList.isEmpty()) AlarmState.NORMAL else AlarmState.NORMAL,
                    isLoading = false
                )
            }
        }

        // 检查是否需要创建默认闹钟
        checkAndCreateDefaultAlarms()
    }

    // ==================== 闹钟操作 ====================

    /**
     * 切换闹钟启用状态
     */
    fun toggleEnabled(alarm: AlarmTimeEntity) = viewModelScope.launch {
        setLoading(true)

        alarmUseCase.toggleAlarmEnabled(alarm)
            .onSuccess { updatedAlarm ->
                if (updatedAlarm.enabled) {
                    scheduleAlarm(updatedAlarm)
                } else {
                    cancelAlarm(updatedAlarm)
                }
            }
            .onError { message, exception ->
                handleError("切换闹钟状态失败: $message", exception)
            }

        setLoading(false)
    }

    /**
     * 更新闹钟时间
     */
    fun updateTime(alarm: AlarmTimeEntity, newTime: String) = viewModelScope.launch {
        updateAlarm(alarm, newTime = newTime)
    }

    /**
     * 更新闹钟信息
     */
    fun updateAlarm(
        alarm: AlarmTimeEntity,
        newTime: String? = null,
        newShift: String? = null,
        newDisplayName: String? = null,
        snoozeCount: Int? = null,
        snoozeInterval: Int? = null
    ) = viewModelScope.launch {
        setLoading(true)

        alarmUseCase.updateAlarm(
            alarm = alarm,
            newTime = newTime,
            newShift = newShift,
            newDisplayName = newDisplayName,
            newSnoozeCount = snoozeCount,
            newSnoozeInterval = snoozeInterval
        ).onSuccess { updatedAlarm ->
            rescheduleAlarm(updatedAlarm)
        }.onError { message, exception ->
            handleError("更新闹钟失败: $message", exception)
        }

        setLoading(false)
    }

    /**
     * 添加新闹钟
     */
    fun addAlarm(
        time: String,
        shift: String,
        displayName: String? = null,
        snoozeCount: Int = Constants.DEFAULT_SNOOZE_COUNT,
        snoozeInterval: Int = Constants.DEFAULT_SNOOZE_INTERVAL,
        identity: String? = null
    ) = viewModelScope.launch {
        setLoading(true)

        alarmUseCase.addAlarm(
            time = time,
            shift = shift,
            displayName = displayName,
            snoozeCount = snoozeCount,
            snoozeInterval = snoozeInterval,
            identity = identity
        ).onSuccess { newAlarm ->
            scheduleAlarm(newAlarm)
        }.onError { message, exception ->
            handleError("添加闹钟失败: $message", exception)
        }

        setLoading(false)
    }

    /**
     * 删除闹钟
     */
    fun deleteAlarm(alarm: AlarmTimeEntity) = viewModelScope.launch {
        setLoading(true)

        // 先取消调度
        cancelAlarm(alarm)

        // 再删除数据
        alarmUseCase.deleteAlarm(alarm)
            .onSuccess {
                showToast("闹钟已删除")
            }
            .onError { message, exception ->
                handleError("删除闹钟失败: $message", exception)
            }

        setLoading(false)
    }

    // ==================== 对话框状态管理 ====================

    /**
     * 显示添加闹钟对话框
     */
    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = true)
    }

    /**
     * 隐藏添加闹钟对话框
     */
    fun hideAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false)
    }

    /**
     * 显示编辑闹钟对话框
     */
    fun showEditDialog(alarm: AlarmTimeEntity) {
        _uiState.value = _uiState.value.copy(
            showEditDialog = true,
            editingAlarm = alarm
        )
    }

    /**
     * 隐藏编辑闹钟对话框
     */
    fun hideEditDialog() {
        _uiState.value = _uiState.value.copy(
            showEditDialog = false,
            editingAlarm = null
        )
    }

    // ==================== 业务逻辑辅助方法 ====================

    /**
     * 获取可用的班次选项
     */
    suspend fun getAvailableShiftOptions(): List<ShiftOption> {
        return try {
            alarmUseCase.getAvailableShiftOptions()
        } catch (e: Exception) {
            handleError("获取班次选项失败", e)
            emptyList()
        }
    }

    /**
     * 检查并创建默认闹钟
     */
    private fun checkAndCreateDefaultAlarms() = viewModelScope.launch {
        try {
            if (alarmUseCase.shouldCreateDefaultAlarms()) {
                // 这里可以根据需要决定是否自动创建默认闹钟
                // 根据用户偏好，当前实现不自动创建
            }
        } catch (e: Exception) {
            handleError("检查默认闹钟失败", e)
        }
    }

    // ==================== 调度相关方法 ====================

    /**
     * 调度闹钟
     */
    private suspend fun scheduleAlarm(alarm: AlarmTimeEntity) {
        scheduleUseCase.scheduleAlarm(alarm)
            .onSuccess { message ->
                showToast(message)
            }
            .onError { message, exception ->
                handleError("调度闹钟失败: $message", exception)
            }
    }

    /**
     * 取消闹钟调度
     */
    private suspend fun cancelAlarm(alarm: AlarmTimeEntity) {
        scheduleUseCase.cancelAlarm(alarm)
            .onError { message, exception ->
                handleError("取消闹钟调度失败: $message", exception)
            }
    }

    /**
     * 重新调度闹钟
     */
    private suspend fun rescheduleAlarm(alarm: AlarmTimeEntity) {
        scheduleUseCase.rescheduleAlarm(alarm)
            .onSuccess { message ->
                showToast(message)
            }
            .onError { message, exception ->
                handleError("重新调度闹钟失败: $message", exception)
            }
    }

    // ==================== UI状态辅助方法 ====================

    /**
     * 设置加载状态
     */
    private fun setLoading(isLoading: Boolean) {
        _uiState.value = _uiState.value.copy(
            isLoading = isLoading,
            state = if (isLoading) AlarmState.LOADING else AlarmState.NORMAL
        )
    }

    /**
     * 显示Toast消息
     */
    private suspend fun showToast(message: String) {
        _toastFlow.emit(message)
    }

    /**
     * 处理错误
     */
    private fun handleError(message: String, exception: Throwable) {
        viewModelScope.launch {
            _errorFlow.emit(message)
            _uiState.value = _uiState.value.copy(
                state = AlarmState.ERROR,
                errorMessage = message,
                isLoading = false
            )
        }
    }

    /**
     * 清除错误状态
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(
            state = AlarmState.NORMAL,
            errorMessage = null
        )
    }
}