package com.andrzejn.xodus.logic

/**
 * Type of the segment. Also references the sides it connects.
 */
@Suppress("KDocMissingDocumentation")
enum class SegmentType(val sides: Array<Side>) {
    LineBT(arrayOf(Side.Bottom, Side.Top)),
    LineLR(arrayOf(Side.Left, Side.Right)),
    ArcLT(arrayOf(Side.Left, Side.Top)),
    ArcTR(arrayOf(Side.Top, Side.Right)),
    ArcRB(arrayOf(Side.Right, Side.Bottom)),
    ArcBL(arrayOf(Side.Bottom, Side.Left));

    /**
     * Returns complemental segment type (the one that connects other two tile sides)
     */
    fun complement(): SegmentType = when (this) {
        LineBT -> LineLR
        LineLR -> LineBT
        ArcLT -> ArcRB
        ArcTR -> ArcBL
        ArcRB -> ArcLT
        ArcBL -> ArcTR
    }
}