package org.example.project.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                text = if (wasImpostor) Strings.get("result_innocents_win", language) else Strings.get("result_impostor_escaped", language),
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

        room.players.forEach { player ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${player.name}: ${player.score} pts",
                    modifier = Modifier.padding(8.dp),
                    fontSize = 18.sp
                )
            }
        }

        Button(
            onClick = { viewModel.leaveRoom() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(Strings.get("result_back_home", language))
        }
    }
}
