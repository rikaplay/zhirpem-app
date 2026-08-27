package com.RIKAPLAY.zhirpem_app

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "energy_settings")

class EnergySaverRepository(private val context: Context) {

    private object Keys {
        val AUTO_ENERGY_SAVER = booleanPreferencesKey("auto_energy_saver_enabled")
        val BATTERY_THRESHOLD = intPreferencesKey("battery_threshold")
        val IS_ENERGY_SAVER_ACTIVE = booleanPreferencesKey("is_energy_saver_active")
        
        // Snapshot keys
        val SAVED_GLASS_ENABLED = booleanPreferencesKey("saved_glass_enabled")
        val SAVED_THEME_TYPE = stringPreferencesKey("saved_theme_type")
        val SAVED_APP_THEME_MODE = stringPreferencesKey("saved_app_theme_mode")
        val SAVED_PERSONALIZATION_ENABLED = booleanPreferencesKey("saved_personalization_enabled")
        val SAVED_ANIMATIONS_ENABLED = booleanPreferencesKey("saved_animations_enabled")
    }

    val autoEnergySaverEnabled: Flow<Boolean> = context.dataStore.data.map { 
        it[Keys.AUTO_ENERGY_SAVER] ?: true 
    }

    val batteryThreshold: Flow<Int> = context.dataStore.data.map { 
        it[Keys.BATTERY_THRESHOLD] ?: 20 
    }

    val isEnergySaverActive: Flow<Boolean> = context.dataStore.data.map { 
        it[Keys.IS_ENERGY_SAVER_ACTIVE] ?: false 
    }

    val snapshot: Flow<EnergySnapshot> = context.dataStore.data.map { prefs ->
        EnergySnapshot(
            glassEnabled = prefs[Keys.SAVED_GLASS_ENABLED],
            themeType = prefs[Keys.SAVED_THEME_TYPE],
            appThemeMode = prefs[Keys.SAVED_APP_THEME_MODE],
            personalizationEnabled = prefs[Keys.SAVED_PERSONALIZATION_ENABLED],
            animationsEnabled = prefs[Keys.SAVED_ANIMATIONS_ENABLED]
        )
    }

    suspend fun setAutoEnergySaver(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_ENERGY_SAVER] = enabled }
    }

    suspend fun setBatteryThreshold(threshold: Int) {
        context.dataStore.edit { it[Keys.BATTERY_THRESHOLD] = threshold }
    }

    suspend fun setEnergySaverActive(active: Boolean) {
        context.dataStore.edit { it[Keys.IS_ENERGY_SAVER_ACTIVE] = active }
    }

    suspend fun saveSnapshot(snapshot: EnergySnapshot) {
        context.dataStore.edit { prefs ->
            snapshot.glassEnabled?.let { prefs[Keys.SAVED_GLASS_ENABLED] = it }
            snapshot.themeType?.let { prefs[Keys.SAVED_THEME_TYPE] = it }
            snapshot.appThemeMode?.let { prefs[Keys.SAVED_APP_THEME_MODE] = it }
            snapshot.personalizationEnabled?.let { prefs[Keys.SAVED_PERSONALIZATION_ENABLED] = it }
            snapshot.animationsEnabled?.let { prefs[Keys.SAVED_ANIMATIONS_ENABLED] = it }
        }
    }
}

data class EnergySnapshot(
    val glassEnabled: Boolean? = null,
    val themeType: String? = null,
    val appThemeMode: String? = null,
    val personalizationEnabled: Boolean? = null,
    val animationsEnabled: Boolean? = null
)
