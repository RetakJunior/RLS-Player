package com.rls.player.core.domain.model

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val uri: Uri,
    val folder: String,
    val dateAdded: Long,
    val isFavorite: Boolean = false
)

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val songCount: Int,
    val coverUri: Uri?
)

data class Artist(
    val name: String,
    val songCount: Int
)

data class Folder(
    val path: String,
    val songCount: Int
)

data class Playlist(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val songCount: Int = 0
)

enum class SongSort(val label: String) {
    TITLE_ASC("A → Z"),
    TITLE_DESC("Z → A"),
    ARTIST("Sanatçı"),
    ALBUM("Albüm"),
    DATE_ADDED("Eklenme Tarihi"),
    DURATION("Süre")
}

enum class RepeatSetting {
    OFF,
    ALL,
    ONE
}
