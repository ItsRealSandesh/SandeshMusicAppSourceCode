package com.sandeshmusic.app.player

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.media3.session.SessionCommand
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.sandeshmusic.app.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerController(
    private val context: Context,
    private val onSongPlayed: (Song) -> Unit = {},
    private val uriResolver: ((Song) -> android.net.Uri?)? = null
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var tickerJob: Job? = null

    private val prefs by lazy {
        context.getSharedPreferences("player_audio_settings", Context.MODE_PRIVATE)
    }

    private val _playerState = MutableStateFlow(
        PlayerState(
            volumeBoostPercent = prefs.getInt("key_volume_boost", 100),
            bassBoostPercent = prefs.getInt("key_bass_boost", 0)
        )
    )
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var currentSongMap = mutableMapOf<String, Song>()
    private var pendingAction: ((MediaController) -> Unit)? = null

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val song = mediaItem?.toSong()
            val index = mediaController?.currentMediaItemIndex ?: -1
            _playerState.update {
                it.copy(
                    currentSong = song,
                    currentIndex = index,
                    currentPositionMs = mediaController?.currentPosition ?: 0L,
                    durationMs = mediaController?.duration?.coerceAtLeast(0L) ?: 0L
                )
            }
            if (song != null) {
                onSongPlayed(song)
            }
        }

        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
            mediaController?.let { controller ->
                if (controller.mediaItemCount > 0) {
                    val queueList = mutableListOf<Song>()
                    for (i in 0 until controller.mediaItemCount) {
                        queueList.add(controller.getMediaItemAt(i).toSong())
                    }
                    _playerState.update {
                        it.copy(
                            queue = queueList,
                            currentIndex = controller.currentMediaItemIndex,
                            durationMs = controller.duration.coerceAtLeast(0L)
                        )
                    }
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playerState.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) {
                startPositionTicker()
            } else {
                stopPositionTicker()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val isBuffering = playbackState == Player.STATE_BUFFERING
            val duration = mediaController?.duration?.coerceAtLeast(0L) ?: 0L
            _playerState.update {
                it.copy(
                    isBuffering = isBuffering,
                    durationMs = duration,
                    currentPositionMs = mediaController?.currentPosition ?: 0L
                )
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _playerState.update { it.copy(shuffleModeEnabled = shuffleModeEnabled) }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _playerState.update { it.copy(repeatMode = repeatMode) }
        }

        override fun onPlayerError(error: PlaybackException) {
            _playerState.update {
                it.copy(
                    isPlaying = false,
                    isBuffering = false,
                    userErrorMessage = "Couldn't play this song: ${error.localizedMessage ?: "Playback error"}"
                )
            }
        }
    }

    init {
        initController()
    }

    private fun initController() {
        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        this.controllerFuture = future

        future.addListener({
            try {
                val controller = future.get()
                this.mediaController = controller
                controller.addListener(playerListener)
                syncStateFromController(controller)
                applyAudioSettingsToController(controller)
                pendingAction?.invoke(controller)
                pendingAction = null
            } catch (e: Exception) {
                _playerState.update {
                    it.copy(userErrorMessage = "Failed to connect to audio service: ${e.message}")
                }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun syncStateFromController(controller: MediaController) {
        val currentMediaItem = controller.currentMediaItem
        val currentSong = currentMediaItem?.toSong()

        val queueList = mutableListOf<Song>()
        for (i in 0 until controller.mediaItemCount) {
            queueList.add(controller.getMediaItemAt(i).toSong())
        }

        _playerState.update {
            it.copy(
                currentSong = currentSong,
                queue = if (queueList.isNotEmpty()) queueList else it.queue,
                isPlaying = controller.isPlaying,
                isBuffering = controller.playbackState == Player.STATE_BUFFERING,
                currentPositionMs = controller.currentPosition.coerceAtLeast(0L),
                durationMs = controller.duration.coerceAtLeast(0L),
                shuffleModeEnabled = controller.shuffleModeEnabled,
                repeatMode = controller.repeatMode,
                currentIndex = controller.currentMediaItemIndex
            )
        }
        if (controller.isPlaying) {
            startPositionTicker()
        }
    }

    fun playSong(song: Song, playlist: List<Song> = listOf(song)) {
        val action: (MediaController) -> Unit = { controller ->
            playlist.forEach { currentSongMap[it.id] = it }

            val targetIndex = playlist.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            val mediaItems = playlist.map { it.toMediaItem() }

            controller.setMediaItems(mediaItems, targetIndex, 0L)
            controller.prepare()
            controller.play()

            _playerState.update {
                it.copy(
                    currentSong = song,
                    queue = playlist,
                    currentIndex = targetIndex,
                    isPlaying = true,
                    userErrorMessage = null
                )
            }
            onSongPlayed(song)
        }

        val controller = mediaController
        if (controller != null) {
            action(controller)
        } else {
            pendingAction = action
        }
    }

    fun playPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            if (controller.playbackState == Player.STATE_IDLE || controller.playbackState == Player.STATE_ENDED) {
                controller.prepare()
            }
            controller.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val controller = mediaController ?: return
        val validPos = positionMs.coerceIn(0L, controller.duration.coerceAtLeast(0L))
        controller.seekTo(validPos)
        _playerState.update { it.copy(currentPositionMs = validPos) }
    }

    fun skipToNext() {
        val controller = mediaController ?: return
        if (controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
        }
    }

    fun skipToPrevious() {
        val controller = mediaController ?: return
        if (controller.currentPosition > 3000L) {
            controller.seekTo(0L)
        } else if (controller.hasPreviousMediaItem()) {
            controller.seekToPreviousMediaItem()
        } else {
            controller.seekTo(0L)
        }
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        val nextMode = !controller.shuffleModeEnabled
        controller.shuffleModeEnabled = nextMode
        _playerState.update { it.copy(shuffleModeEnabled = nextMode) }
    }

    fun cycleRepeatMode() {
        val controller = mediaController ?: return
        val nextMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        controller.repeatMode = nextMode
        _playerState.update { it.copy(repeatMode = nextMode) }
    }

    fun playNext(song: Song) {
        val controller = mediaController ?: return
        currentSongMap[song.id] = song
        val currentIndex = controller.currentMediaItemIndex
        val insertIndex = if (currentIndex >= 0) currentIndex + 1 else 0
        controller.addMediaItem(insertIndex, song.toMediaItem())

        val currentQueue = _playerState.value.queue.toMutableList()
        val queueInsertIndex = (currentIndex + 1).coerceAtMost(currentQueue.size)
        currentQueue.add(queueInsertIndex, song)
        _playerState.update { it.copy(queue = currentQueue) }
    }

    fun addToQueue(song: Song) {
        val controller = mediaController ?: return
        currentSongMap[song.id] = song
        controller.addMediaItem(song.toMediaItem())

        val currentQueue = _playerState.value.queue.toMutableList()
        currentQueue.add(song)
        _playerState.update { it.copy(queue = currentQueue) }
    }

    fun clearError() {
        _playerState.update { it.copy(userErrorMessage = null) }
    }

    fun setVolumeBoost(percent: Int) {
        val clamped = percent.coerceIn(100, 250)
        _playerState.update { it.copy(volumeBoostPercent = clamped) }
        prefs.edit().putInt("key_volume_boost", clamped).apply()
        sendVolumeBoostCommand(clamped)
    }

    fun setBassBoost(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        _playerState.update { it.copy(bassBoostPercent = clamped) }
        prefs.edit().putInt("key_bass_boost", clamped).apply()
        sendBassBoostCommand(clamped)
    }

    private fun applyAudioSettingsToController(controller: MediaController) {
        val boost = _playerState.value.volumeBoostPercent
        val bass = _playerState.value.bassBoostPercent
        val boostBundle = Bundle().apply { putInt(MusicService.KEY_BOOST_PERCENT, boost) }
        controller.sendCustomCommand(
            SessionCommand(MusicService.ACTION_SET_VOLUME_BOOST, Bundle.EMPTY),
            boostBundle
        )
        val bassBundle = Bundle().apply { putInt(MusicService.KEY_BASS_STRENGTH, bass) }
        controller.sendCustomCommand(
            SessionCommand(MusicService.ACTION_SET_BASS_BOOST, Bundle.EMPTY),
            bassBundle
        )
    }

    private fun sendVolumeBoostCommand(boostPercent: Int) {
        val controller = mediaController ?: return
        val bundle = Bundle().apply { putInt(MusicService.KEY_BOOST_PERCENT, boostPercent) }
        controller.sendCustomCommand(
            SessionCommand(MusicService.ACTION_SET_VOLUME_BOOST, Bundle.EMPTY),
            bundle
        )
    }

    private fun sendBassBoostCommand(bassStrength: Int) {
        val controller = mediaController ?: return
        val bundle = Bundle().apply { putInt(MusicService.KEY_BASS_STRENGTH, bassStrength) }
        controller.sendCustomCommand(
            SessionCommand(MusicService.ACTION_SET_BASS_BOOST, Bundle.EMPTY),
            bundle
        )
    }

    private fun startPositionTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                mediaController?.let { controller ->
                    if (controller.isPlaying) {
                        _playerState.update {
                            it.copy(
                                currentPositionMs = controller.currentPosition.coerceAtLeast(0L),
                                durationMs = controller.duration.coerceAtLeast(0L)
                            )
                        }
                    }
                }
                delay(250)
            }
        }
    }

    private fun stopPositionTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    fun release() {
        stopPositionTicker()
        mediaController?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
        controllerFuture = null
    }

    private fun Song.toMediaItem(): MediaItem {
        val resolved = this.withResolvedCovers()
        val metadata = MediaMetadata.Builder()
            .setTitle(resolved.title)
            .setArtist(resolved.artist)
            .setAlbumTitle(resolved.album ?: "")
            .setArtworkUri(resolved.coverUrl?.takeIf { it.isNotBlank() }?.toUri())
            .build()

        val playableUri = uriResolver?.invoke(resolved)
            ?: resolved.audioUrl.takeIf { it.isNotBlank() }?.toUri()
            ?: android.net.Uri.EMPTY

        val requestMetadata = MediaItem.RequestMetadata.Builder()
            .setMediaUri(playableUri)
            .build()

        return MediaItem.Builder()
            .setMediaId(resolved.id)
            .setUri(playableUri)
            .setRequestMetadata(requestMetadata)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun MediaItem.toSong(): Song {
        val id = this.mediaId
        val cached = currentSongMap[id]
        if (cached != null) return cached.withResolvedCovers()

        val uriStr = this.requestMetadata.mediaUri?.toString()
            ?: this.localConfiguration?.uri?.toString()
            ?: ""

        val song = Song(
            id = id,
            title = mediaMetadata.title?.toString()?.takeIf { it.isNotBlank() } ?: "Unknown Track",
            artist = mediaMetadata.artist?.toString()?.takeIf { it.isNotBlank() } ?: "Unknown Artist",
            album = mediaMetadata.albumTitle?.toString(),
            audioUrl = uriStr,
            coverUrl = mediaMetadata.artworkUri?.toString()
        ).withResolvedCovers()
        currentSongMap[id] = song
        return song
    }
}
