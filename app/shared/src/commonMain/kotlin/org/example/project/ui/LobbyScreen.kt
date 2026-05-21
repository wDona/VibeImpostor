package org.example.project.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.project.GameViewModel
import org.example.project.i18n.Strings
import org.example.project.model.RoomState

@Composable
fun LobbyScreen(viewModel: GameViewModel) {
    val state = viewModel.state.collectAsState()
    val room = state.value.room ?: return
    val language = state.value.settings.language

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (room.state != RoomState.LOBBY) {
            Text(
                Strings.get("lobby_waiting_game", language),
                fontWeight = FontWeight.Bold
            )
        }

        val clipboardManager = LocalClipboardManager.current
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${Strings.get("lobby_room_code", language)}: ${room.code}")
            Button(onClick = { clipboardManager.setText(AnnotatedString(room.code)) }) {
                Text(Strings.get("lobby_copy_code", language))
            }
        }

        HorizontalDivider()

        Text("${Strings.get("lobby_players", language)} (${room.players.size}/${10})")
        room.players.forEach { player ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        player.name,
                        color = if (player.isSpectator)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                    Text(" - Puntos: ${player.score}")
                    if (player.isHost) Text(" (Host)")
                }
            }
        }

        HorizontalDivider()

        Text("Configuración")
        Text("${Strings.get("lobby_game_mode", language)}: ${if (room.config.gameMode.name == "VOICE") Strings.get("lobby_voice", language) else Strings.get("lobby_text", language)}")
        Text("${Strings.get("lobby_impostors", language)}: ${room.config.numImpostors}")
        Text("${Strings.get("lobby_vote_time", language)}: ${room.config.voteTimeLimitSeconds}s")

        if (state.value.room?.hostId == state.value.yourPlayerId) {
            val enoughPlayers = room.players.size >= 3
            if (!enoughPlayers) {
                Text(Strings.get("lobby_need_players", language))
            }
            Button(
                onClick = { viewModel.startGame() },
                enabled = enoughPlayers,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(Strings.get("lobby_start_game", language))
            }
        }

        Button(
            onClick = { viewModel.leaveRoom() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(Strings.get("lobby_leave", language))
        }
    }
}
