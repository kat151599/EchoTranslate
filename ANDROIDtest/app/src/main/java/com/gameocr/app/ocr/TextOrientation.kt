package com.gameocr.app.ocr

import kotlinx.serialization.Serializable

@Serializable
enum class TextOrientation {
    HORIZONTAL_LTR,
    HORIZONTAL_RTL,
    VERTICAL_RTL,
    VERTICAL_LTR,
    STACKED,
    UNKNOWN
}
