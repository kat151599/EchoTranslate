package com.gameocr.app.ui

import android.content.Context
import android.net.Uri
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.gameocr.app.R
import com.gameocr.app.data.Languages
import com.gameocr.app.data.TranslationPreset
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

@Composable
internal fun OpenAiPromptSettings(
    prompt: String,
    onPromptChange: (String) -> Unit,
    sourceLang: String,
    targetLang: String,
    dictionaryPrompt: String,
    onDictionaryPromptChange: (String) -> Unit,
) {
    val context = LocalContext.current
    var promptAdvancedExpanded by remember { mutableStateOf(false) }
    var showResetMainPromptDialog by remember { mutableStateOf(false) }
    var showResetDictPromptDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button) {
                promptAdvancedExpanded = !promptAdvancedExpanded
            }
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.settings_prompt_advanced_header),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Icon(
            imageVector = Icons.Default.ExpandMore,
            contentDescription = null,
            modifier = Modifier.graphicsLayer {
                rotationZ = if (promptAdvancedExpanded) 180f else 0f
            },
            tint = MaterialTheme.colorScheme.primary,
        )
    }
    if (!promptAdvancedExpanded) return

    OutlinedTextField(
        value = prompt,
        onValueChange = onPromptChange,
        label = { Text(stringResource(R.string.settings_prompt_label)) },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3,
        maxLines = 6,
    )

    val hasTargetPlaceholder = prompt.contains("{target}") || prompt.contains("{target_lang}")
    val hasSourcePlaceholder = prompt.contains("{source}") || prompt.contains("{source_lang}")
    val targetName = Languages.nameOf(context, targetLang)
    val sourceName = Languages.nameOf(context, sourceLang)
    val autoName = Languages.nameOf(context, Languages.AUTO.code)
    val canFixTarget = !hasTargetPlaceholder && targetName.isNotBlank() && prompt.contains(targetName)
    val canFixSource = !hasSourcePlaceholder && sourceName.isNotBlank() &&
        sourceName != autoName && prompt.contains(sourceName)
    if (!hasTargetPlaceholder || !hasSourcePlaceholder) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val missingPart = buildString {
                    if (!hasTargetPlaceholder) append("{target}")
                    if (!hasTargetPlaceholder && !hasSourcePlaceholder) append(" / ")
                    if (!hasSourcePlaceholder) append("{source}")
                }
                Text(
                    stringResource(R.string.settings_prompt_warn_missing_format, missingPart),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    stringResource(R.string.settings_prompt_warn_hint_format, targetName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (canFixTarget) {
                    TextButton(onClick = { onPromptChange(prompt.replace(targetName, "{target}")) }) {
                        Text(stringResource(R.string.settings_prompt_replace_target_format, targetName))
                    }
                }
                if (canFixSource) {
                    TextButton(onClick = { onPromptChange(prompt.replace(sourceName, "{source}")) }) {
                        Text(stringResource(R.string.settings_prompt_replace_source_format, sourceName))
                    }
                }
            }
        }
    }

    val defaultPrompt = stringResource(R.string.default_prompt)
    TextButton(onClick = { showResetMainPromptDialog = true }) {
        Text(stringResource(R.string.settings_prompt_reset))
    }
    if (showResetMainPromptDialog) {
        CatalystAlertDialog(
            onDismissRequest = { showResetMainPromptDialog = false },
            title = { Text(stringResource(R.string.settings_prompt_reset_confirm_title)) },
            text = { Text(stringResource(R.string.settings_reset_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    onPromptChange(defaultPrompt)
                    showResetMainPromptDialog = false
                }) { Text(stringResource(R.string.settings_reset_confirm_yes)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetMainPromptDialog = false }) {
                    Text(stringResource(R.string.settings_reset_confirm_no))
                }
            },
        )
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    Text(
        stringResource(R.string.settings_dictionary_prompt_title),
        style = MaterialTheme.typography.labelLarge,
    )
    Text(
        stringResource(R.string.settings_dictionary_prompt_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedTextField(
        value = dictionaryPrompt,
        onValueChange = onDictionaryPromptChange,
        modifier = Modifier.fillMaxWidth(),
        minLines = 3,
        maxLines = 12,
    )
    val defaultDictionaryPrompt = stringResource(R.string.default_dictionary_prompt)
    TextButton(onClick = { showResetDictPromptDialog = true }) {
        Text(stringResource(R.string.settings_dictionary_prompt_reset))
    }
    if (showResetDictPromptDialog) {
        CatalystAlertDialog(
            onDismissRequest = { showResetDictPromptDialog = false },
            title = { Text(stringResource(R.string.settings_dictionary_prompt_reset_confirm_title)) },
            text = { Text(stringResource(R.string.settings_reset_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    onDictionaryPromptChange(defaultDictionaryPrompt)
                    showResetDictPromptDialog = false
                }) { Text(stringResource(R.string.settings_reset_confirm_yes)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDictPromptDialog = false }) {
                    Text(stringResource(R.string.settings_reset_confirm_no))
                }
            },
        )
    }
}
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

internal fun missingOpenAiFallbackFields(
    baseUrl: String,
    apiKey: String,
    model: String,
): List<OpenAiFallbackField> = buildList {
    if (baseUrl.isBlank()) add(OpenAiFallbackField.BASE_URL)
    if (apiKey.isBlank()) add(OpenAiFallbackField.API_KEY)
    if (model.isBlank()) add(OpenAiFallbackField.MODEL)
}
internal enum class OpenAiFallbackField {
    BASE_URL,
    API_KEY,
    MODEL,
}


internal fun translationPresetDisplayName(preset: TranslationPreset): String = preset.name
internal fun translationPresetSummary(preset: TranslationPreset): String = "${preset.name} · ${preset.sourceLang} → ${preset.targetLang}"
internal fun newCustomPresetId(): String = "custom_${System.currentTimeMillis()}"
