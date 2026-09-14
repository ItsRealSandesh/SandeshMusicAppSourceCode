package com.sandeshmusic.app.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.sandeshmusic.app.data.model.Song
import com.sandeshmusic.app.player.PlayerState
import com.sandeshmusic.app.ui.components.SongRowItem
import com.sandeshmusic.app.ui.components.VolumeBoosterBottomSheet
import com.sandeshmusic.app.ui.theme.CoralPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playerState: PlayerState,
    isFavorite: Boolean,
    isDownloaded: Boolean = false,
    onCollapse: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onDownloadClick: (Song) -> Unit = {},
    onMenuClick: (Song) -> Unit,
    onPlayFromQueue: (Song, List<Song>) -> Unit,
    onSetVolumeBoost: (Int) -> Unit = {},
    onSetBassBoost: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val song = playerState.currentSong ?: return
    val context = LocalContext.current

    var isDraggingSeek by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    var showQueueSheet by remember { mutableStateOf(false) }
    val queueSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showBoosterSheet by remember { mutableStateOf(false) }
    val boosterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currentSliderValue = if (isDraggingSeek) {
        dragProgress
    } else {
        playerState.progress
    }

    val displayPosition = if (isDraggingSeek) {
        PlayerState.formatTime((dragProgress * playerState.durationMs).toLong())
    } else {
        playerState.formattedPosition
    }

    val surfaceColor = MaterialTheme.colorScheme.surface
    val backgroundColor = MaterialTheme.colorScheme.background

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("now_playing_screen"),
        color = backgroundColor
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            surfaceColor,
                            backgroundColor
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onCollapse,
                        modifier = Modifier.testTag("now_playing_collapse_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Collapse player",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "PLAYING FROM LIBRARY",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "Sandesh Music",
                            style = MaterialTheme.typography.labelMedium,
                            color = CoralPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(
                        onClick = { onMenuClick(song) },
                        modifier = Modifier.testTag("now_playing_menu_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(0.5f))

                // Cover Image Container (Clean, focused 1:1 artwork card with subtle glow)
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 16.dp,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .testTag("now_playing_cover_art")
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(song.coverUrl?.takeIf { it.isNotBlank() } ?: R.drawable.ic_default_cover)
                            .crossfade(true)
                            .error(R.drawable.ic_default_cover)
                            .placeholder(R.drawable.ic_default_cover)
                            .build(),
                        contentDescription = "${song.title} artwork",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.weight(0.5f))

            // Title & Artist + Favorite Heart
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title.ifBlank { "Unknown Title" },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = buildString {
                            append(song.artist.ifBlank { "Sandesh Music" })
                            if (!song.album.isNullOrBlank()) {
                                append(" • ")
                                append(song.album)
                            }
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { onToggleFavorite(song) },
                    modifier = Modifier.testTag("now_playing_fav_btn")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "In favourites" else "Add to favourites",
                        tint = if (isFavorite) CoralPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Seek Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = currentSliderValue,
                    onValueChange = {
                        isDraggingSeek = true
                        dragProgress = it
                    },
                    onValueChangeFinished = {
                        val seekTargetMs = (dragProgress * playerState.durationMs).toLong()
                        onSeekTo(seekTargetMs)
                        isDraggingSeek = false
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = CoralPrimary,
                        activeTrackColor = CoralPrimary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("now_playing_seek_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = displayPosition,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = playerState.formattedDuration,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Media Controls (Shuffle, Previous, Play/Pause, Next, Repeat)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle button
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier.testTag("now_playing_shuffle_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (playerState.shuffleModeEnabled) CoralPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Previous button
                IconButton(
                    onClick = onSkipPrevious,
                    modifier = Modifier.testTag("now_playing_prev_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(38.dp)
                    )
                }

                // Large Play/Pause Button
                FilledIconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(72.dp)
                        .testTag("now_playing_play_pause_btn"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = CoralPrimary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = CircleShape
                ) {
                    if (playerState.isBuffering) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(32.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                // Next button
                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier.testTag("now_playing_next_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(38.dp)
                    )
                }

                // Repeat button
                IconButton(
                    onClick = onCycleRepeat,
                    modifier = Modifier.testTag("now_playing_repeat_btn")
                ) {
                    val repeatIcon = if (playerState.repeatMode == Player.REPEAT_MODE_ONE) {
                        Icons.Default.RepeatOne
                    } else {
                        Icons.Default.Repeat
                    }
                    val isRepeatActive = playerState.repeatMode != Player.REPEAT_MODE_OFF

                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = "Repeat mode",
                        tint = if (isRepeatActive) CoralPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom Actions (Download, Volume Booster, and Queue Sheet Trigger)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onDownloadClick(song) },
                    modifier = Modifier.testTag("now_playing_download_btn")
                ) {
                    Icon(
                        imageVector = if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.FileDownload,
                        contentDescription = if (isDownloaded) "Downloaded to device" else "Download to device",
                        tint = if (isDownloaded) CoralPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Volume Booster Trigger Button
                val isBoostActive = playerState.volumeBoostPercent > 100 || playerState.bassBoostPercent > 0
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isBoostActive) CoralPrimary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { showBoosterSheet = true }
                        .testTag("now_playing_booster_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Volume booster",
                            tint = if (isBoostActive) CoralPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (playerState.volumeBoostPercent > 100) "${playerState.volumeBoostPercent}% BOOST" else "BOOSTER",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isBoostActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = if (isBoostActive) CoralPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = { showQueueSheet = true },
                    modifier = Modifier.testTag("now_playing_queue_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = "Open queue",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

    // Volume Booster Bottom Sheet
    if (showBoosterSheet) {
        VolumeBoosterBottomSheet(
            boostPercent = playerState.volumeBoostPercent,
            bassBoostPercent = playerState.bassBoostPercent,
            onSetVolumeBoost = onSetVolumeBoost,
            onSetBassBoost = onSetBassBoost,
            sheetState = boosterSheetState,
            onDismiss = { showBoosterSheet = false }
        )
    }

    // Queue Bottom Sheet
    if (showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            sheetState = queueSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 6.dp,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Playing Queue",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                if (playerState.queue.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No other songs in queue",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp)
                    ) {
                        itemsIndexed(playerState.queue, key = { index, item -> "${item.id}_$index" }) { index, item ->
                            val isCurrent = item.id == song.id
                            SongRowItem(
                                song = item,
                                isPlaying = isCurrent && playerState.isPlaying,
                                isCurrent = isCurrent,
                                onClick = {
                                    onPlayFromQueue(item, playerState.queue)
                                },
                                onMenuClick = { onMenuClick(item) },
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
