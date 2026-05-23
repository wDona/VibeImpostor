package org.example.project.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.sp
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

    Text(Strings.get("lobby_config", language), fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 12.dp))

    if (!isHost) {
        val modeLabel = if (config.gameMode == GameMode.VOICE)
            Strings.get("lobby_voice", language) else Strings.get("lobby_text", language)
        val langLabel = if (config.language == "en")
            Strings.get("settings_english", language) else Strings.get("settings_spanish", language)
        val maxImpostors = room.players.size
        val impostersLabel = if (maxImpostors == 1) "1" else "0-${config.numImpostors}"
        Text("${Strings.get("lobby_game_mode", language)}: $modeLabel")
        Text("${Strings.get("lobby_impostors", language)}: $impostersLabel")
        Text("${Strings.get("lobby_vote_time", language)}: ${config.voteTimeLimitSeconds}s")
        Text("${Strings.get("lobby_word_language", language)}: $langLabel")
        Text(Strings.get("lobby_only_host", language), fontWeight = FontWeight.Bold)
        return
    }

    fun apply(newConfig: RoomConfig) {
        viewModel.updateConfig(newConfig)
    }

    val maxImpostors = room.players.size

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ConfigSection(
                title = Strings.get("lobby_game_mode", language),
                content = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChoiceButton(
                            text = Strings.get("lobby_voice", language),
                            selected = config.gameMode == GameMode.VOICE,
                            onClick = { apply(config.copy(gameMode = GameMode.VOICE)) },
                            modifier = Modifier.weight(1f)
                        )
                        ChoiceButton(
                            text = Strings.get("lobby_text", language),
                            selected = config.gameMode == GameMode.TEXT,
                            onClick = { apply(config.copy(gameMode = GameMode.TEXT)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            )

            ConfigSection(
                title = Strings.get("lobby_max_impostors", language),
                content = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(config.numImpostors.toString(), fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (!config.winOnFirstEjection && config.numImpostors > 1)
                                        apply(config.copy(numImpostors = config.numImpostors - 1))
                                },
                                enabled = !config.winOnFirstEjection && config.numImpostors > 1,
                                modifier = Modifier.weight(1f)
                            ) { Text("-") }
                            Button(
                                onClick = {
                                    if (!config.winOnFirstEjection && config.numImpostors < maxImpostors)
                                        apply(config.copy(numImpostors = config.numImpostors + 1))
                                },
                                enabled = !config.winOnFirstEjection && config.numImpostors < maxImpostors,
                                modifier = Modifier.weight(1f)
                            ) { Text("+") }
                        }
                        if (config.numImpostors >= 2) {
                            Text(
                                text = Strings.get("lobby_impostor_no_chance", language),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ConfigSection(
                title = Strings.get("lobby_word_language", language),
                content = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChoiceButton(
                            text = Strings.get("settings_spanish", language),
                            selected = config.language == "es",
                            onClick = { apply(config.copy(language = "es")) },
                            modifier = Modifier.weight(1f)
                        )
                        ChoiceButton(
                            text = Strings.get("settings_english", language),
                            selected = config.language == "en",
                            onClick = { apply(config.copy(language = "en")) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            )

            ConfigSection(
                title = Strings.get("lobby_vote_time", language),
                content = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${config.voteTimeLimitSeconds}s", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (config.voteTimeLimitSeconds > 15)
                                        apply(config.copy(voteTimeLimitSeconds = config.voteTimeLimitSeconds - 15))
                                },
                                enabled = config.voteTimeLimitSeconds > 15,
                                modifier = Modifier.weight(1f)
                            ) { Text("-") }
                            Button(
                                onClick = {
                                    if (config.voteTimeLimitSeconds < 180)
                                        apply(config.copy(voteTimeLimitSeconds = config.voteTimeLimitSeconds + 15))
                                },
                                enabled = config.voteTimeLimitSeconds < 180,
                                modifier = Modifier.weight(1f)
                            ) { Text("+") }
                        }
                    }
                }
            )
        }
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
                    val currentLang = config.language
                    val filteredCategories = categories.filter { it.language == currentLang }
                    filteredCategories.forEach { category ->
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                val newValue = !config.winOnFirstEjection
                if (newValue) {
                    apply(config.copy(winOnFirstEjection = true, numImpostors = 1))
                } else {
                    apply(config.copy(winOnFirstEjection = false))
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(Strings.get("lobby_win_first_ejection", language))
        Checkbox(
            checked = config.winOnFirstEjection,
            onCheckedChange = { checked ->
                if (checked) {
                    apply(config.copy(winOnFirstEjection = true, numImpostors = 1))
                } else {
                    apply(config.copy(winOnFirstEjection = false))
                }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { apply(config.copy(anonymousVotes = !config.anonymousVotes)) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(Strings.get("lobby_anonymous_votes", language))
        Checkbox(
            checked = config.anonymousVotes,
            onCheckedChange = { checked ->
                apply(config.copy(anonymousVotes = checked))
            }
        )
    }
}

@Composable
private fun ConfigSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        content()
    }
}

@Composable
private fun ChoiceButton(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier) { Text(text) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) { Text(text) }
    }
}
