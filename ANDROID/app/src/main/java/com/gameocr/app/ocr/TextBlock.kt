package com.gameocr.app.ocr

import android.graphics.Rect

/**
 * 一段识别出的文本（一般对应原图里的一个文本块）。
 * [boundingBox] 用屏幕坐标系，方便 overlay 直接贴在原文下方。
 */
data class TextBlock(
    val text: String,
    val boundingBox: Rect,
    val confidence: Float = 1f,
    val recognizedLanguage: String? = null,
    val layoutOrientation: TextOrientation? = null,
    val sourceBoxes: List<Rect> = emptyList(),
    val bubbleGroupId: Int? = null,
    val historyId: Long? = null,
    // SEMANTIC_DESTINATION_BLOCKS_ANDROID_V1
    val destinationId: String? = null,
    val semanticRole: String? = null,
    val sourceFragmentIds: List<String> = emptyList(),
)

internal fun TextBlock.sourceBoxesOrBoundingBox(): List<Rect> =
    sourceBoxes.takeIf { it.isNotEmpty() }?.map(::Rect) ?: listOf(Rect(boundingBox))

internal fun mergeSourceBoxes(first: TextBlock, second: TextBlock): List<Rect> =
    first.sourceBoxesOrBoundingBox() + second.sourceBoxesOrBoundingBox()

internal fun TextBlock.withFallbackLayoutOrientation(
    fallback: TextOrientation,
): TextBlock =
    if (layoutOrientation == null || layoutOrientation == TextOrientation.UNKNOWN) {
        copy(layoutOrientation = fallback)
    } else {
        this
    }
