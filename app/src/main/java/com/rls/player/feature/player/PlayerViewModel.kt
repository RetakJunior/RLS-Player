package com.rls.player.feature.player

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.rls.player.core.domain.model.RepeatSetting
import com.rls.player.core.domain.model.Song
import com.rls.player.core.domain.usecase.AddRecentUseCase
import com.rls.player.core.domain.usecase.ToggleFavoriteUseCase
import com.rls.player.core.player.RlsPlaybackService
import com.rls.player.core.player.SleepTimerManager
import com.rls.player.core.player.SleepTimerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import javax.inject.Inject

data class QueueItem(val id: String, val title: String, val artist: String)

data class PlayerUiState(
    val song: Song? = null,
    val isPlaying: Boolean = false,
    val durationMs: Long = 0,
    val shuffle: Boolean = false,
    val repeat: RepeatSetting = RepeatSetting.OFF,
    val queue: List<QueueItem> = emptyList(),
    val sleepTimer: SleepTimerState = SleepTimerState.Inactive
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    application: Application,
    private val addRecent: AddRecentUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val sleepTimer: SleepTimerManager
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val executor: Executor = ContextCompat.getMainExecutor(application)
    private var controller: MediaController? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = sync(player)
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.mediaId?.toLongOrNull()?.let { id ->
                viewModelScope.launch { addRecent(id) }
            }
            controller?.let(::sync)
        }
    }

    init {
        viewModelScope.launch {
            sleepTimer.state.collectLatest { timer ->
                _state.update { it.copy(sleepTimer = timer) }
            }
        }

        // Seek Bar ilerleme sayacı (Her 500ms'de bir akıcı güncelleme)
        viewModelScope.launch {
            while (true) {
                delay(500)
                controller?.let {
                    _positionMs.value = it.currentPosition.coerceAtLeast(0)
                }
            }
        }

        val sessionToken = SessionToken(
            application,
            ComponentName(application, RlsPlaybackService::class.java)
        )
        val controllerFuture = MediaController.Builder(application, sessionToken).buildAsync()
        controllerFuture.addListener({
            runCatching { controllerFuture.get() }.onSuccess { resolved ->
                controller = resolved.also {
                    it.addListener(listener)
                    sync(it)
                }
            }
        }, executor)
    }

    private fun sync(player: Player) {
        val item = player.currentMediaItem
        _positionMs.value = player.currentPosition.coerceAtLeast(0)
        _state.update {
            it.copy(
                isPlaying = player.isPlaying,
                durationMs = player.duration.coerceAtLeast(0),
                shuffle = player.shuffleModeEnabled,
                repeat = when (player.repeatMode) {
                    Player.REPEAT_MODE_ONE -> RepeatSetting.ONE
                    Player.REPEAT_MODE_ALL -> RepeatSetting.ALL
                    else -> RepeatSetting.OFF
                },
                queue = List(player.mediaItemCount) { index ->
                    val qItem = player.getMediaItemAt(index)
                    QueueItem(
                        id = qItem.mediaId,
                        title = qItem.mediaMetadata.title?.toString() ?: "Bilinmeyen Parça",
                        artist = qItem.mediaMetadata.artist?.toString() ?: "Bilinmeyen Sanatçı"
                    )
                },
                song = item?.let {
                    Song(
                        id = it.mediaId.toLongOrNull() ?: -1,
                        title = it.mediaMetadata.title?.toString() ?: "Bilinmeyen Parça",
                        artist = it.mediaMetadata.artist?.toString() ?: "Bilinmeyen Sanatçı",
                        album = it.mediaMetadata.albumTitle?.toString() ?: "",
                        albumId = -1,
                        durationMs = it.mediaMetadata.durationMs ?: player.duration.coerceAtLeast(0),
                        uri = it.localConfiguration?.uri ?: Uri.EMPTY,
                        folder = "",
                        dateAdded = 0
                    )
                }
            )
        }
    }

    fun play(songs: List<Song>, startAt: Int) {
        val items = songs.map { song ->
            MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(song.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .setDurationMs(song.durationMs)
                        .build()
                )
                .build()
        }
        controller?.apply {
            setMediaItems(items, startAt, 0)
            prepare()
            play()
        }
    }

    fun togglePlay() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun next() = controller?.seekToNextMediaItem()
    fun previous() = controller?.seekToPreviousMediaItem()
    fun seek(position: Long) = controller?.seekTo(position)

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    fun toggleFavorite() {
        state.value.song?.id?.takeIf { it >= 0 }?.let { id ->
            viewModelScope.launch { toggleFavoriteUseCase(id) }
        }
    }

    fun startSleepTimer(minutes: Int) = sleepTimer.start(minutes)
    fun stopAfterCurrentTrack() = sleepTimer.stopAtEndOfCurrentTrack()
    fun cancelSleepTimer() = sleepTimer.cancel()

    override fun onCleared() {
        controller?.removeListener(listener)
        controller?.release()
        super.onCleared()
    }
}
