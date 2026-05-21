package org.example.project.game

import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.WebSocketSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.project.model.Role
import org.example.project.model.RoomConfig
import org.example.project.model.RoomSnapshot
import org.example.project.model.RoomState
import org.example.project.model.MAX_PLAYERS
import org.example.project.model.MIN_PLAYERS
import org.example.project.protocol.ClientMessage
import org.example.project.protocol.ProtocolJson
import org.example.project.protocol.ServerMessage
import java.util.UUID

private const val ROUND_RESULT_DELAY_MS = 12_000L

fun Route.gameServer() {
    webSocket("/ws/game") {
        val playerId = UUID.randomUUID().toString()
        var room: Room? = null
        var player: Player? = null

        try {
            for (frame in incoming) {
                if (frame !is Frame.Text) continue

                val message = try {
                    ProtocolJson.json.decodeFromString<ClientMessage>(frame.readText())
                } catch (_: Exception) {
                    sendServerMessage(ServerMessage.ErrorMessage("Invalid message"))
                    continue
                }

                when (message) {
                    is ClientMessage.CreateRoom -> {
                        val config = RoomConfig()
                        val newRoom = RoomManager.createRoom(playerId, config)
                        room = newRoom
                        player = Player(playerId, message.playerName, this, isHost = true)
                        room.players.add(player)

                        val snapshot = room.getRoomSnapshot()
                        sendServerMessage(ServerMessage.Joined(playerId, snapshot))
                        broadcastRoomUpdate(room)
                    }

                    is ClientMessage.JoinRoom -> {
                        val targetRoom = RoomManager.findRoom(message.roomCode)
                        if (targetRoom == null) {
                            sendServerMessage(ServerMessage.ErrorMessage("Sala no encontrada"))
                            continue
                        }

                        targetRoom.mutex.lock()
                        val isFull = targetRoom.players.size >= MAX_PLAYERS
                        try {
                            if (!isFull) {
                                val waitingNextGame = !targetRoom.isLobby()
                                player = Player(playerId, message.playerName, this, waitingNextGame = waitingNextGame)
                                targetRoom.players.add(player)
                            }
                        } finally {
                            targetRoom.mutex.unlock()
                        }

                        if (isFull) {
                            sendServerMessage(ServerMessage.ErrorMessage("La sala está llena (máximo $MAX_PLAYERS jugadores)"))
                            continue
                        }

                        room = targetRoom
                        val snapshot = room.getRoomSnapshot()
                        sendServerMessage(ServerMessage.Joined(playerId, snapshot))
                        broadcastRoomUpdate(room)
                    }

                    is ClientMessage.LeaveRoom -> {
                        val r = room
                        val p = player
                        if (r != null && p != null) {
                            leaveRoom(r, p)
                        }
                        room = null
                        player = null
                        close()
                    }

                    is ClientMessage.UpdateConfig -> {
                        if (room != null && player?.isHost == true && room.state == RoomState.LOBBY) {
                            room.config = message.config
                            broadcastRoomUpdate(room)
                        }
                    }

                    is ClientMessage.StartGame -> {
                        if (room != null && player?.isHost == true) {
                            val error = GameEngine.startGame(room)
                            if (error != null) {
                                sendServerMessage(ServerMessage.ErrorMessage(error))
                            } else {
                                broadcastGameStarted(room)
                            }
                        }
                    }

                    is ClientMessage.EndTurn -> {
                        if (room != null && player != null) {
                            val newState = GameEngine.endTurn(room, player.id)
                            if (newState == RoomState.ASK_VOTE) {
                                val deadline = System.currentTimeMillis() + (room.config.voteTimeLimitSeconds * 1000L)
                                broadcastToActivePlayers(room, ServerMessage.AskWantVote(deadline))
                                startVoteTimer(room)
                            } else if (newState == RoomState.IN_GAME) {
                                val current = room.currentTurnPlayer()
                                if (current != null) {
                                    broadcastServerMessage(room, ServerMessage.TurnChanged(current.id, room.roundNumber))
                                }
                            }
                        }
                    }

                    is ClientMessage.SubmitWord -> {
                        if (room != null && player != null) {
                            val newState = GameEngine.submitWord(room, player.id, message.text)
                            broadcastServerMessage(room, ServerMessage.WordPlayed(player.id, message.text))

                            if (newState == RoomState.ASK_VOTE) {
                                val deadline = System.currentTimeMillis() + (room.config.voteTimeLimitSeconds * 1000L)
                                broadcastToActivePlayers(room, ServerMessage.AskWantVote(deadline))
                                startVoteTimer(room)
                            } else if (newState == RoomState.IN_GAME) {
                                val current = room.currentTurnPlayer()
                                if (current != null) {
                                    broadcastServerMessage(room, ServerMessage.TurnChanged(current.id, room.roundNumber))
                                }
                            }
                        }
                    }

                    is ClientMessage.AnswerWantVote -> {
                        if (room != null && player != null && !player.isSpectator) {
                            GameEngine.answerWantVote(room, player.id, message.wantsToVote)
                            GameEngine.checkVotingStart(room)
                            afterAskVoteResolved(room)
                        }
                    }

                    is ClientMessage.CastVote -> {
                        if (room != null && player != null && !player.isSpectator) {
                            GameEngine.castVote(room, player.id, message.targetPlayerId)
                            val result = GameEngine.checkVotingEnd(room)
                            if (result != null) {
                                afterVotingResolved(room, result)
                            }
                        }
                    }

                    is ClientMessage.BackToLobby -> {
                        val r = room
                        if (r != null && player != null) {
                            r.mutex.lock()
                            try {
                                if (r.state == RoomState.FINISHED) {
                                    r.state = RoomState.LOBBY
                                    r.players.forEach {
                                        it.isSpectator = false
                                        it.waitingNextGame = false
                                        it.wantsRematch = false
                                    }
                                }
                            } finally {
                                r.mutex.unlock()
                            }
                            broadcastServerMessage(r, ServerMessage.ReturnedToLobby(r.getRoomSnapshot()))
                        }
                    }

                    is ClientMessage.RequestRematch -> {
                        val r = room
                        val p = player
                        if (r != null && p != null) {
                            handleRequestRematch(r, p)
                        }
                    }

                    is ClientMessage.AnswerEndGame -> {
                        val r = room
                        val p = player
                        if (r != null && p != null) {
                            handleAnswerEndGame(r, p, message.agrees)
                        }
                    }

                    else -> {}
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (room != null && player != null) {
                leaveRoom(room, player)
            }
        }
    }
}

private suspend fun leaveRoom(room: Room, player: Player) {
    var shouldFinishRematch = false
    var roomDeleted = false
    room.mutex.lock()
    try {
        room.players.remove(player)

        if (player.isHost && room.players.isNotEmpty()) {
            room.players[0].isHost = true
        }

        if (room.isGameRunning() && room.currentTurnPlayer()?.id == player.id) {
            room.nextTurnIndex()
        }

        if (room.activePlayers().size < MIN_PLAYERS && room.isGameRunning()) {
            room.state = RoomState.FINISHED
            room.players.forEach { it.isSpectator = false }
            room.roundNumber = 1
        }

        if (room.state == RoomState.REMATCH) {
            val connected = room.players.filter { it.connected }
            if (connected.isNotEmpty() && connected.all { it.wantsRematch }) {
                shouldFinishRematch = true
            }
        }

        if (room.players.isEmpty()) {
            room.rematchJob?.cancel()
            RoomManager.deleteRoom(room.code)
            roomDeleted = true
        }
    } finally {
        room.mutex.unlock()
    }

    if (roomDeleted) return

    if (shouldFinishRematch) {
        room.rematchJob?.cancel()
        finishRematch(room)
        return
    }

    handleGameProgressAfterLeave(room)
    broadcastRoomUpdate(room)
}

private suspend fun handleGameProgressAfterLeave(room: Room) {
    when (room.state) {
        RoomState.IN_GAME -> {
            val newState = GameEngine.recheckRound(room)
            if (newState == RoomState.ASK_VOTE) {
                val deadline = System.currentTimeMillis() + (room.config.voteTimeLimitSeconds * 1000L)
                broadcastToActivePlayers(room, ServerMessage.AskWantVote(deadline))
            } else {
                val current = room.currentTurnPlayer()
                if (current != null) {
                    broadcastServerMessage(room, ServerMessage.TurnChanged(current.id, room.roundNumber))
                }
            }
        }

        RoomState.ASK_VOTE -> {
            GameEngine.checkVotingStart(room)
            afterAskVoteResolved(room)
        }

        RoomState.VOTING -> {
            val result = GameEngine.checkVotingEnd(room)
            if (result != null) {
                afterVotingResolved(room, result)
            }
        }

        RoomState.FINISHED -> {
            broadcastServerMessage(room, ServerMessage.GameEnded(emptyList(), room.getRoomSnapshot()))
        }

        else -> {}
    }
}

private suspend fun broadcastRoomUpdate(room: Room) {
    val msg = ServerMessage.RoomUpdated(room.getRoomSnapshot())
    room.players.forEach { player ->
        sendToPlayer(room, player.id, msg)
    }
}

private suspend fun broadcastServerMessage(room: Room, msg: ServerMessage) {
    room.players.forEach { player ->
        sendToPlayer(room, player.id, msg)
    }
}

private suspend fun broadcastToActivePlayers(room: Room, msg: ServerMessage) {
    room.activePlayers().forEach { player ->
        sendToPlayer(room, player.id, msg)
    }
}

private suspend fun sendToPlayer(room: Room, playerId: String, msg: ServerMessage) {
    val player = room.players.find { it.id == playerId } ?: return
    try {
        player.session.sendServerMessage(msg)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private suspend fun WebSocketSession.sendServerMessage(msg: ServerMessage) {
    send(Frame.Text(ProtocolJson.json.encodeToString(ServerMessage.serializer(), msg)))
}

private fun Room.getRoomSnapshot(): RoomSnapshot {
    return RoomSnapshot(
        code = this.code,
        state = this.state,
        config = this.config,
        players = this.getPublicPlayers(),
        hostId = this.players.find { it.isHost }?.id ?: "",
        currentTurnPlayerId = this.currentTurnPlayer()?.id,
        turnOrder = this.turnOrder,
        roundNumber = this.roundNumber
    )
}

private suspend fun broadcastGameStarted(room: Room) {
    room.players.forEach { p ->
        val isImpostor = p.id in room.impostorIds
        val role = if (isImpostor) Role.IMPOSTOR else Role.INNOCENT
        val content = if (isImpostor) room.category!! else room.word!!
        val msg = ServerMessage.GameStarted(role, !isImpostor, content, room.getRoomSnapshot())
        sendToPlayer(room, p.id, msg)
    }
    val current = room.currentTurnPlayer()
    if (current != null) {
        broadcastServerMessage(room, ServerMessage.TurnChanged(current.id, room.roundNumber))
    }
}

private suspend fun handleRequestRematch(room: Room, player: Player) {
    var justStarted = false
    var startNow = false
    var ignore = false
    room.mutex.lock()
    try {
        when (room.state) {
            RoomState.FINISHED -> {
                room.state = RoomState.REMATCH
                room.players.forEach { it.wantsRematch = false }
                player.wantsRematch = true
                justStarted = true
            }
            RoomState.REMATCH -> {
                player.wantsRematch = true
            }
            else -> {
                ignore = true
            }
        }
        if (!ignore) {
            val connected = room.players.filter { it.connected }
            if (connected.isNotEmpty() && connected.all { it.wantsRematch }) {
                startNow = true
            }
        }
    } finally {
        room.mutex.unlock()
    }

    if (ignore) return

    if (justStarted) {
        broadcastServerMessage(room, ServerMessage.RematchStarted(room.getRoomSnapshot()))
        room.rematchJob = RoomManager.scope.launch {
            delay(20_000L)
            finishRematch(room)
        }
    } else {
        broadcastServerMessage(room, ServerMessage.RoomUpdated(room.getRoomSnapshot()))
    }

    if (startNow) {
        room.rematchJob?.cancel()
        finishRematch(room)
    }
}

private suspend fun finishRematch(room: Room) {
    val kicked = mutableListOf<Player>()
    var canStart = false
    room.mutex.lock()
    try {
        if (room.state != RoomState.REMATCH) return
        kicked.addAll(room.players.filter { !it.wantsRematch })
        room.players.removeAll { !it.wantsRematch }
        room.players.forEach {
            it.isSpectator = false
            it.waitingNextGame = false
        }
        if (room.players.isNotEmpty() && room.players.none { it.isHost }) {
            room.players[0].isHost = true
        }
        room.state = RoomState.LOBBY
        canStart = room.players.count { it.connected } >= 3
    } finally {
        room.mutex.unlock()
    }

    kicked.forEach { p ->
        try {
            p.session.sendServerMessage(
                ServerMessage.RemovedFromRoom("No te uniste a la siguiente partida")
            )
            p.session.close()
        } catch (_: Exception) {
        }
    }

    room.rematchJob = null

    if (room.players.isEmpty()) {
        RoomManager.deleteRoom(room.code)
        return
    }

    if (canStart) {
        val error = GameEngine.startGame(room)
        if (error == null) {
            broadcastGameStarted(room)
        } else {
            broadcastServerMessage(room, ServerMessage.ReturnedToLobby(room.getRoomSnapshot()))
        }
    } else {
        broadcastServerMessage(room, ServerMessage.ReturnedToLobby(room.getRoomSnapshot()))
    }
}

private suspend fun handleAnswerEndGame(room: Room, player: Player, agrees: Boolean) {
    var cancelled = false
    var ended = false
    var proposed = false
    room.mutex.lock()
    try {
        if (!room.isGameRunning() || player.isSpectator) return
        if (!agrees) {
            room.endGameResponses = emptyMap()
            cancelled = true
        } else {
            room.endGameResponses = room.endGameResponses + (player.id to true)
            val activeIds = room.activePlayers().map { it.id }
            if (activeIds.isNotEmpty() && activeIds.all { room.endGameResponses[it] == true }) {
                room.voteTimerJob?.cancel()
                room.state = RoomState.FINISHED
                room.players.forEach { it.isSpectator = false }
                room.roundNumber = 1
                room.endGameResponses = emptyMap()
                ended = true
            } else {
                proposed = true
            }
        }
    } finally {
        room.mutex.unlock()
    }

    when {
        ended -> broadcastServerMessage(room, ServerMessage.GameEnded(emptyList(), room.getRoomSnapshot()))
        cancelled -> broadcastServerMessage(room, ServerMessage.EndGameCancelled)
        proposed -> {
            val agreedIds = room.endGameResponses.keys.toList()
            val total = room.activePlayers().size
            broadcastServerMessage(room, ServerMessage.EndGameProposed(agreedIds, total))
        }
    }
}

private suspend fun startVoteTimer(room: Room) {
    room.voteTimerJob?.cancel()
    val phase = room.state
    room.voteTimerJob = RoomManager.scope.launch {
        delay(room.config.voteTimeLimitSeconds * 1000L)
        if (room.state != phase) return@launch
        when (phase) {
            RoomState.ASK_VOTE -> {
                GameEngine.checkVotingStart(room, force = true)
                afterAskVoteResolved(room)
            }
            RoomState.VOTING -> {
                val result = GameEngine.checkVotingEnd(room, force = true)
                if (result != null) afterVotingResolved(room, result)
            }
            else -> {}
        }
    }
}

private suspend fun afterAskVoteResolved(room: Room) {
    when (room.state) {
        RoomState.VOTING -> {
            val deadline = System.currentTimeMillis() + (room.config.voteTimeLimitSeconds * 1000L)
            val candidates = room.activePlayers().map { it.id }
            broadcastToActivePlayers(room, ServerMessage.VotingStarted(candidates, deadline))
            startVoteTimer(room)
        }
        RoomState.IN_GAME -> {
            room.voteTimerJob?.cancel()
            broadcastServerMessage(room, ServerMessage.RoundContinues(room.roundNumber, room.getRoomSnapshot()))
            val current = room.currentTurnPlayer()
            if (current != null) {
                broadcastServerMessage(room, ServerMessage.TurnChanged(current.id, room.roundNumber))
            }
        }
        else -> {}
    }
}

private suspend fun afterVotingResolved(room: Room, result: Pair<String?, Boolean>) {
    room.voteTimerJob?.cancel()
    val (ejectedId, wasImpostor) = result
    broadcastServerMessage(room, ServerMessage.VotingResult(ejectedId, wasImpostor, room.getRoomSnapshot(), room.lastRoundVotes))
    if (room.state != RoomState.FINISHED) {
        delay(ROUND_RESULT_DELAY_MS)
        broadcastServerMessage(room, ServerMessage.RoundContinues(room.roundNumber, room.getRoomSnapshot()))
        val current = room.currentTurnPlayer()
        if (current != null) {
            broadcastServerMessage(room, ServerMessage.TurnChanged(current.id, room.roundNumber))
        }
    } else {
        broadcastServerMessage(room, ServerMessage.GameEnded(
            room.activePlayers().filterNot { it.id in room.impostorIds }.map { it.id },
            room.getRoomSnapshot()
        ))
    }
}
