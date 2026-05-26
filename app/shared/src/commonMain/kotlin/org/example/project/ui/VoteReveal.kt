package org.example.project.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.i18n.Strings
import org.example.project.model.RoomSnapshot
import org.example.project.protocol.NOBODY_VOTE_ID

@Composable
fun VoteReveal(room: RoomSnapshot, votes: Map<String, String>, language: String, anonymousVotes: Boolean = false) {
    if (votes.isEmpty()) return

    fun nameOf(id: String): String = when (id) {
        NOBODY_VOTE_ID -> Strings.get("voting_nobody_name", language)
        else -> room.players.find { it.id == id }?.name ?: "?"
    }

    val targets = votes.values.distinct().sortedByDescending { target ->
        votes.values.count { it == target }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            Strings.get("votes_title", language),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        targets.forEach { targetId ->
            val voters = votes.filter { it.value == targetId }.keys
            val voteCount = voters.size

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Text(
                        text = "${nameOf(targetId)}  ($voteCount)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!anonymousVotes) {
                        voters.forEach { voterId ->
                            Text(
                                nameOf(voterId),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}
