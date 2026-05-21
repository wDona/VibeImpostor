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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
