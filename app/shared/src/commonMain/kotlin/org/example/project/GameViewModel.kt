package org.example.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import org.example.project.model.PublicPlayer
import org.example.project.model.Role
import org.example.project.model.RoomConfig
import org.example.project.model.RoomSnapshot
import org.example.project.model.RoomState
import org.example.project.net.GameClient
import org.example.project.protocol.ClientMessage
import org.example.project.protocol.ServerMessage
import org.example.project.settings.SettingsStorage
import org.example.project.settings.UserSettings

enum class Screen {
    HOME, LOBBY, GAME, VOTING, ROUND_RESULT, RESULT, SETTINGS
}

data class GameState(
    val screen: Screen = Screen.HOME,
    val room: RoomSnapshot? = null,
    val yourPlayerId: String? = null,
    val yourRole: Role? = null,
    val yourContent: String? = null,
    val contentIsWord: Boolean = true,
    val players: List<PublicPlayer> = emptyList(),
    val error: String? = null,
    val lastWordsPlayed: Map<String, String> = emptyMap(),
    val votingResult: Pair<String?, Boolean>? = null,
    val settings: UserSettings = UserSettings(),
    val askingForVote: Boolean = false,
    val voteDeadlineMs: Long = 0L
    ,
    val debugMessages: List<String> = emptyList(),
    val connecting: Boolean = false,
    val showRematchPopup: Boolean = false
)

class GameViewModel : ViewModel() {
    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state

    private var gameClient: GameClient? = null
    private var playerName: String = ""
    private val baseUrl = serverBaseUrl
    private var connectJob: Job? = null

    init {
        viewModelScope.launch {
            val settings = SettingsStorage.load()
            _state.value = _state.value.copy(settings = settings)
        }
    }

    fun setPlayerName(name: String) {
        playerName = name
    }

    fun createRoom() {
        startConnection(ClientMessage.CreateRoom(playerName, null))
    }

    fun joinRoom(code: String) {
        startConnection(ClientMessage.JoinRoom(code, playerName, null))
    }

    private fun startConnection(firstMsg: ClientMessage) {
        connectJob?.cancel()
        connectJob = viewModelScope.launch {
            _state.value = _state.value.copy(connecting = true, error = null)
            gameClient?.disconnect()
            val client = GameClient(baseUrl)
            gameClient = client

            val collectJob = launch {
                client.connect(firstMsg)
                    .catch { e ->
                        e.printStackTrace()
                        _state.value = _state.value.copy(
                            error = "Error de conexión: ${e.message ?: "desconocido"}",
                            connecting = false
                        )
                    }
                    .onCompletion {
                        if (_state.value.screen != Screen.HOME) {
                            _state.value = _state.value.copy(error = "Conexión perdida")
                        }
                    }
                    .collect { msg ->
                        _state.value = _state.value.copy(connecting = false)
                        handleServerMessage(msg)
                    }
            }

            val watchdog = launch {
                delay(10_000)
                if (_state.value.connecting) {
                    _state.value = _state.value.copy(
                        error = "La conexión tardó demasiado",
                        connecting = false
                    )
                    collectJob.cancel()
                    client.disconnect()
                }
            }

            collectJob.join()
            watchdog.cancel()
            connectJob = null
        }
    }

    fun updateConfig(config: RoomConfig) {
        viewModelScope.launch {
            val msg = ClientMessage.UpdateConfig(config)
            sendGameMessage(msg)
        }
    }

    fun startGame() {
        viewModelScope.launch {
            val msg = ClientMessage.StartGame
            sendGameMessage(msg)
        }
    }

    fun endTurn() {
        viewModelScope.launch {
            val msg = ClientMessage.EndTurn
            sendGameMessage(msg)
        }
    }

    fun submitWord(word: String) {
        viewModelScope.launch {
            val msg = ClientMessage.SubmitWord(word)
            sendGameMessage(msg)
        }
    }

    fun answerWantVote(wantsVote: Boolean) {
        viewModelScope.launch {
            val msg = ClientMessage.AnswerWantVote(wantsVote)
            sendGameMessage(msg)
        }
    }

    fun castVote(targetPlayerId: String) {
        viewModelScope.launch {
            val msg = ClientMessage.CastVote(targetPlayerId)
            sendGameMessage(msg)
        }
    }

    fun leaveRoom() {
        viewModelScope.launch {
            sendGameMessage(ClientMessage.LeaveRoom)
            _state.value = _state.value.copy(screen = Screen.HOME, room = null, yourPlayerId = null)
            connectJob?.cancel()
            connectJob = null
            gameClient?.disconnect()
            gameClient = null
        }
    }

    fun backToLobby() {
        viewModelScope.launch {
            sendGameMessage(ClientMessage.BackToLobby)
        }
    }

    fun requestRematch() {
        viewModelScope.launch {
            sendGameMessage(ClientMessage.RequestRematch)
        }
    }

    fun openSettings() {
        _state.value = _state.value.copy(screen = Screen.SETTINGS)
    }

