package org.example.project.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.project.model.PublicPlayer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CircularPlayers(
    players: List<PublicPlayer>,
    currentTurnId: String?,
    center: @Composable () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
    ) {
        val side = if (maxWidth < maxHeight) maxWidth else maxHeight
        val radius = side.value / 2f - 56f

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
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = player.name,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (player.isSpectator)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
