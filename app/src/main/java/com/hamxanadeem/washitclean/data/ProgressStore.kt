package com.hamxanadeem.washitclean.data

import android.content.Context
import androidx.core.content.edit
import com.hamxanadeem.washitclean.core.GameState

/** Saves the player's [GameState] on the device. */
class ProgressStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("progress", Context.MODE_PRIVATE)

    fun load(): GameState {
        val defaults = GameState()
        return GameState(
            coins = prefs.getInt(KEY_COINS, defaults.coins),
            // Copy the set: SharedPreferences must not see changes to the returned instance.
            ownedNozzles = prefs.getStringSet(KEY_OWNED, null)?.toSet() ?: defaults.ownedNozzles,
            selectedNozzle = prefs.getString(KEY_SELECTED, null) ?: defaults.selectedNozzle,
            currentLevel = prefs.getInt(KEY_LEVEL, defaults.currentLevel),
            totalLevelsCompleted = prefs.getInt(KEY_COMPLETED, defaults.totalLevelsCompleted),
            levelsSinceInterstitial = prefs.getInt(KEY_SINCE_AD, defaults.levelsSinceInterstitial),
        )
    }

    fun save(state: GameState) {
        prefs.edit {
            putInt(KEY_COINS, state.coins)
            putStringSet(KEY_OWNED, state.ownedNozzles.toSet())
            putString(KEY_SELECTED, state.selectedNozzle)
            putInt(KEY_LEVEL, state.currentLevel)
            putInt(KEY_COMPLETED, state.totalLevelsCompleted)
            putInt(KEY_SINCE_AD, state.levelsSinceInterstitial)
        }
    }

    private companion object {
        const val KEY_COINS = "coins"
        const val KEY_OWNED = "owned_nozzles"
        const val KEY_SELECTED = "selected_nozzle"
        const val KEY_LEVEL = "current_level"
        const val KEY_COMPLETED = "total_completed"
        const val KEY_SINCE_AD = "levels_since_interstitial"
    }
}
