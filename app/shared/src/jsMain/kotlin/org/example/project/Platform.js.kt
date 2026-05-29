package org.example.project

import web.clipboard.readText
import web.navigator.navigator

class JsPlatform: Platform {
    private val userAgent = navigator.userAgent
    private val browserList = listOf("Chrome", "Firefox", "Safari", "Edge")

    override val name: String = userAgent.findAnyOf(browserList, ignoreCase = true)
            ?.let { (startIndex) -> userAgent.substring(startIndex).substringBefore(" ") }
            ?: "Unknown"
}

actual fun getPlatform(): Platform = JsPlatform()

actual fun currentTimeMillis(): Long = js("Date.now()").unsafeCast<Double>().toLong()

actual suspend fun readClipboard(): String? = try {
    navigator.clipboard.readText()
} catch (e: Throwable) {
    null
}