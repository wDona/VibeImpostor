package org.example.project.settings

actual object SettingsStorage {
    actual suspend fun load(): UserSettings {
        return UserSettings()
    }

    actual suspend fun save(settings: UserSettings) {
        // WasmJs doesn't support localStorage yet, settings not persisted
    }
}
