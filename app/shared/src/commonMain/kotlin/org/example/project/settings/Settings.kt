package org.example.project.settings

import kotlinx.serialization.Serializable

@Serializable
data class UserSettings(
    val language: String = "es",
    val soundEnabled: Boolean = true,
    val theme: Theme = Theme.SYSTEM
)

enum class Theme {
    LIGHT, DARK, SYSTEM
}

expect object SettingsStorage {
    suspend fun load(): UserSettings
    suspend fun save(settings: UserSettings)
}
