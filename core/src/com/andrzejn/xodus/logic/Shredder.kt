package com.andrzejn.xodus.logic

import aurelienribon.tweenengine.Timeline
import aurelienribon.tweenengine.Tween
import aurelienribon.tweenengine.TweenManager
import com.andrzejn.xodus.Context
import com.andrzejn.xodus.helper.TW_POS_XY
import com.andrzejn.xodus.helper.TW_Y
import kotlin.math.abs
import kotlin.math.sign

/**
 * Horizontal Shredder line. Moves up by 1/2 of cell per turn, wrapping from top to bottom of the field.
 * Kills balls that touch it from any side.
 */
class Shredder(fieldSize: Int) {
    private val halfFieldSize = fieldSize.toFloat() / 2

    /**
     * Current Shredder position, in cell sides. Add the screen scroll offset and multiply it to sideLen
     * to get actual screen coordinates.
     */
    var y: Float = 1.1f

    private var inAdvance = false

    /**
     * Distances from balls to the Shredder line, in pairs <ball.color, distance>, distance < 0 means the ball below
     * the line, > 0 - above the line.
     * Used to detect ball collisions with the line (when distance becomes 0 or changes sign near 0)
     */
    private val ballDist = mutableMapOf<Int, Float>()

    /**
     * Ensures the dist is within -halfFieldSize..halfFieldSize range, wrappint over the field top/bottom edges
     * as needed
     */
    private fun wrapDist(dist: Float): Float {
        if (dist > halfFieldSize)
            return dist - 2 * halfFieldSize
        if (dist < -halfFieldSize)
            return dist + 2 * halfFieldSize
        return dist
    }

    /**
     * Returns the map of ball colors to their distances to the Shredder line
     */
    fun currentBallDist(ball: List<Ball>): Map<Int, Float> =
        ball.associateBy({ it.color }, { wrapDist(it.currentY - y) })

    /**
     * Determine which balls have been shredded since prior function call
     */
    fun shreddedBalls(ball: List<Ball>): List<Ball> {
        if (!inAdvance)
            return emptyList()
        val newDist = currentBallDist(ball)
        val shredded =
            newDist.filter { (c, d) ->
                abs(d) < 0.01f
                        || (abs(d) < 1 && ballDist[c] != null && sign(d) != sign(ballDist[c]!!))
            }.map { it.key }.toSet()
        ballDist.clear()
        ballDist.putAll(newDist.filterKeys { it !in shredded })
        return ball.filter { it.color in shredded }
    }

    /**
     * Advance Shredder line by o.75 cell up during the move
     */
    fun advance(ctx: Context, scrollUp: () -> Unit) {
        inAdvance = true
        val prevY = y.toInt()
        val cameraX = ctx.fieldCamPos.x
        val cameraY = ctx.fieldCamPos.y
        val advanceBy = 0.75f
        val advanceDuration = 1f
        Timeline.createSequence()
            .beginParallel()
            .push(Tween.to(this, TW_Y, advanceDuration).target(y + advanceBy))
            .push(
                Tween.to(ctx.fieldCamPos, TW_POS_XY, advanceDuration)
                    .target(cameraX, cameraY + advanceBy * ctx.sideLen)
            )
            .end()
            .setCallback { _, _ ->
                y = ctx.clipWrap(y)
                if (y.toInt() != prevY)
                    scrollUp()
                inAdvance = false
            }.start(ctx.tweenManager)
    }

    /**
     * Render the Shredder line
     */
    fun render(ctx: Context) {
        val screenX = -ctx.sideLen
        val screenY = ctx.clipWrap(y + ctx.scrollOffset.y) * ctx.sideLen
        val width = ctx.wholeFieldSize + 2 * ctx.sideLen
        val lineWidth = width / 100
        val dash = width / 10
        dashedLineFromLeft(ctx, screenX, screenY + lineWidth / 2, screenX + width, lineWidth, dash)
        dashedLineFromRight(ctx, screenX, screenY - lineWidth / 2, screenX + width, lineWidth, dash)
    }

    private fun dashedLineFromLeft(
        ctx: Context,
        leftX: Float,
        screenY: Float,
        rightX: Float,
        lineWidth: Float,
        dash: Float
    ) {
        var x1 = leftX
        var x2 = leftX + dash - screenY.toInt() % (dash * 1.5).toInt()
        while (x1 < rightX) {
            if (x2 > rightX) x2 = rightX
            if (x2 > x1)
                ctx.sd.line(x1, screenY, x2, screenY, ctx.theme.shredderYellow, lineWidth)
            x1 = x2 + dash / 2
            x2 = x1 + dash
        }
    }

    private fun dashedLineFromRight(
        ctx: Context,
        leftX: Float,
        screenY: Float,
        rightX: Float,
        lineWidth: Float,
        dash: Float
    ) {
        var x2 = rightX
        var x1 = rightX - dash + screenY.toInt() % (dash * 1.5).toInt()
        while (x2 > leftX) {
            if (x1 < leftX) x1 = leftX
            if (x2 > x1)
                ctx.sd.line(x1, screenY, x2, screenY, ctx.theme.shredderRed, lineWidth)
            x2 = x1 - dash / 2
            x1 = x2 - dash
        }
    }
}