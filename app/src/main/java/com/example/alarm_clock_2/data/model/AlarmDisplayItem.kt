package com.example.alarm_clock_2.data.model

import com.example.alarm_clock_2.data.AlarmTimeEntity

/**
 * 闹钟显示项数据模型
 * 用于UI层显示闹钟信息，包含实体数据和显示标签
 */
data class AlarmDisplayItem(
    /** 闹钟实体数据 */
    val entity: AlarmTimeEntity,
    /** 显示标签（中文名称） */
    val label: String,
    /** 是否为自定义闹钟 */
    val isCustom: Boolean = false
) {
    /** 闹钟ID */
    val id: Int get() = entity.id
    
    /** 闹钟时间 */
    val time: String get() = entity.time
    
    /** 班次代码 */
    val shift: String get() = entity.shift
    
    /** 是否启用 */
    val enabled: Boolean get() = entity.enabled
    
    /** 显示名称 */
    val displayName: String? get() = entity.displayName
    
    /** 贪睡次数 */
    val snoozeCount: Int get() = entity.snoozeCount
    
    /** 贪睡间隔 */
    val snoozeInterval: Int get() = entity.snoozeInterval
    
    /** 身份标识 */
    val identity: String get() = entity.identity
    
    /**
     * 获取完整的显示标签
     * 如果有自定义显示名称，优先使用自定义名称
     */
    fun getFullLabel(): String {
        return displayName ?: label
    }
    
    /**
     * 判断是否可以删除
     * 自定义闹钟可以删除，默认闹钟不能删除
     */
    fun canDelete(): Boolean {
        return isCustom || displayName != null
    }
    
    /**
     * 获取下次响铃描述
     */
    fun getNextAlarmDescription(): String {
        return if (enabled) {
            "下次响铃: $time"
        } else {
            "已关闭"
        }
    }
}

/**
 * 班次选项数据模型
 * 用于班次选择器
 */
data class ShiftOption(
    /** 班次代码 */
    val code: String,
    /** 显示名称 */
    val label: String
) {
    companion object {
        /**
         * 根据身份类型获取可用的班次选项
         */
        fun getAvailableOptions(identity: com.example.alarm_clock_2.shift.IdentityType): List<ShiftOption> {
            return when (identity) {
                com.example.alarm_clock_2.shift.IdentityType.LONG_DAY -> listOf(
                    ShiftOption("DAY", "长白班")
                )
                com.example.alarm_clock_2.shift.IdentityType.FOUR_THREE -> listOf(
                    ShiftOption("MORNING", "早班"),
                    ShiftOption("AFTERNOON", "中班"),
                    ShiftOption("NIGHT", "晚班")
                )
                com.example.alarm_clock_2.shift.IdentityType.FOUR_TWO -> listOf(
                    ShiftOption("MORNING", "早班"),
                    ShiftOption("NIGHT", "晚班")
                )
            }
        }
    }
}

/**
 * 闹钟操作结果
 */
sealed class AlarmOperationResult {
    /** 操作成功 */
    object Success : AlarmOperationResult()
    
    /** 操作失败 */
    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : AlarmOperationResult()
    
    /** 权限被拒绝 */
    object PermissionDenied : AlarmOperationResult()
    
    /** 无效输入 */
    data class InvalidInput(val field: String, val reason: String) : AlarmOperationResult()
}

/**
 * 闹钟状态
 */
enum class AlarmState {
    /** 正常状态 */
    NORMAL,
    /** 加载中 */
    LOADING,
    /** 错误状态 */
    ERROR,
    /** 权限缺失 */
    PERMISSION_MISSING
}

/**
 * 闹钟列表UI状态
 */
data class AlarmListUiState(
    /** 闹钟列表 */
    val alarms: List<AlarmDisplayItem> = emptyList(),
    /** 当前状态 */
    val state: AlarmState = AlarmState.NORMAL,
    /** 错误消息 */
    val errorMessage: String? = null,
    /** 是否正在加载 */
    val isLoading: Boolean = false,
    /** 是否显示添加对话框 */
    val showAddDialog: Boolean = false,
    /** 是否显示编辑对话框 */
    val showEditDialog: Boolean = false,
    /** 正在编辑的闹钟 */
    val editingAlarm: AlarmTimeEntity? = null
) {
    /** 是否为空状态 */
    val isEmpty: Boolean get() = alarms.isEmpty() && state == AlarmState.NORMAL
    
    /** 是否有错误 */
    val hasError: Boolean get() = state == AlarmState.ERROR
    
    /** 是否缺少权限 */
    val isPermissionMissing: Boolean get() = state == AlarmState.PERMISSION_MISSING
}
