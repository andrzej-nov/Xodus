package com.andrzejn.xodus.logic

import com.andrzejn.xodus.Context
import com.badlogic.gdx.math.Polygon
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.StringBuilder

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
    var intentSideColor: Int = 0

    /**
     * Sequential number of the segment in respective planned ball track, to determine which ball comes first
     * and if there is a collison conflict
     */
    var trackStep: Int = 0

    /**
     * Tile segments coming from this side
     */
    private var segments: List<TrackSegment> = emptyList()

    /**
     * Default segment (it is set if there is only one segment)
     */
    private var defaultSegment: TrackSegment? = null

    /**
     * Planned segment to move, or null when there is no segment selected yet (e.g. when the player should
     * select the segment from several possibilities)
     */
    var intentSegment: TrackSegment? = null

    /**
     * Selector color. It is set to the ball color when the user chooses the selector, reset when the field changes
     * move the track away from this side.
     */
    var selectorColor: Int = 0

    /**
     * The tile segment selected by the player for moving through this tile side, on null if there are several possible
     * segments and the player has not selected yet
     */
    var selectorSegment: TrackSegment? = null

    /**
     * Possible directions of this selector
     */
    private var directions: List<Side> = emptyList()

    private var directionArrows: List<Pair<Side, Polygon>>? = null

    init {
        initialize()
    }

    /**
     * Initialize intent fields after creation or after deserialization
     */
    fun initialize() {
        segments = tile.segment.filter { it.type.sides.contains(side) }
        defaultSegment = if (segments.size == 1) segments.first() else null
        intentSegment = defaultSegment
        selectorSegment = defaultSegment
        directions = segments.flatMap { it.type.sides.toList() }.filter { it != side }
        resetSelectorArrows()
    }

    /**
     * Clear the intent (before the intents planning)
     */
    fun clearIntent() {
        intentSideColor = 0
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
     * Internal variable for calculations, to reduce GC load
     */
    private val v = Vector2()

    /**
     * Render selector arrows
     */
    fun render(ctx: Context) {
        if (directionArrows == null)
            directionArrows = buildDirectionArrows()
        val arrows = directionArrows ?: return
        segments.forEach { it.render(ctx, ctx.theme.dark[selectorColor], 2f) }
        ctx.cp.renderWithFieldBorders(
            v.set(Vector2.Zero),
            tile.coord
        ) {
            arrows.forEach { (_, p) ->
                with(ctx.sd) {
                    p.setPosition(it.x, it.y)
                    setColor(if (ctx.gs.isDarkTheme) ctx.theme.light[selectorColor] else ctx.theme.dark[selectorColor])
                    filledPolygon(p)
                    setColor(if (ctx.gs.isDarkTheme) ctx.theme.dark[selectorColor] else ctx.theme.light[selectorColor])
                    polygon(p, 2f)
                    p.setPosition(0f, 0f)
                }
            }
        }
    }

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
     * Approximate field coord of the direction arroe at given side
     */
    fun arrowCenter(side: Side): Vector2 {
        if (directionArrows == null)
            directionArrows = buildDirectionArrows()
        return directionArrows!!.first { (s, _) -> s == side }.second.boundingRectangle.getCenter(v)
    }

    /**
     * Serialize the intent selectors
     */
    fun serialize(sb: StringBuilder) {
        sb.append(side.ordinal).append(selectorColor)
            .append(if (selectorSegment == null) '-' else selectorSegment?.type?.ordinal)
    }

    /**
     * Deserialize the intent selectors
     */
    fun deserialize(s: String, i: Int): Int {
        selectorColor = s[i].digitToInt()
        if (s[i + 1] != '-') {
            val type = SegmentType.entries[s[i + 1].digitToInt()]
            selectorSegment = tile.segment.first { it.type == type }
        }
        return i + 2
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