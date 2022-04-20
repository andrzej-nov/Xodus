package com.andrzejn.xodus.helper

import com.andrzejn.xodus.Context
import com.andrzejn.xodus.logic.Tile
import com.badlogic.gdx.math.Vector2

/**
 * Tile rendered for tween animations
 */
class FloatingTile(private val ctx: Context) {
    private var _tile: Tile? = null

    /**
     * The tile to render
     */
    var tile: Tile?
        get() = _tile
        set(value) {
            _tile = value
            position.set(value?.basePos ?: Vector2.Zero)
        }

    /**
     * The tile screen position to render it
     */
    var position: Vector2 = Vector2()

    /**
     * Render the tile at its current position
     */
    fun render() {
        val t = tile ?: return
        with(t) {
            basePos.set(position)
            ctx.sd.filledRectangle(position.x, position.y, sideLen, sideLen, ctx.theme.gameboardBackground)
            ctx.sd.rectangle(position.x, position.y, sideLen, sideLen, ctx.theme.gameBorders, 1f)
            render(ctx)
        }
    }
}