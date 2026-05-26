package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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

    val bgColor = if (room.config.winOnFirstEjection) {
        state.value.votingResult?.let { (_, wasImpostor) ->
            if (wasImpostor) Color(0xFFFFCDD2) else Color(0xFFBBDEFB)
        } ?: Color.Transparent
    } else {
        Color.Transparent
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = Strings.get("round_result_title", language),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }

        state.value.votingResult?.let { (ejectedId, wasImpostor) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (ejectedId != null) {
                        if (wasImpostor) Color(0xFFFFCDD2) else Color(0xFFBBDEFB)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (ejectedId == BOTH_IMPOSTORS_ID) {
                        Text(
                            text = Strings.get("voting_both_impostors", language),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = Strings.get("voting_both_correct", language),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
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
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else if (ejectedId == null) {
                        Text(
                            text = Strings.get("voting_tie", language),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        val ejectedName = room.players.find { it.id == ejectedId }?.name ?: "?"
                        Text(
                            text = if (wasImpostor) Strings.get("round_impostor_caught", language) else Strings.get("player_ejected", language),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = ejectedName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        VoteReveal(room, state.value.lastRoundVotes, language, room.config.anonymousVotes)

        if (mustContinue) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!userContinued.value) {
                        Button(
                            onClick = {
                                userContinued.value = true
                                viewModel.continueRound()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${Strings.get("common_continue", language)} (${secondsLeft.value}s)")
                        }
                        if (isPendingImpostor) {
                            Text(
                                text = Strings.get("impostor_guess_after_continue_hint", language),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = Strings.get("waiting_others_continue", language),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else {
            Text(
                text = Strings.get("waiting_others_continue", language),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
        }

        LeaveGameButton(viewModel, language)
    }
}
