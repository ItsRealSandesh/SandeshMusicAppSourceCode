package com.sandeshmusic.app.data.remote

import com.sandeshmusic.app.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

interface MusicApi {
    suspend fun fetchSongs(): List<Song>
}

class MusicApiImpl(private val okHttpClient: OkHttpClient) : MusicApi {

    companion object {
        const val SONGS_JSON_URL =
            "https://song.codewithsandesh.com/songs.json"
    }

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    override suspend fun fetchSongs(): List<Song> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(SONGS_JSON_URL)
            .header("Cache-Control", "no-cache")
            .build()

        val response = try {
            okHttpClient.newCall(request).execute()
        } catch (e: Exception) {
            throw IOException("Unable to fetch songs: ${e.localizedMessage ?: "Network error"}", e)
        }

        response.use { resp ->
            if (!resp.isSuccessful) {
                throw IOException("Server returned HTTP ${resp.code}: ${resp.message}")
            }

            val rawBody = resp.body?.string()?.trim()

            // Handle empty file or blank string safely without EOF JSON parsing exception
            if (rawBody.isNullOrBlank() || rawBody == "[]") {
                return@withContext emptyList()
            }

            var sanitizedBody = rawBody
            // Auto-heal unclosed JSON array if the remote file omitted the closing bracket
            if (sanitizedBody.startsWith("[") && !sanitizedBody.endsWith("]")) {
                sanitizedBody = "$sanitizedBody\n]"
            }

            try {
                val songs = jsonParser.decodeFromString<List<Song>>(sanitizedBody)
                songs.map { it.withResolvedCovers() }
            } catch (e: Exception) {
                throw IOException("Failed to parse song library metadata: ${e.localizedMessage}", e)
            }
        }
    }
}
