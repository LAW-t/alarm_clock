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
        val playMode: AlarmPlayMode = AlarmPlayMode.SOUND,
        val ringtoneUri: String = "",
        val latestVersion: String? = null
    )

    private val _latestVersion = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            val info = com.example.alarm_clock_2.util.Updater.fetchLatest()
            _latestVersion.value = info?.version
        }
    }

    private val _uiState: StateFlow<UiState> = combine(
        dataStore.identityFlow,
        dataStore.holidayRestFlow,
        dataStore.fourThreeIndexFlow,
        dataStore.fourTwoIndexFlow,
        dataStore.playModeFlow,
        dataStore.ringtoneUriFlow,
        _latestVersion
    ) { values ->
        val identity = toIdentity(values[0] as String)
        val holiday = values[1] as Boolean
        val idx43 = values[2] as Int
        val idx42 = values[3] as Int
        val mode = AlarmPlayMode.from(values[4] as String)
        val uri = values[5] as String
        val latestVersion = values[6] as String?
        UiState(identity, holiday, idx43, idx42, mode, uri, latestVersion)
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

    private fun launch(block: suspend () -> Unit) = viewModelScope.launch { block() }

    private fun toIdentity(value: String): IdentityType =
        runCatching { IdentityType.valueOf(value) }.getOrDefault(IdentityType.LONG_DAY)
} 