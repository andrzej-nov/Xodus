package com.andrzejn.xodus

import com.andrzejn.xodus.logic.Coord
import com.andrzejn.xodus.logic.Field
import com.badlogic.gdx.Gdx.input
import com.badlogic.gdx.Input
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

    /**
     * In-game time tracker.
     */
    private var timeStart: Long = 0

    /**
     * The game field with all logic.
     */
    private lateinit var field: Field

    /**
     * Bottom-left corner of the board
     */
    private val basePos = Vector2()

    /**
     * The field cell side length
     */
    private var sideLen: Float = 0f

    /**
     * The whole field square size
     */
    private var wholeFieldSize: Float = 0f

    /**
     * Logical field coords to screen coords offset.
     * E.g. if the field has been visually scrolled by 1 cell to the right, then scrollOffset.x = 1
     */
    private val scrollOffset = Coord(0, 0)

    /**
     * Variable for internal calculations to reduce the GC load
     */
    private val c2 = Coord()

    init {
        ctx.setTheme()
        newGame(false)
    }

    /**
     * Start new game and load saved one if any
     */
    fun newGame(loadSavedGame: Boolean) {
        ctx.score.reset()
        updateInGameDuration()
        timeStart = Calendar.getInstance().timeInMillis
        scrollOffset.set(0, 0)
        if (loadSavedGame) try {
            val s = ctx.sav.savedGame()
            ctx.sav.loadSettingsAndScore(s)
            //world.deserialize(s)
        } catch (ex: Exception) {
            // Something wrong. Just recreate new World and start new game
            //world = World(ctx)
        }
        else field = Field(ctx).apply {
            newGame()
            setSideLen(sideLen) { t -> setTileBasePos(t.coord, t.basePos) }
        }
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
     * Invoked on the screen show. Continuous rendering is needed by this screen.
     */
    override fun show() {
        super.show()
        input.inputProcessor = ia
        timeStart = Calendar.getInstance().timeInMillis
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
     * Handles window resizing
     */
    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        ctx.setCamera(width, height)

        sideLen = if (width > height)
            min(width.toFloat() / (ctx.gs.fieldSize + 4), height.toFloat() / ctx.gs.fieldSize)
        else
            min(width.toFloat() / ctx.gs.fieldSize, height.toFloat() / (ctx.gs.fieldSize + 4))
        wholeFieldSize = sideLen * ctx.gs.fieldSize
        basePos.set((width - wholeFieldSize) / 2, (height - wholeFieldSize) / 2)
        field.setSideLen(sideLen) { setTileBasePos(it.coord, it.basePos) }
    }

    /**
     * Applies scrolling to the field
     */
    private fun scrollFieldBy(c: Coord) {
        scrollOffset.add(c)
        field.applyToAllTiles { t ->
            setTileBasePos(t.coord, t.basePos)
            t.intent.forEach { i -> i.resetSelectorArrows() }
        }
    }

    /**
     * Set the tile corner screen coordinates, considering scrolling
     */
    private fun setTileBasePos(c: Coord, basePos: Vector2) {
        c2.set(c).fieldTileToScreenCell()
        basePos.set(c2.x * sideLen, c2.y * sideLen).add(this.basePos)
    }

    /**
     * Update the provided coords from the cell pointed on the screen to the logical field tile indexes.
     * Returns the converted coord for chaining.
     */
    private fun Coord.screenCellToFieldTile() =
        this.set(ctx.clipWrap(this.x - scrollOffset.x), ctx.clipWrap(this.y - scrollOffset.y))

    /**
     * Update the provided coords from the logical field tile indexes to the cell pointed on the screen
     * Returns the converted coord for chaining.
     */
    private fun Coord.fieldTileToScreenCell() =
        this.set(ctx.clipWrap(this.x + scrollOffset.x), ctx.clipWrap(this.y + scrollOffset.y))

    /**
     * Variable for internal calculations, to reduce GS load
     */
    private val c = Coord()

    /**
     * Returns indexes of the screen cell pointed by touch/mouse. (-1, -1) of pointed otside of the field.
     */
    private fun Vector2.toScreenCell(): Coord {
        if (this.x !in basePos.x..basePos.x + wholeFieldSize || this.y !in basePos.y..basePos.y + wholeFieldSize)
            return c.unSet()
        return c.set(((this.x - basePos.x) / sideLen).toInt(), ((this.y - basePos.y) / sideLen).toInt())
    }

    /**
     * Returns indexes of the logical field tile pointed by touch/mouse (-1, -1) of pointed otside of the field.
     * Uses the same internal c variable as Vector2.toScreenCell()
     */
    private fun Vector2.toFieldTile(): Coord {
        if (this.toScreenCell().isNotSet())
            return c
        return c.screenCellToFieldTile()
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
        ctx.sd.filledRectangle(basePos.x, basePos.y, basePos.x + wholeFieldSize, basePos.y + wholeFieldSize)
        ctx.sd.setColor(Color(ctx.theme.gameBorders))
        (0..ctx.gs.fieldSize).forEach { y ->
            ctx.sd.line(
                basePos.x,
                basePos.y + y * sideLen,
                basePos.x + wholeFieldSize,
                basePos.y + y * sideLen,
                1f
            )
        }
        (0..ctx.gs.fieldSize).forEach { x ->
            ctx.sd.line(
                basePos.x + x * sideLen,
                basePos.y,
                basePos.x + x * sideLen,
                basePos.y + wholeFieldSize,
                1f
            )
        }
        field.render()
        if (ctx.batch.isDrawing) ctx.batch.end()
    }

    /**
     * Our input adapter
     */
    inner class IAdapter : InputAdapter() {
        private val cellDragOrigin = Coord()

        /**
         * Invoked when the player drags something on the screen.
         */
        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            val v = ctx.pointerPosition(input.x, input.y)
            val c = v.toScreenCell()
            if (c.x >= 0) {
                if (cellDragOrigin.isNotSet())
                    cellDragOrigin.set(c)
                else if (cellDragOrigin != c) {
                    if (c.sub(cellDragOrigin).isNotZero()) {
                        scrollFieldBy(c)
                        cellDragOrigin.add(c)
                    }
                }
            } else
                cellDragOrigin.unSet()
            return super.touchDragged(screenX, screenY, pointer)
        }

        /**
         * Called when screen is untouched (mouse button released)
         */
        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            if (button == Input.Buttons.RIGHT) // For temporary testing. TODO Remove it before release
                newGame(false)
            if (cellDragOrigin.isSet())
                cellDragOrigin.unSet()
            val v = ctx.pointerPosition(input.x, input.y)
            field.selectorsHitTest(v)
            return super.touchUp(screenX, screenY, pointer, button)
        }

    }
}
