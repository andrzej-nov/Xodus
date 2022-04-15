package com.andrzejn.xodus.logic

/**
 * Tile side
 */
@Suppress("KDocMissingDocumentation")
enum class Side {
    Top, Right, Bottom, Left;

    val otherSide: Side
        get() = when (this) {
            Top -> Bottom
            Right -> Left
            Bottom -> Top
            Left -> Right
        }
}