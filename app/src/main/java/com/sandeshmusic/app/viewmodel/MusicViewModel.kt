package com.sandeshmusic.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sandeshmusic.app.data.model.Song
import com.sandeshmusic.app.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface MusicUiState {
    data object Loading : MusicUiState
    data class Success(val songs: List<Song>, val isOffline: Boolean = false) : MusicUiState
    data class Error(val message: String) : MusicUiState
}

class MusicViewModel(
    private val repository: MusicRepository
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _networkError = MutableStateFlow<String?>(null)

    val cachedSongs: StateFlow<List<Song>> = repository.cachedSongsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<Set<String>> = repository.favoritesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val favoriteSongs: StateFlow<List<Song>> = combine(cachedSongs, favorites) { songs, favIds ->
        songs.filter { favIds.contains(it.id) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentPlays: StateFlow<List<Song>> = repository.recentPlaysFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedSongs: StateFlow<List<Song>> = repository.downloadedSongsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userAudioSongs: StateFlow<List<Song>> = repository.userAudioSongsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalUserAudioBytes: StateFlow<Long> = repository.totalUserAudioBytesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val downloadingIds: StateFlow<Set<String>> = repository.downloadingIds

    val totalDownloadedBytes: StateFlow<Long> = repository.totalDownloadedBytesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val uiState: StateFlow<MusicUiState> = combine(
        cachedSongs,
        _networkError,
        _isRefreshing
    ) { songs, error, refreshing ->
        when {
            songs.isEmpty() && error != null && !refreshing -> MusicUiState.Error(error)
            songs.isEmpty() && refreshing -> MusicUiState.Loading
            songs.isEmpty() && error == null -> MusicUiState.Loading
            else -> MusicUiState.Success(songs, isOffline = error != null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MusicUiState.Loading)

    val filteredSongs: StateFlow<List<Song>> = combine(cachedSongs, _searchQuery) { songs, query ->
        if (query.isBlank()) {
            emptyList()
        } else {
            val q = query.trim().lowercase()
            songs.filter { song ->
                song.title.lowercase().contains(q) ||
                song.artist.lowercase().contains(q) ||
                (song.album?.lowercase()?.contains(q) == true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        fetchSongs()
    }

    fun fetchSongs() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _networkError.value = null
            val result = repository.refreshSongs()
            result.onFailure { error ->
                _networkError.value = error.localizedMessage ?: "Unable to connect to music server"
            }
            _isRefreshing.value = false
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            val isFav = favorites.value.contains(song.id)
            repository.toggleFavorite(song, isFav)
        }
    }

    fun clearRecentPlays() {
        viewModelScope.launch {
            repository.clearRecentPlays()
        }
    }

    fun downloadSong(song: Song) {
        viewModelScope.launch {
            repository.downloadSong(song)
        }
    }

    fun deleteDownload(songId: String) {
        viewModelScope.launch {
            repository.deleteDownload(songId)
        }
    }

    fun importUserAudio(
        uri: android.net.Uri,
        customTitle: String? = null,
        customArtist: String? = null,
        onResult: (Result<Song>) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.importUserAudio(uri, customTitle, customArtist)
            onResult(result)
        }
    }

    fun deleteUserAudio(songId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteUserAudio(songId)
            onComplete()
        }
    }

    class Factory(private val repository: MusicRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MusicViewModel(repository) as T
        }
    }
}
