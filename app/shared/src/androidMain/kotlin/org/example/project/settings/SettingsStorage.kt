package org.example.project.settings

import android.content.Context
import android.content.SharedPreferences

lateinit var appContext: Context

actual object SettingsStorage {
    private val preferences: SharedPreferences
        get() = appContext.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    actual suspend fun load(): UserSettings {
        val language = preferences.getString("language", "es") ?: "es"
        val soundEnabled = preferences.getBoolean("sound_enabled", true)
        val themeStr = preferences.getString("theme", "SYSTEM") ?: "SYSTEM"
        val theme = try {
            Theme.valueOf(themeStr)
        } catch (e: Exception) {
            Theme.SYSTEM
        }

        return UserSettings(language, soundEnabled, theme)
    }

    actual suspend fun save(settings: UserSettings) {
        preferences.edit().apply {
            putString("language", settings.language)
            putBoolean("sound_enabled", settings.soundEnabled)
            putString("theme", settings.theme.name)
            apply()
        }
    }
}
