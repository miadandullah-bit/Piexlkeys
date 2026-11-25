package com.pixelkeys.ime.keyboard

import com.pixelkeys.ime.data.prefs.InputLanguage

enum class KeyboardLayer {
    ALPHA,
    SYMBOLS_1,
    SYMBOLS_2
}

enum class ShiftState {
    OFF,
    ONCE,
    CAPS_LOCK
}

enum class UtilityDrawer {
    NONE,
    UTILITIES,
    EMOJI_KAOMOJI,
    CLIPBOARD,
    RESIZE,
    VOICE,
    TRANSLATE,
    AI_REFINE    // Module 4: AI Text Tone Refiner
}

enum class KeyType {
    CHAR,
    SHIFT,
    BACKSPACE,
    SYMBOLS,
    EMOJI,
    LANGUAGE,
    SPACE,
    ACTION
}

data class KeySpec(
    val id: String,
    val label: String,
    val output: String = label,
    val secondaryLabel: String? = null,
    val type: KeyType = KeyType.CHAR,
    val weight: Float = 1f
)

object KeyboardLayouts {

    val numberRow: List<KeySpec> = (1..10).map { n ->
        val digit = (n % 10).toString()
        KeySpec(id = "num_$digit", label = digit, output = digit)
    }

    private val qwertyRow1 = listOf("q" to "1", "w" to "2", "e" to "3", "r" to "4", "t" to "5", "y" to "6", "u" to "7", "i" to "8", "o" to "9", "p" to "0")
    private val qwertyRow2 = listOf("a" to "@", "s" to "#", "d" to "$", "f" to "_", "g" to "&", "h" to "-", "j" to "+", "k" to "(", "l" to ")")
    private val qwertyRow3 = listOf("z" to "*", "x" to "\"", "c" to "'", "v" to ":", "b" to ";", "n" to "!", "m" to "?")

    private val bengaliRow1 = listOf("ঙ" to "১", "য" to "২", "ড" to "৩", "প" to "৪", "ট" to "৫", "চ" to "৬", "জ" to "৭", "হ" to "৮", "গ" to "৯", "ড়" to "০")
    private val bengaliRow1Shift = listOf("ং" to "১", "য়" to "২", "ঢ" to "৩", "ফ" to "৪", "ঠ" to "৫", "ছ" to "৬", "ঝ" to "৭", "ঞ" to "৮", "ঘ" to "৯", "ঢ়" to "০")

    private val bengaliRow2 = listOf("ৃ" to "ঋ", "ু" to "উ", "ি" to "ই", "া" to "আ", "্" to "অ", "ব" to "ভ", "ক" to "খ", "ত" to "থ", "দ" to "ধ")
    private val bengaliRow2Shift = listOf("ূ" to "ঊ", "ী" to "ঈ", "ৌ" to "ঔ", "ৈ" to "ঐ", "ো" to "ও", "ভ" to "ব", "খ" to "ক", "থ" to "ত", "ধ" to "দ")

    private val bengaliRow3 = listOf("ে" to "এ", "র" to "ন", "ন" to "ণ", "স" to "শ", "ম" to "ষ", "ল" to "ৎ", "ল" to "ঁ")
    private val bengaliRow3Shift = listOf("অ" to "আ", "ণ" to "ন", "শ" to "স", "ষ" to "স", "হ" to "ঃ", "ৎ" to "ঁ", "ঁ" to "্")

