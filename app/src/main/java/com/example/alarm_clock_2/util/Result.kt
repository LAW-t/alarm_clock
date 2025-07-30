package com.example.alarm_clock_2.util

/**
 * 统一的结果封装类
 * 用于处理操作结果，包括成功、失败和加载状态
 */
sealed class Result<out T> {
    /** 成功状态 */
    data class Success<T>(val data: T) : Result<T>()
    
    /** 失败状态 */
    data class Error(
        val exception: Throwable,
        val message: String = exception.message ?: Constants.ERROR_UNKNOWN
    ) : Result<Nothing>()
    
    /** 加载状态 */
    object Loading : Result<Nothing>()
    
    /** 空状态 */
    object Empty : Result<Nothing>()
    
    // ==================== 扩展函数 ====================
    
    /** 是否为成功状态 */
    val isSuccess: Boolean get() = this is Success
    
    /** 是否为失败状态 */
    val isError: Boolean get() = this is Error
    
    /** 是否为加载状态 */
    val isLoading: Boolean get() = this is Loading
    
    /** 是否为空状态 */
    val isEmpty: Boolean get() = this is Empty
    
    /**
     * 获取数据，如果不是成功状态则返回null
     */
    fun getDataOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }
    
    /**
     * 获取错误信息，如果不是错误状态则返回null
     */
    fun getErrorOrNull(): String? = when (this) {
        is Error -> message
        else -> null
    }
    
    /**
     * 映射成功状态的数据
     */
    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is Loading -> this
        is Empty -> this
    }
    
    /**
     * 平铺映射
     */
    inline fun <R> flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> transform(data)
        is Error -> this
        is Loading -> this
        is Empty -> this
    }
    
    /**
     * 处理成功状态
     */
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }
    
    /**
     * 处理失败状态
     */
    inline fun onError(action: (String, Throwable) -> Unit): Result<T> {
        if (this is Error) action(message, exception)
        return this
    }
    
    /**
     * 处理加载状态
     */
    inline fun onLoading(action: () -> Unit): Result<T> {
        if (this is Loading) action()
        return this
    }
    
    /**
     * 处理空状态
     */
    inline fun onEmpty(action: () -> Unit): Result<T> {
        if (this is Empty) action()
        return this
    }
    
    companion object {
        /**
         * 创建成功结果
         */
        fun <T> success(data: T): Result<T> = Success(data)
        
        /**
         * 创建失败结果
         */
        fun error(exception: Throwable, message: String? = null): Result<Nothing> = 
            Error(exception, message ?: exception.message ?: Constants.ERROR_UNKNOWN)
        
        /**
         * 创建失败结果（仅消息）
         */
        fun error(message: String): Result<Nothing> = 
            Error(RuntimeException(message), message)
        
        /**
         * 创建加载结果
         */
        fun loading(): Result<Nothing> = Loading
        
        /**
         * 创建空结果
         */
        fun empty(): Result<Nothing> = Empty
        
        /**
         * 安全执行操作并返回Result
         */
        inline fun <T> runCatching(action: () -> T): Result<T> = try {
            success(action())
        } catch (e: Exception) {
            error(e)
        }
        
        /**
         * 安全执行挂起操作并返回Result
         */
        suspend inline fun <T> runCatchingSuspend(crossinline action: suspend () -> T): Result<T> = try {
            success(action())
        } catch (e: Exception) {
            error(e)
        }
    }
}

/**
 * 特定的闹钟相关错误类型
 */
sealed class AlarmError : Exception() {
    /** 权限被拒绝 */
    object PermissionDenied : AlarmError() {
        override val message: String = Constants.ERROR_PERMISSION_DENIED
    }
    
    /** 时间格式无效 */
    object InvalidTimeFormat : AlarmError() {
        override val message: String = Constants.ERROR_INVALID_TIME_FORMAT
    }
    
    /** 调度失败 */
    data class ScheduleFailed(override val message: String) : AlarmError()
    
    /** 数据库错误 */
    data class DatabaseError(override val message: String) : AlarmError()
    
    /** 网络错误 */
    data class NetworkError(override val message: String) : AlarmError()
    
    /** 未知错误 */
    data class Unknown(override val message: String = Constants.ERROR_UNKNOWN) : AlarmError()
}

/**
 * 扩展函数：将Throwable转换为AlarmError
 */
fun Throwable.toAlarmError(): AlarmError = when (this) {
    is AlarmError -> this
    is SecurityException -> AlarmError.PermissionDenied
    is java.time.format.DateTimeParseException -> AlarmError.InvalidTimeFormat
    else -> AlarmError.Unknown(message ?: Constants.ERROR_UNKNOWN)
}
