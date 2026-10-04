package com.antigravity.iptv.data.parser

import android.util.Log
import com.antigravity.iptv.data.local.entity.ChannelEntity
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object M3UParser {

    private const val TAG = "M3UParser"

    fun parse(
        inputStream: InputStream,
        playlistId: Long,
        onProgressBatch: ((List<ChannelEntity>) -> Unit)? = null
    ): List<ChannelEntity> {
        val channels = ArrayList<ChannelEntity>(1000)
        val seenUrls = HashSet<String>(1000)
        val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8), 32768)

        var currentTvgId = ""
        var currentTvgName = ""
        var currentLogoUrl = ""
        var currentGroupTitle = "General"
        var currentLanguage = ""
        var currentCountry = ""
        var currentName = ""
        var hasPendingExtInf = false
        var orderIndex = 0

        reader.useLines { lines ->
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue

                if (trimmed.startsWith("#EXTINF", ignoreCase = true)) {
                    hasPendingExtInf = true
                    currentTvgId = ""
                    currentTvgName = ""
                    currentLogoUrl = ""
                    currentGroupTitle = "General"
                    currentLanguage = ""
                    currentCountry = ""
                    currentName = ""

                    val commaIndex = trimmed.indexOf(',')
                    val attributesPart = if (commaIndex != -1) {
                        currentName = trimmed.substring(commaIndex + 1).trim()
                        trimmed.substring(0, commaIndex)
                    } else {
                        trimmed
                    }

                    parseAttributesFast(attributesPart) { key, value ->
                        when (key) {
                            "tvg-id" -> currentTvgId = value
                            "tvg-name" -> currentTvgName = value
                            "tvg-logo" -> currentLogoUrl = value
                            "group-title" -> if (value.isNotBlank()) currentGroupTitle = value
                            "tvg-country", "country" -> currentCountry = value
                            "tvg-language", "language" -> currentLanguage = value
                        }
                    }

                    if (currentName.isBlank() && currentTvgName.isNotBlank()) {
                        currentName = currentTvgName
                    }
                } else if (!trimmed.startsWith("#") && hasPendingExtInf) {
                    val streamUrl = trimmed
                    if (isValidStreamUrl(streamUrl) && seenUrls.add(streamUrl)) {
                        val finalName = if (currentName.isNotBlank()) currentName else "Channel ${orderIndex + 1}"
                        val detectedLanguage = if (currentLanguage.isNotBlank()) {
                            currentLanguage
                        } else {
                            detectLanguage(finalName, currentTvgName, currentGroupTitle, currentTvgId)
                        }

                        val entity = ChannelEntity(
                            playlistId = playlistId,
                            tvgId = currentTvgId,
                            tvgName = currentTvgName,
                            name = finalName,
                            logoUrl = currentLogoUrl,
                            groupTitle = currentGroupTitle,
                            language = detectedLanguage,
                            country = currentCountry,
                            streamUrl = streamUrl,
                            orderIndex = orderIndex++
                        )
                        channels.add(entity)

                        if (onProgressBatch != null && (channels.size == 25 || channels.size == 100 || channels.size % 250 == 0)) {
                            onProgressBatch(channels.toList())
                        }
                    }
                    hasPendingExtInf = false
                }
            }
        }

        return channels
    }

    private inline fun parseAttributesFast(line: String, onAttribute: (String, String) -> Unit) {
        val len = line.length
        var i = 0

        while (i < len) {
            while (i < len && (line[i] == ' ' || line[i] == '\t')) i++
            if (i >= len) break

            val keyStart = i
            while (i < len && line[i] != '=' && line[i] != ' ' && line[i] != '\t' && line[i] != ',') i++
            if (i >= len || line[i] != '=') {
                i++
                continue
            }
            val key = line.substring(keyStart, i).trim().lowercase()
            i++ // skip '='

            if (i >= len) break

            val value: String
            if (line[i] == '"') {
                i++ // skip open quote
                val valueStart = i
                while (i < len && line[i] != '"') i++
                value = if (i <= len) line.substring(valueStart, i) else ""
                if (i < len && line[i] == '"') i++ // skip close quote
            } else {
                val valueStart = i
                while (i < len && line[i] != ' ' && line[i] != '\t' && line[i] != ',') i++
                value = line.substring(valueStart, i)
            }

            if (key.isNotEmpty()) {
                onAttribute(key, value)
            }
        }
    }

    private fun isValidStreamUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.startsWith("http://") || 
               lower.startsWith("https://") || 
               lower.startsWith("rtmp://") || 
               lower.startsWith("rtsp://")
    }

    fun detectLanguage(name: String, tvgName: String, groupTitle: String, tvgId: String): String {
        val text = "$name $tvgName $groupTitle $tvgId".lowercase()
        return when {
            text.contains("odia") || text.contains("oriya") || text.contains("kalinga") || 
            text.contains("prameya") || text.contains("kanak") || text.contains("nandighosha") || 
            text.contains("sidharth") || text.contains("tarang") || text.contains("argus") ||
            text.contains("otv") -> "Odia"

            text.contains("hindi") || text.contains("aaj tak") || text.contains("ndtv india") || 
            text.contains("abp news") || text.contains("bharat") || text.contains("sansad") || 
            text.contains("dd national") -> "Hindi"

            text.contains("bengali") || text.contains("bangla") || text.contains("kolkata") -> "Bengali"
            text.contains("tamil") || text.contains("polimer") || text.contains("thanthi") || 
            text.contains("puthiyathalaimurai") || text.contains("sun tv") -> "Tamil"

            text.contains("telugu") || text.contains("sakshi") || text.contains("tv9 telugu") || 
            text.contains("ntv") || text.contains("t news") || text.contains("etv") -> "Telugu"

            text.contains("malayalam") || text.contains("asianet") || text.contains("manorama") || 
            text.contains("mathrubhumi") || text.contains("kairali") -> "Malayalam"

            text.contains("kannada") || text.contains("public tv") || text.contains("suvarna") || 
            text.contains("tv9 kannada") -> "Kannada"

            text.contains("punjabi") || text.contains("ptc") -> "Punjabi"
            text.contains("marathi") || text.contains("abp majha") || text.contains("zee 24 taas") -> "Marathi"
            text.contains("gujarati") || text.contains("abp asmita") || text.contains("sandesh") || text.contains("aasthagujarati") -> "Gujarati"
            text.contains("bhojpuri") -> "Bhojpuri"
            text.contains("assamese") || text.contains("assam") || text.contains("dy365") || text.contains("pratidin") -> "Assamese"
            text.contains("urdu") -> "Urdu"
            text.contains("english") || text.contains("times now") || text.contains("wion") || 
            text.contains("india today") || text.contains("republic world") || text.contains("cnbc") -> "English"

            else -> ""
        }
    }
}
