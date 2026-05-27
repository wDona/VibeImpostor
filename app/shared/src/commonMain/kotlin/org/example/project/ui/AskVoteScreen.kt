package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.example.project.GameViewModel
import org.example.project.i18n.Strings

@Composable
fun AskVoteScreen(viewModel: GameViewModel) {
    val state = viewModel.state.collectAsState()
    val room = state.value.room ?: return
    val language = state.value.settings.language
    val answered = state.value.answeredAskVote

    val secondsLeft = remember { mutableStateOf(30) }
    LaunchedEffect(Unit) {
        while (secondsLeft.value > 0) {
            delay(1000)
            secondsLeft.value--
        }
    }

    val timerColor = when {
        secondsLeft.value > 20 -> MaterialTheme.colorScheme.primary
        secondsLeft.value > 10 -> Color(0xFFF59E0B)
        else -> MaterialTheme.colorScheme.error
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = Strings.get("game_ask_vote_title", language),
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            letterSpacing = 1.sp
        )

        Text(
            text = Strings.get("game_ask_vote_text", language),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Box(
            modifier = Modifier
                .background(timerColor.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                .padding(horizontal = 36.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${secondsLeft.value}s",
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = timerColor
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        if (!answered) {
            Button(
                onClick = { viewModel.respondVote(true) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = Strings.get("common_yes", language),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = { viewModel.respondVote(false) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = Strings.get("common_no", language),
                    fontSize = 18.sp
                )
            }
        } else {
            GameCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = Strings.get("ask_vote_waiting", language),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        LeaveGameButton(viewModel, language)
    }
}
