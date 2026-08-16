package com.gameocr.app.data

import com.gameocr.app.capture.LoopFrameStabilityPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsDefaultsTest {

    @Test
    fun overlayFont_defaultsToSystemFont() {
        val settings = Settings()

        assertEquals("", settings.overlayFontFileName)
        assertEquals("", settings.overlayFontDisplayName)
        assertEquals(emptyList<OverlayFontEntry>(), settings.overlayFonts)
    }

    @Test
    fun developerDiagnostics_defaultToOff() {
        val settings = Settings()
        assertEquals("developer options", false, settings.developerOptionsEnabled)
        assertEquals("disable translation cache", false, settings.disableTranslationCache)
    }

    @Test
    fun translationContext_defaults_areConservativeAndFollowRecognition() {
        val settings = Settings()

        assertEquals(true, settings.translationOutputFollowRecognition)
        val output = resolveTranslationOutputSettings(
            settings.translationOutputFollowRecognition,
            settings.translationOutputLayout,
            settings.translationOutputDirection,
        )
        assertEquals(true, output.followRecognition)
        assertEquals(TranslationOutputLayout.HORIZONTAL, output.layout)
        assertEquals(TranslationOutputDirection.LEFT_TO_RIGHT, output.direction)
        assertEquals(true, settings.translationGlossaryEnabled)
        assertEquals(ForegroundAppDetectionMode.AUTO, settings.foregroundAppDetectionMode)
        assertEquals(false, settings.sendAppNameToTranslator)
    }

    @Test
    fun translationBlocks_defaultToVisibleCopyButtons() {
        assertEquals(
            TranslationBlockInteractionMode.COPY_BUTTON,
            Settings().translationBlockInteractionMode,
        )
    }

    @Test
    fun loopTextStableDuration_defaultsTo500MillisecondsAcrossSettingsAndRuntimePolicy() {
        assertEquals(500L, Settings().loopTextStableDurationMs)
        assertEquals(500L, LoopFrameStabilityPolicy.DEFAULT_STABLE_DURATION_MS)
    }

    @Test
    fun commonDefaults_includeFloatingWindowAndTimeouts() {
        val settings = Settings()
        assertEquals(320, settings.floatingWindowWidthDp)
        assertEquals(180, settings.floatingWindowHeightDp)
        assertEquals(30, settings.apiTimeoutSeconds)
        assertEquals(emptyList<String>(), settings.pinnedLanguages)
        assertEquals(TranslatorEngine.REMOTE_PC, settings.translatorEngine)
    }
}
