package com.RIKAPLAY.zhirpem_app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationSettingsViewModel(
    private val repository: NotificationSettingsRepository
) : ViewModel() {

    val settings: StateFlow<NotificationSettings> = repository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotificationSettings()
    )

    fun updateVibration(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateVibrationEnabled(enabled)
        }
    }

    fun updateSenderFilter(filter: NotificationSenderFilter) {
        viewModelScope.launch {
            repository.updateSenderFilter(filter)
        }
    }

    fun toggleCategory(type: NotificationType) {
        viewModelScope.launch {
            val current = settings.value.enabledCategories.toMutableSet()
            if (current.contains(type)) {
                current.remove(type)
            } else {
                current.add(type)
            }
            repository.updateEnabledCategories(current)
        }
    }
}
