package org.example.project.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.model.PublicPlayer

@Composable
fun PlayersList(
    players: List<PublicPlayer>,
    turnOrder: List<String>,
    currentTurnId: String?,
    lastWordsPlayed: Map<String, String> = emptyMap()
) {
    val activePlayers = players.filter { !it.isSpectator }.sortedBy { p ->
        turnOrder.indexOf(p.id)
    }
    val eliminatedPlayers = players.filter { it.isSpectator }

    Column(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (activePlayers.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                activePlayers.forEach { player ->
                    PlayerCard(
                        player = player,
                        isCurrent = player.id == currentTurnId,
                        lastWord = lastWordsPlayed[player.id]
                    )
                }
            }
        }

        if (eliminatedPlayers.isNotEmpty()) {
            Text(
                "Eliminados",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                eliminatedPlayers.forEach { player ->
                    PlayerCard(
                        player = player,
                        isCurrent = false,
                        lastWord = lastWordsPlayed[player.id]
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerCard(
    player: PublicPlayer,
    isCurrent: Boolean,
    lastWord: String?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.45f),
        colors = CardDefaults.cardColors(
            containerColor = when {
                player.isSpectator -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                isCurrent -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = player.name,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp,
                color = if (player.isSpectator)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                else
                    MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            if (lastWord != null) {
                Text(
                    text = "\"$lastWord\"",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1
                )
            }
        }
    }
}
