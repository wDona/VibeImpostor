package org.example.project.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                Strings.get("home_title", language),
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Button(onClick = { viewModel.openSettings() }) {
                Text(Strings.get("settings_title", language), fontSize = 14.sp)
            }
        }

        TextField(
            value = playerName.value,
            onValueChange = { input -> playerName.value = input.take(20) },
            label = { Text(Strings.get("home_player_name", language)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    Strings.get("home_create_room", language),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = {
                        viewModel.setPlayerName(playerName.value.trim())
                        viewModel.createRoom()
                    },
                    enabled = playerName.value.isNotBlank() && !state.value.connecting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (state.value.connecting) Strings.get("home_connecting", language)
                        else Strings.get("home_create_room", language)
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    Strings.get("home_join_room", language),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                TextField(
                    value = roomCode.value,
                    onValueChange = { input ->
                        roomCode.value = input.uppercase().filter { it.isLetterOrDigit() }.take(6)
                    },
                    label = { Text(Strings.get("home_room_code", language)) },
                    singleLine = true,
                    isError = roomCode.value.isNotEmpty() && roomCode.value.length < 6,
                    supportingText = { Text(Strings.get("home_code_hint", language)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        viewModel.setPlayerName(playerName.value.trim())
                        viewModel.joinRoom(roomCode.value)
                    },
                    enabled = playerName.value.isNotBlank() &&
                        roomCode.value.length == 6 &&
                        !state.value.connecting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (state.value.connecting) Strings.get("home_connecting", language)
                        else Strings.get("home_join_room", language)
                    )
                }
            }
        }
    }
}
