package com.hamxanadeem.blockygems.ui

import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.hamxanadeem.blockygems.R
import com.hamxanadeem.blockygems.ads.AdsManager
import com.hamxanadeem.blockygems.core.GameSession
import com.hamxanadeem.blockygems.core.MoveResult
import com.hamxanadeem.blockygems.core.PieceGenerator
import com.hamxanadeem.blockygems.data.Prefs
import com.hamxanadeem.blockygems.fx.Sounds

class GameActivity : AppCompatActivity(), GameView.Listener {

    private lateinit var prefs: Prefs
    private lateinit var session: GameSession
    private lateinit var gameView: GameView
    private lateinit var overlay: View
    private lateinit var card: View
    private lateinit var reviveButton: Button

    /** Best score before this game started, to celebrate a new record. */
    private var startingBest = 0

    /** True once this game has been counted as played; it is then never saved again. */
    private var finished = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        prefs = Prefs(this)
        Sounds.init(this)
        Sounds.enabled = prefs.soundOn

        gameView = findViewById(R.id.gameView)
        overlay = findViewById(R.id.gameOverOverlay)
        card = findViewById(R.id.gameOverCard)
        reviveButton = findViewById(R.id.reviveButton)

        val newGame = intent.getBooleanExtra(EXTRA_NEW_GAME, false)
        val restored = if (newGame) null else prefs.savedGame?.let { GameSession.decode(it, PieceGenerator()) }
        startingBest = prefs.bestScore
        session = restored ?: GameSession(PieceGenerator())
        gameView.listener = this
        gameView.bestScore = startingBest
        gameView.setSession(session)

        findViewById<ImageButton>(R.id.homeButton).setOnClickListener {
            Sounds.play(Sounds.Effect.TAP)
            finish()
        }
        reviveButton.setOnClickListener { revive() }
        findViewById<Button>(R.id.playAgainButton).setOnClickListener { playAgain() }
        findViewById<Button>(R.id.homeFromGameOverButton).setOnClickListener {
            Sounds.play(Sounds.Effect.TAP)
            finishGame()
            finish()
        }
        // Swallow touches so the board underneath can't be played.
        overlay.setOnClickListener { }
    }

    override fun onPause() {
        super.onPause()
        if (!finished) prefs.savedGame = session.encode()
    }

    // ---------------------------------------------------------------- game events

    override fun onPickUp() {
        Sounds.play(Sounds.Effect.PICK_UP)
        haptic(HapticFeedbackConstants.CLOCK_TICK)
    }

    override fun onMove(result: MoveResult) {
        when {
            result.lines == 0 -> {
                Sounds.play(Sounds.Effect.PLACE)
                haptic(HapticFeedbackConstants.VIRTUAL_KEY)
            }
            result.combo >= 2 || result.lines >= 2 -> {
                Sounds.play(Sounds.Effect.COMBO, rate = 1f + 0.04f * minOf(result.combo, 8))
                haptic(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS)
            }
            else -> {
                Sounds.play(Sounds.Effect.CLEAR)
                haptic(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY)
            }
        }
        if (session.score > prefs.bestScore) prefs.bestScore = session.score
    }

    override fun onInvalidDrop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) haptic(HapticFeedbackConstants.REJECT)
    }

    override fun onGameOver() {
        if (!session.isGameOver || overlay.visibility == View.VISIBLE) return
        Sounds.play(Sounds.Effect.GAME_OVER)
        haptic(HapticFeedbackConstants.LONG_PRESS)
        showGameOver()
    }

    private fun haptic(type: Int) {
        if (prefs.vibrationOn) gameView.performHapticFeedback(type)
    }

    // ---------------------------------------------------------------- game over

    private fun showGameOver() {
        findViewById<TextView>(R.id.finalScoreText).text = session.score.toString()
        val newBest = session.score > startingBest && session.score > 0
        findViewById<TextView>(R.id.newBestText).visibility = if (newBest) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.gameOverBestText).text = getString(R.string.best_format, maxOf(prefs.bestScore, session.score))
        reviveButton.visibility = if (session.canRevive && AdsManager.isRewardedReady()) View.VISIBLE else View.GONE

        overlay.visibility = View.VISIBLE
        overlay.alpha = 0f
        overlay.animate().alpha(1f).setDuration(250).start()
        card.scaleX = 0.7f
        card.scaleY = 0.7f
        card.animate().scaleX(1f).scaleY(1f).setDuration(380).setInterpolator(OvershootInterpolator(2f)).start()
    }

    private fun hideGameOver() {
        overlay.animate().alpha(0f).setDuration(180).withEndAction { overlay.visibility = View.GONE }.start()
    }

    private fun revive() {
        Sounds.play(Sounds.Effect.TAP)
        reviveButton.isEnabled = false
        AdsManager.showRewarded(
            this,
            onRewarded = {
                if (session.canRevive) {
                    val cleared = session.revive()
                    hideGameOver()
                    gameView.showRevive(cleared)
                    Sounds.play(Sounds.Effect.COMBO)
                    prefs.savedGame = session.encode()
                }
            },
            onFinished = {
                reviveButton.isEnabled = true
                if (session.isGameOver) reviveButton.visibility = View.GONE
            },
        )
    }

    private fun playAgain() {
        Sounds.play(Sounds.Effect.TAP)
        finishGame()
        AdsManager.maybeShowInterstitial(this, prefs.gamesPlayed, prefs.gamesSinceInterstitial) { shown ->
            if (shown) prefs.gamesSinceInterstitial = 0
            startNewGame()
        }
    }

    /** Counts the game as played and forgets it. Safe to call more than once. */
    private fun finishGame() {
        if (finished) return
        finished = true
        prefs.gamesPlayed += 1
        prefs.gamesSinceInterstitial += 1
        prefs.savedGame = null
    }

    private fun startNewGame() {
        session = GameSession(PieceGenerator())
        finished = false
        startingBest = prefs.bestScore
        gameView.bestScore = startingBest
        hideGameOver()
        gameView.setSession(session)
    }

    companion object {
        const val EXTRA_NEW_GAME = "new_game"
    }
}
