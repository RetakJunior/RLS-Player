package com.rls.player.core.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val uri: String,
    val folder: String,
    val dateAdded: Long
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val songId: Long,
    val addedAt: Long
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long
)

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"]
)
data class PlaylistSongEntity(
    val playlistId: Long,
    val songId: Long,
    val position: Int
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val songId: Long,
    val playedAt: Long
)

data class PlaylistWithCount(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val songCount: Int
)

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<SongEntity>>

    @Query("""
        SELECT * FROM songs 
        WHERE title LIKE '%' || :query || '%' 
           OR artist LIKE '%' || :query || '%' 
           OR album LIKE '%' || :query || '%' 
        ORDER BY title COLLATE NOCASE ASC
    """)
    fun search(query: String): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<SongEntity>)

    @Query("""
        SELECT s.* FROM songs s 
        INNER JOIN playlist_songs ps ON s.id = ps.songId 
        WHERE ps.playlistId = :playlistId 
        ORDER BY ps.position ASC
    """)
    suspend fun playlistSongs(playlistId: Long): List<SongEntity>

    @Query("""
        SELECT s.* FROM songs s 
        INNER JOIN history h ON s.id = h.songId 
        ORDER BY h.playedAt DESC 
        LIMIT 100
    """)
    fun recent(): Flow<List<SongEntity>>
}

@Dao
interface FavoriteDao {
    @Query("SELECT songId FROM favorites")
    fun ids(): Flow<List<Long>>

    @Query("""
        SELECT s.* FROM songs s 
        INNER JOIN favorites f ON s.id = f.songId 
        ORDER BY f.addedAt DESC
    """)
    fun songs(): Flow<List<SongEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE songId = :songId)")
    suspend fun exists(songId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(item: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE songId = :songId")
    suspend fun remove(songId: Long)
}

@Dao
interface PlaylistDao {
    @Query("""
        SELECT p.*, (SELECT COUNT(*) FROM playlist_songs ps WHERE ps.playlistId = p.id) AS songCount 
        FROM playlists p 
        ORDER BY createdAt DESC
    """)
    fun observe(): Flow<List<PlaylistWithCount>>

    @Insert
    suspend fun create(item: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun delete(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSong(item: PlaylistSongEntity)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSong(playlistId: Long, songId: Long)

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun count(playlistId: Long): Int
}

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(item: HistoryEntity)

    @Query("DELETE FROM history")
    suspend fun clear()
}

@Database(
    entities = [
        SongEntity::class,
        FavoriteEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        HistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class RlsDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun historyDao(): HistoryDao
}
