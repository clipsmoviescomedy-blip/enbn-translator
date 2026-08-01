# EN–BN Translator (Android, fully offline)

A native Android app (Kotlin + Jetpack Compose) that translates English to
Bengali with **zero network access** — the `INTERNET` permission isn't even
declared in the manifest.

## Current state: Stage 1 — dictionary-based translation

Translation currently works by greedy longest-phrase dictionary lookup
(see `translation/DictionaryTranslator.kt`): it checks up to 4-word windows
against a bundled SQLite dictionary and falls back word-by-word. This is
fast, has no model to load, and handles common phrases well, but it won't
produce fluent grammar for arbitrary sentences — Bengali word order and
verb conjugation differ from English enough that word-for-word substitution
reads awkwardly beyond short/common phrases.

## Architecture

- **`translation/Translator.kt`** — interface all translation engines
  implement. This is the seam: swapping the dictionary approach for a real
  neural model later means writing a new class, not touching the UI.
- **`translation/DictionaryTranslator.kt`** — today's implementation.
- **`data/`** — Room database: `dictionary` table (prepackaged, read-only)
  and `history` table (user's past translations, read-write).
- **`TranslatorViewModel.kt`** — holds UI state, calls the translator.
- **`ui/TranslatorScreen.kt`** — Compose UI: input box, translate button,
  result card, history list.

## Building

Open the project root in Android Studio (Iguana or newer) and let Gradle
sync — it targets minSdk 24 / compileSdk 34, Kotlin 1.9.24, Compose BOM
2024.06.00.

The dictionary asset is already generated and committed at
`app/src/main/assets/databases/en_bn_translator.db`. To regenerate it after
editing the word list:

```bash
python3 tools/build_dictionary_db.py
```

No other setup is required to run Stage 1 — no API keys, no network, no
model download.

## Stage 2 (not yet built): on-device neural translation

To get fluent sentence-level translation instead of phrase lookup, the plan
is:

1. **Pick a small model.** A distilled MarianMT (Helsinki-NLP `opus-mt-en-bn`
   if available, or a distilled NLLB-200 checkpoint restricted to en/bn)
   gives reasonable quality at a mobile-friendly size once quantized
   (~50–150MB in 8-bit).
2. **Export to ONNX.** Use `optimum` (Hugging Face) or `torch.onnx.export`
   to convert the encoder/decoder to ONNX graphs, then quantize with
   `onnxruntime.quantization` to shrink size and speed up CPU inference.
3. **Bundle the tokenizer.** SentencePiece/BPE vocab files ship as assets
   alongside the `.onnx` files; tokenization runs on-device via a small
   Kotlin/JNI SentencePiece binding (or ONNX Runtime's built-in tokenizer
   support, depending on the model).
4. **Add `onnxruntime-android`** as a dependency (commented placeholder
   already left in `app/build.gradle.kts`) and implement `OnnxNmtTranslator :
   Translator` that loads the model in `isReady()` and runs
   encode→decode(beam search or greedy)→detokenize in `translate()`.
5. **Wire it into `TranslatorViewModel`**, using `DictionaryTranslator` as
   an instant fallback while the (larger, slower-to-load) model
   initializes, and for single-word lookups where it's more precise anyway.

This stage is the harder, more open-ended part — model conversion for a
lower-resource language pair like en-bn often needs some trial and error to
get quality and size to a good tradeoff for mobile. Happy to pick this up
next whenever you're ready.

## Known gaps / things to do before shipping

- Real launcher icon (`drawable/ic_launcher_foreground.xml` is a placeholder
  glyph).
- Dictionary has ~85 entries as a starter set — needs a real EN–BN word
  list. Public options worth checking (verify licensing before bundling):
  a Bengali Wiktionary extract, or FreeDict's en-bn dataset if available.
- Bengali → English (reverse direction) isn't built yet; the schema
  supports it (just add a second index or reversed table) but the UI is
  one-directional for now.
- No unit tests yet for `DictionaryTranslator`'s phrase-matching logic.
