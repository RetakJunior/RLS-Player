package com.rls.player.core.data.mediastore

import android.content.ContentResolver
import android.content.ContentUris
import android.os.Build
import android.provider.MediaStore
import com.rls.player.core.data.local.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MediaStoreDataSource @Inject constructor(
    private val resolver: ContentResolver
) {
    suspend fun readAudio(): List<SongEntity> = withContext(Dispatchers.IO) {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.RELATIVE_PATH
            } else {
                MediaStore.Audio.Media.DATA
            }
        )

        // Sadece müzik dosyaları ve süresi 5 saniyeden büyük olan gerçek parçalar taranır
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 5000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        resolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val pathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
            } else {
                cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            }

            buildList {
                while (cursor.moveToNext()) {
                    val mediaId = cursor.getLong(idCol)
                    val rawTitle = cursor.getString(titleCol)
                    val rawArtist = cursor.getString(artistCol)
                    val rawAlbum = cursor.getString(albumCol)
                    val albumId = cursor.getLong(albumIdCol)
                    val duration = cursor.getLong(durationCol)
                    val dateAdded = cursor.getLong(dateCol)
                    
                    val rawPath = if (pathCol != -1) cursor.getString(pathCol).orEmpty() else ""
                    val folderName = if (rawPath.contains("/")) {
                        rawPath.trimEnd('/').substringAfterLast('/')
                    } else {
                        "Music"
                    }

                    val contentUri = ContentUris.withAppendedId(collection, mediaId).toString()

                    add(
                        SongEntity(
                            id = mediaId,
                            title = rawTitle?.takeIf { it.isNotBlank() } ?: "Bilinmeyen Parça",
                            artist = rawArtist?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Bilinmeyen Sanatçı",
                            album = rawAlbum?.takeIf { it.isNotBlank() } ?: "Bilinmeyen Albüm",
                            albumId = albumId,
                            durationMs = duration,
                            uri = contentUri,
                            folder = folderName,
                            dateAdded = dateAdded
                        )
                    )
                }
            }
        } ?: emptyList()
    }
}
