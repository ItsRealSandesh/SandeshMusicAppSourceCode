package com.sandeshmusic.app.data.local

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.sandeshmusic.app.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class UserAudioManager(
    private val context: Context,
    private val database: AppDatabase
) {
    companion object {
        private const val TAG = "UserAudioManager"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val songDao = database.songDao()

    private val userAudioDir: File by lazy {
        File(context.filesDir, "user_audio").apply { if (!exists()) mkdirs() }
    }

    private val userCoversDir: File by lazy {
        File(context.filesDir, "user_covers").apply { if (!exists()) mkdirs() }
    }

    val userAudioSongsFlow: Flow<List<Song>> = songDao.getUserAudioTracksFlow().map { list ->
        list.map { it.toSong() }
    }

    val totalUserAudioBytesFlow: Flow<Long> = songDao.getTotalUserAudioBytesFlow()

    fun getPlayableUri(song: Song): Uri? {
        if (song.localPath != null) {
            val file = File(song.localPath)
            if (file.exists() && file.length() > 0) {
                return Uri.fromFile(file)
            }
        }
        val file = File(userAudioDir, "${song.id}.mp3")
        if (file.exists() && file.length() > 0) {
            return Uri.fromFile(file)
        }
        return null
    }

    suspend fun importAudioUri(
        uri: Uri,
        customTitle: String? = null,
        customArtist: String? = null
    ): Result<Song> = withContext(Dispatchers.IO) {
        try {
            val id = "user_audio_" + UUID.randomUUID().toString().take(10)
            val originalFileName = getFileNameFromUri(uri) ?: "audio_${System.currentTimeMillis()}.mp3"
            val extension = originalFileName.substringAfterLast(".", "mp3")
            val destFile = File(userAudioDir, "$id.$extension")

            // Copy stream to app internal storage
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return@withContext Result.failure(Exception("Cannot open selected audio stream"))

            if (!destFile.exists() || destFile.length() == 0L) {
                return@withContext Result.failure(Exception("Imported audio file is empty"))
            }

            val fileSize = destFile.length()

            // Extract metadata from audio file
            var extractedTitle: String? = null
            var extractedArtist: String? = null
            var extractedAlbum: String? = null
            var durationMs: Long = 0L
            var coverFilePath: String? = null

            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(destFile.absolutePath)
                extractedTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                extractedArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                extractedAlbum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                durationMs = durStr?.toLongOrNull() ?: 0L

                // Extract embedded cover image if present
                val embeddedPic = retriever.embeddedPicture
                if (embeddedPic != null && embeddedPic.isNotEmpty()) {
                    val coverFile = File(userCoversDir, "$id.jpg")
                    FileOutputStream(coverFile).use { fos ->
                        fos.write(embeddedPic)
                    }
                    if (coverFile.exists() && coverFile.length() > 0) {
                        coverFilePath = coverFile.absolutePath
                    }
                }
                retriever.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error extracting metadata from audio: ${e.message}")
            }

            val finalTitle = customTitle?.takeIf { it.isNotBlank() }
                ?: extractedTitle?.takeIf { it.isNotBlank() }
                ?: originalFileName.substringBeforeLast(".")

            val finalArtist = customArtist?.takeIf { it.isNotBlank() }
                ?: extractedArtist?.takeIf { it.isNotBlank() }
                ?: "Local Import"

            val finalAlbum = extractedAlbum?.takeIf { it.isNotBlank() } ?: "My Audio Files"

            val entity = UserAudioEntity(
                id = id,
                title = finalTitle,
                artist = finalArtist,
                album = finalAlbum,
                localFilePath = destFile.absolutePath,
                fileSize = fileSize,
                durationMs = durationMs,
                coverFilePath = coverFilePath,
                originalFileName = originalFileName,
                addedAt = System.currentTimeMillis()
            )

            songDao.insertUserAudioTrack(entity)
            Result.success(entity.toSong())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import audio: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteAudioTrack(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val entity = songDao.getUserAudioTrack(id)
            if (entity != null) {
                val audioFile = File(entity.localFilePath)
                if (audioFile.exists()) {
                    audioFile.delete()
                }
                entity.coverFilePath?.let { path ->
                    val coverFile = File(path)
                    if (coverFile.exists()) {
                        coverFile.delete()
                    }
                }
            }
            songDao.deleteUserAudioTrack(id)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete audio track: ${e.message}", e)
            false
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            return cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error retrieving content display name: ${e.message}")
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }
}
