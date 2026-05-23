package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.GameViewModel
import org.example.project.i18n.Strings

@Composable
fun RoundResultScreen(viewModel: GameViewModel) {
    val state = viewModel.state.collectAsState()
    val room = state.value.room ?: return
    val language = state.value.settings.language

    var userContinued = remember { mutableStateOf(false) }
    val yourId = state.value.yourPlayerId
    val amSpectator = room.players.find { it.id == yourId }?.isSpectator == true
    val isPendingImpostor = yourId != null && yourId == room.pendingGuessImpostorId
    val mustContinue = !amSpectator || isPendingImpostor

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
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = Strings.get("round_result_title", language),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        state.value.votingResult?.let { (ejectedId, wasImpostor) ->
            if (ejectedId == null) {
                Text(
                    text = Strings.get("voting_tie", language),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp)
                )
            } else {
                val ejectedName = room.players.find { it.id == ejectedId }?.name ?: "?"
                Text(
                    text = if (wasImpostor) Strings.get("round_impostor_caught", language) else Strings.get("player_ejected", language),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text(
                    text = ejectedName,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        VoteReveal(room, state.value.lastRoundVotes, language, room.config.anonymousVotes)

        if (mustContinue) {
            if (!userContinued.value) {
                Button(
                    onClick = {
                        userContinued.value = true
                        viewModel.continueRound()
                    },
                    modifier = Modifier.padding(top = 24.dp)
                ) {
                    Text(Strings.get("common_continue", language))
                }
                if (isPendingImpostor) {
                    Text(
                        text = Strings.get("impostor_guess_after_continue_hint", language),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            } else {
                Text(
                    text = Strings.get("waiting_others_continue", language),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        } else {
            Text(
                text = Strings.get("waiting_others_continue", language),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 24.dp)
            )
        }

        LeaveGameButton(viewModel, language)
    }
}
