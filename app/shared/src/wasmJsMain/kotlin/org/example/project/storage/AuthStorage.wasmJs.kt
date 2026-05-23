package org.example.project.storage

import kotlinx.browser.localStorage
import org.w3c.dom.set
import org.w3c.dom.get

actual object AuthStorage {
    actual fun save(token: String, username: String) {
        localStorage["auth_token"] = token
        localStorage["auth_username"] = username
    }
    actual fun load(): Pair<String, String>? {
        val t = localStorage["auth_token"] ?: return null
        val u = localStorage["auth_username"] ?: return null
        return t to u
    }
    actual fun clear() {
        localStorage.removeItem("auth_token")
        localStorage.removeItem("auth_username")
    }
}
