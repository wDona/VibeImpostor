package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = Strings.get("packs_title", language),
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        if (!loggedIn) {
            Text(
                Strings.get("packs_login_required", language),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        } else {
            if (statusMsg.isNotBlank()) {
                Text(
                    statusMsg,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // New category card
            GameCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        Strings.get("packs_new_category", language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    var catName by remember { mutableStateOf("") }
                    var catWords by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = catName,
                        onValueChange = { catName = it },
                        label = { Text(Strings.get("packs_category_name", language)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = catWords,
                        onValueChange = { catWords = it },
                        label = { Text(Strings.get("packs_words_label", language)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(Strings.get("packs_create", language), fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Import JSON card
            GameCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        Strings.get("packs_import_title", language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    var jsonText by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = jsonText,
                        onValueChange = { jsonText = it },
                        label = { Text(Strings.get("packs_json_label", language)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(Strings.get("packs_import", language), fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // My packs list
            if (state.value.userPacks.isNotEmpty()) {
                GameCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            Strings.get("packs_my_packs", language),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        state.value.userPacks.forEach { pack ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(pack.name, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        pack.language,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        viewModel.deletePack(pack.id) { ok ->
                                            statusMsg = if (ok) Strings.get("packs_deleted", language)
                                                else Strings.get("packs_fail", language)
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(Strings.get("packs_delete", language))
                                }
                            }
                        }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = { viewModel.closePacks() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(Strings.get("settings_back", language))
        }
    }
}
