# Pixelkeys

A fast, customizable Android keyboard (IME) built with **Kotlin** and **Jetpack Compose** — featuring English + offline Avro Bengali typing, Banglish-safe autocorrect, instant key hover previews, and **Gemini 3.8 Flash** AI themes & translation.

📦 **[Download Latest APK (Releases)](https://github.com/miadandullah-bit/Piexlkeys/releases)**

---

## ✨ Features

### ⚡ Fast, Tactile Typing
- **Zero-Lag Key Hover Preview:** Custom in-Compose floating key bubbles with GPU-accelerated press lift — no sluggish `PopupWindow` delay.
- **Banglish-Safe Autocorrect:** Corrects genuine close English typos on Space while leaving Banglish and custom words untouched and surfacing suggestion chips.
- **Long-Press Controls:**
  - Hold **Backspace** for rapid continuous deletion.
  - Hold keys with secondary hints to quickly type **numbers and symbols**.
  - Hold **Spacebar** for **trackpad cursor control** (slide left/right to move the cursor).
- **Session-Clean Input:** Automatically resets composing state between input fields.

### 🌐 Languages & Voice
- **English** with offline word predictions and user dictionary support.
- **Avro Phonetic (Bengali):** Offline phonetic transliteration (e.g., `ami` → `আমি`, `bangla` → `বাংলা`).
- **Bengali Native** layout.
- **Voice Typing:** Built-in `SpeechRecognizer` support (`en-US` / `bn-BD`).

### 🎨 Companion Studio & Gemini 3.8 Flash AI
- **Built-In Themes:** *Nothing Dark*, *Nothing Light*, *Hardware Minimal*, and *Dynamic You* (Material You on Android 12+).
- **AI Theme Generator:** Enter any prompt to generate custom color palettes, corner radii, and typography styles using **Gemini 3.8 Flash** (bring your own free key from [Google AI Studio](https://aistudio.google.com/apikey)).
- **Live EN ↔ BN Translator:** Translate directly inside the keyboard drawer with Gemini 3.8 Flash (plus offline quick-phrase fallbacks).
- **Layout & Feedback Controls:** Adjust keyboard height scale, key font size, number row toggle, dedicated `☺` emoji key toggle, one-handed mode, and haptic/audio click styles.

### 😀 Rich Utility Drawers
- **200+ Categorized Emojis:** Organized into 5 tabs (*Faces*, *Hands*, *Hearts*, *Food*, *Tech*), plus **Kaomoji** and **Symbols**.
- **Clipboard Manager:** Room-backed clip history with pin/unpin and one-tap clear unpinned items.
- **Live Playground:** Test themes, haptics, and layouts directly inside the Pixelkeys Companion app.

---

## 📲 Installation & Setup

1. Download the latest `Pixelkeys-v*.apk` from the [**Releases**](https://github.com/miadandullah-bit/Piexlkeys/releases) page.
2. Install the APK on your Android device (Android 7.0+ / API 24+).
   > *Note: CI builds are debug-signed; if upgrading from an earlier build with a different debug key, uninstall the previous version first.*
3. Open **Pixelkeys**, enable it in **System Settings → Languages & Input → On-screen keyboard**, and select **Pixelkeys** as your active keyboard.

---

## 🛠️ Tech Stack

- **Language & UI:** Kotlin, Jetpack Compose, Material 3
- **Architecture & Storage:** Android `InputMethodService`, Room Database, DataStore Preferences, Kotlin Coroutines & Serialization
- **AI Integration:** Google Gemini REST API (`gemini-3.8-flash`)
- **CI/CD:** GitHub Actions automated APK builds & versioned GitHub Releases
