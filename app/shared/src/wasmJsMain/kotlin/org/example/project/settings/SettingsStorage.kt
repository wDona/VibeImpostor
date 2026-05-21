package org.example.project.settings

import kotlinx.serialization.json.Json

private fun lsGet(key: String): String? =
    js("localStorage.getItem(key)")

private fun lsSet(key: String, value: String) {
    js("localStorage.setItem(key, value)")
}

actual object SettingsStorage {
    actual suspend fun load(): UserSettings {
        return try {
            val json = lsGet("settings")
            if (json != null) {
                Json.decodeFromString<UserSettings>(json)
            } else {
                UserSettings()
            }
        } catch (e: Exception) {
            UserSettings()
        }
    }

    actual suspend fun save(settings: UserSettings) {
        try {
            val json = Json.encodeToString(UserSettings.serializer(), settings)
            lsSet("settings", json)
        } catch (e: Exception) {
            // Silently fail
        }
    }
}
