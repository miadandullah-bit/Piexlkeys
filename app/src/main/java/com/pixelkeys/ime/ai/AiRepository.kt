package com.pixelkeys.ime.ai

import com.pixelkeys.ime.data.local.CustomThemeEntity
import com.pixelkeys.ime.theme.KeyTypographyStyle
import java.util.UUID

/**
 * AI Repository for Theme Generation and EN<->BN Translation.
 * Uses deterministic smart synthesis offline and can be wired to Firebase AI Logic
 * once google-services.json is provisioned.
 */
object AiRepository {

    fun generateThemeFromPrompt(prompt: String): CustomThemeEntity {
        val p = prompt.lowercase()
        val id = "ai_${UUID.randomUUID().toString().take(8)}"

        return when {
            "matcha" in p || "green" in p || "paper" in p -> CustomThemeEntity(
                id = id,
                name = prompt.trim().take(24).ifBlank { "Matcha Paper" },
                prompt = prompt,
                boardBgArgb = 0xFFEDEFE6,
                keyBgArgb = 0xFFFAFBF7,
                modifierBgArgb = 0xFFD9DFCE,
                actionBgArgb = 0xFF4A6B46,
                keyTextArgb = 0xFF232B21,
                secondaryTextArgb = 0xFF667362,
                accentArgb = 0xFF4A6B46,
                borderArgb = 0xFFC8D0BC,
                cornerRadiusDp = 10f,
                isHardwareBevel = true,
                typographyStyle = KeyTypographyStyle.HARDWARE_MONO.name,
                isDark = false
            )

            "cyber" in p || "neon" in p || "amber" in p || "tokyo" in p -> CustomThemeEntity(
                id = id,
                name = prompt.trim().take(24).ifBlank { "Tokyo Amber" },
                prompt = prompt,
                boardBgArgb = 0xFF0A0B0E,
                keyBgArgb = 0xFF151821,
                modifierBgArgb = 0xFF1E2230,
                actionBgArgb = 0xFFFF9F1C,
                keyTextArgb = 0xFFFFD166,
                secondaryTextArgb = 0xFF7D8597,
                accentArgb = 0xFFFF9F1C,
                borderArgb = 0xFF2B3245,
                cornerRadiusDp = 8f,
                isHardwareBevel = false,
                typographyStyle = KeyTypographyStyle.DOT_MATRIX.name,
                isDark = true
            )

            "ocean" in p || "blue" in p || "nord" in p -> CustomThemeEntity(
                id = id,
                name = prompt.trim().take(24).ifBlank { "Nordic Slate" },
                prompt = prompt,
                boardBgArgb = 0xFF1B222C,
                keyBgArgb = 0xFF27303F,
                modifierBgArgb = 0xFF323D4F,
                actionBgArgb = 0xFF5E81AC,
                keyTextArgb = 0xFFECEFF4,
                secondaryTextArgb = 0xFF9AA7B8,
                accentArgb = 0xFF88C0D0,
                borderArgb = 0xFF3B475C,
                cornerRadiusDp = 10f,
                isHardwareBevel = false,
                typographyStyle = KeyTypographyStyle.HARDWARE_MONO.name,
                isDark = true
            )

            else -> CustomThemeEntity(
                id = id,
                name = prompt.trim().take(24).ifBlank { "Custom Studio" },
                prompt = prompt,
                boardBgArgb = 0xFF121214,
                keyBgArgb = 0xFF1E1E22,
                modifierBgArgb = 0xFF2A2A30,
                actionBgArgb = 0xFFE63946,
                keyTextArgb = 0xFFF1FAEE,
                secondaryTextArgb = 0xFFA8DADC,
                accentArgb = 0xFFE63946,
                borderArgb = 0xFF35353D,
                cornerRadiusDp = 11f,
                isHardwareBevel = false,
                typographyStyle = KeyTypographyStyle.DOT_MATRIX.name,
                isDark = true
            )
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

    fun translateQuick(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""
        val hit = quickPhraseMap[trimmed.lowercase()]
        if (hit != null) return hit
        return "Add google-services.json to enable live Gemini EN↔BN translation for: \"$trimmed\""
    }
}
