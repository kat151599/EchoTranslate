package com.gameocr.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

class CrashRecorderSettingsCoverageTest {

    @Test
    fun formatSettingsListsEverySettingsPropertyByName() {
        val formatted = CrashRecorder.formatSettings(Settings())
        val fields = Settings::class.java.declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) }
            .map { it.name }
            .sorted()

        fields.forEach { field ->
            assertTrue("missing crash setting field: $field", formatted.contains("  $field:"))
        }
    }

    @Test
    fun formatSettingsMasksSecretsAndTruncatesLongPrompts_forCurrentSensitiveFieldsOnly() {
        val formatted = CrashRecorder.formatSettings(
            Settings().copy(
                cleartextAllowedHosts = listOf("192.168.1.20"),
                promptTemplate = "first line\nsecond line that is intentionally longer than sixty characters for truncation",
                dictionaryPrompt = "dictionary line\nsecond line that is intentionally longer than sixty characters for truncation",
                baiduFanyiAppId = "baidu-id",
                baiduFanyiSecretKey = "baidu-secret",
                volcAccessKeyId = "volc-id",
                volcSecretAccessKey = "volc-secret",
            )
        )

        // ensure private values are not leaked
        listOf(
            "192.168.1.20",
            "baidu-id",
            "baidu-secret",
            "volc-id",
            "volc-secret",
        ).forEach { secret ->
            assertFalse("secret leaked: $secret", formatted.contains(secret))
        }

        // masked and truncated markers for current sensitive fields
        assertTrue(formatted.contains("  baiduFanyiAppId: <set>"))
        assertTrue(formatted.contains("  baiduFanyiSecretKey: <set>"))
        assertTrue(formatted.contains("  volcAccessKeyId: <set>"))
        assertTrue(formatted.contains("  volcSecretAccessKey: <set>"))
        assertTrue(formatted.contains("  promptTemplate: <configured;"))
        assertTrue(formatted.contains("  dictionaryPrompt: <configured;"))
        assertFalse(formatted.contains("first line"))
        assertFalse(formatted.contains("dictionary line"))
    }
}
