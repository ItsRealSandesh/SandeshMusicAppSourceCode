package com.sandeshmusic.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sandeshmusic.app.data.model.Song

@Entity(tableName = "cached_songs")
data class CachedSongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val audioUrl: String,
    val coverUrl: String?,
    val orderIndex: Int
) {
    fun toSong(): Song = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        audioUrl = audioUrl,
        coverUrl = coverUrl
    )

    companion object {
        fun fromSong(song: Song, index: Int): CachedSongEntity = CachedSongEntity(
            id = song.id,
            title = song.title,
            artist = song.artist,
            album = song.album,
            audioUrl = song.audioUrl,
            coverUrl = song.coverUrl,
            orderIndex = index
        )
    }
}

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val songId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recent_plays")
data class RecentPlayEntity(
    @PrimaryKey val songId: String,
    val playedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "downloaded_songs")
data class DownloadedSongEntity(
    @PrimaryKey val songId: String,
    val title: String,
    val artist: String,
    val album: String?,
    val audioUrl: String,
    val coverUrl: String?,
    val localFilePath: String,
    val fileSize: Long,
    val downloadedAt: Long = System.currentTimeMillis()
) {
    fun toSong(): Song = Song(
        id = songId,
        title = title,
        artist = artist,
        album = album,
        audioUrl = audioUrl,
        coverUrl = coverUrl,
        localPath = localFilePath,
        isDownloaded = true
    )

    companion object {
        fun fromSong(song: Song, localFilePath: String, fileSize: Long): DownloadedSongEntity =
            DownloadedSongEntity(
                songId = song.id,
                title = song.title,
                artist = song.artist,
                album = song.album,
                audioUrl = song.audioUrl,
                coverUrl = song.coverUrl,
                localFilePath = localFilePath,
                fileSize = fileSize
            )
    }
}

@Entity(tableName = "user_audio_tracks")
data class UserAudioEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val localFilePath: String,
    val fileSize: Long,
    val durationMs: Long,
    val coverFilePath: String?,
    val originalFileName: String,
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toSong(): Song = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        audioUrl = localFilePath,
        coverUrl = coverFilePath,
        localPath = localFilePath,
        isDownloaded = true
    )
}



