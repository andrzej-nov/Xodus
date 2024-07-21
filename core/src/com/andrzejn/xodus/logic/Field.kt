package com.andrzejn.xodus.logic

import aurelienribon.tweenengine.Tween
import com.andrzejn.xodus.Context
import com.andrzejn.xodus.helper.TW_POSITION
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.StringBuilder
import kotlin.math.abs

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
    val openSelector: MutableList<MoveIntent> = mutableListOf()

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
    private fun applyToAllTiles(lambda: (Tile) -> Unit): Unit = flatTile.forEach { lambda(it) }

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
     * Set basePos for all tiles according to current scroll position.
     */
    fun updateTilePositions(): Unit = applyToAllTiles { t ->
        ctx.cp.setTileBasePos(t.coord, t.basePos)
        t.intent.forEach { i -> i.resetSelectorArrows() }
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
            movingBalls.removeAll(collided(movingBalls).toSet())
            movingBalls.forEach { b -> if (setIntents(b, step)) ballsToClear.add(b) }
            movingBalls.removeAll(ballsToClear)
            ballsToClear.clear()
            movingBalls.removeAll(onCollisionCourse(movingBalls).toSet())
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
        ) return true // This side is already included into some planned track. Proceed no further.
        sideIntent.intentSideColor = b.color
        sideIntent.trackStep = step
        if (b.segment != null) // The ball already knows its direction. Record it to the intent.
            sideIntent.intentSegment = b.segment
        else if (sideIntent.intentSegment != null) // The side already has the (only one possible) direction intent.
            b.segment = sideIntent.intentSegment // Use it.
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
            if (step < 3 && b.color !in clickedSelectorColors) openSelector.add(sideIntent)
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
    fun advanceBalls(chaosMoves: () -> Unit, distFromShredder: (List<Ball>) -> Map<Int, Float>) {
        killBalls(collided(ball))
        ballsOnCollisionCourse.clear()
        ballsOnCollisionCourse.addAll(onCollisionCourse(ball))
        ballPosition = 0f
        ball.forEach { it.segment = segmentFromTileSide(it) }
        if (deadBall.isNotEmpty() && ctx.gs.reincarnation) {
            val currentBallSegments = ball.mapNotNull { it.segment }
            val dist = distFromShredder(ball)
            ball.fold(mutableListOf<Pair<Side, TrackSegment>>()) { l, b ->
                l.addAll(b.tile.segment.filter { s ->
                    s.type.sides.contains(b.movingFromSide) && s.color.all { it == 0 } && s !in currentBallSegments
                }.map { (b.movingFromSide to it) to abs(dist[b.color]!!) }.sortedByDescending { it.second }
                    .map { it.first })
                l
            }.zip(deadBall).forEach { (ss, b) ->
                b.movingFromSide = ss.first
                b.segment = ss.second
                b.tile = ss.second.tile
                ball.add(b)
                deadBall.remove(b)
            }
        }
        blot.forEach { it.fade() }
        blot.removeAll(blot.filter { it.alpha <= 0 }.toSet())
        prepareBallSegmentColors()
        Tween.to(this, TW_POSITION, 1f).target(1f)
            .setCallback { _, _ ->
                ball.forEach { advanceToNextTile(it) }
                ballPosition = 0f
                killBalls(collided(ball))
                clickedSelectorColors.clear()
                planTracks()
                chaosMoves()
            }
            .start(ctx.tweenManager)
    }

    /**
     * Prepare current ball segment end colors to correctly split when the balls move
     */
    private fun prepareBallSegmentColors() {
        for (b in ball) {
            if (b in ballsOnCollisionCourse)
                break
            with(b.segment ?: break) {
                val prevSide = otherSide(b.tile, b.movingFromSide)
                val prevSideColors =
                    prevSide.first.segment.filter { it.type.sides.contains(prevSide.second) }
                        .map { it.color[it.type.sides.indexOf(prevSide.second)] }.filter { it != 0 }.distinct()
                if (prevSideColors.isEmpty() || prevSideColors.size > 1)
                    setBehindColorForBall(b, 0)
                else
                    setBehindColorForBall(b, prevSideColors.first())
                setSplitFromBall(b)
            }
        }
    }

    /**
     * Kill specified balls
     */
    private fun killBalls(collisions: List<Ball>) {
        if (collisions.isEmpty())
            return
        ctx.score.addPoints(collisions.distinct().size)
        deadBall.addAll(collisions)
        ball.removeAll(collisions.toSet())
        collisions.distinct().forEach {
            blot.add(Blot(ctx, it.color, it.tile, it.currentPosition))
        }
    }

    /**
     * Kill shredded balls
     */
    fun shredBalls(shredded: (List<Ball>) -> List<Ball>): Unit = killBalls(shredded(ball))

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
            Side.Top -> tile[currentCoord.x][ctx.cp.clipWrap(currentCoord.y + 1)]
            Side.Right -> tile[ctx.cp.clipWrap(currentCoord.x + 1)][currentCoord.y]
            Side.Bottom -> tile[currentCoord.x][ctx.cp.clipWrap(currentCoord.y - 1)]
            Side.Left -> tile[ctx.cp.clipWrap(currentCoord.x - 1)][currentCoord.y]
        } to currentSide.otherSide
    }

    /**
     * Picks nearest matching segment for the ball move, if it is defined.
     */
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
        ball.forEach { if (it !in ballsOnCollisionCourse) it.segment?.setSplitFromBall(it) }
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
     * Field pointer coordinates of suitable open selector, for auto-move
     */
    fun suggestOpenSelectorFieldCoord(): Vector2 {
        val myIntent = openSelector.filter { it.tile in ball.map { b -> b.tile } }.ifEmpty { openSelector }
            .minByOrNull { it.trackStep } ?: return Vector2.Zero
        val segments = myIntent.tile.segment.filter { s -> s.type.sides.contains(myIntent.side) }
        val optimalSegment = if (segments.size == 1) segments.first()
        else segments.minByOrNull { optimalDirection[myIntent.side]!!.indexOf(it.type) }
        return myIntent.arrowCenter((optimalSegment ?: segments.random()).type.sides.first { it != myIntent.side })
    }

    /**
     * Possible segment types when moving from side, ordered by preferred direction
     */
    private val optimalDirection = mapOf(
        Side.Top to listOf(SegmentType.ArcLT, SegmentType.ArcTR, SegmentType.LineBT),
        Side.Left to listOf(SegmentType.ArcLT, SegmentType.LineLR, SegmentType.ArcBL),
        Side.Right to listOf(SegmentType.ArcTR, SegmentType.LineLR, SegmentType.ArcRB),
        Side.Bottom to listOf(SegmentType.LineBT, SegmentType.ArcBL, SegmentType.ArcRB)
    )

    private val v = Vector2()

    /**
     * Check if the given pointer screen coordinates match some of the active selector arrows.
     * It there is a match, respective selector is set and the track extended.
     * Returns true if there was the selector match.
     */
    fun selectorsHitTest(vf: Vector2): Boolean {
        v.set(ctx.cp.clipWrapCoord(vf.x), ctx.cp.clipWrapCoord(vf.y))
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
        t.intent.forEachIndexed { i, intent -> intent.intentSideColor = oldTile.intent[i].intentSideColor }
        tile[x][y] = t
        ball.filter { it.tile == oldTile }.forEach {
            it.tile = t
            it.segment = null
            clickedSelectorColors.remove(it.color)
        }
        blot.filter { it.baseTile == oldTile }.forEach { it.baseTile = t }
        planTracks()
    }

    /**
     * Return indexes of random tile that has any colored track.
     */
    fun chaosTileCoord(): Coord {
        val fTile = flatTile
        return fTile.filter { it.segment.any { s -> s.color.any { c -> c != 0 } } }
            .ifEmpty { ball.map { it.tile } }
            .ifEmpty { fTile }.random().coord
    }


    /**
     * Return indexes of random tile that has an open selector, or then any random tile
     */
    fun suggestTileCoord(): Coord {
        val fTile = flatTile
        return openSelector.map { it.tile }.distinct().ifEmpty { fTile }.random().coord
    }

    /**
     * Are there no more selectable selectors
     */
    fun noMoreSelectors(): Boolean = openSelector.isEmpty()

    /**
     * Are there no more balls
     */
    fun noMoreBalls(): Boolean = ball.isEmpty()

    /**
     * Serialize the field tiles and balls
     */
    fun serialize(sb: StringBuilder) {
        flatTile.forEach {
            sb.append(it.coord.x, 2).append(it.coord.y, 2)
            it.serialize(sb)
        }
        sb.append(ball.size)
        ball.forEach { it.serialize(sb) }
        sb.append(deadBall.size)
        deadBall.forEach { it.serialize(sb) }
        sb.append(blot.size)
        blot.forEach { it.serialize(sb) }
        sb.append(clickedSelectorColors.size)
        clickedSelectorColors.forEach { sb.append(it) }
    }

    /**
     * Deserialize the field tiles and balls
     */
    fun deserialize(s: String, i: Int) {
        var j = i
        repeat(flatTile.size) {
            val x = s.substring(j..j + 1).toInt()
            val y = s.substring(j + 2..j + 3).toInt()
            val t = tile[x][y]
            t.coord.set(x, y)
            j = t.deserialize(s, j + 4)
        }
        repeat(s[j++].digitToInt()) {
            ball.add(Ball(
                s[j].digitToInt(),
                this.tile[s.substring(j + 1..j + 2).toInt()][s.substring(j + 3..j + 4).toInt()]
            ).apply {
                j = this.deserialize(s, j + 5)
            })
        }
        repeat(s[j++].digitToInt()) {
            deadBall.add(Ball(
                s[j].digitToInt(),
                this.tile[s.substring(j + 1..j + 2).toInt()][s.substring(j + 3..j + 4).toInt()]
            ).apply {
                j = this.deserialize(s, j + 5)
            })
        }
        repeat(s[j++].digitToInt()) {
            blot.add(
                Blot(
                    ctx,
                    s[j].digitToInt(),
                    tile[s.substring(j + 1..j + 2).toInt()][s.substring(j + 3..j + 4).toInt()],
                    Vector2.Zero
                ).apply { deserialize(s, j + 5) }
            )
            j += 20
        }
        repeat(s[j++].digitToInt()) {
            clickedSelectorColors.add(s[j++].digitToInt())
        }
        planTracks()
    }

}