package com.andrzejn.xodus.logic

import aurelienribon.tweenengine.Tween
import com.andrzejn.xodus.Context
import com.andrzejn.xodus.helper.TW_POSITION
import com.badlogic.gdx.math.Vector2

/**
 * The playfield, with tiles, balls, tracks and logic
 */
class Field(
    /**
     * Reference to the app context
     */
    private val ctx: Context
) {
    /**
     * The field of tiles
     */
    private val tile: Array<Array<Tile>> =
        Array(ctx.gs.fieldSize) { x -> Array(ctx.gs.fieldSize) { y -> Tile().apply { coord.set(x, y) } } }

    /**
     * Active balls on the field
     */
    private val ball = mutableListOf<Ball>()

    /**
     * The balls removed from the field (but they may reappear, according to the game settings
     * and game conditions
     */
    private val deadBall = mutableSetOf<Ball>()

    /**
     * The balls that are going on collision course. They will be killed during the move
     */
    private val ballsOnCollisionCourse = mutableSetOf<Ball>()

    /**
     * List of selectors that the player can (and should) select.
     */
    private val openSelector = mutableListOf<MoveIntent>()

    /**
     * The list of selector colors already clicked on this move.
     * Cleared after placing a tile before advancing the bakk.
     */
    private val clickedSelectorColors = mutableListOf<Int>()

    /**
     * Blots on the field. Fading out at the end of each move. Blots with alpha<=0 should be removed.
     */
    private val blot = mutableListOf<Blot>()

    /**
     * Cell side length, for scaling on render
     */
    private var sideLen: Float = 0f

    /**
     * Update sideLen and base coordinates
     */
    fun setSideLen(value: Float, setBasePos: (Tile) -> Unit) {
        sideLen = value
        applyToAllTiles {
            setBasePos(it)
            it.setSideLen(value)
        }
        ball.plus(deadBall).forEach { it.sideLen = value }
    }

    /**
     * Convenience property to shorten code
     */
    private val flatTile: List<Tile> get() = tile.flatten()

    /**
     * Apply given lambda to all tiles
     */
    fun applyToAllTiles(lambda: (Tile) -> Unit): Unit = flatTile.forEach { lambda(it) }

    /**
     * Prepare field for new game.
     */
    fun newGame() {
        createInitialBalls()
        planTracks()
    }

    /**
     * Initial balls placement.
     */
    private fun createInitialBalls() {
        var tilePos = 1
        (1..(ctx.gs.fieldSize - 1) / 2).forEach {
            ball.add(Ball(it, tile[tilePos][2]))
            tilePos += 2
        }
    }

    /**
     * Init/update the planned ball tracks.
     */
    private fun planTracks() {
        applyToAllTiles { // Clear old plans
            it.clearIntents()
            it.clearSegmentColors()
        }
        val movingBalls = ball.map { Ball(it) }.toMutableList()
        var step = 1
        openSelector.clear()
        val ballsToClear = mutableSetOf<Ball>()
        while (movingBalls.isNotEmpty()) {
            movingBalls.removeAll(collided(movingBalls))
            movingBalls.forEach { b -> if (setIntents(b, step)) ballsToClear.add(b) }
            movingBalls.removeAll(ballsToClear)
            ballsToClear.clear()
            movingBalls.removeAll(onCollisionCourse(movingBalls))
            movingBalls.forEach { advanceToNextTile(it) }
            step++
        }
        // We have planned all intents for moving balls.
        // Clear obsolete selectors
        flatTile.flatMap { it.intent.toList() }
            .filter { it.selectorColor != 0 && it.selectorColor != it.intentSideColor }.forEach { it.resetSelector() }
        applyToAllTiles { t ->
            t.intent.mapNotNull { it.intentSegment }.distinct().forEach { s -> colorSegmentByIntents(t, s) }
        }
        applyToAllTiles { it.sortSegments() } // Ensure that colored segments are drawn over uncolored
        openSelector.sortByDescending { it.trackStep } // If some selectors visually overlap, give priority to ones
        // that will be needed earlier
    }

    /**
     * Set move intents for the ball at given planning position
     */
    private fun setIntents(
        b: Ball,
        step: Int
    ): Boolean {
        val sideIntent = b.tile.intent[b.movingFromSide.ordinal]
        if (sideIntent.intentSideColor != 0 || with(
                otherSide(
                    b.tile,
                    b.movingFromSide
                )
            ) { first.intent[second.ordinal] }.intentSideColor != 0
        ) return true
        // This side is already included into some planned track. Proceed no further.
        sideIntent.intentSideColor = b.color
        sideIntent.trackStep = step
        if (b.segment != null) // The ball already knows its direction. Record it to the intent.
            sideIntent.intentSegment = b.segment
        else if (sideIntent.intentSegment != null)
        // The side already has the (only one possible) direction intent. Use it.
            b.segment = sideIntent.intentSegment
        else { // Default intent checks are over. Check the selector.
            if (sideIntent.selectorColor != b.color) {
                // The selector was not set or was set for another ball, but plans have changed now
                sideIntent.resetSelector()
                sideIntent.selectorColor = b.color
            }
            // The selector at this side is either already set by the player for this ball or may have
            // a single direction only. Use that direction, then.
            sideIntent.intentSegment = sideIntent.selectorSegment
            b.segment = sideIntent.selectorSegment
        }
        if (b.segment == null) { // If we don't know where to move further then stop planning for that ball.
            if (b.color !in clickedSelectorColors) openSelector.add(sideIntent)
            return true
        }
        return false
    }

    /**
     * Color the segment accordint to the plan
     */
    private fun colorSegmentByIntents(t: Tile, s: TrackSegment) {
        val intent0 = t.intent[s.type.sides[0].ordinal]
        val intent1 = t.intent[s.type.sides[1].ordinal]

        if (intent0.intentSegment == s && intent1.intentSegment != s) {
            // Side 1 has no claim over this segment
            s.color[0] = intent0.intentSideColor
            s.color[1] = intent0.intentSideColor
            s.split = 0f
        } else if (intent1.intentSegment == s && intent0.intentSegment != s) {
            // Side 0 has no claim over this segment
            s.color[0] = intent1.intentSideColor
            s.color[1] = intent1.intentSideColor
            s.split = 0f
        } else if (intent0.intentSideColor == 0 || intent0.intentSideColor == intent1.intentSideColor ||
            (intent0.trackStep > intent1.trackStep && intent1.trackStep > 0)
        ) {
            // Side 0 has no claim, or both sides claim the same color, or side 1 was here earlier
            s.color[0] = intent1.intentSideColor
            s.color[1] = intent1.intentSideColor
            s.split = 0f
        } else if (intent1.intentSideColor == 0 ||
            (intent1.trackStep > intent0.trackStep && intent0.trackStep > 0)
        ) {
            // Side 1 has no claim or side 0 was here earlier
            s.color[0] = intent0.intentSideColor
            s.color[1] = intent0.intentSideColor
            s.split = 0f
        } else { // Sides claim different colors and came here at the same time
            s.color[0] = intent0.intentSideColor
            s.color[1] = intent1.intentSideColor
            s.split = 0.5f
        }
    }

    /**
     * List of balls entered the same segment. They are either at the same side (and have collided already)
     * or on other side (and will collide on next move)
     */
    private fun onCollisionCourse(balls: List<Ball>) = balls.filter { b1 ->
        balls.any { b2 ->
            b1 != b2 && b1.segment != null && b1.segment == b2.segment
        }
    }

    /**
     * List of balls collided at the same tile side.
     */
    private fun collided(balls: List<Ball>) = balls.filter { b1 ->
        balls.any { b2 ->
            b1 != b2 && ((b1.tile == b2.tile && b1.movingFromSide == b2.movingFromSide) || (with(
                otherSide(
                    b1.tile, b1.movingFromSide
                )
            ) { b2.tile == first && b2.movingFromSide == second }))
        }
    }

    /**
     * Move balls by one position. Returns true if there are no open selectors to select and need to create new tile.
     */
    fun advanceBalls(callback: () -> Unit) {
        killBalls(collided(ball))
        ballsOnCollisionCourse.clear()
        ballsOnCollisionCourse.addAll(onCollisionCourse(ball))
        ballPosition = 0f
        ball.forEach { it.segment = segmentFromTileSide(it) }
        blot.forEach { it.fade() }
        blot.removeIf { it.alpha <= 0 }
        Tween.to(this, TW_POSITION, 1f).target(1f)
            .setCallback { _, _ ->
                ball.forEach { advanceToNextTile(it) }
                ballPosition = 0f
                killBalls(collided(ball))
                clickedSelectorColors.clear()
                planTracks()
                callback()
            }
            .start(ctx.tweenManager)
    }

    private fun killBalls(collisions: List<Ball>) {
        if (collisions.isEmpty())
            return
        deadBall.addAll(collisions)
        ball.removeAll(collisions)
        collisions.distinct().forEach { blot.add(Blot(ctx, it.color, it.tile, it.currentPosition)) }
        //TODO if ball.isEmpty then trigger endgame
    }

    /**
     * Moves the ball to the beginning of next tile according to the segment direction.
     */
    private fun advanceToNextTile(ball: Ball) {
        with(
            otherSide(ball.tile,
                (ball.segment ?: segmentFromTileSide(ball) ?: return).type.sides.first { it != ball.movingFromSide })
        ) {
            ball.tile = first
            ball.movingFromSide = second
        }
        ball.segment = segmentFromTileSide(ball)
        ball.position = 0f
    }

    /**
     * Returns the other side of the given tile side
     */
    private fun otherSide(
        currentTile: Tile, currentSide: Side
    ): Pair<Tile, Side> {
        val currentCoord = currentTile.coord
        return when (currentSide) {
            Side.Top -> tile[currentCoord.x][ctx.clipWrap(currentCoord.y + 1)]
            Side.Right -> tile[ctx.clipWrap(currentCoord.x + 1)][currentCoord.y]
            Side.Bottom -> tile[currentCoord.x][ctx.clipWrap(currentCoord.y - 1)]
            Side.Left -> tile[ctx.clipWrap(currentCoord.x - 1)][currentCoord.y]
        } to currentSide.otherSide
    }

    private fun segmentFromTileSide(ball: Ball): TrackSegment? {
        with(ball.tile.intent[ball.movingFromSide.ordinal]) {
            return intentSegment ?: (if (selectorColor == ball.color) selectorSegment else null)
        }
    }

    /**
     * Current tween position of movong balls
     */
    var ballPosition: Float = 0f

    /**
     * Render everything on the field
     */
    fun render() {
        blot.forEach { it.render() }
        applyToAllTiles { it.render(ctx) }
        val ballsToClear = mutableListOf<Ball>()
        ball.forEach {
            it.position = ballPosition
            it.render(ctx)
            if (ballPosition >= 0.5f && it in ballsOnCollisionCourse) {
                ballsOnCollisionCourse.remove(it)
                ballsToClear.add(it)
            }
        }
        killBalls(ballsToClear)
        openSelector.forEach { it.render(ctx) }
    }

    /**
     * Check if the given pointer screen coordinates match some of the active selector arrows.
     * It there is a match, respective selector is set and the track extended.
     * Returns true if there was the selector match.
     */
    fun selectorsHitTest(v: Vector2): Boolean {
        openSelector.toTypedArray().reversed().forEach {
            if (it.selectorClicked(v)) {
                clickedSelectorColors.add(it.selectorColor)
                planTracks()
                return true
            }
        }
        return false
    }

    /**
     * Put given tile to the specified field cell. Updates everything for next move
     */
    fun putTile(t: Tile, x: Int, y: Int) {
        val oldTile = tile[x][y]
        t.coord.set(x, y)
        t.basePos.set(oldTile.basePos)
        tile[x][y] = t
        ball.filter { it.tile == oldTile }.forEach {
            it.tile = t
            it.segment = null
        }
        blot.filter { it.baseTile == oldTile }.forEach { it.baseTile = t }
        planTracks()
    }

    /**
     * Return indexes of random tile that has any colored track.
     */
    fun chaosTileCoord(): Coord {
        val fTile = flatTile
        return fTile.filter { it.segment.any { s -> s.color.any { c -> c != 0 } } }.ifEmpty { fTile }.random().coord
    }

    /**
     * Are there no more selectable selectors
     */
    fun noMoreSelectors(): Boolean = openSelector.isEmpty()

    /**
     * Kill shredded balls
     */
    fun shredBalls(shredded: (List<Ball>) -> List<Ball>): Unit = killBalls(shredded(ball))

}