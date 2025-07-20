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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

@HiltViewModel
class AlarmsViewModel @Inject constructor(
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler
) : ViewModel() {

    val alarms: StateFlow<List<AlarmTimeEntity>> = repository.getAlarms()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _toastFlow = MutableSharedFlow<String>()
    val toastFlow: SharedFlow<String> = _toastFlow

    fun toggleEnabled(alarm: AlarmTimeEntity) = viewModelScope.launch {
        val updated = alarm.copy(enabled = !alarm.enabled)
        repository.upsert(updated)
        if (updated.enabled) {
            scheduler.schedule(updated)
            notifyNextAlarm(updated)
        } else {
            scheduler.cancel(updated)
        }
    }

    fun updateTime(alarm: AlarmTimeEntity, newTime: String) = viewModelScope.launch {
        val updated = alarm.copy(time = newTime)
        repository.upsert(updated)
        if (updated.enabled) {
            scheduler.schedule(updated)
            notifyNextAlarm(updated)
        } else {
            scheduler.cancel(updated)
        }
    }

    fun addAlarm(time: String, shift: String) = viewModelScope.launch {
        val newAlarm = AlarmTimeEntity(time = time, shift = shift)
        repository.upsert(newAlarm)
        scheduler.schedule(newAlarm)
        notifyNextAlarm(newAlarm)

    }

    private suspend fun notifyNextAlarm(entity: AlarmTimeEntity) {
        val next = scheduler.nextTriggerMillis(entity) ?: return
        val diff = next - System.currentTimeMillis()
        if (diff <= 0) return
        val hours = diff / (1000 * 60 * 60)
        val minutes = (diff / (1000 * 60) % 60)
        val msg = if (hours > 0) {
            "最近的闹钟将在${hours}小时${minutes}分钟后响起"
        } else {
            "最近的闹钟将在${minutes}分钟后响起"
        }
        _toastFlow.emit(msg)
    }
} 