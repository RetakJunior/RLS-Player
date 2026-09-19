package com.rls.player.core.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SleepTimerState {
    data object Inactive : SleepTimerState
    data class Until(val endsAtEpochMs: Long) : SleepTimerState
    data object EndOfCurrentTrack : SleepTimerState
}

@Singleton
class SleepTimerManager @Inject constructor() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow<SleepTimerState>(SleepTimerState.Inactive)
    val state: StateFlow<SleepTimerState> = _state.asStateFlow()
    private var timerJob: Job? = null
    private val pauseAction = AtomicReference<(() -> Unit)?>(null)

    fun attachPauseAction(action: (() -> Unit)?) {
        pauseAction.set(action)
    }

    fun start(minutes: Int) {
        require(minutes > 0) { "Zamanlayıcı pozitif bir değer olmalıdır" }
        cancel()
        val endAt = System.currentTimeMillis() + minutes * 60_000L
        _state.value = SleepTimerState.Until(endAt)
        timerJob = scope.launch {
            delay(minutes * 60_000L)
            _state.value = SleepTimerState.Inactive
            pauseAction.get()?.invoke()
        }
    }

    fun stopAtEndOfCurrentTrack() {
        timerJob?.cancel()
        timerJob = null
        _state.value = SleepTimerState.EndOfCurrentTrack
    }

    fun consumeEndOfCurrentTrackRequest(): Boolean {
        if (_state.value !is SleepTimerState.EndOfCurrentTrack) return false
        _state.value = SleepTimerState.Inactive
        return true
    }

    fun cancel() {
        timerJob?.cancel()
        timerJob = null
        _state.value = SleepTimerState.Inactive
    }
}
