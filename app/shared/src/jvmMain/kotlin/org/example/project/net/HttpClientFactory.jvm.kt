package org.example.project.net

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpTimeoutConfig

actual fun createHttpClient(): HttpClient = HttpClient(CIO) {
    install(WebSockets)
    install(HttpTimeout) {
        requestTimeoutMillis = 10_000
        connectTimeoutMillis = 5_000
        socketTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS // No timeout para WebSocket
    }
}

