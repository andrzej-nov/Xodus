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

    /**
     * Standard equality check
     */
    override fun equals(other: Any?): Boolean = (other is Coord) && this.x == other.x && this.y == other.y

    /**
     * Subtracts another coord from this one. Returns this instance to allow chained calls.
     */
    fun sub(other: Coord): Coord {
        this.x -= other.x
        this.y -= other.y
        return this
    }

    /**
     * Adds another coord to this one. Returns this instance to allow chained calls.
     */
    fun add(other: Coord): Coord {
        this.x += other.x
        this.y += other.y
        return this
    }

    /**
     * True if both coordinates are 0
     */
    fun isZero(): Boolean {
        return x == 0 && y == 0
    }

    /**
     * True if at least one of the coordinates is not 0
     */
    fun isNotZero(): Boolean {
        return x != 0 || y != 0
    }

    /**
     * True if both coordinates are >=0
     */
    fun isSet(): Boolean {
        return x >= 0 && y >= 0
    }

    /**
     * True if both coordinates are -1
     */
    fun isNotSet(): Boolean {
        return x == -1 && y == -1
    }

    fun unSet(): Coord {
        x = -1
        y = -1
        return this
    }

}