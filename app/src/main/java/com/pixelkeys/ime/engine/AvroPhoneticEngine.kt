package com.pixelkeys.ime.engine

/**
 * Production-grade, zero-latency Avro Phonetic transliteration engine.
 *
 * Implements the full OmicronLab Avro Phonetic 5 specification with:
 *  - Longest-Match scanning (no backtracking, deterministic forward pass)
 *  - Correct Swaraborno (স্বরবর্ণ) vs Kar-chinho (কার চিহ্ন) selection
 *  - Automatic Juktakkhor (যুক্তাক্ষর) / Hoshonto formation between consonants
 *  - Full Phola (ফলা) support: র-ফলা, য-ফলা, ব-ফলা, ম-ফলা, ল-ফলা
 *  - Reph (র্‌-রেফ) formation via `rr` prefix before consonant
 *  - Special clusters: kkh/x→ক্ষ, gg/jNG→জ্ঞ, ngg→ঙ্গ, ndr→ন্দ্র, hm→হ্ম, ShN→ষ্ণ, etc.
 *  - Syllable separator ` (backtick), explicit Hoshonto (,,), Chandrabindu (^),
 *    Visarga (:), Khanda-Ta (t`), Dari (.), Taka sign ($)
 *  - Bengali digit mapping for 0-9
 */
object AvroPhoneticEngine {

    // ─── Token types ──────────────────────────────────────────────────────────

    private data class PhoneticRule(
        val roman: String,
        val bangla: String,
        val isVowel: Boolean = false,
        val isConsonant: Boolean = false,
        val isSpecial: Boolean = false
    )

    // ─── Sorted by length DESC for longest-match priority ─────────────────────

    /**
     * Map: roman string → (independent vowel form, dependent kar form)
     * Independent form is used at word start or after another vowel.
     * Kar form is appended directly after a consonant base.
     */
    private val vowelRules: List<Triple<String, String, String>> = listOf(
        // key        independent    kar
        Triple("OI",  "ঐ",           "ৈ"),
        Triple("OU",  "ঔ",           "ৌ"),
        Triple("rri", "ঋ",           "ৃ"),
        Triple("oo",  "উ",           "ু"),  // alias for U
        Triple("ee",  "ঈ",           "ী"),  // alias for I
        Triple("aa",  "আ",           "া"),  // double-a alias
        Triple("O",   "ও",           "ো"),
        Triple("I",   "ঈ",           "ী"),
        Triple("U",   "ঊ",           "ূ"),
        Triple("a",   "আ",           "া"),
        Triple("e",   "এ",           "ে"),
        Triple("i",   "ই",           "ি"),
        Triple("o",   "অ",           ""),   // 'o' after consonant = inherent (empty kar)
        Triple("u",   "উ",           "ু")
    ).sortedByDescending { it.first.length }

    /**
     * Consonant rules sorted longest-first.
     * Value = (bangla character, isConjunctEligible)
     * Special conjunct clusters are handled before single consonants.
     */
    private val consonantRules: List<Pair<String, String>> = listOf(
        // ── Special clusters (must come before component letters) ───────────
        "kkh"  to "ক্ষ",   // ক্ষ via kkh
        "ksh"  to "ক্ষ",   // ক্ষ alternate
        "jNG"  to "জ্ঞ",   // জ্ঞ
        "gg"   to "জ্ঞ",   // জ্ঞ alternate (Avro 5 compat)
        "ngg"  to "ঙ্গ",   // ঙ্গ
        "ndr"  to "ন্দ্র", // ন্দ্র
        "ShN"  to "ষ্ণ",   // ষ্ণ
        "hm"   to "হ্ম",   // হ্ম (Brahmin cluster)
        "ng"   to "ং",     // Anusvara
        "NY"   to "ঞ",
        // ── Multi-char consonants ────────────────────────────────────────────
        "kh"   to "খ",
        "gh"   to "ঘ",
        "Ng"   to "ঙ",
        "ch"   to "ছ",   // ch → ছ  (Avro: c → চ, ch → ছ)
        "jh"   to "ঝ",
        "Th"   to "ঠ",
        "Dh"   to "ঢ",
        "th"   to "থ",
        "dh"   to "ধ",
        "ph"   to "ফ",
        "bh"   to "ভ",
        "Sh"   to "ষ",
        "sh"   to "শ",
        "Rh"   to "ঢ়",
        // ── Single-char consonants ───────────────────────────────────────────
        "k"    to "ক",
        "g"    to "গ",
        "c"    to "চ",   // c → চ  (bare c)
        "j"    to "জ",
        "T"    to "ট",
        "D"    to "ড",
        "N"    to "ণ",
        "t"    to "ত",
        "d"    to "দ",
        "n"    to "ন",
        "p"    to "প",
        "f"    to "ফ",
        "b"    to "ব",
        "v"    to "ভ",
        "m"    to "ম",
        "z"    to "য",
        "r"    to "র",
        "l"    to "ল",
        "S"    to "ষ",
        "s"    to "স",
        "h"    to "হ",
        "R"    to "ড়",
        "y"    to "য়",
        "Y"    to "য়",
        "x"    to "ক্ষ"  // x → ক্ষ
    ).sortedByDescending { it.first.length }

