package org.example.project.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.i18n.Strings
import org.example.project.model.RoomSnapshot
import org.example.project.protocol.NOBODY_VOTE_ID

@Composable
fun VoteReveal(
    room: RoomSnapshot,
    votes: Map<String, String>,
    language: String,
    anonymousVotes: Boolean = false,
    voteTypes: Map<String, Boolean> = emptyMap()
) {
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
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            Strings.get("votes_title", language),
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )

        targets.forEach { targetId ->
            val voters = votes.filter { it.value == targetId }.keys
            val voteCount = voters.size
            val targetIsPlayer = room.players.any { it.id == targetId }

            GameCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Radii.md)
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (targetIsPlayer) PlayerDot(room.players.find { it.id == targetId }?.colorIndex ?: 0)
                            Text(
                                text = nameOf(targetId),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Text(
                            text = "$voteCount",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (!anonymousVotes) {
                        voters.forEach { voterId ->
                            val voter = room.players.find { it.id == voterId }
                            val voterIsPlayer = voter != null
                            val isHard = voteTypes[voterId] ?: true
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                if (voterIsPlayer) PlayerDot(voter?.colorIndex ?: 0)
                                Text(
                                    nameOf(voterId),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                )
                                if (voteTypes.isNotEmpty()) {
                                    Text(
                                        text = if (isHard) "🔴" else "🟡",
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
