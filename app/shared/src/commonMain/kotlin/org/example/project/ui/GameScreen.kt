package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.GameViewModel
import org.example.project.i18n.Strings
import org.example.project.model.GameMode

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val state = viewModel.state.collectAsState()
    val room = state.value.room ?: return
    val wordInput = remember { mutableStateOf("") }
    val language = state.value.settings.language

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("${Strings.get("game_round", language)} ${room.roundNumber}")

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = state.value.yourRole?.name ?: Strings.get("game_waiting", language),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = state.value.yourContent ?: "",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(8.dp)
                )
                Text(
                    text = if (state.value.contentIsWord) Strings.get("game_word", language) else Strings.get("game_category", language),
                    fontSize = 14.sp
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(Strings.get("game_players", language), fontWeight = FontWeight.Bold)
            room.players.forEach { player ->
                val isCurrentTurn = player.id == room.currentTurnPlayerId
                val lastWord = state.value.lastWordsPlayed[player.id]

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isCurrentTurn) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.surface
                        )
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "${player.name}",
                            fontWeight = if (isCurrentTurn) FontWeight.Bold else FontWeight.Normal
                        )
                        if (room.config.gameMode == GameMode.TEXT && lastWord != null) {
                            Text(
                                text = "\"$lastWord\"",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        if (state.value.room?.currentTurnPlayerId == state.value.yourPlayerId) {
            if (room.config.gameMode == GameMode.TEXT) {
                TextField(
                    value = wordInput.value,
                    onValueChange = { wordInput.value = it },
                    label = { Text(Strings.get("game_your_word", language)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
                Button(
                    onClick = {
                        viewModel.submitWord(wordInput.value)
                        wordInput.value = ""
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Text(Strings.get("game_submit_word", language))
                }
            } else {
                Button(
                    onClick = { viewModel.endTurn() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Text(Strings.get("game_end_turn", language))
                }
            }
        } else {
            Text("${Strings.get("game_waiting", language)} ${room.players.find { it.id == room.currentTurnPlayerId }?.name}...")
        }

        if (state.value.askingForVote) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { viewModel.respondVote(false) },
                title = { Text("¿Votar ahora?") },
                text = { Text("¿Quieres votar para expulsar a alguien?") },
                confirmButton = {
                    Button(onClick = { viewModel.respondVote(true) }) { Text("Sí") }
                },
                dismissButton = {
                    Button(onClick = { viewModel.respondVote(false) }) { Text("No") }
                }
            )
        }
    }
}