    fun closeSettings() {
        _state.value = _state.value.copy(screen = Screen.HOME)
    }

    fun updateSettings(settings: UserSettings) {
        viewModelScope.launch {
            _state.value = _state.value.copy(settings = settings)
            SettingsStorage.save(settings)
        }
    }

    fun dismissError() {
        _state.value = _state.value.copy(error = null)
    }

    fun respondVote(wants: Boolean) {
        _state.value = _state.value.copy(askingForVote = false)
        viewModelScope.launch { sendGameMessage(ClientMessage.AnswerWantVote(wants)) }
    }

    private suspend fun sendGameMessage(msg: ClientMessage) {
        gameClient?.send(msg)
    }

    private fun handleServerMessage(msg: ServerMessage) {
        println("handleServerMessage: received -> $msg")
        val prevScreen = _state.value.screen
        // append to debug messages (keep last 20)
        val newDebug = (_state.value.debugMessages + msg.toString()).takeLast(20)
        _state.value = _state.value.copy(debugMessages = newDebug)
        when (msg) {
            is ServerMessage.Joined -> {
                val screen = when (msg.room.state) {
                    RoomState.FINISHED -> Screen.RESULT
                    RoomState.REMATCH -> Screen.RESULT
                    else -> Screen.LOBBY
                }
                _state.value = _state.value.copy(
                    screen = screen,
                    room = msg.room,
                    yourPlayerId = msg.yourPlayerId,
                    players = msg.room.players,
                    showRematchPopup = msg.room.state == RoomState.REMATCH
                )
            }

            is ServerMessage.RoomUpdated -> {
                _state.value = _state.value.copy(
                    room = msg.room,
                    players = msg.room.players,
                    showRematchPopup = msg.room.state == RoomState.REMATCH
                )
            }

            is ServerMessage.GameStarted -> {
                _state.value = _state.value.copy(
                    screen = Screen.GAME,
                    yourRole = msg.yourRole,
                    yourContent = msg.content,
                    contentIsWord = msg.contentIsWord,
                    room = msg.room,
                    players = msg.room.players,
                    showRematchPopup = false,
                    lastWordsPlayed = emptyMap(),
                    votingResult = null,
                    askingForVote = false,
                    voteDeadlineMs = 0L
                )
            }

            is ServerMessage.TurnChanged -> {
                _state.value = _state.value.copy(
                    room = _state.value.room?.copy(
                        currentTurnPlayerId = msg.currentTurnPlayerId,
                        roundNumber = msg.roundNumber
                    )
                )
            }

            is ServerMessage.WordPlayed -> {
                _state.value = _state.value.copy(
                    lastWordsPlayed = _state.value.lastWordsPlayed + (msg.playerId to msg.word)
                )
            }

            is ServerMessage.AskWantVote -> {
                _state.value = _state.value.copy(
                    askingForVote = true,
                    voteDeadlineMs = msg.deadlineEpochMs
                )
            }

            is ServerMessage.VotingStarted -> {
                _state.value = _state.value.copy(screen = Screen.VOTING)
            }

            is ServerMessage.VotingResult -> {
                val gameOver = msg.room.state == RoomState.FINISHED
                _state.value = _state.value.copy(
                    screen = if (gameOver) Screen.RESULT else Screen.ROUND_RESULT,
                    votingResult = Pair(msg.ejectedPlayerId, msg.wasImpostor),
                    room = msg.room,
                    players = msg.room.players
                )
            }

            is ServerMessage.RoundContinues -> {
                _state.value = _state.value.copy(
                    screen = Screen.GAME,
                    lastWordsPlayed = emptyMap()
                )
            }

            is ServerMessage.GameEnded -> {
                _state.value = _state.value.copy(
                    screen = Screen.RESULT,
                    room = msg.room,
                    players = msg.room.players,
                    showRematchPopup = false
                )
            }

            is ServerMessage.RematchStarted -> {
                _state.value = _state.value.copy(
                    screen = Screen.RESULT,
                    room = msg.room,
                    players = msg.room.players,
                    showRematchPopup = true
                )
            }

            is ServerMessage.ReturnedToLobby -> {
                _state.value = _state.value.copy(
                    screen = Screen.LOBBY,
                    room = msg.room,
                    players = msg.room.players,
                    showRematchPopup = false,
                    votingResult = null,
                    lastWordsPlayed = emptyMap()
                )
            }

            is ServerMessage.RemovedFromRoom -> {
                _state.value = _state.value.copy(
                    screen = Screen.HOME,
                    room = null,
                    yourPlayerId = null,
                    showRematchPopup = false,
                    error = msg.reason
                )
                connectJob?.cancel()
                connectJob = null
                viewModelScope.launch { gameClient?.disconnect() }
                gameClient = null
            }

            is ServerMessage.ErrorMessage -> {
                _state.value = _state.value.copy(error = msg.text)
                println("ErrorMessage from server: ${msg.text}")
            }
        }
    }

}
