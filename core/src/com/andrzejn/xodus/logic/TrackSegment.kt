package com.andrzejn.xodus.logic

import com.andrzejn.xodus.Context
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.graphics.Color

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
     * Translates the current ball position into the split value
     */
    fun ballPositionToSplit(b: Ball): Float =
        if (isMovingFromSegmentStart(b)) b.position else 1f - b.position

    fun isMovingFromSegmentStart(b: Ball): Boolean = b.movingFromSide == type.sides[0]

    fun aheadSideIndex(b: Ball): Int = if (isMovingFromSegmentStart(b)) 1 else 0

    fun behindSideIndex(b: Ball): Int = if (isMovingFromSegmentStart(b)) 0 else 1

    /**
     * Colors the segment by the ball
     */
    fun setColorFrom(b: Ball) {
        val ahead = aheadSideIndex(b)
        color[ahead] = b.color
        split = ballPositionToSplit(b)
    }

    protected var lineWidth: Float = 0f

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

    abstract fun render(ctx: Context, clr: Color, lWidth: Float)
    abstract fun render(ctx: Context)
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
    protected fun colorFor(color: Int, ctx: Context) =
        if (color == 0 && tile.coord.isNotSet()) ctx.theme.light[0] else ctx.theme.dark[color]

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
