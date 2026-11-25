package com.pixelkeys.ime.engine

/**
 * MODULE 2 — Offline Fuzzy Bangla Suggestion Dictionary
 *
 * Lightweight Trie-backed lookup that maps normalized/lowercase Banglish
 * keys (including dropped vowels, alternate spellings) → sorted list of
 * high-frequency Bangla words.
 *
 * Usage:
 *   val dict = BanglaFuzzyDictionary.getInstance()
 *   val suggestions = dict.lookup("amdr")   // → ["আমাদের", "আমার", ...]
 *
 * Design decisions:
 *  - Pure in-memory Trie so there is zero I/O on the typing hot-path.
 *  - Populated at app start-up from the embedded corpus below (< 2 ms on
 *    modern hardware for ~1 000 entries).
 *  - lookup() is O(k) where k = key length.
 *  - Falls back to AvroPhoneticEngine.phraseDictionary so both share one source of truth.
 */
class BanglaFuzzyDictionary private constructor() {

    // ─── Trie Node ────────────────────────────────────────────────────────────

    private class TrieNode {
        val children: HashMap<Char, TrieNode> = HashMap(4)
        val words: MutableList<String> = mutableListOf()
    }

    private val root = TrieNode()

    // ─── Build ────────────────────────────────────────────────────────────────

    private fun insert(key: String, words: List<String>) {
        var node = root
        for (ch in key.lowercase()) {
            node = node.children.getOrPut(ch) { TrieNode() }
        }
        for (word in words) {
            if (!node.words.contains(word)) node.words.add(word)
        }
    }

    private fun buildTrie() {
        // Seed from AvroPhoneticEngine's phrase dictionary (single source of truth)
        for ((key, words) in AvroPhoneticEngine.phraseDictionary) {
            insert(key, words)
        }

        // ── Additional fuzzy aliases not in phrase dictionary ──────────────
        // These cover dropped vowels, common SMS/chat shortcuts, and alternate
        // romanizations used by real Bengali speakers on mobile.
        val fuzzyAliases: Map<String, List<String>> = mapOf(
            // Dropped-vowel forms
            "amdr"    to listOf("আমাদের"),
            "amadr"   to listOf("আমাদের"),
            "amk"     to listOf("আমাকে"),
            "tmk"     to listOf("তোমাকে"),
            "tmr"     to listOf("তোমার"),
            "apnr"    to listOf("আপনার"),
            "amr"     to listOf("আমার"),
            "kmnach"  to listOf("কেমন আছো"),
            "kmnacho" to listOf("কেমন আছো"),
            "bndhu"   to listOf("বন্ধু"),
            "bndh"    to listOf("বন্ধু"),
            "kntu"    to listOf("কিন্তু"),
            "ebn"     to listOf("এবং"),
            "jnno"    to listOf("জন্য"),
            "jnn"     to listOf("জন্য"),
            "thkeo"   to listOf("থেকেও"),
            "niy"     to listOf("নিয়ে"),
            "diy"     to listOf("দিয়ে"),

            // Social-media / chat shorthand
            "ty"      to listOf("ধন্যবাদ", "থ্যাংকস"),
            "ty vm"   to listOf("অনেক ধন্যবাদ"),
            "np"      to listOf("কোনো ব্যাপার না"),
            "lol"     to listOf("হাহা"),
            "haha"    to listOf("হাহাহা"),
            "jk"      to listOf("মজা করছিলাম"),
            "ttyl"    to listOf("পরে কথা হবে"),
            "brb"     to listOf("এক মিনিট"),
            "omg"     to listOf("ওহ মাই গড"),
            "wtf"     to listOf("কী হচ্ছে"),
            "asap"    to listOf("যত তাড়াতাড়ি সম্ভব"),

            // Alternate spellings / common misspellings
            "valo"    to listOf("ভালো"),
            "bhalo"   to listOf("ভালো"),
            "balo"    to listOf("ভালো"),
            "sundor"  to listOf("সুন্দর"),
            "shundor" to listOf("সুন্দর"),
            "sondar"  to listOf("সুন্দর"),
            "kotha"   to listOf("কথা"),
            "gaan"    to listOf("গান"),
            "gan"     to listOf("গান"),
            "khabar"  to listOf("খাবার"),
            "khawar"  to listOf("খাওয়ার"),
            "khaoa"   to listOf("খাওয়া"),
            "pani"    to listOf("পানি"),
            "jol"     to listOf("জল"),
            "rasta"   to listOf("রাস্তা"),
            "rest"    to listOf("রেস্ট", "বিশ্রাম"),
            "jaben"   to listOf("যাবেন"),
            "asben"   to listOf("আসবেন"),
            "bolben"  to listOf("বলবেন"),
            "korben"  to listOf("করবেন"),
            "dekhben" to listOf("দেখবেন"),
            "shunben" to listOf("শুনবেন"),
            "bolun"   to listOf("বলুন"),
            "korun"   to listOf("করুন"),
            "dekhun"  to listOf("দেখুন"),
            "shunun"  to listOf("শুনুন"),
            "jan"     to listOf("জানুন", "জান"),
            "janen"   to listOf("জানেন"),

            // Commonly typed verbs with -ছি/-ছো/-ছে
            "korchi"  to listOf("করছি"),
            "korcho"  to listOf("করছো"),
            "korche"  to listOf("করছে"),
            "jachi"   to listOf("যাচ্ছি"),
            "jacho"   to listOf("যাচ্ছো"),
            "jache"   to listOf("যাচ্ছে"),
            "aschi"   to listOf("আসছি"),
            "ascho"   to listOf("আসছো"),
            "asche"   to listOf("আসছে"),
            "khachi"  to listOf("খাচ্ছি"),
            "khacho"  to listOf("খাচ্ছো"),
            "khache"  to listOf("খাচ্ছে"),
            "dekhchi" to listOf("দেখছি"),
            "dekhcho" to listOf("দেখছো"),
            "dekhche" to listOf("দেখছে"),
            "bolchi"  to listOf("বলছি"),
            "bolcho"  to listOf("বলছো"),
            "bolche"  to listOf("বলছে"),

            // Religious / cultural
            "bismilla" to listOf("বিসমিল্লাহ"),
            "alhamd"   to listOf("আলহামদুলিল্লাহ"),
            "insha"    to listOf("ইনশাল্লাহ"),
            "masha"    to listOf("মাশাল্লাহ"),
            "subhanallah" to listOf("সুবহানাল্লাহ"),
            "astaghfirullah" to listOf("আস্তাগফিরুল্লাহ"),
            "jajakallah" to listOf("জাযাকাল্লাহ"),

            // Emotions / common expressions
            "miss kori"  to listOf("মিস করি"),
            "miss korchi" to listOf("মিস করছি"),
            "love kori"  to listOf("ভালোবাসি"),
            "proud"      to listOf("গর্বিত"),
            "ektu"       to listOf("একটু"),
            "ekta"       to listOf("একটা"),
            "oita"       to listOf("ওইটা"),
            "oikhane"    to listOf("ওইখানে"),
            "ekhane"     to listOf("এখানে"),
            "sekhane"    to listOf("সেখানে"),
            "jaikhanei"  to listOf("যেখানেই"),

            // Professional
            "meeting"    to listOf("মিটিং"),
            "office"     to listOf("অফিস"),
            "work"       to listOf("কাজ"),
            "kaj"        to listOf("কাজ"),
            "chakri"     to listOf("চাকরি"),
            "byabsa"     to listOf("ব্যবসা"),
            "porikhsha"  to listOf("পরীক্ষা"),
            "poriksha"   to listOf("পরীক্ষা"),
            "school"     to listOf("স্কুল"),
            "college"    to listOf("কলেজ"),
            "university" to listOf("বিশ্ববিদ্যালয়"),
            "bishwobidaloy" to listOf("বিশ্ববিদ্যালয়"),
            "hospital"   to listOf("হাসপাতাল"),
            "hashpatal"  to listOf("হাসপাতাল"),
            "doctor"     to listOf("ডাক্তার"),
            "daktar"     to listOf("ডাক্তার"),
            "medicine"   to listOf("ওষুধ"),
            "oshud"      to listOf("ওষুধ"),

            // Tech / modern
            "mobile"     to listOf("মোবাইল"),
            "phone"      to listOf("ফোন"),
            "internet"   to listOf("ইন্টারনেট"),
            "facebook"   to listOf("ফেসবুক"),
            "fb"         to listOf("ফেসবুক"),
            "whatsapp"   to listOf("হোয়াটসঅ্যাপ"),
            "wa"         to listOf("হোয়াটসঅ্যাপ"),
            "youtube"    to listOf("ইউটিউব"),
            "yt"         to listOf("ইউটিউব"),
            "google"     to listOf("গুগল"),
            "app"        to listOf("অ্যাপ"),
            "download"   to listOf("ডাউনলোড"),
            "update"     to listOf("আপডেট"),
            "password"   to listOf("পাসওয়ার্ড"),
            "email"      to listOf("ইমেইল")
        )

        for ((key, words) in fuzzyAliases) {
            insert(key, words)
        }
    }

