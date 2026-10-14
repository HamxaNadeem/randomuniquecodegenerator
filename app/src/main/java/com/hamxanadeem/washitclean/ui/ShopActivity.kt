package com.hamxanadeem.washitclean.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hamxanadeem.washitclean.R
import com.hamxanadeem.washitclean.ads.AdsManager
import com.hamxanadeem.washitclean.core.GameState
import com.hamxanadeem.washitclean.core.NozzleCatalog
import com.hamxanadeem.washitclean.core.Progression
import com.hamxanadeem.washitclean.core.PurchaseResult
import com.hamxanadeem.washitclean.data.ProgressStore

class ShopActivity : AppCompatActivity() {

    private lateinit var store: ProgressStore
    private lateinit var state: GameState
    private lateinit var list: LinearLayout
    private lateinit var coinsText: TextView
    private lateinit var freeCoinsButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop)
        store = ProgressStore(this)
        state = store.load()
        list = findViewById(R.id.nozzleList)
        coinsText = findViewById(R.id.coinsText)
        freeCoinsButton = findViewById(R.id.freeCoinsButton)

        freeCoinsButton.text = getString(R.string.free_coins, Progression.FREE_COINS_REWARD)
        freeCoinsButton.setOnClickListener {
            freeCoinsButton.isEnabled = false
            AdsManager.showRewarded(
                this,
                onRewarded = { update(Progression.addCoins(state, Progression.FREE_COINS_REWARD)) },
                onFinished = { render() },
            )
        }
        findViewById<Button>(R.id.backButton).setOnClickListener { finish() }
        render()
    }

    private fun update(newState: GameState) {
        state = newState
        store.save(state)
        render()
    }

    private fun render() {
        coinsText.text = getString(R.string.coins_format, state.coins)
        freeCoinsButton.isEnabled = AdsManager.isRewardedReady()
        list.removeAllViews()
        val inflater = LayoutInflater.from(this)
        for (nozzle in NozzleCatalog.all) {
            val row = inflater.inflate(R.layout.item_nozzle, list, false)
            row.findViewById<TextView>(R.id.nozzleName).text = nozzle.name
            row.findViewById<TextView>(R.id.nozzleStats).text =
                getString(R.string.nozzle_stats, nozzle.radius.toInt(), (nozzle.power * 100).toInt())
            val button = row.findViewById<Button>(R.id.nozzleAction)
            when {
                nozzle.id == state.selectedNozzle -> {
                    button.text = getString(R.string.selected)
                    button.isEnabled = false
                }
                nozzle.id in state.ownedNozzles -> {
                    button.text = getString(R.string.select)
                    button.setOnClickListener { update(Progression.select(state, nozzle)) }
                }
                else -> {
                    button.text = getString(R.string.buy_format, nozzle.price)
                    button.setOnClickListener {
                        when (val result = Progression.buy(state, nozzle)) {
                            is PurchaseResult.Purchased -> update(result.state)
                            PurchaseResult.NotEnoughCoins ->
                                Toast.makeText(this, R.string.not_enough_coins, Toast.LENGTH_SHORT).show()
                            PurchaseResult.AlreadyOwned -> render()
                        }
                    }
                }
            }
            list.addView(row)
        }
    }
}
