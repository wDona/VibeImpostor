package org.example.project.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
    val playerName = remember { mutableStateOf("") }
    val roomCode = remember { mutableStateOf("") }
    val state = viewModel.state.collectAsState()
    val language = state.value.settings.language

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
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
                Text(Strings.get("settings_title", language), fontSize = 16.sp)
            }
        }

        TextField(
            value = playerName.value,
            onValueChange = { input -> playerName.value = input.take(20) },
            label = { Text(Strings.get("home_player_name", language)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        Button(
            onClick = {
                viewModel.setPlayerName(playerName.value.trim())
                viewModel.createRoom()
            },
            enabled = playerName.value.isNotBlank() && !state.value.connecting,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                if (state.value.connecting) Strings.get("home_connecting", language)
                else Strings.get("home_create_room", language)
            )
        }

        TextField(
            value = roomCode.value,
            onValueChange = { input ->
                roomCode.value = input.uppercase().filter { it.isLetterOrDigit() }.take(6)
            },
            label = { Text(Strings.get("home_room_code", language)) },
            singleLine = true,
            isError = roomCode.value.isNotEmpty() && roomCode.value.length < 6,
            supportingText = { Text(Strings.get("home_code_hint", language)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        Button(
            onClick = {
                viewModel.setPlayerName(playerName.value.trim())
                viewModel.joinRoom(roomCode.value)
            },
            enabled = playerName.value.isNotBlank() &&
                roomCode.value.length == 6 &&
                !state.value.connecting,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                if (state.value.connecting) Strings.get("home_connecting", language)
                else Strings.get("home_join_room", language)
            )
        }
    }
}
