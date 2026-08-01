package com.example.enbntranslator.translation

/**
 * Abstraction over "something that turns English text into Bengali text,
 * entirely on-device."
 *
 * Today [DictionaryTranslator] is the only implementation: it does
 * word/phrase lookup against a bundled offline dictionary. It's fast,
 * has zero model-loading cost, and needs no ML runtime — but it translates
 * word-by-word, so grammar and word order stay English-shaped.
 *
 * Later, an OnnxNmtTranslator implementing this same interface can wrap a
 * distilled seq2seq model (e.g. MarianMT/NLLB exported to ONNX) run through
 * ONNX Runtime Mobile, for fluent sentence-level translation. Because both
 * implementations satisfy this same interface, swapping one for the other
 * — or falling back from one to the other — requires no UI or ViewModel
 * changes.
 */
interface Translator {

    /** True once the translator has loaded whatever it needs (dictionary, model, etc). */
    suspend fun isReady(): Boolean

    /**
     * Translate [text] from English to Bengali.
     * Returns a [TranslationResult] describing what happened, since a
     * dictionary translator may only manage partial/word-level coverage.
     */
    suspend fun translate(text: String): TranslationResult
}

data class TranslationResult(
    val sourceText: String,
    val translatedText: String,
    /** Words from the input that had no dictionary entry (empty for a full NMT model). */
    val unmatchedWords: List<String> = emptyList(),
    val engine: Engine = Engine.DICTIONARY
) {
    enum class Engine { DICTIONARY, NEURAL_MODEL }
}
