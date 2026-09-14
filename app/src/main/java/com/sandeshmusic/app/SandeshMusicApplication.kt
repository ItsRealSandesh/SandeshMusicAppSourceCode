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
import com.sandeshmusic.app.data.preferences.ThemePreferencesRepository
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
        com.sandeshmusic.app.inappmessaging.InAppMessagingHelper.initialize(this)

        // Initialize and subscribe to Firebase Cloud Messaging (Push Notifications)
        applicationScope.launch {
            try {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val token = task.result
                            android.util.Log.d("SandeshFCM", "==================================================")
                            android.util.Log.d("SandeshFCM", "FCM Device Token for Test Push Notifications:")
                            android.util.Log.d("SandeshFCM", token ?: "unknown")
                            android.util.Log.d("SandeshFCM", "==================================================")
                            
                            // Only subscribe to topic once token/registration is confirmed
                            com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("all_users")
                                .addOnCompleteListener { subTask ->
                                    if (subTask.isSuccessful) {
                                        android.util.Log.d("SandeshFCM", "Subscribed to 'all_users' topic successfully")
                                    } else {
                                        android.util.Log.w("SandeshFCM", "Failed to subscribe to 'all_users' topic", subTask.exception)
                                    }
                                }
                        } else {
                            android.util.Log.w("SandeshFCM", "FCM token registration failed or waiting for Google Play Services connection: ${task.exception?.message}")
                        }
                    }
            } catch (e: Exception) {
                android.util.Log.w("SandeshFCM", "FCM initialization non-fatal exception: ${e.message}")
            }
        }
    }

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    val themePreferencesRepository: ThemePreferencesRepository by lazy {
        ThemePreferencesRepository(this)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository()
    }

    val googleAuthManager: com.sandeshmusic.app.data.auth.GoogleAuthManager by lazy {
        com.sandeshmusic.app.data.auth.GoogleAuthManager(this)
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

    val userAudioManager: com.sandeshmusic.app.data.local.UserAudioManager by lazy {
        com.sandeshmusic.app.data.local.UserAudioManager(
            context = this,
            database = database
        )
    }

    val musicRepository: MusicRepository by lazy {
        MusicRepository(
            context = this,
            musicApi = NetworkModule.musicApi,
            database = database,
            authRepository = authRepository,
            firestoreSyncManager = firestoreSyncManager,
            offlineMusicManager = offlineMusicManager,
            userAudioManager = userAudioManager
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
                userAudioManager.getPlayableUri(song) ?: offlineMusicManager.getPlayableUri(song)
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
