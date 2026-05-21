package org.example.project.game

import io.ktor.websocket.WebSocketSession
import org.example.project.model.Role

data class Player(
    val id: String,
    val name: String,
    val session: WebSocketSession,
    val userId: Int? = null,
    var score: Int = 0,
    var connected: Boolean = true,
    var isHost: Boolean = false,
    var waitingNextGame: Boolean = false,
    var isSpectator: Boolean = false,
    var role: Role? = null,
    var word: String? = null,
    var category: String? = null,
    var lastWord: String? = null,
    var wantsRematch: Boolean = false
)
