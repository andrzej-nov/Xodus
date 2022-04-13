package com.andrzejn.xodus.logic

import com.badlogic.gdx.math.Vector2

/**
 * A track segment that fits into a single square tile. Always joins some two sides of the square, so it could be
 * a line or an arc.
 * May be colored with a single color or with two colors starting on both sides and meeting at some segment point.
 * Segment colors and the point position may change.
 */
abstract class TrackSegment(val type: SegmentType) {
    /**
     * The segment colors. Colors start from both ends of the segment and meet at some position in between.
     * Both colors may be the same.
     */
    val color: Pair<Int, Int> = 0 to 0

    /**
     * The tile sides where this segment starts and ends
     */
    val sides: Pair<Side, Side> = type.sides

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
    fun coordinatesOf(position: Float, fromSide: Side): Vector2 = coordinatesOf(ballPositionToSplit(position, fromSide))

    /**
     * Translates the current ball position into the split value
     */
    fun ballPositionToSplit(position: Float, fromSide: Side): Float =
        if (fromSide == type.sides.first) position else 1f - position

    private var _sideLen: Float = 0f

    /**
     * The tile square side length. Used to calculate screen positions of the segment parts.
     * Changes only on the window resize.
     */
    open var sideLen: Float
        get() = _sideLen
        set(value) {
            _sideLen = value
        }

    /**
     * Type of the segment. Used primarily for the initial random segments generation.
     */
    @Suppress("KDocMissingDocumentation")
    enum class SegmentType(val sides: Pair<Side, Side>) {
        LineBT(Side.Bottom to Side.Top),
        LineLR(Side.Left to Side.Right),
        ArcLT(Side.Left to Side.Top),
        ArcTR(Side.Top to Side.Right),
        ArcRB(Side.Right to Side.Bottom),
        ArcBL(Side.Bottom to Side.Left)
    }

    /**
     * Tile side
     */
    @Suppress("KDocMissingDocumentation")
    enum class Side {
        Top, Right, Bottom, Left
    }

    /**
     * Creates TrackSegment instances
     */
    companion object Factory {
        /**
         * Create the TrackSegment instance matching the type
         */
        fun of(type: SegmentType): TrackSegment = when (type) {
            SegmentType.LineBT, SegmentType.LineLR -> LineSegment(type)
            else -> ArcSegment(type)
        }
    }
}