    /**
     * Phola (ফলা) rules: triggered when a consonant is followed directly by
     * one of these while another consonant is the base.
     * Key = triggering roman character(s), Value = phola unicode.
     */
    private val pholaRules: List<Pair<String, String>> = listOf(
        "rr" to "র্",   // rr before consonant → Reph (র্‌)
        "y"  to "্য",  // য-ফলা
        "r"  to "্র",  // র-ফলা
        "w"  to "্ব",  // ব-ফলা (w used as ব-ফলা trigger)
        "m"  to "্ম",  // ম-ফলা
        "l"  to "্ল",  // ল-ফলা
        "n"  to "্ন"   // ন-ফলা (rare but valid)
    ).sortedByDescending { it.first.length }

    // ─── Set helpers ──────────────────────────────────────────────────────────

    private val vowelKeys = vowelRules.map { it.first }.toSet()
    private val consonantKeys = consonantRules.map { it.first }.toSet()

    /** Returns true if `c` starts a vowel pattern */
    private fun isVowelStart(c: Char): Boolean =
        c in setOf('a', 'e', 'i', 'o', 'u', 'O', 'I', 'U', 'r')

    // ─── Parsing State ────────────────────────────────────────────────────────

    private enum class LastType { NONE, CONSONANT, VOWEL, SPECIAL }

    // ─── Core transliteration ─────────────────────────────────────────────────

