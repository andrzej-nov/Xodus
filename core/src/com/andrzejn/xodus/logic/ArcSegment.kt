package com.andrzejn.xodus.logic

import com.andrzejn.xodus.Context
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.graphics.Color
import kotlin.math.PI

/**
 * Arc track segment (arcLT, arcTR, arcRB, arcBL)
 */
class ArcSegment(type: SegmentType, tile: Tile) : TrackSegment(type, tile) {
    /**
     * Arc radians size
     */
    val radians: Float = PI.toFloat() / 2

    /**
     * Arc starting angle
     */
    val angle: Float = when (type) {
        SegmentType.ArcLT -> -PI.toFloat() / 2
        SegmentType.ArcTR -> -PI.toFloat()
        SegmentType.ArcRB -> PI.toFloat() / 2
        else -> 0f
    }

    /**
     * Arc radius
     */
    var radius: Float = 0f

    /**
     * Arc center. Coordinates are relative to the bottom-left corner of the tile
     */
    val center: Vector2 = Vector2()

    /**
     * The tile square side length. Used to calculate screen positions of the segment parts.
     * Changes only on the window resize.
     */
    override var sideLen: Float
        get() = super.sideLen
        set(value) {
            super.sideLen = value
            radius = value / 2
            when (type) {
                SegmentType.ArcLT -> center.set(0f, value)
                SegmentType.ArcTR -> center.set(value, value)
                SegmentType.ArcRB -> center.set(value, 0f)
                else -> center.set(0f, 0f)
            }
            this.split = super.split // Trigger splitPos update
        }

    /**
     * Radians from the arc start to the split position
     */
    var splitRadians: Float = 0f

    /**
     * The color split position, in range 0..1.
     * 0 means the position on the first segment point (so the whole segment is of the color.second color),
     * 1 means the position on the second segment point (so the whole segment is of the color.first color).
     */
    override var split: Float
        get() = super.split
        set(value) {
            super.split = value
            splitRadians = radians * value
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
        return v.set(radius, 0f).rotateRad(angle + radians * split).add(center)
    }

    val c = Vector2()
    override fun render(ctx: Context, basePos: Vector2) {
        c.set(center).add(basePos)
        if (split > 0) {
            ctx.sd.setColor(ctx.theme.dark[color[0]])
            ctx.sd.arc(c.x, c.y, radius, angle, splitRadians, 4f)
        }
        ctx.sd.setColor(ctx.theme.dark[color[1]])
        ctx.sd.arc(c.x, c.y, radius, angle + splitRadians, radians - splitRadians, 4f)
    }
}