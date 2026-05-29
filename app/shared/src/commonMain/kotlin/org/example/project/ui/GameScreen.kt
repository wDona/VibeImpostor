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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import org.example.project.currentTimeMillis
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val state = viewModel.state.collectAsState()
    val room = state.value.room ?: return
    val wordInput = remember { mutableStateOf("") }
    val language = state.value.settings.language
    val amSpectator = room.players.find { it.id == state.value.yourPlayerId }?.isSpectator == true
    val isHost = room.hostId == state.value.yourPlayerId
    var showKickDialog by remember { mutableStateOf(false) }
    var kickTargetId by remember { mutableStateOf<String?>(null) }

    val gameStartedAt = state.value.gameStartedAtMs
    val elapsedSeconds = remember { mutableLongStateOf(0L) }
    LaunchedEffect(gameStartedAt) {
        while (true) {
            elapsedSeconds.longValue = if (gameStartedAt > 0) (currentTimeMillis() - gameStartedAt) / 1000 else 0
            delay(1000)
        }
    }
    val hiddenRole = room.config.hiddenRole
    val isImpostor = state.value.yourRole == Role.IMPOSTOR
    val roleCardGradient = when {
        hiddenRole -> Brush.linearGradient(listOf(Color(0xFF2D3748), Color(0xFF4A5568)))
        isImpostor -> Brush.linearGradient(listOf(Color(0xFF7F1D1D), Color(0xFFD32F2F)))
        else -> Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.tertiaryContainer))
    }
    val roleTextColor = when {
        hiddenRole -> Color(0xFF9CA3AF)
        isImpostor -> Color(0xFFFECACA)
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    val contentColor = if (hiddenRole || isImpostor) Color.White else MaterialTheme.colorScheme.onPrimaryContainer

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${Strings.get("game_round", language)} ${room.roundNumber}",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            val elapsed = elapsedSeconds.longValue
            val mm = elapsed / 60
            val ss = elapsed % 60
            Text(
                text = "${mm.toString().padStart(2, '0')}:${ss.toString().padStart(2, '0')}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
        }

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
            anonymousVotes = room.config.anonymousVotes,
            language = language
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
                    if (!hiddenRole) {
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
                    }
                    Text(
                        text = (state.value.yourContent ?: "").lowercase(),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        color = contentColor
                    )
                    if (!hiddenRole) {
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

                    // Hints for impostors
                    if (isImpostor && !state.value.contentIsWord && room.config.progressiveHints && state.value.yourHintList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val visibleHints = state.value.yourHintList.take(room.roundNumber)
                        visibleHints.forEachIndexed { idx, hint ->
                            Text(
                                text = "${Strings.get("game_hint", language)} ${idx + 1}: ${hint.lowercase()}",
                                fontSize = 13.sp,
                                color = contentColor.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
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
            OutlinedButton(
                onClick = { viewModel.proposeEndGame() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(Strings.get("game_propose_end", language))
            }
        }

        if (isHost) {
            OutlinedButton(
                onClick = { showKickDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ImpostorRed)
            ) {
                Text(Strings.get("game_kick_title", language))
            }
        }

        LeaveGameButton(viewModel, language)

        if (state.value.showEndGameDialog) {
            AlertDialog(
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

        if (showKickDialog) {
            val kickablePlayers = room.players.filter { it.id != state.value.yourPlayerId }
            AlertDialog(
                onDismissRequest = { showKickDialog = false },
                title = { Text(Strings.get("game_kick_title", language)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(Strings.get("game_kick_select", language))
                        kickablePlayers.forEach { player ->
                            OutlinedButton(
                                onClick = {
                                    kickTargetId = player.id
                                    showKickDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(player.name)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showKickDialog = false }) {
                        Text(Strings.get("common_cancel", language))
                    }
                }
            )
        }

        kickTargetId?.let { targetId ->
            val targetName = room.players.find { it.id == targetId }?.name ?: "?"
            AlertDialog(
                onDismissRequest = { kickTargetId = null },
                title = { Text(Strings.get("kick_confirm_title", language).replace("{name}", targetName)) },
                text = { Text(Strings.get("kick_confirm_text", language)) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.kickPlayer(targetId)
                            kickTargetId = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ImpostorRed)
                    ) {
                        Text(Strings.get("kick_player", language))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { kickTargetId = null }) {
                        Text(Strings.get("common_cancel", language))
                    }
                }
            )
        }
    }
}
