package org.example.project.settings

import kotlinx.serialization.json.Json
import java.io.File

actual object SettingsStorage {
    private val settingsFile = File(System.getProperty("user.home"), ".impostor_settings.json")

    actual suspend fun load(): UserSettings {
        return if (settingsFile.exists()) {
            try {
                val json = settingsFile.readText()
                Json.decodeFromString<UserSettings>(json)
            } catch (e: Exception) {
                UserSettings()
            }
        } else {
            UserSettings()
        }
    }

    actual suspend fun save(settings: UserSettings) {
        settingsFile.parentFile?.mkdirs()
        settingsFile.writeText(Json.encodeToString(UserSettings.serializer(), settings))
    }
}
