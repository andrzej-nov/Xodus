package com.andrzejn.xodus.helper

import aurelienribon.tweenengine.TweenAccessor
import com.andrzejn.xodus.logic.Blot
import com.andrzejn.xodus.logic.Field

/**
 * Tween balls moving position
 */
const val TW_SCATTER: Int = 3

/**
 * Used by the Tween Engine to access field properties
 */
class BlotAccessor : TweenAccessor<Blot> {
    /**
     * Gets one or many values from the target object associated to the given tween type.
     * It is used by the Tween Engine to determine starting values.
     */
    override fun getValues(target: Blot?, tweenType: Int, returnValues: FloatArray?): Int {
        when (tweenType) {
            TW_SCATTER -> returnValues!![0] = target!!.scatter
        }
        return 1
    }

    /**
     * This method is called by the Tween Engine each time
     * a running tween associated with the current target object has been updated.
     */
    override fun setValues(target: Blot?, tweenType: Int, newValues: FloatArray?) {
        when (tweenType) {
            TW_SCATTER -> (target ?: return).scatter = (newValues ?: return)[0]
        }
    }
}