package com.sandeshmusic.app.data.repository

import android.content.Context
import android.util.Log
import com.sandeshmusic.app.data.auth.AuthRepository
import com.sandeshmusic.app.data.firestore.FirestoreSyncManager
import com.sandeshmusic.app.data.firestore.SyncResult
import com.sandeshmusic.app.data.firestore.SyncState
import com.sandeshmusic.app.data.local.AppDatabase
import com.sandeshmusic.app.data.local.CachedSongEntity
import com.sandeshmusic.app.data.local.FavoriteEntity
import com.sandeshmusic.app.data.local.RecentPlayEntity
import com.sandeshmusic.app.data.model.Song
import com.sandeshmusic.app.data.offline.OfflineMusicManager
import com.sandeshmusic.app.data.remote.MusicApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStreamReader

class MusicRepository(
    private val context: Context,
    private val musicApi: MusicApi,
    private val database: AppDatabase,
    private val authRepository: AuthRepository,
    private val firestoreSyncManager: FirestoreSyncManager,
    private val offlineMusicManager: OfflineMusicManager
) {
    companion object {
        private const val TAG = "MusicRepository"
    }

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val songDao = database.songDao()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    val cachedSongsFlow: Flow<List<Song>> = songDao.getCachedSongsFlow().map { entities ->
        entities.map { it.toSong().withResolvedCovers() }
    }

    val favoritesFlow: Flow<Set<String>> = songDao.getFavoritesFlow().map { entities ->
        entities.map { it.songId }.toSet()
    }

    val recentPlaysFlow: Flow<List<Song>> = combine(
        songDao.getRecentPlaysFlow(),
        cachedSongsFlow
    ) { recents, songs ->
        val songMap = songs.associateBy { it.id }
        recents.mapNotNull { recent ->
            songMap[recent.songId]
        }
    }

    val downloadedSongsFlow: Flow<List<Song>> = offlineMusicManager.downloadedSongsFlow
    val downloadingIds: StateFlow<Set<String>> = offlineMusicManager.downloadingIds
    val totalDownloadedBytesFlow: Flow<Long> = offlineMusicManager.totalDownloadedBytesFlow

    init {
        // Automatically sync favorites in background whenever user is authenticated or logs in
        repositoryScope.launch {
            authRepository.currentUserFlow.collect { user ->
                if (user != null) {
                    try {
                        syncFavoritesWithCloud()
                    } catch (e: Exception) {
                        Log.d(TAG, "Background auto-sync failed: ${e.message}")
                    }
                }
            }
        }
    }

    suspend fun downloadSong(song: Song): Result<Unit> = offlineMusicManager.downloadSong(song)

    suspend fun deleteDownload(songId: String): Result<Unit> = offlineMusicManager.deleteDownload(songId)

    fun isSongDownloaded(songId: String): Boolean = offlineMusicManager.isDownloaded(songId)

    fun isDownloadedFlow(songId: String): Flow<Boolean> = offlineMusicManager.isDownloadedFlow(songId)

    suspend fun refreshSongs(): Result<List<Song>> = withContext(Dispatchers.IO) {
        try {
            val remoteSongs = musicApi.fetchSongs()
            // Only replace cached songs on successful fetch
            val entities = remoteSongs.mapIndexed { index, song ->
                CachedSongEntity.fromSong(song, index)
            }
            songDao.replaceCachedSongs(entities)

            // Auto-sync favorites with cloud when library is refreshed
            if (authRepository.getCurrentUser() != null) {
                repositoryScope.launch {
                    try {
                        syncFavoritesWithCloud()
                    } catch (_: Exception) {
                    }
                }
            }

            Result.success(remoteSongs)
        } catch (e: Exception) {
            val cached = songDao.getCachedSongsSync()
            if (cached.isEmpty()) {
                // If local cache is empty and network failed, load the bundled sample so user isn't stranded
                val sampleSongs = loadSampleSongsFromAssets()
                if (sampleSongs.isNotEmpty()) {
                    val entities = sampleSongs.mapIndexed { index, song ->
                        CachedSongEntity.fromSong(song, index)
                    }
                    songDao.replaceCachedSongs(entities)
                    return@withContext Result.success(sampleSongs)
                }
            }
            Result.failure(e)
        }
    }

    private fun loadSampleSongsFromAssets(): List<Song> {
        return try {
            context.assets.open("sample_songs.json").use { inputStream ->
                val json = InputStreamReader(inputStream).readText()
                if (json.isNotBlank() && json != "[]") {
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        coerceInputValues = true
                    }.decodeFromString<List<Song>>(json)
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun toggleFavorite(song: Song, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        val user = authRepository.getCurrentUser()

        if (isFavorite) {
            // Unfavorite
            songDao.removeFavorite(song.id)
            if (user != null) {
                repositoryScope.launch {
                    firestoreSyncManager.removeFavorite(user.uid, song.id)
                }
            }
        } else {
            // Favorite
            songDao.addFavorite(
                FavoriteEntity(
                    songId = song.id,
                    addedAt = System.currentTimeMillis()
                )
            )
            if (user != null) {
                repositoryScope.launch {
                    firestoreSyncManager.uploadFavorite(user.uid, song)
                }
            }
        }
    }

    suspend fun syncFavoritesWithCloud(): Result<SyncResult> = withContext(Dispatchers.IO) {
        val user = authRepository.getCurrentUser()
            ?: return@withContext Result.failure(IllegalStateException("Please sign in to sync favorites."))

        _syncState.value = SyncState.Syncing

        try {
            val localFavs = favoritesFlow.first()
            val allCached = cachedSongsFlow.first()
            val songMap = allCached.associateBy { it.id }

            val result = firestoreSyncManager.syncFavorites(
                userId = user.uid,
                localFavoriteIds = localFavs,
                getSongById = { id -> songMap[id] }
            )

            result.fold(
                onSuccess = { syncResult ->
                    if (syncResult.addedToLocal.isNotEmpty()) {
                        val newEntities = syncResult.addedToLocal.map { songId ->
                            FavoriteEntity(songId = songId, addedAt = System.currentTimeMillis())
                        }
                        songDao.insertFavorites(newEntities)
                    }
                    _syncState.value = SyncState.Synced(
                        count = syncResult.totalCount,
                        timestamp = System.currentTimeMillis()
                    )
                    Result.success(syncResult)
                },
                onFailure = { error ->
                    _syncState.value = SyncState.Error(error.localizedMessage ?: "Sync failed")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.localizedMessage ?: "Sync failed")
            Result.failure(e)
        }
    }

    suspend fun recordRecentPlay(songId: String) = withContext(Dispatchers.IO) {
        songDao.recordRecentPlay(RecentPlayEntity(songId = songId, playedAt = System.currentTimeMillis()))
        songDao.trimRecentPlays(50)
    }

    suspend fun clearRecentPlays() = withContext(Dispatchers.IO) {
        songDao.clearRecentPlays()
    }
}
