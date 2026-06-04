package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import org.example.project.protocol.BOTH_IMPOSTORS_ID
import org.example.project.protocol.NOBODY_VOTE_ID
import org.example.project.protocol.WRONG_CLAIM_PREFIX
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
import androidx.compose.ui.graphics.Brush
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
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = Strings.get("result_title", language),
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )

        state.value.votingResult?.let { (ejectedId, wasImpostor) ->
            val ejectedDisplay = when {
                ejectedId == NOBODY_VOTE_ID ->
                    "${Strings.get("result_ejected", language)}: ${Strings.get("voting_nobody_name", language)}"
                ejectedId == BOTH_IMPOSTORS_ID ->
                    Strings.get("voting_both_correct", language)
                ejectedId != null && ejectedId.startsWith(WRONG_CLAIM_PREFIX) -> {
                    val voterId = ejectedId.removePrefix(WRONG_CLAIM_PREFIX)
                    val voterName = room.players.find { it.id == voterId }?.name ?: "?"
                    Strings.get("voting_wrong_claim", language).replace("{name}", voterName)
                }
                else -> "${Strings.get("result_ejected", language)}: ${room.players.find { it.id == ejectedId }?.name ?: "?"}"
            }
            val impostorsWon = room.lastWinners.isNotEmpty() && room.lastWinners.all { it in room.impostorIds }

            // Win/loss banner with gradient
            GameCard(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (impostorsWon)
                                Brush.linearGradient(
                                    listOf(Color(0xFF7F1D1D), Color(0xFFD32F2F))
                                )
                            else
                                Brush.linearGradient(
                                    listOf(Color(0xFF1E3A5F), Color(0xFF1976D2))
                                )
                        )
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (impostorsWon)
                                Strings.get("result_impostor_wins", language)
                            else
                                Strings.get("result_innocents_win", language),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = ejectedDisplay,
                            fontSize = 15.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        VoteReveal(room, state.value.lastRoundVotes, language, room.config.anonymousVotes)

        HorizontalDivider()

        Text(
            Strings.get("result_final_scores", language),
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        val sortedPlayers = room.players.sortedByDescending { it.score }
        val topScore = sortedPlayers.firstOrNull()?.score ?: 0
        val highlightIds = if (room.lastWinners.isNotEmpty()) room.lastWinners.toSet()
                           else if (topScore > 0) sortedPlayers.filter { it.score == topScore }.map { it.id }.toSet()
                           else emptySet()

        sortedPlayers.forEachIndexed { index, player ->
            val isWinner = player.id in highlightIds
            val isLivingImpostor = player.id in room.impostorIds && !player.isSpectator
            val isImpostor = player.id in room.impostorIds
            val nameColor = when {
                isWinner -> WinnerGreen
                isLivingImpostor -> ImpostorRed
                else -> MaterialTheme.colorScheme.onSurface
            }
            val roleColor = if (isImpostor) ImpostorRed else nameColor

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Radii.md),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isWinner -> WinnerGreen.copy(alpha = 0.12f)
                        isLivingImpostor -> ImpostorRed.copy(alpha = 0.08f)
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "#${index + 1}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isWinner) WinnerGreen
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        PlayerAvatar(
                            name = player.name,
                            colorIndex = player.colorIndex,
                            size = 28.dp,
                            dimmed = player.isSpectator
                        )
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = player.name,
                                    fontSize = 16.sp,
                                    fontWeight = if (isWinner || isLivingImpostor) FontWeight.Bold else FontWeight.Normal,
                                    color = nameColor
                                )
                                Text(
                                    text = if (isImpostor) Strings.get("role_impostor", language)
                                           else Strings.get("role_innocent", language),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = roleColor.copy(alpha = if (isImpostor) 0.9f else 0.6f),
                                    letterSpacing = 0.5.sp
                                )
                            }
                            if (isWinner) {
                                Text(
                                    Strings.get("result_winner", language),
                                    fontSize = 11.sp,
                                    color = WinnerGreen,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                    Text(
                        "${player.score} pts",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isWinner) WinnerGreen
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        HorizontalDivider()

        Button(
            onClick = { viewModel.requestRematch() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(Radii.md)
        ) {
            Text(
                Strings.get("result_play_again", language),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        OutlinedButton(
            onClick = { viewModel.backToLobby() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(Radii.md)
        ) {
            Text(Strings.get("result_back_to_lobby", language))
        }

        TextButton(
            onClick = { viewModel.leaveRoom() },
            modifier = Modifier.fillMaxWidth()
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Color(0xFFF59E0B).copy(alpha = 0.12f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            "${Strings.get("rematch_seconds", language)}: $secondsLeft",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309)
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
