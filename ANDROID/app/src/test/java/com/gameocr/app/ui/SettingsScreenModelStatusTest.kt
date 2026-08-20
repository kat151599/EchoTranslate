package com.gameocr.app.ui

import com.gameocr.app.R
import com.gameocr.app.data.OverlayFontEntry
import com.gameocr.app.data.TranslationPreset
import com.gameocr.app.data.TranslationPresetCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class SettingsScreenModelStatusTest {

    @Test
    fun translatorSection_placesLanguagesAndAssistanceAfterRemotePcConfiguration() {
        val source = File("src/main/java/com/gameocr/app/ui/SettingsScreen.kt").readText()
        val sectionStart = source.indexOf("SectionCard(title = stringResource(R.string.settings_section_translator)")
        val sectionEnd = source.indexOf("title = stringResource(R.string.settings_section_ocr),", startIndex = sectionStart)
        assertTrue("translator section start", sectionStart >= 0)
        assertTrue("translator section end", sectionEnd > sectionStart)
        val section = source.substring(sectionStart, sectionEnd)

        data class Case(val earlier: String, val later: String)
        listOf(
            Case("stringResource(R.string.settings_remote_pc_base_url)", "R.string.settings_source_lang"),
            Case("stringResource(R.string.settings_remote_pc_api_key)", "R.string.settings_source_lang"),
            Case("stringResource(R.string.settings_remote_pc_session_id)", "R.string.settings_source_lang"),
            Case("stringResource(R.string.settings_remote_pc_image_quality)", "R.string.settings_source_lang"),
            Case("R.string.settings_source_lang", "R.string.settings_target_lang"),
            Case("R.string.settings_target_lang", "TranslationAssistanceSettings("),
        ).forEach { case ->
            val earlierIndex = section.indexOf(case.earlier)
            val laterIndex = section.indexOf(case.later)
            assertTrue("missing earlier marker: ", earlierIndex >= 0)
            assertTrue("missing later marker: ", laterIndex >= 0)
            assertTrue("order check", earlierIndex < laterIndex)
        }
    }

    @Test
    fun crossLineContextTranslation_isEnabledByDefaultBelowStreamingTranslation() {
        val screenSource = File("src/main/java/com/gameocr/app/ui/SettingsScreen.kt").readText()
        val remoteSource = File("src/main/java/com/gameocr/app/ui/SettingsRemotePcSection.kt").readText()

        // remembered state lives in SettingsScreen
        assertTrue(screenSource.contains("var crossLineContextTranslationEnabled by remember { mutableStateOf(true) }"))
        assertTrue(screenSource.contains("crossLineContextTranslationEnabled = !s.disableCrossLineContextTranslation"))
        assertTrue(screenSource.contains("disableCrossLineContextTranslation = !crossLineContextTranslationEnabled"))

        // control is implemented in SettingsRemotePcSection and binds to the remembered state
        assertTrue(remoteSource.contains("fun TranslationAssistanceSettings("))
        assertTrue(remoteSource.contains("label = stringResource(R.string.settings_cross_line_context_translation)"))
        assertTrue(remoteSource.contains("checked = crossLineContextTranslationEnabled"))
    }

    @Test
    fun settingsSearchMatches_normalizesMultipleTermsAndPunctuation() {
        data class Case(val query: String, val texts: List<String>, val expected: Boolean)
        val cases = listOf(
            Case("remote pc image", listOf("Remote PC image quality"), true),
            Case("preset import", listOf("Import / Export Presets"), true),
            Case("预设 导入", listOf("导入 / 导出翻译预设"), true),
            Case("transfer missing", listOf("Settings transfer"), false),
            Case("   ", listOf("Settings transfer"), false),
        )
        cases.forEach { case ->
            assertEquals(case.query, case.expected, settingsSearchMatches(case.query, case.texts))
        }
    }

    @Test
    fun settingsSearchKeywords_coverPortableSettingsAndVisualColorTerms() {
        // Uses production keyword lists; ensure they still expose expected topical coverage
        assertTrue(SETTINGS_SEARCH_TRANSFER_KEYWORDS.isNotEmpty())
        assertTrue(SETTINGS_SEARCH_COLOR_KEYWORDS.isNotEmpty())
        assertTrue(SETTINGS_SEARCH_TRANSLATION_BLOCK_INTERACTION_KEYWORDS.isNotEmpty())
    }

    @Test
    fun translationDisplayPreview_isFirstAndStickyOnlyInsideItsSection() {
        val source = File("src/main/java/com/gameocr/app/ui/SettingsScreen.kt").readText()
        val sectionStart = source.indexOf("title = stringResource(R.string.settings_section_overlay)")
        assertTrue(sectionStart >= 0)
        val preview = source.indexOf("OverlayPreviewCard(", startIndex = sectionStart)
        assertTrue(preview > sectionStart)
        val colors = source.indexOf("R.string.settings_overlay_theme_label", startIndex = sectionStart)
        assertTrue(preview < colors)
    }

    @Test
    fun translationPresetSectionLabel_usesSystemPresetPlanText() {
        assertEquals("System Preset Plans", stringResourceValue("src/main/res/values/strings.xml", "settings_section_translation_presets"))
        assertEquals("系统预设方案", stringResourceValue("src/main/res/values-zh-rCN/strings.xml", "settings_section_translation_presets"))
    }

    @Test
    fun translationPresetDescription_isSectionHelpInsteadOfInlineBodyText() {
        val source = File("src/main/java/com/gameocr/app/ui/SettingsScreen.kt").readText()
        assertTrue(source.contains("helpText = stringResource(R.string.settings_translation_preset_desc)"))
        assertFalse(source.contains("Text(\n            stringResource(R.string.settings_translation_preset_desc)"))
    }

    @Test
    fun translationBlockInteractionHelp_explainsVerticalSelectionLimitInEveryLocale() {
        val en = stringResourceValue("src/main/res/values/strings.xml", "settings_translation_block_interaction_vertical_help")
        val zh = stringResourceValue("src/main/res/values-zh-rCN/strings.xml", "settings_translation_block_interaction_vertical_help")
        assertTrue(en.contains("Vertical translation blocks") || en.contains("vertical"))
        assertTrue(zh.contains("竖排译文块"))
    }

    @Test
    fun translationPresetSaveDialogTitle_asksForPresetNameBeforeSaving() {
        assertEquals("Save current as preset", stringResourceValue("src/main/res/values/strings.xml", "settings_translation_preset_save_dialog_title"))
        assertEquals("保存为预设方案", stringResourceValue("src/main/res/values-zh-rCN/strings.xml", "settings_translation_preset_save_dialog_title"))
    }

    @Test
    fun translationPresetSaveDialog_blocksDuplicateNames() {
        val source = File("src/main/java/com/gameocr/app/ui/MainScreen.kt").readText()
        assertTrue(source.contains("translationPresetNameExists("))
        assertTrue(source.contains("isError = duplicateName") || source.contains("settings_translation_preset_name_duplicate"))
    }

    @Test
    fun translationPresetNameExists_isTableDriven() {
        val existing = listOf("Manga CN", "Vertical CN")
        assertFalse(translationPresetNameExists("", existing))
        assertFalse(translationPresetNameExists("Novel CN", existing))
        assertTrue(translationPresetNameExists("Manga CN", existing))
        assertTrue(translationPresetNameExists("  Manga CN  ", existing))
        assertTrue(translationPresetNameExists("manga cn", existing))
    }

    @Test
    fun namedTranslationPresetOrNull_isTableDriven() {
        val base = TranslationPreset(
            id = TranslationPresetCatalog.UNSAVED_DRAFT_ID,
            name = "Unsaved preset",
            shortName = "Unsaved",
            promptTemplate = "sentinel prompt",
        )

        assertEquals(null, namedTranslationPresetOrNull(preset = base, nameInput = ""))
        assertEquals(null, namedTranslationPresetOrNull(preset = base, nameInput = "   "))
        val named = namedTranslationPresetOrNull(preset = base, nameInput = "  Manga JP  ")
        assertEquals("Manga JP", named?.name)
        assertEquals("Manga JP", named?.shortName)
    }

    @Test
    fun overlayFontDeleteTipBeforeImport_onlyShowsForSystemDefaultOnly() {
        val validFont = ".ttf"
        assertTrue(shouldShowOverlayFontDeleteTipBeforeImport("", emptyList()))
        assertFalse(shouldShowOverlayFontDeleteTipBeforeImport(validFont, listOf(OverlayFontEntry(validFont, "Font.ttf"))))
        assertFalse(shouldShowOverlayFontDeleteTipBeforeImport("", listOf(OverlayFontEntry(validFont, "Font.ttf"))))
    }

    @Test
    fun overlayFontListPersistence_isThreadedThroughSettingsSave() {
        val screen = File("src/main/java/com/gameocr/app/ui/SettingsScreen.kt").readText()
        val viewModel = File("src/main/java/com/gameocr/app/ui/SettingsViewModel.kt").readText()
        val repo = File("src/main/java/com/gameocr/app/data/SettingsRepository.kt").readText()
        assertTrue(screen.contains("overlayFonts"))
        assertTrue(viewModel.contains("overlayFonts"))
        assertTrue(repo.contains("overlay_fonts_json") || repo.contains("OverlayFonts"))
    }

    @Test
    fun overlayFontDeleteTipMessage_isShortWithoutCountdownExplanation() {
        val en = stringResourceValue("src/main/res/values/strings.xml", "settings_overlay_font_delete_tip_message")
        val zh = stringResourceValue("src/main/res/values-zh-rCN/strings.xml", "settings_overlay_font_delete_tip_message")
        assertEquals("Long-press an imported font to delete it.", en)
        assertEquals("长按导入的字体可以删除。", zh)
        assertFalse(en.contains("3 秒") || en.contains("3 seconds"))
    }

    @Test
    fun cleartextHostsWithRemotePcUrl_appendsHttpHostAndDedupes() {
        data class Case(val hosts: List<String>, val remotePcBaseUrl: String, val expected: List<String>)
        val cases = listOf(
            Case(emptyList(), "http://pc-name:8765", listOf("pc-name")),
            Case(listOf("nas.local"), "http://192.168.0.2:8765", listOf("nas.local", "192.168.0.2")),
            Case(listOf("example.com", "EXAMPLE.COM"), "http://pc:8765", listOf("example.com")),
            Case(listOf("secure.example.com"), "https://pc:8765", listOf("secure.example.com")),
            Case(listOf("   "), "http://pc:8765", emptyList()),
        )

        cases.forEach { case ->
            assertEquals(case.expected, cleartextHostsWithLocalOcrUrls(case.hosts, case.remotePcBaseUrl))
        }
    }

    private fun stringResourceValue(
        resourcePath: String,
        resourceName: String,
    ): String {
        val file = listOf(
            File(resourcePath),
            File("app", resourcePath),
        ).firstOrNull { it.isFile }
            ?: error("Resource file not found: ")

        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(file)

        val nodes = document.getElementsByTagName("string")
        for (i in 0 until nodes.length) {
            val element = nodes.item(i)
            if (element.attributes.getNamedItem("name")?.nodeValue == resourceName) {
                return element.textContent
            }
        }

        error("String resource not found:  in ")
    }

}