    fun getRows(
        language: InputLanguage,
        layer: KeyboardLayer,
        shiftState: ShiftState,
        showNumberRow: Boolean,
        showEmojiKey: Boolean = true
    ): List<List<KeySpec>> {
        val rows = mutableListOf<List<KeySpec>>()
        if (showNumberRow && layer == KeyboardLayer.ALPHA) {
            rows.add(numberRow)
        }

        when (layer) {
            KeyboardLayer.ALPHA -> {
                val isUpper = shiftState != ShiftState.OFF
                if (language == InputLanguage.BENGALI_NATIVE) {
                    val r1 = if (isUpper) bengaliRow1Shift else bengaliRow1
                    val r2 = if (isUpper) bengaliRow2Shift else bengaliRow2
                    val r3 = if (isUpper) bengaliRow3Shift else bengaliRow3
                    rows.add(r1.map { KeySpec("bn_${it.first}", it.first, it.first, it.second) })
                    rows.add(r2.map { KeySpec("bn_${it.first}", it.first, it.first, it.second) })
                    rows.add(buildBottomAlphaRow(r3.map { KeySpec("bn_${it.first}", it.first, it.first, it.second) }, isUpper))
                } else {
                    rows.add(qwertyRow1.map { (ch, sec) ->
                        val c = if (isUpper) ch.uppercase() else ch
                        KeySpec("en_$ch", c, c, sec)
                    })
                    rows.add(qwertyRow2.map { (ch, sec) ->
                        val c = if (isUpper) ch.uppercase() else ch
                        KeySpec("en_$ch", c, c, sec)
                    })
                    rows.add(buildBottomAlphaRow(qwertyRow3.map { (ch, sec) ->
                        val c = if (isUpper) ch.uppercase() else ch
                        KeySpec("en_$ch", c, c, sec)
                    }, isUpper))
                }
                rows.add(buildBottomBar(language, isSymbols = false, showEmojiKey = showEmojiKey))
            }

            KeyboardLayer.SYMBOLS_1 -> {
                rows.add(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map { KeySpec("s1_$it", it, it) })
                rows.add(listOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "/").map { KeySpec("s1_$it", it, it) })
                rows.add(buildSymbolsRow3(isPage2 = false, listOf("*", "\"", "'", ":", ";", "!", "?")))
                rows.add(buildBottomBar(language, isSymbols = true, showEmojiKey = showEmojiKey))
            }

            KeyboardLayer.SYMBOLS_2 -> {
                rows.add(listOf("~", "`", "|", "•", "√", "π", "÷", "×", "§", "∆").map { KeySpec("s2_$it", it, it) })
                rows.add(listOf("£", "¢", "€", "¥", "^", "°", "=", "{", "}", "\\").map { KeySpec("s2_$it", it, it) })
                rows.add(buildSymbolsRow3(isPage2 = true, listOf("%", "©", "®", "™", "✓", "[", "]")))
                rows.add(buildBottomBar(language, isSymbols = true, showEmojiKey = showEmojiKey))
            }
        }
        return rows
    }

    private fun buildBottomAlphaRow(midKeys: List<KeySpec>, isUpper: Boolean): List<KeySpec> {
        return buildList {
            add(KeySpec(id = "shift", label = if (isUpper) "⬆" else "⇧", type = KeyType.SHIFT, weight = 1.45f))
            addAll(midKeys)
            add(KeySpec(id = "backspace", label = "⌫", type = KeyType.BACKSPACE, weight = 1.45f))
        }
    }

    private fun buildSymbolsRow3(isPage2: Boolean, chars: List<String>): List<KeySpec> {
        return buildList {
            add(KeySpec(id = "sym_page", label = if (isPage2) "1/2" else "=\\<", type = KeyType.SYMBOLS, weight = 1.45f))
            addAll(chars.map { KeySpec("sym_$it", it, it) })
            add(KeySpec(id = "backspace", label = "⌫", type = KeyType.BACKSPACE, weight = 1.45f))
        }
    }

    private fun buildBottomBar(language: InputLanguage, isSymbols: Boolean, showEmojiKey: Boolean): List<KeySpec> {
        return buildList {
            add(KeySpec(id = "symbols", label = if (isSymbols) "ABC" else "?123", type = KeyType.SYMBOLS, weight = 1.3f))
            add(KeySpec(id = "comma", label = ",", output = ",", weight = 0.8f))
            if (showEmojiKey) {
                add(KeySpec(id = "emoji", label = "☺", type = KeyType.EMOJI, weight = 0.9f))
            }
            add(KeySpec(id = "lang", label = language.shortLabel, type = KeyType.LANGUAGE, weight = 1.05f))
            add(KeySpec(id = "space", label = "pixelkeys", output = " ", type = KeyType.SPACE, weight = if (showEmojiKey) 3.1f else 3.8f))
            add(KeySpec(id = "period", label = ".", output = ".", weight = 0.8f))
            add(KeySpec(id = "action", label = "↵", type = KeyType.ACTION, weight = 1.4f))
        }
    }
}
