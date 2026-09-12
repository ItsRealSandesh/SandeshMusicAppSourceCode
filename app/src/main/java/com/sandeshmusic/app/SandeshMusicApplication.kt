package com.sandeshmusic.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.sandeshmusic.app.data.auth.AuthRepository
import com.sandeshmusic.app.data.firestore.FirestoreSyncManager
import com.sandeshmusic.app.data.local.AppDatabase
import com.sandeshmusic.app.data.offline.OfflineMusicManager
import com.sandeshmusic.app.data.remote.NetworkModule
import com.sandeshmusic.app.data.repository.MusicRepository
import com.sandeshmusic.app.player.PlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SandeshMusicApplication : Application(), ImageLoaderFactory {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        com.sandeshmusic.app.notifications.NotificationHelper.createNotificationChannel(this)
    }

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository()
    }

    val firestoreSyncManager: FirestoreSyncManager by lazy {
        FirestoreSyncManager()
    }

    val offlineMusicManager: OfflineMusicManager by lazy {
        OfflineMusicManager(
            context = this,
            database = database,
            okHttpClient = NetworkModule.okHttpClient
        )
    }

    val musicRepository: MusicRepository by lazy {
        MusicRepository(
            context = this,
            musicApi = NetworkModule.musicApi,
            database = database,
            authRepository = authRepository,
            firestoreSyncManager = firestoreSyncManager,
            offlineMusicManager = offlineMusicManager
        )
    }

    val playerController: PlayerController by lazy {
        PlayerController(
            context = this,
            onSongPlayed = { song ->
                applicationScope.launch {
                    musicRepository.recordRecentPlay(song.id)
                    offlineMusicManager.cacheRecentSongAudio(song)
                }
            },
            uriResolver = { song ->
                offlineMusicManager.getPlayableUri(song)
            }
        )
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient(NetworkModule.okHttpClient)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()
    }
}
