package com.hamxanadeem.washitclean.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ProgressionTest {

    private val level1 = LevelCatalog.level(1)
    private val jet = NozzleCatalog.byId("jet")

    @Test
    fun `new player owns and uses the starting nozzle`() {
        val state = GameState()
        assertEquals(NozzleCatalog.default, state.nozzle)
        assertTrue(NozzleCatalog.default.id in state.ownedNozzles)
        assertEquals(0, NozzleCatalog.default.price)
    }

    @Test
    fun `completing a level pays out and unlocks the next one`() {
        val state = Progression.completeLevel(GameState(), level1)
        assertEquals(level1.reward, state.coins)
        assertEquals(2, state.currentLevel)
        assertEquals(1, state.totalLevelsCompleted)
        assertEquals(1, state.levelsSinceInterstitial)
    }

    @Test
    fun `replaying an old level does not move progress back`() {
        val state = Progression.completeLevel(GameState(currentLevel = 5), level1)
        assertEquals(5, state.currentLevel)
    }

    @Test
    fun `rewarded bonus brings the total to the multiplier`() {
        val afterLevel = Progression.completeLevel(GameState(), level1)
        val afterBonus = Progression.claimRewardedBonus(afterLevel, level1)
        assertEquals(level1.reward * Progression.REWARDED_MULTIPLIER, afterBonus.coins)
    }

    @Test
    fun `buying a nozzle spends coins and selects it`() {
        val result = Progression.buy(GameState(coins = 200), jet)
        val state = (result as PurchaseResult.Purchased).state
        assertEquals(200 - jet.price, state.coins)
        assertTrue(jet.id in state.ownedNozzles)
        assertEquals(jet, state.nozzle)
    }

    @Test
    fun `cannot buy without enough coins or twice`() {
        assertSame(PurchaseResult.NotEnoughCoins, Progression.buy(GameState(coins = jet.price - 1), jet))
        assertSame(PurchaseResult.AlreadyOwned, Progression.buy(GameState(coins = 9999), NozzleCatalog.default))
    }

    @Test
    fun `can only select owned nozzles`() {
        val state = GameState()
        assertEquals(state, Progression.select(state, jet))
        val owner = state.copy(ownedNozzles = state.ownedNozzles + jet.id)
        assertEquals(jet, Progression.select(owner, jet).nozzle)
    }

    @Test
    fun `unknown nozzle id falls back to the default`() {
        assertEquals(NozzleCatalog.default, GameState(selectedNozzle = "removed").nozzle)
    }

    @Test
    fun `nozzles get better as they get pricier`() {
        NozzleCatalog.all.zipWithNext().forEach { (a, b) ->
            assertTrue(b.price > a.price)
            assertTrue(b.power * b.radius > a.power * a.radius)
        }
    }

    @Test
    fun `cannot add negative coins`() {
        assertThrows<IllegalArgumentException> { Progression.addCoins(GameState(), -1) }
    }
}
