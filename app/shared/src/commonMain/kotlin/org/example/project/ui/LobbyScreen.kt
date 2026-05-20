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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.example.project.GameViewModel
import org.example.project.i18n.Strings

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
        Text("${Strings.get("lobby_room_code", language)}: ${room.code}")

        HorizontalDivider()

        Text("${Strings.get("lobby_players", language)} (${room.players.size}/${10})")
        room.players.forEach { player ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(player.name)
                    Text("Puntos: ${player.score}")
                    if (player.isHost) Text("(Host)")
                }
            }
        }

        HorizontalDivider()

        Text("Configuración")
        Text("${Strings.get("lobby_game_mode", language)}: ${if (room.config.gameMode.name == "VOICE") Strings.get("lobby_voice", language) else Strings.get("lobby_text", language)}")
        Text("${Strings.get("lobby_impostors", language)}: ${room.config.numImpostors}")
        Text("${Strings.get("lobby_vote_time", language)}: ${room.config.voteTimeLimitSeconds}s")

        if (state.value.room?.hostId == state.value.yourPlayerId) {
            Button(
                onClick = { viewModel.startGame() },
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
