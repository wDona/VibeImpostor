package org.example.project.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.project.GameViewModel
import org.example.project.i18n.Strings
import org.example.project.model.GameMode
import org.example.project.model.RoomConfig
import org.example.project.model.RoomSnapshot

@Composable
fun RoomConfigPanel(
    viewModel: GameViewModel,
    room: RoomSnapshot,
    isHost: Boolean,
    language: String
) {
    val config = room.config

    Text(Strings.get("lobby_config", language), fontWeight = FontWeight.Bold)

    if (!isHost) {
        val modeLabel = if (config.gameMode == GameMode.VOICE)
            Strings.get("lobby_voice", language) else Strings.get("lobby_text", language)
        val langLabel = if (config.language == "en")
            Strings.get("settings_english", language) else Strings.get("settings_spanish", language)
        Text("${Strings.get("lobby_game_mode", language)}: $modeLabel")
        Text("${Strings.get("lobby_impostors", language)}: ${config.numImpostors}")
        Text("${Strings.get("lobby_vote_time", language)}: ${config.voteTimeLimitSeconds}s")
        Text("${Strings.get("lobby_word_language", language)}: $langLabel")
        Text(Strings.get("lobby_only_host", language), fontWeight = FontWeight.Bold)
        return
    }

    fun apply(newConfig: RoomConfig) {
        viewModel.updateConfig(newConfig)
    }

    Text(Strings.get("lobby_game_mode", language))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ChoiceButton(
            text = Strings.get("lobby_voice", language),
            selected = config.gameMode == GameMode.VOICE,
            onClick = { apply(config.copy(gameMode = GameMode.VOICE)) }
        )
        ChoiceButton(
            text = Strings.get("lobby_text", language),
            selected = config.gameMode == GameMode.TEXT,
            onClick = { apply(config.copy(gameMode = GameMode.TEXT)) }
        )
    }

    StepperRow(
        label = Strings.get("lobby_impostors", language),
        value = config.numImpostors.toString(),
        onMinus = {
            if (config.numImpostors > 1) apply(config.copy(numImpostors = config.numImpostors - 1))
        },
        onPlus = {
            if (config.numImpostors < 8) apply(config.copy(numImpostors = config.numImpostors + 1))
        }
    )

    StepperRow(
        label = Strings.get("lobby_vote_time", language),
        value = "${config.voteTimeLimitSeconds}s",
        onMinus = {
            if (config.voteTimeLimitSeconds > 15)
                apply(config.copy(voteTimeLimitSeconds = config.voteTimeLimitSeconds - 15))
        },
        onPlus = {
            if (config.voteTimeLimitSeconds < 180)
                apply(config.copy(voteTimeLimitSeconds = config.voteTimeLimitSeconds + 15))
        }
    )

    Text(Strings.get("lobby_word_language", language))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ChoiceButton(
            text = Strings.get("settings_spanish", language),
            selected = config.language == "es",
            onClick = { apply(config.copy(language = "es")) }
        )
        ChoiceButton(
            text = Strings.get("settings_english", language),
            selected = config.language == "en",
            onClick = { apply(config.copy(language = "en")) }
        )
    }

    var showCategoryDialog by remember { mutableStateOf(false) }
    val categories = viewModel.state.value.availableCategories
    val selectedCount = config.selectedCategoryIds.size

    OutlinedButton(
        onClick = { showCategoryDialog = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        val resumen = if (selectedCount == 0)
            Strings.get("lobby_categories_none", language)
        else
            selectedCount.toString()
        Text("${Strings.get("lobby_categories", language)}: $resumen")
    }

    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text(Strings.get("lobby_categories", language)) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(Strings.get("lobby_categories_all", language))
                    categories.forEach { category ->
                        val selected = category.id in config.selectedCategoryIds
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selected,
                                onCheckedChange = { checked ->
                                    val newIds = if (checked) {
                                        config.selectedCategoryIds + category.id
                                    } else {
                                        config.selectedCategoryIds - category.id
                                    }
                                    apply(config.copy(selectedCategoryIds = newIds))
                                }
                            )
                            Text(category.name)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showCategoryDialog = false }) {
                    Text(Strings.get("common_done", language))
                }
            }
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(Strings.get("lobby_win_first_ejection", language))
        Checkbox(
            checked = config.winOnFirstEjection,
            onCheckedChange = { checked ->
                apply(config.copy(winOnFirstEjection = checked))
            }
        )
    }
}

@Composable
private fun ChoiceButton(text: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick) { Text(text) }
    } else {
        OutlinedButton(onClick = onClick) { Text(text) }
    }
}

@Composable
private fun StepperRow(
    label: String,
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("$label: $value", modifier = Modifier.padding(end = 8.dp))
        Button(onClick = onMinus) { Text("-") }
        Button(onClick = onPlus) { Text("+") }
    }
}