    // ─── Lookup ───────────────────────────────────────────────────────────────

    /**
     * Returns up to [maxResults] Bangla words for the given roman key.
     * Performs exact match first, then prefix match if exact yields nothing.
     */
    fun lookup(key: String, maxResults: Int = 5): List<String> {
        val normalizedKey = key.trim().lowercase()
        if (normalizedKey.isBlank()) return emptyList()

        val results = LinkedHashSet<String>()

        // 1. Exact node
        val exactNode = navigate(normalizedKey)
        if (exactNode != null) results.addAll(exactNode.words)

        // 2. Prefix expansion (DFS) if we have room
        if (results.size < maxResults && exactNode != null) {
            collectAll(exactNode, results, maxResults)
        }

        // 3. If still nothing, try edit-distance-1 (single char deletion)
        if (results.isEmpty() && normalizedKey.length >= 3) {
            for (omitIdx in normalizedKey.indices) {
                val candidate = normalizedKey.removeRange(omitIdx, omitIdx + 1)
                val node = navigate(candidate)
                if (node != null) {
                    results.addAll(node.words)
                    if (results.size >= maxResults) break
                }
            }
        }

        return results.take(maxResults).toList()
    }

    private fun navigate(key: String): TrieNode? {
        var node = root
        for (ch in key) {
            node = node.children[ch] ?: return null
        }
        return node
    }

    private fun collectAll(node: TrieNode, results: MutableSet<String>, max: Int) {
        if (results.size >= max) return
        results.addAll(node.words)
        for (child in node.children.values) {
            collectAll(child, results, max)
            if (results.size >= max) return
        }
    }

    // ─── Singleton ────────────────────────────────────────────────────────────

    companion object {
        @Volatile
        private var instance: BanglaFuzzyDictionary? = null

        fun getInstance(): BanglaFuzzyDictionary {
            return instance ?: synchronized(this) {
                instance ?: BanglaFuzzyDictionary().also {
                    it.buildTrie()
                    instance = it
                }
            }
        }
    }
}
