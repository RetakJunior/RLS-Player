package com.rls.player.core.domain.repository

import com.rls.player.core.domain.model.*
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
    fun songs(): Flow<List<Song>>
    fun search(query: String): Flow<List<Song>>
    fun albums(): Flow<List<Album>>
    fun artists(): Flow<List<Artist>>
    fun folders(): Flow<List<Folder>>
    suspend fun scanLibrary(): Result<Int>
}

interface CollectionRepository {
    fun favorites(): Flow<List<Song>>
    fun recent(): Flow<List<Song>>
    fun playlists(): Flow<List<Playlist>>
    suspend fun toggleFavorite(songId: Long): Boolean
    suspend fun addRecent(songId: Long)
    suspend fun createPlaylist(name: String): Long
    suspend fun deletePlaylist(id: Long)
    suspend fun songsInPlaylist(id: Long): List<Song>
    suspend fun addSongToPlaylist(playlistId: Long, songId: Long)
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)
}
