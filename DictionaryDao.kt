package com.example.enbntranslator.data

import androidx.room.Dao
import androidx.room.Query

@Dao
interface DictionaryDao {

    /** Exact match on a single lowercased English word or phrase. */
    @Query("SELECT * FROM dictionary WHERE englishLower = :word LIMIT 1")
    suspend fun lookupExact(word: String): DictionaryEntry?

    /** Longest-phrase-first lookup helper: check if a given phrase exists at all. */
    @Query("SELECT * FROM dictionary WHERE englishLower = :phrase LIMIT 1")
    suspend fun lookupPhrase(phrase: String): DictionaryEntry?

    @Query("SELECT COUNT(*) FROM dictionary")
    suspend fun count(): Int
}
