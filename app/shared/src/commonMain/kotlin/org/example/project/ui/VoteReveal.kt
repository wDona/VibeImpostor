package org.example.project.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.i18n.Strings
import org.example.project.model.RoomSnapshot

private val voteColors = listOf(
    Color(0xFF1565C0),
    Color(0xFFC62828),
    Color(0xFF2E7D32),
    Color(0xFF6A1B9A),
    Color(0xFFEF6C00),
    Color(0xFF00838F),
    Color(0xFFAD1457),
    Color(0xFF558B2F)
)

@Composable
fun VoteReveal(room: RoomSnapshot, votes: Map<String, String>, language: String, anonymousVotes: Boolean = false) {
    if (votes.isEmpty()) return

    fun nameOf(id: String): String = room.players.find { it.id == id }?.name ?: "?"

    val targets = votes.values.distinct().sortedBy { nameOf(it) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(Strings.get("votes_title", language), fontWeight = FontWeight.Bold)

        targets.forEach { targetId ->
            val voters = votes.filter { it.value == targetId }.keys
            val voteCount = voters.size

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF424242).copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (anonymousVotes) {
                        Text(
                            text = voteCount.toString(),
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "${nameOf(targetId)} (${voteCount})",
                            fontWeight = FontWeight.Bold
                        )
                        voters.forEach { voterId ->
                            Text(nameOf(voterId), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
