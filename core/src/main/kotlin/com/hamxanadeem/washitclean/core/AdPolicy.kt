package com.hamxanadeem.washitclean.core

/**
 * Decides when an interstitial ad may be shown, so ads appear between levels
 * without annoying players.
 */
class AdPolicy(
    /** Show at most one interstitial per this many completed levels. */
    val levelsBetweenInterstitials: Int = 3,
    /** Minimum time between two interstitials. */
    val minMillisBetweenInterstitials: Long = 90_000L,
    /** No interstitials until the player has completed this many levels. */
    val gracePeriodLevels: Int = 3,
) {
    fun shouldShowInterstitial(state: GameState, nowMillis: Long, lastShownMillis: Long?): Boolean =
        state.totalLevelsCompleted >= gracePeriodLevels &&
            state.levelsSinceInterstitial >= levelsBetweenInterstitials &&
            (lastShownMillis == null || nowMillis - lastShownMillis >= minMillisBetweenInterstitials)
}