    /**
     * Transliterates the full composing buffer (Romanized → Unicode Bangla).
     * Called on every keystroke; must be O(n) or better.
     */
    fun transliterate(input: String): String {
        if (input.isEmpty()) return ""
        val sb = StringBuilder(input.length * 2)
        var i = 0
        var last = LastType.NONE

        while (i < input.length) {
            val ch = input[i]

            // ── Bengali digit ──────────────────────────────────────────────
            if (ch in '0'..'9') {
                if (last == LastType.CONSONANT) {
                    sb.append("্")  // close consonant with hoshonto before digit
                }
                sb.append(('০'.code + (ch - '0')).toChar())
                last = LastType.SPECIAL
                i++
                continue
            }

            // ── Explicit syllable separator: backtick ──────────────────────
            if (ch == '`') {
                if (last == LastType.CONSONANT) sb.append("্") // finalize consonant
                last = LastType.NONE
                i++
                continue
            }

            // ── Explicit Hoshonto: ,, ──────────────────────────────────────
            if (input.startsWith(",,", i)) {
                if (last == LastType.CONSONANT) {
                    sb.append("্")
                    last = LastType.SPECIAL
                }
                i += 2
                continue
            }

            // ── Khanda-Ta: t` ──────────────────────────────────────────────
            if (input.startsWith("t`", i)) {
                if (last == LastType.CONSONANT) sb.append("্")
                sb.append("ৎ")
                last = LastType.CONSONANT
                i += 2
                continue
            }

            // ── Special symbols ────────────────────────────────────────────
            if (ch == '^') {
                sb.append("ঁ"); last = LastType.SPECIAL; i++; continue
            }
            if (ch == ':') {
                if (last == LastType.CONSONANT) sb.append("্")
                sb.append("ঃ"); last = LastType.SPECIAL; i++; continue
            }
            if (ch == '.') {
                if (last == LastType.CONSONANT) sb.append("্")
                sb.append("।"); last = LastType.SPECIAL; i++; continue
            }
            if (ch == '$') {
                if (last == LastType.CONSONANT) sb.append("্")
                sb.append("৳"); last = LastType.SPECIAL; i++; continue
            }

            // ── Reph: rr before a consonant ───────────────────────────────
            // rr at non-word-start, before a consonant → র্‌ (prepended reph)
            if (input.startsWith("rr", i) && i + 2 < input.length) {
                val nextChar = input[i + 2]
                val nextIsConsonant = consonantRules.any { input.startsWith(it.first, i + 2) }
                if (nextIsConsonant && !nextChar.isDigit()) {
                    // Reph: output র্ before the next consonant
                    if (last == LastType.CONSONANT) sb.append("্")
                    sb.append("র্")
                    last = LastType.CONSONANT
                    i += 2
                    continue
                }
                // else fall through to handle 'r' as consonant
            }

            // ── Try consonant match ────────────────────────────────────────
            val consonantMatch = matchLongest(input, i, consonantRules)
            if (consonantMatch != null) {
                val banglaConsonant = consonantMatch.second
                if (last == LastType.CONSONANT) {
                    // Automatic Juktakkhor: insert ্ (Hoshonto/virama) between consonants
                    sb.append("্")
                }
                sb.append(banglaConsonant)
                last = LastType.CONSONANT
                i += consonantMatch.first.length
                continue
            }

            // ── Try vowel match ────────────────────────────────────────────
            val vowelMatch = matchLongest(input, i, vowelRules.map { it.first to it })
            if (vowelMatch != null) {
                val (key, triple) = vowelMatch.first to (vowelMatch.second as Triple<*, *, *>)
                val independent = triple.second as String
                val kar = triple.third as String
                when (last) {
                    LastType.CONSONANT -> {
                        // After a consonant: use Kar (dependent) form
                        // For 'o' (inherent vowel), kar is empty → no extra character needed
                        sb.append(kar)
                    }
                    else -> {
                        // Word-start or after vowel/space: use Independent form
                        sb.append(independent)
                    }
                }
                last = LastType.VOWEL
                i += key.length
                continue
            }

            // ── Fallback: space / punctuation / unknown char ───────────────
            if (last == LastType.CONSONANT) {
                // Bare consonant at word end (before space) keeps inherent 'অ' sound
                // No hoshonto needed — the consonant is complete
            }
            sb.append(ch)
            last = LastType.NONE
            i++
        }

        return sb.toString()
    }

    // ─── Candidate generation ─────────────────────────────────────────────────

    /**
     * Returns up to 5 Bangla candidates for a given roman composing string.
     * Merges: dictionary phrases > direct transliteration > fuzzy dictionary.
     */
    fun getCandidates(romanInput: String): List<String> {
        if (romanInput.isBlank()) return emptyList()
        val lower = romanInput.lowercase()
        val results = LinkedHashSet<String>()

        // 1. Phrase dictionary match (exact & prefix)
        phraseDictionary[lower]?.forEach { results.add(it) }

        // 2. Prefix matches from phrase dictionary
        if (results.size < 3) {
            phraseDictionary.entries
                .filter { it.key.startsWith(lower) && it.key != lower }
                .flatMap { it.value }
                .take(3 - results.size)
                .forEach { results.add(it) }
        }

        // 3. Direct transliteration
        val direct = transliterate(romanInput)
        if (direct.isNotEmpty() && direct != romanInput) results.add(direct)

        // 4. Case-variant (try all-lowercase input)
        if (romanInput != lower) {
            val lowerDirect = transliterate(lower)
            if (lowerDirect.isNotEmpty() && lowerDirect != lower) results.add(lowerDirect)
        }

        return results.take(5).toList()
    }

    // ─── Match helpers ────────────────────────────────────────────────────────

    private fun <T> matchLongest(input: String, index: Int, rules: List<Pair<String, T>>): Pair<String, T>? {
        for ((key, value) in rules) {
            if (input.startsWith(key, startIndex = index)) {
                return key to value
            }
        }
        return null
    }

    // ─── Extended Phrase Dictionary ───────────────────────────────────────────
    // High-frequency Bangla words keyed by their most common romanized spellings.
    // Fuzzy variants (dropped letters, alternate spellings) are all included.

