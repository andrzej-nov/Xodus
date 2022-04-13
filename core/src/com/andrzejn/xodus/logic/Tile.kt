package com.andrzejn.xodus.logic

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
    val segments: List<TrackSegment> = randomSegments()

    /**
     * The tile coordinates on the field. Null for unplaced new tiles. Once put to the field, the tiles do not move.
     */
    var coord: Coord? = null

    private var _sideLen: Float = 0f

    /**
     * The tile square side length. Used to calculate screen positions of the segment parts.
     * Changes only on the window resize.
     */
    var sideLen: Float
        get() = _sideLen
        set(value) {
            _sideLen = value
            segments.forEach { it.sideLen = value }
        }

    /**
     * Initializes the tile with random set of track segments. Onse initialized, the segments never change,
     * they just may change colors.
     * All four tile sides always have at least one segment ending at them (may be several segments when tracks split
     * or join at that side).
     */
    private fun randomSegments(): List<TrackSegment> {
        val sidesCovered = mutableSetOf<TrackSegment.Side>()
        return TrackSegment.SegmentType.values().also { it.shuffle() }.fold(mutableListOf()) { list, type ->
            list.add(TrackSegment.of(type))
            sidesCovered.add(type.sides.first)
            sidesCovered.add(type.sides.second)
            if (sidesCovered.size == 4)
                return list
            list
        }
    }

    /**
     * Get initial segment to put the ball at the tile bottom, preferably LineBT
     */
    val startupBottomSegment: TrackSegment
        get() = segments.firstOrNull { it.type == TrackSegment.SegmentType.LineBT }
            ?: segments.first { it.type in listOf(TrackSegment.SegmentType.ArcBL, TrackSegment.SegmentType.ArcRB) }
}