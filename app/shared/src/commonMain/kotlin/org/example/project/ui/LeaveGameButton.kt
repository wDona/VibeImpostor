package org.example.project.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.example.project.GameViewModel
import org.example.project.i18n.Strings

@Composable
fun LeaveGameButton(viewModel: GameViewModel, language: String) {
    var showConfirm by remember { mutableStateOf(false) }

    TextButton(
        onClick = { showConfirm = true },
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(Strings.get("game_leave", language))
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text(Strings.get("game_leave_confirm_title", language)) },
            text = { Text(Strings.get("game_leave_confirm_text", language)) },
            confirmButton = {
                Button(onClick = {
                    showConfirm = false
                    viewModel.leaveRoom()
                }) {
                    Text(Strings.get("game_leave", language))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text(Strings.get("common_cancel", language))
                }
            }
        )
    }
}
