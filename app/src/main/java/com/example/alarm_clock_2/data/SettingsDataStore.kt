package com.example.alarm_clock_2.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(@ApplicationContext private val context: Context) {
    object Keys {
        val IDENTITY = stringPreferencesKey("identity_type")
        val HOLIDAY_REST = booleanPreferencesKey("holiday_rest")
        val FOUR3_INDEX = intPreferencesKey("four_three_index")
        val FOUR2_INDEX = intPreferencesKey("four_two_index")
        /** 记录用户确认班次时的“今天”日期，用 ISO-8601 字符串存储 */
        val FOUR3_BASE_DATE = stringPreferencesKey("four_three_base_date")
        val FOUR2_BASE_DATE = stringPreferencesKey("four_two_base_date")
        // 标记节假日数据是否已成功加载并缓存
        val HOLIDAY_LOADED = booleanPreferencesKey("holiday_loaded")
        // 新增: 铃声 uri & 播放模式
        val RINGTONE_URI = stringPreferencesKey("ringtone_uri")
        val PLAY_MODE = stringPreferencesKey("play_mode")
        // 重复响铃次数与间隔
        val SNOOZE_COUNT = intPreferencesKey("snooze_count")
        val SNOOZE_INTERVAL = intPreferencesKey("snooze_interval")
    }

    val identityFlow: Flow<String> = context.dataStore.data.map { it[Keys.IDENTITY] ?: "LONG_DAY" }
    val holidayRestFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.HOLIDAY_REST] ?: false }

    val fourThreeIndexFlow: Flow<Int> = context.dataStore.data.map { it[Keys.FOUR3_INDEX] ?: 0 }
    val fourTwoIndexFlow: Flow<Int> = context.dataStore.data.map { it[Keys.FOUR2_INDEX] ?: 0 }

    /** 若未设置则使用应用首次启动当天作为默认值 */
    val fourThreeBaseDateFlow: Flow<String> = context.dataStore.data.map {
        it[Keys.FOUR3_BASE_DATE] ?: java.time.LocalDate.now().toString()
    }

    val fourTwoBaseDateFlow: Flow<String> = context.dataStore.data.map {
        it[Keys.FOUR2_BASE_DATE] ?: java.time.LocalDate.now().toString()
    }

    // 节假日数据加载状态
    val holidayLoadedFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.HOLIDAY_LOADED] ?: false }

    // === 新增设置 ===
    val ringtoneUriFlow: Flow<String> = context.dataStore.data.map { it[Keys.RINGTONE_URI] ?: "" }
    val playModeFlow: Flow<String> = context.dataStore.data.map { it[Keys.PLAY_MODE] ?: "SOUND" }

    // 新增：重复响铃设置，默认次数 3 次，间隔 5 分钟
    val snoozeCountFlow: Flow<Int> = context.dataStore.data.map { it[Keys.SNOOZE_COUNT] ?: 3 }
    val snoozeIntervalFlow: Flow<Int> = context.dataStore.data.map { it[Keys.SNOOZE_INTERVAL] ?: 5 }

    // === setter ===
    suspend fun setIdentity(value: String) {
        context.dataStore.edit { it[Keys.IDENTITY] = value }
    }
    suspend fun setHolidayRest(value: Boolean) {
        context.dataStore.edit { it[Keys.HOLIDAY_REST] = value }
    }

    suspend fun setFourThreeIndex(value: Int) {
        context.dataStore.edit { it[Keys.FOUR3_INDEX] = value }
    }

    suspend fun setFourTwoIndex(value: Int) {
        context.dataStore.edit { it[Keys.FOUR2_INDEX] = value }
    }

    suspend fun setFourThreeBaseDate(value: String) {
        context.dataStore.edit { it[Keys.FOUR3_BASE_DATE] = value }
    }

    suspend fun setFourTwoBaseDate(value: String) {
        context.dataStore.edit { it[Keys.FOUR2_BASE_DATE] = value }
    }

    suspend fun setHolidayLoaded(value: Boolean) {
        context.dataStore.edit { it[Keys.HOLIDAY_LOADED] = value }
    }

    suspend fun setRingtoneUri(uri: String) {
        context.dataStore.edit { it[Keys.RINGTONE_URI] = uri }
    }
    suspend fun setPlayMode(mode: String) {
        context.dataStore.edit { it[Keys.PLAY_MODE] = mode }
    }

    suspend fun setSnoozeCount(count: Int) {
        context.dataStore.edit { it[Keys.SNOOZE_COUNT] = count }
    }

    suspend fun setSnoozeInterval(minutes: Int) {
        context.dataStore.edit { it[Keys.SNOOZE_INTERVAL] = minutes }
    }
} 