package org.example.project.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.GameViewModel
import org.example.project.i18n.Strings

@Composable
fun VotingScreen(viewModel: GameViewModel) {
    val state = viewModel.state.collectAsState()
    val room = state.value.room ?: return
    val language = state.value.settings.language

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = Strings.get("voting_title", language),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        val amSpectator = room.players.find { it.id == state.value.yourPlayerId }?.isSpectator == true
        val voted = remember { mutableStateOf(false) }

        if (amSpectator) {
            Text(Strings.get("voting_spectator", language))
        } else if (voted.value) {
            Text(Strings.get("voting_voted", language))
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
                        Button(
                            onClick = {
                                viewModel.castVote(player.id)
                                voted.value = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(player.name)
                        }
                    }
            }
        }

        LeaveGameButton(viewModel, language)
    }
}
