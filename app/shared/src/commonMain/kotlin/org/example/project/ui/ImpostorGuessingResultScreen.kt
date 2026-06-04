package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = Strings.get("impostor_guessing_results", language),
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )

        impostorGuesses.forEach { (impostorId, result) ->
            val impostor = room.players.find { it.id == impostorId }
            val guessed = result.guessed

            GameCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (guessed)
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF14532D), Color(0xFF16A34A))
                                )
                            else
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF7F1D1D), Color(0xFFB91C1C))
                                )
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = impostor?.name ?: "?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = Color.White
                            )
                            Text(
                                text = if (guessed)
                                    Strings.get("impostor_guessed_correct", language)
                                else
                                    Strings.get("impostor_guessed_wrong", language),
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(
                                    Color.White.copy(alpha = 0.15f),
                                    RoundedCornerShape(Radii.sm)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (guessed) "OK" else "FAIL",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                letterSpacing = 1.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = { viewModel.continueToResults() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(top = 8.dp),
            shape = RoundedCornerShape(Radii.md)
        ) {
            Text(
                Strings.get("result_back_to_lobby", language),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
