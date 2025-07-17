package com.example.alarm_clock_2.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm_clock_2.data.AlarmRepository
import com.example.alarm_clock_2.data.AlarmTimeEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.alarm_clock_2.alarm.AlarmScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmsViewModel @Inject constructor(
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler
) : ViewModel() {

    val alarms: StateFlow<List<AlarmTimeEntity>> = repository.getAlarms()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun toggleEnabled(alarm: AlarmTimeEntity) = viewModelScope.launch {
        val updated = alarm.copy(enabled = !alarm.enabled)
        repository.upsert(updated)
        if (updated.enabled) scheduler.schedule(updated) else scheduler.cancel(updated)
    }

    fun updateTime(alarm: AlarmTimeEntity, newTime: String) = viewModelScope.launch {
        val updated = alarm.copy(time = newTime)
        repository.upsert(updated)
        if (updated.enabled) scheduler.schedule(updated) else scheduler.cancel(updated)
    }

    fun addAlarm(time: String, shift: String) = viewModelScope.launch {
        val newAlarm = AlarmTimeEntity(time = time, shift = shift)
        repository.upsert(newAlarm)
        scheduler.schedule(newAlarm)
    }
} 