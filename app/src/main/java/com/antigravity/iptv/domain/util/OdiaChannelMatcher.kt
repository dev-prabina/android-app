package com.antigravity.iptv.domain.util

import com.antigravity.iptv.domain.model.Channel
import com.antigravity.iptv.domain.model.WatchHistoryItem

object OdiaChannelMatcher {

    private data class CuratedOdiaSpec(
        val canonicalName: String,
        val namePatterns: List<Regex>,
        val idPrefixes: List<String>
    )

    private val CURATED_SPECS = listOf(
        // 1. OTV (Odisha TV)
        CuratedOdiaSpec(
            canonicalName = "OTV",
            namePatterns = listOf(
                Regex("""\botv\b""", RegexOption.IGNORE_CASE),
                Regex("""\bodisha tv\b""", RegexOption.IGNORE_CASE),
                Regex("""\bodisha television\b""", RegexOption.IGNORE_CASE)
            ),
            idPrefixes = listOf("odishatv", "otv")
        ),
        // 2. Kanak News
        CuratedOdiaSpec(
            canonicalName = "Kanak News",
            namePatterns = listOf(
                Regex("""\bkanak news\b""", RegexOption.IGNORE_CASE),
                Regex("""\bkanak tv\b""", RegexOption.IGNORE_CASE),
                Regex("""\bkanak\b""", RegexOption.IGNORE_CASE)
            ),
            idPrefixes = listOf("kanaknews", "kanaktv", "kanak")
        ),
        // 3. Kalinga TV
        CuratedOdiaSpec(
            canonicalName = "Kalinga TV",
            namePatterns = listOf(
                Regex("""\bkalinga tv\b""", RegexOption.IGNORE_CASE),
                Regex("""\bkalinga news\b""", RegexOption.IGNORE_CASE),
                Regex("""\bkalinga\b""", RegexOption.IGNORE_CASE)
            ),
            idPrefixes = listOf("kalingatv", "kalinga")
        ),
        // 4. News18 Odia
        CuratedOdiaSpec(
            canonicalName = "News18 Odia",
            namePatterns = listOf(
                Regex("""\bnews18 odia\b""", RegexOption.IGNORE_CASE),
                Regex("""\bnews 18 odia\b""", RegexOption.IGNORE_CASE)
            ),
            idPrefixes = listOf("news18odia")
        ),
        // 5. Prameya News7
        CuratedOdiaSpec(
            canonicalName = "Prameya News7",
            namePatterns = listOf(
                Regex("""\bprameya news7\b""", RegexOption.IGNORE_CASE),
                Regex("""\bprameya news 7\b""", RegexOption.IGNORE_CASE),
                Regex("""\bprameya\b""", RegexOption.IGNORE_CASE),
                Regex("""\bnews7 odia\b""", RegexOption.IGNORE_CASE),
                Regex("""\bnews 7 odia\b""", RegexOption.IGNORE_CASE)
            ),
            idPrefixes = listOf("prameyanews7", "prameya")
        ),
        // 6. Nandighosha TV
        CuratedOdiaSpec(
            canonicalName = "Nandighosha TV",
            namePatterns = listOf(
                Regex("""\bnandighosha tv\b""", RegexOption.IGNORE_CASE),
                Regex("""\bnandighosha\b""", RegexOption.IGNORE_CASE),
                Regex("""\bnandighosa\b""", RegexOption.IGNORE_CASE)
            ),
            idPrefixes = listOf("nandighoshatv", "nandighosha")
        ),
        // 7. Argus News
        CuratedOdiaSpec(
            canonicalName = "Argus News",
            namePatterns = listOf(
                Regex("""\bargus news\b""", RegexOption.IGNORE_CASE),
                Regex("""\bargus tv\b""", RegexOption.IGNORE_CASE),
                Regex("""\bargus\b""", RegexOption.IGNORE_CASE)
            ),
            idPrefixes = listOf("argusnews", "argus")
        ),
        // 8. Naxatra News
        CuratedOdiaSpec(
            canonicalName = "Naxatra News",
            namePatterns = listOf(
                Regex("""\bnaxatra news\b""", RegexOption.IGNORE_CASE),
                Regex("""\bnaxatra\b""", RegexOption.IGNORE_CASE),
                Regex("""\bnakshatra news odia\b""", RegexOption.IGNORE_CASE)
            ),
            idPrefixes = listOf("naxatranews", "naxatra")
        ),
        // 9. MBC TV
        CuratedOdiaSpec(
            canonicalName = "MBC TV",
            namePatterns = listOf(
                Regex("""\bmbc tv\b""", RegexOption.IGNORE_CASE),
                Regex("""\bmbc news\b""", RegexOption.IGNORE_CASE)
            ),
            idPrefixes = listOf("mbctv")
        ),
        // 10. DD Odia
        CuratedOdiaSpec(
            canonicalName = "DD Odia",
            namePatterns = listOf(
                Regex("""\bdd odia\b""", RegexOption.IGNORE_CASE),
                Regex("""\bdoordarshan odia\b""", RegexOption.IGNORE_CASE)
            ),
            idPrefixes = listOf("ddodia")
        ),
        // 11. Tarang Music
        CuratedOdiaSpec(
            canonicalName = "Tarang Music",
            namePatterns = listOf(
                Regex("""\btarang music\b""", RegexOption.IGNORE_CASE)
            ),
            idPrefixes = listOf("tarangmusic")
        ),
        // Extended verified regional Odia channels
        CuratedOdiaSpec(
            canonicalName = "Tarang TV",
            namePatterns = listOf(
                Regex("""\btarang tv\b""", RegexOption.IGNORE_CASE),
                Regex("""\btarang\b""", RegexOption.IGNORE_CASE)
            ),
            idPrefixes = listOf("tarangtv")
        ),
        CuratedOdiaSpec(
            canonicalName = "Alankar TV",
            namePatterns = listOf(
                Regex("""\balankar tv\b""", RegexOption.IGNORE_CASE),
                Regex("""\balankar\b""", RegexOption.IGNORE_CASE)
            ),
            idPrefixes = listOf("alankartv")
        ),
        CuratedOdiaSpec(
            canonicalName = "Prarthana TV",
            namePatterns = listOf(
                Regex("""\bprarthana tv\b""", RegexOption.IGNORE_CASE)
            ),
            idPrefixes = listOf("prarthanatv")
        ),
        CuratedOdiaSpec(
            canonicalName = "Ekamra Bharat Odia",
            namePatterns = listOf(
                Regex("""\bekamra bharat odia\b""", RegexOption.IGNORE_CASE),
                Regex("""\bekamra\b""", RegexOption.IGNORE_CASE)
            ),
            idPrefixes = listOf("ekamrabharatodia")
        ),
        CuratedOdiaSpec(
            canonicalName = "Sidharth TV",
            namePatterns = listOf(
                Regex("""\bsidharth tv\b""", RegexOption.IGNORE_CASE)
            ),
            idPrefixes = listOf("sidharthtv")
        ),
        CuratedOdiaSpec(
            canonicalName = "Sidharth Gold",
            namePatterns = listOf(
                Regex("""\bsidharth gold\b""", RegexOption.IGNORE_CASE)
            ),
            idPrefixes = listOf("sidharthgold")
        ),
        CuratedOdiaSpec(
            canonicalName = "Sidharth Utsav",
            namePatterns = listOf(
                Regex("""\bsidharth utsav\b""", RegexOption.IGNORE_CASE)
            ),
            idPrefixes = listOf("sidharthutsav")
        )
    )

