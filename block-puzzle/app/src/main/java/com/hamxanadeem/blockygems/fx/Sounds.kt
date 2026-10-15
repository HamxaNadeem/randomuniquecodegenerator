package com.hamxanadeem.blockygems.fx

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.hamxanadeem.blockygems.core.ToneSynth
import java.io.File

/**
 * Plays the game's sound effects. The sounds are synthesised once into the cache
 * folder (see [ToneSynth]) and loaded into a low-latency [SoundPool].
 */
object Sounds {

    enum class Effect { PICK_UP, PLACE, CLEAR, COMBO, GAME_OVER, TAP }

    var enabled = true

    private var pool: SoundPool? = null
    private val ids = IntArray(Effect.entries.size)

    fun init(context: Context) {
        if (pool != null) return
        val app = context.applicationContext
        val soundPool = SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .build()
        pool = soundPool
        // Writing the files is quick but still disk I/O, so keep it off the main thread.
        Thread {
            for (effect in Effect.entries) {
                try {
                    val file = File(app.cacheDir, "sfx_v1_${effect.name.lowercase()}.wav")
                    if (!file.exists()) file.writeBytes(synth(effect))
                    ids[effect.ordinal] = soundPool.load(file.path, 1)
                } catch (e: Exception) {
                    Log.w("Sounds", "Could not prepare ${effect.name}", e)
                }
            }
        }.start()
    }

    private fun synth(effect: Effect): ByteArray = when (effect) {
        Effect.PICK_UP -> ToneSynth.pickUp()
        Effect.PLACE -> ToneSynth.place()
        Effect.CLEAR -> ToneSynth.clear()
        Effect.COMBO -> ToneSynth.combo()
        Effect.GAME_OVER -> ToneSynth.gameOver()
        Effect.TAP -> ToneSynth.tap()
    }

    fun play(effect: Effect, rate: Float = 1f) {
        if (!enabled) return
        val id = ids[effect.ordinal]
        if (id != 0) pool?.play(id, 1f, 1f, 1, 0, rate.coerceIn(0.5f, 2f))
    }
}
