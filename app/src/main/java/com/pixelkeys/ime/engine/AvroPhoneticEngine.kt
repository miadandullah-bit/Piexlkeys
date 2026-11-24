package com.pixelkeys.ime.engine

/**
 * Lightweight, zero-latency Avro Phonetic transliteration engine.
 * Converts Romanized Bengali (e.g. "ami banglay gan gai") into Unicode Bengali.
 */
object AvroPhoneticEngine {

    private val independentVowels = linkedMapOf(
        "OU" to "ঔ", "OI" to "ঐ", "rri" to "ঋ",
        "oo" to "উ", "ee" to "ঈ",
        "o" to "অ", "a" to "আ", "i" to "ই", "I" to "ঈ",
        "u" to "উ", "U" to "ঊ", "e" to "এ", "O" to "ও"
    )

    private val vowelKars = linkedMapOf(
        "OU" to "ৌ", "OI" to "ৈ", "rri" to "ৃ",
        "oo" to "ু", "ee" to "ী",
        "o" to "", "a" to "া", "i" to "ি", "I" to "ী",
        "u" to "ু", "U" to "ূ", "e" to "ে", "O" to "ো"
    )

    private val consonants = linkedMapOf(
        "kh" to "খ", "gh" to "ঘ", "Ng" to "ঙ",
        "ch" to "ছ", "jh" to "ঝ", "NG" to "ঞ",
        "Th" to "ঠ", "Dh" to "ঢ", "th" to "থ", "dh" to "ধ",
        "ph" to "ফ", "bh" to "ভ", "sh" to "শ", "Sh" to "ষ",
        "Rh" to "ঢ়", "t`" to "ৎ", "ng" to "ং",
        "k" to "ক", "g" to "গ", "c" to "চ", "j" to "জ",
        "T" to "ট", "D" to "ড", "N" to "ণ",
        "t" to "ত", "d" to "দ", "n" to "ন",
        "p" to "প", "f" to "ফ", "b" to "ব", "v" to "ভ", "m" to "ম",
        "z" to "য", "r" to "র", "l" to "ল",
        "S" to "ষ", "s" to "স", "h" to "হ",
        "R" to "ড়", "y" to "য়", "Y" to "য়",
        ":" to "ঃ", "^" to "ঁ"
    )

    private val commonPhrases = mapOf(
        "ami" to listOf("আমি", "অমি"),
        "tumi" to listOf("তুমি"),
        "amra" to listOf("আমরা"),
        "tomra" to listOf("তোমরা"),
        "bangla" to listOf("বাংলা", "বাঙলা"),
        "banglay" to listOf("বাংলায়", "বাংলাই"),
        "bangladesh" to listOf("বাংলাদেশ"),
        "gan" to listOf("গান", "গন"),
        "gai" to listOf("গাই", "গাই"),
        "kemon" to listOf("কেমন"),
        "acho" to listOf("আছো", "আছ"),
        "achi" to listOf("আছি"),
        "bhalo" to listOf("ভালো", "ভাল"),
        "valo" to listOf("ভালো", "ভাল"),
        "dhonnobad" to listOf("ধন্যবাদ"),
        "ki" to listOf("কি", "কী"),
        "keno" to listOf("কেন", "কেনো"),
        "kothay" to listOf("কোথায়"),
        "bhai" to listOf("ভাই"),
        "vai" to listOf("ভাই"),
        "apu" to listOf("আপু"),
        "bondhu" to listOf("বন্ধু"),
        "shokal" to listOf("সকাল"),
        "rat" to listOf("রাত"),
        "bikel" to listOf("বিকেল"),
        "khub" to listOf("খুব"),
        "sundor" to listOf("সুন্দর"),
        "shundor" to listOf("সুন্দর"),
        "prem" to listOf("প্রেম"),
        "mon" to listOf("মন")
    )

    fun transliterate(input: String): String {
        if (input.isEmpty()) return ""
        val sb = StringBuilder()
        var i = 0
        var prevWasConsonant = false

        while (i < input.length) {
            // Check digits
            val ch = input[i]
            if (ch in '0'..'9') {
                sb.append(('০'.code + (ch - '0')).toChar())
                prevWasConsonant = false
                i++
                continue
            }

            // Try consonant match first (longest key first)
            val consonantMatch = matchPrefix(input, i, consonants.keys)
            if (consonantMatch != null) {
                val banglaConsonant = consonants[consonantMatch]!!
                if (prevWasConsonant && consonantMatch !in listOf(":", "^", "ng", "t`")) {
                    // Check if user explicitly typed double consonant or cluster; keep simple for fluid typing
                    // Support explicit hasanta with '_' or natural cluster for common pairs
                }
                sb.append(banglaConsonant)
                prevWasConsonant = consonantMatch !in listOf(":", "^", "ng", "t`")
                i += consonantMatch.length
                continue
            }

            // Explicit hasanta with '_'
            if (ch == '_' && prevWasConsonant) {
                sb.append("্")
                prevWasConsonant = false
                i++
                continue
            }

            // Try vowel match
            val vowelMatch = matchPrefix(input, i, independentVowels.keys)
            if (vowelMatch != null) {
                if (prevWasConsonant) {
                    sb.append(vowelKars[vowelMatch] ?: "")
                } else {
                    sb.append(independentVowels[vowelMatch] ?: "")
                }
                prevWasConsonant = false
                i += vowelMatch.length
                continue
            }

            // Fallback: copy character as-is
            sb.append(ch)
            prevWasConsonant = false
            i++
        }

        return sb.toString()
    }

    fun getCandidates(romanInput: String): List<String> {
        if (romanInput.isBlank()) return emptyList()
        val lower = romanInput.lowercase()
        val results = LinkedHashSet<String>()
        commonPhrases[lower]?.forEach { results.add(it) }
        val direct = transliterate(romanInput)
        if (direct.isNotEmpty()) {
            results.add(direct)
        }
        return results.take(3)
    }

    private fun matchPrefix(input: String, index: Int, keys: Set<String>): String? {
        for (key in keys) {
            if (input.startsWith(key, startIndex = index)) {
                return key
            }
        }
        return null
    }
}
