package com.andrzejn.xodus.logic

/**
 * The ball move intent from the tile side by one of tile segments to another side
 */
class MoveIntent(segments: List<TrackSegment>) {
    /**
     * Planned track color
     */
    var color: Int = 0

    /**
     * Sequential number of the segment in respective planned ball track, to determine which ball comes first
     * and if there is a collison conflict
     */
    var trackStep: Int = 0

    private val defaultSegment = if (segments.size == 1) segments.first() else null

    /**
     * Planned segment to move, or null when there is no segment selected yet (e.g. when the player should
     * select the segment from several possibilities)
     */
    var segment: TrackSegment? = defaultSegment

    /**
     * Clear the intent (before the intents planning)
     */
    fun clear() {
        color = 0
        trackStep = 0
        segment = defaultSegment
    }
}