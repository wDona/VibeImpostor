package org.example.project.settings

import kotlinx.serialization.json.Json
import org.w3c.dom.Window

actual object SettingsStorage {
    actual suspend fun load(): UserSettings {
        return try {
            val json = js("localStorage.getItem('settings')") as? String
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
            js("localStorage.setItem('settings', json)")
        } catch (e: Exception) {
            // Silently fail
        }
    }
}
