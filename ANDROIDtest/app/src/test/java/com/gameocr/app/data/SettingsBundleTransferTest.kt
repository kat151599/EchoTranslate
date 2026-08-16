package com.gameocr.app.data

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsBundleTransferTest {

    @Test
    fun portableSettings_excludesCredentialsPrivateConnectionsAndDeviceState() {
        val original = sampleSettings()
        val portable = SettingsBundleTransfer.portableSettings(original)

        data class SecretCase(val name: String, val value: (Settings) -> String)
        val secretCases = listOf(
            SecretCase("OpenAI API key", Settings::apiKey),
            SecretCase("DeepL API key", Settings::deeplApiKey),
            SecretCase("Youdao app key", Settings::youdaoAppKey),
        )
        secretCases.forEach { case ->
            assertTrue(case.name, case.value(original).isNotBlank())
            assertEquals(case.name, "", case.value(portable))
        }

        data class PortableCase(val name: String, val expected: Any?, val actual: Any?)
        val portableCases = listOf(
            PortableCase("prompt", original.promptTemplate, portable.promptTemplate),
            PortableCase("loop interval", original.captureLoopIntervalMs, portable.captureLoopIntervalMs),
            PortableCase("overlay style", original.overlayTextStyle, portable.overlayTextStyle),
            PortableCase("pinned languages", original.pinnedLanguages, portable.pinnedLanguages),
            PortableCase("font list", original.overlayFonts, portable.overlayFonts),
            PortableCase("preset count", original.translationPresets.size, portable.translationPresets.size),
            PortableCase("active preset", original.activeTranslationPresetId, portable.activeTranslationPresetId),
        )
        portableCases.forEach { case -> assertEquals(case.name, case.expected, case.actual) }

        assertEquals("private base URL", Settings().baseUrl, portable.baseUrl)
        assertEquals("private cleartext hosts", emptyList<String>(), portable.cleartextAllowedHosts)
        assertEquals("device floating geometry", Settings().floatingWindowWidthDp, portable.floatingWindowWidthDp)
    }

    @Test
    fun settingsBundle_roundTripsSettingsPresetsAndFontBytes() {
        val original = sampleSettings()
        val fontBytes = "portable-font-fixture".toByteArray()
        val fontFile = File.createTempFile("settings-bundle-font", ".ttf").apply {
            writeBytes(fontBytes)
            deleteOnExit()
        }
        val fontName = storedName(fontBytes)
        val settings = original.copy(
            overlayFontFileName = fontName,
            overlayFontDisplayName = "Portable.ttf",
            overlayFonts = listOf(OverlayFontEntry(fontName, "Portable.ttf")),
        )
        val output = ByteArrayOutputStream()

        val exported = SettingsBundleTransfer.write(output, settings) { requested ->
            fontFile.takeIf { requested == fontName }
        }
        val importedFontBytes = mutableMapOf<String, ByteArray>()
        val decoded = SettingsBundleTransfer.read(ByteArrayInputStream(output.toByteArray())) { font, input ->
            importedFontBytes[font.fileName] = input.readBytes()
        }

        assertEquals(1, exported.presetCount)
        assertEquals(1, exported.fontCount)
        assertFalse(decoded.legacyPresetOnly)
        assertEquals(SettingsBundleTransfer.portableSettings(settings), decoded.settings)
        assertTrue(fontBytes.contentEquals(importedFontBytes.getValue(fontName)))
    }

    @Test
    fun v2Manifest_containsOnlyAllowlistedPortableValues() {
        val settings = sampleSettings().copy(
            overlayFontFileName = "",
            overlayFontDisplayName = "",
            overlayFonts = emptyList(),
        )
        val output = ByteArrayOutputStream()

        SettingsBundleTransfer.write(output, settings, resolveFontFile = { null })
        val manifest = ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
            assertEquals("manifest.json", zip.nextEntry.name)
            zip.readBytes().toString(Charsets.UTF_8)
        }

        assertTrue(manifest.contains("\"version\":2"))
        assertTrue(manifest.contains("\"values\""))
        listOf("openai-secret", "deepl-key").forEach { protectedValue ->
            assertFalse("protected value leaked: $protectedValue", manifest.contains(protectedValue))
        }
    }

    @Test
    fun mergeImportedSettings_keepsLocalCredentialsAndAppliesPortableSettings() {
        val current = Settings(
            baseUrl = "https://local-device.example/v1/",
            apiKey = "local-openai-key",
            umiOcrBaseUrl = "http://192.168.1.10:1224/api/ocr",
            cleartextAllowedHosts = listOf("192.168.1.10"),
            floatingWindowWidthDp = 777,
            overlayFonts = listOf(OverlayFontEntry(storedName("old".toByteArray()), "Old.ttf")),
        )
        val imported = sampleSettings()
        val result = SettingsBundleTransfer.mergeImportedSettings(
            current = current,
            imported = imported,
            availableFonts = imported.overlayFonts + current.overlayFonts,
        )

        assertEquals("local-openai-key", result.settings.apiKey)
        assertEquals(current.baseUrl, result.settings.baseUrl)
        assertEquals(current.umiOcrBaseUrl, result.settings.umiOcrBaseUrl)
        assertEquals(current.cleartextAllowedHosts, result.settings.cleartextAllowedHosts)
        assertEquals(current.floatingWindowWidthDp, result.settings.floatingWindowWidthDp)
        assertEquals(imported.captureLoopIntervalMs, result.settings.captureLoopIntervalMs)
        assertEquals(imported.loopTriggerMode, result.settings.loopTriggerMode)
        assertEquals(imported.loopTextStableDurationMs, result.settings.loopTextStableDurationMs)
        assertEquals(imported.loopSkipSimilarFrames, result.settings.loopSkipSimilarFrames)
        assertEquals(imported.loopFrameSimilarityThreshold, result.settings.loopFrameSimilarityThreshold)
        assertEquals(imported.loopTextRegionMode, result.settings.loopTextRegionMode)
        assertEquals(imported.loopTranslateRegionOnly, result.settings.loopTranslateRegionOnly)
        assertEquals(imported.retryEmptyTranslation, result.settings.retryEmptyTranslation)
        assertEquals(imported.developerOptionsEnabled, result.settings.developerOptionsEnabled)
        assertEquals(imported.ocrRedBoxModeEnabled, result.settings.ocrRedBoxModeEnabled)
        assertEquals(imported.ocrRedBoxShowSourceText, result.settings.ocrRedBoxShowSourceText)
        assertEquals(imported.ocrRedBoxShowTranslation, result.settings.ocrRedBoxShowTranslation)
        assertEquals(imported.floatingMenuItemOrder, result.settings.floatingMenuItemOrder)
        assertEquals(imported.overlayFontFileName, result.settings.overlayFontFileName)
        assertTrue(result.settings.overlayFonts.containsAll(current.overlayFonts))
        assertEquals(1, result.presetResult.importedCount)
    }

    @Test
    fun settingsBundleReader_acceptsLegacyPresetExports() {
        val legacyPreset = sampleSettings().translationPresets.single()
        val encoded = TranslationPresetTransfer.encodeEncrypted(listOf(legacyPreset))

        val preview = SettingsBundleTransfer.readPreview(
            ByteArrayInputStream(encoded.toByteArray(Charsets.UTF_8)),
        )

        assertTrue(preview.legacyPresetOnly)
        assertEquals(null, preview.settings)
        assertEquals(listOf(legacyPreset), preview.presets)
        assertTrue(preview.fonts.isEmpty())
    }

    @Test
    fun settingsBundleReader_migratesLegacyV1SettingsAndProtectsLocalConnections() {
        val legacySettings = sampleSettings().copy(
            overlayFontFileName = "",
            overlayFontDisplayName = "",
            overlayFonts = emptyList(),
            baseUrl = "https://legacy-private.example/v1/",
            cleartextAllowedHosts = listOf("legacy-private.example"),
            targetLang = "zh-TW",
        )
        val manifest = """{"format":"overlay-translator.settings","version":1,"settings":${Json.encodeToString(legacySettings)},"fonts":[]}"""
        val bytes = ByteArrayOutputStream().also { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(manifest.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }.toByteArray()

        val preview = SettingsBundleTransfer.readPreview(ByteArrayInputStream(bytes))

        assertEquals(1, preview.formatVersion)
        assertEquals(Settings().baseUrl, preview.settings?.baseUrl)
        assertEquals(emptyList<String>(), preview.settings?.cleartextAllowedHosts)
        assertEquals("zh-TW", preview.settings?.targetLang)
        assertEquals(0, preview.settings?.bubbleClusterGap)
        assertEquals(0, preview.settings?.mangaOcrCropPaddingPx)
        assertTrue(preview.skippedSettingFields.isEmpty())
    }

    private fun sampleSettings(): Settings {
        val fontName = storedName("portable".toByteArray())
        val base = Settings(
            baseUrl = "https://portable.example/v1/",
            apiKey = "openai-secret",
            model = "portable-model",
            sourceLang = "ja",
            targetLang = "zh-TW",
            promptTemplate = "portable prompt",
            captureLoopIntervalMs = 3456L,
            loopTriggerMode = LoopTriggerMode.FIXED_INTERVAL,
            loopTextStableDurationMs = 1400L,
            loopSkipSimilarFrames = false,
            loopFrameSimilarityThreshold = 0.87f,
            loopTextRegionMode = LoopTextRegionMode.LOWER_SCREEN_FIRST,
            loopTranslateRegionOnly = false,
            retryEmptyTranslation = true,
            translationOutputFollowRecognition = false,
            translationOutputLayout = TranslationOutputLayout.VERTICAL,
            translationOutputDirection = TranslationOutputDirection.LEFT_TO_RIGHT,
            translationGlossaryEnabled = false,
            foregroundAppDetectionMode = ForegroundAppDetectionMode.USAGE_ACCESS,
            sendAppNameToTranslator = true,
            developerOptionsEnabled = true,
            batchCumulativeCompletionTimeEnabled = true,
            ocrRedBoxModeEnabled = true,
            ocrRedBoxShowSourceText = false,
            ocrRedBoxShowTranslation = true,
            overlayTextSizeSp = 21,
            overlayTextStyle = OverlayTextStyle(bold = true, italic = true, underline = true),
            overlayAlpha = 0.61f,
            overlayFontFileName = fontName,
            overlayFontDisplayName = "Portable.ttf",
            overlayFonts = listOf(OverlayFontEntry(fontName, "Portable.ttf")),
            baiduOcrApiKey = "baidu-key",
            baiduOcrSecretKey = "baidu-secret",
            paddleAiStudioToken = "paddle-token",
            tencentSecretId = "tencent-id",
            tencentSecretKey = "tencent-secret",
            deeplApiKey = "deepl-key",
            deeplCustomToken = "deepl-token",
            youdaoAppKey = "youdao-key",
            youdaoAppSecret = "youdao-secret",
            volcAccessKeyId = "volc-id",
            volcSecretAccessKey = "volc-secret",
            baiduFanyiAppId = "baidu-app-id",
            baiduFanyiSecretKey = "baidu-secret-key",
            floatingButtonSizeDp = 52,
            floatingWindowWidthDp = 444,
            floatingWindowHeightDp = 222,
            pinnedLanguages = listOf("ja", "zh-TW"),
            cleartextAllowedHosts = listOf("192.168.0.2"),
            floatingMenuItemOrder = FloatingMenu.DEFAULT_ORDER.reversed(),
            arcMenuPageSize = 5,
            floatingButtonSkill = FloatingSkill.LOOP,
            dictionaryPrompt = "portable dictionary prompt",
            localLlmContextSize = 3072,
            localLlmMaxNewTokens = 384,
            localLlmMirror = LlmMirrorChoice.CUSTOM,
            localLlmMirrorUrl = "https://portable.example/models/",
            dbnetProbThresh = 0.31f,
            bubbleClusterGap = 41,
            mangaOcrCropPaddingPx = 17,
        )
        val preset = TranslationPresetCatalog.fromSettings(
            id = "custom_portable",
            name = "Portable",
            shortName = "Port",
            settings = base,
        )
        return base.copy(
            translationPresets = listOf(preset),
            activeTranslationPresetId = preset.id,
        )
    }

    private fun storedName(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        val hex = digest.joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return OverlayFontPolicy.storedFileNameForSha256(hex)
    }
}
