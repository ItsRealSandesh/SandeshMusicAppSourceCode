package com.sandeshmusic.app.data.offline

import android.content.Context
import android.net.Uri
import android.util.Log
import com.sandeshmusic.app.data.local.AppDatabase
import com.sandeshmusic.app.data.local.DownloadedSongEntity
import com.sandeshmusic.app.data.model.Song
import com.sandeshmusic.app.data.remote.NetworkModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class OfflineMusicManager(
    private val context: Context,
    private val database: AppDatabase,
    private val okHttpClient: OkHttpClient = NetworkModule.okHttpClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val songDao = database.songDao()

    private val downloadsDir: File by lazy {
        File(context.filesDir, "downloads").apply { if (!exists()) mkdirs() }
    }

    private val recentAudioCacheDir: File by lazy {
        File(context.cacheDir, "recent_audio_cache").apply { if (!exists()) mkdirs() }
    }

    private val _downloadingIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingIds: StateFlow<Set<String>> = _downloadingIds.asStateFlow()

    val downloadedSongsFlow: Flow<List<Song>> = songDao.getDownloadedSongsFlow().map { entities ->
        entities.map { it.toSong() }
    }

    val totalDownloadedBytesFlow: Flow<Long> = songDao.getTotalDownloadedBytesFlow()

    fun isDownloadedFlow(songId: String): Flow<Boolean> = songDao.isDownloadedFlow(songId)

    /**
     * Resolves the playable Uri for a song:
     * 1. Returns local file Uri if saved in Downloads (device library)
     * 2. Returns local file Uri if cached in recent 2-3 plays cache
     * 3. Returns null if not stored locally (will stream over network)
     */
    fun getPlayableUri(song: Song): Uri? {
        val downloadFile = File(downloadsDir, "${song.id}.mp3")
        if (downloadFile.exists() && downloadFile.length() > 0) {
            return Uri.fromFile(downloadFile)
        }

        val cacheFile = File(recentAudioCacheDir, "${song.id}.mp3")
        if (cacheFile.exists() && cacheFile.length() > 0) {
            return Uri.fromFile(cacheFile)
        }

        return null
    }

    fun isDownloaded(songId: String): Boolean {
        val downloadFile = File(downloadsDir, "$songId.mp3")
        return downloadFile.exists() && downloadFile.length() > 0
    }

    /**
     * Automatically caches the audio file of the last 2-3 played songs into
     * local cache directory so the user can listen even when offline without internet.
     */
    fun cacheRecentSongAudio(song: Song) {
        if (song.audioUrl.isBlank()) return

        // If already downloaded permanently, no need to cache duplicate
        val downloadFile = File(downloadsDir, "${song.id}.mp3")
        if (downloadFile.exists() && downloadFile.length() > 0) return

        val cachedFile = File(recentAudioCacheDir, "${song.id}.mp3")
        if (cachedFile.exists() && cachedFile.length() > 0) {
            // Update last modified for LRU eviction
            cachedFile.setLastModified(System.currentTimeMillis())
            return
        }

        scope.launch {
            try {
                val tempFile = File(recentAudioCacheDir, "${song.id}.tmp")
                val request = Request.Builder().url(song.audioUrl).build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val body = response.body ?: return@use

                    FileOutputStream(tempFile).use { output ->
                        body.byteStream().copyTo(output)
                    }

                    if (tempFile.exists() && tempFile.length() > 0) {
                        tempFile.renameTo(cachedFile)
                        cachedFile.setLastModified(System.currentTimeMillis())
                        Log.d(TAG, "Cached audio for last played song: ${song.title}")
                    } else {
                        tempFile.delete()
                    }
                }

                trimRecentCache(maxCount = 3)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to cache recent song audio: ${e.message}")
            }
        }
    }

    private fun trimRecentCache(maxCount: Int = 3) {
        try {
            val files = recentAudioCacheDir.listFiles { file -> file.extension == "mp3" } ?: return
            if (files.size > maxCount) {
                // Sort oldest modified first
                val sorted = files.sortedBy { it.lastModified() }
                val toDelete = sorted.take(files.size - maxCount)
                toDelete.forEach { file ->
                    file.delete()
                    Log.d(TAG, "Evicted old cached song file: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error trimming recent cache: ${e.message}")
        }
    }

    /**
     * Downloads a song to the device library for permanent offline listening.
     */
    suspend fun downloadSong(song: Song): Result<Unit> = withContext(Dispatchers.IO) {
        if (song.audioUrl.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Song audio URL is empty."))
        }

        _downloadingIds.update { it + song.id }
        try {
            val destFile = File(downloadsDir, "${song.id}.mp3")
            val tempFile = File(downloadsDir, "${song.id}.tmp")

            val request = Request.Builder().url(song.audioUrl).build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                response.close()
                return@withContext Result.failure(IllegalStateException("Download failed with HTTP ${response.code}"))
            }

            val body = response.body ?: return@withContext Result.failure(IllegalStateException("Empty response body"))

            FileOutputStream(tempFile).use { output ->
                body.byteStream().copyTo(output)
            }

            if (tempFile.exists() && tempFile.length() > 0) {
                if (destFile.exists()) destFile.delete()
                tempFile.renameTo(destFile)

                val entity = DownloadedSongEntity.fromSong(
                    song = song,
                    localFilePath = destFile.absolutePath,
                    fileSize = destFile.length()
                )
                songDao.insertDownloadedSong(entity)
                Log.d(TAG, "Downloaded song successfully: ${song.title} (${destFile.length()} bytes)")
                Result.success(Unit)
            } else {
                tempFile.delete()
                Result.failure(IllegalStateException("Downloaded file is empty."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download failed for ${song.title}", e)
            Result.failure(e)
        } finally {
            _downloadingIds.update { it - song.id }
        }
    }

    /**
     * Deletes a downloaded song from the device.
     */
    suspend fun deleteDownload(songId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(downloadsDir, "$songId.mp3")
            if (file.exists()) {
                file.delete()
            }
            songDao.deleteDownloadedSong(songId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "OfflineMusicManager"

        fun formatFileSize(bytes: Long): String {
            if (bytes <= 0) return "0 MB"
            val mb = bytes.toDouble() / (1024 * 1024)
            return if (mb < 0.1) {
                val kb = bytes.toDouble() / 1024
                String.format("%.1f KB", kb)
            } else {
                String.format("%.1f MB", mb)
            }
        }
    }
}
