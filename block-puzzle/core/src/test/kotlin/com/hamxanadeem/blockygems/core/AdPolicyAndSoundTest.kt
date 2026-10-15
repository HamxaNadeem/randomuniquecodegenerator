package com.hamxanadeem.blockygems.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AdPolicyAndSoundTest {

    private val policy = AdPolicy(gamesBetweenInterstitials = 2, minMillisBetweenInterstitials = 60_000, graceGames = 2)

    @Test
    fun `interstitials wait for the grace period, the game gap and the time gap`() {
        assertFalse(policy.shouldShowInterstitial(1, 5, 1_000_000, null))
        assertFalse(policy.shouldShowInterstitial(5, 1, 1_000_000, null))
        assertTrue(policy.shouldShowInterstitial(5, 2, 1_000_000, null))
        assertFalse(policy.shouldShowInterstitial(5, 2, 100_000, 50_000))
        assertTrue(policy.shouldShowInterstitial(5, 2, 120_000, 50_000))
    }

    @Test
    fun `sounds are valid wav files`() {
        for (wav in listOf(ToneSynth.pickUp(), ToneSynth.place(), ToneSynth.clear(), ToneSynth.combo(), ToneSynth.gameOver(), ToneSynth.tap())) {
            assertEquals("RIFF", String(wav, 0, 4))
            assertEquals("WAVE", String(wav, 8, 4))
            assertEquals("data", String(wav, 36, 4))
            val dataSize = (wav[40].toInt() and 0xFF) or ((wav[41].toInt() and 0xFF) shl 8) or
                ((wav[42].toInt() and 0xFF) shl 16) or ((wav[43].toInt() and 0xFF) shl 24)
            assertEquals(wav.size - 44, dataSize)
            assertTrue(dataSize > 0)
        }
    }

    @Test
    fun `notes play one after another`() {
        val one = ToneSynth.wav(listOf(ToneSynth.Note(440.0, 100)))
        val two = ToneSynth.wav(listOf(ToneSynth.Note(440.0, 100), ToneSynth.Note(440.0, 100)))
        assertEquals((one.size - 44) * 2, two.size - 44)
    }
}
