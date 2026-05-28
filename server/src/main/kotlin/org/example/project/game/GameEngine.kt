package org.example.project.game

import org.example.project.db.WordRepository
import org.example.project.model.Role
import org.example.project.model.RoomState
import org.example.project.model.MIN_PLAYERS
import org.example.project.protocol.BOTH_IMPOSTORS_ID
import org.example.project.protocol.NOBODY_VOTE_ID
import org.example.project.protocol.PUNISHMENT_PREFIX
import org.example.project.protocol.WRONG_CLAIM_PREFIX
import kotlin.random.Random

object GameEngine {
    suspend fun startGame(room: Room): String? {
        if (!room.isLobby() || room.activePlayers().size < MIN_PLAYERS) return "Se necesitan al menos $MIN_PLAYERS jugadores"

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

            val word = WordRepository.randomWordFrom(room.config.selectedCategoryIds, room.config.language)
                ?: return "No hay palabras disponibles"

            room.word = word.first
            room.category = word.second
            room.wordHints = word.third.shuffled(Random.Default)

            val active = room.activePlayers()
            val configured = room.config.numImpostors
            val rawImpostors = when {
                configured == 1 -> 1
                configured >= 2 && Random.nextDouble() < 0.05 -> 0
                else -> Random.nextInt(1, minOf(configured + 1, active.size))
            }
            val numImpostors = rawImpostors.coerceIn(0, maxOf(1, active.size - 1))

            room.impostorIds = active.shuffled(Random.Default).take(numImpostors).map { it.id }.toSet()
            val ids = active.map { it.id }.toMutableList()
            ids.shuffle(Random.Default)
            room.turnOrder = ids.toList()
            room.currentTurnIndex = 0
            room.roundNumber = 1
            room.playedThisRound = emptySet()
            room.wantVoteResponses = emptyMap()
            room.votes = emptyMap()
            room.endGameResponses = emptyMap()
            println("[startGame] room=${room.code} turnOrder=${room.turnOrder} impostors=${room.impostorIds}")

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

            if (room.roundIsComplete()) {
                if (room.config.singleWordRound || room.isInPunishmentRound) {
                    room.state = RoomState.VOTING
                    room.votes = emptyMap()
                    return RoomState.VOTING
                }
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

    suspend fun checkVotingStart(room: Room, force: Boolean = false): Boolean {
        room.mutex.lock()
        try {
            val active = room.activePlayers().map { it.id }.toSet()
            if (!force && room.wantVoteResponses.keys.size < active.size) return false

            val yesCount = room.wantVoteResponses.values.count { it }
            val shouldVote = yesCount > active.size / 2

            if (shouldVote) {
                room.state = RoomState.VOTING
                room.votes = emptyMap()
            } else {
                room.impostorIds.forEach { id ->
                    val player = room.players.find { it.id == id }
                    if (player != null) player.score += 5
                }
                room.resetForNewRound()
                val newActive = room.activePlayers()
                if (newActive.isEmpty()) {
                    room.state = RoomState.FINISHED
                } else {
                    room.state = RoomState.IN_GAME
                }
            }

            return shouldVote
        } finally {
            room.mutex.unlock()
        }
    }

    suspend fun checkBothImpostorsVote(room: Room, voterId: String): Pair<String?, Boolean>? {
        room.mutex.lock()
        try {
            val active = room.activePlayers()
            if (active.size < 2 || room.config.numImpostors < 2) return null
            val others = active.filter { it.id != voterId }
            val allOthersAreImpostors = others.all { it.id in room.impostorIds }

            if (allOthersAreImpostors) {
                room.players.find { it.id == voterId }?.score += 5
                room.lastWinners = listOf(voterId)
                room.state = RoomState.FINISHED
                room.players.forEach { it.isSpectator = false }
                room.roundNumber = 1
                return Pair(BOTH_IMPOSTORS_ID, false)
            } else {
                val voterWasImpostor = voterId in room.impostorIds
                room.players.find { it.id == voterId }?.isSpectator = true
                room.lastRoundVotes = emptyMap()

                val activeNow = room.activePlayers()
                if (activeNow.size < MIN_PLAYERS) {
                    val activeImpostors = activeNow.filter { it.id in room.impostorIds }
                    if (activeImpostors.isNotEmpty()) {
                        activeImpostors.forEach { it.score += 5 }
                        room.lastWinners = activeImpostors.map { it.id }
                    } else {
                        activeNow.forEach { it.score += 5 }
                        room.lastWinners = activeNow.map { it.id }
                    }
                    room.state = RoomState.FINISHED
                    room.players.forEach { it.isSpectator = false }
                    room.roundNumber = 1
                } else {
                    room.resetForNewRound()
                    room.state = RoomState.IN_GAME
                }
                return Pair(WRONG_CLAIM_PREFIX + voterId, voterWasImpostor)
            }
        } finally {
            room.mutex.unlock()
        }
    }

    suspend fun castVote(room: Room, voterId: String, targetId: String, voteIsHard: Boolean = true) {
        room.mutex.lock()
        try {
            room.votes = room.votes + (voterId to targetId)
            room.voteTypes = room.voteTypes + (voterId to voteIsHard)
        } finally {
            room.mutex.unlock()
        }
    }

    fun startPunishmentRound(room: Room, accusedId: String) {
        room.isInPunishmentRound = true
        room.punishmentPlayerId = accusedId
        room.votes = emptyMap()
        room.voteTypes = emptyMap()
        room.wantVoteResponses = emptyMap()
        room.continueResponses = emptySet()
        room.playedThisRound = room.activePlayers().map { it.id }.filter { it != accusedId }.toSet()
        val accused = room.players.find { it.id == accusedId }
        val others = room.activePlayers().filter { it.id != accusedId }.map { it.id }
        room.turnOrder = listOfNotNull(accusedId.takeIf { accused != null }) + others
        room.currentTurnIndex = 0
        room.state = RoomState.IN_GAME
    }

    suspend fun checkVotingEnd(room: Room, force: Boolean = false): Pair<String?, Boolean>? {
        room.mutex.lock()
        try {
            val active = room.activePlayers().map { it.id }
            val voted = room.votes.keys

            if (!force && voted.size < active.size) return null
            if (force && voted.isEmpty() && active.isNotEmpty()) {
                room.lastRoundVotes = emptyMap()
                room.voteTypes = emptyMap()
                room.resetForNewRound()
                val newActive = room.activePlayers()
                room.state = when {
                    newActive.isEmpty() -> RoomState.FINISHED
                    room.config.singleWordRound -> RoomState.VOTING
                    else -> RoomState.IN_GAME
                }
                return Pair(null, false)
            }

            val finalVotes = room.votes.toMutableMap()

            val voteCountByTarget = finalVotes.values.groupingBy { it }.eachCount()
            val maxCount = voteCountByTarget.maxOf { it.value }
            val tied = voteCountByTarget.filter { it.value == maxCount }.keys

            val ejected = if (tied.size == 1) tied.first() else null

            room.lastRoundVotes = room.votes.toMap()

            // Nobody vote wins → instant game end
            if (ejected == NOBODY_VOTE_ID) {
                val impostorsExist = room.impostorIds.isNotEmpty()
                room.state = RoomState.FINISHED
                room.players.forEach { it.isSpectator = false }
                room.roundNumber = 1
                val nobodyVoters = room.votes.entries.filter { it.value == NOBODY_VOTE_ID }.map { it.key }
                if (impostorsExist) {
                    room.impostorIds.forEach { id -> room.players.find { it.id == id }?.score += 5 }
                    room.lastWinners = room.players.filter { it.id in room.impostorIds }.map { it.id }
                    return Pair(NOBODY_VOTE_ID, true)
                } else {
                    nobodyVoters.forEach { id -> room.players.find { it.id == id }?.score += 5 }
                    room.lastWinners = nobodyVoters
                    return Pair(NOBODY_VOTE_ID, false)
                }
            }

            if (ejected != null) {
                val wasImpostor = ejected in room.impostorIds

                // Punishment vote: if no tie and not already in punishment round → warning turn
                if (room.config.punishmentVote && !room.isInPunishmentRound && !wasImpostor) {
                    room.lastRoundVotes = room.votes.toMap()
                    // Don't eject: give them a punishment turn
                    room.state = RoomState.IN_GAME
                    return Pair(PUNISHMENT_PREFIX + ejected, false)
                }

                // Punishment round: accused gets voted again → immediate ejection, no more mercy
                val wasAlreadyPunished = room.isInPunishmentRound && ejected == room.punishmentPlayerId
                if (!wasAlreadyPunished) {
                    // Clear punishment round if different player or no punishment round
                    room.isInPunishmentRound = false
                    room.punishmentPlayerId = null
                }

                val ejectedPlayer = room.players.find { it.id == ejected }
                if (ejectedPlayer != null) ejectedPlayer.isSpectator = true

                // Win on first ejection mode
                if (room.config.winOnFirstEjection) {
                    if (wasImpostor) {
                        room.pendingGuessImpostorId = ejected
                        room.impostorGuesses = emptyMap()
                        room.state = RoomState.IMPOSTORS_GUESSING
                        println("[checkVotingEnd] winOnFirstEjection IMPOSTOR EJECTED pending=$ejected state=${room.state}")
                        return Pair(ejected, true)
                    } else {
                        room.impostorIds.forEach { id ->
                            val player = room.players.find { it.id == id }
                            if (player != null) player.score += 5
                        }
                        room.lastWinners = room.players.filter { it.id in room.impostorIds }.map { it.id }
                        room.state = RoomState.FINISHED
                        room.players.forEach { it.isSpectator = false }
                        room.roundNumber = 1
                        return Pair(ejected, false)
                    }
                }

                if (wasImpostor) {
                    room.pendingGuessImpostorId = ejected
                    room.impostorGuesses = emptyMap()
                    room.state = RoomState.IMPOSTORS_GUESSING
                    println("[checkVotingEnd] IMPOSTOR EJECTED setting pending=$ejected state=${room.state}")
                    return Pair(ejected, true)
                }

                room.impostorIds.forEach { id ->
                    val player = room.players.find { it.id == id }
                    if (player != null) player.score += 5
                }

                val activeNow = room.activePlayers()
                val activeImpostors = activeNow.count { it.id in room.impostorIds }

                if (activeNow.size <= 2) {
                    activeNow.filter { it.id in room.impostorIds }.forEach { it.score += 5 }
                    room.state = RoomState.FINISHED
                    room.players.forEach { it.isSpectator = false }
                    room.roundNumber = 1
                    room.lastWinners = room.players.filter { it.id in room.impostorIds }.map { it.id }
                    return Pair(ejected, false)
                }
            }

            room.resetForNewRound()
            room.state = if (room.config.singleWordRound) RoomState.VOTING else RoomState.IN_GAME
            return Pair(ejected, false)
        } finally {
            room.mutex.unlock()
        }
    }

    suspend fun submitWord(room: Room, playerId: String, word: String): RoomState? {
        val player = room.players.find { it.id == playerId } ?: return null
        player.lastWord = word

        return endTurn(room, playerId)
    }

    suspend fun submitImpostorGuess(room: Room, impostorId: String, guess: String) {
        room.mutex.lock()
        try {
            if (room.state != RoomState.IMPOSTORS_GUESSING) return
            if (room.pendingGuessImpostorId != impostorId) return
            val guessed = WordMatcher.matches(guess, room.word ?: "", room.config.language)
            room.impostorGuesses = room.impostorGuesses + (impostorId to guessed)
        } finally {
            room.mutex.unlock()
        }
    }

    suspend fun checkImpostorGuessingComplete(room: Room): Boolean {
        room.mutex.lock()
        try {
            if (room.state != RoomState.IMPOSTORS_GUESSING) return false
            val pending = room.pendingGuessImpostorId ?: return false
            return pending in room.impostorGuesses.keys
        } finally {
            room.mutex.unlock()
        }
    }

    suspend fun finalizeImpostorGuessing(room: Room): RoomState {
        room.mutex.lock()
        try {
            println("[finalizeImpostorGuessing] state=${room.state} pending=${room.pendingGuessImpostorId} guesses=${room.impostorGuesses}")
            if (room.state != RoomState.IMPOSTORS_GUESSING) return room.state

            val pending = room.pendingGuessImpostorId
            val guessedCorrect = pending != null && room.impostorGuesses[pending] == true

            room.pendingGuessImpostorId = null

            if (guessedCorrect) {
                room.players.filter { it.id in room.impostorIds }.forEach { it.score += 5 }
                room.state = RoomState.FINISHED
                room.players.forEach { it.isSpectator = false }
                room.roundNumber = 1
                room.lastWinners = room.players.filter { it.id in room.impostorIds }.map { it.id }
                println("[finalizeImpostorGuessing] returning FINISHED (guessed correct) lastWinners=${room.lastWinners}")
                return RoomState.FINISHED
            }

            val activeNow = room.activePlayers()
            val activeImpostors = activeNow.count { it.id in room.impostorIds }

            if (activeImpostors == 0) {
                val winners = room.players.filterNot { it.id in room.impostorIds }
                winners.forEach { it.score += 5 }
                room.state = RoomState.FINISHED
                room.players.forEach { it.isSpectator = false }
                room.roundNumber = 1
                room.lastWinners = winners.map { it.id }
                println("[finalizeImpostorGuessing] returning FINISHED (no active impostors) lastWinners=${room.lastWinners}")
                return RoomState.FINISHED
            }

            if (activeNow.size <= 2) {
                activeNow.filter { it.id in room.impostorIds }.forEach { it.score += 5 }
                room.state = RoomState.FINISHED
                room.players.forEach { it.isSpectator = false }
                room.roundNumber = 1
                room.lastWinners = room.players.filter { it.id in room.impostorIds }.map { it.id }
                println("[finalizeImpostorGuessing] returning FINISHED (<=2 vivos) lastWinners=${room.lastWinners}")
                return RoomState.FINISHED
            }

            room.resetForNewRound()
            val nextState = if (room.config.singleWordRound) RoomState.VOTING else RoomState.IN_GAME
            room.state = nextState
            println("[finalizeImpostorGuessing] returning $nextState (next round)")
            return nextState
        } finally {
            room.mutex.unlock()
        }
    }

    suspend fun recheckRound(room: Room): RoomState {
        room.mutex.lock()
        try {
            if (room.state != RoomState.IN_GAME) return room.state
            if (room.activePlayers().size < MIN_PLAYERS) {
                room.state = RoomState.FINISHED
                return RoomState.FINISHED
            }
            if (room.roundIsComplete()) {
                if (room.config.singleWordRound || room.isInPunishmentRound) {
                    room.state = RoomState.VOTING
                    room.votes = emptyMap()
                    return RoomState.VOTING
                }
                room.state = RoomState.ASK_VOTE
                room.wantVoteResponses = emptyMap()
                return RoomState.ASK_VOTE
            }
            return RoomState.IN_GAME
        } finally {
            room.mutex.unlock()
        }
    }

    suspend fun checkRoomValid(room: Room): Boolean {
        room.mutex.lock()
        try {
            val active = room.activePlayers().size
            if (active < MIN_PLAYERS) {
                if (room.isGameRunning()) room.state = RoomState.FINISHED
                if (room.isLobby()) return false
            }
            return true
        } finally {
            room.mutex.unlock()
        }
    }

    suspend fun registerContinue(room: Room, playerId: String): Boolean {
        room.mutex.lock()
        try {
            room.continueResponses = room.continueResponses + playerId
            val required = room.activePlayers().map { it.id }.toMutableSet()
            room.pendingGuessImpostorId?.let { required.add(it) }
            val complete = room.continueResponses.containsAll(required)
            println("[registerContinue] player=$playerId responses=${room.continueResponses} required=$required complete=$complete")
            return complete
        } finally {
            room.mutex.unlock()
        }
    }

    suspend fun resetContinueResponses(room: Room) {
        room.mutex.lock()
        try {
            room.continueResponses = emptySet()
        } finally {
            room.mutex.unlock()
        }
    }
}
