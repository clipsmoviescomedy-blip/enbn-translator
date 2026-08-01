package com.example.enbntranslator.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.enbntranslator.TranslatorViewModel
import com.example.enbntranslator.data.HistoryEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorScreen(viewModel: TranslatorViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("English → বাংলা") },
                actions = {
                    if (state.history.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearHistory() }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear history")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            if (!state.isDictionaryReady && state.errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        state.errorMessage!!,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = state.inputText,
                onValueChange = viewModel::onInputChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("English") },
                minLines = 3,
                maxLines = 6,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { viewModel.translate() })
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { viewModel.translate() },
                    enabled = state.inputText.isNotBlank() && !state.isTranslating
                ) {
                    Icon(Icons.Default.SwapVert, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Translate")
                }
            }

            Spacer(Modifier.height(16.dp))

            state.result?.let { result ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "বাংলা",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            result.translatedText.ifBlank { "—" },
                            style = MaterialTheme.typography.headlineSmall
                        )

                        if (result.unmatchedWords.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Not in dictionary: ${result.unmatchedWords.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            if (state.isTranslating) {
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(24.dp))

            if (state.history.isNotEmpty()) {
                Text(
                    "History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(state.history) { entry -> HistoryRow(entry) }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: HistoryEntry) {
    val timeFormat = remember(entry.timestampMillis) {
        SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
            .format(Date(entry.timestampMillis))
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(entry.englishText, style = MaterialTheme.typography.bodyMedium)
            Text(entry.bengaliText, style = MaterialTheme.typography.bodyLarge)
            Text(
                timeFormat,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
