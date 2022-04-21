package com.andrzejn.xodus.helper

import aurelienribon.tweenengine.TweenAccessor
import com.andrzejn.xodus.logic.Field

/**
 * Tween balls moving position
 */
const val TW_POSITION: Int = 2

/**
 * Used by the Tween Engine to access field properties
 */
class FieldAccessor : TweenAccessor<Field> {
    /**
     * Gets one or many values from the target object associated to the given tween type.
     * It is used by the Tween Engine to determine starting values.
     */
    override fun getValues(target: Field?, tweenType: Int, returnValues: FloatArray?): Int {
        when (tweenType) {
            TW_POSITION -> returnValues!![0] = target!!.ballPosition
        }
        return 1
    }

    /**
     * This method is called by the Tween Engine each time
     * a running tween associated with the current target object has been updated.
     */
    override fun setValues(target: Field?, tweenType: Int, newValues: FloatArray?) {
        when (tweenType) {
            TW_POSITION -> (target ?: return).ballPosition = (newValues ?: return)[0]
        }
    }
}