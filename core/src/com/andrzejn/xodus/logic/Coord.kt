package com.andrzejn.xodus.logic

/**
 * Similar to Vector2 class but holds integer coordinates, to specify a tile position on the grid or something of
 * that kind.
 */
class Coord(
    /**
     * Horizontal position
     */
    var x: Int = -1,
    /**
     * Vertical position
     */
    var y: Int = -1
) {
    /**
     * Set this coord equal to the one passed as the parameter. Returns this instance to allow chained calls.
     */
    fun set(fromOther: Coord): Coord {
        x = fromOther.x
        y = fromOther.y
        return this
    }

    /**
     * Set this coord to the given values. Returns this instance to allow chained calls.
     */
    fun set(x: Int, y: Int): Coord {
        this.x = x
        this.y = y
        return this
    }
}