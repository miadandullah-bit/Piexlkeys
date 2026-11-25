package com.pixelkeys.ime.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AvroAndEnglishEngineTest {

    // ─── Module 1: Avro Phonetic Engine ───────────────────────────────────────

    @Test
    fun avroTransliteratesBasicWords() {
        assertEquals("আমি", AvroPhoneticEngine.transliterate("ami"))
        assertEquals("তুমি", AvroPhoneticEngine.transliterate("tumi"))
        assertEquals("গান", AvroPhoneticEngine.transliterate("gan"))
    }

    @Test
    fun avroHandlesIndependentVowelsAtWordStart() {
        // 'e' at start → এ (independent)
        val ek = AvroPhoneticEngine.transliterate("ek")
        assertTrue("'ek' should start with এ", ek.startsWith("এ"))

        // 'o' at start → অ (independent)
        val onek = AvroPhoneticEngine.transliterate("onek")
        assertTrue("'onek' should start with অ", onek.startsWith("অ"))
    }

    @Test
    fun avroHandlesKarAfterConsonant() {
        // 'ka' → ক + া (kar, NOT independent আ)
        val ka = AvroPhoneticEngine.transliterate("ka")
        assertEquals("কা", ka)

        // 'ki' → কি
        assertEquals("কি", AvroPhoneticEngine.transliterate("ki"))

        // 'ku' → কু
        assertEquals("কু", AvroPhoneticEngine.transliterate("ku"))
    }

    @Test
    fun avroHandlesJuktakkhorConjuncts() {
        // 'kkh' → ক্ষ
        val kkh = AvroPhoneticEngine.transliterate("kkh")
        assertTrue("'kkh' should produce ক্ষ cluster", kkh.contains("ক্ষ"))

        // 'ksh' → ক্ষ alternate
        val ksh = AvroPhoneticEngine.transliterate("ksh")
        assertTrue("'ksh' should produce ক্ষ cluster", ksh.contains("ক্ষ"))

        // 'x' → ক্ষ
        val x = AvroPhoneticEngine.transliterate("x")
        assertEquals("ক্ষ", x)

        // Consecutive consonants → automatic hoshonto
        // 'kta' → ক্তা
        val kta = AvroPhoneticEngine.transliterate("kta")
        assertTrue("'kta' should contain ্", kta.contains("্"))
    }

    @Test
    fun avroHandlesSpecialOperators() {
        // ^ → Chandrabindu ঁ
        assertTrue(AvroPhoneticEngine.transliterate("a^").contains("ঁ"))

        // : → Visarga ঃ
        assertTrue(AvroPhoneticEngine.transliterate("duH").contains("ঃ"))

        // . → Dari (।)
        assertTrue(AvroPhoneticEngine.transliterate("ami.").contains("।"))

        // $ → Taka ৳
        assertTrue(AvroPhoneticEngine.transliterate("100$").contains("৳"))

        // t` → Khanda-Ta ৎ
        assertTrue(AvroPhoneticEngine.transliterate("t`").contains("ৎ"))
    }

    @Test
    fun avroHandlesBengaliDigits() {
        val result = AvroPhoneticEngine.transliterate("123")
        assertEquals("১২৩", result)
    }

    @Test
    fun avroHandlesReph() {
        // 'rr' before consonant → র্ (reph)
        val reph = AvroPhoneticEngine.transliterate("rrk")
        assertTrue("'rrk' should start with র্", reph.startsWith("র্"))
    }

    @Test
    fun avroGetCandidatesMergesPhrasesAndTransliteration() {
        val candidates = AvroPhoneticEngine.getCandidates("ami")
        assertTrue("Should contain আমি", candidates.contains("আমি"))
        assertTrue("Should return at least 1 candidate", candidates.isNotEmpty())
    }

    @Test
    fun avroGetCandidatesFuzzyVariants() {
        val candidates = AvroPhoneticEngine.getCandidates("bhalo")
        assertTrue(
            "Should contain ভালো for 'bhalo'",
            candidates.any { it.contains("ভালো") }
        )
    }

    // ─── Module 2: Bangla Fuzzy Dictionary ────────────────────────────────────

    @Test
    fun fuzzyDictReturnsResultsForDroppedVowelKey() {
        val dict = BanglaFuzzyDictionary.getInstance()

        // "amdr" (dropped vowels) → আমাদের
        val amdr = dict.lookup("amdr")
        assertTrue("'amdr' should map to আমাদের", amdr.any { it.contains("আমাদের") })
    }

    @Test
    fun fuzzyDictReturnsResultsForAltSpelling() {
        val dict = BanglaFuzzyDictionary.getInstance()

        // "valo" and "bhalo" both → ভালো
        val valo = dict.lookup("valo")
        assertTrue("'valo' should map to ভালো", valo.any { it.contains("ভালো") })

        val bhalo = dict.lookup("bhalo")
        assertTrue("'bhalo' should map to ভালো", bhalo.any { it.contains("ভালো") })
    }

    @Test
    fun fuzzyDictPrefixExpandsCorrectly() {
        val dict = BanglaFuzzyDictionary.getInstance()

        // Partial key "bondh" should still expand and return বন্ধু via prefix
        val results = dict.lookup("bondh")
        assertTrue("Prefix 'bondh' should suggest বন্ধু", results.any { it.contains("বন্ধু") })
    }

    @Test
    fun fuzzyDictEditDistanceFallback() {
        val dict = BanglaFuzzyDictionary.getInstance()

        // "bndhu" with one dropped char → still find বন্ধু
        val results = dict.lookup("bndhu")
        assertTrue("'bndhu' (fuzzy) should suggest বন্ধু", results.any { it.contains("বন্ধু") })
    }

    // ─── English Engine (unchanged) ───────────────────────────────────────────

    @Test
    fun englishAutocorrectsCloseTypoButPreservesBanglish() {
        val close = EnglishEngine.suggestWithAutocorrect("helo", autocorrectEnabled = true)
        assertTrue("Expected non-null autoCorrection for 'helo'", close.autoCorrection != null)

        val thnik = EnglishEngine.suggestWithAutocorrect("thnik", autocorrectEnabled = true)
        assertTrue("Expected non-null autoCorrection for 'thnik'", thnik.autoCorrection != null)

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
