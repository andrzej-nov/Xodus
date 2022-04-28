package com.andrzejn.xodus.helper

import com.andrzejn.xodus.Context
import com.andrzejn.xodus.logic.Coord
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.StringBuilder
import kotlin.math.floor

open class CoordProcessor constructor(private val ctx: Context) {
    /**
     * Logical field coords to screen coords offset.
     * E.g. if the field has been visually scrolled by 1 cell to the right, then scrollOffset.x = 1
     */
    private val scrollOffset: Coord = Coord(0, 0)
    private val bottomLeft = Coord(0, 0)
    private val topRight = Coord(ctx.gs.fieldSize - 1, ctx.gs.fieldSize - 1)

    /**
     * The field cell side length
     */
    var sideLen: Float = 0f

    /**
     * Variables for internal calculations to reduce the GC load
     */
    private val v = Vector2()
    private val c = Coord()

    /**
     * The whole field square size, in pixels
     */
    var wholeFieldSize: Float = 0f

    /**
     * Variable for internal calculations, to reduce GC load
     */
    private val vrend = Vector2()

    /**
     * A variable for internal calculations, to reduce GC load
     */
    private val v3 = Vector3()
    private val vpp = Vector2()
    private val vppf = Vector2()

    /**
     * Fit a sprite into given rectangle, retaining proportions
     */
    fun fitToRect(s: Sprite, wBound: Float, hBound: Float) {
        var width = wBound
        var height = wBound * s.regionHeight / s.regionWidth
        if (height > hBound) {
            height = hBound
            width = hBound * s.regionWidth / s.regionHeight
        }
        s.setSize(width, height)
    }

    /**
     * Reset scroll offset to zero
     */
    fun resetScrollOffset() {
        scrollOffset.set(0, 0)
        ctx.centerFieldCamera()
        recalculateFieldCorners()
    }

    /**
     * Serialize the scroll offset
     */
    fun serialize(sb: StringBuilder) {
        sb.append(String.format("%03d", scrollOffset.x)).append(String.format("%03d", scrollOffset.y))
    }


    /**
     * Deserialize the scroll offset
     */
    fun deserialize(s: String, i: Int): Int {
        val cx = s.substring(i..i + 2).toInt()
        val cy = s.substring(i + 3..i + 5).toInt()
        scrollOffset.set(cx, cy)
        recalculateFieldCorners()
        return i + 6
    }

    /**
     * Applies scrolling to the field
     */
    fun scrollFieldBy(change: Coord) {
        ctx.fieldCamPos.x += sideLen * change.x
        ctx.fieldCamPos.y += sideLen * change.y
        scrollOffset.add(change)
        scrollOffset.set(clipWrap(scrollOffset.x), clipWrap(scrollOffset.y))
        recalculateFieldCorners()
    }

    /**
     * Determine where on the logical field the visual field corners are placed
     */
    private fun recalculateFieldCorners() {
        bottomLeft.set(fieldIndexToTileIndex(bottomLeft.set(0, 0)))
        topRight.set(fieldIndexToTileIndex(topRight.set(ctx.gs.fieldSize - 1, ctx.gs.fieldSize - 1)))
    }

    /**
     * Set the tile corner screen coordinates, considering scrolling
     */
    fun setTileBasePos(crd: Coord, basePos: Vector2) {
        c.set(tileIndexToFieldIndex(crd))
        basePos.set(c.x * sideLen, c.y * sideLen)
    }

    /**
     * Convert the provided coords from the cell pointed on the screen to the logical field tile indexes.
     * Returns the converted coord for chaining.
     */
    fun fieldIndexToTileIndex(crd: Coord): Coord =
        c.set(clipWrap(crd.x - scrollOffset.x), clipWrap(crd.y - scrollOffset.y))

    /**
     * Convert the provided coords from the logical field tile indexes to the cell pointed on the field
     * Returns the converted coord for chaining.
     */
    fun tileIndexToFieldIndex(crd: Coord): Coord =
        c.set(clipWrap(crd.x + scrollOffset.x), clipWrap(crd.y + scrollOffset.y))

    /**
     * Convert the provided Y coord from the logical field tile index to the cell pointed on the field
     */
    fun tileYToFieldY(y: Float): Float = clipWrap(y + scrollOffset.y)

    /**
     * Converts screen cell indexes to the bottom-left screen coordinates to draw the rectangle
     * Sets private var v to the same value
     */
    fun toScreenCellCorner(c: Coord): Vector2 {
        val v = toFieldCellCorner(c)
        v3.set(v.x, v.y, 0f)
        ctx.field.project(v3)
        return v.set(v3.x, v3.y)
    }

