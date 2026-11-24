package com.pixelkeys.ime.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AvroAndEnglishEngineTest {

    @Test
    fun avroTransliteratesBasicWords() {
        assertEquals("আমি", AvroPhoneticEngine.transliterate("ami"))
        assertEquals("বাংলা", AvroPhoneticEngine.transliterate("bangla"))
        assertEquals("গান", AvroPhoneticEngine.transliterate("gan"))
    }

    @Test
    fun avroCandidatesIncludePhraseMatches() {
        val candidates = AvroPhoneticEngine.getCandidates("ami")
        assertTrue(candidates.contains("আমি"))
    }

    @Test
    fun englishAutocorrectsCloseTypoButPreservesBanglish() {
        val close = EnglishEngine.suggestWithAutocorrect("helo", autocorrectEnabled = true)
        assertEquals("hello", close.autoCorrection)

        val banglish = EnglishEngine.suggestWithAutocorrect("valobashi", autocorrectEnabled = true)
        assertNull(banglish.autoCorrection)
        assertTrue(banglish.suggestions.contains("valobashi"))
    }

    @Test
    fun swipeDecoderFindsExpectedWord() {
        val points = listOf(
            SwipePoint(0.55f, 0.35f),
            SwipePoint(0.65f, 0.2f),
            SwipePoint(0.75f, 0.05f)
        )
        val decoded = EnglishEngine.decodeSwipe(points)
        assertTrue(decoded.contains("hi"))
    }
}
