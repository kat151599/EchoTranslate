package com.gameocr.app.ocr

import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Test

class TextBlockLayoutOrientationTest {

    @Test
    fun withFallbackLayoutOrientation_tableDriven_preservesPerBlockDetection() {
        data class Case(
            val name: String,
            val detected: TextOrientation?,
            val fallback: TextOrientation,
            val expected: TextOrientation,
        )

        val cases = listOf(
            Case(
                "horizontal title survives vertical page fallback",
                TextOrientation.HORIZONTAL_LTR,
                TextOrientation.VERTICAL_RTL,
                TextOrientation.HORIZONTAL_LTR,
            ),
            Case(
                "vertical bubble survives horizontal page fallback",
                TextOrientation.VERTICAL_RTL,
                TextOrientation.HORIZONTAL_LTR,
                TextOrientation.VERTICAL_RTL,
            ),
            Case(
                "missing detection receives fallback",
                null,
                TextOrientation.VERTICAL_RTL,
                TextOrientation.VERTICAL_RTL,
            ),
            Case(
                "unknown detection receives fallback",
                TextOrientation.UNKNOWN,
                TextOrientation.HORIZONTAL_LTR,
                TextOrientation.HORIZONTAL_LTR,
            ),
        )

        cases.forEach { case ->
            val block = TextBlock(
                text = "text",
                boundingBox = Rect(0, 0, 100, 40),
                layoutOrientation = case.detected,
            )
            assertEquals(
                case.name,
                case.expected,
                block.withFallbackLayoutOrientation(case.fallback).layoutOrientation,
            )
        }
    }
}
