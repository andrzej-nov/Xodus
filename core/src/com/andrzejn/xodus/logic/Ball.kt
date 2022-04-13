package com.andrzejn.xodus.logic

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
     * The segment on which the ball currently moves.
     */
    var segment: TrackSegment = tile.startupBottomSegment

    /**
     * The ball position on the current tile segment. In range 0..1, then it translates into segment.split
     */
    var tilePosition: Float = 0f

    /**
     * From which segment side the tilePosition is calculated
     */
    var positionFromSide: TrackSegment.Side = TrackSegment.Side.Bottom
}