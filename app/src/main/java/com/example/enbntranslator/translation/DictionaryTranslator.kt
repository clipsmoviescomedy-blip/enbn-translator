package com.example.enbntranslator.translation

import com.example.enbntranslator.data.DictionaryDao

/**
 * Offline translator backed by a bundled English->Bengali dictionary.
 *
 * Strategy: greedy longest-match. We look at windows of up to
 * [maxPhraseWords] consecutive words at a time and prefer translating the
 * longest matching phrase (so a known idiom like "how are you" gets its own
 * entry instead of being translated word-by-word). Anything with no match
 * at all falls back to the original English word so the user still gets
 * a readable, if imperfect, result — and we track those in
 * [TranslationResult.unmatchedWords] so the UI can surface them.
 */
class DictionaryTranslator(
    private val dao: DictionaryDao,
    private val maxPhraseWords: Int = 4
) : Translator {

    override suspend fun isReady(): Boolean = dao.count() > 0

    override suspend fun translate(text: String): TranslationResult {
        val tokens = tokenize(text)
        if (tokens.isEmpty()) {
            return TranslationResult(sourceText = text, translatedText = "")
        }

        val outputParts = mutableListOf<String>()
        val unmatched = mutableListOf<String>()

        var i = 0
        while (i < tokens.size) {
            val (translatedPiece, consumed, matched) = matchLongestPhrase(tokens, i)
            outputParts += translatedPiece
            if (!matched) unmatched += tokens[i]
            i += consumed
        }

        return TranslationResult(
            sourceText = text,
            translatedText = outputParts.joinToString(" "),
            unmatchedWords = unmatched,
            engine = TranslationResult.Engine.DICTIONARY
        )
    }

    /**
     * Tries phrase lengths from [maxPhraseWords] down to 1 starting at
     * index [start]. Returns Triple(outputText, wordsConsumed, wasMatchFound).
     */
    private suspend fun matchLongestPhrase(
        tokens: List<String>,
        start: Int
    ): Triple<String, Int, Boolean> {
        val maxLen = minOf(maxPhraseWords, tokens.size - start)
        for (len in maxLen downTo 1) {
            val phrase = tokens.subList(start, start + len).joinToString(" ")
            val entry = dao.lookupPhrase(phrase)
            if (entry != null) {
                val firstSense = entry.bengali.split(",").first().trim()
                return Triple(firstSense, len, true)
            }
        }
        // No match at any length: keep the original word untranslated.
        return Triple(tokens[start], 1, false)
    }

    private fun tokenize(text: String): List<String> {
        return text
            .lowercase()
            .split(Regex("\\s+"))
            .map { it.trim(',', '.', '!', '?', ';', ':', '"', '\'') }
            .filter { it.isNotBlank() }
    }
}
