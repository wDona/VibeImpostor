package org.example.project.ui

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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
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
            val ejectedName = room.players.find { it.id == ejectedId }?.name ?: "Unknown"

            Text(
                text = if (wasImpostor) Strings.get("result_innocents_win", language) else Strings.get("result_impostor_wins", language),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${Strings.get("result_ejected", language)}: $ejectedName",
                fontSize = 16.sp
            )
        }

        HorizontalDivider()

        Text(Strings.get("result_final_scores", language), fontWeight = FontWeight.Bold)

        val sortedPlayers = room.players.sortedByDescending { it.score }
        val topScore = sortedPlayers.firstOrNull()?.score ?: 0
        sortedPlayers.forEach { player ->
            val isWinner = player.score == topScore && topScore > 0
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isWinner) {
                        "${player.name}: ${player.score} pts  - ${Strings.get("result_winner", language)}"
                    } else {
                        "${player.name}: ${player.score} pts"
                    },
                    modifier = Modifier.padding(8.dp),
                    fontSize = 18.sp,
                    fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        HorizontalDivider()

        Button(
            onClick = { viewModel.requestRematch() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(Strings.get("result_play_again", language))
        }

        Button(
            onClick = { viewModel.backToLobby() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(Strings.get("result_back_to_lobby", language))
        }

        TextButton(
            onClick = { viewModel.leaveRoom() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(Strings.get("rematch_waiting", language))
                    Text("${Strings.get("rematch_seconds", language)}: $secondsLeft")
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
