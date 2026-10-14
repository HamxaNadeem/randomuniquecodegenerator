package com.hamxanadeem.blockygems.core

import java.io.ByteArrayOutputStream
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Makes the game's sound effects as WAV files in code, so the app ships no audio
 * assets. Each note is a soft bell: a sine wave plus an octave overtone with a
 * quick attack and exponential decay.
 */
object ToneSynth {

    const val SAMPLE_RATE = 22_050

    data class Note(val frequency: Double, val millis: Int, val volume: Double = 0.6)

    /** Plays [notes] one after another, each starting [gapMillis] after the last. */
    fun wav(notes: List<Note>, gapMillis: Int? = null): ByteArray {
        require(notes.isNotEmpty()) { "Need at least one note" }
        val starts = notes.runningFold(0) { acc, note -> acc + (gapMillis ?: note.millis) }
        val totalMillis = notes.indices.maxOf { starts[it] + notes[it].millis }
        val samples = DoubleArray(totalMillis * SAMPLE_RATE / 1000)
        notes.forEachIndexed { i, note ->
            val offset = starts[i] * SAMPLE_RATE / 1000
            val length = note.millis * SAMPLE_RATE / 1000
            val attack = SAMPLE_RATE / 200 // 5 ms
            for (n in 0 until length) {
                val index = offset + n
                if (index >= samples.size) break
                val t = n.toDouble() / SAMPLE_RATE
                val envelope = minOf(1.0, n.toDouble() / attack) * exp(-5.0 * n / length)
                val wave = sin(2 * PI * note.frequency * t) + 0.3 * sin(4 * PI * note.frequency * t)
                samples[index] += note.volume * envelope * wave / 1.3
            }
        }
        return encodeWav(samples)
    }

    private fun encodeWav(samples: DoubleArray): ByteArray {
        val dataSize = samples.size * 2
        val out = ByteArrayOutputStream(44 + dataSize)
        fun int32(v: Int) = repeat(4) { out.write((v shr (8 * it)) and 0xFF) }
        fun int16(v: Int) = repeat(2) { out.write((v shr (8 * it)) and 0xFF) }
        out.write("RIFF".toByteArray())
        int32(36 + dataSize)
        out.write("WAVEfmt ".toByteArray())
        int32(16) // format chunk size
        int16(1) // PCM
        int16(1) // mono
        int32(SAMPLE_RATE)
        int32(SAMPLE_RATE * 2) // byte rate
        int16(2) // block align
        int16(16) // bits per sample
        out.write("data".toByteArray())
        int32(dataSize)
        for (s in samples) int16((s.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).roundToInt())
        return out.toByteArray()
    }

    // The game's sounds. Frequencies are notes of a C major scale.
    private const val C5 = 523.25
    private const val E5 = 659.25
    private const val G5 = 783.99
    private const val C6 = 1046.50
    private const val E6 = 1318.51
    private const val G6 = 1567.98
    private const val C7 = 2093.00

    fun pickUp() = wav(listOf(Note(E6, 60, 0.35)))
    fun place() = wav(listOf(Note(G5, 90, 0.5), Note(C5, 90, 0.3)), gapMillis = 0)
    fun clear() = wav(listOf(Note(C6, 160), Note(E6, 160), Note(G6, 260)), gapMillis = 70)
    fun combo() = wav(listOf(Note(E6, 140), Note(G6, 140), Note(C7, 140), Note(G6, 140), Note(C7, 320)), gapMillis = 65)
    fun gameOver() = wav(listOf(Note(G5, 220), Note(E5, 220), Note(C5, 480)), gapMillis = 180)
    fun tap() = wav(listOf(Note(C6, 50, 0.3)))
}
