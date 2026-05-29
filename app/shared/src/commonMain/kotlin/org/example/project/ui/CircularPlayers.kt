package org.example.project.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.BorderStroke
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CircularPlayers(
    players: List<PublicPlayer>,
    currentTurnId: String?,
    lastWordsPlayed: Map<String, String> = emptyMap(),
    center: @Composable () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().aspectRatio(2f)
    ) {
        val side = if (maxWidth < maxHeight) maxWidth else maxHeight
        val radius = side.value / 2.5f - 40f

        Box(modifier = Modifier.align(Alignment.Center)) {
            center()
        }

        players.forEachIndexed { index, player ->
            val angle = 2.0 * PI * index / players.size - PI / 2.0
            val x = (radius * cos(angle)).toFloat()
            val y = (radius * sin(angle)).toFloat()
            val isCurrent = player.id == currentTurnId

            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = x.dp, y = y.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrent)
                        MaterialTheme.colorScheme.tertiary
                    else
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
                ),
                border = if (!player.isSpectator)
                    BorderStroke(1.5.dp, playerColor(player.colorIndex).copy(alpha = if (isCurrent) 0.95f else 0.5f))
                else null
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = player.name,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = if (player.isSpectator)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                    val lastWord = lastWordsPlayed[player.id]
                    if (lastWord != null) {
                        Text(
                            text = "\"$lastWord\"",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
