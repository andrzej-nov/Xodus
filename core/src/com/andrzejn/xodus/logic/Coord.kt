package com.andrzejn.xodus.logic

/**
 * Magical number meaning the coord is not set
 */
const val unsetCoord: Int = -999

/**
 * Similar to Vector2 class but holds integer coordinates, to specify a tile position on the grid or something of
 * that kind.
 */
class Coord(
    /**
     * Horizontal position
     */
    var x: Int = unsetCoord,
    /**
     * Vertical position
     */
    var y: Int = unsetCoord
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
     * Adds another coord to this one. Returns this instance to allow chained calls.
     */
    fun add(other: Coord): Coord {
        this.x += other.x
        this.y += other.y
        return this
    }

    /**
     * True if at least one of the coordinates is not 0
     */
    fun isNotZero(): Boolean {
        return x != 0 || y != 0
    }

    /**
     * True if both coordinates are not unsetCoord
     */
    fun isSet(): Boolean {
        return x != unsetCoord && y != unsetCoord
    }

    /**
     * True if both coordinates are unsetCoord
     */
    fun isNotSet(): Boolean {
        return x == unsetCoord && y == unsetCoord
    }

    fun unSet(): Coord {
        x = unsetCoord
        y = unsetCoord
        return this
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        return result
    }

}