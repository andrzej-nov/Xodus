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
    private val radians: Float = PI.toFloat() / 2

    /**
     * Arc starting angle
     */
    private val angle = mapOf(
        SegmentType.ArcLT to -PI.toFloat() / 2,
        SegmentType.ArcTR to -PI.toFloat(),
        SegmentType.ArcRB to PI.toFloat() / 2,
        SegmentType.ArcBL to 0f
    )[type]!!

    /**
     * Arc radius
     */
    private var radius: Float = 0f

    /**
     * Arc center. Coordinates are relative to the bottom-left corner of the tile
     */
    private val center: Vector2 = Vector2()

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
    private var splitRadians: Float = 0f

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
    override fun coordinatesOf(split: Float): Vector2 = v.set(radius, 0f).rotateRad(angle + radians * split).add(center)

    private val ballFaceDirection = mapOf(
        (Side.Left to SegmentType.ArcLT) to PI.toFloat() / 2,
        (Side.Top to SegmentType.ArcLT) to -PI.toFloat() / 2,
        (Side.Top to SegmentType.ArcTR) to PI.toFloat() / 2,
        (Side.Right to SegmentType.ArcTR) to -PI.toFloat() / 2,
        (Side.Right to SegmentType.ArcRB) to PI.toFloat() / 2,
        (Side.Bottom to SegmentType.ArcRB) to -PI.toFloat() / 2,
        (Side.Bottom to SegmentType.ArcBL) to PI.toFloat() / 2,
        (Side.Left to SegmentType.ArcBL) to -PI.toFloat() / 2
    )

    /**
     * Current direction angle for the ball, considering its position, segment type and move direction.
     * In radians, 0 is straight right, counterclockwise.
     */
    override fun directionAngleFor(b: Ball): Float =
        angle + radians * (ballPositionToSplit(b)) + ballFaceDirection[b.movingFromSide to type]!!

    /**
     * Render this segment, overriding color and line width
     */
    override fun render(ctx: Context, clr: Color, lWidth: Float) {
        v.set(center).add(tile.basePos)
        ctx.sd.setColor(clr)
        ctx.sd.arc(v.x, v.y, radius, angle + splitRadians, radians - splitRadians, lWidth)
    }

    /**
     * Render this segment
     */
    override fun render(ctx: Context) {
        v.set(center).add(tile.basePos)
        if (split > 0) {
            ctx.sd.setColor(colorFor(color[0], ctx))
            ctx.sd.arc(v.x, v.y, radius, angle, splitRadians, colorBasedLineWidth(color[0]))
        }
        ctx.sd.setColor(colorFor(color[1], ctx))
        ctx.sd.arc(
            v.x, v.y, radius, angle + splitRadians, radians - splitRadians, colorBasedLineWidth(color[1])
        )
    }

}