package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import org.example.project.protocol.NOBODY_VOTE_ID
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.example.project.GameViewModel
import org.example.project.i18n.Strings

@Composable
fun ResultScreen(viewModel: GameViewModel) {
    val state = viewModel.state.collectAsState()
    val room = state.value.room ?: return
    val language = state.value.settings.language

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = Strings.get("result_title", language),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        state.value.votingResult?.let { (ejectedId, wasImpostor) ->
            val ejectedName = when (ejectedId) {
                NOBODY_VOTE_ID -> Strings.get("voting_nobody_name", language)
                else -> room.players.find { it.id == ejectedId }?.name ?: "?"
            }
            val impostorsWon = room.lastWinners.isNotEmpty() && room.lastWinners.all { it in room.impostorIds }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (impostorsWon)
                        Color(0xFFD32F2F).copy(alpha = 0.15f)
                    else
                        Color(0xFF1976D2).copy(alpha = 0.15f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (impostorsWon) Strings.get("result_impostor_wins", language) else Strings.get("result_innocents_win", language),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${Strings.get("result_ejected", language)}: $ejectedName",
                        fontSize = 16.sp
                    )
                }
            }
        }

        VoteReveal(room, state.value.lastRoundVotes, language, room.config.anonymousVotes)

        HorizontalDivider()

        Text(Strings.get("result_final_scores", language), fontWeight = FontWeight.Bold, fontSize = 16.sp)

        val sortedPlayers = room.players.sortedByDescending { it.score }
        val topScore = sortedPlayers.firstOrNull()?.score ?: 0
        val highlightIds = if (room.lastWinners.isNotEmpty()) room.lastWinners.toSet()
                           else if (topScore > 0) sortedPlayers.filter { it.score == topScore }.map { it.id }.toSet()
                           else emptySet()
        sortedPlayers.forEach { player ->
            val isWinner = player.id in highlightIds
            val isLivingImpostor = player.id in room.impostorIds && !player.isSpectator
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = if (isWinner)
                    CardDefaults.cardColors(containerColor = Color(0xFF2E7D32).copy(alpha = 0.18f))
                else
                    CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    if (isWinner) {
                        Text(
                            text = player.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                        Text(
                            text = "${player.score} pts · ${Strings.get("result_winner", language)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2E7D32)
                        )
                    } else {
                        Text(
                            text = "${player.name}: ${player.score} pts",
                            fontSize = 16.sp,
                            fontWeight = if (isLivingImpostor) FontWeight.Bold else FontWeight.Normal,
                            color = if (isLivingImpostor) Color(0xFFD32F2F)
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        Button(
            onClick = { viewModel.requestRematch() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text(Strings.get("result_play_again", language))
        }

        OutlinedButton(
            onClick = { viewModel.backToLobby() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text(Strings.get("result_back_to_lobby", language))
        }

        TextButton(
            onClick = { viewModel.leaveRoom() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text(Strings.get("result_leave", language))
        }
    }

    if (state.value.showRematchPopup) {
        val yourPlayer = room.players.find { it.id == state.value.yourPlayerId }
        val alreadyReady = yourPlayer?.wantsRematch == true
        val readyCount = room.players.count { it.wantsRematch }
        val totalCount = room.players.size

        var secondsLeft by remember { mutableStateOf(20) }
        LaunchedEffect(Unit) {
            secondsLeft = 20
            while (secondsLeft > 0) {
                delay(1000)
                secondsLeft -= 1
            }
        }

        AlertDialog(
            onDismissRequest = { },
            title = { Text(Strings.get("rematch_title", language)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(Strings.get("rematch_waiting", language))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                    ) {
                        Text(
                            "${Strings.get("rematch_seconds", language)}: $secondsLeft",
                            modifier = Modifier.padding(12.dp),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                    }
                    Text("${Strings.get("rematch_ready_count", language)}: $readyCount / $totalCount")
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.requestRematch() },
                    enabled = !alreadyReady
                ) {
                    Text(Strings.get("rematch_join", language))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.leaveRoom() }) {
                    Text(Strings.get("result_leave", language))
                }
            }
        )
    }
}
