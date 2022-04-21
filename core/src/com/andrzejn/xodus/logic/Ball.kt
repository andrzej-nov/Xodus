package com.andrzejn.xodus.logic

import com.andrzejn.xodus.Context
import com.badlogic.gdx.math.Vector2
import kotlin.math.PI

/**
 * The ball moves on the track built from the field tiles.
 */
class Ball(
    /**
     * The ball color. Never changes.
     */
    val color: Int,
    /**
     * Initial tile. The ball will be put at the tile bottom, on the LineBT segment if available.
     * Then the tile changes as the ball in moving.
     */
    var tile: Tile
) {
    /**
     * The segment on which the ball is currently moving.
     * Null means the ball is at the tile side that has several segments going from it,
     * so the player must select the segment before movement continues.
     */
    var segment: TrackSegment? = null

    /**
     * The ball position on the current tile segment. In range 0..1, then it translates into segment.split
     */
    var position: Float = 0f

    /**
     * The ball current move direction at the tile (from which segment side the tilePosition is calculated)
     */
    var movingFromSide: Side = Side.Bottom

    /**
     * Clone from another ball. Used to plan the ball movement on initial track filling.
     */
    constructor(from: Ball) : this(from.color, from.tile) {
        segment = from.segment
        position = from.position
        movingFromSide = from.movingFromSide
    }

    private var outerRadius: Float = 0f
    private var innerRadius: Float = 0f
    private var _sideLen: Float = 0f

    /**
     * The tile square side length. Used to calculate screen positions of the segment parts.
     * Changes only on the window resize.
     */
    var sideLen: Float
        get() = _sideLen
        set(value) {
            _sideLen = value
            outerRadius = value / 4
            innerRadius = value / 5
        }


    /**
     * Coordinates of the middle of the given side. Relative to the tile bottim-left corner, multiply by sideLen
     */
    private val sideMiddle = mapOf(
        Side.Top to Vector2(0.5f, 1f),
        Side.Right to Vector2(1f, 0.5f),
        Side.Bottom to Vector2(0.5f, 0f),
        Side.Left to Vector2(0f, 0.5f),
    )

    /**
     * Default direction angle when moving from side.
     */
    private val directionAngle = mapOf(
        Side.Top to -PI.toFloat() / 2,
        Side.Right to PI.toFloat(),
        Side.Bottom to PI.toFloat() / 2,
        Side.Left to 0f
    )

    /**
     * Value for internal calculations to reduce GC load
     */
    private val v = Vector2()
    private val v2 = Vector2()

    /**
     * Render the ball
     */
    fun render(ctx: Context) {
        val s = segment
        (if (s != null) v.set(s.coordinatesOf(this)) else v.set(sideMiddle[movingFromSide])
            .scl(sideLen)).add(tile.basePos)
        ctx.sd.filledCircle(v, outerRadius, ctx.theme.dark[color])
        ctx.sd.filledCircle(v, innerRadius, ctx.theme.light[color])
        v2.set(innerRadius * 2f / 3, innerRadius / 3f).rotateRad(
            s?.directionAngleFor(this) ?: (directionAngle[movingFromSide]
                ?: return)
        ).add(v)
        ctx.sd.filledCircle(v2, innerRadius / 6f, ctx.theme.eyeColor)
        v2.set(innerRadius * 2f / 3, -innerRadius / 3f).rotateRad(
            s?.directionAngleFor(this) ?: (directionAngle[movingFromSide]
                ?: return)
        ).add(v)
        ctx.sd.filledCircle(v2, innerRadius / 6f, ctx.theme.eyeColor)
    }

}