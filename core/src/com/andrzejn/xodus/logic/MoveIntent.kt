package com.andrzejn.xodus.logic

import com.andrzejn.xodus.Context
import com.badlogic.gdx.math.Polygon
import com.badlogic.gdx.math.Vector2

/**
 * The ball move intent from the tile side by one of tile segments to another side
 */
class MoveIntent(
    /**
     * The tile to which this selector belongs
     */
    val tile: Tile,
    /**
     * The tile side where this selector is placed
     */
    val side: Side
) {
    /**
     * Planned (intended) track color
     */
    var sideColor: Int = 0

    /**
     * Sequential number of the segment in respective planned ball track, to determine which ball comes first
     * and if there is a collison conflict
     */
    var trackStep: Int = 0

    /**
     * Tile segments coming from this side
     */
    private val segments = tile.segment.filter { it.type.sides.contains(side) }

    /**
     * Default segment (it is set if there is only one segment)
     */
    private val defaultSegment = if (segments.size == 1) segments.first() else null

    /**
     * Planned segment to move, or null when there is no segment selected yet (e.g. when the player should
     * select the segment from several possibilities)
     */
    var intentSegment: TrackSegment? = defaultSegment

    /**
     * Selector color. It is set to the ball color when the user chooses the selector, reset when the field changes
     * move the track away from this side.
     */
    var selectorColor: Int = 0

    /**
     * The tile segment selected by the player for moving through this tile side, on null if there are several possible
     * segments and the player has not selected yet
     */
    var selectorSegment: TrackSegment? = defaultSegment

    /**
     * Clear the intent (before the intents planning)
     */
    fun clearIntent() {
        sideColor = 0
        trackStep = 0
        intentSegment = defaultSegment
    }

    /**
     * Reset the selector, when the ball has already moved through it, or there is no more planned ball track here
     * due to the changes on the field
     */
    fun resetSelector() {
        selectorColor = 0
        selectorSegment = defaultSegment
    }

    /**
     * Render selector arrows
     */
    fun render(ctx: Context) {
        if (directionArrows == null)
            directionArrows = buildDirectionArrows()
        val arrows = directionArrows ?: return
        segments.forEach { it.render(ctx, ctx.theme.dark[selectorColor], 2f) }
        arrows.forEach {
            ctx.sd.setColor(ctx.theme.light[selectorColor])
            ctx.sd.filledPolygon(it.second)
            ctx.sd.setColor(ctx.theme.dark[selectorColor])
            ctx.sd.polygon(it.second)
        }
    }

    /**
     * Possible directions of this selector
     */
    private val directions: List<Side> = segments.flatMap { it.type.sides.toList() }.filter { it != side }

    private var directionArrows: List<Pair<Side, Polygon>>? = null

    /**
     * Returns the directionArrows list with the polygons already scaled and placed to required screen coordinates
     */
    private fun buildDirectionArrows(): List<Pair<Side, Polygon>> = directions.map { it to scaleArrow(it) }

    /**
     * Gets the arrow definition and creates the properly scaled polygon placed at required screen coordinates
     */
    private fun scaleArrow(key: Side): Polygon {
        var takeX = false
        return Polygon(arrow[key]!!.clone().map {
            takeX = !takeX
            it * tile.sideLen + if (takeX) tile.basePos.x else tile.basePos.y
        }.toFloatArray())
    }

    /**
     * Reset selector arrow polygons (used on window resize)
     */
    fun resetSelectorArrows() {
        directionArrows = null
    }

    /**
     * Checks if the given pointer screen coordinates match some of the selector arrows, and if yes, then set
     * the selector segment to that direction
     */
    fun selectorClicked(v: Vector2): Boolean {
        val da = directionArrows?.firstOrNull { it.second.boundingRectangle.contains(v) } ?: return false
        selectorSegment = segments.first { it.type.sides.contains(da.first) }
        return true
    }

    /**
     * Selector arrow coordinates, (fromSide to direction) to (triangle polygon vertices).
     * Multiply them to sideLen, plus basePos.
     */
    private val arrow = mapOf(
        Side.Left to floatArrayOf(0f, 1 / 2f, 1 / 3f, 2 / 3f, 1 / 3f, 1 / 3f),
        Side.Top to floatArrayOf(1 / 2f, 1f, 2 / 3f, 2 / 3f, 1 / 3f, 2 / 3f),
        Side.Right to floatArrayOf(1f, 1 / 2f, 2 / 3f, 1 / 3f, 2 / 3f, 2 / 3f),
        Side.Bottom to floatArrayOf(1 / 2f, 0f, 1 / 3f, 1 / 3f, 2 / 3f, 1 / 3f)
    )

}