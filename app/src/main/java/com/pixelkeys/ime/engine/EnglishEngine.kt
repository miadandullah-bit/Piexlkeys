package com.pixelkeys.ime.engine

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

data class SwipePoint(val x: Float, val y: Float)

/**
 * Offline English prediction, proximity autocorrect, and swipe-path decoder.
 */
object EnglishEngine {

    private val qwertyCoords: Map<Char, Pair<Float, Float>> = buildMap {
        val rows = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
        val rowOffsets = listOf(0f, 0.5f, 1.5f)
        rows.forEachIndexed { rowIdx, row ->
            row.forEachIndexed { colIdx, c ->
                put(c, Pair((colIdx + rowOffsets[rowIdx]) / 10f, rowIdx / 3f))
            }
        }
    }

    private val baseDictionary: List<String> = listOf(
        "the", "be", "to", "of", "and", "a", "in", "that", "have", "i",
        "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
        "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
        "or", "an", "will", "my", "one", "all", "would", "there", "their", "what",
        "so", "up", "out", "if", "about", "who", "get", "which", "go", "me",
        "when", "make", "can", "like", "time", "no", "just", "him", "know", "take",
        "people", "into", "year", "your", "good", "some", "could", "them", "see", "other",
        "than", "then", "now", "look", "only", "come", "its", "over", "think", "also",
        "back", "after", "use", "two", "how", "our", "work", "first", "well", "way",
        "even", "new", "want", "because", "any", "these", "give", "day", "most", "us",
        "hello", "hey", "hi", "thanks", "thank", "please", "sorry", "yes", "no", "okay",
        "ok", "great", "awesome", "nice", "cool", "love", "happy", "today", "tomorrow", "night",
        "morning", "evening", "friend", "family", "home", "phone", "keyboard", "pixel", "keys", "android",
        "kotlin", "compose", "theme", "dark", "light", "minimal", "hardware", "typing", "swipe", "voice",
        "translate", "bangla", "english", "message", "send", "call", "later", "soon", "right", "left",
        "where", "why", "here", "always", "never", "maybe", "really", "very", "much", "more",
        "need", "help", "done", "ready", "start", "stop", "open", "close", "save", "delete",
        "copy", "paste", "clipboard", "emoji", "sound", "haptic", "fast", "clean", "design", "studio",
        "world", "life", "music", "photo", "video", "game", "play", "read", "write", "code",
        "build", "test", "app", "screen", "button", "space", "cursor", "trackpad", "word", "text"
    )

    fun suggest(
        composing: String,
        userWords: List<String> = emptyList(),
        autocorrectEnabled: Boolean = true
    ): List<String> {
        if (composing.isBlank()) return emptyList()
        val lower = composing.lowercase()
        val allWords = (userWords + baseDictionary).distinct()

        // 1. Exact prefix matches
        val prefixMatches = allWords
            .filter { it.startsWith(lower) }
            .sortedWith(compareBy<String> { it.length }.thenBy { it })

        if (prefixMatches.size >= 3 || !autocorrectEnabled) {
            return prefixMatches.take(3)
        }

        // 2. Fuzzy / edit-distance matches for typos
        val fuzzyMatches = allWords
            .asSequence()
            .filter { abs(it.length - lower.length) <= 2 && it !in prefixMatches }
            .map { it to editDistance(lower, it) }
            .filter { it.second <= 2 }
            .sortedBy { it.second }
            .map { it.first }
            .toList()

        return (prefixMatches + fuzzyMatches).distinct().take(3)
    }

    fun decodeSwipe(
        points: List<SwipePoint>,
        userWords: List<String> = emptyList()
    ): List<String> {
        if (points.size < 3) return emptyList()
        val start = points.first()
        val end = points.last()

        val startChars = nearestKeys(start, 2)
        val endChars = nearestKeys(end, 2)

        val allWords = (userWords + baseDictionary).distinct()
        val scored = allWords.asSequence()
            .filter { it.length >= 2 }
            .filter { word ->
                word.first() in startChars && word.last() in endChars
            }
            .map { word ->
                word to pathScore(word, points)
            }
            .sortedBy { it.second }
            .map { it.first }
            .take(3)
            .toList()

        return scored
    }

    private fun nearestKeys(point: SwipePoint, count: Int): Set<Char> {
        return qwertyCoords.entries
            .sortedBy { (_, coord) -> hypot(coord.first - point.x, coord.second - point.y) }
            .take(count)
            .map { it.key }
            .toSet()
    }

    private fun pathScore(word: String, points: List<SwipePoint>): Float {
        var total = 0f
        var searchIdx = 0
        for (ch in word) {
            val target = qwertyCoords[ch] ?: continue
            var bestDist = Float.MAX_VALUE
            var bestIdx = searchIdx
            for (i in searchIdx until points.size) {
                val p = points[i]
                val d = hypot(target.first - p.x, target.second - p.y)
                if (d < bestDist) {
                    bestDist = d
                    bestIdx = i
                }
            }
            total += bestDist
            searchIdx = bestIdx
        }
        return total / word.length.coerceAtLeast(1)
    }

    private fun editDistance(a: String, b: String): Int {
        val dp = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..b.length) {
                val temp = dp[j]
                dp[j] = if (a[i - 1] == b[j - 1]) {
                    prev
                } else {
                    1 + min(prev, min(dp[j], dp[j - 1]))
                }
                prev = temp
            }
        }
        return dp[b.length]
    }
}
