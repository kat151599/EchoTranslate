package com.gameocr.app.ocr

import com.gameocr.app.ocr.BubbleClusterer.IntRect
import kotlin.math.ceil
import kotlin.math.floor

/**
 * A local, transparent overlay patch in the segmentation image coordinate system.
 *
 * Only repaired background pixels and rendered translation glyphs are non-transparent. Keeping the
 * rest transparent avoids freezing unrelated screen content while loop translation is active.
 */
internal data class ShapeAwareBubblePatch(
    val modelBubbleIndex: Int,
    val bounds: IntRect,
    val pixels: IntArray,
    val coordinateScale: Float,
    val blockIndices: List<Int>,
) {
    init {
        require(bounds.width > 0 && bounds.height > 0)
        require(pixels.size == bounds.width * bounds.height)
        require(coordinateScale > 0f)
        require(blockIndices.isNotEmpty())
    }

    fun displayBounds(): IntRect = scaledPatchBounds(
        bounds = bounds,
        coordinateScale = coordinateScale,
    )
}

internal object ShapeAwareBubblePatchComposer {

    /**
     * Copies only confirmed repair pixels inside the model mask into a transparent local image.
     */
    fun composeBackground(
        imageWidth: Int,
        imageHeight: Int,
        repairedPixels: IntArray,
        repairedMask: BooleanArray,
        modelMask: BubbleSegmentationPostprocessor.InstanceMask,
    ): IntArray {
        require(imageWidth > 0 && imageHeight > 0)
        require(repairedPixels.size == imageWidth * imageHeight)
        require(repairedMask.size == imageWidth * imageHeight)
        require(modelMask.pixels.size == modelMask.width * modelMask.height)

        val output = IntArray(modelMask.width * modelMask.height)
        for (localY in 0 until modelMask.height) {
            val imageY = modelMask.top + localY
            if (imageY !in 0 until imageHeight) continue
            for (localX in 0 until modelMask.width) {
                val localIndex = localY * modelMask.width + localX
                if (!modelMask.pixels[localIndex]) continue
                val imageX = modelMask.left + localX
                if (imageX !in 0 until imageWidth) continue
                val imageIndex = imageY * imageWidth + imageX
                if (repairedMask[imageIndex]) {
                    output[localIndex] = repairedPixels[imageIndex]
                }
            }
        }
        return output
    }
}

internal fun scaledPatchBounds(
    bounds: IntRect,
    coordinateScale: Float,
): IntRect {
    require(bounds.width > 0 && bounds.height > 0)
    require(coordinateScale > 0f)
    val left = floor(bounds.left / coordinateScale).toInt()
    val top = floor(bounds.top / coordinateScale).toInt()
    val right = ceil(bounds.right / coordinateScale).toInt().coerceAtLeast(left + 1)
    val bottom = ceil(bounds.bottom / coordinateScale).toInt().coerceAtLeast(top + 1)
    return IntRect(left, top, right, bottom)
}
