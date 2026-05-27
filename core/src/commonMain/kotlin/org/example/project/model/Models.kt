package org.example.project.model

import kotlinx.serialization.Serializable

const val MIN_PLAYERS = 3
const val MAX_PLAYERS = 16

@Serializable
enum class RoomState {
    LOBBY,
    IN_GAME,
    ASK_VOTE,
    VOTING,
    IMPOSTORS_GUESSING,
    FINISHED,
    REMATCH
}

@Serializable
enum class Role {
    INNOCENT,
    IMPOSTOR
}

@Serializable
enum class GameMode {
    VOICE,
    TEXT
}

@Serializable
enum class ImpostorMode {
    FIXED,
    RANDOM
}

@Serializable
data class PublicPlayer(
    val id: String,
    val name: String,
    val score: Int,
    val connected: Boolean,
    val isHost: Boolean,
    val waitingNextGame: Boolean,
    val isSpectator: Boolean,
    val wantsRematch: Boolean = false
)

@Serializable
data class RoomConfig(
    val gameMode: GameMode = GameMode.VOICE,
    val numImpostors: Int = 1,
    val impostorMode: ImpostorMode = ImpostorMode.FIXED,
    val allCanBeImpostor: Boolean = false,
    val voteTimeLimitSeconds: Int = 60,
    val selectedCategoryIds: List<Int> = emptyList(),
    val language: String = "es",
    val winOnFirstEjection: Boolean = false,
    val anonymousVotes: Boolean = false,
    val singleWordRound: Boolean = false,
    val noCategory: Boolean = false,
    val hiddenRole: Boolean = false
)

@Serializable
data class RoomSnapshot(
    val code: String,
    val state: RoomState,
    val config: RoomConfig,
    val players: List<PublicPlayer>,
    val hostId: String,
    val currentTurnPlayerId: String?,
    val turnOrder: List<String>,
    val roundNumber: Int,
    val impostorIds: Set<String> = emptySet(),
    val impostorGuesses: Map<String, Boolean> = emptyMap(),
    val pendingGuessImpostorId: String? = null,
    val lastWinners: List<String> = emptyList(),
    val continueResponses: Set<String> = emptySet()
)
