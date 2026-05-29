package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.GameViewModel
import org.example.project.i18n.Strings
import org.example.project.model.GameMode
import org.example.project.model.RoomConfig
import org.example.project.model.RoomSnapshot

private data class SpecialModeOption(val key: String, val labelKey: String, val descKey: String)

private val SPECIAL_MODE_OPTIONS = listOf(
    SpecialModeOption("normal",           "lobby_variant_normal",       "lobby_variant_normal_desc"),
    SpecialModeOption("noCategory",       "lobby_variant_no_category",  "lobby_variant_no_category_desc"),
    SpecialModeOption("hiddenRole",       "lobby_variant_hidden_role",  "lobby_variant_hidden_role_desc"),
    SpecialModeOption("progressiveHints", "lobby_progressive_hints",    "lobby_progressive_hints_desc"),
    SpecialModeOption("hiddenImpostor",   "lobby_hidden_impostor",      "lobby_hidden_impostor_desc"),
    SpecialModeOption("random",           "lobby_variant_random",       "lobby_variant_random_desc")
)

private fun activeSpecialMode(config: RoomConfig) = when {
    config.randomVariant    -> "random"
    config.hiddenImpostor   -> "hiddenImpostor"
    config.progressiveHints -> "progressiveHints"
    config.hiddenRole       -> "hiddenRole"
    config.noCategory       -> "noCategory"
    else                    -> "normal"
}

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
        val impostersLabel = if (config.numImpostors == 1) "1" else "0-${config.numImpostors}"
        val packsLabel = if (config.selectedCategoryIds.isEmpty())
            Strings.get("lobby_word_packs_all", language) else config.selectedCategoryIds.size.toString()

        Text("${Strings.get("lobby_game_mode", language)}: $modeLabel")
        Text("${Strings.get("lobby_impostors", language)}: $impostersLabel")
        Text("${Strings.get("lobby_vote_time", language)}: ${config.voteTimeLimitSeconds}s")
        Text("${Strings.get("lobby_word_language", language)}: $langLabel")
        Text("${Strings.get("lobby_word_packs_selected", language)}: $packsLabel")

        fun boolLabel(value: Boolean) =
            if (value) Strings.get("lobby_enabled", language) else Strings.get("lobby_disabled", language)

        val activeMode = SPECIAL_MODE_OPTIONS.first { it.key == activeSpecialMode(config) }
        Text("${Strings.get("lobby_game_variant", language)}: ${Strings.get(activeMode.labelKey, language)}")
        if (activeMode.key != "normal" && activeMode.key != "random") {
            Text(
                Strings.get(activeMode.descKey, language),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text("${Strings.get("lobby_win_first_ejection", language)}: ${boolLabel(config.winOnFirstEjection)}")
        Text("${Strings.get("lobby_anonymous_votes", language)}: ${boolLabel(config.anonymousVotes)}")
        Text("${Strings.get("lobby_single_word_round", language)}: ${boolLabel(config.singleWordRound)}")
        Text("${Strings.get("lobby_punishment_vote", language)}: ${boolLabel(config.punishmentVote)}")

        Text(Strings.get("lobby_only_host", language), fontWeight = FontWeight.Bold)
        return
    }

    val updateConfig: (RoomConfig) -> Unit = { newConfig -> viewModel.updateConfig(newConfig) }

    val maxImpostors = room.players.size

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val isNarrow = maxWidth < 420.dp
        if (isNarrow) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ConfigSectionsColumn1(config, language, maxImpostors, updateConfig)
                ConfigSectionsColumn2(config, language, maxImpostors, updateConfig)
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ConfigSectionsColumn1(config, language, maxImpostors, updateConfig)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ConfigSectionsColumn2(config, language, maxImpostors, updateConfig)
                }
            }
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
                    val allSelected = filteredCategories.isNotEmpty() && filteredCategories.all { it.id in config.selectedCategoryIds }

                    Button(
                        onClick = {
                            val newIds = if (allSelected) {
                                config.selectedCategoryIds.filter { id ->
                                    id !in filteredCategories.map { it.id }
                                }
                            } else {
                                config.selectedCategoryIds + filteredCategories.map { it.id }
                            }
                            updateConfig(config.copy(selectedCategoryIds = newIds))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (allSelected)
                                Strings.get("lobby_deselect_all_categories", language)
                            else
                                Strings.get("lobby_select_all_categories", language)
                        )
                    }

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
                                    updateConfig(config.copy(selectedCategoryIds = newIds))
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

    // --- Special game variant selector ---
    SpecialModeSection(config, language, updateConfig)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                val newValue = !config.winOnFirstEjection
                if (newValue) {
                    updateConfig(config.copy(winOnFirstEjection = true, numImpostors = 1))
                } else {
                    updateConfig(config.copy(winOnFirstEjection = false))
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
                    updateConfig(config.copy(winOnFirstEjection = true, numImpostors = 1))
                } else {
                    updateConfig(config.copy(winOnFirstEjection = false))
                }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { updateConfig(config.copy(anonymousVotes = !config.anonymousVotes)) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(Strings.get("lobby_anonymous_votes", language))
        Checkbox(
            checked = config.anonymousVotes,
            onCheckedChange = { checked ->
                updateConfig(config.copy(anonymousVotes = checked))
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                val newVal = !config.singleWordRound
                if (newVal) {
                    updateConfig(config.copy(singleWordRound = true, numImpostors = 1))
                } else {
                    updateConfig(config.copy(singleWordRound = false))
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(Strings.get("lobby_single_word_round", language))
        Checkbox(
            checked = config.singleWordRound,
            onCheckedChange = { checked ->
                if (checked) {
                    updateConfig(config.copy(singleWordRound = true, numImpostors = 1))
                } else {
                    updateConfig(config.copy(singleWordRound = false))
                }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { updateConfig(config.copy(punishmentVote = !config.punishmentVote)) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(Strings.get("lobby_punishment_vote", language))
            Text(
                Strings.get("lobby_punishment_vote_desc", language),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Checkbox(
            checked = config.punishmentVote,
            onCheckedChange = { checked -> updateConfig(config.copy(punishmentVote = checked)) }
        )
    }
}

@Composable
private fun SpecialModeSection(
    config: RoomConfig,
    language: String,
    updateConfig: (RoomConfig) -> Unit
) {
    val activeKey = activeSpecialMode(config)

    ConfigSection(
        title = Strings.get("lobby_game_variant", language),
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SPECIAL_MODE_OPTIONS.forEach { option ->
                    val isSelected = activeKey == option.key
                    val shape = RoundedCornerShape(12.dp)
                    val borderColor = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    val bgColor = if (isSelected)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
                    else
                        Color.Transparent
                    val titleColor = if (isSelected)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    val descAlpha = if (isSelected) 1f else 0.5f

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = borderColor,
                                shape = shape
                            )
                            .background(bgColor)
                            .clickable {
                                updateConfig(
                                    config.copy(
                                        randomVariant    = option.key == "random",
                                        noCategory       = option.key == "noCategory",
                                        hiddenRole       = option.key == "hiddenRole",
                                        progressiveHints = option.key == "progressiveHints",
                                        hiddenImpostor   = option.key == "hiddenImpostor",
                                        numImpostors     = if (option.key == "hiddenImpostor") 1 else config.numImpostors
                                    )
                                )
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RadioButton(selected = isSelected, onClick = null)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                Strings.get(option.labelKey, language),
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 14.sp,
                                color = titleColor
                            )
                            Text(
                                Strings.get(option.descKey, language),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = descAlpha)
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun ConfigSectionsColumn1(
    config: RoomConfig,
    language: String,
    maxImpostors: Int,
    updateConfig: (RoomConfig) -> Unit
) {
    ConfigSection(
        title = Strings.get("lobby_game_mode", language),
        content = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceButton(
                    text = Strings.get("lobby_voice", language),
                    selected = config.gameMode == GameMode.VOICE,
                    onClick = { updateConfig(config.copy(gameMode = GameMode.VOICE)) },
                    modifier = Modifier.weight(1f)
                )
                ChoiceButton(
                    text = Strings.get("lobby_text", language),
                    selected = config.gameMode == GameMode.TEXT,
                    onClick = { updateConfig(config.copy(gameMode = GameMode.TEXT)) },
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
                    val impostorLocked = config.winOnFirstEjection || config.singleWordRound || config.hiddenImpostor
                    Button(
                        onClick = {
                            if (!impostorLocked && config.numImpostors > 1)
                                updateConfig(config.copy(numImpostors = config.numImpostors - 1))
                        },
                        enabled = !impostorLocked && config.numImpostors > 1,
                        modifier = Modifier.weight(1f)
                    ) { Text("-") }
                    Button(
                        onClick = {
                            if (!impostorLocked && config.numImpostors < maxImpostors)
                                updateConfig(config.copy(numImpostors = config.numImpostors + 1))
                        },
                        enabled = !impostorLocked && config.numImpostors < maxImpostors,
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

@Composable
private fun ConfigSectionsColumn2(
    config: RoomConfig,
    language: String,
    maxImpostors: Int,
    updateConfig: (RoomConfig) -> Unit
) {
    ConfigSection(
        title = Strings.get("lobby_word_language", language),
        content = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceButton(
                    text = Strings.get("settings_spanish", language),
                    selected = config.language == "es",
                    onClick = { updateConfig(config.copy(language = "es")) },
                    modifier = Modifier.weight(1f)
                )
                ChoiceButton(
                    text = Strings.get("settings_english", language),
                    selected = config.language == "en",
                    onClick = { updateConfig(config.copy(language = "en")) },
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
                                updateConfig(config.copy(voteTimeLimitSeconds = config.voteTimeLimitSeconds - 15))
                        },
                        enabled = config.voteTimeLimitSeconds > 15,
                        modifier = Modifier.weight(1f)
                    ) { Text("-") }
                    Button(
                        onClick = {
                            if (config.voteTimeLimitSeconds < 180)
                                updateConfig(config.copy(voteTimeLimitSeconds = config.voteTimeLimitSeconds + 15))
                        },
                        enabled = config.voteTimeLimitSeconds < 180,
                        modifier = Modifier.weight(1f)
                    ) { Text("+") }
                }
            }
        }
    )
}

@Composable
private fun ConfigSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
