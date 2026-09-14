package com.sandeshmusic.app.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sandeshmusic.app.data.auth.AuthUser
import com.sandeshmusic.app.data.model.Song
import com.sandeshmusic.app.data.offline.OfflineMusicManager
import com.sandeshmusic.app.data.preferences.ThemeMode
import com.sandeshmusic.app.player.PlayerState
import com.sandeshmusic.app.ui.components.SongRowItem
import com.sandeshmusic.app.ui.theme.CoralPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    favoriteSongs: List<Song>,
    recentPlays: List<Song>,
    downloadedSongs: List<Song> = emptyList(),
    userAudioSongs: List<Song> = emptyList(),
    totalDownloadedBytes: Long = 0L,
    totalAudioBytes: Long = 0L,
    playerState: PlayerState,
    currentUser: AuthUser? = null,
    themeMode: ThemeMode = ThemeMode.DARK,
    onThemeToggleClick: () -> Unit = {},
    onAccountClick: () -> Unit = {},
    onDeveloperSupportClick: () -> Unit = {},
    onSongClick: (Song, List<Song>) -> Unit,
    onSongMenuClick: (Song) -> Unit,
    onDeleteDownload: (String) -> Unit = {},
    onClearHistory: () -> Unit,
    onExploreClick: () -> Unit,
    onNavigateToMyAudio: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "Favourites (${favoriteSongs.size})",
        "Downloads (${downloadedSongs.size})",
        "My Audio (${userAudioSongs.size})",
        "History (${recentPlays.size})"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("library_screen")
    ) {
        // Header with Account Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your Library",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onThemeToggleClick,
                    modifier = Modifier.testTag("library_theme_toggle_btn")
                ) {
                    Icon(
                        imageVector = when (themeMode) {
                            ThemeMode.LIGHT -> Icons.Default.LightMode
                            ThemeMode.DARK -> Icons.Default.DarkMode
                            ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                        },
                        contentDescription = "Switch Theme",
                        tint = CoralPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = onDeveloperSupportClick,
                    modifier = Modifier.testTag("library_developer_support_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.SupportAgent,
                        contentDescription = "Developer Support",
                        tint = CoralPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                IconButton(
                    onClick = onAccountClick,
                    modifier = Modifier.testTag("library_account_btn")
                ) {
                    if (currentUser != null) {
                        Surface(
                            shape = CircleShape,
                            color = CoralPrimary,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = currentUser.email.firstOrNull()?.uppercaseChar()?.toString() ?: "U",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Login to account",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // Tab Selector
        SecondaryTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = CoralPrimary,
            indicator = {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(selectedTabIndex),
                    color = CoralPrimary
                )
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTabIndex == index) CoralPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.testTag("library_tab_$index")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (selectedTabIndex) {
            0 -> {
                // Favourites Tab
                if (favoriteSongs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = CoralPrimary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No favourites yet",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap the heart icon on any track to save it here for instant access.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = onExploreClick,
                                colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary),
                                modifier = Modifier.testTag("explore_songs_btn")
                            ) {
                                Text("Discover Music")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        items(favoriteSongs, key = { "lib_fav_${it.id}" }) { song ->
                            val isCurrent = playerState.currentSong?.id == song.id
                            SongRowItem(
                                song = song,
                                isPlaying = isCurrent && playerState.isPlaying,
                                isCurrent = isCurrent,
                                onClick = { onSongClick(song, favoriteSongs) },
                                onMenuClick = { onSongMenuClick(song) }
                            )
                        }
                    }
                }
            }

            1 -> {
                // Downloads (Device Library) Tab
                if (downloadedSongs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.FileDownload,
                                        contentDescription = null,
                                        tint = CoralPrimary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No downloaded tracks",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Download songs from any playlist or player menu to play offline on your device without internet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = onExploreClick,
                                colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary),
                                modifier = Modifier.testTag("explore_download_btn")
                            ) {
                                Text("Explore Songs")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.DownloadDone,
                                            contentDescription = null,
                                            tint = CoralPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Device Storage • ${OfflineMusicManager.formatFileSize(totalDownloadedBytes)}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    FilledTonalButton(
                                        onClick = { onSongClick(downloadedSongs.first(), downloadedSongs) },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Play All", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }

                        items(downloadedSongs, key = { "lib_down_${it.id}" }) { song ->
                            val isCurrent = playerState.currentSong?.id == song.id
                            SongRowItem(
                                song = song,
                                isPlaying = isCurrent && playerState.isPlaying,
                                isCurrent = isCurrent,
                                onClick = { onSongClick(song, downloadedSongs) },
                                onMenuClick = { onSongMenuClick(song) }
                            )
                        }
                    }
                }
            }

            2 -> {
                // My Audio (Imported local audio files) Tab
                if (userAudioSongs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AudioFile,
                                        contentDescription = null,
                                        tint = CoralPrimary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No personal audio imported",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Import and play your own MP3, WAV, FLAC, and AAC files directly from your device storage.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = onNavigateToMyAudio,
                                colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary),
                                modifier = Modifier.testTag("lib_open_my_audio_btn")
                            ) {
                                Text("Go to My Audio")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AudioFile,
                                            contentDescription = null,
                                            tint = CoralPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Personal Tracks • ${OfflineMusicManager.formatFileSize(totalAudioBytes)}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    FilledTonalButton(
                                        onClick = { onSongClick(userAudioSongs.first(), userAudioSongs) },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Play All")
                                    }
                                }
                            }
                        }

                        items(userAudioSongs, key = { "lib_user_audio_${it.id}" }) { song ->
                            val isCurrent = playerState.currentSong?.id == song.id
                            SongRowItem(
                                song = song,
                                isPlaying = isCurrent && playerState.isPlaying,
                                isCurrent = isCurrent,
                                onClick = { onSongClick(song, userAudioSongs) },
                                onMenuClick = { onSongMenuClick(song) }
                            )
                        }
                    }
                }
            }

            3 -> {
                // Listening History Tab
                if (recentPlays.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No listening history",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Songs you stream will appear here automatically, and the last 2-3 are cached for offline play.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${recentPlays.size} recently played (last 2-3 cached offline)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                TextButton(
                                    onClick = onClearHistory,
                                    colors = ButtonDefaults.textButtonColors(contentColor = CoralPrimary),
                                    modifier = Modifier.testTag("clear_history_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Clear")
                                }
                            }
                        }

                        items(recentPlays, key = { "lib_rec_${it.id}" }) { song ->
                            val isCurrent = playerState.currentSong?.id == song.id
                            SongRowItem(
                                song = song,
                                isPlaying = isCurrent && playerState.isPlaying,
                                isCurrent = isCurrent,
                                onClick = { onSongClick(song, recentPlays) },
                                onMenuClick = { onSongMenuClick(song) }
                            )
                        }
                    }
                }
            }
        }
    }
}
