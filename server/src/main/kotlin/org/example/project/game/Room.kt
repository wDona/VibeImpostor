package org.example.project.game

import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import org.example.project.model.RoomConfig
import org.example.project.model.RoomState
import org.example.project.model.Role
import java.util.UUID

class Room(
    val code: String,
    val hostId: String,
    var config: RoomConfig
) {
    val mutex = Mutex()

    val players = mutableListOf<Player>()
    var state: RoomState = RoomState.LOBBY

    var word: String? = null
    var category: String? = null
    var wordHints: List<String> = emptyList()
    var impostorIds: Set<String> = emptySet()
    var turnOrder: List<String> = emptyList()
    var currentTurnIndex: Int = 0
    var roundNumber: Int = 1
    var playedThisRound: Set<String> = emptySet()
    var wantVoteResponses: Map<String, Boolean> = emptyMap()
    var votes: Map<String, String> = emptyMap()
    var lastRoundVotes: Map<String, String> = emptyMap()
    var endGameResponses: Map<String, Boolean> = emptyMap()
    var impostorGuesses: Map<String, Boolean> = emptyMap()
    var continueResponses: Set<String> = emptySet()
    var pendingGuessImpostorId: String? = null
    var lastWinners: List<String> = emptyList()
    var rematchJob: Job? = null
    var voteTimerJob: Job? = null
    var isInPunishmentRound: Boolean = false
    var punishmentPlayerId: String? = null
    var voteTypes: Map<String, Boolean> = emptyMap()

    fun getPublicPlayers() = players.map {
        org.example.project.model.PublicPlayer(
            id = it.id,
            name = it.name,
            score = it.score,
            connected = it.connected,
            isHost = it.isHost,
            waitingNextGame = it.waitingNextGame,
            isSpectator = it.isSpectator,
            wantsRematch = it.wantsRematch
        )
    }

    fun isLobby() = state == RoomState.LOBBY
    fun isGameRunning() = state == RoomState.IN_GAME ||
        state == RoomState.ASK_VOTE ||
        state == RoomState.VOTING
    fun activePlayers() = players.filter { it.connected && !it.isSpectator && !it.waitingNextGame }
    fun allPlayers() = players.filter { it.connected }
    fun currentTurnPlayer(): Player? {
        if (turnOrder.isEmpty() || currentTurnIndex >= turnOrder.size) return null
        val playerId = turnOrder[currentTurnIndex]
        return players.find { it.id == playerId }
    }

    fun roundIsComplete(): Boolean {
        val active = activePlayers()
        return active.isNotEmpty() && active.all { it.id in playedThisRound }
    }

    fun nextTurnIndex() {
        currentTurnIndex++
        while (currentTurnIndex < turnOrder.size) {
            val player = players.find { it.id == turnOrder[currentTurnIndex] }
            if (player != null && player.connected && !player.isSpectator) break
            currentTurnIndex++
        }
        if (currentTurnIndex >= turnOrder.size) {
            currentTurnIndex = 0
        }
    }

    fun resetForNewRound() {
        roundNumber++
        playedThisRound = emptySet()
        currentTurnIndex = 0
        wantVoteResponses = emptyMap()
        votes = emptyMap()
        endGameResponses = emptyMap()
        continueResponses = emptySet()
        pendingGuessImpostorId = null
        impostorGuesses = emptyMap()
        isInPunishmentRound = false
        punishmentPlayerId = null
        voteTypes = emptyMap()

        while (currentTurnIndex < turnOrder.size) {
            val player = players.find { it.id == turnOrder[currentTurnIndex] }
            if (player != null && player.connected && !player.isSpectator) break
            currentTurnIndex++
        }
        if (currentTurnIndex >= turnOrder.size) {
            currentTurnIndex = 0
        }
    }
}
