package com.pixelkeys.ime.ai

import com.pixelkeys.ime.data.local.CustomThemeEntity
import com.pixelkeys.ime.data.prefs.AiProvider
import com.pixelkeys.ime.data.prefs.KeyboardPreferences
import com.pixelkeys.ime.theme.KeyTypographyStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

// ─── Serializable models ──────────────────────────────────────────────────────

@Serializable
private data class AiThemePayload(
    val name: String = "AI Theme",
    val boardBg: String = "#121214",
    val keyBg: String = "#1E1E22",
    val modifierBg: String = "#2A2A30",
    val actionBg: String = "#D71921",
    val keyText: String = "#FFFFFF",
    val secondaryText: String = "#8E8E93",
    val accent: String = "#D71921",
    val border: String = "#33333A",
    val cornerRadiusDp: Float = 10f,
    val isHardwareBevel: Boolean = false,
    val typographyStyle: String = "DOT_MATRIX",
    val isDark: Boolean = true
)

// ─── Custom exceptions ────────────────────────────────────────────────────────

class RateLimitException(message: String) : Exception(message)
class GeminiNotFoundException(message: String) : Exception(message)

// ─── AI Repository ────────────────────────────────────────────────────────────

/**
 * MODULE 3 + 4 + 5 — AI Repository
 *
 * Handles:
 *  - Theme generation (structured JSON via Gemini / OpenAI-compatible providers)
 *  - Translation (auto-detect Banglish / Bangla / English → target language)
 *  - Text refinement with tone chips (Fix Grammar, Formal, Casual, Polite, Clean Bangla)
 *  - Multi-provider dispatch with automatic Gemini → Groq rate-limit fallback (HTTP 429)
 *  - Resilient HTTP client: timeouts, error extraction, JSON fence stripping
 */
object AiRepository {

    private val json = Json { ignoreUnknownKeys = true }

    // ── Timeouts ──────────────────────────────────────────────────────────────
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS    = 30_000

