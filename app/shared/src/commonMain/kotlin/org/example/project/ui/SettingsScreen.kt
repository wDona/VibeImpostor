package org.example.project.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
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
import org.example.project.settings.Theme
import org.example.project.settings.UserSettings

@Composable
fun SettingsScreen(viewModel: GameViewModel) {
    val state = viewModel.state.collectAsState()
    val settings = state.value.settings
    val language = settings.language

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = Strings.get("settings_title", language),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = Strings.get("settings_language", language),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.updateSettings(settings.copy(language = "es"))
                        }
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(Strings.get("settings_spanish", language))
                    RadioButton(
                        selected = settings.language == "es",
                        onClick = { viewModel.updateSettings(settings.copy(language = "es")) }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.updateSettings(settings.copy(language = "en"))
                        }
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(Strings.get("settings_english", language))
                    RadioButton(
                        selected = settings.language == "en",
                        onClick = { viewModel.updateSettings(settings.copy(language = "en")) }
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = Strings.get("settings_theme", language),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                ThemeOption("light", Strings.get("settings_light", language), settings, language, viewModel)
                ThemeOption("dark", Strings.get("settings_dark", language), settings, language, viewModel)
                ThemeOption("system", Strings.get("settings_system", language), settings, language, viewModel)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Strings.get("settings_sound", language),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Switch(
                    checked = settings.soundEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.updateSettings(settings.copy(soundEnabled = enabled))
                    }
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = Strings.get("settings_account", language),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                val authUser = state.value.authUsername
                if (authUser != null) {
                    Text("${Strings.get("auth_logged_as", language)}: $authUser")
                    OutlinedButton(onClick = { viewModel.logout() }) {
                        Text(Strings.get("auth_logout", language))
                    }
                    OutlinedButton(onClick = { viewModel.openPacks() }) {
                        Text(Strings.get("settings_packs", language))
                    }
                } else {
                    val username = remember { mutableStateOf("") }
                    val password = remember { mutableStateOf("") }
                    Text(Strings.get("auth_guest", language))
                    TextField(
                        value = username.value,
                        onValueChange = { username.value = it },
                        label = { Text(Strings.get("auth_username", language)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextField(
                        value = password.value,
                        onValueChange = { password.value = it },
                        label = { Text(Strings.get("auth_password", language)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.login(username.value, password.value) },
                            enabled = username.value.isNotBlank() && password.value.isNotBlank() && !state.value.authBusy
                        ) {
                            Text(Strings.get("auth_login", language))
                        }
                        OutlinedButton(
                            onClick = { viewModel.register(username.value, password.value) },
                            enabled = username.value.isNotBlank() && password.value.isNotBlank() && !state.value.authBusy
                        ) {
                            Text(Strings.get("auth_register", language))
                        }
                    }
                }
            }
        }

        Button(
            onClick = { viewModel.closeSettings() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text(Strings.get("settings_back", language))
        }
    }
}

@Composable
private fun ThemeOption(
    themeValue: String,
    label: String,
    settings: UserSettings,
    language: String,
    viewModel: GameViewModel
) {
    val theme = when (themeValue) {
        "light" -> Theme.LIGHT
        "dark" -> Theme.DARK
        else -> Theme.SYSTEM
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                viewModel.updateSettings(settings.copy(theme = theme))
            }
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        RadioButton(
            selected = settings.theme == theme,
            onClick = { viewModel.updateSettings(settings.copy(theme = theme)) }
        )
    }
}
