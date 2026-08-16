package com.gameocr.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gameocr.app.R
import com.gameocr.app.data.TranslationPreset

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun TranslationPresetSection(
    customPresets: List<TranslationPreset>,
    activeId: String,
    unsavedPreset: TranslationPreset?,
    message: String?,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onSaveUnsaved: (TranslationPreset) -> Unit,
    onApply: (TranslationPreset) -> Unit,
    onDelete: (TranslationPreset) -> Unit,
) {
    message?.let { Text(it) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onImport) { Text(stringResource(R.string.settings_translation_preset_import)) }
        OutlinedButton(onClick = onExport) { Text(stringResource(R.string.settings_translation_preset_export)) }
    }
}
internal fun normalizedTranslationPresetName(input: String): String? =
    input.trim().takeIf { it.isNotEmpty() }

internal fun translationPresetNameExists(
    nameInput: String,
    existingNames: Iterable<String>,
): Boolean {
    val name = normalizedTranslationPresetName(nameInput) ?: return false
    return existingNames.any { existingName ->
        normalizedTranslationPresetName(existingName)?.equals(name, ignoreCase = true) == true
    }
}

internal fun translationPresetShortNameFor(name: String): String =
    name.take(8)

internal fun namedTranslationPresetOrNull(
    preset: TranslationPreset,
    nameInput: String,
    id: String = preset.id,
): TranslationPreset? {
    val name = normalizedTranslationPresetName(nameInput) ?: return null
    return preset.copy(
        id = id,
        name = name,
        shortName = translationPresetShortNameFor(name),
    )
}

internal fun translationPresetDisplayName(preset: TranslationPreset): String = preset.name
internal fun translationPresetSummary(preset: TranslationPreset): String = "${preset.name} В· ${preset.sourceLang} в†’ ${preset.targetLang}"
internal fun newCustomPresetId(): String = "custom_${System.currentTimeMillis()}"
