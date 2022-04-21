package com.andrzejn.xodus.logic

import com.andrzejn.xodus.Context
import com.badlogic.gdx.math.Vector2

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
     * Value for internal calculations
     */
    private val v = Vector2()

    /**
     * Render the ball
     */
    fun render(ctx: Context) {
        val s = segment
        if (s != null)
            v.set(s.coordinatesOf(this)).add(tile.basePos)
        else
            v.set(tile.middleOfSide(movingFromSide))
        ctx.sd.filledCircle(v, outerRadius, ctx.theme.dark[color])
        ctx.sd.filledCircle(v, innerRadius, ctx.theme.light[color])
    }

}