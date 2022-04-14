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
    var segment: TrackSegment? = tile.startupBottomSegment

    /**
     * The ball position on the current tile segment. In range 0..1, then it translates into segment.split
     */
    var position: Float = 0f

    /**
     * The ball move direction (from which segment side the tilePosition is calculated)
     */
    var movingFromSide: TrackSegment.Side = TrackSegment.Side.Bottom

    /**
     * Clone from another ball. Used to plan the ball movement on initial track filling.
     */
    constructor(from: Ball) : this(from.color, from.tile) {
        segment = from.segment
        position = from.position
        movingFromSide = from.movingFromSide
    }

    private val c = Vector2()
    fun render(ctx: Context, basePos: Vector2) {
        if (segment != null)
            c.set(segment!!.coordinatesOf(this)).add(basePos)
        else
            c.set(tile.middleOfSide(movingFromSide)).add(basePos)
        c.add(tile.coord!!.x * tile.sideLen, tile.coord!!.y * tile.sideLen)
        ctx.sd.filledCircle(c, 20f, ctx.theme.dark[color])
        ctx.sd.filledCircle(c, 15f, ctx.theme.light[color])
    }

}