    /**
     * Converts screen cell indexes to the bottom-left screen coordinates to draw the rectangle
     * Sets private var v to the same value
     */
    fun toFieldCellCorner(c: Coord): Vector2 = v.set(c.x.toFloat(), c.y.toFloat()).scl(sideLen)

    /**
     * Returns indexes of the screen cell pointed by touch/mouse. (-1, -1) of pointed otside of the field.
     * Sets private var c to the same return value
     */
    fun toScreenIndex(v: Vector2): Coord {
        with(ctx.field) {
            if (v.x !in screenX.toFloat()..screenX.toFloat() + wholeFieldSize ||
                v.y !in screenY.toFloat()..screenY.toFloat() + wholeFieldSize
            ) return c.unSet()
            return c.set(
                floor((v.x - screenX) / sideLen).toInt(),
                floor((v.y - screenY) / sideLen).toInt()
            )
        }
    }

    /**
     * Returns indexes of the field cell pointed by touch/mouse. (-1, -1) of pointed otside of the field.
     * Sets private var c to the same return value
     */
    fun toFieldIndex(v: Vector2): Coord {
        if (v.x !in -sideLen..wholeFieldSize + sideLen || v.y !in -sideLen..wholeFieldSize + sideLen) return c.unSet()
        return c.set(floor(v.x / sideLen).toInt(), floor(v.y / sideLen).toInt())
    }

    /**
     * Perform given render at given coordinates, and repeat it at respective duplicate positions
     * if it is at the field border tile
     */
    fun renderWithFieldBorders(v: Vector2, c: Coord, renderIt: (Vector2) -> Unit) {
        var xAtBorder = false
        renderIt(v)
        if (c.x == bottomLeft.x) {
            xAtBorder = true
            renderIt(vrend.set(v).add(wholeFieldSize, 0f))
        } else if (c.x == topRight.x) {
            xAtBorder = true
            renderIt(vrend.set(v).add(-wholeFieldSize, 0f))
        }
        if (xAtBorder) {
            if (c.y == bottomLeft.y)
                renderIt(vrend.add(0f, wholeFieldSize))
            else if (c.y == topRight.y)
                renderIt(vrend.add(0f, -wholeFieldSize))
        }
        if (c.y == bottomLeft.y)
            renderIt(vrend.set(v).add(0f, wholeFieldSize))
        else if (c.y == topRight.y)
            renderIt(vrend.set(v).add(0f, -wholeFieldSize))
    }

    /**
     * Ensures the index is within (0  until fieldSize), wrapping through another side as necessary.
     */
    fun clipWrap(c: Int): Int {
        val fieldSize = ctx.gs.fieldSize
        if (c < 0)
            return c + (-(c + 1) / fieldSize + 1) * fieldSize
        if (c >= fieldSize)
            return c - (c / fieldSize) * fieldSize
        return c
    }

    /**
     * Ensures the float index is within (0  until fieldSize), wrapping through another side as necessary.
     */
    fun clipWrap(x: Float): Float {
        val fieldSize = ctx.gs.fieldSize
        if (x < 0)
            return x + ((-(x + 1) / fieldSize).toInt() + 1) * fieldSize
        if (x >= fieldSize)
            return x - (x / fieldSize).toInt() * fieldSize
        return x
    }

    /**
     * Ensures the field coord is within (0  until wholeFieldSize), wrapping through another side as necessary.
     */
    fun clipWrapCoord(x: Float): Float {
        if (x < 0)
            return x + wholeFieldSize
        if (x >= wholeFieldSize)
            return x - wholeFieldSize
        return x
    }

    /**
     * Convert the UI screen coordinates (mouse clicks or touches, for example) to the OpenGL scene coordinates
     * which are used for drawing
     */
    fun pointerPositionScreen(screenX: Int, screenY: Int): Vector2 {
        v3.set(screenX.toFloat(), screenY.toFloat(), 0f)
        ctx.screen.unproject(v3)
        return vpp.set(v3.x, v3.y)
    }

    /**
     * Convert the UI screen coordinates (mouse clicks or touches, for example) to the OpenGL field coordinates
     */
    fun pointerPositionField(screenX: Int, screenY: Int): Vector2 {
        v3.set(screenX.toFloat(), screenY.toFloat(), 0f)
        ctx.field.unproject(v3)
        return vppf.set(v3.x, v3.y)
    }
}