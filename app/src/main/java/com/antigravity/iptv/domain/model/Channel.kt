package com.antigravity.iptv.domain.model

data class Channel(
    val id: Long = 0,
    val playlistId: Long,
    val tvgId: String = "",
    val tvgName: String = "",
    val name: String,
    val logoUrl: String = "",
    val groupTitle: String = "General",
    val language: String = "",
    val country: String = "",
    val streamUrl: String,
    val orderIndex: Int = 0,
    val isFavorite: Boolean = false,
    val lastWatchedAt: Long? = null
) {
    val cleanResolution: String
        get() {
            val lower = name.lowercase()
            return when {
                lower.contains("4k") || lower.contains("2160p") -> "4K"
                lower.contains("1080p") || lower.contains("fhd") -> "1080p"
                lower.contains("720p") || lower.contains("hd") -> "720p"
                lower.contains("576p") || lower.contains("sd") -> "SD"
                else -> ""
            }
        }

    val displayName: String
        get() {
            // Remove resolution tags from name for cleaner display
            return name
                .replace(Regex("\\((1080p|720p|576p|480p|360p|4k|2160p|hd|sd|fhd)\\)", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\[(1080p|720p|576p|480p|360p|4k|2160p|hd|sd|fhd)\\]", RegexOption.IGNORE_CASE), "")
                .trim()
        }
}
