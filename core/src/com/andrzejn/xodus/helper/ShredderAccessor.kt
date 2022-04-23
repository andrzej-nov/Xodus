package com.andrzejn.xodus.helper

import aurelienribon.tweenengine.TweenAccessor
import com.andrzejn.xodus.logic.Shredder

/**
 * Tween balls moving position
 */
const val TW_Y: Int = 4

/**
 * Used by the Tween Engine to access field properties
 */
class ShredderAccessor : TweenAccessor<Shredder> {
    /**
     * Gets one or many values from the target object associated to the given tween type.
     * It is used by the Tween Engine to determine starting values.
     */
    override fun getValues(target: Shredder?, tweenType: Int, returnValues: FloatArray?): Int {
        when (tweenType) {
            TW_Y -> returnValues!![0] = target!!.y
        }
        return 1
    }

    /**
     * This method is called by the Tween Engine each time
     * a running tween associated with the current target object has been updated.
     */
    override fun setValues(target: Shredder?, tweenType: Int, newValues: FloatArray?) {
        when (tweenType) {
            TW_Y -> (target ?: return).y = (newValues ?: return)[0]
        }
    }
}