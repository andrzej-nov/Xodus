package com.andrzejn.xodus.logic

/**
 * Values that do not change during the game (except for the screen resize)
 */
class WorldConstants(
) {
    /**
     * Screen width
     */
    var width: Float = 1f

    /**
     * Screen height
     */
    var height: Float = 1f

    fun setValues(w: Float, h: Float) {
        width = w
        height = h
    }
}