    private val NON_ODIA_INDICATORS = listOf(
        "tamil", "telugu", "kannada", "bengali", "malayalam",
        "marathi", "gujarati", "punjabi", "bhojpuri", "urdu", "assamese"
    )

    fun normalize(text: String): String {
        return text.lowercase()
            .replace(Regex("""\s*\((1080p|720p|576p|480p|360p|4k|2160p|hd|sd|fhd)\)""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*\[[^\]]*\]"""), "")
            .replace(Regex("""[^a-z0-9\s]"""), " ")
            .split(Regex("""\s+"""))
            .filter { it.isNotBlank() }
            .joinToString(" ")
    }

    /**
     * Determines whether the channel is an Odia channel and returns its curated priority rank.
     * Returns 0..10 for the primary user-suggested channels,
     * 11..17 for extended regional Odia channels,
     * 50 for general metadata-matched Odia channels,
     * -1 if not an Odia channel.
     */
    fun matchOdiaRank(channel: Channel): Int {
        val normName = normalize(channel.name)
        val normTvgName = normalize(channel.tvgName)
        val idClean = channel.tvgId.lowercase().substringBefore('.').substringBefore('@')
        val groupNorm = normalize(channel.groupTitle)
        val langNorm = normalize(channel.language)

        // Negative filter: exclude explicit indicators of other languages/regions
        for (nonOdia in NON_ODIA_INDICATORS) {
            if (normName.contains(nonOdia) || idClean.contains(nonOdia)) {
                return -1
            }
        }

        // 1. Check curated priority specs
        for (index in CURATED_SPECS.indices) {
            val spec = CURATED_SPECS[index]
            // ID prefix match
            for (prefix in spec.idPrefixes) {
                if (idClean == prefix || idClean.startsWith(prefix)) {
                    return index
                }
            }
            // Name regex match
            for (pattern in spec.namePatterns) {
                if (pattern.containsMatchIn(normName) || pattern.containsMatchIn(normTvgName)) {
                    return index
                }
            }
        }

        // 2. Language metadata match
        if (langNorm.contains("odia") || langNorm.contains("ori") || langNorm == "or") {
            return 50
        }

        // 3. Whole-word match for "odia" or "oriya" in name, ID, or group
        val odiaWordRegex = Regex("""\b(odia|oriya)\b""", RegexOption.IGNORE_CASE)
        if (odiaWordRegex.containsMatchIn(normName) ||
            odiaWordRegex.containsMatchIn(idClean) ||
            odiaWordRegex.containsMatchIn(groupNorm)
        ) {
            return 50
        }

        return -1
    }

