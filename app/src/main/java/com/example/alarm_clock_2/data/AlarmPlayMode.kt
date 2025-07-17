package com.example.alarm_clock_2.data

enum class AlarmPlayMode {
    /** 仅响铃 */
    SOUND,
    /** 仅震动 */
    VIBRATE,
    /** 响铃并震动 */
    BOTH;

    companion object {
        fun from(name: String?): AlarmPlayMode =
            runCatching { valueOf(name ?: "SOUND") }.getOrDefault(SOUND)
    }
} 