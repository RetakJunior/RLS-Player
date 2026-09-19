package com.rls.player.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rls.player.core.data.settings.PlayerSettings
import com.rls.player.core.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel class SettingsViewModel @Inject constructor(private val repository: SettingsRepository) : ViewModel() {
    val state: StateFlow<PlayerSettings> = repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayerSettings())
    fun setReducedMotion(enabled: Boolean) = viewModelScope.launch { repository.setReduceMotion(enabled) }
}
