package com.andrzejn.xodus.logic

import com.andrzejn.xodus.Context
import com.badlogic.gdx.math.Vector2

/**
 * Square tile with track fragments on it. Once placed to the field, it is never moved until the whole bottom line
 * disappears.
 * Track fragments may be colored when they are parts of the ball tracks. Sometimes, when two bals are going to
 * collide on the tile, half of the track fragment can be of one color and another half of another color.
 * When a ball is moving through the tile, the track part ahead of it is colored, and the part behind it may be
 * either uncolored, or colored in another color when another ball follows current one.
 */
class Tile {
    /**
     * Tile segments. Onse initialized, the segments never change,
     * they just may change colors.
     * All four tile sides always have at least one segment ending at them (may be several segments when tracks split
     * or join at thet point).
     */
    val segment: List<TrackSegment> = randomSegments()

    /**
     * The move selectors at the tile sides. Since the tile segments never change, we can setup the selectors
     * on tile creation.
     */
    val selector: Array<MoveSelector> = Array(Side.values().size) { MoveSelector(this, Side.values()[it]) }

    /**
     * The tile coordinates on the field. (-1, -1) for unplaced new tiles. Once put to the field, the tiles do not move.
     */
    var coord: Coord = Coord()

    /**
     * When balls are planning their movement from the tile side to another side, record its color,
     * track step number and requirement for segment selector here
     */
    val intent: Array<MoveIntent> =
        Array(Side.values().size) { i -> MoveIntent(segment.filter { it.type.sides.contains(Side.values()[i]) }) }

    /**
     * Clear move intents
     */
    fun clearIntents(): Unit = intent.forEach { it.clear() }

    private var _sideLen: Float = 0f

    /**
     * The tile square side length. Used to calculate screen positions of the segment parts.
     * Changes only on the window resize.
     */
    var sideLen: Float
        get() = _sideLen
        set(value) {
            _sideLen = value
            segment.forEach { it.sideLen = value }
        }

    /**
     * Initializes the tile with random set of track segments. Onse initialized, the segments never change,
     * they just may change colors.
     * All four tile sides always have at least one segment ending at them (may be several segments when tracks split
     * or join at that side).
     */
    private fun randomSegments(): List<TrackSegment> {
        val sidesCovered = mutableSetOf<Side>()
        return SegmentType.values().also { it.shuffle() }.fold(mutableListOf()) { list, type ->
            list.add(TrackSegment.of(type, this))
            sidesCovered.addAll(type.sides)
            if (sidesCovered.size == 4)
                return list
            list
        }
    }

    /**
     * Render the tile segments and selectors
     */
    fun render(ctx: Context, basePos: Vector2) {
        segment.sortedBy { it.color[0] }.forEach { it.render(ctx, basePos) }
        //TODO render selectors
    }

    /**
     * Variable for internal calculations to reduce the GC load
     */
    private val v = Vector2()

    /**
     * Coordinates of the middle of the given side. Relative to the tile bottim-left corner
     */
    fun middleOfSide(side: Side): Vector2 = when (side) {
        Side.Top -> v.set(sideLen / 2, sideLen)
        Side.Right -> v.set(sideLen, sideLen / 2)
        Side.Bottom -> v.set(sideLen / 2, 0f)
        Side.Left -> v.set(0f, sideLen / 2)
    }

    /**
     * Get initial segment to put the ball at the tile bottom, preferably LineBT
     */
    val startupBottomSegment: TrackSegment
        get() = segment.firstOrNull { it.type == SegmentType.LineBT }
            ?: segment.first { it.type in listOf(SegmentType.ArcBL, SegmentType.ArcRB) }
}