    fun isOdiaChannel(channel: Channel): Boolean = matchOdiaRank(channel) >= 0

    /**
     * Sorts matching Odia channels using intelligent user prioritization:
     * 1. Favorited channels
     * 2. Recently watched channels (ordered by recency)
     * 3. Matching Odia channels from the suggested list
     * 4. Other Odia regional channels
     * 5. Fallback metadata-matched Odia channels
     */
    fun sortOdiaChannels(
        channels: List<Channel>,
        watchHistory: List<WatchHistoryItem>,
        favorites: List<Channel>
    ): List<Channel> {
        val favoriteIds = favorites.map { it.id }.toSet()
        val historyMap = watchHistory
            .groupBy { it.channelId }
            .mapValues { (_, items) -> items.maxOf { it.watchedAt } }

        val matchingOdia = channels
            .filter { isOdiaChannel(it) }
            .map { ch ->
                if (ch.id in favoriteIds) ch.copy(isFavorite = true) else ch
            }
        if (matchingOdia.isEmpty()) return emptyList()

        return matchingOdia.sortedWith { a, b ->
            val aFav = a.isFavorite || a.id in favoriteIds
            val bFav = b.isFavorite || b.id in favoriteIds

            val aWatchTime = historyMap[a.id] ?: a.lastWatchedAt ?: 0L
            val bWatchTime = historyMap[b.id] ?: b.lastWatchedAt ?: 0L

            val aRank = matchOdiaRank(a)
            val bRank = matchOdiaRank(b)

            // Tier determination:
            // Tier 0: Favorited
            // Tier 1: Watched
            // Tier 2: Curated suggested list (rank in 0..10)
            // Tier 3: Extended regional Odia channels (rank in 11..17)
            // Tier 4: Metadata Odia (rank 50)
            val aTier = when {
                aFav -> 0
                aWatchTime > 0L -> 1
                aRank in 0..10 -> 2
                aRank in 11..30 -> 3
                else -> 4
            }

            val bTier = when {
                bFav -> 0
                bWatchTime > 0L -> 1
                bRank in 0..10 -> 2
                bRank in 11..30 -> 3
                else -> 4
            }

            if (aTier != bTier) {
                aTier.compareTo(bTier)
            } else {
                when (aTier) {
                    0 -> {
                        // For favorites, sort by curated rank then recency
                        if (aRank != bRank) aRank.compareTo(bRank)
                        else bWatchTime.compareTo(aWatchTime)
                    }
                    1 -> {
                        // For watched, sort most recent first
                        bWatchTime.compareTo(aWatchTime)
                    }
                    2, 3 -> {
                        // For curated list, sort by predefined rank
                        if (aRank != bRank) aRank.compareTo(bRank)
                        else a.name.compareTo(b.name, ignoreCase = true)
                    }
                    else -> {
                        a.name.compareTo(b.name, ignoreCase = true)
                    }
                }
            }
        }
    }
}
