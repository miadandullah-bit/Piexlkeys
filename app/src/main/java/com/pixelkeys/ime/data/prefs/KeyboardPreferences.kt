package com.pixelkeys.ime.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pixel_keys_prefs")

enum class InputLanguage(val shortLabel: String, val displayName: String) {
    EN_US("EN", "English (US)"),
    AVRO_BN("AVRO", "Avro Phonetic"),
    BENGALI_NATIVE("বাংলা", "Bengali Native")
}

enum class OneHandedMode {
    NONE,
    LEFT,
    RIGHT
}

enum class HapticStyle {
    CRISP,
    THOCKY,
    SOFT
}

data class KeyboardPreferences(
    val activeThemeId: String = "nothing_dark",
    val activeLanguage: InputLanguage = InputLanguage.EN_US,
    val heightScale: Float = 1.0f,
    val keyFontSizeSp: Float = 19f,
    val oneHandedMode: OneHandedMode = OneHandedMode.NONE,
    val showNumberRow: Boolean = false,
    val hapticStrengthMs: Int = 18,
    val hapticStyle: HapticStyle = HapticStyle.CRISP,
    val soundEnabled: Boolean = false,
    val autocorrectEnabled: Boolean = true,
    val swipeTypingEnabled: Boolean = true,
    val geminiApiKey: String = ""
)

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val THEME_ID = stringPreferencesKey("active_theme_id")
        val LANGUAGE = stringPreferencesKey("active_language")
        val HEIGHT_SCALE = floatPreferencesKey("height_scale")
        val KEY_FONT_SIZE = floatPreferencesKey("key_font_size_sp")
        val ONE_HANDED = stringPreferencesKey("one_handed_mode")
        val SHOW_NUMBER_ROW = booleanPreferencesKey("show_number_row")
        val HAPTIC_MS = intPreferencesKey("haptic_strength_ms")
        val HAPTIC_STYLE = stringPreferencesKey("haptic_style")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val AUTOCORRECT = booleanPreferencesKey("autocorrect_enabled")
        val SWIPE_TYPING = booleanPreferencesKey("swipe_typing_enabled")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
    }

    val preferencesFlow: Flow<KeyboardPreferences> = context.dataStore.data.map { prefs ->
        KeyboardPreferences(
            activeThemeId = prefs[Keys.THEME_ID] ?: "nothing_dark",
            activeLanguage = runCatching {
                InputLanguage.valueOf(prefs[Keys.LANGUAGE] ?: InputLanguage.EN_US.name)
            }.getOrDefault(InputLanguage.EN_US),
            heightScale = (prefs[Keys.HEIGHT_SCALE] ?: 1.0f).coerceIn(0.85f, 1.45f),
            keyFontSizeSp = (prefs[Keys.KEY_FONT_SIZE] ?: 19f).coerceIn(15f, 24f),
            oneHandedMode = runCatching {
                OneHandedMode.valueOf(prefs[Keys.ONE_HANDED] ?: OneHandedMode.NONE.name)
            }.getOrDefault(OneHandedMode.NONE),
            showNumberRow = prefs[Keys.SHOW_NUMBER_ROW] ?: false,
            hapticStrengthMs = (prefs[Keys.HAPTIC_MS] ?: 18).coerceIn(0, 50),
            hapticStyle = runCatching {
                HapticStyle.valueOf(prefs[Keys.HAPTIC_STYLE] ?: HapticStyle.CRISP.name)
            }.getOrDefault(HapticStyle.CRISP),
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: false,
            autocorrectEnabled = prefs[Keys.AUTOCORRECT] ?: true,
            swipeTypingEnabled = prefs[Keys.SWIPE_TYPING] ?: true,
            geminiApiKey = prefs[Keys.GEMINI_API_KEY] ?: ""
        )
    }

    suspend fun setThemeId(id: String) {
        context.dataStore.edit { it[Keys.THEME_ID] = id }
    }

    suspend fun setLanguage(language: InputLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun cycleLanguage(current: InputLanguage): InputLanguage {
        val values = InputLanguage.entries
        val next = values[(values.indexOf(current) + 1) % values.size]
        setLanguage(next)
        return next
    }

    suspend fun setHeightScale(scale: Float) {
        context.dataStore.edit { it[Keys.HEIGHT_SCALE] = scale.coerceIn(0.85f, 1.45f) }
    }

    suspend fun setKeyFontSizeSp(sizeSp: Float) {
        context.dataStore.edit { it[Keys.KEY_FONT_SIZE] = sizeSp.coerceIn(15f, 24f) }
    }

    suspend fun setOneHandedMode(mode: OneHandedMode) {
        context.dataStore.edit { it[Keys.ONE_HANDED] = mode.name }
    }

    suspend fun setShowNumberRow(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_NUMBER_ROW] = show }
    }

    suspend fun setHapticStrength(ms: Int) {
        context.dataStore.edit { it[Keys.HAPTIC_MS] = ms.coerceIn(0, 50) }
    }

    suspend fun setHapticStyle(style: HapticStyle) {
        context.dataStore.edit { it[Keys.HAPTIC_STYLE] = style.name }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setAutocorrectEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTOCORRECT] = enabled }
    }

    suspend fun setSwipeTypingEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SWIPE_TYPING] = enabled }
    }

    suspend fun setGeminiApiKey(apiKey: String) {
        context.dataStore.edit { it[Keys.GEMINI_API_KEY] = apiKey.trim() }
    }
}
