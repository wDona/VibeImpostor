package org.example.project.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.GameViewModel
import org.example.project.i18n.Strings
import org.example.project.protocol.BOTH_IMPOSTORS_ID
import org.example.project.protocol.NOBODY_VOTE_ID
import org.example.project.protocol.PUNISHMENT_PREFIX
import org.example.project.protocol.WRONG_CLAIM_PREFIX

@Composable
fun RoundResultScreen(viewModel: GameViewModel) {
    val state = viewModel.state.collectAsState()
    val room = state.value.room ?: return
    val language = state.value.settings.language

    var userContinued = remember { mutableStateOf(false) }
    val secondsLeft = remember { mutableStateOf(30) }
    val yourId = state.value.yourPlayerId
    val amSpectator = room.players.find { it.id == yourId }?.isSpectator == true
    val isPendingImpostor = yourId != null && yourId == room.pendingGuessImpostorId
    val mustContinue = !amSpectator || isPendingImpostor

    LaunchedEffect(Unit) {
        while (secondsLeft.value > 0 && !userContinued.value) {
            delay(1000)
            secondsLeft.value--
        }
        if (!userContinued.value && mustContinue) {
            userContinued.value = true
            viewModel.continueRound()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = Strings.get("round_result_title", language),
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        state.value.votingResult?.let { (ejectedId, wasImpostor) ->
            val (cardBg, cardBorder) = when {
                ejectedId == null -> Pair(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
                wasImpostor -> Pair(ImpostorRed.copy(alpha = 0.1f), ImpostorRed.copy(alpha = 0.35f))
                else -> Pair(InnocentBlue.copy(alpha = 0.1f), InnocentBlue.copy(alpha = 0.35f))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, cardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (ejectedId == BOTH_IMPOSTORS_ID) {
                        Text(
                            text = Strings.get("voting_both_impostors", language),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = Strings.get("voting_both_correct", language),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    } else if (ejectedId == NOBODY_VOTE_ID) {
                        Text(
                            text = Strings.get("voting_nobody", language),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (wasImpostor) Strings.get("voting_nobody_wrong", language)
                                   else Strings.get("voting_nobody_correct", language),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else if (ejectedId == null) {
                        Text(
                            text = Strings.get("voting_tie", language),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (ejectedId.startsWith(PUNISHMENT_PREFIX)) {
                        val punishedId = ejectedId.removePrefix(PUNISHMENT_PREFIX)
                        val punishedName = room.players.find { it.id == punishedId }?.name ?: "?"
                        Text(
                            text = Strings.get("punishment_warning_title", language),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = punishedName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFF59E0B)
                        )
                        Text(
                            text = Strings.get("punishment_warning_desc", language),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    } else if (ejectedId.startsWith(WRONG_CLAIM_PREFIX)) {
                        val actualId = ejectedId.removePrefix(WRONG_CLAIM_PREFIX)
                        val claimerName = room.players.find { it.id == actualId }?.name ?: "?"
                        Text(
                            text = Strings.get("voting_wrong_claim", language).replace("{name}", claimerName),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        val ejectedName = room.players.find { it.id == ejectedId }?.name ?: "?"
                        Text(
                            text = if (wasImpostor) Strings.get("round_impostor_caught", language)
                                   else Strings.get("player_ejected", language),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = ejectedName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = if (wasImpostor) ImpostorRed else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        VoteReveal(room, state.value.lastRoundVotes, language, room.config.anonymousVotes, state.value.lastVoteTypes)

        // Continue progress card
        val continuedIds = room.continueResponses
        val requiredToContinue = room.players
            .filter { !it.isSpectator || it.id == room.pendingGuessImpostorId }
        val doneCount = continuedIds.size
        val totalCount = requiredToContinue.size

        GameCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${Strings.get("common_continue", language)}: $doneCount/$totalCount",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    letterSpacing = 0.5.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                requiredToContinue.forEach { player ->
                    val hasContinued = player.id in continuedIds
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (hasContinued) playerColor(player.colorIndex)
                                    else playerColor(player.colorIndex).copy(alpha = 0.25f),
                                    CircleShape
                                )
                        )
                        Text(
                            text = player.name,
                            fontSize = 14.sp,
                            color = if (hasContinued)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }

                if (mustContinue && !userContinued.value) {
                    Button(
                        onClick = {
                            userContinued.value = true
                            viewModel.continueRound()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            "${Strings.get("common_continue", language)} (${secondsLeft.value}s)",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (isPendingImpostor) {
                        Text(
                            text = Strings.get("impostor_guess_after_continue_hint", language),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

        LeaveGameButton(viewModel, language)
    }
}
