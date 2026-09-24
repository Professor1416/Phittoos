package com.professor1416.phittoos.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode(val storageKey: String, val displayName: String) {
    SYSTEM("system", "System default"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark");

    companion object {
        fun fromKey(key: String?): AppThemeMode {
            return entries.firstOrNull { it.storageKey.equals(key, ignoreCase = true) } ?: SYSTEM
        }
    }
}

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("phittoos_prefs", Context.MODE_PRIVATE)

    private val _themeModeFlow = MutableStateFlow(
        AppThemeMode.fromKey(prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.storageKey))
    )
    val themeModeFlow: StateFlow<AppThemeMode> = _themeModeFlow.asStateFlow()

    var themeMode: AppThemeMode
        get() = AppThemeMode.fromKey(prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.storageKey))
        set(value) {
            prefs.edit().putString(KEY_THEME_MODE, value.storageKey).apply()
            _themeModeFlow.value = value
        }

    var hasCompletedOnboarding: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var remindersEnabled: Boolean
        get() = prefs.getBoolean(KEY_REMINDERS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_REMINDERS_ENABLED, value).apply()

    var lastBackupTime: Long
        get() = prefs.getLong(KEY_LAST_BACKUP_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_BACKUP_TIME, value).apply()

    fun clearAll() {
        prefs.edit().clear().apply()
        _themeModeFlow.value = AppThemeMode.SYSTEM
    }

    companion object {
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_REMINDERS_ENABLED = "reminders_enabled"
        private const val KEY_LAST_BACKUP_TIME = "last_backup_time"
        private const val KEY_THEME_MODE = "app_theme_mode"
    }
}

