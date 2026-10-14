package com.hamxanadeem.washitclean.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.hamxanadeem.washitclean.R
import com.hamxanadeem.washitclean.ads.AdsManager
import com.hamxanadeem.washitclean.core.GameState
import com.hamxanadeem.washitclean.core.Level
import com.hamxanadeem.washitclean.core.LevelCatalog
import com.hamxanadeem.washitclean.core.Progression
import com.hamxanadeem.washitclean.data.ProgressStore

class GameActivity : AppCompatActivity(), WashView.Listener {

    private lateinit var store: ProgressStore
    private lateinit var state: GameState
    private lateinit var level: Level

    private lateinit var washView: WashView
    private lateinit var titleText: TextView
    private lateinit var coinsText: TextView
    private lateinit var percentText: TextView
    private lateinit var nozzleText: TextView
    private lateinit var progress: LinearProgressIndicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        store = ProgressStore(this)
        state = store.load()

        washView = findViewById(R.id.washView)
        titleText = findViewById(R.id.levelTitle)
        coinsText = findViewById(R.id.coinsText)
        percentText = findViewById(R.id.percentText)
        nozzleText = findViewById(R.id.nozzleText)
        progress = findViewById(R.id.cleanProgress)

        washView.listener = this
        startLevel(state.currentLevel)
    }

    private fun startLevel(number: Int) {
        level = LevelCatalog.level(number)
        titleText.text = getString(R.string.level_title, level.number, level.name)
        nozzleText.text = getString(R.string.nozzle_format, state.nozzle.name)
        updateCoins()
        washView.start(level, state.nozzle)
    }

    private fun updateCoins() {
        coinsText.text = getString(R.string.coins_format, state.coins)
    }

    override fun onProgress(cleanFraction: Float) {
        val percent = (cleanFraction * 100).toInt()
        progress.setProgressCompat(percent, true)
        percentText.text = getString(R.string.percent_format, percent)
    }

    override fun onCleaned() {
        // Pay out straight away so the reward is kept even if the player leaves now.
        state = Progression.completeLevel(state, level)
        store.save(state)
        updateCoins()
        washView.postDelayed({ if (!isFinishing) showResult() }, RESULT_DELAY_MILLIS)
    }

    private fun showResult() {
        val completed = level
        val builder = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.level_clean_title)
            .setMessage(getString(R.string.level_clean_message, completed.reward))
            .setCancelable(false)
            .setNegativeButton(R.string.next_level) { _, _ -> goToNextLevel() }
        if (AdsManager.isRewardedReady()) {
            val bonusTotal = completed.reward * Progression.REWARDED_MULTIPLIER
            builder.setPositiveButton(getString(R.string.watch_ad_bonus, bonusTotal)) { _, _ ->
                AdsManager.showRewarded(
                    this,
                    onRewarded = {
                        state = Progression.claimRewardedBonus(state, completed)
                        store.save(state)
                        updateCoins()
                    },
                    // A rewarded ad was just shown, so skip the interstitial this time.
                    onFinished = { startLevel(state.currentLevel) },
                )
            }
        }
        builder.show()
    }

    private fun goToNextLevel() {
        AdsManager.maybeShowInterstitial(this, state) { shown ->
            if (shown) {
                state = Progression.interstitialShown(state)
                store.save(state)
            }
            startLevel(state.currentLevel)
        }
    }

    override fun onResume() {
        super.onResume()
        // The nozzle may have changed in the shop.
        val saved = store.load()
        if (saved.selectedNozzle != state.selectedNozzle) {
            state = state.copy(selectedNozzle = saved.selectedNozzle, ownedNozzles = saved.ownedNozzles)
            washView.setNozzle(state.nozzle)
            nozzleText.text = getString(R.string.nozzle_format, state.nozzle.name)
        }
    }

    private companion object {
        const val RESULT_DELAY_MILLIS = 900L
    }
}
