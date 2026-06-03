package com.example.alarm_clock_2.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm_clock_2.data.SettingsDataStore
import com.example.alarm_clock_2.shift.IdentityType
import com.example.alarm_clock_2.data.AlarmPlayMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

import java.time.LocalDate

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: SettingsDataStore
) : ViewModel() {

    data class UiState(
        val identity: IdentityType = IdentityType.LONG_DAY,
        val holidayRest: Boolean = false,
        val fourThreeIndex: Int = 0,
        val fourTwoIndex: Int = 0,
        val customPattern: String = "MORNING,NIGHT,OFF,OFF",
        val customIndex: Int = 0,
        val playMode: AlarmPlayMode = AlarmPlayMode.SOUND,
        val ringtoneUri: String = "",
        val snoozeCount: Int = 3,
        val snoozeInterval: Int = 5,
        val latestVersion: String? = null
    )

    private val _latestVersion = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            val info = com.example.alarm_clock_2.util.Updater.fetchLatest()
            _latestVersion.value = info?.version
        }
    }

    // 两级 combine 突破 5-Flow 参数上限
    @Suppress("UNCHECKED_CAST")
    private val _uiState: StateFlow<UiState> = combine(
        combine(
            dataStore.identityFlow,
            dataStore.holidayRestFlow,
            dataStore.fourThreeIndexFlow,
            dataStore.fourTwoIndexFlow,
            dataStore.customPatternFlow
        ) { id, hr, i43, i42, cp -> listOf(id, hr, i43, i42, cp) },
        combine(
            dataStore.customIndexFlow,
            dataStore.playModeFlow,
            dataStore.ringtoneUriFlow,
            dataStore.snoozeCountFlow,
            dataStore.snoozeIntervalFlow
        ) { ci, pm, ru, sc, si -> listOf(ci, pm, ru, sc, si) },
        _latestVersion
    ) { g1, g2, lv ->
        UiState(
            identity = toIdentity(g1[0] as String),
            holidayRest = g1[1] as Boolean,
            fourThreeIndex = g1[2] as Int,
            fourTwoIndex = g1[3] as Int,
            customPattern = g1[4] as String,
            customIndex = g2[0] as Int,
            playMode = AlarmPlayMode.from(g2[1] as String),
            ringtoneUri = g2[2] as String,
            snoozeCount = g2[3] as Int,
            snoozeInterval = g2[4] as Int,
            latestVersion = lv
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState())

    val uiState: StateFlow<UiState> = _uiState

    fun onIdentitySelected(identity: IdentityType) = launch { dataStore.setIdentity(identity.name) }

    fun onHolidayRestChanged(enabled: Boolean) = launch { dataStore.setHolidayRest(enabled) }

    fun onFourThreeIndexChanged(index: Int) = launch {
        dataStore.setFourThreeIndex(index)
        dataStore.setFourThreeBaseDate(LocalDate.now().toString())
    }

    fun onFourTwoIndexChanged(index: Int) = launch {
        dataStore.setFourTwoIndex(index)
        dataStore.setFourTwoBaseDate(LocalDate.now().toString())
    }

    fun onPlayModeSelected(mode: AlarmPlayMode) = launch { dataStore.setPlayMode(mode.name) }
    fun onRingtoneSelected(uri: String) = launch { dataStore.setRingtoneUri(uri) }

    fun onSnoozeCountChanged(count: Int) = launch { dataStore.setSnoozeCount(count) }

    fun onSnoozeIntervalChanged(minutes: Int) = launch { dataStore.setSnoozeInterval(minutes) }

    fun onCustomPatternChanged(pattern: String) = launch { dataStore.setCustomPattern(pattern) }

    fun onCustomIndexChanged(index: Int) = launch {
        dataStore.setCustomIndex(index)
        dataStore.setCustomBaseDate(LocalDate.now().toString())
    }

    private fun launch(block: suspend () -> Unit) = viewModelScope.launch { block() }

    private fun toIdentity(value: String): IdentityType =
        runCatching { IdentityType.valueOf(value) }.getOrDefault(IdentityType.LONG_DAY)
} 