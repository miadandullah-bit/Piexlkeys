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

enum class AiProvider(
    val displayName: String,
    val defaultModel: String,
    val presetModels: List<String>,
    val defaultBaseUrl: String,
    val keyHint: String,
    val helpSource: String
) {
    GEMINI(
        displayName = "Google Gemini",
        defaultModel = "gemini-2.5-flash",
        presetModels = listOf("gemini-2.5-flash", "gemini-2.5-flash-lite", "gemini-2.0-flash"),
        defaultBaseUrl = "https://generativelanguage.googleapis.com/v1beta",
        keyHint = "AIza...",
        helpSource = "aistudio.google.com/apikey"
    ),
    GROQ(
        displayName = "Groq",
        defaultModel = "llama-3.3-70b-versatile",
        presetModels = listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant", "meta-llama/llama-4-scout-17b-16e-instruct"),
        defaultBaseUrl = "https://api.groq.com/openai/v1",
        keyHint = "gsk_...",
        helpSource = "console.groq.com/keys"
    ),
    OPENAI(
        displayName = "OpenAI",
        defaultModel = "gpt-4o-mini",
        presetModels = listOf("gpt-4o-mini", "gpt-4.1-mini", "gpt-4o"),
        defaultBaseUrl = "https://api.openai.com/v1",
        keyHint = "sk-...",
        helpSource = "platform.openai.com/api-keys"
    ),
    OPENROUTER(
        displayName = "OpenRouter",
        defaultModel = "meta-llama/llama-3.3-70b-instruct:free",
        presetModels = listOf(
            "meta-llama/llama-3.3-70b-instruct:free",
            "google/gemini-2.0-flash-001",
            "openai/gpt-4o-mini"
        ),
        defaultBaseUrl = "https://openrouter.ai/api/v1",
        keyHint = "sk-or-...",
        helpSource = "openrouter.ai/keys"
    ),
    CUSTOM(
        displayName = "Custom API",
        defaultModel = "gpt-4o-mini",
        presetModels = emptyList(),
        defaultBaseUrl = "https://api.openai.com/v1",
        keyHint = "API key...",
        helpSource = "Any OpenAI-compatible /v1 endpoint"
    )
}

