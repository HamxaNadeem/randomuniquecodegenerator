package com.hamxanadeem.blockygems.ui

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.hamxanadeem.blockygems.R
import com.hamxanadeem.blockygems.ads.AdsManager
import com.hamxanadeem.blockygems.data.Prefs
import com.hamxanadeem.blockygems.fx.Sounds

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var playButton: Button
    private lateinit var newGameButton: Button
    private lateinit var soundButton: ImageButton
    private lateinit var vibrationButton: ImageButton
    private var pulse: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)
        Sounds.init(this)
        Sounds.enabled = prefs.soundOn

        AdsManager.initialize(this)
        AdsManager.loadBanner(this, findViewById<FrameLayout>(R.id.bannerContainer))

        playButton = findViewById(R.id.playButton)
        newGameButton = findViewById(R.id.newGameButton)
        soundButton = findViewById(R.id.soundButton)
        vibrationButton = findViewById(R.id.vibrationButton)

        playButton.setOnClickListener { startGame(newGame = false) }
        newGameButton.setOnClickListener { startGame(newGame = true) }
        soundButton.setOnClickListener {
            prefs.soundOn = !prefs.soundOn
            Sounds.enabled = prefs.soundOn
            Sounds.play(Sounds.Effect.TAP)
            renderToggles()
        }
        vibrationButton.setOnClickListener {
            prefs.vibrationOn = !prefs.vibrationOn
            if (prefs.vibrationOn) it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            renderToggles()
        }
        findViewById<Button>(R.id.privacyButton).setOnClickListener { AdsManager.showPrivacyOptions(this) }

        // A gentle heartbeat on the play button invites the first tap.
        pulse = ObjectAnimator.ofPropertyValuesHolder(
            playButton,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.06f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.06f),
        ).apply {
            duration = 700
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }
    }

    private fun startGame(newGame: Boolean) {
        Sounds.play(Sounds.Effect.TAP)
        startActivity(Intent(this, GameActivity::class.java).putExtra(GameActivity.EXTRA_NEW_GAME, newGame))
    }

    private fun renderToggles() {
        soundButton.setImageResource(if (prefs.soundOn) R.drawable.ic_volume_on else R.drawable.ic_volume_off)
        soundButton.alpha = if (prefs.soundOn) 1f else 0.5f
        vibrationButton.alpha = if (prefs.vibrationOn) 1f else 0.35f
    }

    override fun onResume() {
        super.onResume()
        val hasSavedGame = prefs.savedGame != null
        playButton.setText(if (hasSavedGame) R.string.continue_game else R.string.play)
        newGameButton.visibility = if (hasSavedGame) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.bestText).text = getString(R.string.best_format, prefs.bestScore)
        findViewById<Button>(R.id.privacyButton).visibility =
            if (AdsManager.isPrivacyOptionsRequired(this)) View.VISIBLE else View.GONE
        renderToggles()
        pulse?.start()
    }

    override fun onPause() {
        super.onPause()
        pulse?.cancel()
        playButton.scaleX = 1f
        playButton.scaleY = 1f
    }
}
