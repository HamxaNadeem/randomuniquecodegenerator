package com.hamxanadeem.washitclean.core

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AdPolicyTest {

    private val policy = AdPolicy(levelsBetweenInterstitials = 3, minMillisBetweenInterstitials = 60_000, gracePeriodLevels = 3)
    private val ready = GameState(totalLevelsCompleted = 10, levelsSinceInterstitial = 3)

    @Test
    fun `no interstitials for brand new players`() {
        val state = GameState(totalLevelsCompleted = 2, levelsSinceInterstitial = 2)
        assertFalse(policy.shouldShowInterstitial(state, nowMillis = 1_000_000, lastShownMillis = null))
    }

    @Test
    fun `shows after enough levels`() {
        assertTrue(policy.shouldShowInterstitial(ready, nowMillis = 1_000_000, lastShownMillis = null))
    }

    @Test
    fun `waits for enough levels since the last one`() {
        val state = ready.copy(levelsSinceInterstitial = 2)
        assertFalse(policy.shouldShowInterstitial(state, nowMillis = 1_000_000, lastShownMillis = null))
    }

    @Test
    fun `respects the minimum time between interstitials`() {
        assertFalse(policy.shouldShowInterstitial(ready, nowMillis = 100_000, lastShownMillis = 50_000))
        assertTrue(policy.shouldShowInterstitial(ready, nowMillis = 110_000, lastShownMillis = 50_000))
    }

    @Test
    fun `counter resets after showing`() {
        val after = Progression.interstitialShown(ready)
        assertFalse(policy.shouldShowInterstitial(after, nowMillis = 1_000_000, lastShownMillis = null))
    }
}
