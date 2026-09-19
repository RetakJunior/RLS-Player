package com.rls.player.core.player

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import com.rls.player.MainActivity
import com.rls.player.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RlsPlaybackService : MediaSessionService() {

    @Inject lateinit var sleepTimer: SleepTimerManager
    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer

    companion object {
        const val ACTION_SHUFFLE = "com.rls.player.ACTION_SHUFFLE"
        const val ACTION_REPEAT = "com.rls.player.ACTION_REPEAT"
    }

    override fun onCreate() {
        super.onCreate()

        // Donanım düzeyinde ses odağı ve müzik nitelikleri
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true) // Otomatik ses odağı yönetimi
            .setHandleAudioBecomingNoisy(true)        // Kulaklık çıktığında otomatik duraklat
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        // "Şarkı bitince dur" modu etkinse ve sonraki parçaya geçildiyse durdur
                        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO &&
                            sleepTimer.consumeEndOfCurrentTrackRequest()
                        ) {
                            pauseForSleepTimer()
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED &&
                            sleepTimer.consumeEndOfCurrentTrackRequest()
                        ) {
                            pauseForSleepTimer()
                        }
                    }

                    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                        updateNotificationLayout()
                    }

                    override fun onRepeatModeChanged(repeatMode: Int) {
                        updateNotificationLayout()
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        // Hata durumunda servisi çökertmek yerine güvenli loglama ve sıradaki parçaya atlama
                    }
                })
            }

        sleepTimer.attachPauseAction(::pauseForSleepTimer)

        val sessionCallback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val connectionResult = super.onConnect(session, controller)
                val sessionCommands = connectionResult.availableSessionCommands.buildUpon()
                    .add(SessionCommand(ACTION_SHUFFLE, Bundle.EMPTY))
                    .add(SessionCommand(ACTION_REPEAT, Bundle.EMPTY))
                    .build()
                return MediaSession.ConnectionResult.accept(sessionCommands, connectionResult.availablePlayerCommands)
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): com.google.common.util.concurrent.ListenableFuture<androidx.media3.session.SessionResult> {
                when (customCommand.customAction) {
                    ACTION_SHUFFLE -> {
                        player.shuffleModeEnabled = !player.shuffleModeEnabled
                        updateNotificationLayout()
                    }
                    ACTION_REPEAT -> {
                        player.repeatMode = when (player.repeatMode) {
                            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                            else -> Player.REPEAT_MODE_OFF
                        }
                        updateNotificationLayout()
                    }
                }
                return com.google.common.util.concurrent.Futures.immediateFuture(
                    androidx.media3.session.SessionResult(androidx.media3.session.SessionResult.RESULT_SUCCESS)
                )
            }
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(sessionCallback)
            .setSessionActivity(pendingIntent)
            .build()

        updateNotificationLayout()
    }

    private fun updateNotificationLayout() {
        val shuffleButton = CommandButton.Builder()
            .setSessionCommand(SessionCommand(ACTION_SHUFFLE, Bundle.EMPTY))
            .setIconResId(if (player.shuffleModeEnabled) R.drawable.ic_shuffle else R.drawable.ic_music_note) // Fallback if needed
            .setDisplayName("Karıştır")
            .build()

        val repeatButton = CommandButton.Builder()
            .setSessionCommand(SessionCommand(ACTION_REPEAT, Bundle.EMPTY))
            .setIconResId(if (player.repeatMode != Player.REPEAT_MODE_OFF) R.drawable.ic_repeat else R.drawable.ic_music_note)
            .setDisplayName("Tekrar")
            .build()

        mediaSession?.setCustomLayout(listOf(shuffleButton, repeatButton))
    }

    private fun pauseForSleepTimer() {
        Handler(Looper.getMainLooper()).post {
            if (::player.isInitialized) {
                player.pause()
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        // Kullanıcı uygulamayı son uygulamalardan kapattığında, eğer müzik çalmıyorsa servis sonlanır
        if (mediaSession?.player?.isPlaying != true) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        sleepTimer.attachPauseAction(null)
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
