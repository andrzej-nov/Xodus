package com.andrzejn.xodus

import com.andrzejn.xodus.logic.Field
import com.badlogic.gdx.Gdx.input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import ktx.app.KtxScreen
import java.util.*
import kotlin.math.min

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
        newGame(false)
    }

    lateinit var field: Field

    /**
     * Start new game and load saved one if any
     */
    fun newGame(loadSavedGame: Boolean) {
        ctx.score.reset()
        updateInGameDuration()
        timeStart = Calendar.getInstance().timeInMillis
        if (loadSavedGame) try {
            val s = ctx.sav.savedGame()
            ctx.sav.loadSettingsAndScore(s)
            //world.deserialize(s)
        } catch (ex: Exception) {
            // Something wrong. Just recreate new World and start new game
            //world = World(ctx)
        }
        else field = Field(ctx).also { it.newGame() }
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
     * Bottom-left corner of the board
     */
    val basePos = Vector2()

    /**
     * The squre cell side length
     */
    var sideLen: Float = 0f
    var fieldWidth: Float = 0f
    var fieldHeight: Float = 0f

    /**
     * Handles window resizing
     */
    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        ctx.setCamera(width, height)

        sideLen = min(width.toFloat() / ctx.gs.fieldWidth, height.toFloat() / 8)
        fieldWidth = sideLen * ctx.gs.fieldWidth
        fieldHeight = sideLen * 8
        basePos.set((width - fieldWidth) / 2, (height - fieldHeight) / 2)
        field.sideLen = sideLen

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
        if (!thereWasAMove) return
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
        ctx.sd.filledRectangle(basePos.x, basePos.y, basePos.x + fieldWidth, basePos.y + fieldHeight)
        ctx.sd.setColor(Color(ctx.theme.gameBorders))
        (0..8).forEach { y ->
            ctx.sd.line(
                basePos.x,
                basePos.y + y * sideLen,
                basePos.x + fieldWidth,
                basePos.y + y * sideLen,
                2f
            )
        }
        (0..ctx.gs.fieldWidth).forEach { x ->
            ctx.sd.line(
                basePos.x + x * sideLen,
                basePos.y,
                basePos.x + x * sideLen,
                basePos.y + fieldHeight,
                2f
            )
        }
        field.render(basePos)
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
