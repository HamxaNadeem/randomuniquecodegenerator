package com.hamxanadeem.washitclean.core

/** Everything about the player's progress that is saved between sessions. */
data class GameState(
    val coins: Int = 0,
    val ownedNozzles: Set<String> = setOf(NozzleCatalog.default.id),
    val selectedNozzle: String = NozzleCatalog.default.id,
    /** The next level to play. */
    val currentLevel: Int = 1,
    val totalLevelsCompleted: Int = 0,
    val levelsSinceInterstitial: Int = 0,
) {
    val nozzle: Nozzle get() = NozzleCatalog.byId(selectedNozzle)
}

sealed interface PurchaseResult {
    data class Purchased(val state: GameState) : PurchaseResult
    data object AlreadyOwned : PurchaseResult
    data object NotEnoughCoins : PurchaseResult
}

/** The rules for changing [GameState]. Every function returns a new state. */
object Progression {

    /** Watching a rewarded ad after a level multiplies its reward by this. */
    const val REWARDED_MULTIPLIER = 3

    /** Coins for watching a rewarded ad in the shop. */
    const val FREE_COINS_REWARD = 50

    fun completeLevel(state: GameState, level: Level): GameState = state.copy(
        coins = state.coins + level.reward,
        currentLevel = maxOf(state.currentLevel, level.number + 1),
        totalLevelsCompleted = state.totalLevelsCompleted + 1,
        levelsSinceInterstitial = state.levelsSinceInterstitial + 1,
    )

    /** Extra coins for a rewarded ad after [level], on top of the reward already given. */
    fun claimRewardedBonus(state: GameState, level: Level): GameState =
        addCoins(state, level.reward * (REWARDED_MULTIPLIER - 1))

    fun addCoins(state: GameState, amount: Int): GameState {
        require(amount >= 0) { "Amount must not be negative" }
        return state.copy(coins = state.coins + amount)
    }

    fun buy(state: GameState, nozzle: Nozzle): PurchaseResult = when {
        nozzle.id in state.ownedNozzles -> PurchaseResult.AlreadyOwned
        state.coins < nozzle.price -> PurchaseResult.NotEnoughCoins
        else -> PurchaseResult.Purchased(
            state.copy(
                coins = state.coins - nozzle.price,
                ownedNozzles = state.ownedNozzles + nozzle.id,
                selectedNozzle = nozzle.id,
            ),
        )
    }

    fun select(state: GameState, nozzle: Nozzle): GameState =
        if (nozzle.id in state.ownedNozzles) state.copy(selectedNozzle = nozzle.id) else state

    fun interstitialShown(state: GameState): GameState = state.copy(levelsSinceInterstitial = 0)
}
