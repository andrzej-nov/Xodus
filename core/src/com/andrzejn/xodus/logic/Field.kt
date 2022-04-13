package com.andrzejn.xodus.logic

import com.andrzejn.xodus.Context

/**
 * The playfield, with tiles, balls, tracks and logic
 */
class Field(
    /**
     * Reference to the app context
     */
    private val ctx: Context
) {
    /**
     * The field of tiles (not all of them are set)
     */
    private val tiles: Array<Array<Tile?>> = Array(ctx.gs.fieldWidth) { Array(8) { null } }

    fun createInitialTiles() {
        tiles.indices.forEach { x -> (0..3).forEach { y -> putTile(x, y, Tile()) } }
    }

    fun putTile(x: Int, y: Int, t: Tile) {
        tiles[x][y] = t
        t.coord = Coord(x, y)
    }
}