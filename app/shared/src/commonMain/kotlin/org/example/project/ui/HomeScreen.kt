package org.example.project.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Button(onClick = { viewModel.openSettings() }) {
                Text("⚙", fontSize = 16.sp)
            }
        }

        TextField(
            value = playerName.value,
            onValueChange = { playerName.value = it },
            label = { Text(Strings.get("home_player_name", language)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        Button(
            onClick = {
                viewModel.setPlayerName(playerName.value)
                viewModel.createRoom()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(Strings.get("home_create_room", language))
        }

        TextField(
            value = roomCode.value,
            onValueChange = { roomCode.value = it },
            label = { Text(Strings.get("home_room_code", language)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        Button(
            onClick = {
                viewModel.setPlayerName(playerName.value)
                viewModel.joinRoom(roomCode.value)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(Strings.get("home_join_room", language))
        }
    }
}
