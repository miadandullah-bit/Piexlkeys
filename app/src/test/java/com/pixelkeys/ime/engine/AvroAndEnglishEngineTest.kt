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
        // "helo" is a close typo for a real English word — engine must autocorrect to something
        val close = EnglishEngine.suggestWithAutocorrect("helo", autocorrectEnabled = true)
        assertTrue("Expected non-null autoCorrection for 'helo'", close.autoCorrection != null)

        // "thnik" → close to "think" (distance 2, length 5, same first char 't')
        val thnik = EnglishEngine.suggestWithAutocorrect("thnik", autocorrectEnabled = true)
        assertTrue("Expected non-null autoCorrection for 'thnik'", thnik.autoCorrection != null)

        // Banglish / unknown words must NOT be autocorrected; typed word preserved in suggestions
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
