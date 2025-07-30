package com.example.alarm_clock_2.util

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 应用程序常量定义
 * 集中管理所有硬编码值，提高代码可维护性
 */
object Constants {
    
    // ==================== 闹钟相关常量 ====================
    
    /** 默认贪睡次数 */
    const val DEFAULT_SNOOZE_COUNT = 3
    
    /** 默认贪睡间隔（分钟） */
    const val DEFAULT_SNOOZE_INTERVAL = 5
    
    /** 默认身份类型 */
    const val DEFAULT_IDENTITY = "LONG_DAY"
    
    /** 闹钟搜索天数范围 */
    const val ALARM_SEARCH_DAYS = 60
    
    /** 固定的Activity PendingIntent请求码 */
    const val ACTIVITY_PENDING_INTENT_REQUEST_CODE = -1
    
    // ==================== UI尺寸常量 ====================
    
    /** 标准间距 */
    val SPACING_STANDARD = 16.dp
    
    /** 小间距 */
    val SPACING_SMALL = 8.dp
    
    /** 大间距 */
    val SPACING_LARGE = 24.dp
    
    /** 超大间距 */
    val SPACING_EXTRA_LARGE = 32.dp
    
    /** 卡片圆角 */
    val CARD_CORNER_RADIUS = 16.dp
    
    /** 小圆角 */
    val SMALL_CORNER_RADIUS = 8.dp
    
    /** FAB尺寸 */
    val FAB_SIZE = 60.dp
    
    /** 图标尺寸 - 标准 */
    val ICON_SIZE_STANDARD = 24.dp
    
    /** 图标尺寸 - 大 */
    val ICON_SIZE_LARGE = 28.dp
    
    /** 图标尺寸 - 超大 */
    val ICON_SIZE_EXTRA_LARGE = 80.dp
    
    /** 列表项高度 */
    val LIST_ITEM_HEIGHT = 72.dp
    
    // ==================== 字体大小常量 ====================
    
    /** 标题字体大小 */
    val TITLE_FONT_SIZE = 28.sp
    
    /** 副标题字体大小 */
    val SUBTITLE_FONT_SIZE = 22.sp
    
    /** 正文字体大小 */
    val BODY_FONT_SIZE = 17.sp
    
    /** 小字体大小 */
    val SMALL_FONT_SIZE = 12.sp
    
    /** 行高 */
    val LINE_HEIGHT = 24.sp
    
    // ==================== 动画常量 ====================
    
    /** 标准动画时长 */
    const val ANIMATION_DURATION_STANDARD = 300
    
    /** 快速动画时长 */
    const val ANIMATION_DURATION_FAST = 150
    
    /** 慢速动画时长 */
    const val ANIMATION_DURATION_SLOW = 500
    
    // ==================== 透明度常量 ====================
    
    /** 禁用状态透明度 */
    const val ALPHA_DISABLED = 0.6f
    
    /** 次要内容透明度 */
    const val ALPHA_SECONDARY = 0.8f
    
    /** 背景透明度 */
    const val ALPHA_BACKGROUND = 0.95f
    
    /** 选择器透明度 */
    const val ALPHA_SELECTOR = 0.1f
    
    /** 边框透明度 */
    const val ALPHA_BORDER = 0.3f
    
    // ==================== 字符串常量 ====================
    
    /** 日志标签前缀 */
    const val LOG_TAG_PREFIX = "AlarmClock"
    
    /** 空状态提示文本 */
    const val EMPTY_STATE_TITLE = "暂无闹钟"
    const val EMPTY_STATE_SUBTITLE = "点击右下角的 + 按钮\n添加您的第一个闹钟"
    
    /** 错误消息 */
    const val ERROR_PERMISSION_DENIED = "权限被拒绝，无法设置精准闹钟"
    const val ERROR_INVALID_TIME_FORMAT = "时间格式无效"
    const val ERROR_SCHEDULE_FAILED = "闹钟设置失败"
    const val ERROR_UNKNOWN = "未知错误"
    
    // ==================== 班次相关常量 ====================
    
    /** 班次代码映射 */
    val SHIFT_CODE_LABELS = mapOf(
        "DAY" to "长白班",
        "MORNING" to "早班", 
        "AFTERNOON" to "中班",
        "NIGHT" to "晚班"
    )
    
    /** 默认闹钟时间映射 */
    val DEFAULT_ALARM_TIMES = mapOf(
        "DAY" to "08:00",
        "MORNING" to "06:00",
        "AFTERNOON" to "14:00", 
        "NIGHT" to "22:00"
    )
    
    // ==================== 数据库相关常量 ====================
    
    /** 数据库名称 */
    const val DATABASE_NAME = "alarm_database"
    
    /** 当前数据库版本 */
    const val DATABASE_VERSION = 5
    
    // ==================== 网络相关常量 ====================
    
    /** 网络请求超时时间（秒） */
    const val NETWORK_TIMEOUT_SECONDS = 30L
    
    /** 重试次数 */
    const val RETRY_COUNT = 3
    
    // ==================== 权限相关常量 ====================
    
    /** 权限请求码 */
    const val PERMISSION_REQUEST_CODE = 1001
    
    /** 通知权限请求码 */
    const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1002
}