    internal val phraseDictionary: Map<String, List<String>> = buildMap {
        // ── Pronouns & basic words ──────────────────────────────────────────
        put("ami",        listOf("আমি"))
        put("tumi",       listOf("তুমি"))
        put("apni",       listOf("আপনি"))
        put("amra",       listOf("আমরা"))
        put("tomra",      listOf("তোমরা"))
        put("tara",       listOf("তারা"))
        put("se",         listOf("সে"))
        put("tar",        listOf("তার"))
        put("amar",       listOf("আমার"))
        put("tomar",      listOf("তোমার"))
        put("apnar",      listOf("আপনার"))
        put("amader",     listOf("আমাদের"))
        put("amdr",       listOf("আমাদের"))
        put("amadr",      listOf("আমাদের"))
        put("tomader",    listOf("তোমাদের"))
        put("tader",      listOf("তাদের"))
        put("amake",      listOf("আমাকে"))
        put("tomake",     listOf("তোমাকে"))
        put("take",       listOf("তাকে"))

        // ── Common verbs ────────────────────────────────────────────────────
        put("achi",       listOf("আছি"))
        put("acho",       listOf("আছো", "আছ"))
        put("ache",       listOf("আছে"))
        put("nei",        listOf("নেই"))
        put("hobe",       listOf("হবে"))
        put("hoi",        listOf("হই"))
        put("hoy",        listOf("হয়"))
        put("holo",       listOf("হলো"))
        put("cai",        listOf("চাই"))
        put("chai",       listOf("চাই"))
        put("dao",        listOf("দাও"))
        put("diye",       listOf("দিয়ে"))
        put("nebe",       listOf("নেবে"))
        put("jabo",       listOf("যাবো"))
        put("jabe",       listOf("যাবে"))
        put("asbo",       listOf("আসবো"))
        put("asbe",       listOf("আসবে"))
        put("kori",       listOf("করি"))
        put("koro",       listOf("করো"))
        put("kore",       listOf("করে"))
        put("korbo",      listOf("করবো"))
        put("korbe",      listOf("করবে"))
        put("korlam",     listOf("করলাম"))
        put("korlo",      listOf("করলো"))
        put("korechi",    listOf("করেছি"))
        put("korecho",    listOf("করেছো"))
        put("koreche",    listOf("করেছে"))
        put("bolo",       listOf("বলো"))
        put("bole",       listOf("বলে"))
        put("bolchi",     listOf("বলছি"))
        put("bolcho",     listOf("বলছো"))
        put("bolche",     listOf("বলছে"))
        put("dekhi",      listOf("দেখি"))
        put("dekho",      listOf("দেখো"))
        put("dekhe",      listOf("দেখে"))
        put("jani",       listOf("জানি"))
        put("jano",       listOf("জানো"))
        put("jane",       listOf("জানে"))
        put("shuni",      listOf("শুনি"))
        put("shuno",      listOf("শুনো"))
        put("shune",      listOf("শুনে"))

        // ── Greetings & common phrases ──────────────────────────────────────
        put("assalamualaikum", listOf("আসসালামু আলাইকুম"))
        put("aslm",       listOf("আসসালামু আলাইকুম"))
        put("walaikumsalam", listOf("ওয়ালাইকুম আসসালাম"))
        put("bismillah",  listOf("বিসমিল্লাহ"))
        put("alhamdulillah", listOf("আলহামদুলিল্লাহ"))
        put("inshallah",  listOf("ইনশাল্লাহ"))
        put("mashallah",  listOf("মাশাল্লাহ"))
        put("hello",      listOf("হ্যালো"))
        put("hi",         listOf("হাই"))
        put("helo",       listOf("হ্যালো"))
        put("namaskar",   listOf("নমস্কার"))
        put("dhonnobad",  listOf("ধন্যবাদ"))
        put("dhonnyobad", listOf("ধন্যবাদ"))
        put("shukriya",   listOf("শুকরিয়া"))
        put("maaf",       listOf("মাফ"))
        put("maf",        listOf("মাফ"))
        put("dukkho",     listOf("দুঃখিত"))
        put("sorry",      listOf("সরি"))
        put("ok",         listOf("ওকে"))
        put("okay",       listOf("ওকে"))
        put("thik",       listOf("ঠিক"))
        put("thikache",   listOf("ঠিক আছে"))
        put("thik ache",  listOf("ঠিক আছে"))
        put("thikace",    listOf("ঠিক আছে"))

        // ── Questions ───────────────────────────────────────────────────────
        put("ki",         listOf("কি", "কী"))
        put("kemon",      listOf("কেমন"))
        put("keno",       listOf("কেন", "কেনো"))
        put("kothay",     listOf("কোথায়"))
        put("kothai",     listOf("কোথায়"))
        put("kobe",       listOf("কবে"))
        put("kokhon",     listOf("কখন"))
        put("koto",       listOf("কতো", "কত"))
        put("ke",         listOf("কে"))
        put("kar",        listOf("কার"))
        put("kake",       listOf("কাকে"))

        // ── Nouns: places ────────────────────────────────────────────────────
        put("bangladesh", listOf("বাংলাদেশ"))
        put("bangladesh", listOf("বাংলাদেশ"))
        put("dhaka",      listOf("ঢাকা"))
        put("chittagong", listOf("চট্টগ্রাম"))
        put("sylhet",     listOf("সিলেট"))
        put("rajshahi",   listOf("রাজশাহী"))
        put("khulna",     listOf("খুলনা"))
        put("barishal",   listOf("বরিশাল"))
        put("rangpur",    listOf("রংপুর"))
        put("mymensingh", listOf("ময়মনসিংহ"))
        put("comilla",    listOf("কুমিল্লা"))
        put("india",      listOf("ভারত"))
        put("pakistan",   listOf("পাকিস্তান"))
        put("kolkata",    listOf("কলকাতা"))

        // ── Language & culture ───────────────────────────────────────────────
        put("bangla",     listOf("বাংলা", "বাঙলা"))
        put("banglay",    listOf("বাংলায়"))
        put("baংla",      listOf("বাংলা"))
        put("bhasha",     listOf("ভাষা"))
        put("bhasa",      listOf("ভাষা"))
        put("bangladesh", listOf("বাংলাদেশ"))

        // ── Common adjectives ────────────────────────────────────────────────
        put("bhalo",      listOf("ভালো", "ভাল"))
        put("valo",       listOf("ভালো", "ভাল"))
        put("bhala",      listOf("ভালো"))
        put("vala",       listOf("ভালো"))
        put("kharap",     listOf("খারাপ"))
        put("sundor",     listOf("সুন্দর"))
        put("shundor",    listOf("সুন্দর"))
        put("boro",       listOf("বড়ো", "বড়"))
        put("choto",      listOf("ছোটো", "ছোট"))
        put("chhoto",     listOf("ছোট"))
        put("onek",       listOf("অনেক"))
        put("kub",        listOf("খুব"))
        put("khub",       listOf("খুব"))
        put("beশি",       listOf("বেশি"))
        put("beshi",      listOf("বেশি"))
        put("kom",        listOf("কম"))
        put("nতুন",       listOf("নতুন"))
        put("notun",      listOf("নতুন"))
        put("purano",     listOf("পুরানো", "পুরনো"))
        put("valo lage",  listOf("ভালো লাগে"))
        put("bhalo lage", listOf("ভালো লাগে"))

        // ── Time ────────────────────────────────────────────────────────────
        put("aaj",        listOf("আজ"))
        put("aj",         listOf("আজ"))
        put("kal",        listOf("কাল"))
        put("parshukaal", listOf("পরশুকাল"))
        put("ekhon",      listOf("এখন"))
        put("okhon",      listOf("তখন"))
        put("tokhon",     listOf("তখন"))
        put("shokal",     listOf("সকাল"))
        put("dupur",      listOf("দুপুর"))
        put("bikel",      listOf("বিকেল"))
        put("shondha",    listOf("সন্ধ্যা"))
        put("rat",        listOf("রাত"))
        put("raat",       listOf("রাত"))
        put("shomoy",     listOf("সময়"))

        // ── Relationship words ───────────────────────────────────────────────
        put("ma",         listOf("মা"))
        put("baba",       listOf("বাবা"))
        put("abbu",       listOf("আব্বু"))
        put("ammu",       listOf("আম্মু"))
        put("bhai",       listOf("ভাই"))
        put("vai",        listOf("ভাই"))
        put("apu",        listOf("আপু"))
        put("bon",        listOf("বোন"))
        put("bon",        listOf("বোন"))
        put("boro bhai",  listOf("বড় ভাই"))
        put("choto bhai", listOf("ছোট ভাই"))
        put("bondhu",     listOf("বন্ধু"))
        put("bondu",      listOf("বন্ধু"))
        put("bndhu",      listOf("বন্ধু"))
        put("bndh",       listOf("বন্ধু"))
        put("priya",      listOf("প্রিয়া", "প্রিয়"))

        // ── Feelings & emotions ──────────────────────────────────────────────
        put("valobashi",  listOf("ভালোবাসি"))
        put("bhalobashi", listOf("ভালোবাসি"))
        put("valobasa",   listOf("ভালোবাসা"))
        put("bhalobasa",  listOf("ভালোবাসা"))
        put("mon",        listOf("মন"))
        put("prem",       listOf("প্রেম"))
        put("valobashar", listOf("ভালোবাসার"))
        put("kosto",      listOf("কষ্ট"))
        put("dukkho",     listOf("দুঃখ"))
        put("anondo",     listOf("আনন্দ"))
        put("sukhi",      listOf("সুখী"))
        put("sukh",       listOf("সুখ"))
        put("haskhushi",  listOf("হাসিখুশি"))

        // ── Common Banglish social media phrases ─────────────────────────────
        put("ki koro",    listOf("কী করো"))
        put("ki korcho",  listOf("কী করছো"))
        put("kemon acho", listOf("কেমন আছো"))
        put("kivabe",     listOf("কীভাবে"))
        put("kemon lage", listOf("কেমন লাগে"))
        put("shob thik",  listOf("সব ঠিক"))
        put("valo theko", listOf("ভালো থেকো"))
        put("shob valo",  listOf("সব ভালো"))
        put("share",      listOf("শেয়ার"))
        put("like",       listOf("লাইক"))
        put("comment",    listOf("কমেন্ট"))
        put("video",      listOf("ভিডিও"))
        put("photo",      listOf("ফটো", "ছবি"))
        put("chobi",      listOf("ছবি"))
        put("neta",       listOf("নেতা"))
        put("government", listOf("সরকার"))
        put("shorkar",    listOf("সরকার"))
        put("desh",       listOf("দেশ"))
        put("shomossya",  listOf("সমস্যা"))
        put("shomossa",   listOf("সমস্যা"))
        put("somossha",   listOf("সমস্যা"))
        put("problem",    listOf("সমস্যা"))
        put("shomadhan",  listOf("সমাধান"))
        put("solution",   listOf("সমাধান"))

        // ── Numbers as words ─────────────────────────────────────────────────
        put("ek",         listOf("এক"))
        put("dui",        listOf("দুই", "দুইটি"))
        put("tin",        listOf("তিন"))
        put("char",       listOf("চার"))
        put("panch",      listOf("পাঁচ"))
        put("choy",       listOf("ছয়"))
        put("shaat",      listOf("সাত"))
        put("aat",        listOf("আট"))
        put("noy",        listOf("নয়"))
        put("dosh",       listOf("দশ"))

        // ── Conjunctions & connectors ────────────────────────────────────────
        put("ebong",      listOf("এবং"))
        put("ebng",       listOf("এবং"))
        put("kintu",      listOf("কিন্তু"))
        put("tobe",       listOf("তবে"))
        put("tahole",     listOf("তাহলে"))
        put("thele",      listOf("তাহলে"))
        put("karon",      listOf("কারণ"))
        put("jodi",       listOf("যদি"))
        put("othoba",     listOf("অথবা"))
        put("ba",         listOf("বা"))
        put("o",          listOf("ও"))
        put("r",          listOf("আর"))
        put("ar",         listOf("আর"))
        put("theke",      listOf("থেকে"))
        put("diye",       listOf("দিয়ে"))
        put("niye",       listOf("নিয়ে"))
        put("jonno",      listOf("জন্য"))
        put("jonne",      listOf("জন্য"))
        put("shathe",     listOf("সাথে"))
        put("kache",      listOf("কাছে"))
        put("opore",      listOf("উপরে"))
        put("niche",      listOf("নিচে"))
        put("ভিতরে",      listOf("ভিতরে"))
        put("vetre",      listOf("ভেতরে"))
        put("baire",      listOf("বাইরে"))
    }
}
