package com.sandeshmusic.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sandeshmusic.app.data.auth.AuthUser
import com.sandeshmusic.app.data.model.Song
import com.sandeshmusic.app.player.PlayerState
import com.sandeshmusic.app.data.preferences.ThemeMode
import com.sandeshmusic.app.ui.components.SongCardHorizontal
import com.sandeshmusic.app.ui.components.SongRowItem
import com.sandeshmusic.app.ui.theme.CoralPrimary
import com.sandeshmusic.app.viewmodel.MusicUiState
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: MusicUiState,
    isRefreshing: Boolean,
    recentPlays: List<Song>,
    favoriteSongs: List<Song>,
    playerState: PlayerState,
    currentUser: AuthUser? = null,
    themeMode: ThemeMode = ThemeMode.DARK,
    onThemeToggleClick: () -> Unit = {},
    onRefresh: () -> Unit,
    onAccountClick: () -> Unit = {},
    onDeveloperSupportClick: () -> Unit = {},
    onSongClick: (Song, List<Song>) -> Unit,
    onSongMenuClick: (Song) -> Unit,
    onShuffleAll: (List<Song>) -> Unit,
    modifier: Modifier = Modifier
) {
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..21 -> "Good Evening"
            else -> "Good Night"
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen")
    ) {
        when (uiState) {
            is MusicUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = CoralPrimary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading Sandesh Music...",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is MusicUiState.Error -> {
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
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = "Offline",
                                    tint = CoralPrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Couldn't load your music.",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Check your internet connection and try again.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onRefresh,
                            colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary),
                            modifier = Modifier.testTag("retry_button")
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry")
                        }
                    }
                }
            }

            is MusicUiState.Success -> {
                val rawSongs = uiState.songs
                var sortMode by remember { mutableStateOf<SongSortOrder>(SongSortOrder.RANDOM) }
                var randomSeed by remember { mutableStateOf(0) }

                // Display songs randomized by default so the same song doesn't always appear at the top
                val displaySongs = remember(rawSongs, sortMode, randomSeed) {
                    when (sortMode) {
                        SongSortOrder.RANDOM -> rawSongs.shuffled()
                        SongSortOrder.ORIGINAL -> rawSongs
                        SongSortOrder.TITLE -> rawSongs.sortedBy { it.title.lowercase() }
                        SongSortOrder.ARTIST -> rawSongs.sortedBy { it.artist.lowercase() }
                    }
                }

                if (rawSongs.isEmpty()) {
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
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = "No songs",
                                        tint = CoralPrimary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No songs available yet",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Add songs to songs.json or tap refresh to check for updates.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = onRefresh,
                                colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary),
                                modifier = Modifier.testTag("empty_refresh_button")
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Refresh Library")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        // Header
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = greeting,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Sandesh Music",
                                            style = MaterialTheme.typography.headlineLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = onThemeToggleClick,
                                            modifier = Modifier.testTag("home_theme_toggle_btn")
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
                                            modifier = Modifier.testTag("home_developer_support_btn")
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
                                            modifier = Modifier.testTag("home_account_btn")
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

                                        IconButton(
                                            onClick = onRefresh,
                                            modifier = Modifier.testTag("home_refresh_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Refresh library",
                                                tint = CoralPrimary
                                            )
                                        }
                                    }
                                }

                                AnimatedVisibility(visible = uiState.isOffline) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 10.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudOff,
                                            contentDescription = "Offline indicator",
                                            tint = CoralPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Offline Mode — displaying cached library",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Recently Played Horizontal Carousel (if any)
                        if (recentPlays.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Recently Played",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(recentPlays, key = { "recent_${it.id}" }) { song ->
                                            val isCurrent = playerState.currentSong?.id == song.id
                                            SongCardHorizontal(
                                                song = song,
                                                isCurrent = isCurrent,
                                                isPlaying = isCurrent && playerState.isPlaying,
                                                onClick = { onSongClick(song, recentPlays) },
                                                onMenuClick = { onSongMenuClick(song) }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }

                        // Favorites Horizontal Carousel (if any)
                        if (favoriteSongs.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Your Favourites",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(favoriteSongs, key = { "fav_${it.id}" }) { song ->
                                            val isCurrent = playerState.currentSong?.id == song.id
                                            SongCardHorizontal(
                                                song = song,
                                                isCurrent = isCurrent,
                                                isPlaying = isCurrent && playerState.isPlaying,
                                                onClick = { onSongClick(song, favoriteSongs) },
                                                onMenuClick = { onSongMenuClick(song) }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }

                        // All Songs Header with Shuffle & Order controls
                        item {
                            var showSortMenu by remember { mutableStateOf(false) }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "All Songs",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${displaySongs.size} ${if (displaySongs.size == 1) "track" else "tracks"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Randomize / Reshuffle order button
                                        IconButton(
                                            onClick = {
                                                sortMode = SongSortOrder.RANDOM
                                                randomSeed++
                                            },
                                            modifier = Modifier.testTag("reshuffle_order_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Casino,
                                                contentDescription = "Randomize order",
                                                tint = if (sortMode == SongSortOrder.RANDOM) CoralPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        // Sort menu button
                                        Box {
                                            IconButton(
                                                onClick = { showSortMenu = true },
                                                modifier = Modifier.testTag("sort_menu_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.SortByAlpha,
                                                    contentDescription = "Sort order",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            DropdownMenu(
                                                expanded = showSortMenu,
                                                onDismissRequest = { showSortMenu = false }
                                            ) {
                                                SongSortOrder.entries.forEach { order ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Text(
                                                                text = order.label,
                                                                color = if (sortMode == order) CoralPrimary else MaterialTheme.colorScheme.onSurface,
                                                                fontWeight = if (sortMode == order) FontWeight.Bold else FontWeight.Normal
                                                            )
                                                        },
                                                        onClick = {
                                                            if (order == SongSortOrder.RANDOM) {
                                                                randomSeed++
                                                            }
                                                            sortMode = order
                                                            showSortMenu = false
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        // Quick Shuffle & Play button
                                        FilledTonalButton(
                                            onClick = { onShuffleAll(displaySongs) },
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = CoralPrimary
                                            ),
                                            modifier = Modifier.testTag("shuffle_all_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Shuffle,
                                                contentDescription = "Shuffle and Play",
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Shuffle")
                                        }
                                    }
                                }
                            }
                        }

                        // Vertically scrolling song list
                        items(displaySongs, key = { it.id }) { song ->
                            val isCurrent = playerState.currentSong?.id == song.id
                            SongRowItem(
                                song = song,
                                isPlaying = isCurrent && playerState.isPlaying,
                                isCurrent = isCurrent,
                                onClick = { onSongClick(song, displaySongs) },
                                onMenuClick = { onSongMenuClick(song) },
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
