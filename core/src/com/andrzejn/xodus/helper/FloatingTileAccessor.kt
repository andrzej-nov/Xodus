package com.andrzejn.xodus.helper

import aurelienribon.tweenengine.TweenAccessor

/**
 * Tween tile position
 */
const val TW_POS_XY: Int = 1

/**
 * Used by the Tween Engine to access field properties
 */
class FloatingTileAccessor : TweenAccessor<FloatingTile> {
    /**
     * Gets one or many values from the target object associated to the given tween type.
     * It is used by the Tween Engine to determine starting values.
     */
    override fun getValues(target: FloatingTile?, tweenType: Int, returnValues: FloatArray?): Int {
        when (tweenType) {
            TW_POS_XY -> {
                returnValues!![0] = target!!.position.x
                returnValues[1] = target.position.y
                return 2
            }
        }
        return 1
    }

    /**
     * This method is called by the Tween Engine each time
     * a running tween associated with the current target object has been updated.
     */
    override fun setValues(target: FloatingTile?, tweenType: Int, newValues: FloatArray?) {
        when (tweenType) {
            TW_POS_XY -> {
                (target ?: return).position.x = (newValues ?: return)[0]
                target.position.y = newValues[1]
            }
        }
    }
}