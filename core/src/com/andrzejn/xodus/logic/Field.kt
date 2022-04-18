package com.andrzejn.xodus.logic

import com.andrzejn.xodus.Context
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
    private val deadBall = mutableListOf<Ball>()

    /**
     * List of selectors that the player can (and should) select.
     */
    private val openSelector = mutableListOf<MoveIntent>()

    /**
     * The list of selector colors already clicked on this move.
     * Cleared after placing a tile before advancing the bakk.
     */
    private val clickedSelectorColors = mutableListOf<Int>()

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
     * Apply given lambda to all tiles
     */
    fun applyToAllTiles(lambda: (Tile) -> Unit): Unit = tile.flatten().forEach { lambda(it) }

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
        applyToAllTiles {
            it.clearIntents()
            it.clearSegmentColors()
        } // Clear old plans
        val movingBalls = ball.map { Ball(it) }.toMutableList()
        var step = 1
        while (movingBalls.isNotEmpty()) {
            // Remove balls that collided at the same point - they die here and have no further tracks
            val ballsToClear = movingBalls.filter { b1 ->
                movingBalls
                    .any { b2 -> b1 != b2 && b1.tile == b2.tile && b1.movingFromSide == b2.movingFromSide }
            }.toMutableSet()
            movingBalls.removeAll(ballsToClear)
            ballsToClear.clear()
            // Now record moving intents of remaining balls
            for (b in movingBalls) {
                val sideIntent = b.tile.intent[b.movingFromSide.ordinal]
                if (sideIntent.intentSideColor != 0) {
                    // This side is already included into some planned track. Proceed no further.
                    ballsToClear.add(b)
                    continue
                }
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
                if (b.segment == null) // We don't know where to move further. Stop planning for that ball.
                    ballsToClear.add(b)
            }
            // Clear all balls that entered the same segment. They will collide on nearest advance.
            ballsToClear.addAll(movingBalls.filter { b1 ->
                movingBalls.any { b2 -> b1 != b2 && b1.segment != null && b1.segment == b2.segment }
            })
            movingBalls.removeAll(ballsToClear)
            ballsToClear.clear()
            movingBalls.forEach { advanceToNextTile(it) }
            step++
        }
        // We have planned all intents for moving balls.
        // Clear obsolete selectors
        tile.flatten().flatMap { it.intent.toList() }
            .filter { it.selectorColor != 0 && it.selectorColor != it.intentSideColor }.forEach { it.resetSelector() }
        // Now color the segments according to intents
        applyToAllTiles { t ->
            t.intent.mapNotNull { it.intentSegment }.distinct().forEach { s ->
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
                } else if (intent1.intentSideColor == 0 || (intent1.trackStep > intent0.trackStep && intent0.trackStep > 0)) {
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
        }
        applyToAllTiles { it.sortSegments() }
        openSelector.clear()
        openSelector.addAll(tile.flatten().flatMap { it.intent.toList() }
            .filter {
                it.selectorColor != 0 && it.selectorSegment == null && it.selectorColor !in clickedSelectorColors
            }.sortedByDescending { it.trackStep })
    }

    /**
     * Moves the ball to the beginning of next tile according to the segment direction.
     */
    private fun advanceToNextTile(ball: Ball) {
        val currentSegment = ball.segment ?: segmentFromTileSide(ball) ?: return
        val movingToSide = currentSegment.type.sides.first { it != ball.movingFromSide }
        val current = currentSegment.tile.coord
        ball.tile = when (movingToSide) {
            Side.Top -> tile[current.x][ctx.clipWrap(current.y + 1)]
            Side.Right -> tile[ctx.clipWrap(current.x + 1)][current.y]
            Side.Bottom -> tile[current.x][ctx.clipWrap(current.y - 1)]
            Side.Left -> tile[ctx.clipWrap(current.x - 1)][current.y]
        }
        ball.movingFromSide = movingToSide.otherSide
        ball.segment = segmentFromTileSide(ball)
        ball.position = 0f
    }

    private fun segmentFromTileSide(ball: Ball): TrackSegment? {
        with(ball.tile.intent[ball.movingFromSide.ordinal]) {
            return intentSegment ?: (if (selectorColor == ball.color) selectorSegment else null)
        }
    }

    /**
     * Render everything on the field
     */
    fun render() {
        applyToAllTiles { it.render(ctx) }
        ball.forEach { it.render(ctx) }
        openSelector.forEach { it.render(ctx) }
    }

    /**
     * Check if the given pointer screen coordinates match some of the active selector arrows.
     * It there is a march, respective selector is set.
     */
    fun selectorsHitTest(v: Vector2) {
        openSelector.toTypedArray().reversed().forEach {
            if (it.selectorClicked(v)) {
                clickedSelectorColors.add(it.selectorColor)
                planTracks()
                return
            }
        }
    }

}