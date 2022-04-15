package com.andrzejn.xodus.logic

/**
 * Displayed at the tile side when there are several possible segments to continue the planned ball track
 * and the player must choose.
 * Once selected, the segment remains selected and cannot be changed.
 * The selector is reset when there is no more MoveIntent on that side (when the ball has passed the side
 * or when due to changes on the field there is no more planned track through that side)
 */
class MoveSelector(
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
     * Possible directions of this selector
     */
    val directions: List<Side> =
        tile.segment.filter { it.type.sides.contains(side) }.flatMap { it.type.sides.toList() }.filter { it != side }

    /**
     * Selector color, when there is a planned ball track here
     */
    var color: Int = 0

    /**
     * The tile segment selected for moving through this tile side, on null if there are several possible
     * segments and the player has not selected yet
     */
    var segment: TrackSegment? = defaultSelection()

    /**
     * When there is only single segment from this side, there is no need of selector and it is always
     * hardwired to that single direction
     */
    private fun defaultSelection() = if (directions.size == 1) tile.segment.first() else null

    /**
     * Reset the selector, when the ball has already moved through it, or there is no more planned ball track here
     * due to the changes on the field
     */
    fun reset() {
        color = 0
        segment = defaultSelection()
    }
}