package com.hamxanadeem.washitclean.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.hamxanadeem.washitclean.R
import com.hamxanadeem.washitclean.ads.AdsManager
import com.hamxanadeem.washitclean.core.LevelCatalog
import com.hamxanadeem.washitclean.data.ProgressStore

class MainActivity : AppCompatActivity() {

    private lateinit var store: ProgressStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        store = ProgressStore(this)

        AdsManager.initialize(this)
        AdsManager.loadBanner(this, findViewById<FrameLayout>(R.id.bannerContainer))

        findViewById<Button>(R.id.playButton).setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
        }
        findViewById<Button>(R.id.shopButton).setOnClickListener {
            startActivity(Intent(this, ShopActivity::class.java))
        }
        findViewById<Button>(R.id.privacyButton).setOnClickListener {
            AdsManager.showPrivacyOptions(this)
        }
    }

    override fun onResume() {
        super.onResume()
        val state = store.load()
        val level = LevelCatalog.level(state.currentLevel)
        findViewById<TextView>(R.id.coinsText).text = getString(R.string.coins_format, state.coins)
        findViewById<Button>(R.id.playButton).text = getString(R.string.play_level, level.number, level.name)
        findViewById<Button>(R.id.privacyButton).visibility =
            if (AdsManager.isPrivacyOptionsRequired(this)) View.VISIBLE else View.GONE
    }
}
