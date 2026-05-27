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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.GameViewModel
import org.example.project.i18n.Strings
import org.example.project.model.GameMode
import org.example.project.model.Role
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.Color

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val state = viewModel.state.collectAsState()
    val room = state.value.room ?: return
    val wordInput = remember { mutableStateOf("") }
    val language = state.value.settings.language
    val amSpectator = room.players.find { it.id == state.value.yourPlayerId }?.isSpectator == true

    val isImpostor = state.value.yourRole == Role.IMPOSTOR
    val roleCardGradient = if (isImpostor) {
        Brush.linearGradient(listOf(Color(0xFF7F1D1D), Color(0xFFD32F2F)))
    } else {
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.tertiaryContainer
            )
        )
    }
    val roleTextColor = if (isImpostor) Color(0xFFFECACA) else MaterialTheme.colorScheme.onPrimaryContainer
    val contentColor = if (isImpostor) Color.White else MaterialTheme.colorScheme.onPrimaryContainer

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            "${Strings.get("game_round", language)} ${room.roundNumber}",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        if (amSpectator) {
            Text(
                text = Strings.get("spectator_banner", language),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Players list
        PlayersList(
            players = room.players,
            turnOrder = room.turnOrder,
            currentTurnId = room.currentTurnPlayerId,
            lastWordsPlayed = state.value.lastWordsPlayed,
            eliminationVotes = state.value.eliminationVotes,
            anonymousVotes = room.config.anonymousVotes
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Role card - dramatic gradient
        GameCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(roleCardGradient)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = when (state.value.yourRole) {
                            Role.IMPOSTOR -> Strings.get("role_impostor", language)
                            Role.INNOCENT -> Strings.get("role_innocent", language)
                            null -> Strings.get("game_waiting", language)
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = roleTextColor
                    )
                    Text(
                        text = state.value.yourContent ?: "",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        color = contentColor
                    )
                    Text(
                        text = if (state.value.contentIsWord)
                            Strings.get("game_word", language)
                        else
                            Strings.get("game_category", language),
                        fontSize = 12.sp,
                        color = contentColor.copy(alpha = 0.7f),
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // Text mode player list
        if (room.config.gameMode == GameMode.TEXT) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    Strings.get("game_players", language),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                val activePlayers = room.players.filter { !it.isSpectator }
                activePlayers.forEach { player ->
                    val isCurrentTurn = player.id == room.currentTurnPlayerId
                    val lastWord = state.value.lastWordsPlayed[player.id]

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrentTurn)
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                            else
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isCurrentTurn) 4.dp else 1.dp
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = player.name,
                                fontWeight = if (isCurrentTurn) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrentTurn)
                                    MaterialTheme.colorScheme.tertiary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                            if (lastWord != null) {
                                Text(
                                    text = "\"$lastWord\"",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Turn action card
        GameCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (state.value.room?.currentTurnPlayerId == state.value.yourPlayerId) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (room.config.gameMode == GameMode.TEXT) {
                        TextField(
                            value = wordInput.value,
                            onValueChange = { wordInput.value = it },
                            label = { Text(Strings.get("game_your_word", language)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                viewModel.submitWord(wordInput.value)
                                wordInput.value = ""
                            },
                            enabled = wordInput.value.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                Strings.get("game_submit_word", language),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Button(
                            onClick = { viewModel.endTurn() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                Strings.get("game_end_turn", language),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${Strings.get("game_waiting_player", language)} ${room.players.find { it.id == room.currentTurnPlayerId }?.name}...",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (!amSpectator) {
            androidx.compose.material3.OutlinedButton(
                onClick = { viewModel.proposeEndGame() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(Strings.get("game_propose_end", language))
            }
        }

        LeaveGameButton(viewModel, language)

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
    }
}
