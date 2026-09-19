package com.rls.player.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.rls.player.core.domain.model.RepeatSetting
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class PlayerSettings(
    val reduceMotion: Boolean = false,
    val defaultShuffle: Boolean = false,
    val defaultRepeat: RepeatSetting = RepeatSetting.OFF
)

class SettingsRepository @Inject constructor(
    private val store: DataStore<Preferences>
) {
    private object Keys {
        val reduceMotion = booleanPreferencesKey("reduce_motion")
        val shuffle = booleanPreferencesKey("default_shuffle")
        val repeat = stringPreferencesKey("default_repeat")
    }

    val settings: Flow<PlayerSettings> = store.data.map { pref ->
        PlayerSettings(
            reduceMotion = pref[Keys.reduceMotion] ?: false,
            defaultShuffle = pref[Keys.shuffle] ?: false,
            defaultRepeat = runCatching {
                RepeatSetting.valueOf(pref[Keys.repeat] ?: "OFF")
            }.getOrDefault(RepeatSetting.OFF)
        )
    }

    suspend fun setReduceMotion(value: Boolean) {
        store.edit { it[Keys.reduceMotion] = value }
    }
}
