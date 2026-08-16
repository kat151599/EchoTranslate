package com.gameocr.app.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsFieldPolicyTest {

    @Test
    fun portableEncoding_isAnAllowlistAndNeverIncludesProtectedFields() {
        val s = Settings().copy(
            promptTemplate = "portable prompt",
            pinnedLanguages = listOf("ja", "zh-TW"),
        )

        val encoded = SettingsFieldPolicy.encodePortable(s)

        assertTrue("portable prompt", "promptTemplate" in encoded)
        assertTrue("portable pinned languages", "pinnedLanguages" in encoded)
        // ensure non-portable/protected fields are not included
        SettingsFieldPolicy.protectedFieldNames.forEach { field ->
            assertFalse("protected export field: $field", field in encoded)
        }
    }

    @Test
    fun applyPortable_preservesLocalFieldsAndAppliesPortableFields() {
        val current = Settings().copy(
            cleartextAllowedHosts = listOf("local.example"),
            floatingWindowX = 99,
            targetLang = "en",
        )
        val imported = Settings().copy(
            cleartextAllowedHosts = listOf("must-not-import.example"),
            floatingWindowX = 500,
            targetLang = "zh-TW",
        )

        val merged = SettingsFieldPolicy.applyPortable(current, imported)

        // protected/device-local fields must be preserved from current
        assertEquals(current.cleartextAllowedHosts, merged.cleartextAllowedHosts)
        assertEquals(current.floatingWindowX, merged.floatingWindowX)
        // portable field should be taken from imported
        assertEquals(imported.targetLang, merged.targetLang)
    }

    @Test
    fun decodePortable_skipsOneFutureEnumWithoutRejectingThePackage() {
        val values = SettingsFieldPolicy.encodePortable(
            Settings(targetLang = "zh-TW", translatorEngine = TranslatorEngine.DEEPL)
        ).toMutableMap()
        values["translatorEngine"] = JsonPrimitive("FUTURE_ENGINE")

        val decoded = SettingsFieldPolicy.decodePortable(JsonObject(values))

        assertEquals(listOf("translatorEngine"), decoded.skippedFields)
        assertEquals(Settings().translatorEngine, decoded.settings.translatorEngine)
        assertEquals("zh-TW", decoded.settings.targetLang)
    }

}
