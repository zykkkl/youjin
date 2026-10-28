package com.youjin.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val Context.dataStore by preferencesDataStore(name = "settings")

/** 应用偏好：主题模式、减少动效 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val REDUCE_ANIM = booleanPreferencesKey("reduce_animations")
        val AUTO_CHECK_UPDATES = booleanPreferencesKey("auto_check_updates")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        runCatching { ThemeMode.valueOf(prefs[Keys.THEME] ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM)
    }

    val reduceAnimations: Flow<Boolean> = context.dataStore.data.map { it[Keys.REDUCE_ANIM] ?: false }

    /** 是否每天后台自动检查更新（默认开启） */
    val autoCheckUpdates: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_CHECK_UPDATES] ?: true }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun setReduceAnimations(value: Boolean) {
        context.dataStore.edit { it[Keys.REDUCE_ANIM] = value }
    }

    suspend fun setAutoCheckUpdates(value: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_CHECK_UPDATES] = value }
    }
}
