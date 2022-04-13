package com.andrzejn.xodus.logic

import com.badlogic.gdx.math.Vector2

/**
 * Line track segment (lineTB or lineLR)
 */
class LineSegment(type: SegmentType) : TrackSegment(type) {
    /**
     * Coordinates are relative to the tile left-bottom corner
     */
    val ends: Pair<Vector2, Vector2> = Vector2() to Vector2()

    /**
     * The tile square side length. Used to calculate screen positions of the segment parts.
     * Changes only on the window resize.
     */
    override var sideLen: Float
        get() = super.sideLen
        set(value) {
            super.sideLen = value
            when (type) {
                SegmentType.LineBT -> {
                    ends.first.set(value / 2, 0f)
                    ends.second.set(value / 2, value)
                }
                else -> {
                    ends.first.set(0f, value / 2)
                    ends.second.set(value, value / 2)
                }
            }
            this.split = super.split // Trigger splitPos update
        }

    /**
     * The color split position, in range 0..1.
     * 0 means the position on the first segment point (so the whole segment is of the color.second color),
     * 1 means the position on the second segment point (so the whole segment is of the color.first color).
     */
    override var split: Float
        get() = super.split
        set(value) {
            super.split = value
            splitPos.set(coordinatesOf(value))
        }

    /**
     * Value for internal calculations to reduce GC load
     */
    private val v = Vector2()

    /**
     * Point coordinates for the given split position.
     * Relative to the tile bottom-left corner.
     */
    override fun coordinatesOf(split: Float): Vector2 {
        return when (type) {
            SegmentType.LineBT -> v.set(sideLen / 2, sideLen * split)
            else -> v.set(sideLen * split, sideLen / 2)
        }
    }
}