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
import java.net.URLEncoder
import java.util.UUID

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

object AiRepository {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun generateTheme(
        prompt: String,
        prefs: KeyboardPreferences
    ): Result<CustomThemeEntity> = withContext(Dispatchers.IO) {
        val provider = prefs.aiProvider
        val apiKey = prefs.apiKeyFor(provider).trim()
        if (apiKey.isBlank() && provider != AiProvider.CUSTOM) {
            return@withContext Result.failure(
                IllegalStateException("Add your ${provider.displayName} API key in Settings -> AI Service.")
            )
        }

        runCatching {
            val systemPrompt = """
                You are an expert Android keyboard UI theme designer.
                Given a user's theme prompt, return ONLY a valid JSON object with high-contrast, legible colors:
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
                - Ensure keyText has strong contrast against keyBg and modifierBg.
                - typographyStyle must be one of: DOT_MATRIX, HARDWARE_MONO, CLEAN_SANS.
                - cornerRadiusDp must be between 4 and 18.
                User prompt: $prompt
            """.trimIndent()

            val rawJson = callProvider(
                provider = provider,
                baseUrl = prefs.baseUrlFor(provider),
                model = prefs.modelFor(provider),
                apiKey = apiKey,
                prompt = systemPrompt,
                jsonMode = true
            )
            val cleaned = extractJsonObject(rawJson)
            val parsed = json.decodeFromString<AiThemePayload>(cleaned)
            val safeStyle = runCatching { KeyTypographyStyle.valueOf(parsed.typographyStyle) }
                .getOrDefault(KeyTypographyStyle.DOT_MATRIX)

            CustomThemeEntity(
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
    }

    suspend fun translate(
        input: String,
        prefs: KeyboardPreferences
    ): Result<String> = withContext(Dispatchers.IO) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return@withContext Result.success("")

        val provider = prefs.aiProvider
        val apiKey = prefs.apiKeyFor(provider).trim()
        if (apiKey.isBlank() && provider != AiProvider.CUSTOM) {
            val quick = quickPhraseMap[trimmed.lowercase()]
            return@withContext if (quick != null) {
                Result.success(quick)
            } else {
                Result.failure(
                    IllegalStateException("Configure an AI key in Settings -> AI Service for live EN<->BN translation.")
                )
            }
        }

        runCatching {
            val prompt = """
                Translate the following text naturally between English and Bengali.
                If the text is English (or Banglish), translate it to natural Bengali script.
                If the text is Bengali script, translate it to natural English.
                Return ONLY the translated text without quotes or explanations.
                Text: $trimmed
            """.trimIndent()
            callProvider(
                provider = provider,
                baseUrl = prefs.baseUrlFor(provider),
                model = prefs.modelFor(provider),
                apiKey = apiKey,
                prompt = prompt,
                jsonMode = false
            ).trim()
        }
    }

    suspend fun testConnection(prefs: KeyboardPreferences): Result<String> = withContext(Dispatchers.IO) {
        val provider = prefs.aiProvider
        val apiKey = prefs.apiKeyFor(provider).trim()
        val model = prefs.modelFor(provider)
        if (apiKey.isBlank() && provider != AiProvider.CUSTOM) {
            return@withContext Result.failure(IllegalStateException("Enter your ${provider.displayName} API key first."))
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

    private fun callProvider(
        provider: AiProvider,
        baseUrl: String,
        model: String,
        apiKey: String,
        prompt: String,
        jsonMode: Boolean
    ): String {
        return when (provider) {
            AiProvider.GEMINI -> callGeminiWithFallback(apiKey = apiKey, model = model, prompt = prompt, jsonMode = jsonMode)
            AiProvider.GROQ,
            AiProvider.OPENAI,
            AiProvider.OPENROUTER,
            AiProvider.CUSTOM -> callOpenAiCompatibleApi(
                provider = provider,
                baseUrl = baseUrl,
                apiKey = apiKey,
                model = model,
                prompt = prompt
            )
        }
    }

    private fun callGeminiWithFallback(
        apiKey: String,
        model: String,
        prompt: String,
        jsonMode: Boolean
    ): String {
        val primaryModel = model.trim().ifBlank { AiProvider.GEMINI.defaultModel }
        return try {
            callGeminiApi(apiKey = apiKey, model = primaryModel, prompt = prompt, jsonMode = jsonMode)
        } catch (_: GeminiNotFoundException) {
            val fallback = if (primaryModel != "gemini-2.5-flash") "gemini-2.5-flash" else "gemini-2.0-flash"
            callGeminiApi(apiKey = apiKey, model = fallback, prompt = prompt, jsonMode = jsonMode)
        }
    }

    private class GeminiNotFoundException(message: String) : IllegalStateException(message)

    private fun callGeminiApi(apiKey: String, model: String, prompt: String, jsonMode: Boolean): String {
        val encodedKey = URLEncoder.encode(apiKey.trim(), "UTF-8")
        val cleanModel = model.trim().removePrefix("models/")
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$encodedKey")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }

        val bodyJson = buildJsonObject {
            put("contents", buildJsonArray {
                add(buildJsonObject {
                    put("parts", buildJsonArray {
                        add(buildJsonObject {
                            put("text", prompt)
                        })
                    })
                })
            })
            if (jsonMode) {
                put("generationConfig", buildJsonObject {
                    put("responseMimeType", "application/json")
                })
            }
        }

        conn.outputStream.use { os ->
            os.write(json.encodeToString(JsonObject.serializer(), bodyJson).toByteArray(Charsets.UTF_8))
        }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

        if (code !in 200..299) {
            val detail = extractApiErrorMessage(responseText)
            if (code == 404) {
                throw GeminiNotFoundException("Gemini model '$cleanModel' unavailable (404): $detail")
            }
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

    private fun callOpenAiCompatibleApi(
        provider: AiProvider,
        baseUrl: String,
        apiKey: String,
        model: String,
        prompt: String
    ): String {
        val cleanBase = baseUrl.trim().trimEnd('/')
        val endpoint = if (cleanBase.endsWith("/chat/completions")) {
            cleanBase
        } else {
            "$cleanBase/chat/completions"
        }
        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            if (apiKey.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer ${apiKey.trim()}")
            }
            if (provider == AiProvider.OPENROUTER) {
                setRequestProperty("HTTP-Referer", "https://github.com/miadandullah-bit/Piexlkeys")
                setRequestProperty("X-Title", "Pixelkeys")
            }
        }

        val bodyJson = buildJsonObject {
            put("model", model.trim().ifBlank { provider.defaultModel })
            put("temperature", 0.4)
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

    private fun extractApiErrorMessage(raw: String): String {
        val parsed = runCatching {
            json.parseToJsonElement(raw).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
        }.getOrNull()
        return (parsed ?: raw).trim().take(200).ifBlank { "Unknown API error" }
    }

    private fun extractJsonObject(raw: String): String {
        val trimmed = raw.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        return if (start >= 0 && end > start) trimmed.substring(start, end + 1) else trimmed
    }

    private fun parseHexColor(hex: String, fallback: Long): Long {
        val clean = hex.trim().removePrefix("#")
        return when (clean.length) {
            6 -> ("FF$clean").toLongOrNull(16) ?: fallback
            8 -> clean.toLongOrNull(16) ?: fallback
            else -> fallback
        }
    }

    private val quickPhraseMap = mapOf(
        "hello" to "হ্যালো",
        "how are you" to "কেমন আছেন?",
        "thank you" to "ধন্যবাদ",
        "good morning" to "সুপ্রভাত",
        "good night" to "শুভ রাত্রি",
        "i love bangla" to "আমি বাংলা ভালোবাসি",
        "আমি ভালো আছি" to "I am doing well",
        "ধন্যবাদ" to "Thank you",
        "কেমন আছো" to "How are you?"
    )
}
