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
     * The field of tiles (not all of them are set)
     */
    private val tile: Array<Array<Tile?>> = Array(ctx.gs.fieldWidth) { Array(8) { null } }

    private val balls = mutableListOf<Ball>()
    private val deadBalls = mutableListOf<Ball>()

    private var _sideLen: Float = 0f

    /**
     * The tile square side length. Used to calculate screen positions of the segment parts.
     * Changes only on the window resize.
     */
    var sideLen: Float
        get() = _sideLen
        set(value) {
            _sideLen = value
            tile.flatten().filterNotNull().forEach { it.sideLen = value }
        }

    /**
     * Fill the field with tiles on new game start.
     */
    private fun createInitialTiles() = tile.indices.forEach { x -> (0..3).forEach { y -> putTile(x, y, Tile()) } }

    /**
     * Prepare field for new game.
     */
    fun newGame() {
        createInitialTiles()
        createInitialBalls()
        initTracks()
    }

    /**
     * Init the tracks on startup.
     */
    private fun initTracks() {
        val movingBalls = balls.filter { it.segment != null }.map { Ball(it) }.toMutableList()
        while (movingBalls.isNotEmpty()) {
            val ballsToClear = mutableListOf<Ball>()
            for (b in movingBalls) {
                if (b.segment!!.color[0] != 0) {
                    // Another ball has moved here before us, do not plan the track further
                    ballsToClear.add(b)
                    continue
                }
                if (b in ballsToClear)
                // This ball has been already processed in conflict with another ball. Skip it.
                    continue
                // Color current segments
                val seg = b.segment!!
                val pretenders = movingBalls.filter { it.segment == seg }
                if (pretenders.size == 1) {
                    // The path is clear. Color the whole segment by this ball
                    seg.setColorFrom(b)
                    continue
                }
                // There are other balls claiming the same segment.
                val (fromSideA, fromSideB) = pretenders.partition { it.movingFromSide == b.movingFromSide }
                if (fromSideA.size > 1) // Collision at side A. No balls income from here
                    ballsToClear.addAll(fromSideA)
                if (fromSideB.size > 1) // Collision at side B. No balls income from here
                    ballsToClear.addAll(fromSideB)
                if (fromSideA.size == 1 && fromSideB.size == 1) {
                    // Two balls, one from each side, will collide at the middle of this segment
                    seg.split = 0.5f
                    val bA = fromSideA.first()
                    val side = seg.behindSideIndex(bA)
                    seg.color[side] = bA.color
                    seg.color[1 - side] = fromSideB.first().color
                    ballsToClear.addAll(fromSideA)
                    ballsToClear.addAll(fromSideB)
                } else if (fromSideA.size == 1)
                // After collisions, only side A ball remains and claims this segment
                    seg.setColorFrom(fromSideA.first())
                else if (fromSideB.size == 1)
                // After collisions, only side B ball remains and claims this segment
                    seg.setColorFrom(fromSideB.first())
            }
            movingBalls.removeAll(ballsToClear)
            ballsToClear.clear()
            movingBalls.forEach { if (advanceToNextTile(it) != NextTileState.Ok) ballsToClear.add(it) }
            movingBalls.removeAll(ballsToClear)
        }
    }

    /**
     * Possible states ov moving the ball to next tile
     */
    private enum class NextTileState {
        Ok, NoNextTile, NeedsSelector
    }

    /**
     * Moves the ball to the beginning of next tile according to the segment direction.
     */
    private fun advanceToNextTile(ball: Ball): NextTileState {
        val movingToSide = ball.segment!!.type.sides.first { it != ball.movingFromSide }
        val coord = ball.segment!!.tile.coord!!
        val nextTile: Tile? = when (movingToSide) {
            TrackSegment.Side.Top -> if (coord.y == 7) null else tile[coord.x][coord.y + 1]
            TrackSegment.Side.Right ->
                if (coord.x == ctx.gs.fieldWidth - 1) tile[0][coord.y] else tile[coord.x + 1][coord.y]
            TrackSegment.Side.Bottom -> if (coord.y == 0) null else tile[coord.x][coord.y - 1]
            TrackSegment.Side.Left ->
                if (coord.x == 0) tile[ctx.gs.fieldWidth - 1][coord.y] else tile[coord.x - 1][coord.y]
        }
        if (nextTile == null) {
            ball.position = 1f
            return NextTileState.NoNextTile
        }
        ball.tile = nextTile
        ball.position = 0f
        ball.movingFromSide = movingToSide.otherSide
        val seg = nextTile.segments.filter { it.type.sides.contains(ball.movingFromSide) }
        if (seg.size == 1) {
            ball.segment = seg.first()
            return NextTileState.Ok
        }
        ball.segment = null
        return NextTileState.NeedsSelector
    }

    /**
     * Initial balls placement.
     */
    private fun createInitialBalls() {
        var tilePos = 1
        (1..ctx.gs.ballsCount).forEach {
            balls.add(Ball(it, tile[tilePos][2] ?: return@forEach))
            tilePos += 2
        }
    }

    /**
     * Put tile to field.
     */
    fun putTile(x: Int, y: Int, t: Tile) {
        tile[x][y] = t
        t.coord = Coord(x, y)
    }

    fun render(basePos: Vector2) {
        val v = Vector2()
        tile.flatten().filter { it?.coord != null }
            .forEach { it!!.render(ctx, v.set(basePos).add(it.coord!!.x * sideLen, it.coord!!.y * sideLen)) }
        balls.forEach { it.render(ctx, basePos) }
    }
}