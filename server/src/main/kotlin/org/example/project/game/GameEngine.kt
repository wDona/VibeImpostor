package org.example.project.game

import org.example.project.db.WordRepository
import org.example.project.model.Role
import org.example.project.model.RoomState
import kotlin.random.Random

object GameEngine {
    suspend fun startGame(room: Room): String? {
        if (!room.isLobby() || room.activePlayers().size < 3) return "Se necesitan al menos 3 jugadores"

        room.mutex.lock()
        try {
            room.players.forEach {
                it.waitingNextGame = false
                it.isSpectator = false
                it.wantsRematch = false
                it.role = null
                it.word = null
                it.category = null
                it.lastWord = null
            }

            val word = WordRepository.randomWordFrom(room.config.selectedCategoryIds)
                ?: return "No hay palabras disponibles"

            room.word = word.first
            room.category = word.second

            val active = room.activePlayers()
            val numImpostors = when {
                room.config.impostorMode.ordinal == 1 -> Random.nextInt(1, maxOf(2, active.size))
                room.config.allCanBeImpostor -> Random.nextInt(1, maxOf(2, active.size))
                else -> room.config.numImpostors
            }

            room.impostorIds = active.shuffled().take(numImpostors).map { it.id }.toSet()
            room.turnOrder = active.map { it.id }
            room.currentTurnIndex = 0
            room.roundNumber = 1
            room.playedThisRound = emptySet()
            room.wantVoteResponses = emptyMap()
            room.votes = emptyMap()
            room.endGameResponses = emptyMap()

            room.state = RoomState.IN_GAME

            return null
        } finally {
            room.mutex.unlock()
        }
    }

    suspend fun endTurn(room: Room, playerId: String): RoomState? {
        room.mutex.lock()
        try {
            if (room.currentTurnPlayer()?.id != playerId) return null

            room.playedThisRound = room.playedThisRound + playerId

            val allActive = room.activePlayers().map { it.id }.toSet()
            if (room.playedThisRound == allActive) {
                room.state = RoomState.ASK_VOTE
                room.wantVoteResponses = emptyMap()
                return RoomState.ASK_VOTE
            }

            room.nextTurnIndex()
            return RoomState.IN_GAME
        } finally {
            room.mutex.unlock()
        }
    }

    suspend fun answerWantVote(room: Room, playerId: String, wantsVote: Boolean) {
        room.mutex.lock()
        try {
            room.wantVoteResponses = room.wantVoteResponses + (playerId to wantsVote)
        } finally {
            room.mutex.unlock()
        }
    }

    suspend fun checkVotingStart(room: Room): Boolean {
        room.mutex.lock()
        try {
            val active = room.activePlayers().map { it.id }.toSet()
            if (room.wantVoteResponses.keys.size < active.size) return false

            val yesCount = room.wantVoteResponses.values.count { it }
            val shouldVote = yesCount > active.size / 2

            if (shouldVote) {
                room.state = RoomState.VOTING
                room.votes = emptyMap()
            } else {
                room.impostorIds.forEach { id ->
                    val player = room.players.find { it.id == id }
                    if (player != null) player.score++
                }
                room.resetForNewRound()
                room.state = RoomState.IN_GAME
            }

            return shouldVote
        } finally {
            room.mutex.unlock()
        }
    }

    suspend fun castVote(room: Room, voterId: String, targetId: String) {
        room.mutex.lock()
        try {
            room.votes = room.votes + (voterId to targetId)
        } finally {
            room.mutex.unlock()
        }
    }

    suspend fun checkVotingEnd(room: Room): Pair<String?, Boolean>? {
        room.mutex.lock()
        try {
            val active = room.activePlayers().map { it.id }
            val voted = room.votes.keys

            if (voted.size < active.size) return null

            val nonVoters = active.filterNot { it in voted }
            val finalVotes = room.votes.toMutableMap()

            if (nonVoters.isNotEmpty()) {
                val voteCountByTarget = room.votes.values.groupingBy { it }.eachCount()
                val maxTarget = voteCountByTarget.maxByOrNull { it.value }?.key
                if (maxTarget != null) {
                    nonVoters.forEach { finalVotes[it] = maxTarget }
                }
            }

            val voteCountByTarget = finalVotes.values.groupingBy { it }.eachCount()
            val maxCount = voteCountByTarget.maxOf { it.value }
            val tied = voteCountByTarget.filter { it.value == maxCount }.keys

            val ejected = if (tied.size == 1) tied.first() else tied.random()
            val wasImpostor = ejected in room.impostorIds

            if (wasImpostor) {
                val winners = room.activePlayers().filterNot { it.id in room.impostorIds }
                winners.forEach { it.score++ }
                room.state = RoomState.FINISHED
                room.players.forEach { it.isSpectator = false }
                room.roundNumber = 1
                return Pair(ejected, true)
            } else {
                val ejectedPlayer = room.players.find { it.id == ejected }
                if (ejectedPlayer != null) ejectedPlayer.isSpectator = true

                room.impostorIds.forEach { id ->
                    val player = room.players.find { it.id == id }
                    if (player != null) player.score++
                }

                val activeNow = room.activePlayers()
                val activeImpostors = activeNow.count { it.id in room.impostorIds }
                val activeInnocents = activeNow.size - activeImpostors
                if (activeInnocents <= activeImpostors) {
                    room.state = RoomState.FINISHED
                    room.players.forEach { it.isSpectator = false }
                    room.roundNumber = 1
                    return Pair(ejected, false)
                }

                room.resetForNewRound()
                room.state = RoomState.IN_GAME
                return Pair(ejected, false)
            }
        } finally {
            room.mutex.unlock()
        }
    }

    suspend fun submitWord(room: Room, playerId: String, word: String): RoomState? {
        val player = room.players.find { it.id == playerId } ?: return null
        player.lastWord = word

        return endTurn(room, playerId)
    }

    suspend fun checkRoomValid(room: Room): Boolean {
        room.mutex.lock()
        try {
            val active = room.activePlayers().size
            if (active < 3) {
                if (room.isGameRunning()) room.state = RoomState.FINISHED
                if (room.isLobby()) return false
            }
            return true
        } finally {
            room.mutex.unlock()
        }
    }
}
