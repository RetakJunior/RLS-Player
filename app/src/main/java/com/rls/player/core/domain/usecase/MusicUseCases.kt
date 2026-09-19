package com.rls.player.core.domain.usecase

import com.rls.player.core.domain.model.Song
import com.rls.player.core.domain.repository.CollectionRepository
import com.rls.player.core.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ScanLibraryUseCase @Inject constructor(
    private val repository: MusicRepository
) {
    suspend operator fun invoke(): Result<Int> = repository.scanLibrary()
}

class GetSongsUseCase @Inject constructor(
    private val repository: MusicRepository
) {
    operator fun invoke(): Flow<List<Song>> = repository.songs()
}

class SearchMusicUseCase @Inject constructor(
    private val repository: MusicRepository
) {
    operator fun invoke(query: String): Flow<List<Song>> = repository.search(query)
}

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: CollectionRepository
) {
    suspend operator fun invoke(songId: Long): Boolean = repository.toggleFavorite(songId)
}

class AddRecentUseCase @Inject constructor(
    private val repository: CollectionRepository
) {
    suspend operator fun invoke(songId: Long) = repository.addRecent(songId)
}

class CreatePlaylistUseCase @Inject constructor(
    private val repository: CollectionRepository
) {
    suspend operator fun invoke(name: String): Long = repository.createPlaylist(name)
}

class DeletePlaylistUseCase @Inject constructor(
    private val repository: CollectionRepository
) {
    suspend operator fun invoke(id: Long) = repository.deletePlaylist(id)
}

class AddSongToPlaylistUseCase @Inject constructor(
    private val repository: CollectionRepository
) {
    suspend operator fun invoke(playlistId: Long, songId: Long) = repository.addSongToPlaylist(playlistId, songId)
}

class RemoveSongFromPlaylistUseCase @Inject constructor(
    private val repository: CollectionRepository
) {
    suspend operator fun invoke(playlistId: Long, songId: Long) = repository.removeSongFromPlaylist(playlistId, songId)
}
