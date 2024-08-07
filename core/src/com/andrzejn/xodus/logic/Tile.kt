package com.andrzejn.xodus.logic

import com.andrzejn.xodus.Context
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.StringBuilder
import kotlin.random.Random

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
    var segment: Array<TrackSegment> = randomSegments()

    /**
     * When balls are planning their movement from the tile side to another side, record its color,
     * track step number and requirement for segment selector here.
     * Also provides selector fields for the user to select the ball moving direction from this side.
     */
    val intent: Array<MoveIntent> = Array(Side.entries.size) { i -> MoveIntent(this, Side.entries[i]) }

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
        var types = SegmentType.entries.toTypedArray().apply { shuffle() }.toList()
        var type = types.first()
        types = types.drop(1)
        list.add(TrackSegment.of(type, this))
        if (Random.nextFloat() < 0.5f)
            list.add(TrackSegment.of(type.complement(), this))
        else
            while (sidesCovered.keys.size < 4) {
                type = types.first()
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
     * Reset this segment colors.
     */
    fun clearSegmentColors(): Unit = segment.forEach { it.reset() }

    /**
     * Serialize tile segments
     */
    fun serialize(sb: StringBuilder) {
        sb.append(segment.size)
        segment.forEach { sb.append(it.type.ordinal) }
        intent.forEach { it.serialize(sb) }
    }

    /**
     * Deserialize tile segments
     */
    fun deserialize(s: String, i: Int): Int {
        var j = i
        segment = Array(s[j++].digitToInt()) {
            TrackSegment.of(SegmentType.entries[s[j++].digitToInt()], this)
        }
        repeat(intent.size) {
            with(intent[s[j].digitToInt()]) {
                initialize()
                j = deserialize(s, j + 1)
            }
        }
        return j
    }

}