    // ─────────────────────────────────────────────────────────────────────────
    // Connection test
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun testConnection(prefs: KeyboardPreferences): Result<String> = withContext(Dispatchers.IO) {
        val provider = prefs.aiProvider
        val apiKey = prefs.apiKeyFor(provider).trim()
        val model = prefs.modelFor(provider)
        if (apiKey.isBlank() && provider != AiProvider.CUSTOM) {
            return@withContext Result.failure(
                IllegalStateException("Enter your ${provider.displayName} API key first.")
            )
        }
        runCatching {
            val reply = callProvider(
                provider = provider,
                baseUrl = prefs.baseUrlFor(provider),
                model = model,
                apiKey = apiKey,
                prompt = "Reply with only the word OK.",
                jsonMode = false
            ).trim()
            "Connected to ${provider.displayName} ($model): ${reply.take(32)}"
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MODULE 3: AI Keyboard Theme Generator
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Generates a [CustomThemeEntity] from a natural-language [prompt].
     *
     * Uses structured JSON output on Gemini (responseMimeType: "application/json").
     * Falls back to best-effort JSON extraction on other providers.
     * On HTTP 429 from Gemini, automatically retries with Groq if key is configured.
     */
    suspend fun generateTheme(
        prompt: String,
        prefs: KeyboardPreferences
    ): Result<CustomThemeEntity> = withContext(Dispatchers.IO) {
        val provider = prefs.aiProvider
        val apiKey = prefs.apiKeyFor(provider).trim()
        if (apiKey.isBlank() && provider != AiProvider.CUSTOM) {
            return@withContext Result.failure(
                IllegalStateException("Add your ${provider.displayName} API key in Settings → AI Service.")
            )
        }

        runCatching {
            val systemPrompt = buildThemeSystemPrompt(prompt)
            val rawJson = callProviderWithFallback(
                provider = provider,
                prefs = prefs,
                prompt = systemPrompt,
                jsonMode = true
            )
            parseThemeFromJson(rawJson, provider, prompt)
        }
    }

    private fun buildThemeSystemPrompt(userPrompt: String): String = """
        You are an expert Android keyboard UI theme designer.
        Given a user's theme prompt, return ONLY a valid JSON object with these exact fields and high-contrast, legible colors:
        {
          "name": "Short 2-3 word theme name",
          "boardBg": "#RRGGBB",
          "keyBg": "#RRGGBB",
          "modifierBg": "#RRGGBB",
          "actionBg": "#RRGGBB",
          "keyText": "#RRGGBB",
          "secondaryText": "#RRGGBB",
          "accent": "#RRGGBB",
          "border": "#RRGGBB",
          "cornerRadiusDp": 10.0,
          "isHardwareBevel": false,
          "typographyStyle": "DOT_MATRIX",
          "isDark": true
        }
        Rules:
        - Ensure keyText has WCAG AA contrast (≥4.5:1) against keyBg and modifierBg.
        - typographyStyle must be exactly one of: DOT_MATRIX, HARDWARE_MONO, CLEAN_SANS.
        - cornerRadiusDp must be a number between 4 and 18.
        - isDark should reflect the overall brightness of the theme.
        - boardBg should be visually distinct from keyBg.
        - Return ONLY the JSON object, no markdown, no explanation.
        User prompt: $userPrompt
    """.trimIndent()

    private fun parseThemeFromJson(
        rawJson: String,
        provider: AiProvider,
        prompt: String
    ): CustomThemeEntity {
        val cleaned = extractJsonObject(rawJson)
        val parsed = json.decodeFromString<AiThemePayload>(cleaned)
        val safeStyle = runCatching { KeyTypographyStyle.valueOf(parsed.typographyStyle) }
            .getOrDefault(KeyTypographyStyle.DOT_MATRIX)

        return CustomThemeEntity(
            id = "ai_${UUID.randomUUID().toString().take(8)}",
            name = parsed.name.take(28).ifBlank { "${provider.displayName} Theme" },
            prompt = prompt,
            boardBgArgb = parseHexColor(parsed.boardBg, 0xFF121214),
            keyBgArgb = parseHexColor(parsed.keyBg, 0xFF1E1E22),
            modifierBgArgb = parseHexColor(parsed.modifierBg, 0xFF2A2A30),
            actionBgArgb = parseHexColor(parsed.actionBg, 0xFFD71921),
            keyTextArgb = parseHexColor(parsed.keyText, 0xFFFFFFFF),
            secondaryTextArgb = parseHexColor(parsed.secondaryText, 0xFF8E8E93),
            accentArgb = parseHexColor(parsed.accent, 0xFFD71921),
            borderArgb = parseHexColor(parsed.border, 0xFF33333A),
            cornerRadiusDp = parsed.cornerRadiusDp.coerceIn(4f, 18f),
            isHardwareBevel = parsed.isHardwareBevel,
            typographyStyle = safeStyle.name,
            isDark = parsed.isDark
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MODULE 4: AI Translator & Text Tone Refiner
    // ─────────────────────────────────────────────────────────────────────────

    enum class ToneMode(val label: String) {
        TRANSLATE("Translate"),
        FIX_GRAMMAR("Fix Grammar"),
        FORMAL("Formal / Professional"),
        CASUAL("Casual / Friendly"),
        POLITE("Polite"),
        CLEAN_BANGLA("Clean Bangla")
    }

    /**
     * Translates [text] into [targetLanguage] (e.g. "Bengali", "English", "Arabic", "Hindi").
     * Auto-detects source language (Banglish / Bangla / English).
     * Falls back to Groq on rate limit.
     */
    suspend fun translate(
        text: String,
        prefs: KeyboardPreferences,
        targetLanguage: String = "Bengali"
    ): Result<String> = withContext(Dispatchers.IO) {
        val trimmedText = text.trim()
        if (trimmedText.isBlank()) return@withContext Result.failure(
            IllegalArgumentException("Input text is empty.")
        )

        // Fast-path: check local quick phrase table first
        val lowerInput = trimmedText.lowercase()
        val quick = quickPhraseMap[lowerInput]
        if (quick != null) return@withContext Result.success(quick)

        val prompt = """
            You are a professional translator specializing in Bengali, English, Banglish (Romanized Bengali), Arabic, Hindi, and other languages.
            
            Task: Translate the following text into $targetLanguage.
            
            Rules:
            - Auto-detect the source language (may be Banglish/Romanized Bengali, Bengali Unicode, English, or mixed).
            - If the source is Banglish (Romanized Bengali like "ami tomake bhalobasi"), translate it naturally as Bengali content.
            - Produce a natural, fluent translation — not a word-for-word literal one.
            - Return ONLY the translated text. No explanation, no markdown, no labels.
            
            Text to translate:
            $trimmedText
        """.trimIndent()

        runCatching {
            callProviderWithFallback(
                provider = prefs.aiProvider,
                prefs = prefs,
                prompt = prompt,
                jsonMode = false
            ).trim()
        }
    }

    /**
     * Refines [text] according to [mode]:
     * - FIX_GRAMMAR → corrects grammar/spelling, keeps language & meaning
     * - FORMAL → rewrites in formal/professional register
     * - CASUAL → rewrites in friendly, conversational register
     * - POLITE → adds polite phrasing and softens directness
     * - CLEAN_BANGLA → converts messy Banglish/mixed input into proper Unicode Bangla
     */
    suspend fun refineText(
        text: String,
        mode: ToneMode,
        prefs: KeyboardPreferences
    ): Result<String> = withContext(Dispatchers.IO) {
        val trimmedText = text.trim()
        if (trimmedText.isBlank()) return@withContext Result.failure(
            IllegalArgumentException("Input text is empty.")
        )
        if (mode == ToneMode.TRANSLATE) return@withContext translate(trimmedText, prefs)

        val instruction = when (mode) {
            ToneMode.FIX_GRAMMAR -> """
                Fix all grammar, spelling, and punctuation errors in the text below.
                Preserve the original language (Bengali, English, or mixed) and meaning.
                Only correct errors — do not rephrase unnecessarily.
                Return ONLY the corrected text, no explanation.
            """.trimIndent()

            ToneMode.FORMAL -> """
                Rewrite the text below in a formal, professional tone.
                - Use polite and respectful language (use "আপনি" instead of "তুমি" for Bengali).
                - Remove slang, abbreviations, and casual expressions.
                - Keep the same language as the input (Bengali, English, or mixed).
                Return ONLY the rewritten text, no explanation.
            """.trimIndent()

            ToneMode.CASUAL -> """
                Rewrite the text below in a casual, friendly, conversational tone.
                - Use natural everyday language.
                - It's okay to use common contractions and informal expressions.
                - Keep the same language as the input (Bengali, English, or mixed).
                Return ONLY the rewritten text, no explanation.
            """.trimIndent()

            ToneMode.POLITE -> """
                Rewrite the text below to sound more polite and considerate.
                - Add appropriate polite phrases and soften any directness.
                - Use respectful forms of address where applicable.
                - Keep the same language as the input (Bengali, English, or mixed).
                Return ONLY the rewritten text, no explanation.
            """.trimIndent()

            ToneMode.CLEAN_BANGLA -> """
                Convert the following Banglish (Romanized Bengali) or mixed Bengali-English text into proper, 
                naturally written Unicode Bengali (বাংলা).
                
                Rules:
                - Convert ALL romanized Bengali words to proper Unicode Bengali script.
                - Fix any spelling mistakes in the Bengali words.
                - Use standard, modern Bengali spelling (not archaic forms).
                - Preserve any intentional English words (proper nouns, technical terms) in English.
                - Use correct punctuation: দাড়ি (।) for sentence endings in Bengali.
                - Return ONLY the clean Bengali text, no explanation.
                
                Input:
                $trimmedText
            """.trimIndent()

            ToneMode.TRANSLATE -> "" // handled above
        }

        val prompt = if (mode == ToneMode.CLEAN_BANGLA) {
            instruction // already includes the input
        } else {
            "$instruction\n\nText:\n$trimmedText"
        }

        runCatching {
            callProviderWithFallback(
                provider = prefs.aiProvider,
                prefs = prefs,
                prompt = prompt,
                jsonMode = false
            ).trim()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MODULE 5: Multi-Provider HTTP Client with Rate-Limit Resilience
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Dispatches the prompt to the configured provider.
     * On HTTP 429 from Gemini, automatically retries with Groq if a Groq key is set.
     * On HTTP 429 from any other provider, throws [RateLimitException] for the UI to handle.
     */
    private fun callProviderWithFallback(
        provider: AiProvider,
        prefs: KeyboardPreferences,
        prompt: String,
        jsonMode: Boolean
    ): String {
        return try {
            callProvider(
                provider = provider,
                baseUrl = prefs.baseUrlFor(provider),
                model = prefs.modelFor(provider),
                apiKey = prefs.apiKeyFor(provider).trim(),
                prompt = prompt,
                jsonMode = jsonMode
            )
        } catch (e: RateLimitException) {
            // Auto-fallback: Gemini 429 → Groq (if key configured)
            val groqKey = prefs.groqApiKey.trim()
            if (provider == AiProvider.GEMINI && groqKey.isNotBlank()) {
                callProvider(
                    provider = AiProvider.GROQ,
                    baseUrl = AiProvider.GROQ.defaultBaseUrl,
                    model = prefs.groqModel.ifBlank { AiProvider.GROQ.defaultModel },
                    apiKey = groqKey,
                    prompt = prompt,
                    jsonMode = false // Groq OpenAI-compat; JSON mode not forced via MIME type
                )
            } else {
                throw e
            }
        }
    }

    private fun callProvider(
        provider: AiProvider,
        baseUrl: String,
        model: String,
        apiKey: String,
        prompt: String,
        jsonMode: Boolean
    ): String {
        return when (provider) {
            AiProvider.GEMINI -> callGeminiApi(
                baseUrl = baseUrl,
                model = model,
                apiKey = apiKey,
                prompt = prompt,
                jsonMode = jsonMode
            )
            else -> callOpenAiCompatibleApi(
                provider = provider,
                baseUrl = baseUrl,
                apiKey = apiKey,
                model = model,
                prompt = prompt
            )
        }
    }

    // ── Gemini REST API (native format) ───────────────────────────────────────

    private fun callGeminiApi(
        baseUrl: String,
        model: String,
        apiKey: String,
        prompt: String,
        jsonMode: Boolean
    ): String {
        val cleanBase = baseUrl.trim().trimEnd('/')
        val cleanModel = model.trim().ifBlank { AiProvider.GEMINI.defaultModel }

        // Support both v1beta and v1 paths; always use generateContent
        val endpoint = "$cleanBase/models/$cleanModel:generateContent?key=${apiKey.trim()}"

        val generationConfig = buildJsonObject {
            put("temperature", 0.4)
            put("maxOutputTokens", 1024)
            if (jsonMode) {
                put("responseMimeType", "application/json")
            }
        }

        val body = buildJsonObject {
            put("contents", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("parts", buildJsonArray {
                        add(buildJsonObject { put("text", prompt) })
                    })
                })
            })
            put("generationConfig", generationConfig)
        }

        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("User-Agent", "PixelKeys-IME/1.5")
        }

        conn.outputStream.use { os ->
            os.write(json.encodeToString(JsonObject.serializer(), body).toByteArray(Charsets.UTF_8))
        }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

        if (code == 429) throw RateLimitException("Gemini rate limit exceeded (429). Retrying with fallback provider.")
        if (code !in 200..299) {
            val detail = extractApiErrorMessage(responseText)
            if (code == 404) throw GeminiNotFoundException("Gemini model '$cleanModel' unavailable (404): $detail")
            throw IllegalStateException("Gemini API error ($code): $detail")
        }

        val root = json.parseToJsonElement(responseText).jsonObject
        val candidates = root["candidates"]?.jsonArray
            ?: throw IllegalStateException("No candidates returned from Gemini")
        return candidates.firstOrNull()?.jsonObject?.get("content")?.jsonObject
            ?.get("parts")?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("text")?.jsonPrimitive?.content
            ?: throw IllegalStateException("Empty Gemini response")
    }

    // ── OpenAI-compatible API (Groq, OpenAI, OpenRouter, Custom) ─────────────

    private fun callOpenAiCompatibleApi(
        provider: AiProvider,
        baseUrl: String,
        apiKey: String,
        model: String,
        prompt: String
    ): String {
        val cleanBase = baseUrl.trim().trimEnd('/')
        val endpoint = if (cleanBase.endsWith("/chat/completions")) cleanBase
                       else "$cleanBase/chat/completions"

        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("User-Agent", "PixelKeys-IME/1.5")
            if (apiKey.isNotBlank()) setRequestProperty("Authorization", "Bearer ${apiKey.trim()}")
            if (provider == AiProvider.OPENROUTER) {
                setRequestProperty("HTTP-Referer", "https://github.com/miadandullah-bit/Piexlkeys")
                setRequestProperty("X-Title", "Pixelkeys")
            }
        }

        val bodyJson = buildJsonObject {
            put("model", model.trim().ifBlank { provider.defaultModel })
            put("temperature", 0.4)
            put("max_tokens", 1024)
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }

        conn.outputStream.use { os ->
            os.write(json.encodeToString(JsonObject.serializer(), bodyJson).toByteArray(Charsets.UTF_8))
        }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

        if (code == 429) throw RateLimitException("${provider.displayName} rate limit exceeded (429).")
        if (code !in 200..299) {
            val detail = extractApiErrorMessage(responseText)
            throw IllegalStateException("${provider.displayName} API error ($code): $detail")
        }

        val root = json.parseToJsonElement(responseText).jsonObject
        val choices = root["choices"]?.jsonArray
            ?: throw IllegalStateException("No choices returned from ${provider.displayName}")
        return choices.firstOrNull()?.jsonObject
            ?.get("message")?.jsonObject
            ?.get("content")?.jsonPrimitive?.content
            ?: throw IllegalStateException("Empty ${provider.displayName} response")
    }

    // ─── Utilities ────────────────────────────────────────────────────────────

    private fun extractApiErrorMessage(raw: String): String {
        val parsed = runCatching {
            json.parseToJsonElement(raw).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
        }.getOrNull()
        return (parsed ?: raw).trim().take(300).ifBlank { "Unknown API error" }
    }

    internal fun extractJsonObject(raw: String): String {
        val trimmed = raw.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        return if (start >= 0 && end > start) trimmed.substring(start, end + 1) else trimmed
    }

    internal fun parseHexColor(hex: String, fallback: Long): Long {
        val clean = hex.trim().removePrefix("#")
        return when (clean.length) {
            6 -> ("FF$clean").toLongOrNull(16) ?: fallback
            8 -> clean.toLongOrNull(16) ?: fallback
            else -> fallback
        }
    }

    // ─── Quick phrase table (offline, zero-latency) ───────────────────────────

    private val quickPhraseMap: Map<String, String> = mapOf(
        "hello"                 to "হ্যালো",
        "hi"                    to "হাই",
        "how are you"           to "কেমন আছেন?",
        "how are you?"          to "কেমন আছেন?",
        "i am fine"             to "আমি ভালো আছি।",
        "thank you"             to "ধন্যবাদ",
        "thanks"                to "ধন্যবাদ",
        "good morning"          to "সুপ্রভাত",
        "good night"            to "শুভ রাত্রি",
        "good evening"          to "শুভ সন্ধ্যা",
        "good afternoon"        to "শুভ অপরাহ্ন",
        "i love you"            to "আমি তোমাকে ভালোবাসি",
        "i love bangla"         to "আমি বাংলা ভালোবাসি",
        "আমি ভালো আছি"          to "I am doing well.",
        "ধন্যবাদ"               to "Thank you.",
        "কেমন আছো"             to "How are you?",
        "কেমন আছেন"            to "How are you?",
        "আমি তোমাকে ভালোবাসি"   to "I love you.",
        "শুভ সকাল"             to "Good morning.",
        "শুভ রাত্রি"            to "Good night."
    )
}
