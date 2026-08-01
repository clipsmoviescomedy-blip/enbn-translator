package com.example.enbntranslator.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One English -> Bengali dictionary entry.
 *
 * [englishLower] is stored lowercase and indexed for fast case-insensitive
 * lookup. [bengali] can contain multiple comma-separated senses
 * (e.g. "book" -> "বই, গ্রন্থ") — the translator picks the first sense
 * for running text and the UI can show the rest as alternatives.
 */
@Entity(tableName = "dictionary")
data class DictionaryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val englishLower: String,
    val bengali: String,
    /** e.g. "noun", "verb", "phrase" — optional, used for display only. */
    val partOfSpeech: String? = null
)
