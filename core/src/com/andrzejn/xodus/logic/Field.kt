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
        Array(ctx.gs.fieldSize) { x -> Array(ctx.gs.fieldSize) { y -> Tile().also { it.coord.set(x, y) } } }

    /**
     * Active balls on the field
     */
    private val ball = mutableListOf<Ball>()

    /**
     * The balls removed from the field (but they may reappear, according to the game settings
     * and game conditions
     */
    private val deadBall = mutableListOf<Ball>()

    private var _sideLen: Float = 0f

    /**
     * The tile square side length. Used to calculate screen positions of the segment parts.
     * Changes only on the window resize.
     */
    val sideLen: Float
        get() = _sideLen

    /**
     * Update sideLen and base coordinates
     */
    fun setSideLen(value: Float, basePos: Vector2) {
        _sideLen = value
        tile.flatten().forEach { it.setSideLen(value, basePos) }
        ball.plus(deadBall).forEach { it.sideLen = value }
    }

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
        tile.flatten().forEach { it.clearIntents() } // Clear old plans
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
                val sideIndex = b.movingFromSide.ordinal
                val sideIntent = b.tile.intent[sideIndex]
                if (sideIntent.color != 0) {
                    // This side is already included into some planned track. Proceed no further.
                    ballsToClear.add(b)
                    continue
                }
                sideIntent.color = b.color
                sideIntent.trackStep = step
                if (b.segment != null) // The ball already knows its direction. Record it to the intent.
                    sideIntent.segment = b.segment
                else if (sideIntent.segment != null)
                // The side already has the (only one possible) direction intent. Use it.
                    b.segment = sideIntent.segment
                else {
                    val sideSelector = b.tile.selector[sideIndex]
                    if (sideSelector.color != b.color) {
                        // The selector was not set or was set for another ball, but plans have changed now
                        sideSelector.reset()
                        sideSelector.color = b.color
                    }
                    // The selector at this side is either already set by the player for this ball or may have
                    // a single direction only. Use that direction, then.
                    sideIntent.segment = sideSelector.segment
                    b.segment = sideSelector.segment
                }
                if (b.segment == null) // We don't know where to move further. Stop planning for that ball.
                    ballsToClear.add(b)
            }
            movingBalls.removeAll(ballsToClear)
            ballsToClear.clear()
            movingBalls.forEach { advanceToNextTile(it) }
            step++
        }
        // We have planned all intents for moving balls.
        // Clear obsolete selectors
        tile.flatten().forEach { t ->
            t.selector.forEachIndexed { i, s -> if (s.color != 0 && s.color != t.intent[i].color) s.reset() }
        }
        // Now color the segments according to intents
        tile.flatten().forEach { t ->
            t.intent.mapNotNull { it.segment }.distinct().forEach { s ->
                val intent0 = t.intent[s.type.sides[0].ordinal]
                val intent1 = t.intent[s.type.sides[1].ordinal]

                if (intent0.segment == s && intent1.segment != s) {
                    // Side 1 has no claim over this segment
                    s.color[0] = intent0.color
                    s.color[1] = intent0.color
                    s.split = 0f
                } else if (intent1.segment == s && intent0.segment != s) {
                    // Side 0 has no claim over this segment
                    s.color[0] = intent1.color
                    s.color[1] = intent1.color
                    s.split = 0f
                } else if (intent0.color == 0 || intent0.color == intent1.color ||
                    (intent0.trackStep > intent1.trackStep && intent1.trackStep > 0)
                ) {
                    // Side 0 has no claim, or both sides claim the same color, or side 1 was here earlier
                    s.color[0] = intent1.color
                    s.color[1] = intent1.color
                    s.split = 0f
                } else if (intent1.color == 0 || (intent1.trackStep > intent0.trackStep && intent0.trackStep > 0)) {
                    // Side 1 has no claim or side 0 was here earlier
                    s.color[0] = intent0.color
                    s.color[1] = intent0.color
                    s.split = 0f
                } else { // Sides claim different colors and came here at the same time
                    s.color[0] = intent0.color
                    s.color[1] = intent1.color
                    s.split = 0.5f
                }
            }
        }
        tile.flatten().forEach { it.sortSegments() }
    }

    /**
     * Moves the ball to the beginning of next tile according to the segment direction.
     */
    private fun advanceToNextTile(ball: Ball) {
        val currentSegment = ball.segment ?: return
        val movingToSide = currentSegment.type.sides.first { it != ball.movingFromSide }
        val current = currentSegment.tile.coord
        val maxCoord = ctx.gs.fieldSize - 1
        ball.tile = when (movingToSide) {
            Side.Top -> if (current.y == maxCoord) tile[current.x][0] else tile[current.x][current.y + 1]
            Side.Right -> if (current.x == maxCoord) tile[0][current.y] else tile[current.x + 1][current.y]
            Side.Bottom -> if (current.y == 0) tile[current.x][maxCoord] else tile[current.x][current.y - 1]
            Side.Left -> if (current.x == 0) tile[maxCoord][current.y] else tile[current.x - 1][current.y]
        }
        ball.position = 0f
        ball.movingFromSide = movingToSide.otherSide
        ball.segment = null
    }

    /**
     * Render everything on the field
     */
    fun render(basePos: Vector2) {
        val v = Vector2()
        tile.flatten().forEach { it.render(ctx, v.set(basePos)) }
        ball.forEach { it.render(ctx) }
    }
}