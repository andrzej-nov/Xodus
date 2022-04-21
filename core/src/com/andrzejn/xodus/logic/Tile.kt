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
    val segment: Array<TrackSegment> = randomSegments()

    /**
     * When balls are planning their movement from the tile side to another side, record its color,
     * track step number and requirement for segment selector here.
     * Also provides selector fields for the user to select the ball moving direction from this side.
     */
    val intent: Array<MoveIntent> = Array(Side.values().size) { i -> MoveIntent(this, Side.values()[i]) }

    /**
     * Clear move intents
     */
    fun clearIntents(): Unit = intent.forEach { it.clearIntent() }

    /**
     * The tile coordinates on the field. (-1, -1) for unplaced new tiles. Once put to the field, the tiles do not move.
     */
    var coord: Coord = Coord()

    /**
     * The bottom-left tile corner in screen coordinates, to vase all content renders on it
     */
    val basePos: Vector2 = Vector2()

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
    fun setSideLen(value: Float) {
        _sideLen = value
        segment.forEach { it.sideLen = value }
        intent.forEach { it.resetSelectorArrows() }
    }

    /**
     * Initializes the tile with random set of track segments. Onse initialized, the segments never change,
     * they just may change colors.
     * All four tile sides always have at least one segment ending at them (may be several segments when tracks split
     * or join at that side).
     */
    private fun randomSegments(): Array<TrackSegment> {
        val list = mutableListOf<TrackSegment>()
        val sidesCovered = mutableMapOf<Side, Int>()
        var types = SegmentType.values().apply { shuffle() }.toList()
        while (sidesCovered.keys.size < 4) {
            val type = types.first()
            list.add(TrackSegment.of(type, this))
            type.sides.forEach { sidesCovered[it] = (sidesCovered[it] ?: 0) + 1 }
            val sidesWithEnoughSegments = sidesCovered.filterValues { it >= 2 }.keys
            val typeSplit = types.drop(1).partition { t -> t.sides.none { sidesWithEnoughSegments.contains(it) } }
            types = typeSplit.first.toMutableList().apply { addAll(typeSplit.second) }
        }
        return list.toTypedArray()
    }

    /**
     * Sort segments by color, to render uncolored segments first
     */
    fun sortSegments(): Unit = segment.sortBy { it.color[0] }

    /**
     * Render the tile segments and selectors
     */
    fun render(ctx: Context): Unit = segment.forEach { it.render(ctx) }

    /**
     * Variable for internal calculations to reduce the GC load
     */
    private val v = Vector2()

    /**
     * Coordinates of the middle of the given side. Relative to the tile bottim-left corner
     */
    fun middleOfSide(side: Side): Vector2 {
        v.set(basePos)
        return when (side) {
            Side.Top -> v.add(sideLen / 2, sideLen)
            Side.Right -> v.add(sideLen, sideLen / 2)
            Side.Bottom -> v.add(sideLen / 2, 0f)
            Side.Left -> v.add(0f, sideLen / 2)
        }
    }

    /**
     * Reset this segment colors.
     */
    fun clearSegmentColors(): Unit = segment.forEach { it.reset() }

}