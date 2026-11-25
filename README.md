# Pixelkeys

A fast, customizable Android keyboard (IME) built with **Kotlin** and **Jetpack Compose**, featuring English + offline Avro Bengali typing, Banglish-safe autocorrect, instant key hover previews, live clipboard sync, and multi-provider AI themes and translation.

📦 **[Download Latest APK (Releases)](https://github.com/miadandullah-bit/Piexlkeys/releases)**

---

## Features

### Fast, Tactile Typing
- **Zero-Lag Key Hover Preview:** Custom in-Compose floating key bubbles with GPU-accelerated press lift.
- **Banglish-Safe Autocorrect:** Corrects close English typos on Space while keeping Banglish and custom words intact.
- **Long-Press Controls:** Hold **Backspace** for continuous deletion, hold keys with secondary hints for numbers and symbols, and hold **Spacebar** for **trackpad cursor control**.
- **Session-Clean Input:** Automatically resets composing state between input fields.

### Languages & Voice
- **English** with offline word predictions and user dictionary support.
- **Avro Phonetic (Bengali):** Offline phonetic transliteration (`ami` -> `আমি`, `bangla` -> `বাংলা`).
- **Bengali Native** layout.
- **Voice Typing:** Built-in `SpeechRecognizer` support (`en-US` / `bn-BD`).

### Pixelkeys Studio (Trial, Themes, Settings)
- **Trial Sandbox:** Test typing, Avro transliteration, clipboard capture, and themes directly inside the app with live word/character counts and quick sample actions.
- **Built-In Themes:** *Pixel Block* (color-blocked Bauhaus palette with 5x7 dot-matrix glyphs), *Nothing Dark*, *Nothing Light*, *Hardware Minimal*, and *Dynamic You*.
- **Multi-Provider AI Studio:** Generate custom themes and run live EN <-> BN translation using **Google Gemini** (`gemini-2.5-flash` / `2.0-flash`), **Groq**, **OpenAI**, **OpenRouter**, or any **Custom OpenAI-compatible** endpoint, with a built-in connection tester.
- **Categorized Settings:** Dedicated filterable options for Layout, Typing, Haptics, AI Service, and Clipboard.

### Utility Drawers
- **200+ Categorized Emojis:** Organized into 5 tabs (*Faces*, *Hands*, *Hearts*, *Food*, *Tech*), plus **Kaomoji** and **Symbols**.
- **System Clipboard Sync:** Automatic `ClipboardManager` listener + focus-time sync into Room-backed clip history with pin/unpin, manual sync, and one-tap clear unpinned.

---

## Installation & Setup

1. Download the latest `Pixelkeys-v*.apk` from the [**Releases**](https://github.com/miadandullah-bit/Piexlkeys/releases) page.
2. Install the APK on your Android device (Android 7.0+ / API 24+).
   > *Note: CI builds are debug-signed; if upgrading from an earlier build with a different debug key, uninstall the previous version first.*
3. Open **Pixelkeys**, enable it in **System Settings -> Languages & Input -> On-screen keyboard**, and select **Pixelkeys** as your active keyboard.

---

## Tech Stack

- **Language & UI:** Kotlin, Jetpack Compose, Material 3
- **Architecture & Storage:** Android `InputMethodService`, Room Database, DataStore Preferences, Kotlin Coroutines & Serialization
- **AI Integration:** Google Gemini, Groq, OpenAI, OpenRouter, and Custom OpenAI-compatible REST APIs
- **CI/CD:** GitHub Actions automated APK builds & versioned GitHub Releases
