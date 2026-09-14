package com.sandeshmusic.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileDownloadDone
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.sandeshmusic.app.data.model.Song
import com.sandeshmusic.app.ui.theme.CoralPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongMenuBottomSheet(
    song: Song,
    isFavorite: Boolean,
    isDownloaded: Boolean = false,
    isDownloading: Boolean = false,
    isUserAudio: Boolean = false,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleDownload: () -> Unit = {},
    onDeleteUserAudio: () -> Unit = {},
    onShowSongInfo: () -> Unit
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 6.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = Modifier.testTag("song_menu_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Header with Song Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(68.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(song.coverUrl?.takeIf { it.isNotBlank() } ?: R.drawable.ic_default_cover)
                            .crossfade(true)
                            .error(R.drawable.ic_default_cover)
                            .placeholder(R.drawable.ic_default_cover)
                            .build(),
                        contentDescription = "Cover art",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title.ifBlank { "Untitled" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = song.artist.ifBlank { "Sandesh Music" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outline
            )

            // Actions
            MenuActionItem(
                icon = Icons.Default.PlayArrow,
                title = "Play",
                onClick = {
                    onPlay()
                    onDismiss()
                }
            )

            MenuActionItem(
                icon = Icons.Default.PlaylistAdd,
                title = "Play Next",
                onClick = {
                    onPlayNext()
                    onDismiss()
                }
            )

            MenuActionItem(
                icon = Icons.Default.QueueMusic,
                title = "Add to Queue",
                onClick = {
                    onAddToQueue()
                    onDismiss()
                }
            )

            MenuActionItem(
                icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                title = if (isFavorite) "Remove from Favourites" else "Add to Favourites",
                iconTint = if (isFavorite) CoralPrimary else MaterialTheme.colorScheme.onSurface,
                onClick = {
                    onToggleFavorite()
                    onDismiss()
                }
            )

            if (isUserAudio) {
                MenuActionItem(
                    icon = Icons.Default.DeleteOutline,
                    title = "Delete from My Audio",
                    iconTint = MaterialTheme.colorScheme.error,
                    onClick = {
                        onDeleteUserAudio()
                        onDismiss()
                    }
                )
            } else {
                MenuActionItem(
                    icon = if (isDownloaded) Icons.Default.FileDownloadDone else Icons.Default.Download,
                    title = when {
                        isDownloading -> "Downloading to Device..."
                        isDownloaded -> "Delete from Device"
                        else -> "Download to Device"
                    },
                    iconTint = if (isDownloaded) CoralPrimary else MaterialTheme.colorScheme.onSurface,
                    onClick = {
                        onToggleDownload()
                        onDismiss()
                    }
                )
            }

            MenuActionItem(
                icon = Icons.Default.Info,
                title = "Song Info",
                onClick = {
                    onShowSongInfo()
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun MenuActionItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(18.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
