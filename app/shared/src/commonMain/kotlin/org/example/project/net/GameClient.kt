package org.example.project.net

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.HttpMethod
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.readText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import org.example.project.protocol.ClientMessage
import org.example.project.protocol.ProtocolJson
import org.example.project.protocol.ServerMessage

class GameClient(private val baseUrl: String) {
    private val client: HttpClient = createHttpClient()
    private var currentSession: WebSocketSession? = null

    suspend fun disconnect() {
        try {
            currentSession?.close()
        } catch (_: Exception) {
            // ignore
        }
        currentSession = null
    }


    suspend fun send(message: ClientMessage) {
        currentSession?.let {
            try {
                it.send(Frame.Text(ProtocolJson.json.encodeToString(message)))
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    fun connect(firstMessage: ClientMessage): Flow<ServerMessage> = channelFlow {
        try {
            val host = baseUrl.removePrefix("http://").removePrefix("https://").substringBefore(":")
            val port = baseUrl.substringAfterLast(":").toIntOrNull() ?: 8080
            println("GameClient.connect(): connecting to $host:$port (baseUrl=$baseUrl)")
            client.webSocket(method = HttpMethod.Get, host = host, port = port, path = "/ws/game") {
                currentSession = this
                send(Frame.Text(ProtocolJson.json.encodeToString(firstMessage)))

                for (frame in incoming) {
                    if (frame !is Frame.Text) continue
                    val text = frame.readText()
                    println("GameClient: raw frame -> $text")
                    try {
                        val msg = ProtocolJson.json.decodeFromString<ServerMessage>(text)
                        try {
                            send(msg)
                        } catch (e: Throwable) {
                            // channel closed or cancelled
                            e.printStackTrace()
                        }
                    } catch (e: Throwable) {
                        e.printStackTrace()
                        // inform the UI about decode errors so it can display useful info
                        try {
                            send(ServerMessage.ErrorMessage("Decode error: ${e.message ?: "unknown"} raw=$text"))
                        } catch (_: Throwable) {
                        }
                    }
                }
                currentSession = null
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            // propagate a user-visible error message so the UI can show a snackbar
            try {
                send(ServerMessage.ErrorMessage("Connection failed: ${e.message ?: "unknown"}"))
            } catch (_: Throwable) {
            }
        }
    }
    .flowOn(Dispatchers.Default)
}
