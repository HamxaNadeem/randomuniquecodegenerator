package com.hamxanadeem.blockygems.data

import android.content.Context
import androidx.core.content.edit

/** Everything saved on the device: scores, settings, ad pacing and the game in progress. */
class Prefs(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("blocky_gems", Context.MODE_PRIVATE)

    var bestScore: Int
        get() = prefs.getInt("best_score", 0)
        set(value) = prefs.edit { putInt("best_score", value) }

    var gamesPlayed: Int
        get() = prefs.getInt("games_played", 0)
        set(value) = prefs.edit { putInt("games_played", value) }

    var gamesSinceInterstitial: Int
        get() = prefs.getInt("games_since_interstitial", 0)
        set(value) = prefs.edit { putInt("games_since_interstitial", value) }

    var soundOn: Boolean
        get() = prefs.getBoolean("sound_on", true)
        set(value) = prefs.edit { putBoolean("sound_on", value) }

    var vibrationOn: Boolean
        get() = prefs.getBoolean("vibration_on", true)
        set(value) = prefs.edit { putBoolean("vibration_on", value) }

    /** The unfinished game, encoded by GameSession.encode(), or null. */
    var savedGame: String?
        get() = prefs.getString("saved_game", null)
        set(value) = prefs.edit { if (value == null) remove("saved_game") else putString("saved_game", value) }
}
