package com.sandeshmusic.app.ui.myaudio

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sandeshmusic.app.data.auth.AuthUser
import com.sandeshmusic.app.data.model.Song
import com.sandeshmusic.app.data.offline.OfflineMusicManager
import com.sandeshmusic.app.data.preferences.ThemeMode
import com.sandeshmusic.app.player.PlayerState
import com.sandeshmusic.app.ui.components.SongRowItem
import com.sandeshmusic.app.ui.theme.CoralPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAudioScreen(
    userAudioSongs: List<Song>,
    totalAudioBytes: Long,
    playerState: PlayerState,
    currentUser: AuthUser? = null,
    themeMode: ThemeMode = ThemeMode.DARK,
    onThemeToggleClick: () -> Unit = {},
    onAccountClick: () -> Unit = {},
    onDeveloperSupportClick: () -> Unit = {},
    onSongClick: (Song, List<Song>) -> Unit,
    onSongMenuClick: (Song) -> Unit,
    onImportAudio: (Uri, String?, String?, (Result<Song>) -> Unit) -> Unit,
    onDeleteAudio: (String) -> Unit,
    onShufflePlay: (List<Song>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isImporting by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var customTitle by remember { mutableStateOf("") }
    var customArtist by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var deleteCandidateSong by remember { mutableStateOf<Song?>(null) }

    // Audio file picker launcher (supports audio/* files from device storage, files app, Google Drive)
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            customTitle = ""
            customArtist = ""
            showImportDialog = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("my_audio_screen")
    ) {
        // Header with Theme Toggle, Developer Support and Account Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "My Audio",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (userAudioSongs.isEmpty()) "Import audio files from device" else "${userAudioSongs.size} tracks • ${OfflineMusicManager.formatFileSize(totalAudioBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Theme Toggle
                IconButton(
                    onClick = onThemeToggleClick,
                    modifier = Modifier.testTag("my_audio_theme_toggle_btn")
                ) {
                    val (icon, desc) = when (themeMode) {
                        ThemeMode.DARK -> Pair(Icons.Default.DarkMode, "Dark theme active")
                        ThemeMode.LIGHT -> Pair(Icons.Default.LightMode, "Light theme active")
                        ThemeMode.SYSTEM -> Pair(Icons.Default.BrightnessAuto, "System theme active")
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = desc,
                        tint = CoralPrimary
                    )
                }

                // Developer Support Button
                IconButton(
                    onClick = onDeveloperSupportClick,
                    modifier = Modifier.testTag("my_audio_dev_support_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.SupportAgent,
                        contentDescription = "Developer Support",
                        tint = CoralPrimary
                    )
                }

                // Account Avatar/Login Button
                IconButton(
                    onClick = onAccountClick,
                    modifier = Modifier.testTag("my_audio_account_btn")
                ) {
                    if (currentUser != null) {
                        Surface(
                            shape = CircleShape,
                            color = CoralPrimary,
                            modifier = Modifier.size(32.dp)
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

        // Status banner if any
        AnimatedVisibility(visible = statusMessage != null) {
            statusMessage?.let { msg ->
                Surface(
                    color = CoralPrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = CoralPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { statusMessage = null },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("OK", style = MaterialTheme.typography.labelSmall, color = CoralPrimary)
                        }
                    }
                }
            }
        }

        if (userAudioSongs.isEmpty() && !isImporting) {
            // Empty State
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
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AudioFile,
                                contentDescription = null,
                                tint = CoralPrimary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "No personal audio added",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add and listen to your own MP3, WAV, AAC, M4A, or FLAC audio files directly within Sandesh Music.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { audioPickerLauncher.launch("audio/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("add_first_audio_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Audio File")
                    }
                }
            }
        } else {
            // Tracks List with Actions Bar
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 88.dp)
            ) {
                // Header Actions Bar (Play All, Shuffle, Add More)
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = {
                                        if (userAudioSongs.isNotEmpty()) {
                                            onSongClick(userAudioSongs.first(), userAudioSongs)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    modifier = Modifier.testTag("my_audio_play_all_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Play All", style = MaterialTheme.typography.labelMedium)
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                FilledTonalButton(
                                    onClick = {
                                        if (userAudioSongs.isNotEmpty()) {
                                            onShufflePlay(userAudioSongs)
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier.testTag("my_audio_shuffle_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shuffle,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Shuffle", style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            FilledTonalButton(
                                onClick = { audioPickerLauncher.launch("audio/*") },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("my_audio_add_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                items(userAudioSongs, key = { "user_audio_${it.id}" }) { song ->
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

    // Import Metadata Confirmation / Customization Dialog
    if (showImportDialog && selectedUri != null) {
        val uri = selectedUri!!
        AlertDialog(
            onDismissRequest = {
                if (!isImporting) {
                    showImportDialog = false
                    selectedUri = null
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AudioFile,
                        contentDescription = null,
                        tint = CoralPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Audio File", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Customize song details or keep default file tags:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = customTitle,
                        onValueChange = { customTitle = it },
                        label = { Text("Track Title (optional)") },
                        placeholder = { Text("Auto-detect from file") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_title_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = customArtist,
                        onValueChange = { customArtist = it },
                        label = { Text("Artist Name (optional)") },
                        placeholder = { Text("e.g., Sandesh, Local Audio") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_artist_input")
                    )
                    if (isImporting) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = CoralPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Importing and extracting tags...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isImporting = true
                        onImportAudio(
                            uri,
                            customTitle.takeIf { it.isNotBlank() },
                            customArtist.takeIf { it.isNotBlank() }
                        ) { result ->
                            isImporting = false
                            showImportDialog = false
                            selectedUri = null
                            result.onSuccess { importedSong ->
                                statusMessage = "Added \"${importedSong.title}\" to My Audio"
                            }.onFailure { err ->
                                statusMessage = "Failed to import: ${err.message}"
                            }
                        }
                    },
                    enabled = !isImporting,
                    colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary),
                    modifier = Modifier.testTag("confirm_import_btn")
                ) {
                    Text("Import to App")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImportDialog = false
                        selectedUri = null
                    },
                    enabled = !isImporting
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete confirmation dialog
    deleteCandidateSong?.let { song ->
        AlertDialog(
            onDismissRequest = { deleteCandidateSong = null },
            title = { Text("Delete Track?") },
            text = { Text("Are you sure you want to remove \"${song.title}\" from your personal audio library?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAudio(song.id)
                        deleteCandidateSong = null
                        statusMessage = "Removed \"${song.title}\""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidateSong = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
