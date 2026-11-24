package com.pixelkeys.ime.engine

import org.junit.Assert.assertEquals
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
    fun englishSuggestFindsPrefixAndFuzzyMatches() {
        val prefix = EnglishEngine.suggest("pix")
        assertTrue(prefix.contains("pixel"))

        val fuzzy = EnglishEngine.suggest("helo", autocorrectEnabled = true)
        assertTrue(fuzzy.contains("hello"))
    }

    @Test
    fun swipeDecoderFindsExpectedWord() {
        // Approximate path from 'h' -> 'i'
        val points = listOf(
            SwipePoint(0.55f, 0.35f),
            SwipePoint(0.65f, 0.2f),
            SwipePoint(0.75f, 0.05f)
        )
        val decoded = EnglishEngine.decodeSwipe(points)
        assertTrue(decoded.contains("hi"))
    }
}
