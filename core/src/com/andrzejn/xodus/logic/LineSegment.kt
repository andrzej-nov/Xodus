package com.andrzejn.xodus.logic

import com.andrzejn.xodus.Context
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.graphics.Color
import kotlin.math.PI

/**
 * Line track segment (lineTB or lineLR)
 */
class LineSegment(type: SegmentType, tile: Tile) : TrackSegment(type, tile) {
    /**
     * Coordinates are relative to the tile left-bottom corner
     */
    private val ends = Array(2) { Vector2() }

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
                    ends[0].set(value / 2, 0f)
                    ends[1].set(value / 2, value)
                }
                else -> {
                    ends[0].set(0f, value / 2)
                    ends[1].set(value, value / 2)
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

    /**
     * Value for internal calculations to reduce GC load
     */
    private val s = Vector2()

    /**
     * Render this segment, overriding color and line width
     */
    override fun render(ctx: Context, clr: Color, lWidth: Float) {
        s.set(splitPos).add(tile.basePos)
        ctx.sd.line(s, v.set(ends[1]).add(tile.basePos), clr, lWidth)
    }


    /**
     * Render this segment
     */
    override fun render(ctx: Context) {
        s.set(splitPos).add(tile.basePos)
        if (split > 0)
            ctx.sd.line(
                v.set(ends[0]).add(tile.basePos),
                s,
                colorFor(color[0], ctx),
                colorBasedLineWidth(color[0])
            )
        ctx.sd.line(
            s,
            v.set(ends[1]).add(tile.basePos),
            colorFor(color[1], ctx),
            colorBasedLineWidth(color[1])
        )
    }

    /**
     * Current direction angle for the ball, considering its position, segment type and move direction.
     * In radians, 0 is straight right, counterclockwise.
     */
    override fun directionAngleFor(b: Ball): Float = when (type) {
        SegmentType.LineBT -> if (b.movingFromSide == Side.Bottom) PI.toFloat() / 2 else -PI.toFloat() / 2
        else -> if (b.movingFromSide == Side.Left) 0f else PI.toFloat()
    }
}