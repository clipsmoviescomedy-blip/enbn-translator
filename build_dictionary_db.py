#!/usr/bin/env python3
"""
Builds app/src/main/assets/databases/en_bn_translator.db from
tools/seed_dictionary.csv (or any CSV with english,bengali,pos columns).

Run this whenever the dictionary word list changes:
    python3 tools/build_dictionary_db.py

The schema here must match Room's expectations for the `dictionary` table
defined by DictionaryEntry.kt: Room's createFromAsset() copies this file
verbatim as the app's initial database, so column names/types must match
exactly what @Entity generates (id INTEGER PRIMARY KEY, englishLower TEXT,
bengali TEXT, partOfSpeech TEXT).
"""
import csv
import sqlite3
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CSV_PATH = ROOT / "tools" / "seed_dictionary.csv"
DB_PATH = ROOT / "app" / "src" / "main" / "assets" / "databases" / "en_bn_translator.db"


def main() -> None:
    if not CSV_PATH.exists():
        sys.exit(f"CSV not found: {CSV_PATH}")

    DB_PATH.parent.mkdir(parents=True, exist_ok=True)
    if DB_PATH.exists():
        DB_PATH.unlink()

    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()

    # Matches the Room @Entity(tableName = "dictionary") schema exactly.
    cur.execute(
        """
        CREATE TABLE dictionary (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            englishLower TEXT NOT NULL,
            bengali TEXT NOT NULL,
            partOfSpeech TEXT
        )
        """
    )
    cur.execute("CREATE INDEX idx_dictionary_english ON dictionary(englishLower)")

    # Room also needs its own bookkeeping table so it recognizes this as a
    # valid Room-managed database on first open instead of wiping it.
    cur.execute(
        """
        CREATE TABLE room_master_table (
            id INTEGER PRIMARY KEY,
            identity_hash TEXT
        )
        """
    )

    # history table must exist too, since it's part of the same Room database.
    cur.execute(
        """
        CREATE TABLE history (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            englishText TEXT NOT NULL,
            bengaliText TEXT NOT NULL,
            timestampMillis INTEGER NOT NULL
        )
        """
    )

    count = 0
    with open(CSV_PATH, encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            english = row["english"].strip().lower()
            bengali = row["bengali"].strip()
            pos = row.get("pos", "").strip() or None
            if not english or not bengali:
                continue
            cur.execute(
                "INSERT INTO dictionary (englishLower, bengali, partOfSpeech) VALUES (?, ?, ?)",
                (english, bengali, pos),
            )
            count += 1

    conn.commit()
    conn.close()
    print(f"Wrote {count} entries to {DB_PATH}")
    print(
        "NOTE: Room validates a schema 'identity_hash' on first run in debug "
        "builds. If Room throws an IllegalStateException about the schema "
        "not matching on first launch, run the app once, let Room log the "
        "expected hash from app/schemas (enable exportSchema + gradle schema "
        "location), or simply call .fallbackToDestructiveMigration() "
        "(already set in AppDatabase.kt) so Room rebuilds cleanly instead "
        "of crashing."
    )


if __name__ == "__main__":
    main()
