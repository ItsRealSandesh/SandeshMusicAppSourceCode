package com.sandeshmusic.app.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.sandeshmusic.app.data.model.Song
import com.sandeshmusic.app.ui.auth.AuthBottomSheet
import com.sandeshmusic.app.ui.auth.AuthGateScreen
import com.sandeshmusic.app.ui.components.DeveloperSupportDialog
import com.sandeshmusic.app.ui.components.MiniPlayer
import com.sandeshmusic.app.ui.components.NavigationDestination
import com.sandeshmusic.app.ui.components.SandeshBottomNavigation
import com.sandeshmusic.app.ui.components.SongInfoDialog
import com.sandeshmusic.app.ui.components.SongMenuBottomSheet
import com.sandeshmusic.app.ui.home.HomeScreen
import com.sandeshmusic.app.ui.library.LibraryScreen
import com.sandeshmusic.app.ui.player.NowPlayingScreen
import com.sandeshmusic.app.ui.search.SearchScreen
import com.sandeshmusic.app.viewmodel.AuthViewModel
import com.sandeshmusic.app.viewmodel.MusicViewModel
import com.sandeshmusic.app.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    musicViewModel: MusicViewModel,
    playerViewModel: PlayerViewModel,
    authViewModel: AuthViewModel
) {
    val uiState by musicViewModel.uiState.collectAsState()
    val isRefreshing by musicViewModel.isRefreshing.collectAsState()
    val searchQuery by musicViewModel.searchQuery.collectAsState()
    val filteredSongs by musicViewModel.filteredSongs.collectAsState()
    val cachedSongs by musicViewModel.cachedSongs.collectAsState()
    val favorites by musicViewModel.favorites.collectAsState()
    val favoriteSongs by musicViewModel.favoriteSongs.collectAsState()
    val recentPlays by musicViewModel.recentPlays.collectAsState()
    val downloadedSongs by musicViewModel.downloadedSongs.collectAsState()
    val totalDownloadedBytes by musicViewModel.totalDownloadedBytes.collectAsState()

    val currentUser by authViewModel.currentUser.collectAsState()
    var showAuthSheet by remember { mutableStateOf(false) }
    var showDeveloperSupportDialog by remember { mutableStateOf(false) }
    var dismissedAuthGate by remember { mutableStateOf(false) }

    val playerState by playerViewModel.playerState.collectAsState()
    val isNowPlayingExpanded by playerViewModel.isNowPlayingExpanded.collectAsState()

    var currentDestination by remember { mutableStateOf(NavigationDestination.HOME) }
    var selectedMenuSong by remember { mutableStateOf<Song?>(null) }
    var infoDialogSong by remember { mutableStateOf<Song?>(null) }

    val songMenuSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Show errors in snackbar
    LaunchedEffect(playerState.userErrorMessage) {
        playerState.userErrorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            playerViewModel.clearError()
        }
    }

    // Force register/login on app open if not logged in
    val shouldShowAuthGate = currentUser == null && !dismissedAuthGate

    if (shouldShowAuthGate) {
        AuthGateScreen(
            authViewModel = authViewModel,
            onContinueOffline = {
                dismissedAuthGate = true
            }
        )
        return
    }

    // Intercept back button if Now Playing is expanded
    BackHandler(enabled = isNowPlayingExpanded) {
        playerViewModel.collapseNowPlaying()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                Column {
                    // Persistent Mini Player above bottom navigation
                    if (playerState.currentSong != null && !isNowPlayingExpanded) {
                        MiniPlayer(
                            playerState = playerState,
                            onClick = { playerViewModel.expandNowPlaying() },
                            onPlayPauseClick = { playerViewModel.playPause() },
                            onNextClick = { playerViewModel.skipToNext() }
                        )
                    }

                    SandeshBottomNavigation(
                        currentDestination = currentDestination,
                        onNavigate = { destination -> currentDestination = destination }
                    )
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("main_scaffold")
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentDestination) {
                    NavigationDestination.HOME -> {
                        HomeScreen(
                            uiState = uiState,
                            isRefreshing = isRefreshing,
                            recentPlays = recentPlays,
                            favoriteSongs = favoriteSongs,
                            playerState = playerState,
                            currentUser = currentUser,
                            onRefresh = { musicViewModel.fetchSongs() },
                            onAccountClick = { showAuthSheet = true },
                            onDeveloperSupportClick = { showDeveloperSupportDialog = true },
                            onSongClick = { song, list ->
                                playerViewModel.playSong(song, list)
                            },
                            onSongMenuClick = { song ->
                                selectedMenuSong = song
                            },
                            onShuffleAll = { songs ->
                                val shuffled = songs.shuffled()
                                shuffled.firstOrNull()?.let { first ->
                                    playerViewModel.playSong(first, shuffled)
                                    if (!playerState.shuffleModeEnabled) {
                                        playerViewModel.toggleShuffle()
                                    }
                                }
                            }
                        )
                    }

                    NavigationDestination.SEARCH -> {
                        SearchScreen(
                            searchQuery = searchQuery,
                            filteredSongs = filteredSongs,
                            allSongs = cachedSongs,
                            playerState = playerState,
                            onQueryChange = { musicViewModel.updateSearchQuery(it) },
                            onSongClick = { song, list ->
                                playerViewModel.playSong(song, list)
                            },
                            onSongMenuClick = { song ->
                                selectedMenuSong = song
                            }
                        )
                    }

                    NavigationDestination.LIBRARY -> {
                        LibraryScreen(
                            favoriteSongs = favoriteSongs,
                            recentPlays = recentPlays,
                            downloadedSongs = downloadedSongs,
                            totalDownloadedBytes = totalDownloadedBytes,
                            playerState = playerState,
                            currentUser = currentUser,
                            onAccountClick = { showAuthSheet = true },
                            onDeveloperSupportClick = { showDeveloperSupportDialog = true },
                            onSongClick = { song, list ->
                                playerViewModel.playSong(song, list)
                            },
                            onSongMenuClick = { song ->
                                selectedMenuSong = song
                            },
                            onDeleteDownload = { songId ->
                                musicViewModel.deleteDownload(songId)
                            },
                            onClearHistory = { musicViewModel.clearRecentPlays() },
                            onExploreClick = { currentDestination = NavigationDestination.HOME }
                        )
                    }
                }
            }
        }

        // Full Screen Now Playing Screen with animated slide transition
        AnimatedVisibility(
            visible = isNowPlayingExpanded && playerState.currentSong != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            val currentSong = playerState.currentSong
            if (currentSong != null) {
                val isDownloaded = downloadedSongs.any { it.id == currentSong.id }
                NowPlayingScreen(
                    playerState = playerState,
                    isFavorite = favorites.contains(currentSong.id),
                    isDownloaded = isDownloaded,
                    onCollapse = { playerViewModel.collapseNowPlaying() },
                    onPlayPause = { playerViewModel.playPause() },
                    onSeekTo = { playerViewModel.seekTo(it) },
                    onSkipNext = { playerViewModel.skipToNext() },
                    onSkipPrevious = { playerViewModel.skipToPrevious() },
                    onToggleShuffle = { playerViewModel.toggleShuffle() },
                    onCycleRepeat = { playerViewModel.cycleRepeatMode() },
                    onToggleFavorite = { musicViewModel.toggleFavorite(it) },
                    onDownloadClick = { song ->
                        if (isDownloaded) {
                            musicViewModel.deleteDownload(song.id)
                            scope.launch {
                                snackbarHostState.showSnackbar("Removed \"${song.title}\" from device")
                            }
                        } else {
                            musicViewModel.downloadSong(song)
                            scope.launch {
                                snackbarHostState.showSnackbar("Downloading \"${song.title}\" for offline play")
                            }
                        }
                    },
                    onMenuClick = { song -> selectedMenuSong = song },
                    onPlayFromQueue = { song, queue ->
                        playerViewModel.playSong(song, queue)
                    }
                )
            }
        }

        // Song Context Menu Bottom Sheet
        selectedMenuSong?.let { song ->
            val isFav = favorites.contains(song.id)
            val isDownloaded = downloadedSongs.any { it.id == song.id }
            SongMenuBottomSheet(
                song = song,
                isFavorite = isFav,
                isDownloaded = isDownloaded,
                sheetState = songMenuSheetState,
                onDismiss = { selectedMenuSong = null },
                onPlay = {
                    playerViewModel.playSong(song, listOf(song))
                },
                onPlayNext = {
                    playerViewModel.playNext(song)
                    scope.launch {
                        snackbarHostState.showSnackbar("Playing \"${song.title}\" next")
                    }
                },
                onAddToQueue = {
                    playerViewModel.addToQueue(song)
                    scope.launch {
                        snackbarHostState.showSnackbar("Added \"${song.title}\" to queue")
                    }
                },
                onToggleFavorite = {
                    musicViewModel.toggleFavorite(song)
                },
                onToggleDownload = {
                    if (isDownloaded) {
                        musicViewModel.deleteDownload(song.id)
                        scope.launch {
                            snackbarHostState.showSnackbar("Removed \"${song.title}\" from device")
                        }
                    } else {
                        musicViewModel.downloadSong(song)
                        scope.launch {
                            snackbarHostState.showSnackbar("Downloading \"${song.title}\" for offline play")
                        }
                    }
                },
                onShowSongInfo = {
                    infoDialogSong = song
                }
            )
        }

        // Song Information Dialog
        infoDialogSong?.let { song ->
            SongInfoDialog(
                song = song,
                onDismiss = { infoDialogSong = null }
            )
        }

        // Email Authentication & Cloud Favorites Sync Sheet
        if (showAuthSheet) {
            AuthBottomSheet(
                authViewModel = authViewModel,
                onDismiss = { showAuthSheet = false },
                onDeveloperSupportClick = {
                    showDeveloperSupportDialog = true
                }
            )
        }

        // Developer Support Dialog
        if (showDeveloperSupportDialog) {
            DeveloperSupportDialog(
                onDismiss = { showDeveloperSupportDialog = false }
            )
        }
    }
}
