package com.example.enbntranslator.data

import android.content.Context
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Entity(tableName = "history")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val englishText: String,
    val bengaliText: String,
    val timestampMillis: Long
)

@Dao
interface HistoryDao {
    @Insert
    suspend fun insert(entry: HistoryEntry)

    @Query("SELECT * FROM history ORDER BY timestampMillis DESC LIMIT 100")
    suspend fun recent(): List<HistoryEntry>

    @Query("DELETE FROM history")
    suspend fun clear()
}

@Database(
    entities = [DictionaryEntry::class, HistoryEntry::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dictionaryDao(): DictionaryDao
    abstract fun historyDao(): HistoryDao

    companion object {
        private const val DB_NAME = "en_bn_translator.db"

        @Volatile private var instance: AppDatabase? = null

        /**
         * The dictionary table is prepopulated and shipped as a prebuilt
         * SQLite file in app/src/main/assets/databases/en_bn_translator.db
         * (see assets/databases/README.md for how to generate it).
         * Room's createFromAsset copies that file in on first access instead
         * of building the table row-by-row at runtime, which is the right
         * approach for a dictionary with tens of thousands of entries.
         */
        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    .createFromAsset("databases/$DB_NAME")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
