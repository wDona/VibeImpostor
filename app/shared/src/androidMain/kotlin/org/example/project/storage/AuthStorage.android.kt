package org.example.project.storage

actual object AuthStorage {
    actual fun save(token: String, username: String) {}
    actual fun load(): Pair<String, String>? = null
    actual fun clear() {}
}
