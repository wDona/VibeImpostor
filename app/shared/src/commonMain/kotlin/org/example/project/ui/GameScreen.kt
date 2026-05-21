package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import org.example.project.model.Role

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val state = viewModel.state.collectAsState()
    val room = state.value.room ?: return
    val wordInput = remember { mutableStateOf("") }
    val language = state.value.settings.language
    val amSpectator = room.players.find { it.id == state.value.yourPlayerId }?.isSpectator == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("${Strings.get("game_round", language)} ${room.roundNumber}")

        if (amSpectator) {
            Text(
                text = Strings.get("spectator_banner", language),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        }

        CircularPlayers(
            players = room.players,
            currentTurnId = room.currentTurnPlayerId,
            center = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(16.dp)
                ) {
                    Text(
                        text = when (state.value.yourRole) {
                            Role.IMPOSTOR -> Strings.get("role_impostor", language)
                            Role.INNOCENT -> Strings.get("role_innocent", language)
                            null -> Strings.get("game_waiting", language)
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = state.value.yourContent ?: "",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp)
                    )
                    Text(
                        text = if (state.value.contentIsWord)
                            Strings.get("game_word", language)
                        else
                            Strings.get("game_category", language),
                        fontSize = 12.sp
                    )
                }
            }
        )

        if (room.config.gameMode == GameMode.TEXT) {
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
                            ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = player.name,
                                fontWeight = if (isCurrentTurn) FontWeight.Bold else FontWeight.Normal,
                                color = if (player.isSpectator)
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                            if (lastWord != null) {
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
                    enabled = wordInput.value.isNotBlank(),
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

        LeaveGameButton(viewModel, language)

        if (!amSpectator) {
            androidx.compose.material3.OutlinedButton(
                onClick = { viewModel.proposeEndGame() },
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Text(Strings.get("game_propose_end", language))
            }
        }

        if (state.value.showEndGameDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { viewModel.answerEndGame(false) },
                title = { Text(Strings.get("end_game_title", language)) },
                text = {
                    Text(
                        Strings.get("end_game_text", language) +
                            " (${state.value.endGameAgreed}/${state.value.endGameTotal})"
                    )
                },
                confirmButton = {
                    Button(onClick = { viewModel.answerEndGame(true) }) {
                        Text(Strings.get("common_yes", language))
                    }
                },
                dismissButton = {
                    Button(onClick = { viewModel.answerEndGame(false) }) {
                        Text(Strings.get("common_no", language))
                    }
                }
            )
        }

        if (state.value.askingForVote && !amSpectator) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { viewModel.respondVote(false) },
                title = { Text(Strings.get("game_ask_vote_title", language)) },
                text = { Text(Strings.get("game_ask_vote_text", language)) },
                confirmButton = {
                    Button(onClick = { viewModel.respondVote(true) }) {
                        Text(Strings.get("common_yes", language))
                    }
                },
                dismissButton = {
                    Button(onClick = { viewModel.respondVote(false) }) {
                        Text(Strings.get("common_no", language))
                    }
                }
            )
        }
    }
}