data class KeyboardPreferences(
    val activeThemeId: String = "pixel_block",
    val activeLanguage: InputLanguage = InputLanguage.EN_US,
    val heightScale: Float = 1.0f,
    val keyFontSizeSp: Float = 19f,
    val oneHandedMode: OneHandedMode = OneHandedMode.NONE,
    val showNumberRow: Boolean = false,
    val showEmojiKey: Boolean = true,
    val hapticStrengthMs: Int = 18,
    val hapticStyle: HapticStyle = HapticStyle.CRISP,
    val soundEnabled: Boolean = false,
    val autocorrectEnabled: Boolean = true,
    val swipeTypingEnabled: Boolean = true,
    val clipboardAutoCapture: Boolean = true,
    val aiProvider: AiProvider = AiProvider.GEMINI,
    val geminiApiKey: String = "",
    val geminiModel: String = AiProvider.GEMINI.defaultModel,
    val groqApiKey: String = "",
    val groqModel: String = AiProvider.GROQ.defaultModel,
    val openaiApiKey: String = "",
    val openaiModel: String = AiProvider.OPENAI.defaultModel,
    val openrouterApiKey: String = "",
    val openrouterModel: String = AiProvider.OPENROUTER.defaultModel,
    val customAiBaseUrl: String = AiProvider.CUSTOM.defaultBaseUrl,
    val customAiApiKey: String = "",
    val customAiModel: String = AiProvider.CUSTOM.defaultModel
) {
    fun apiKeyFor(provider: AiProvider = aiProvider): String = when (provider) {
        AiProvider.GEMINI -> geminiApiKey
        AiProvider.GROQ -> groqApiKey
        AiProvider.OPENAI -> openaiApiKey
        AiProvider.OPENROUTER -> openrouterApiKey
        AiProvider.CUSTOM -> customAiApiKey
    }

    fun modelFor(provider: AiProvider = aiProvider): String = when (provider) {
        AiProvider.GEMINI -> geminiModel.ifBlank { provider.defaultModel }
        AiProvider.GROQ -> groqModel.ifBlank { provider.defaultModel }
        AiProvider.OPENAI -> openaiModel.ifBlank { provider.defaultModel }
        AiProvider.OPENROUTER -> openrouterModel.ifBlank { provider.defaultModel }
        AiProvider.CUSTOM -> customAiModel.ifBlank { provider.defaultModel }
    }

    fun baseUrlFor(provider: AiProvider = aiProvider): String = when (provider) {
        AiProvider.CUSTOM -> customAiBaseUrl.ifBlank { provider.defaultBaseUrl }
        else -> provider.defaultBaseUrl
    }
}

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val THEME_ID = stringPreferencesKey("active_theme_id")
        val LANGUAGE = stringPreferencesKey("active_language")
        val HEIGHT_SCALE = floatPreferencesKey("height_scale")
        val KEY_FONT_SIZE = floatPreferencesKey("key_font_size_sp")
        val ONE_HANDED = stringPreferencesKey("one_handed_mode")
        val SHOW_NUMBER_ROW = booleanPreferencesKey("show_number_row")
        val SHOW_EMOJI_KEY = booleanPreferencesKey("show_emoji_key")
        val HAPTIC_MS = intPreferencesKey("haptic_strength_ms")
        val HAPTIC_STYLE = stringPreferencesKey("haptic_style")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val AUTOCORRECT = booleanPreferencesKey("autocorrect_enabled")
        val SWIPE_TYPING = booleanPreferencesKey("swipe_typing_enabled")
        val CLIPBOARD_AUTO_CAPTURE = booleanPreferencesKey("clipboard_auto_capture")
        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val GEMINI_MODEL = stringPreferencesKey("gemini_model")
        val GROQ_API_KEY = stringPreferencesKey("groq_api_key")
        val GROQ_MODEL = stringPreferencesKey("groq_model")
        val OPENAI_API_KEY = stringPreferencesKey("openai_api_key")
        val OPENAI_MODEL = stringPreferencesKey("openai_model")
        val OPENROUTER_API_KEY = stringPreferencesKey("openrouter_api_key")
        val OPENROUTER_MODEL = stringPreferencesKey("openrouter_model")
        val CUSTOM_AI_BASE_URL = stringPreferencesKey("custom_ai_base_url")
        val CUSTOM_AI_API_KEY = stringPreferencesKey("custom_ai_api_key")
        val CUSTOM_AI_MODEL = stringPreferencesKey("custom_ai_model")
    }

    val preferencesFlow: Flow<KeyboardPreferences> = context.dataStore.data.map { prefs ->
        KeyboardPreferences(
            activeThemeId = prefs[Keys.THEME_ID] ?: "pixel_block",
            activeLanguage = runCatching {
                InputLanguage.valueOf(prefs[Keys.LANGUAGE] ?: InputLanguage.EN_US.name)
            }.getOrDefault(InputLanguage.EN_US),
            heightScale = (prefs[Keys.HEIGHT_SCALE] ?: 1.0f).coerceIn(0.85f, 1.45f),
            keyFontSizeSp = (prefs[Keys.KEY_FONT_SIZE] ?: 19f).coerceIn(15f, 24f),
            oneHandedMode = runCatching {
                OneHandedMode.valueOf(prefs[Keys.ONE_HANDED] ?: OneHandedMode.NONE.name)
            }.getOrDefault(OneHandedMode.NONE),
            showNumberRow = prefs[Keys.SHOW_NUMBER_ROW] ?: false,
            showEmojiKey = prefs[Keys.SHOW_EMOJI_KEY] ?: true,
            hapticStrengthMs = (prefs[Keys.HAPTIC_MS] ?: 18).coerceIn(0, 50),
            hapticStyle = runCatching {
                HapticStyle.valueOf(prefs[Keys.HAPTIC_STYLE] ?: HapticStyle.CRISP.name)
            }.getOrDefault(HapticStyle.CRISP),
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: false,
            autocorrectEnabled = prefs[Keys.AUTOCORRECT] ?: true,
            swipeTypingEnabled = prefs[Keys.SWIPE_TYPING] ?: true,
            clipboardAutoCapture = prefs[Keys.CLIPBOARD_AUTO_CAPTURE] ?: true,
            aiProvider = runCatching {
                AiProvider.valueOf(prefs[Keys.AI_PROVIDER] ?: AiProvider.GEMINI.name)
            }.getOrDefault(AiProvider.GEMINI),
            geminiApiKey = prefs[Keys.GEMINI_API_KEY] ?: "",
            geminiModel = prefs[Keys.GEMINI_MODEL]?.ifBlank { AiProvider.GEMINI.defaultModel }
                ?: AiProvider.GEMINI.defaultModel,
            groqApiKey = prefs[Keys.GROQ_API_KEY] ?: "",
            groqModel = prefs[Keys.GROQ_MODEL]?.ifBlank { AiProvider.GROQ.defaultModel }
                ?: AiProvider.GROQ.defaultModel,
            openaiApiKey = prefs[Keys.OPENAI_API_KEY] ?: "",
            openaiModel = prefs[Keys.OPENAI_MODEL]?.ifBlank { AiProvider.OPENAI.defaultModel }
                ?: AiProvider.OPENAI.defaultModel,
            openrouterApiKey = prefs[Keys.OPENROUTER_API_KEY] ?: "",
            openrouterModel = prefs[Keys.OPENROUTER_MODEL]?.ifBlank { AiProvider.OPENROUTER.defaultModel }
                ?: AiProvider.OPENROUTER.defaultModel,
            customAiBaseUrl = prefs[Keys.CUSTOM_AI_BASE_URL]?.ifBlank { AiProvider.CUSTOM.defaultBaseUrl }
                ?: AiProvider.CUSTOM.defaultBaseUrl,
            customAiApiKey = prefs[Keys.CUSTOM_AI_API_KEY] ?: "",
            customAiModel = prefs[Keys.CUSTOM_AI_MODEL]?.ifBlank { AiProvider.CUSTOM.defaultModel }
                ?: AiProvider.CUSTOM.defaultModel
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

    suspend fun setShowEmojiKey(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_EMOJI_KEY] = show }
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

    suspend fun setClipboardAutoCapture(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CLIPBOARD_AUTO_CAPTURE] = enabled }
    }

    suspend fun setAiProvider(provider: AiProvider) {
        context.dataStore.edit { it[Keys.AI_PROVIDER] = provider.name }
    }

    suspend fun setProviderApiKey(provider: AiProvider, apiKey: String) {
        val trimmed = apiKey.trim()
        context.dataStore.edit { prefs ->
            when (provider) {
                AiProvider.GEMINI -> prefs[Keys.GEMINI_API_KEY] = trimmed
                AiProvider.GROQ -> prefs[Keys.GROQ_API_KEY] = trimmed
                AiProvider.OPENAI -> prefs[Keys.OPENAI_API_KEY] = trimmed
                AiProvider.OPENROUTER -> prefs[Keys.OPENROUTER_API_KEY] = trimmed
                AiProvider.CUSTOM -> prefs[Keys.CUSTOM_AI_API_KEY] = trimmed
            }
        }
    }

    suspend fun setProviderModel(provider: AiProvider, model: String) {
        val trimmed = model.trim().ifBlank { provider.defaultModel }
        context.dataStore.edit { prefs ->
            when (provider) {
                AiProvider.GEMINI -> prefs[Keys.GEMINI_MODEL] = trimmed
                AiProvider.GROQ -> prefs[Keys.GROQ_MODEL] = trimmed
                AiProvider.OPENAI -> prefs[Keys.OPENAI_MODEL] = trimmed
                AiProvider.OPENROUTER -> prefs[Keys.OPENROUTER_MODEL] = trimmed
                AiProvider.CUSTOM -> prefs[Keys.CUSTOM_AI_MODEL] = trimmed
            }
        }
    }

    suspend fun setCustomAiBaseUrl(baseUrl: String) {
        context.dataStore.edit {
            it[Keys.CUSTOM_AI_BASE_URL] = baseUrl.trim().ifBlank { AiProvider.CUSTOM.defaultBaseUrl }
        }
    }

    suspend fun setGeminiApiKey(apiKey: String) {
        setProviderApiKey(AiProvider.GEMINI, apiKey)
    }
}
