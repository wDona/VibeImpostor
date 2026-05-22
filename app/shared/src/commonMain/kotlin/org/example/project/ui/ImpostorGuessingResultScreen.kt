package org.example.project.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.GameViewModel
import org.example.project.i18n.Strings

@Composable
fun ImpostorGuessingResultScreen(viewModel: GameViewModel) {
    val state = viewModel.state.collectAsState()
    val room = state.value.room ?: return
    val language = state.value.settings.language
    val impostorGuesses = state.value.impostorGuesses

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = Strings.get("impostor_guessing_results", language),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        impostorGuesses.forEach { (impostorId, result) ->
            val impostor = room.players.find { it.id == impostorId }
            val guessed = result.guessed

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (guessed)
                        Color(0xFF2E7D32).copy(alpha = 0.3f)
                    else
                        Color(0xFFC62828).copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = impostor?.name ?: "?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = if (guessed)
                            Strings.get("impostor_guessed_correct", language)
                        else
                            Strings.get("impostor_guessed_wrong", language),
                        fontSize = 14.sp,
                        color = if (guessed)
                            Color(0xFF2E7D32)
                        else
                            Color(0xFFC62828)
                    )
                }
            }
        }

        Button(
            onClick = { viewModel.continueToResults() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text(Strings.get("result_back_to_lobby", language))
        }
    }
}
