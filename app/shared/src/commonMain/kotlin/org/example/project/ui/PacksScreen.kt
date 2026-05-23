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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.GameViewModel
import org.example.project.i18n.Strings

@Composable
fun PacksScreen(viewModel: GameViewModel) {
    val state = viewModel.state.collectAsState()
    val language = state.value.settings.language
    val loggedIn = state.value.authUsername != null

    var statusMsg by remember { mutableStateOf("") }

    LaunchedEffect(loggedIn) {
        if (loggedIn) {
            viewModel.loadUserPacks()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = Strings.get("packs_title", language),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        if (!loggedIn) {
            Text(Strings.get("packs_login_required", language))
        } else {
            if (statusMsg.isNotBlank()) {
                Text(statusMsg, fontWeight = FontWeight.Bold)
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(Strings.get("packs_new_category", language), fontWeight = FontWeight.Bold)
                    var catName by remember { mutableStateOf("") }
                    var catWords by remember { mutableStateOf("") }
                    TextField(
                        value = catName,
                        onValueChange = { catName = it },
                        label = { Text(Strings.get("packs_category_name", language)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextField(
                        value = catWords,
                        onValueChange = { catWords = it },
                        label = { Text(Strings.get("packs_words_label", language)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            val words = catWords.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                            viewModel.createCategory(catName.trim(), words) { ok ->
                                statusMsg = if (ok) Strings.get("packs_ok", language)
                                    else Strings.get("packs_fail", language)
                            }
                            catName = ""
                            catWords = ""
                        },
                        enabled = catName.isNotBlank() && catWords.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(Strings.get("packs_create", language))
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(Strings.get("packs_import_title", language), fontWeight = FontWeight.Bold)
                    var jsonText by remember { mutableStateOf("") }
                    TextField(
                        value = jsonText,
                        onValueChange = { jsonText = it },
                        label = { Text(Strings.get("packs_json_label", language)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            viewModel.importPackJson(jsonText.trim()) { ok ->
                                statusMsg = if (ok) Strings.get("packs_ok", language)
                                    else Strings.get("packs_fail", language)
                            }
                            jsonText = ""
                        },
                        enabled = jsonText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(Strings.get("packs_import", language))
                    }
                }
            }

            if (state.value.userPacks.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(Strings.get("packs_my_packs", language), fontWeight = FontWeight.Bold)
                        state.value.userPacks.forEach { pack ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(pack.name, fontWeight = FontWeight.Bold)
                                        Text(pack.language, fontSize = 12.sp)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.deletePack(pack.id) { ok ->
                                                statusMsg = if (ok) Strings.get("packs_deleted", language)
                                                    else Strings.get("packs_fail", language)
                                            }
                                        }
                                    ) {
                                        Text(Strings.get("packs_delete", language))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = { viewModel.closePacks() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(Strings.get("settings_back", language))
        }
    }
}
