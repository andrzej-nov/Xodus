package com.andrzejn.xodus.logic

import aurelienribon.tweenengine.Tween
import com.andrzejn.xodus.Context
import com.andrzejn.xodus.helper.TW_SCATTER
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.StringBuilder
import java.util.*
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
    private var basePos = Vector2(position).scl(1f / baseTile.sideLen) // Save a local copy of the vector

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
        if (position != Vector2.Zero)
            initBlots(true)
    }

    /**
     * Initialize blot coordinates
     */
    private fun initBlots(doTween: Boolean) {
        biggerBlot.forEach { it.set(Random.nextFloat() * 0.5f - 0.25f, Random.nextFloat() * 0.5f - 0.25f) }
        smallerBlot.forEach { it.set(Random.nextFloat() * 0.8f - 0.4f, Random.nextFloat() * 0.8f - 0.4f) }
        if (doTween)
            Tween.to(this, TW_SCATTER, 0.2f).target(1f).start(ctx.tweenManager)
        else
            scatter = 1f
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
        val tilePos = baseTile.basePos
        val radius = baseTile.sideLen / radiusFactor
        blots.forEach { b ->
            ctx.cp.renderWithFieldBorders(
                v.set(b).scl(scatter).add(basePos).scl(baseTile.sideLen).add(tilePos),
                baseTile.coord
            ) { ctx.sd.filledCircle(it, radius) }
        }
    }

    /**
     * Serialize the blot
     */
    fun serialize(sb: StringBuilder) {
        sb.append(color).append(baseTile.coord.x, 2).append(baseTile.coord.y, 2)
            .append(String.format(Locale.ROOT, "%05.3f", basePos.x))
            .append(String.format(Locale.ROOT, "%05.3f", basePos.y))
            .append(String.format(Locale.ROOT, "%05.3f", alpha))
    }

    /**
     * Deserialize the blot fields
     */
    fun deserialize(s: String, i: Int) {
        basePos.set(s.substring(i..i + 4).toFloat(), s.substring(i + 5..i + 9).toFloat())
        alpha = s.substring(i + 10..i + 14).toFloat()
        initBlots(false)
    }
}