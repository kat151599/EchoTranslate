package com.gameocr.app.data

import android.content.Context
import android.content.ContextWrapper
import com.gameocr.app.capture.CaptureRegion
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsRepositoryBehaviorTest {

    @Test
    fun mainStatusPresetSeen_tableDriven_persistsDiscoveryState() = runBlocking {
        data class Case(
            val name: String,
            val markSeen: Boolean,
            val expectedSeen: Boolean,
        )

        listOf(
            Case("fresh install has not discovered presets", markSeen = false, expectedSeen = false),
            Case("visiting presets is persisted", markSeen = true, expectedSeen = true),
        ).forEach { case ->
            val repository = fileBackedRepository(
                Files.createTempDirectory("settings-main-preset-seen-test").toFile()
            )

            if (case.markSeen) repository.markMainStatusPresetSeen()

            assertEquals(case.name, case.expectedSeen, repository.hasSeenMainStatusPreset())
        }
    }

    @Test
    fun translationLanguagePair_tableDriven_rejectsConflictingUpdates() = runBlocking {
        data class Case(
            val name: String,
            val requestedSource: String,
            val requestedTarget: String,
            val expectedSource: String,
            val expectedTarget: String,
        )

        listOf(
            Case("valid pair is stored", "ja", "zh-CN", "ja", "zh-CN"),
            Case("same source and target are rejected", "en", "en", "auto", "zh-CN"),
            Case("case-only conflict is rejected", " JA ", "ja", "auto", "zh-CN"),
            Case("regional variants remain valid", "zh-CN", "zh-TW", "zh-CN", "zh-TW"),
        ).forEach { case ->
            val repository = fileBackedRepository(
                Files.createTempDirectory("settings-language-pair-test").toFile()
            )

            repository.update {
                it.copy(sourceLang = Languages.AUTO.code, targetLang = Languages.ZH_CN.code)
            }
            repository.update {
                it.copy(
                    sourceLang = case.requestedSource,
                    targetLang = case.requestedTarget,
                )
            }

            val actual = repository.get()
            assertEquals("${case.name} source", case.expectedSource, actual.sourceLang)
            assertEquals("${case.name} target", case.expectedTarget, actual.targetLang)
        }
    }

    @Test
    fun rescaleCaptureRegion_tableDriven_migratesWorkspaceAndOrientationCoordinates() = runBlocking {
        data class Case(
            val name: String,
            val region: CaptureRegion,
            val savedWidth: Int,
            val savedHeight: Int,
            val currentWidth: Int,
            val currentHeight: Int,
            val expectedRegion: CaptureRegion,
        )

        val cases = listOf(
            Case(
                name = "old HyperOS workspace width migrates to physical width",
                region = CaptureRegion(0, 0, 3053, 1440),
                savedWidth = 3053,
                savedHeight = 1440,
                currentWidth = 3200,
                currentHeight = 1440,
                expectedRegion = CaptureRegion(0, 0, 3200, 1440),
            ),
            Case(
                name = "same screen keeps coordinates",
                region = CaptureRegion(100, 200, 1000, 900),
                savedWidth = 3200,
                savedHeight = 1440,
                currentWidth = 3200,
                currentHeight = 1440,
                expectedRegion = CaptureRegion(100, 200, 1000, 900),
            ),
            Case(
                name = "orientation change scales both axes",
                region = CaptureRegion(144, 320, 720, 1600),
                savedWidth = 1440,
                savedHeight = 3200,
                currentWidth = 3200,
                currentHeight = 1440,
                expectedRegion = CaptureRegion(320, 144, 1600, 720),
            ),
            Case(
                name = "scaled out of range coordinates clamp to physical screen",
                region = CaptureRegion(-100, -100, 4000, 2000),
                savedWidth = 1600,
                savedHeight = 720,
                currentWidth = 3200,
                currentHeight = 1440,
                expectedRegion = CaptureRegion(0, 0, 3200, 1440),
            ),
            Case(
                name = "missing saved metadata preserves region",
                region = CaptureRegion(120, 240, 960, 1200),
                savedWidth = 0,
                savedHeight = 0,
                currentWidth = 1440,
                currentHeight = 3200,
                expectedRegion = CaptureRegion(120, 240, 960, 1200),
            ),
        )

        val root = Files.createTempDirectory("settings-region-rescale-test").toFile()
        val repository = fileBackedRepository(root)
        cases.forEach { case ->
            repository.update {
                Settings(
                    captureRegion = case.region,
                    captureRegionSavedScreenW = case.savedWidth,
                    captureRegionSavedScreenH = case.savedHeight,
                )
            }
            repository.rescaleCaptureRegionIfNeeded(case.currentWidth, case.currentHeight)
            val actual = repository.get()
            assertEquals("${case.name} region", case.expectedRegion, actual.captureRegion)
            assertEquals("${case.name} saved width", case.currentWidth, actual.captureRegionSavedScreenW)
            assertEquals("${case.name} saved height", case.currentHeight, actual.captureRegionSavedScreenH)
        }
    }

    @Test
    fun repository_roundTripsACompleteNonDefaultSettingsObject() = runBlocking {
        val root = Files.createTempDirectory("settings-repository-test").toFile()
        val repository = fileBackedRepository(root)
        val fontName = "${"b".repeat(64)}.ttf"
        val preset = TranslationPreset(id = "custom_roundtrip", name = "Round trip")
        val requested = Settings().copy(
            sourceLang = "ja",
            targetLang = "zh-TW",
            promptTemplate = "roundtrip prompt",
            captureLoopIntervalMs = 4321L,
            loopTriggerMode = LoopTriggerMode.FIXED_INTERVAL,
            captureRegion = CaptureRegion(11, 22, 333, 444),
            captureRegionSavedScreenW = 1920,
            captureRegionSavedScreenH = 1080,
            overlayTextSizeSp = 22,
            overlayTextStyle = OverlayTextStyle(
                bold = true,
                italic = true,
                underline = true,
                letterSpacingEm = 0.12f,
                lineSpacingMultiplier = 1.6f,
                alignment = OverlayTextAlignment.END,
                strokeEnabled = true,
                shadowEnabled = true,
            ),
            overlayAlpha = 0.62f,
            overlayFontFileName = fontName,
            overlayFontDisplayName = "Roundtrip.ttf",
            overlayFonts = listOf(OverlayFontEntry(fontName, "Roundtrip.ttf")),
            streamingTranslate = false,
            retryEmptyTranslation = true,
            renderMode = RenderMode.FLOATING_WINDOW,
            translationBlockInteractionMode = TranslationBlockInteractionMode.OPEN_COPY_PANEL,
            overlayPlacement = OverlayPlacement.ABOVE,
            overlayTheme = OverlayTheme.CUSTOM,
            customBgColor = 0xAA102030.toInt(),
            customFgColor = 0xFF405060.toInt(),
            customBorderColor = 0xCC708090.toInt(),
            customBorderWidth = 4,
            overlayOffsetX = 31,
            overlayOffsetY = -17,
            preferShizukuCapture = true,
            a11yVolumeTrigger = true,
            translatorEngine = TranslatorEngine.REMOTE_PC,
            remotePcBaseUrl = "https://roundtrip.remotepc/",
            remotePcApiKey = "roundtrip-remote-key",
            translationGlossaryEnabled = false,
            foregroundAppDetectionMode = ForegroundAppDetectionMode.USAGE_ACCESS,
            sendAppNameToTranslator = true,
            floatingButtonSizeDp = 53,
            floatingButtonX = 101,
            floatingButtonY = 202,
            floatingButtonSnapToEdge = false,
            floatingButtonAutoDock = true,
            floatingButtonDockInsetDp = 17,
            floatingWindowX = 303,
            floatingWindowY = 404,
            floatingWindowWidthDp = 455,
            floatingWindowHeightDp = 233,
            floatingWindowContentMode = FloatingWindowContentMode.DST_ONLY,
            floatingWindowLocked = true,
            customBorderStyle = BorderStyle.DOTTED,
            overlayAllowWrap = false,
            overlayAvoidCollision = false,
            apiTimeoutSeconds = 47,
            disableCrossLineContextTranslation = true,
            pinnedLanguages = listOf("ja", "zh-TW", "en"),
            cleartextAllowedHosts = listOf("192.168.0.2", "localhost"),
            floatingMenuItemOrder = FloatingMenu.DEFAULT_ORDER.reversed(),
            arcMenuPageSize = 5,
            floatingButtonSkill = FloatingSkill.LOOP,
            dictionaryPrompt = "roundtrip dictionary",
            translationPresets = listOf(preset),
            activeTranslationPresetId = preset.id,
        )

        repository.update { requested }

        val actual = repository.get()

        // Assert representative persisted fields
        assertEquals("sourceLang", requested.sourceLang, actual.sourceLang)
        assertEquals("targetLang", requested.targetLang, actual.targetLang)
        assertEquals("promptTemplate", requested.promptTemplate, actual.promptTemplate)
        assertEquals("captureRegion", requested.captureRegion, actual.captureRegion)
        assertEquals("captureRegionSavedW", requested.captureRegionSavedScreenW, actual.captureRegionSavedScreenW)
        assertEquals("captureRegionSavedH", requested.captureRegionSavedScreenH, actual.captureRegionSavedScreenH)
        assertEquals("overlayFontFileName", requested.overlayFontFileName, actual.overlayFontFileName)
        assertEquals("overlayFontDisplayName", requested.overlayFontDisplayName, actual.overlayFontDisplayName)
        assertEquals("overlayFonts size", requested.overlayFonts.size, actual.overlayFonts.size)
        assertEquals("streamingTranslate", requested.streamingTranslate, actual.streamingTranslate)
        assertEquals("retryEmptyTranslation", requested.retryEmptyTranslation, actual.retryEmptyTranslation)
        assertEquals("renderMode", requested.renderMode, actual.renderMode)
        assertEquals("translationBlockInteractionMode", requested.translationBlockInteractionMode, actual.translationBlockInteractionMode)
        assertEquals("overlayPlacement", requested.overlayPlacement, actual.overlayPlacement)
        assertEquals("overlayTheme", requested.overlayTheme, actual.overlayTheme)
        assertEquals("customBgColor", requested.customBgColor, actual.customBgColor)
        assertEquals("overlayOffsetX", requested.overlayOffsetX, actual.overlayOffsetX)
        assertEquals("overlayOffsetY", requested.overlayOffsetY, actual.overlayOffsetY)
        assertEquals("preferShizukuCapture", requested.preferShizukuCapture, actual.preferShizukuCapture)
        assertEquals("a11yVolumeTrigger", requested.a11yVolumeTrigger, actual.a11yVolumeTrigger)
        assertEquals("translatorEngine", requested.translatorEngine, actual.translatorEngine)
        assertEquals("remotePcBaseUrl", requested.remotePcBaseUrl, actual.remotePcBaseUrl)
        assertEquals("remotePcApiKey", requested.remotePcApiKey, actual.remotePcApiKey)
        assertEquals("floatingWindowWidthDp", requested.floatingWindowWidthDp, actual.floatingWindowWidthDp)
        assertEquals("floatingWindowHeightDp", requested.floatingWindowHeightDp, actual.floatingWindowHeightDp)
        assertEquals("pinnedLanguages", requested.pinnedLanguages, actual.pinnedLanguages)
        assertEquals("cleartextAllowedHosts", requested.cleartextAllowedHosts, actual.cleartextAllowedHosts)
        assertEquals("floatingMenuItemOrder size", requested.floatingMenuItemOrder.size, actual.floatingMenuItemOrder.size)
        assertEquals("arcMenuPageSize", requested.arcMenuPageSize, actual.arcMenuPageSize)
        assertEquals("dictionaryPrompt", requested.dictionaryPrompt, actual.dictionaryPrompt)
        assertEquals("translationPresets size", requested.translationPresets.size, actual.translationPresets.size)
        assertEquals("activeTranslationPresetId", requested.activeTranslationPresetId, actual.activeTranslationPresetId)
    }

    private fun fileBackedRepository(root: File): SettingsRepository =
        SettingsRepository(FileBackedContext(root), PlainTestCipher).apply {
            setDefaultPromptProvidersForTest(
                prompt = { "default prompt" },
                dictionaryPrompt = { "default dictionary prompt" },
            )
        }

    private class FileBackedContext(private val root: File) : ContextWrapper(null) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = root
        override fun getPackageName(): String = "com.gameocr.app.repositorytest"
    }

    private object PlainTestCipher : SettingsSecretCipher {
        override fun encrypt(plainText: String): String = "test:$plainText"
        override fun decrypt(cipherText: String): String = cipherText.removePrefix("test:")
    }
}
