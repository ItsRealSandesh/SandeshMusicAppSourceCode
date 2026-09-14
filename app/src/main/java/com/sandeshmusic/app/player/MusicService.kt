package com.sandeshmusic.app.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.sandeshmusic.app.MainActivity
import com.sandeshmusic.app.data.model.Song
import com.example.R

class MusicService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private val audioEffectsManager = AudioEffectsManager()

    companion object {
        const val CHANNEL_ID = "sandesh_music_playback_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_SET_VOLUME_BOOST = "com.sandeshmusic.app.ACTION_SET_VOLUME_BOOST"
        const val ACTION_SET_BASS_BOOST = "com.sandeshmusic.app.ACTION_SET_BASS_BOOST"
        const val KEY_BOOST_PERCENT = "key_boost_percent"
        const val KEY_BASS_STRENGTH = "key_bass_strength"
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("SandeshMusic/1.0 (Linux; Android)")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        val dataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        val exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        this.player = exoPlayer

        // Listen for audio session ID changes to bind the hardware audio effects
        exoPlayer.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                audioEffectsManager.attachSession(audioSessionId)
            }
        })

        if (exoPlayer.audioSessionId != C.AUDIO_SESSION_ID_UNSET && exoPlayer.audioSessionId != 0) {
            audioEffectsManager.attachSession(exoPlayer.audioSessionId)
        }

        val sessionActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            sessionActivityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val sessionCallback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                    .add(SessionCommand(ACTION_SET_VOLUME_BOOST, Bundle.EMPTY))
                    .add(SessionCommand(ACTION_SET_BASS_BOOST, Bundle.EMPTY))
                    .build()
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(commands)
                    .build()
            }

            override fun onSetMediaItems(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: MutableList<MediaItem>,
                startIndex: Int,
                startPositionMs: Long
            ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                val updatedMediaItems = mediaItems.map { resolveMediaItemUri(it) }.toMutableList()
                return Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(
                        updatedMediaItems,
                        startIndex,
                        startPositionMs
                    )
                )
            }

            override fun onAddMediaItems(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: MutableList<MediaItem>
            ): ListenableFuture<MutableList<MediaItem>> {
                val updatedMediaItems = mediaItems.map { resolveMediaItemUri(it) }.toMutableList()
                return Futures.immediateFuture(updatedMediaItems)
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                when (customCommand.customAction) {
                    ACTION_SET_VOLUME_BOOST -> {
                        val boost = args.getInt(KEY_BOOST_PERCENT, 100)
                        audioEffectsManager.setBoostPercent(boost)
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    ACTION_SET_BASS_BOOST -> {
                        val strength = args.getInt(KEY_BASS_STRENGTH, 0)
                        audioEffectsManager.setBassBoostPercent(strength)
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                }
                return super.onCustomCommand(session, controller, customCommand, args)
            }
        }

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(sessionCallback)
            .build()

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(CHANNEL_ID)
                .setChannelName(R.string.playback_channel_name)
                .setNotificationId(NOTIFICATION_ID)
                .build()
        )
    }

    private fun resolveMediaItemUri(mediaItem: MediaItem): MediaItem {
        val existingUri = mediaItem.localConfiguration?.uri
            ?: mediaItem.requestMetadata.mediaUri

        if (existingUri != null && existingUri != Uri.EMPTY && existingUri.toString().isNotBlank()) {
            return mediaItem.buildUpon().setUri(existingUri).build()
        }

        val fallbackUrl = Song.resolveSongAudioUrl(null, mediaItem.mediaId)
        val resolvedUri = fallbackUrl.toUri()
        return mediaItem.buildUpon()
            .setUri(resolvedUri)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.playback_channel_name)
            val descriptionText = getString(R.string.playback_channel_description)
            val channel = NotificationChannel(
                CHANNEL_ID,
                name,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = descriptionText
                setShowBadge(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val p = player
        if (p == null || !p.playWhenReady || p.playbackState == Player.STATE_ENDED || p.playbackState == Player.STATE_IDLE) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        player = null
        super.onDestroy()
    }
}

