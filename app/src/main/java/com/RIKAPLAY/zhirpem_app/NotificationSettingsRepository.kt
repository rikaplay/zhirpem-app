package com.RIKAPLAY.zhirpem_app

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.notificationDataStore: DataStore<Preferences> by preferencesDataStore(name = "notification_settings")

class NotificationSettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val IS_VIBRATION_ENABLED = booleanPreferencesKey("is_vibration_enabled")
        val SENDER_FILTER = stringPreferencesKey("sender_filter")
        val ENABLED_CATEGORIES = stringSetPreferencesKey("enabled_categories")
    }

    val settingsFlow: Flow<NotificationSettings> = context.notificationDataStore.data
        .map { preferences ->
            val isVibrationEnabled = preferences[PreferencesKeys.IS_VIBRATION_ENABLED] ?: true
            val senderFilter = try {
                NotificationSenderFilter.valueOf(
                    preferences[PreferencesKeys.SENDER_FILTER] ?: NotificationSenderFilter.ALL.name
                )
            } catch (e: Exception) {
                NotificationSenderFilter.ALL
            }
            
            val enabledCategoriesNames = preferences[PreferencesKeys.ENABLED_CATEGORIES]
                ?: NotificationType.entries.map { it.name }.toSet()
            
            val enabledCategories = enabledCategoriesNames.mapNotNull {
                try { NotificationType.valueOf(it) } catch (e: Exception) { null }
            }.toSet()

            NotificationSettings(
                isVibrationEnabled = isVibrationEnabled,
                enabledCategories = enabledCategories,
                senderFilter = senderFilter
            )
        }

    suspend fun updateVibrationEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_VIBRATION_ENABLED] = enabled
        }
    }

    suspend fun updateSenderFilter(filter: NotificationSenderFilter) {
        context.notificationDataStore.edit { preferences ->
            preferences[PreferencesKeys.SENDER_FILTER] = filter.name
        }
    }

    suspend fun updateEnabledCategories(categories: Set<NotificationType>) {
        context.notificationDataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLED_CATEGORIES] = categories.map { it.name }.toSet()
        }
    }
}
