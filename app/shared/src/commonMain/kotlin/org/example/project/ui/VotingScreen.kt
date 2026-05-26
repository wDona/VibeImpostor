package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.example.project.GameViewModel
import org.example.project.i18n.Strings

@Composable
fun VotingScreen(viewModel: GameViewModel) {
    val state = viewModel.state.collectAsState()
    val room = state.value.room ?: return
    val language = state.value.settings.language

    val secondsLeft = remember { mutableStateOf(60) }
    LaunchedEffect(Unit) {
        while (secondsLeft.value > 0) {
            delay(1000)
            secondsLeft.value--
        }
    }

    val amSpectator = room.players.find { it.id == state.value.yourPlayerId }?.isSpectator == true
    val voted = remember { mutableStateOf(false) }
    val activePlayers = room.players.filter { !it.isSpectator }
    val votedPlayerIds = state.value.votedPlayerIds
    val votedCount = votedPlayerIds.size
    val totalVoters = activePlayers.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = Strings.get("voting_title", language),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Card(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.CenterHorizontally),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
            ) {
                Text(
                    text = "${secondsLeft.value}s",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    color = Color(0xFFE65100)
                )
            }
        }

        // Vote progress card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "${Strings.get("voting_progress", language)}: $votedCount/$totalVoters",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                activePlayers.forEach { player ->
                    val hasVoted = player.id in votedPlayerIds
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = player.name,
                            fontSize = 14.sp,
                            color = if (hasVoted)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = if (hasVoted) "✓" else "...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasVoted) Color(0xFF4CAF50)
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }

        if (amSpectator) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(Strings.get("voting_spectator", language), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        } else if (voted.value) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(Strings.get("voting_voted", language), fontSize = 16.sp, fontWeight = FontWeight.Medium)
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
                        Button(
                            onClick = {
                                viewModel.castVote(player.id)
                                voted.value = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(player.name, fontSize = 16.sp)
                        }
                    }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        LeaveGameButton(viewModel, language)
    }
}
