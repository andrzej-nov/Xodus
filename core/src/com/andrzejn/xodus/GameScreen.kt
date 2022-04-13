package com.andrzejn.xodus

import com.badlogic.gdx.Gdx.input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import ktx.app.KtxScreen
import java.util.*

/**
 * The main game screen with the UI logic
 */
class GameScreen(
    /**
     * Reference to the main app context
     */
    val ctx: Context
) : KtxScreen {

    /**
     * The input adapter instance for this screen
     */
    private val ia = IAdapter()

    private var timeStart: Long = 0

    init {
        ctx.setTheme()
    }

    /**
     * Start new game and load saved one if any
     */
    fun newGame(loadSavedGame: Boolean) {
        ctx.score.reset()
        updateInGameDuration()
        timeStart = Calendar.getInstance().timeInMillis
        if (loadSavedGame)
            try {
                val s = ctx.sav.savedGame()
                ctx.sav.loadSettingsAndScore(s)
                //world.deserialize(s)
            } catch (ex: Exception) {
                // Something wrong. Just recreate new World and start new game
                //world = World(ctx)
            }
        resize(ctx.wc.width.toInt(), ctx.wc.height.toInt())
    }

    /**
     * Invoked on the screen show. Continuous rendering is needed by this screen.
     */
    override fun show() {
        super.show()
        input.inputProcessor = ia
        timeStart = Calendar.getInstance().timeInMillis
    }

    /**
     * Handles window resizing
     */
    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        ctx.setCamera(width, height)
    }

    /**
     * Invoked when the screen is about to switch away, for any reason.
     * Update the in-game time and save records.
     */
    override fun hide() {
        super.hide()
        input.inputProcessor = null
        updateInGameDuration()
        ctx.score.saveRecords()
    }

    /**
     * Invoked when the screen is about to close, for any reason.
     * Update the in-game time and save records.
     */
    override fun pause() {
        updateInGameDuration()
        ctx.score.saveRecords()
        super.pause()
    }

    /**
     * Addns the last in-game duration to the total counter
     */
    private fun updateInGameDuration() {
        ctx.gs.inGameDuration += Calendar.getInstance().timeInMillis - timeStart
        timeStart = Calendar.getInstance().timeInMillis
    }

    private var thereWasAMove = false

    /**
     * Autosaves the game every 5 seconds
     */
    private fun autoSaveGame() {
        if (!thereWasAMove)
            return
        ctx.sav.saveGame(/*world*/)
        thereWasAMove = false
    }

    /**
     * Invoked on each screen rendering. Recalculates ball moves, invokes timer actions and draws rthe screen.
     */
    override fun render(delta: Float) {
        super.render(delta)
        try {
            ctx.tweenManager.update(delta)
        } catch (ex: Exception) {
            // There should be no exceptions here. But if they are, simply restart the game.
            newGame(false)
        }
        if (!ctx.batch.isDrawing) ctx.batch.begin()
        // Draw screen background and border panels
        ctx.sd.setColor(Color(ctx.theme.gameboardBackground))
        ctx.sd.filledRectangle(0f, 0f, ctx.wc.width, ctx.wc.height)
        if (ctx.batch.isDrawing) ctx.batch.end()
    }


    /**
     * Our input adapter
     */
    inner class IAdapter : InputAdapter() {

        /**
         * Handle presses/clicks
         */
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            return super.touchDown(screenX, screenY, pointer, button)
        }

        /**
         * Invoked when the player drags something on the screen.
         */
        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            //val v = ctx.pointerPosition(input.x, input.y)
            return super.touchDragged(screenX, screenY, pointer)
        }

        /**
         * Called when screen is untouched (mouse button released)
         */
        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            //val v = ctx.pointerPosition(input.x, input.y)
            return super.touchUp(screenX, screenY, pointer, button)
        }

    }
}
