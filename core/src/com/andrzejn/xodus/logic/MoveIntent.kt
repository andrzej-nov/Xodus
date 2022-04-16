package com.andrzejn.xodus.logic

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
     * Possible directions of this selector
     */
    private val directions: List<Side> = segments.flatMap { it.type.sides.toList() }.filter { it != side }

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

}