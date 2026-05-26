package org.example.project.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.example.project.model.Role
import org.example.project.model.RoomConfig
import org.example.project.model.RoomSnapshot

@Serializable
sealed interface ClientMessage {
    @Serializable
    data class CreateRoom(
        val playerName: String,
        val authToken: String? = null
    ) : ClientMessage

    @Serializable
    data class JoinRoom(
        val roomCode: String,
        val playerName: String,
        val authToken: String? = null
    ) : ClientMessage

    @Serializable
    data object LeaveRoom : ClientMessage

    @Serializable
    data class UpdateConfig(
        val config: RoomConfig
    ) : ClientMessage

    @Serializable
    data object StartGame : ClientMessage

    @Serializable
    data object EndTurn : ClientMessage

    @Serializable
    data class SubmitWord(
        val text: String
    ) : ClientMessage

    @Serializable
    data class AnswerWantVote(
        val wantsToVote: Boolean
    ) : ClientMessage

    @Serializable
    data class CastVote(
        val targetPlayerId: String
    ) : ClientMessage

    @Serializable
    data class AnswerEndGame(
        val agrees: Boolean
    ) : ClientMessage

    @Serializable
    data object BackToLobby : ClientMessage

    @Serializable
    data object RequestRematch : ClientMessage

    @Serializable
    data class SubmitImpostorGuess(
        val word: String
    ) : ClientMessage

    @Serializable
    data object ContinueRound : ClientMessage
}

@Serializable
sealed interface ServerMessage {
    @Serializable
    data class Joined(
        val yourPlayerId: String,
        val room: RoomSnapshot
    ) : ServerMessage

    @Serializable
    data class RoomUpdated(
        val room: RoomSnapshot
    ) : ServerMessage

    @Serializable
    data class GameStarted(
        val yourRole: Role,
        val contentIsWord: Boolean,
        val content: String,
        val room: RoomSnapshot
    ) : ServerMessage

    @Serializable
    data class TurnChanged(
        val currentTurnPlayerId: String,
        val roundNumber: Int
    ) : ServerMessage

    @Serializable
    data class WordPlayed(
        val playerId: String,
        val word: String
    ) : ServerMessage

    @Serializable
    data class AskWantVote(
        val deadlineEpochMs: Long
    ) : ServerMessage

    @Serializable
    data class VotingStarted(
        val candidateIds: List<String>,
        val deadlineEpochMs: Long
    ) : ServerMessage

    @Serializable
    data class VotingResult(
        val ejectedPlayerId: String?,
        val wasImpostor: Boolean,
        val room: RoomSnapshot,
        val votes: Map<String, String> = emptyMap()
    ) : ServerMessage

    @Serializable
    data class RoundContinues(
        val roundNumber: Int,
        val room: RoomSnapshot
    ) : ServerMessage

    @Serializable
    data class ProceedToGuessing(
        val room: RoomSnapshot
    ) : ServerMessage

    @Serializable
    data class GameEnded(
        val winnerIds: List<String>,
        val room: RoomSnapshot
    ) : ServerMessage

    @Serializable
    data class ErrorMessage(
        val text: String
    ) : ServerMessage

    @Serializable
    data class RematchStarted(
        val room: RoomSnapshot
    ) : ServerMessage

    @Serializable
    data class ReturnedToLobby(
        val room: RoomSnapshot
    ) : ServerMessage

    @Serializable
    data class RemovedFromRoom(
        val reason: String
    ) : ServerMessage

    @Serializable
    data class EndGameProposed(
        val agreedPlayerIds: List<String>,
        val totalActive: Int
    ) : ServerMessage

    @Serializable
    data object EndGameCancelled : ServerMessage

    @Serializable
    data class VoteCast(val voterId: String) : ServerMessage
}

object ProtocolJson {
    val json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
    }
}
