package org.example.project.storage

expect object AuthStorage {
    fun save(token: String, username: String)
    fun load(): Pair<String, String>?
    fun clear()
}
