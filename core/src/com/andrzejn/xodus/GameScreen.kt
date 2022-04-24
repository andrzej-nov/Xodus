package com.andrzejn.xodus

import aurelienribon.tweenengine.Tween
import com.andrzejn.xodus.helper.FloatingTile
import com.andrzejn.xodus.helper.TW_POS_XY
import com.andrzejn.xodus.logic.Coord
import com.andrzejn.xodus.logic.Field
import com.andrzejn.xodus.logic.Shredder
import com.andrzejn.xodus.logic.Tile
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Gdx.graphics
import com.badlogic.gdx.Gdx.input
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.math.Vector2
import ktx.app.KtxScreen
import ktx.app.clearScreen
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
     * Center point of the chaos sprite placement
     */
    private val chaosPos = Vector2()

    /**
     * Center point of the new tile placement
     */
    private val newTilePos = Vector2()

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
     * Variables for internal calculations to reduce the GC load
     */
    private val v = Vector2()
    private val c = Coord()
    private val c2 = Coord()

    /**
     * It is set to the screen pointer coordinates when dragging
     */
    private val dragPos = Vector2()
    private val dragStart = Vector2()

    private enum class DragSource {
        None, Board, NewTile
    }

    private var dragFrom = DragSource.None

    /**
     * New tile to show at the newTilePos and put to field on click
     */
    private var newTile: Tile? = null

    /**
     * The floating tile rendered for tween animations
     */
    private var floatingTile = FloatingTile(ctx)

    /**
     * The shredder line
     */
    private lateinit var shredder: Shredder

    private val chaos = Sprite(ctx.chaos)
    private val logo = Sprite(ctx.logo).apply { setAlpha(0.5f) }
    private val play = Sprite(ctx.play).apply { setAlpha(0.8f) }
    private val playblue = Sprite(ctx.playblue).apply { setAlpha(0.8f) }
    private val help = Sprite(ctx.help).apply { setAlpha(0.8f) }
    private val ok = Sprite(ctx.ok).apply { setAlpha(0.8f) }
    private val settings = Sprite(ctx.settings).apply { setAlpha(0.8f) }
    private val exit = Sprite(ctx.exit).apply { setAlpha(0.8f) }

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
        shredder = Shredder(ctx.gs.fieldSize)
        if (loadSavedGame) try {
            val s = ctx.sav.savedGame()
            ctx.sav.loadSettingsAndScore(s)
            //world.deserialize(s)
        } catch (ex: Exception) {
            // Something wrong. Just recreate new World and start new game
            //world = World(ctx)
        }
        else {
            field = Field(ctx).apply {
                newGame()
                setSideLen(sideLen) { t -> setTileBasePos(t.coord, t.basePos) }
            }
            createNewTile()
        }
    }

    /**
     * Addns the last in-game duration to the total counter
     */
    private fun updateInGameDuration() {
        ctx.gs.inGameDuration += Calendar.getInstance().timeInMillis - timeStart
        timeStart = Calendar.getInstance().timeInMillis
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
        if (width > height) {
            newTilePos.set((width + wholeFieldSize) / 2f + (width - wholeFieldSize) / 4f, height / 2f)
            chaosPos.set((width - wholeFieldSize) / 2f - (width - wholeFieldSize) / 4f, height / 2f)
            ctx.fitToRect(logo, (width - wholeFieldSize) / 2f, height / 2f - sideLen)
        } else {
            newTilePos.set(width / 2f, (height - wholeFieldSize) / 2f - (height - wholeFieldSize) / 4f)
            chaosPos.set(width / 2f, (height + wholeFieldSize) / 2f + (height - wholeFieldSize) / 4f)
            ctx.fitToRect(logo, width / 2f - sideLen, (height - wholeFieldSize) / 2f)
        }
        basePos.set((width - wholeFieldSize) / 2, (height - wholeFieldSize) / 2)
        field.setSideLen(sideLen) { setTileBasePos(it.coord, it.basePos) }
        newTile?.setSideLen(sideLen)
        newTile?.setDefaultNewTileBasePos()
        floatingTile.tile = newTile
        chaos.setSize(sideLen * 1.8f, sideLen * 1.8f)
        chaos.setCenter(chaosPos.x, chaosPos.y)
        logo.setPosition(0f, height - logo.height)
        val offset = sideLen * 0.1f
        val buttonSize = sideLen * 0.8f
        play.setBounds(offset, sideLen + offset, buttonSize, buttonSize)
        playblue.setBounds(offset, sideLen + offset, buttonSize, buttonSize)
        help.setBounds(offset, offset, buttonSize, buttonSize)
        settings.setBounds(width - sideLen + offset, sideLen + offset, buttonSize, buttonSize)
        exit.setBounds(width - sideLen + offset, offset, buttonSize, buttonSize)
        if (width > height)
            ok.setPosition(newTilePos.x - sideLen / 2, newTilePos.y + sideLen)
        else
            ok.setPosition(newTilePos.x + sideLen, newTilePos.y - sideLen / 2)
        ok.setSize(sideLen, sideLen)
        ctx.score.setCoords((sideLen / 2).toInt(), sideLen, width.toFloat())
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
     * Converts screen cell indexes to the bottom-left screen coordinates to draw the rectangle
     * Sets private var v to the same value
     */
    private fun Coord.toScreenCellCorner(): Vector2 =
        v.set(this.x.toFloat(), this.y.toFloat()).scl(sideLen).add(basePos)

    /**
     * Returns indexes of the screen cell pointed by touch/mouse. (-1, -1) of pointed otside of the field.
     * Sets private var c to the same return value
     */
    private fun Vector2.toScreenCell(): Coord {
        if (this.x !in basePos.x..basePos.x + wholeFieldSize || this.y !in basePos.y..basePos.y + wholeFieldSize)
            return c.unSet()
        return c.set(((this.x - basePos.x) / sideLen).toInt(), ((this.y - basePos.y) / sideLen).toInt())
    }

    /**
     * Invoked on each screen rendering. Recalculates ball moves, invokes timer actions and draws rthe screen.
     */
    override fun render(delta: Float) {
        super.render(delta)

        // Here the Tween Engine updates all our respective object fields when any tween animation is requested
        ctx.tweenManager.update(if (graphics.isContinuousRendering) delta else 0.01f)
        // Hack to enable continuous rendering only when it is needed
        graphics.isContinuousRendering = ctx.tweenAnimationRunning()

        with(ctx.theme.screenBackground) {
            clearScreen(r, g, b, a, true)
        }
        if (!ctx.batch.isDrawing) ctx.batch.begin()
        ctx.sd.setColor(ctx.theme.gameboardBackground)
        ctx.sd.filledRectangle(basePos.x, basePos.y, wholeFieldSize, wholeFieldSize)
        ctx.pointerPosition(input.x, input.y).toScreenCell().toScreenCellCorner() // Sets the c and v variables
        if (c.isSet())
            ctx.sd.filledRectangle(v.x, v.y, sideLen, sideLen, ctx.theme.cellHilight)
        renderFieldGrid()
        field.shredBalls { shredder.shreddedBalls(it) }
        field.render()
        shredder.render(ctx, basePos.x, basePos.y + ctx.clipWrap(shredder.y + scrollOffset.y) * sideLen, wholeFieldSize)
        logo.draw(ctx.batch)
        chaos.draw(ctx.batch)
        ctx.sd.setColor(ctx.theme.gameboardBackground)
        ctx.sd.filledCircle(newTilePos, sideLen * 0.9f)
        ctx.sd.setColor(ctx.theme.settingSeparator)
        ctx.sd.circle(newTilePos.x, newTilePos.y, sideLen * 0.9f, 1f)
        floatingTile.render()
        if (field.noMoreBalls())
            play.draw(ctx.batch)
        else
            playblue.draw(ctx.batch)
        help.draw(ctx.batch)
        settings.draw(ctx.batch)
        exit.draw(ctx.batch)
        ok.draw(ctx.batch)
        ctx.score.draw(ctx.batch)
        if (ctx.batch.isDrawing) ctx.batch.end()
    }

    private fun renderFieldGrid() {
        ctx.sd.setColor(ctx.theme.gameBorders)
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
    }

    private var inAnimation = false

    /**
     * Set the selector or put the new tile into place, depending on current state
     */
    private fun setSelectorOrPutNewTileAt(v: Vector2) {
        val nT = newTile
        if (field.selectorsHitTest(v)) {
            if (nT == null && field.noMoreSelectors())
                endOfTurn()
            return
        }
        if (nT == null) return
        val coord = v.toScreenCell()
        if (coord.isNotSet()) {
            inAnimation = true
            v.set(newTilePos).sub(sideLen / 2f, sideLen / 2f)
            Tween.to(floatingTile, TW_POS_XY, 0.1f).target(v.x, v.y)
                .setCallback { _, _ ->
                    nT.setDefaultNewTileBasePos()
                    inAnimation = false
                }
                .start(ctx.tweenManager)
        } else {
            inAnimation = true
            v.set(coord.toScreenCellCorner())
            coord.screenCellToFieldTile()
            val (x, y) = coord.x to coord.y // Split to local variables to avoid side evvects from reusing class
            // properties for coord translations in subsequent render() calls
            Tween.to(floatingTile, TW_POS_XY, 0.3f).target(v.x, v.y)
                .setCallback { _, _ ->
                    field.putTile(nT, x, y)
                    newTile = null
                    floatingTile.tile = null
                    inAnimation = false
                    if (field.noMoreSelectors()) endOfTurn()
                }
                .start(ctx.tweenManager)
        }
    }

    /**
     * Creates new tile and prepares it to show at the newTilePos
     */
    private fun createNewTile() {
        newTile = Tile().apply {
            setSideLen(this@GameScreen.sideLen)
            setDefaultNewTileBasePos()
            floatingTile.tile = this
        }
    }

    /**
     * Set default new tile position in the circle
     */
    private fun Tile.setDefaultNewTileBasePos() {
        basePos.set(newTilePos).sub(sideLen / 2f, sideLen / 2f)
    }

    /**
     * Find a position for the Chaos move, create and put the tile there.
     */
    private fun chaosMove(move: Int) {
        if (move <= 0) {
            createNewTile()
            return
        }
        val t = Tile().apply {
            setSideLen(this@GameScreen.sideLen)
            basePos.set(chaosPos).sub(sideLen / 2, sideLen / 2)
        }
        floatingTile.tile = t
        val c = field.chaosTileCoord()
        val (x, y) = c.x to c.y // split to local variables to avoid side effects of reusing the class properties
        // in subsequent render() calls
        c.fieldTileToScreenCell().toScreenCellCorner() // sets the v variable
        inAnimation = true
        Tween.to(floatingTile, TW_POS_XY, 0.5f).target(v.x, v.y)
            .setCallback { _, _ ->
                field.putTile(t, x, y)
                floatingTile.tile = null
                inAnimation = false
                chaosMove(move - 1)
            }
            .start(ctx.tweenManager)
    }

    private val scrollUp = Coord(0, -1)

    /**
     * End of player's turn. Do Shredder and Chaos moves.
     */
    private fun endOfTurn() {
        if (field.noMoreBalls())
            return
        ctx.score.incrementMoves()
        shredder.advance(ctx) { scrollFieldBy(scrollUp) }
        field.advanceBalls({ chaosMove(ctx.gs.chaosMoves) }) { b -> shredder.currentBallDist(b) }
    }

    /**
     * Show player a random move. Not the best one, and not always even a good one. Just a random one, to show
     * the game controls.
     */
    private fun randomMove() {
        TODO("Not yet implemented")
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
            if (inAnimation)
                return super.touchDragged(screenX, screenY, pointer)
            dragPos.set(ctx.pointerPosition(screenX, screenY))
            val c = dragPos.toScreenCell()
            if (dragFrom == DragSource.None) {
                dragStart.set(dragPos)
                if (c.isSet())
                    dragFrom = DragSource.Board
                else if (dragPos.dst(newTilePos) < sideLen)
                    dragFrom = DragSource.NewTile
            }
            if (dragFrom == DragSource.NewTile)
                floatingTile.position.set(dragPos).sub(sideLen / 2, sideLen / 2)
            if (dragFrom == DragSource.Board) {
                if (c.isSet()) {
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
            }
            return super.touchDragged(screenX, screenY, pointer)
        }

        /**
         * Called when screen is untouched (mouse button released)
         */
        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            if (inAnimation)
                return super.touchDragged(screenX, screenY, pointer)
            if (button == Input.Buttons.RIGHT) { // Right click (on desktop)
                endOfTurn()
                return super.touchDragged(screenX, screenY, pointer)
            }
            val v = ctx.pointerPosition(screenX, screenY)
            when {
                buttonTouched(v, play) -> newGame(false)
                buttonTouched(v, exit) -> Gdx.app.exit()
                buttonTouched(v, settings) -> ctx.game.setScreen<HomeScreen>()
                buttonTouched(v, ok) -> endOfTurn()
                buttonTouched(v, help) -> randomMove()
                else -> if (dragFrom == DragSource.None || dragFrom == DragSource.NewTile || dragStart.dst(dragPos) < 4)
                // The last condition is a safeguard against clicks with minor pointer slides that are erroneously
                // interpreted as drags
                    setSelectorOrPutNewTileAt(v)
            }
            cellDragOrigin.unSet()
            dragPos.set(Vector2.Zero)
            dragFrom = DragSource.None
            newTile?.setDefaultNewTileBasePos()
            return super.touchUp(screenX, screenY, pointer, button)
        }

        /**
         * Checks if particular button is touched.
         */
        private fun buttonTouched(v: Vector2, s: Sprite) = v.x in s.x..s.x + s.width && v.y in s.y..s.y + s.height

    }

}
