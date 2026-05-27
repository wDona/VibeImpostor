package org.example.project.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.model.PublicPlayer

@Composable
fun PlayerDot(playerId: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(8.dp)
            .background(playerColor(playerId), CircleShape)
    )
}

@Composable
fun PlayersList(
    players: List<PublicPlayer>,
    turnOrder: List<String>,
    currentTurnId: String?,
    lastWordsPlayed: Map<String, String> = emptyMap(),
    eliminationVotes: Map<String, Map<String, String>> = emptyMap(),
    anonymousVotes: Boolean = false
) {
    val activePlayers = players.filter { !it.isSpectator }.sortedBy { p ->
        turnOrder.indexOf(p.id)
    }
    val eliminatedPlayers = players.filter { it.isSpectator }

    var selectedEliminatedId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
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
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 8.dp)
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                eliminatedPlayers.forEach { player ->
                    val hasVotes = player.id in eliminationVotes
                    PlayerCard(
                        player = player,
                        isCurrent = false,
                        lastWord = lastWordsPlayed[player.id],
                        onClick = if (hasVotes) ({ selectedEliminatedId = player.id }) else null
                    )
                }
            }
        }
    }

    selectedEliminatedId?.let { ejectedId ->
        val votes = eliminationVotes[ejectedId] ?: emptyMap()
        val ejectedPlayer = players.find { it.id == ejectedId }
        val voterIds = votes.filter { it.value == ejectedId }.keys.toList()

        AlertDialog(
            onDismissRequest = { selectedEliminatedId = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlayerDot(ejectedId)
                    Text(
                        ejectedPlayer?.name ?: "?",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Recibio ${voterIds.size} voto${if (voterIds.size != 1) "s" else ""}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    if (!anonymousVotes && voterIds.isNotEmpty()) {
                        voterIds.forEach { voterId ->
                            val voterName = players.find { it.id == voterId }?.name ?: "?"
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                PlayerDot(voterId)
                                Text(voterName, fontSize = 14.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedEliminatedId = null }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

@Composable
fun PlayerCard(
    player: PublicPlayer,
    isCurrent: Boolean,
    lastWord: String?,
    onClick: (() -> Unit)? = null
) {
    val containerColor = when {
        player.isSpectator -> MaterialTheme.colorScheme.primary.copy(alpha = 0.03f)
        isCurrent -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
    }

    val baseModifier = Modifier.fillMaxWidth(0.45f)
    val cardModifier = if (onClick != null) {
        baseModifier
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable { onClick() }
    } else {
        baseModifier
    }

    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (!player.isSpectator)
            BorderStroke(1.5.dp, playerColor(player.id).copy(alpha = if (isCurrent) 0.9f else 0.45f))
        else if (onClick != null)
            BorderStroke(1.dp, playerColor(player.id).copy(alpha = 0.35f))
        else null,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCurrent) 6.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                PlayerDot(player.id)
                Text(
                    text = player.name,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp,
                    color = if (player.isSpectator)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    else if (isCurrent)
                        MaterialTheme.colorScheme.onTertiary
                    else
                        MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
            if (lastWord != null) {
                Text(
                    text = "\"$lastWord\"",
                    fontSize = 11.sp,
                    color = if (isCurrent)
                        MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.75f)
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    maxLines = 1
                )
            }
            if (onClick != null) {
                Text(
                    text = "Ver votos",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}
