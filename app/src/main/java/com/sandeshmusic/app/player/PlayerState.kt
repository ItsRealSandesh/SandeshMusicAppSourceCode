package com.sandeshmusic.app.player

import androidx.media3.common.Player
import com.sandeshmusic.app.data.model.Song
import java.util.Locale

data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleModeEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val volumeBoostPercent: Int = 100,
    val bassBoostPercent: Int = 0,
    val userErrorMessage: String? = null
) {
    val progress: Float
        get() = if (durationMs > 0L) {
            (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val formattedPosition: String
        get() = formatTime(currentPositionMs)

    val formattedDuration: String
        get() = formatTime(durationMs)

    companion object {
        fun formatTime(ms: Long): String {
            if (ms <= 0L) return "0:00"
            val totalSeconds = ms / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val hours = minutes / 60
            return if (hours > 0) {
                val remMinutes = minutes % 60
                String.format(Locale.US, "%d:%02d:%02d", hours, remMinutes, seconds)
            } else {
                String.format(Locale.US, "%d:%02d", minutes, seconds)
            }
        }
    }
}
