package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import org.example.project.protocol.BOTH_IMPOSTORS_ID
import org.example.project.protocol.NOBODY_VOTE_ID

@Composable
fun VotingScreen(viewModel: GameViewModel) {
    val state = viewModel.state.collectAsState()
    val room = state.value.room ?: return
    val language = state.value.settings.language

    val secondsLeft = remember { mutableStateOf(room.config.voteTimeLimitSeconds) }
    LaunchedEffect(Unit) {
        while (secondsLeft.value > 0) {
            delay(1000)
            secondsLeft.value--
        }
    }

    val amSpectator = room.players.find { it.id == state.value.yourPlayerId }?.isSpectator == true
    val voted = remember { mutableStateOf(false) }
    val selectedTarget = remember { mutableStateOf<String?>(null) }
    val punishmentMode = room.config.punishmentVote
    val activePlayers = room.players.filter { !it.isSpectator }
    val votedPlayerIds = state.value.votedPlayerIds
    val votedCount = votedPlayerIds.size
    val totalVoters = activePlayers.size

    val timerColor = when {
        secondsLeft.value > 20 -> MaterialTheme.colorScheme.primary
        secondsLeft.value > 10 -> Color(0xFFF59E0B)
        else -> MaterialTheme.colorScheme.error
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Title + timer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = Strings.get("voting_title", language),
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )
            Box(
                modifier = Modifier
                    .background(timerColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${secondsLeft.value}s",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = timerColor
                )
            }
        }

        // Vote progress
        GameCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${Strings.get("voting_progress", language)}: $votedCount/$totalVoters",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activePlayers.forEach { player ->
                        val hasVoted = player.id in votedPlayerIds
                        Row(
                            modifier = Modifier
                                .background(
                                    if (hasVoted) playerColor(player.id).copy(alpha = 0.18f)
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            PlayerDot(player.id)
                            Text(
                                text = player.name,
                                fontSize = 12.sp,
                                fontWeight = if (hasVoted) FontWeight.Bold else FontWeight.Normal,
                                color = if (hasVoted) playerColor(player.id)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }

        // Main content
        if (amSpectator) {
            GameCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        Strings.get("voting_spectator", language),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        } else if (voted.value) {
            GameCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "OK",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            Strings.get("voting_voted", language),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                room.players
                    .filter { it.id != state.value.yourPlayerId && !it.isSpectator }
                    .forEach { player ->
                        val isSelected = selectedTarget.value == player.id
                        Button(
                            onClick = {
                                if (punishmentMode) {
                                    selectedTarget.value = if (isSelected) null else player.id
                                } else {
                                    viewModel.castVote(player.id)
                                    voted.value = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .then(
                                    if (isSelected)
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(12.dp))
                                    else Modifier
                                ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PlayerDot(player.id)
                                Text(
                                    player.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                // Vote type buttons when player is selected in punishment mode
                if (punishmentMode && selectedTarget.value != null && !voted.value) {
                    val targetId = selectedTarget.value!!
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.castVote(targetId, voteIsHard = false)
                                voted.value = true
                            },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("🟡 ${Strings.get("vote_suspicious", language)}", fontSize = 14.sp)
                        }
                        Button(
                            onClick = {
                                viewModel.castVote(targetId, voteIsHard = true)
                                voted.value = true
                            },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("🔴 ${Strings.get("vote_sure", language)}", fontSize = 14.sp)
                        }
                    }
                }

                if (room.config.numImpostors >= 2) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    OutlinedButton(
                        onClick = {
                            viewModel.castVote(NOBODY_VOTE_ID)
                            voted.value = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(Strings.get("voting_nobody", language), fontSize = 15.sp)
                    }
                    if (activePlayers.size >= 2) {
                        OutlinedButton(
                            onClick = {
                                viewModel.castVote(BOTH_IMPOSTORS_ID)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(Strings.get("voting_both_impostors", language), fontSize = 15.sp)
                        }
                    }
                }
            }
        }

        LeaveGameButton(viewModel, language)
    }
}
