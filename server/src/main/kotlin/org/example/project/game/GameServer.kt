package org.example.project.game

import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.WebSocketSession
import kotlinx.coroutines.delay
import org.example.project.model.RoomConfig
import org.example.project.model.RoomSnapshot
import org.example.project.model.RoomState
import org.example.project.protocol.ClientMessage
import org.example.project.protocol.ProtocolJson
import org.example.project.protocol.ServerMessage
import java.util.UUID

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
                            sendServerMessage(ServerMessage.ErrorMessage("Room not found"))
                            continue
                        }

                        room = targetRoom
                        val waitingNextGame = !targetRoom.isLobby()
                        player = Player(playerId, message.playerName, this, waitingNextGame = waitingNextGame)
                        room.players.add(player)

                        val snapshot = room.getRoomSnapshot()
                        sendServerMessage(ServerMessage.Joined(playerId, snapshot))
                        broadcastRoomUpdate(room)
                    }

                    is ClientMessage.LeaveRoom -> {
                        if (room != null && player != null) {
                            leaveRoom(room, player)
                        }
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
                                val started = ServerMessage.GameStarted(
                                    yourRole = if (player.id in room.impostorIds) org.example.project.model.Role.IMPOSTOR else org.example.project.model.Role.INNOCENT,
                                    contentIsWord = player.id !in room.impostorIds,
                                    content = if (player.id in room.impostorIds) room.category!! else room.word!!,
                                    room = room.getRoomSnapshot()
                                )
                                room.players.forEach { p ->
                                    if (p.id != player.id) {
                                        val role = if (p.id in room.impostorIds) org.example.project.model.Role.IMPOSTOR else org.example.project.model.Role.INNOCENT
                                        val content = if (p.id in room.impostorIds) room.category!! else room.word!!
                                        val msg = ServerMessage.GameStarted(role, p.id !in room.impostorIds, content, room.getRoomSnapshot())
                                        sendToPlayer(room, p.id, msg)
                                    }
                                }
                                sendServerMessage(started)

                                val current = room.currentTurnPlayer()
                                if (current != null) {
                                    broadcastServerMessage(room, ServerMessage.TurnChanged(current.id, room.roundNumber))
                                }
                            }
                        }
                    }

                    is ClientMessage.EndTurn -> {
                        if (room != null && player != null) {
                            val newState = GameEngine.endTurn(room, player.id)
                            if (newState == RoomState.ASK_VOTE) {
                                val deadline = System.currentTimeMillis() + (room.config.voteTimeLimitSeconds * 1000L)
                                broadcastServerMessage(room, ServerMessage.AskWantVote(deadline))
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
                                broadcastServerMessage(room, ServerMessage.AskWantVote(deadline))
                            } else if (newState == RoomState.IN_GAME) {
                                val current = room.currentTurnPlayer()
                                if (current != null) {
                                    broadcastServerMessage(room, ServerMessage.TurnChanged(current.id, room.roundNumber))
                                }
                            }
                        }
                    }

                    is ClientMessage.AnswerWantVote -> {
                        if (room != null && player != null) {
                            GameEngine.answerWantVote(room, player.id, message.wantsToVote)
                            val shouldVote = GameEngine.checkVotingStart(room)
                            if (shouldVote) {
                                val deadline = System.currentTimeMillis() + (room.config.voteTimeLimitSeconds * 1000L)
                                val candidates = room.activePlayers().map { it.id }
                                broadcastServerMessage(room, ServerMessage.VotingStarted(candidates, deadline))
                            }
                        }
                    }

                    is ClientMessage.CastVote -> {
                        if (room != null && player != null) {
                            GameEngine.castVote(room, player.id, message.targetPlayerId)
                            val result = GameEngine.checkVotingEnd(room)
                            if (result != null) {
                                val (ejectedId, wasImpostor) = result
                                broadcastServerMessage(room, ServerMessage.VotingResult(ejectedId, wasImpostor, room.getRoomSnapshot()))

                                if (!wasImpostor) {
                                    delay(1000)
                                    broadcastServerMessage(room, ServerMessage.RoundContinues(room.roundNumber, room.getRoomSnapshot()))
                                    val current = room.currentTurnPlayer()
                                    if (current != null) {
                                        broadcastServerMessage(room, ServerMessage.TurnChanged(current.id, room.roundNumber))
                                    }
                                } else {
                                    broadcastServerMessage(room, ServerMessage.GameEnded(room.activePlayers().filterNot { it.id in room.impostorIds }.map { it.id }, room.getRoomSnapshot()))
                                }
                            }
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
    room.mutex.lock()
    try {
        room.players.remove(player)

        if (player.isHost && room.players.isNotEmpty()) {
            room.players[0].isHost = true
        }

        if (room.isGameRunning() && room.currentTurnPlayer()?.id == player.id) {
            room.nextTurnIndex()
        }

        if (room.activePlayers().size < 3 && room.isGameRunning()) {
            room.state = RoomState.FINISHED
        }

        if (room.players.isEmpty()) {
            RoomManager.deleteRoom(room.code)
        } else {
            broadcastRoomUpdate(room)
        }
    } finally {
        room.mutex.unlock()
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
