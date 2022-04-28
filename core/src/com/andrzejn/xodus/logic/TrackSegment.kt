package com.andrzejn.xodus.logic

import com.andrzejn.xodus.Context
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2

/**
 * A track segment that fits into a single square tile. Always joins some two sides of the square, so it could be
 * a line or an arc.
 * May be colored with a single color or with two colors starting on both sides and meeting at some segment point.
 * Segment colors and the point position may change.
 */
abstract class TrackSegment(
    /**
     * The segment type
     */
    val type: SegmentType,
    /**
     * The tile to which that segment belongs
     */
    val tile: Tile
) {
    /**
     * The segment colors. Colors start from both ends of the segment and meet at some position in between.
     * Both colors may be the same.
     */
    val color: Array<Int> = arrayOf(0, 0)

    private var _split: Float = 0f

    /**
     * The color split position, in range 0..1.
     * 0 means the position on the first segment point (so the whole segment is of the color.second color),
     * 1 means the position on the second segment point (so the whole segment is of the color.first color).
     */
    open var split: Float
        get() = _split
        set(value) {
            _split = value
        }

    /**
     * The screen coordinates of the colors split point, according to the current split position and the side length.
     * Coordinates are relative to the left-bottom tile corner.
     * Calculated in the subclasses.
     */
    val splitPos: Vector2 = Vector2()

    /**
     * Point coordinates for the given split position.
     * Relative to the tile bottom-left corner.
     */
    abstract fun coordinatesOf(split: Float): Vector2

    /**
     * Point coordinates for the given ball position.
     * Relative to the tile bottom-left corner.
     */
    fun coordinatesOf(b: Ball): Vector2 = coordinatesOf(ballPositionToSplit(b))

    /**
     * Current direction angle for the ball, considering its position, segment type and move direction.
     * In radians, 0 is straight right, counterclockwise.
     */
    abstract fun directionAngleFor(b: Ball): Float

    /**
     * Translates the current ball position into the split value
     */
    fun ballPositionToSplit(b: Ball): Float =
        if (isMovingFromSegmentStart(b)) b.position else 1f - b.position

    /**
     * Set current segment color split from the ball position
     */
    fun setSplitFromBall(b: Ball) {
        split = ballPositionToSplit(b)
    }

    /**
     * Set the color that will be drawn behind the ball while it is moving.
     */
    fun setBehindColorForBall(b: Ball, clr: Int) {
        color[behindSideIndexFor(b)] = clr
    }

    private fun isMovingFromSegmentStart(b: Ball): Boolean = b.movingFromSide == type.sides[0]

    private fun behindSideIndexFor(b: Ball): Int = if (isMovingFromSegmentStart(b)) 0 else 1

    private var lineWidth: Float = 0f

    private var _sideLen: Float = 0f

    /**
     * The tile square side length. Used to calculate screen positions of the segment parts.
     * Changes only on the window resize.
     */
    open var sideLen: Float
        get() = _sideLen
        set(value) {
            _sideLen = value
            lineWidth = value / 6
        }

    /**
     * Render the segment, overriding the color and line width
     */
    abstract fun render(ctx: Context, clr: Color, lWidth: Float)

    /**
     * Render the segment
     */
    abstract fun render(ctx: Context)

    /**
     * Reset the segment colors to none
     */
    fun reset() {
        color[0] = 0
        color[1] = 0
        split = 0f
    }

    /**
     * Ensures that non-colored segments are drawn in thinner lines
     */
    protected fun colorBasedLineWidth(color: Int): Float = if (color == 0) lineWidth / 2 else lineWidth

    /**
     * Ensures that unplaced new tile is drawn in light color
     */
    protected fun colorFor(color: Int, ctx: Context): Color =
        if (color != 0) ctx.theme.dark[color]
        else if (tile.coord.isNotSet()) ctx.theme.newTileSegment else ctx.theme.placedTileSegment

    /**
     * Creates TrackSegment instances
     */
    companion object Factory {
        /**
         * Create the TrackSegment instance matching the type
         */
        fun of(type: SegmentType, tile: Tile): TrackSegment = when (type) {
            SegmentType.LineBT, SegmentType.LineLR -> LineSegment(type, tile)
            else -> ArcSegment(type, tile)
        }
    }
}
