package com.rls.player.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rls.player.core.domain.model.*
import com.rls.player.core.domain.repository.CollectionRepository
import com.rls.player.core.domain.repository.MusicRepository
import com.rls.player.core.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val favorites: List<Song> = emptyList(),
    val recent: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val query: String = "",
    val sort: SongSort = SongSort.TITLE_ASC,
    val scanning: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val scan: ScanLibraryUseCase,
    getSongs: GetSongsUseCase,
    music: MusicRepository,
    private val collection: CollectionRepository,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val createPlaylistUseCase: CreatePlaylistUseCase,
    private val deletePlaylistUseCase: DeletePlaylistUseCase,
    private val addSongToPlaylistUseCase: AddSongToPlaylistUseCase,
    private val removeSongFromPlaylistUseCase: RemoveSongFromPlaylistUseCase
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val sort = MutableStateFlow(SongSort.TITLE_ASC)
    private val scanning = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    private val filteredAndSortedSongs = combine(
        getSongs(),
        query,
        sort
    ) { rawSongs, currentQuery, currentSort ->
        val filtered = if (currentQuery.isBlank()) rawSongs
        else rawSongs.filter {
            it.title.contains(currentQuery, ignoreCase = true) ||
                    it.artist.contains(currentQuery, ignoreCase = true) ||
                    it.album.contains(currentQuery, ignoreCase = true)
        }

        when (currentSort) {
            SongSort.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
            SongSort.TITLE_DESC -> filtered.sortedByDescending { it.title.lowercase() }
            SongSort.ARTIST -> filtered.sortedBy { it.artist.lowercase() }
            SongSort.ALBUM -> filtered.sortedBy { it.album.lowercase() }
            SongSort.DATE_ADDED -> filtered.sortedByDescending { it.dateAdded }
            SongSort.DURATION -> filtered.sortedByDescending { it.durationMs }
        }
    }.flowOn(kotlinx.coroutines.Dispatchers.Default)

    val state: StateFlow<LibraryUiState> = combine(
        filteredAndSortedSongs,
        music.albums(),
        music.artists(),
        music.folders(),
        collection.favorites(),
        collection.recent(),
        collection.playlists(),
        query,
        sort,
        scanning,
        message
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        LibraryUiState(
            songs = values[0] as List<Song>,
            albums = values[1] as List<Album>,
            artists = values[2] as List<Artist>,
            folders = values[3] as List<Folder>,
            favorites = values[4] as List<Song>,
            recent = values[5] as List<Song>,
            playlists = values[6] as List<Playlist>,
            query = values[7] as String,
            sort = values[8] as SongSort,
            scanning = values[9] as Boolean,
            message = values[10] as String?
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun setQuery(value: String) {
        query.value = value
    }

    fun setSort(newSort: SongSort) {
        sort.value = newSort
    }

    fun rescan() = viewModelScope.launch {
        scanning.value = true
        scan()
            .onSuccess { count -> message.value = "$count parça bulundu ve kütüphaneye eklendi" }
            .onFailure { err -> message.value = "Tarama tamamlanamadı: ${err.message ?: "Bilinmeyen hata"}" }
        scanning.value = false
    }

    fun consumeMessage() {
        message.value = null
    }

    fun createPlaylist(name: String) = viewModelScope.launch {
        runCatching { createPlaylistUseCase(name) }
            .onSuccess { message.value = "Çalma listesi oluşturuldu" }
            .onFailure { message.value = it.message ?: "Hata oluştu" }
    }

    fun deletePlaylist(playlistId: Long) = viewModelScope.launch {
        runCatching { deletePlaylistUseCase(playlistId) }
            .onSuccess { message.value = "Çalma listesi silindi" }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) = viewModelScope.launch {
        runCatching { addSongToPlaylistUseCase(playlistId, songId) }
            .onSuccess { message.value = "Parça listeye eklendi" }
    }

    fun toggleFavorite(songId: Long) = viewModelScope.launch {
        toggleFavoriteUseCase(songId)
    }
}
