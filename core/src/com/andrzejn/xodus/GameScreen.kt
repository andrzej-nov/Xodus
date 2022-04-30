package com.andrzejn.xodus

import aurelienribon.tweenengine.Timeline
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
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.input.GestureDetector
import com.badlogic.gdx.input.GestureDetector.GestureAdapter
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.StringBuilder
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
     * The input multiplexer first calls our input adapter that does the most part of the processing,
     * then the gesture adapter handles long presses and zooms
     */
    private val im = InputMultiplexer().apply {
        addProcessor(GestureDetector(GAdapter()))
        addProcessor(IAdapter())
    }

    /**
     * Set to true on first show call. Used by the Home/Settings screen to determone what to do on the Back button
     */
    var wasDisplayed = false

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

    private val chaos = Sprite(ctx.a.chaos)
    private val logo = Sprite(ctx.a.logo).apply { setAlpha(0.5f) }
    private val play = Sprite(ctx.a.play).apply { setAlpha(0.8f) }
    private val playblue = Sprite(ctx.a.playblue).apply { setAlpha(0.8f) }
    private val help = Sprite(ctx.a.help).apply { setAlpha(0.8f) }
    private val ok = Sprite(ctx.a.ok).apply { setAlpha(0.8f) }
    private val settings = Sprite(ctx.a.settings).apply { setAlpha(0.8f) }
    private val exit = Sprite(ctx.a.exit).apply { setAlpha(0.8f) }
    private val hand = Sprite(ctx.a.hand).apply { setAlpha(0.7f) }

    private var inAutoMove = false

    init {
        ctx.setTheme()
    }

    /**
     * Start new game and load saved one if any
     */
    fun newGame(loadSavedGame: Boolean) {
        ctx.cp.resetScrollOffset()
        shredder = Shredder(ctx.gs.fieldSize)
        ctx.score.reset()
        if (loadSavedGame) try {
            val s = ctx.sav.savedGame()
            val i = ctx.sav.loadSettingsAndScore(s)
            if (i > 0)
                deserialize(s, i)
            field.setSideLen(ctx.cp.sideLen) { t -> ctx.cp.setTileBasePos(t.coord, t.basePos) }
            return
        } catch (ex: Exception) {
            newGame(false)
            return
            // Something wrong. Just proceed to recreate and start new game
        }
        field = Field(ctx).apply {
            newGame()
            setSideLen(ctx.cp.sideLen) { t -> ctx.cp.setTileBasePos(t.coord, t.basePos) }
        }
        createNewTile()
    }

    /**
     * Addns the last in-game duration to the total counter
     */
    private fun updateInGameDuration() {
        if (ctx.gs.inGameDuration > 31536000000) // Milliseconds in year
            ctx.gs.inGameDuration = 0
        ctx.gs.inGameDuration += Calendar.getInstance().timeInMillis - timeStart
        timeStart = Calendar.getInstance().timeInMillis
    }

    /**
     * Invoked on the screen show. Continuous rendering is needed by this screen.
     */
    override fun show() {
        super.show()
        input.inputProcessor = im
        wasDisplayed = true
        timeStart = Calendar.getInstance().timeInMillis
    }

    /**
     * Invoked when the screen is about to switch away, for any reason.
     * Update the in-game time and save records.
     */
    override fun hide() {
        input.inputProcessor = null
        updateInGameDuration()
        ctx.score.saveRecords()
        super.hide()
    }

    /**
     * Invoked when the screen is about to close, for any reason.
     * Update the in-game time and save records.
     */
    override fun pause() {
        updateInGameDuration()
        ctx.score.saveRecords()
        ctx.sav.saveGame(this)
        super.pause()
    }

    /**
     * Handles window resizing
     */
    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        if (width == 0 || height == 0) // Window minimize on desktop works that way
            return
        ctx.setScreenSize(width, height)
        val sideLen =
            if (width > height) min(width.toFloat() / (ctx.gs.fieldSize + 4), height.toFloat() / ctx.gs.fieldSize)
            else min(width.toFloat() / ctx.gs.fieldSize, height.toFloat() / (ctx.gs.fieldSize + 4))
        ctx.cp.sideLen = sideLen
        ctx.cp.wholeFieldSize = ctx.cp.sideLen * ctx.gs.fieldSize
        if (width > height) {
            newTilePos.set((width + ctx.cp.wholeFieldSize) / 2f + (width - ctx.cp.wholeFieldSize) / 4f, height / 2f)
            chaosPos.set((width - ctx.cp.wholeFieldSize) / 2f - (width - ctx.cp.wholeFieldSize) / 4f, height / 2f)
            ctx.cp.fitToRect(logo, (width - ctx.cp.wholeFieldSize) / 2f, height / 2f - sideLen)
        } else {
            newTilePos.set(width / 2f, (height - ctx.cp.wholeFieldSize) / 2f - (height - ctx.cp.wholeFieldSize) / 4f)
            chaosPos.set(width / 2f, (height + ctx.cp.wholeFieldSize) / 2f + (height - ctx.cp.wholeFieldSize) / 4f)
            ctx.cp.fitToRect(logo, width / 2f - sideLen, (height - ctx.cp.wholeFieldSize) / 2f)
        }
        basePos.set((width - ctx.cp.wholeFieldSize) / 2, (height - ctx.cp.wholeFieldSize) / 2)
        ctx.setFieldSize(basePos)
        field.setSideLen(sideLen) { ctx.cp.setTileBasePos(it.coord, it.basePos) }
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
        if (width > height) ok.setPosition(newTilePos.x - sideLen / 2, newTilePos.y + sideLen)
        else ok.setPosition(newTilePos.x + sideLen, newTilePos.y - sideLen / 2)
        ok.setSize(sideLen, sideLen)
        ctx.score.setCoords((sideLen / 2).toInt(), sideLen, width.toFloat())
        hand.setSize(sideLen, sideLen)
        hand.setOrigin(sideLen / 2, sideLen)
    }

    private val v = Vector2()
    private val c = Coord()

    /**
     * Smooth scroll field by field world coordinates
     */
    private fun panFieldBy(deltaX: Float, deltaY: Float) {
        val k = ctx.fieldScale
        with(ctx.fieldCamPos) {
            x -= deltaX / k
            y -= deltaY / k
        }
        normalizeScrollOffcetByCells()
    }

    /**
     * When field camera position goes too far, adjust the field scroll offset by cells
     */
    private fun normalizeScrollOffcetByCells(): Coord {
        c.set(
            ((ctx.cp.wholeFieldSize / 2 - ctx.fieldCamPos.x) / ctx.cp.sideLen).toInt(),
            ((ctx.cp.wholeFieldSize / 2 - ctx.fieldCamPos.y) / ctx.cp.sideLen).toInt()
        )
        if (c.isNotZero())
            scrollFieldBy(c)
        return c
    }

    /**
     * Scroll field by field cells
     */
    private fun scrollFieldBy(c: Coord) {
        ctx.cp.scrollFieldBy(c)
        field.updateTilePositions()
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
        if (inAutoMoveTile)
            floatingTile.position.set(hand.x, hand.y).add(0f, ctx.cp.sideLen / 2)
        with(ctx.theme.screenBackground) {
            clearScreen(r, g, b, a, true)
        }
        if (!ctx.batch.isDrawing) ctx.batch.begin()
        val sideLen = ctx.cp.sideLen
        renderField(sideLen)
        ctx.batch.flush()
        renderScreen(sideLen)
        if (ctx.batch.isDrawing) ctx.batch.end()
    }

    /**
     * Render on screen viewport
     */
    private fun renderScreen(sideLen: Float) {
        ctx.drawToScreen()
        logo.draw(ctx.batch)
        chaos.draw(ctx.batch)
        ctx.sd.setColor(ctx.theme.gameboardBackground)
        ctx.sd.filledCircle(newTilePos, sideLen * 0.9f)
        ctx.sd.setColor(ctx.theme.settingSeparator)
        ctx.sd.circle(newTilePos.x, newTilePos.y, sideLen * 0.9f, 1f)
        floatingTile.render()
        if (field.noMoreBalls()) play.draw(ctx.batch) else playblue.draw(ctx.batch)
        help.draw(ctx.batch)
        settings.draw(ctx.batch)
        exit.draw(ctx.batch)
        ok.draw(ctx.batch)
        ctx.score.draw(ctx.batch)
        if (inAutoMove)
            hand.draw(ctx.batch)
    }

    /**
     * Render on field viewport
     */
    private fun renderField(sideLen: Float) {
        ctx.drawToField()
        ctx.sd.setColor(ctx.theme.gameboardBackground)
        ctx.sd.filledRectangle(
            -sideLen,
            -sideLen,
            ctx.cp.wholeFieldSize + sideLen * 2,
            ctx.cp.wholeFieldSize + sideLen * 2
        )
        if (input.isTouched) {
            c.set(ctx.cp.toFieldIndex(ctx.cp.pointerPositionField(input.x, input.y)))
            if (c.isSet()) {
                v.set(ctx.cp.toFieldCellCorner(c))
                ctx.sd.filledRectangle(v.x, v.y, sideLen, sideLen, ctx.theme.cellHilight)
            }
        }
        renderFieldGrid()
        field.shredBalls { shredder.shreddedBalls(it) }
        field.render()
        shredder.render(ctx)
    }

    private fun renderFieldGrid() {
        val sideLen = ctx.cp.sideLen
        ctx.sd.setColor(ctx.theme.gameBorders)
        (-1..ctx.gs.fieldSize + 1).forEach { y ->
            ctx.sd.line(
                -sideLen, y * sideLen, ctx.cp.wholeFieldSize + 2 * sideLen, y * sideLen, 1f
            )
        }
        (-1..ctx.gs.fieldSize + 1).forEach { x ->
            ctx.sd.line(
                x * sideLen, -sideLen, x * sideLen, ctx.cp.wholeFieldSize + 2 * sideLen, 1f
            )
        }
    }

    private var inAnimation = false

    /**
     * Set the selector or put the new tile into place, depending on current state
     */
    private fun setSelectorOrPutNewTileAt(vf: Vector2, newTileDragged: Boolean) {
        val nT = newTile
        if (field.selectorsHitTest(vf)) {
            if (nT == null && field.noMoreSelectors()) endOfTurn()
            return
        }
        if (newTileDragged && nT != null) {
            val coord = ctx.cp.toFieldIndex(vf)
            if (vf.y >= 0 && !coord.isNotSet()) {
                dropNewTileToField(coord, nT)
                return
            }
        }
        if (nT != null)
            cancelNewTileDrag(nT)
    }

    private fun dropNewTileToField(
        coord: Coord,
        nT: Tile
    ) {
        inAnimation = true
        v.set(ctx.cp.toScreenCellCorner(coord))
        coord.set(ctx.cp.fieldIndexToTileIndex(coord))
        val (x, y) = coord.x to coord.y // Split to local variables to avoid side evvects from reusing class
        // properties for coord translations in subsequent render() calls
        Tween.to(floatingTile, TW_POS_XY, 0.3f).target(v.x, v.y).setCallback { _, _ ->
            field.putTile(nT, x, y)
            newTile = null
            floatingTile.tile = null
            inAnimation = false
            if (field.noMoreSelectors()) endOfTurn()
        }.start(ctx.tweenManager)
    }

    private fun cancelNewTileDrag(nT: Tile) {
        inAnimation = true
        v.set(newTilePos).sub(ctx.cp.sideLen / 2f, ctx.cp.sideLen / 2f)
        Tween.to(floatingTile, TW_POS_XY, 0.1f).target(v.x, v.y).setCallback { _, _ ->
            nT.setDefaultNewTileBasePos()
            inAnimation = false
        }.start(ctx.tweenManager)
    }

    /**
     * Creates new tile and prepares it to show at the newTilePos
     */
    private fun createNewTile() {
        newTile = Tile().apply {
            setSideLen(ctx.cp.sideLen)
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
        if (field.noMoreBalls())
            return
        if (move <= 0) {
            createNewTile()
            ctx.sav.saveGame(this)
            return
        }
        val t = Tile().apply {
            setSideLen(ctx.cp.sideLen)
            basePos.set(chaosPos).sub(sideLen / 2, sideLen / 2)
        }
        floatingTile.tile = t
        val c = field.chaosTileCoord()
        val (x, y) = c.x to c.y // split to local variables to avoid side effects of reusing the class properties
        // in subsequent render() calls
        v.set(ctx.cp.toScreenCellCorner(ctx.cp.tileIndexToFieldIndex(c)))
        inAnimation = true
        Tween.to(floatingTile, TW_POS_XY, 0.5f).target(v.x, v.y).setCallback { _, _ ->
            field.putTile(t, x, y)
            floatingTile.tile = null
            inAnimation = false
            chaosMove(move - 1)
        }.start(ctx.tweenManager)
    }

    private val scrollUp = Coord(0, -1)

    /**
     * End of player's turn. Do Shredder and Chaos moves.
     */
    private fun endOfTurn() {
        ctx.fieldScale = 1f
        if (field.noMoreBalls()) return
        ctx.score.incrementMoves()
        shredder.advance(ctx) { scrollFieldBy(scrollUp) }
        field.advanceBalls({ chaosMove(ctx.gs.chaosMoves) }, { b -> shredder.currentBallDist(b) })
    }

    private enum class MoveTarget {
        NewTile, Selector, EndOfTurn
    }

    private val vf2 = Vector2()
    private val c2 = Coord()

    private var inAutoMoveTile = false

    /**
     * Show player a random move. Not the best one, and not always even a good one. Just a random one, to show
     * the game controls.
     */
    private fun autoMove() {
        if (inAutoMove || ctx.tweenAnimationRunning())
            return
        val moveTargets = mutableListOf<MoveTarget>()
        if (newTile != null) repeat(2) { moveTargets.add(MoveTarget.NewTile) }
        if (field.openSelector.isNotEmpty())
            repeat(5) { moveTargets.add(MoveTarget.Selector) }
        moveTargets.add(MoveTarget.EndOfTurn)
        val seq = Timeline.createSequence()
        val mt = moveTargets.random()
        when (mt) {
            MoveTarget.NewTile -> {
                val nT = newTile ?: return
                v.set(newTilePos).sub(ctx.cp.sideLen / 2, ctx.cp.sideLen)
                c2.set(ctx.cp.tileIndexToFieldIndex(field.suggestTileCoord()))
                vf2.set(ctx.cp.toScreenCellCorner(c2).add(0f, -ctx.cp.sideLen / 2))
                seq
                    .push(Tween.to(hand, TW_POS_XY, 0.5f).target(v.x, v.y))
                    .pushPause(0.3f)
                    .push(Tween.call { _, _ ->
                        inAutoMoveTile = true
                        //dragStart.set(hand.x, hand.y)
                    })
                    .push(Tween.to(hand, TW_POS_XY, 1f).target(vf2.x, vf2.y))
                    .pushPause(0.1f)
                    .push(Tween.call { _, _ ->
                        inAutoMoveTile = false
                        dropNewTileToField(c2, nT)
                    })
                    .pushPause(0.3f)
            }
            MoveTarget.Selector -> {
                vf2.set(field.suggestOpenSelectorFieldCoord())
                v.set(ctx.field.project(v.set(vf2))).sub(ctx.cp.sideLen / 2, ctx.cp.sideLen)
                seq.push(Tween.to(hand, TW_POS_XY, 1f).target(v.x, v.y))
                    .pushPause(0.3f)
                    .push(Tween.call { _, _ -> field.selectorsHitTest(vf2) })
                    .pushPause(0.3f)
            }
            MoveTarget.EndOfTurn -> {
                ok.boundingRectangle.getCenter(v)
                v.sub(ctx.cp.sideLen / 2, ctx.cp.sideLen)
                seq.push(Tween.to(hand, TW_POS_XY, 1f).target(v.x, v.y))
                    .pushPause(0.3f)
                    .push(Tween.call { _, _ ->
                        endOfTurn()
                        inAutoMove = false
                    })
                    .pushPause(0.3f)
            }
        }
        if (mt != MoveTarget.EndOfTurn)
            seq.setCallback { _, _ ->
                if (newTile == null && field.noMoreSelectors() && !field.noMoreBalls())
                    endOfTurn()
                inAutoMove = false
            }
        inAutoMove = true
        hand.setOriginBasedPosition(help.x, help.y)
        seq.start(ctx.tweenManager)
    }

    /**
     * Serialize the game contents
     */
    fun serialize(sb: StringBuilder) {
        ctx.serialize(sb)
        ctx.cp.serialize(sb)
        shredder.serialize(sb)
        if (newTile == null) sb.append("-") else newTile?.serialize(sb)
        field.serialize(sb)
    }

    /**
     * Deserialize the game contents
     */
    private fun deserialize(s: String, i: Int) {
        var j = ctx.deserialize(s, i)
        j = ctx.cp.deserialize(s, j)
        j = shredder.deserialize(s, j)
        if (s[j] == '-') j++ else newTile = Tile().apply { j = this.deserialize(s, j) }
        field = Field(ctx).apply { this.deserialize(s, j) }
        resize(graphics.width, graphics.height)
    }

    /**
     * Our input adapter
     */
    inner class IAdapter : InputAdapter() {
        private var inFieldDrag = false
        private val prevDragPos = Vector2()
        private val v = Vector2()
        private val c = Coord()

        /**
         * Invoked when the player drags something on the screen.
         */
        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            if (inAnimation) return super.touchDragged(screenX, screenY, pointer)
            prevDragPos.set(dragPos)
            dragPos.set(ctx.cp.pointerPositionScreen(screenX, screenY))
            c.set(ctx.cp.toScreenIndex(dragPos))
            if (dragFrom == DragSource.None) {
                dragStart.set(dragPos)
                if (c.isSet()) dragFrom = DragSource.Board
                else if (dragPos.dst(newTilePos) < ctx.cp.sideLen) dragFrom = DragSource.NewTile
            }
            if (dragFrom == DragSource.NewTile) floatingTile.position.set(dragPos)
                .sub(ctx.cp.sideLen / 2, ctx.cp.sideLen / 2)
            if (dragFrom == DragSource.Board) {
                if (c.isNotSet()) {
                    inFieldDrag = false
                    return super.touchDragged(screenX, screenY, pointer)
                }
                if (!inFieldDrag) {
                    inFieldDrag = true
                    return super.touchDragged(screenX, screenY, pointer)
                }
                v.set(dragPos).sub(prevDragPos)
                panFieldBy(v.x, v.y)
            }
            return super.touchDragged(screenX, screenY, pointer)
        }

        /**
         * Called when screen is untouched (mouse button released)
         */
        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            if (inAnimation) return super.touchDragged(screenX, screenY, pointer)
            if (button == Input.Buttons.RIGHT) { // Right click (on desktop)
                endOfTurn()
                return super.touchDragged(screenX, screenY, pointer)
            }
            val v = ctx.cp.pointerPositionScreen(screenX, screenY)
            when {
                buttonTouched(v, play) -> newGame(false)
                buttonTouched(v, exit) -> Gdx.app.exit()
                buttonTouched(v, settings) -> ctx.game.setScreen<HomeScreen>()
                buttonTouched(v, ok) -> endOfTurn()
                buttonTouched(v, help) -> autoMove()
                else -> if (dragFrom == DragSource.None || dragFrom == DragSource.NewTile || dragStart.dst(dragPos) < 4)
                // The last condition is a safeguard against clicks with minor pointer slides that are erroneously
                // interpreted as drags
                    setSelectorOrPutNewTileAt(
                        ctx.cp.pointerPositionField(screenX, screenY),
                        dragFrom == DragSource.NewTile
                    )
            }
            inFieldDrag = false
            prevDragPos.set(Vector2.Zero)
            dragPos.set(Vector2.Zero)
            dragFrom = DragSource.None
            newTile?.setDefaultNewTileBasePos()
            return super.touchUp(screenX, screenY, pointer, button)
        }

        /**
         * Handle mouse wheel for field scrolling and panning on desktop
         */
        override fun scrolled(amountX: Float, amountY: Float): Boolean {
            if (input.isKeyPressed(Input.Keys.CONTROL_LEFT) || input.isKeyPressed(Input.Keys.CONTROL_RIGHT))
                ctx.fieldScale = ctx.fieldScale - amountY / 5f
            else
                panFieldBy(amountX * ctx.cp.sideLen, amountY * ctx.cp.sideLen)
            return super.scrolled(amountX, amountY)
        }

        /**
         * Checks if particular button is touched.
         */
        private fun buttonTouched(v: Vector2, s: Sprite) = v.x in s.x..s.x + s.width && v.y in s.y..s.y + s.height

    }

    /**
     * Our gesture adapter, for field zooming
     */
    inner class GAdapter : GestureAdapter() {
        /**
         * Handle long-press event on the field
         */
        override fun longPress(x: Float, y: Float): Boolean {
            if (withinTheField(x, y))
                ctx.fieldScale = if (ctx.fieldScale <= 1) 2f else 1f
            return super.longPress(x, y)
        }

        private fun withinTheField(x: Float, y: Float) =
            x in basePos.x..basePos.x + ctx.cp.wholeFieldSize && y in basePos.y..basePos.y + ctx.cp.wholeFieldSize

        private var initialDistance = -1f
        private val shift = Vector2()
        private val initialWorldCenter = Vector2()

        /**
         * Handle pinch-zoom scaling
         */
        override fun pinch(
            initialPointer1: Vector2?,
            initialPointer2: Vector2?,
            pointer1: Vector2?,
            pointer2: Vector2?
        ): Boolean {
            if (initialPointer1 == null || initialPointer2 == null || pointer1 == null || pointer2 == null ||
                !(withinTheField(initialPointer1.x, initialPointer1.y) || withinTheField(
                    initialPointer2.x,
                    initialPointer2.y
                ))
            )
                return super.pinch(initialPointer1, initialPointer2, pointer1, pointer2)
            if (initialDistance < 0) {
                initialDistance = initialPointer1.dst(initialPointer2)
                if (initialDistance <= 1f) {
                    initialDistance = -1f
                    return super.pinch(initialPointer1, initialPointer2, pointer1, pointer2)
                }
                ctx.field.unproject(
                    initialWorldCenter.set(initialPointer2).sub(initialPointer1).setLength(initialDistance / 2)
                        .add(initialPointer1)
                )
            }
            val distance = pointer1.dst(pointer2)
            var k = distance / initialDistance
            if (k > 1) k = 1 + (k - 1) / 2f
            else if (k < 1) k = 1 - (1 - k) / 2f
            ctx.fieldScale *= k
            ctx.moveFieldCameraBy(
                ctx.field.unproject(shift.set(pointer2).sub(pointer1).setLength(distance / 2).add(pointer1))
                    .sub(initialWorldCenter)
            )
            val cshift = normalizeScrollOffcetByCells()
            if (cshift.isNotZero()) with(initialWorldCenter) {
                set(
                    x + cshift.x * ctx.cp.sideLen,
                    y + cshift.y * ctx.cp.sideLen
                )
            }
            return true // Do not pass processing to the IAdapter drag processing
        }

        /**
         * Pinch-zooming ended
         */
        override fun pinchStop() {
            super.pinchStop()
            initialDistance = -1f
            //normalizeScrollOffcetByCells()
        }

    }

}
