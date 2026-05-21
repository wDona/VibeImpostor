package org.example.project.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
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
fun RoundResultScreen(viewModel: GameViewModel) {
    val state = viewModel.state.collectAsState()
    val room = state.value.room ?: return
    val language = state.value.settings.language

    var secondsLeft by remember { mutableStateOf(12) }
    LaunchedEffect(Unit) {
        secondsLeft = 12
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
            val ejectedName = room.players.find { it.id == ejectedId }?.name ?: "?"
            Text(
                text = if (wasImpostor) Strings.get("round_impostor_caught", language) else Strings.get("result_impostor_escaped", language),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = "${Strings.get("result_ejected", language)}: $ejectedName",
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        VoteReveal(room, state.value.lastRoundVotes, language)

        Text(
            text = "${Strings.get("round_result_continues", language)} $secondsLeft",
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 24.dp)
        )

        LeaveGameButton(viewModel, language)
    }
}
