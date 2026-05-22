package org.example.project.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.example.project.GameViewModel
import org.example.project.Screen
import org.example.project.settings.Theme

@Composable
fun AppRoot() {
    val viewModel = remember { GameViewModel() }
    val state = viewModel.state.collectAsState()
    val settings = state.value.settings

    val isDarkTheme = when (settings.theme) {
        Theme.LIGHT -> false
        Theme.DARK -> true
        Theme.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDarkTheme) AppDarkColors else AppLightColors
    val error = state.value.error
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        if (error != null) {
            snackbarHostState.showSnackbar(error)
            viewModel.dismissError()
        }
    }

    MaterialTheme(colorScheme = colorScheme) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (state.value.screen) {
                    Screen.HOME -> HomeScreen(viewModel)
                    Screen.LOBBY -> LobbyScreen(viewModel)
                    Screen.GAME -> GameScreen(viewModel)
                    Screen.VOTING -> VotingScreen(viewModel)
                    Screen.ROUND_RESULT -> RoundResultScreen(viewModel)
                    Screen.IMPOSTOR_GUESSING -> ImpostorGuessingScreen(viewModel)
                    Screen.IMPOSTOR_GUESSING_RESULT -> ImpostorGuessingResultScreen(viewModel)
                    Screen.RESULT -> ResultScreen(viewModel)
                    Screen.SETTINGS -> SettingsScreen(viewModel)
                    Screen.PACKS -> PacksScreen(viewModel)
                }
            }
        }
    }
}
