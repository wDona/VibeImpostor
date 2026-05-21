package org.example.project.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.project.GameViewModel
import org.example.project.i18n.Strings
import org.example.project.model.MAX_PLAYERS
import org.example.project.model.MIN_PLAYERS
import org.example.project.model.RoomState

@Composable
fun LobbyScreen(viewModel: GameViewModel) {
    val state = viewModel.state.collectAsState()
    val room = state.value.room ?: return
    val language = state.value.settings.language
    val isHost = state.value.room?.hostId == state.value.yourPlayerId
    var showLeaveConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
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

            Text("${Strings.get("lobby_players", language)} (${room.players.size}/$MAX_PLAYERS)")
            room.players.forEach { player ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
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

            RoomConfigPanel(
                viewModel = viewModel,
                room = room,
                isHost = isHost,
                language = language
            )
        }

        HorizontalDivider()

        if (isHost) {
            val enoughPlayers = room.players.size >= MIN_PLAYERS
            if (!enoughPlayers) {
                Text(Strings.get("lobby_need_players", language))
            }
            Button(
                onClick = { viewModel.startGame() },
                enabled = enoughPlayers,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text(Strings.get("lobby_start_game", language))
            }
        }

        OutlinedButton(
            onClick = { showLeaveConfirm = true },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text(Strings.get("lobby_leave", language))
        }
    }

    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text(Strings.get("lobby_leave_confirm_title", language)) },
            text = { Text(Strings.get("lobby_leave_confirm_text", language)) },
            confirmButton = {
                Button(onClick = {
                    showLeaveConfirm = false
                    viewModel.leaveRoom()
                }) {
                    Text(Strings.get("lobby_leave", language))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirm = false }) {
                    Text(Strings.get("common_cancel", language))
                }
            }
        )
    }
}
