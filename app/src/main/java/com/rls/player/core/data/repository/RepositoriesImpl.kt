package com.rls.player.core.data.repository

import android.net.Uri
import com.rls.player.core.data.local.*
import com.rls.player.core.data.mediastore.MediaStoreDataSource
import com.rls.player.core.domain.model.*
import com.rls.player.core.domain.repository.CollectionRepository
import com.rls.player.core.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private fun SongEntity.asSong(favorite: Boolean = false) = Song(
    id = id,
    title = title,
    artist = artist,
    album = album,
    albumId = albumId,
    durationMs = durationMs,
    uri = Uri.parse(uri),
    folder = folder,
    dateAdded = dateAdded,
    isFavorite = favorite
)

class LocalMusicRepository @Inject constructor(
    private val scanner: MediaStoreDataSource,
    private val songs: SongDao,
    private val favorites: FavoriteDao
) : MusicRepository {

    override fun songs(): Flow<List<Song>> = songs.observeAll()
        .combine(favorites.ids()) { rows, ids ->
            rows.map { it.asSong(it.id in ids) }
        }

    override fun search(query: String): Flow<List<Song>> = songs.search(query.trim())
        .combine(favorites.ids()) { rows, ids ->
            rows.map { it.asSong(it.id in ids) }
        }

    override fun albums(): Flow<List<Album>> = songs().map { all ->
        all.groupBy { it.albumId }.map { (id, tracks) ->
            Album(
                id = id,
                title = tracks.first().album,
                artist = tracks.first().artist,
                songCount = tracks.size,
                coverUri = Uri.parse("content://media/external/audio/albumart/$id")
            )
        }.sortedBy { it.title.lowercase() }
    }

    override fun artists(): Flow<List<Artist>> = songs().map { all ->
        all.groupBy { it.artist }.map {
            Artist(it.key, it.value.size)
        }.sortedBy { it.name.lowercase() }
    }

    override fun folders(): Flow<List<Folder>> = songs().map { all ->
        all.groupBy { it.folder.ifBlank { "Music" } }.map {
            Folder(it.key, it.value.size)
        }.sortedBy { it.path.lowercase() }
    }

    override suspend fun scanLibrary(): Result<Int> = runCatching {
        val audioList = scanner.readAudio()
        songs.upsertAll(audioList)
        audioList.size
    }
}

class LocalCollectionRepository @Inject constructor(
    private val songDao: SongDao,
    private val favoriteDao: FavoriteDao,
    private val playlistDao: PlaylistDao,
    private val historyDao: HistoryDao
) : CollectionRepository {

    override fun favorites(): Flow<List<Song>> = favoriteDao.songs()
        .map { list -> list.map { row -> row.asSong(favorite = true) } }

    override fun recent(): Flow<List<Song>> = songDao.recent()
        .map { list -> list.map { row -> row.asSong() } }

    override fun playlists(): Flow<List<Playlist>> = playlistDao.observe()
        .map { list -> list.map { row -> Playlist(row.id, row.name, row.createdAt, row.songCount) } }

    override suspend fun toggleFavorite(songId: Long): Boolean {
        return if (favoriteDao.exists(songId)) {
            favoriteDao.remove(songId)
            false
        } else {
            favoriteDao.add(FavoriteEntity(songId, System.currentTimeMillis()))
            true
        }
    }

    override suspend fun addRecent(songId: Long) {
        historyDao.put(HistoryEntity(songId, System.currentTimeMillis()))
    }

    override suspend fun createPlaylist(name: String): Long {
        require(name.isNotBlank()) { "Çalma listesi adı boş olamaz" }
        return playlistDao.create(PlaylistEntity(name = name.trim(), createdAt = System.currentTimeMillis()))
    }

    override suspend fun deletePlaylist(id: Long) {
        playlistDao.delete(id)
    }

    override suspend fun songsInPlaylist(id: Long): List<Song> {
        return songDao.playlistSongs(id).map { it.asSong() }
    }

    override suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        val count = playlistDao.count(playlistId)
        playlistDao.addSong(PlaylistSongEntity(playlistId, songId, count))
    }

    override suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        playlistDao.removeSong(playlistId, songId)
    }
}
