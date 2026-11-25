package com.pixelkeys.ime.theme

import com.pixelkeys.ime.data.prefs.AiProvider
import com.pixelkeys.ime.data.prefs.KeyboardPreferences
import com.pixelkeys.ime.keyboard.KeySpec
import com.pixelkeys.ime.keyboard.KeyType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeAndAiConfigTest {

    @Test
    fun dotMatrixFontSupportsAsciiAndSkipsBengali() {
        assertTrue(DotMatrixFont.supports("123ABC"))
        assertTrue(DotMatrixFont.supports("pixelkeys"))
        assertTrue(DotMatrixFont.supports("@#$%&-+[]"))
        assertFalse(DotMatrixFont.supports("বাংলা"))
        assertFalse(DotMatrixFont.supports(""))
    }

    @Test
    fun pixelBlockThemeProducesDistinctColorBlockedSwatches() {
        val theme = BuiltInThemes.PixelBlock
        val k0 = theme.resolveKeySwatch(0, 0, KeySpec("1", "1"), isShiftActive = false)
        val k1 = theme.resolveKeySwatch(0, 1, KeySpec("2", "2"), isShiftActive = false)
        val k2 = theme.resolveKeySwatch(0, 2, KeySpec("3", "3"), isShiftActive = false)
        val space = theme.resolveKeySwatch(3, 4, KeySpec("space", "pixelkeys", type = KeyType.SPACE), isShiftActive = false)

        assertNotEquals(k0.background, k1.background)
        assertNotEquals(k1.background, k2.background)
        assertEquals(theme.spaceKeySurface, space.background)
    }

    @Test
    fun keyboardPreferencesResolvesMultipleAiProviders() {
        val prefs = KeyboardPreferences(
            aiProvider = AiProvider.GROQ,
            geminiApiKey = "gem-key",
            groqApiKey = "groq-key",
            groqModel = "llama-3.3-70b-versatile"
        )

        assertEquals("groq-key", prefs.apiKeyFor())
        assertEquals("llama-3.3-70b-versatile", prefs.modelFor())
        assertEquals("gem-key", prefs.apiKeyFor(AiProvider.GEMINI))
        assertEquals("gemini-2.5-flash", prefs.modelFor(AiProvider.GEMINI))
    }
}
