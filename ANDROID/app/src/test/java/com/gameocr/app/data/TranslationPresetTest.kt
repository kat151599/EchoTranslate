package com.gameocr.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslationPresetTest {

    @Test
    fun translationPresetRoundTripsDictionaryPrompt() {
        val base = Settings(dictionaryPrompt = "custom dictionary prompt")
        val preset = TranslationPresetCatalog.fromSettings(
            id = "custom_dictionary",
            name = "Dictionary",
            shortName = "Dict",
            settings = base
        )
        val applied = preset.applyTo(Settings(dictionaryPrompt = "old prompt"))

        assertEquals("custom dictionary prompt", preset.dictionaryPrompt)
        assertEquals("custom dictionary prompt", applied.dictionaryPrompt)
        assertTrue(TranslationPresetCatalog.matchesSettings(preset, base))
        assertFalse(
            TranslationPresetCatalog.matchesSettings(
                preset,
                base.copy(dictionaryPrompt = "changed dictionary prompt")
            )
        )
    }

    @Test
    fun translationPresetRoundTripsGlobalOverlayTextStyle() {
        val style = OverlayTextStyle(
            bold = true,
            italic = true,
            underline = true,
            letterSpacingEm = 0.12f,
            lineSpacingMultiplier = 1.4f,
            alignment = OverlayTextAlignment.CENTER,
            strokeEnabled = true,
            strokeWidthDp = 2.5f,
            strokeColor = 0xFF102030.toInt(),
            shadowEnabled = true,
            shadowRadiusDp = 5f,
            shadowOffsetXDp = -2f,
            shadowOffsetYDp = 3f,
            shadowColor = 0xAA405060.toInt()
        )
        val base = Settings(
            overlayTheme = OverlayTheme.PAPER_LIGHT,
            overlayTextStyle = style
        )
        val preset = TranslationPresetCatalog.fromSettings(
            id = "custom_text_style",
            name = "Text style",
            shortName = "Style",
            settings = base
        )
        val applied = preset.applyTo(Settings(overlayTheme = OverlayTheme.CUSTOM))

        assertEquals(style, preset.overlayTextStyle)
        assertEquals(style, applied.overlayTextStyle)
        assertEquals(OverlayTheme.PAPER_LIGHT, applied.overlayTheme)
        assertTrue(TranslationPresetCatalog.matchesSettings(preset, base))
    }

    @Test
    fun translationPreset_simpleRoundTrip_preservesIdAndNameAndSettingsHash() {
        val base = Settings(sourceLang = "ja", targetLang = "zh-CN", promptTemplate = "p")
        val preset = TranslationPresetCatalog.fromSettings(
            id = "custom_roundtrip",
            name = "Roundtrip",
            shortName = "Round",
            settings = base,
        )
        val applied = preset.applyTo(Settings())
        val rebuilt = TranslationPresetCatalog.fromSettings(
            id = preset.id,
            name = preset.name,
            shortName = preset.shortName,
            settings = applied,
        )

        assertEquals(preset.id, rebuilt.id)
        assertEquals(preset.name, rebuilt.name)
        assertTrue(preset.settingsHash.isNotBlank())
        assertEquals(preset.settingsHash, rebuilt.settingsHash)
    }
}
