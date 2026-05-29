package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.GameViewModel
import org.example.project.i18n.Strings

@Composable
fun HomeScreen(viewModel: GameViewModel) {
    val state = viewModel.state.collectAsState()
    val playerName = remember { mutableStateOf(viewModel.state.value.settings.lastPlayerName) }
    val roomCode = remember { mutableStateOf(viewModel.state.value.settings.lastRoomCode) }
    val language = state.value.settings.language

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero header - full width gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            TextButton(
                onClick = { viewModel.openSettings() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
            ) {
                Text(Strings.get("settings_title", language), fontSize = 13.sp)
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .align(Alignment.Center)
                    .padding(horizontal = 28.dp, vertical = 40.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(ImpostorRed, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                    )
                }
                Text(
                    "IMPOSTOR",
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(4.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(MaterialTheme.colorScheme.primary, ImpostorRed)
                            ),
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        }

        // Form content - centered with max width
        Column(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = playerName.value,
                onValueChange = { input -> playerName.value = input.take(20) },
                label = { Text(Strings.get("home_player_name", language)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            GameCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        Strings.get("home_create_room", language),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = {
                            viewModel.setPlayerName(playerName.value.trim())
                            viewModel.createRoom()
                        },
                        enabled = playerName.value.isNotBlank() && !state.value.connecting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            if (state.value.connecting) Strings.get("home_connecting", language)
                            else Strings.get("home_create_room", language),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            GameCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        Strings.get("home_join_room", language),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val clipboardManager = LocalClipboardManager.current
                    OutlinedTextField(
                        value = roomCode.value,
                        onValueChange = { input ->
                            roomCode.value = input.uppercase().filter { it.isLetterOrDigit() }.take(6)
                        },
                        label = { Text(Strings.get("home_room_code", language)) },
                        singleLine = true,
                        isError = roomCode.value.isNotEmpty() && roomCode.value.length < 6,
                        supportingText = { Text(Strings.get("home_code_hint", language)) },
                        trailingIcon = {
                            TextButton(
                                onClick = {
                                    val clipboard = clipboardManager.getText()
                                    if (clipboard != null) {
                                        val pasted = clipboard.text.uppercase().filter { it.isLetterOrDigit() }.take(6)
                                        roomCode.value = pasted
                                    }
                                },
                                modifier = Modifier.width(80.dp)
                            ) {
                                Text(Strings.get("home_paste", language), fontSize = 12.sp)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    FilledTonalButton(
                        onClick = {
                            viewModel.setPlayerName(playerName.value.trim())
                            viewModel.joinRoom(roomCode.value)
                        },
                        enabled = playerName.value.isNotBlank() &&
                            roomCode.value.length == 6 &&
                            !state.value.connecting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            if (state.value.connecting) Strings.get("home_connecting", language)
                            else Strings.get("home_join_room", language),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
