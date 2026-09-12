package com.sandeshmusic.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Query("SELECT * FROM cached_songs ORDER BY orderIndex ASC")
    fun getCachedSongsFlow(): Flow<List<CachedSongEntity>>

    @Query("SELECT * FROM cached_songs ORDER BY orderIndex ASC")
    suspend fun getCachedSongsSync(): List<CachedSongEntity>

    @Query("DELETE FROM cached_songs")
    suspend fun clearCachedSongs()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedSongs(songs: List<CachedSongEntity>)

    @Transaction
    suspend fun replaceCachedSongs(songs: List<CachedSongEntity>) {
        clearCachedSongs()
        insertCachedSongs(songs)
    }

    // Favorites
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getFavoritesFlow(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE songId = :songId)")
    fun isFavoriteFlow(songId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFavorites(favorites: List<FavoriteEntity>)

    @Query("DELETE FROM favorites WHERE songId = :songId")
    suspend fun removeFavorite(songId: String)

    // Recent plays
    @Query("SELECT * FROM recent_plays ORDER BY playedAt DESC LIMIT 50")
    fun getRecentPlaysFlow(): Flow<List<RecentPlayEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordRecentPlay(recent: RecentPlayEntity)

    @Query("DELETE FROM recent_plays WHERE songId NOT IN (SELECT songId FROM recent_plays ORDER BY playedAt DESC LIMIT :maxItems)")
    suspend fun trimRecentPlays(maxItems: Int = 50)

    @Query("DELETE FROM recent_plays")
    suspend fun clearRecentPlays()

    // Downloaded songs for device library
    @Query("SELECT * FROM downloaded_songs ORDER BY downloadedAt DESC")
    fun getDownloadedSongsFlow(): Flow<List<DownloadedSongEntity>>

    @Query("SELECT * FROM downloaded_songs WHERE songId = :songId LIMIT 1")
    suspend fun getDownloadedSong(songId: String): DownloadedSongEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_songs WHERE songId = :songId)")
    fun isDownloadedFlow(songId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_songs WHERE songId = :songId)")
    suspend fun isDownloadedSync(songId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadedSong(song: DownloadedSongEntity)

    @Query("DELETE FROM downloaded_songs WHERE songId = :songId")
    suspend fun deleteDownloadedSong(songId: String)

    @Query("DELETE FROM downloaded_songs")
    suspend fun clearDownloadedSongs()

    @Query("SELECT COALESCE(SUM(fileSize), 0) FROM downloaded_songs")
    fun getTotalDownloadedBytesFlow(): Flow<Long>
}
