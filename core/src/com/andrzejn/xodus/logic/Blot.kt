package com.andrzejn.xodus.logic

import aurelienribon.tweenengine.Tween
import com.andrzejn.xodus.Context
import com.andrzejn.xodus.helper.TW_SCATTER
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import kotlin.random.Random

/**
 * Blot on place of the ball death. Gradually is fading out.
 */
class Blot(
    /**
     * Reference to the app context
     */
    val ctx: Context,
    /**
     * Blot color, coming from the ball color
     */
    val color: Int,
    /**
     * The tile that is the anchor base position of the blot. Also provides the sideLen value for scaling.
     */
    var baseTile: Tile,
    /**
     * Blot center position relative to the baseTile.basePos. Multiply it to sideLen to get actual coordinates.
     */
    position: Vector2
) {
    /**
     * Blot center position relative to the baseTile.basePos. Multiply it to sideLen to get actual coordinates.
     */
    private val basePos = Vector2(position).scl(1f / baseTile.sideLen) // Save a local copy of the vector

    /**
     * Bigger blot circles, colored as the ball inner circle. Coordinates are relative to the basePos.
     * Multiply them by baseTile.sideLen on render
     */
    private val biggerBlot = List(4) { Vector2() }

    /**
     * Smaller blot circles, colored as the ball outer circle. Coordinates are relative to the basePos.
     * Multiply them by baseTile.sideLen on render
     */
    private val smallerBlot = List(6) { Vector2() }

    init {
        biggerBlot.forEach { it.set(Random.nextFloat() * 0.5f - 0.25f, Random.nextFloat() * 0.5f - 0.25f) }
        smallerBlot.forEach { it.set(Random.nextFloat() * 0.8f - 0.4f, Random.nextFloat() * 0.8f - 0.4f) }
        Tween.to(this, TW_SCATTER, 0.2f).target(1f).start(ctx.tweenManager)
    }

    /**
     * Scatter coefficient for tween animation on the blot creation. In range 0..1. Multiply it to the biggerBlot /
     * smallerBlot vectors on render
     */
    var scatter: Float = 0f

    /**
     * The colors alpha channel. Blots are fading by 0.2 alpha on each move.
     */
    var alpha: Float = 1f

    /**
     * Fade the blot by another 0.2 alpha. Intended for calling on all blots at the end of the move.
     */
    fun fade() {
        alpha -= 0.2f
        if (alpha < 0) alpha = 0f
    }

    /**
     * Variable for internal calculations, to reduce the GC load.
     */
    private val v = Vector2()
    private val c = Color()

    /**
     * Render the blot
     */
    fun render() {
        if (alpha <= 0) return
        renderBlots(smallerBlot, ctx.theme.dark, 14)
        renderBlots(biggerBlot, ctx.theme.light, 10)
    }

    /**
     * Render the set of blot circles
     */
    private fun renderBlots(blots: List<Vector2>, colors: Array<Color>, radiusFactor: Int) {
        ctx.sd.setColor(c.set(colors[color]).apply { a = alpha })
        val sideLen = baseTile.sideLen
        val tilePos = baseTile.basePos
        blots.forEach {
            ctx.sd.filledCircle(v.set(it).scl(scatter).add(basePos).scl(sideLen).add(tilePos), sideLen / radiusFactor)
        }
    }
}