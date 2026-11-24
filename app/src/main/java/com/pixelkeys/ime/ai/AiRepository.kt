package com.pixelkeys.ime.ai

import com.pixelkeys.ime.data.local.CustomThemeEntity
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

@Serializable
private data class GeminiThemePayload(
    val name: String = "Gemini Theme",
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

    suspend fun generateThemeWithGemini(
        prompt: String,
        apiKey: String
    ): Result<CustomThemeEntity> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Add your Gemini API key first (free at aistudio.google.com/apikey).")
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

            val rawJson = callGeminiApi(
                apiKey = apiKey,
                prompt = systemPrompt,
                jsonMode = true
            )
            val cleaned = rawJson.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val parsed = json.decodeFromString<GeminiThemePayload>(cleaned)
            val safeStyle = runCatching { KeyTypographyStyle.valueOf(parsed.typographyStyle) }
                .getOrDefault(KeyTypographyStyle.DOT_MATRIX)

            CustomThemeEntity(
                id = "ai_${UUID.randomUUID().toString().take(8)}",
                name = parsed.name.take(28).ifBlank { "Gemini Theme" },
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

    suspend fun translateWithGemini(
        input: String,
        apiKey: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return@withContext Result.success("")
        if (apiKey.isBlank()) {
            val quick = quickPhraseMap[trimmed.lowercase()]
            return@withContext if (quick != null) {
                Result.success(quick)
            } else {
                Result.failure(IllegalStateException("Add your Gemini API key in the Themes tab for live EN↔BN translation."))
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
            callGeminiApi(apiKey = apiKey, prompt = prompt, jsonMode = false).trim()
        }
    }

    private fun callGeminiApi(apiKey: String, prompt: String, jsonMode: Boolean): String {
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
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
            throw IllegalStateException("Gemini API error ($code): ${responseText.take(180)}")
        }

        val root = json.parseToJsonElement(responseText).jsonObject
        val candidates = root["candidates"]?.jsonArray
            ?: throw IllegalStateException("No candidates returned from Gemini")
        val text = candidates[0].jsonObject["content"]?.jsonObject
            ?.get("parts")?.jsonArray?.get(0)?.jsonObject
            ?.get("text")?.jsonPrimitive?.content
            ?: throw IllegalStateException("Empty Gemini response")
        return text
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
