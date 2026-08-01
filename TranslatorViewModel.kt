package com.example.enbntranslator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.enbntranslator.data.AppDatabase
import com.example.enbntranslator.data.HistoryEntry
import com.example.enbntranslator.translation.DictionaryTranslator
import com.example.enbntranslator.translation.TranslationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TranslatorUiState(
    val inputText: String = "",
    val result: TranslationResult? = null,
    val isTranslating: Boolean = false,
    val isDictionaryReady: Boolean = false,
    val history: List<HistoryEntry> = emptyList(),
    val errorMessage: String? = null
)

class TranslatorViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val translator = DictionaryTranslator(db.dictionaryDao())

    private val _uiState = MutableStateFlow(TranslatorUiState())
    val uiState: StateFlow<TranslatorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val ready = runCatching { translator.isReady() }.getOrDefault(false)
            _uiState.value = _uiState.value.copy(
                isDictionaryReady = ready,
                errorMessage = if (ready) null else
                    "Dictionary not loaded yet. See assets/databases/README.md."
            )
            refreshHistory()
        }
    }

    fun onInputChanged(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun translate() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTranslating = true)
            val result = runCatching { translator.translate(text) }
                .getOrElse {
                    _uiState.value = _uiState.value.copy(
                        isTranslating = false,
                        errorMessage = "Translation failed: ${it.message}"
                    )
                    return@launch
                }
            _uiState.value = _uiState.value.copy(result = result, isTranslating = false)

            db.historyDao().insert(
                HistoryEntry(
                    englishText = result.sourceText,
                    bengaliText = result.translatedText,
                    timestampMillis = System.currentTimeMillis()
                )
            )
            refreshHistory()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            db.historyDao().clear()
            refreshHistory()
        }
    }

    private suspend fun refreshHistory() {
        val items = db.historyDao().recent()
        _uiState.value = _uiState.value.copy(history = items)
    }
}
