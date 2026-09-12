package com.sandeshmusic.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Song(
    @SerialName("id")
    val id: String = "",
    @SerialName("title")
    val title: String = "Unknown Title",
    @SerialName("artist")
    val artist: String = "Unknown Artist",
    @SerialName("album")
    val album: String? = null,
    @SerialName("audioUrl")
    val audioUrl: String = "",
    @SerialName("coverUrl")
    val coverUrl: String? = null,
    @SerialName("localPath")
    val localPath: String? = null,
    @SerialName("isDownloaded")
    val isDownloaded: Boolean = false
) {
    fun withResolvedCovers(): Song {
        val resolvedCover = resolveSongCoverUrl(this.coverUrl, this.id)
        return this.copy(coverUrl = resolvedCover)
    }

    companion object {
        fun resolveSongCoverUrl(url: String?, id: String): String? {
            if (url.isNullOrBlank()) {
                val num = id.trim().toIntOrNull()
                return if (num != null) {
                    val padded = if (num < 10) String.format("%02d", num) else num.toString()
                    "https://raw.githubusercontent.com/ItsRealSandesh/SandeshMusic/main/covers/song$padded.png"
                } else {
                    null
                }
            }

            var clean = url.trim()
            if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
                val rel = clean.removePrefix("/")
                clean = "https://raw.githubusercontent.com/ItsRealSandesh/SandeshMusic/main/$rel"
            }

            // Normalizes song1.png / song1.jpg to song01.png to match repository asset
            val singleDigitRegex = Regex("/covers/song([1-9])\\.(png|jpg|jpeg)", RegexOption.IGNORE_CASE)
            if (singleDigitRegex.containsMatchIn(clean)) {
                clean = singleDigitRegex.replace(clean) { matchResult ->
                    val digit = matchResult.groupValues[1]
                    "/covers/song0$digit.png"
                }
            }

            return clean
        }
    }
}
