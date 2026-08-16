package com.gameocr.app.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gameocr.app.R
import com.gameocr.app.appcontext.isUsageAccessGranted
import com.gameocr.app.data.TranslatorEngine

@Composable
internal fun rememberUsageAccessGranted(context: Context): Boolean {
    var granted by remember(context) { mutableStateOf(isUsageAccessGranted(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = isUsageAccessGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return granted
}

internal fun usageAccessPackageUri(packageName: String): String = "package:$packageName"

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TranslationAssistanceSettings(
    searchTargetRegistry: SettingsSearchTargetRegistry,
    translatorEngine: TranslatorEngine,
    streaming: Boolean,
    onStreamingChange: (Boolean) -> Unit,
    crossLineContextTranslationEnabled: Boolean,
    onCrossLineContextTranslationEnabledChange: (Boolean) -> Unit,
    foregroundAppDetectionMode: com.gameocr.app.data.ForegroundAppDetectionMode,
    onForegroundAppDetectionModeChange: (com.gameocr.app.data.ForegroundAppDetectionMode) -> Unit,
    usageAccessGranted: Boolean,
    onOpenUsageAccess: () -> Unit,
    retryEmptyTranslation: Boolean,
    onRetryEmptyTranslationChange: (Boolean) -> Unit,
) {
    if (translatorEngine == TranslatorEngine.OPENAI ||
        translatorEngine == TranslatorEngine.ANTHROPIC
    ) {
        SettingsSearchTarget(searchTargetRegistry, R.string.settings_search_item_streaming) {
        SwitchRow(stringResource(R.string.settings_streaming), streaming, onChange = onStreamingChange)
        }
    }
    SettingsSearchTarget(searchTargetRegistry, R.string.settings_search_item_cross_line_context) {
    SwitchRow(
        label = stringResource(R.string.settings_cross_line_context_translation),
        checked = crossLineContextTranslationEnabled,
        helpText = stringResource(R.string.settings_cross_line_context_translation_hint),
        onChange = onCrossLineContextTranslationEnabledChange,
    )
    }
    SettingsSearchTarget(searchTargetRegistry, R.string.settings_search_item_empty_translation_retry) {
    SwitchRow(
        label = stringResource(R.string.settings_retry_empty_translation_label),
        checked = retryEmptyTranslation,
        helpText = stringResource(R.string.settings_retry_empty_translation_hint),
        onChange = onRetryEmptyTranslationChange,
    )
    }
        SettingsSearchTarget(searchTargetRegistry, R.string.settings_foreground_app_detection) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            stringResource(R.string.settings_foreground_app_detection),
            style = MaterialTheme.typography.labelLarge,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            com.gameocr.app.data.ForegroundAppDetectionMode.entries.forEach { mode ->
                val label = when (mode) {
                    com.gameocr.app.data.ForegroundAppDetectionMode.AUTO ->
                        stringResource(R.string.settings_foreground_app_auto)
                    com.gameocr.app.data.ForegroundAppDetectionMode.ACCESSIBILITY ->
                        stringResource(R.string.settings_foreground_app_accessibility)
                    com.gameocr.app.data.ForegroundAppDetectionMode.USAGE_ACCESS ->
                        stringResource(R.string.settings_foreground_app_usage_access)
                    com.gameocr.app.data.ForegroundAppDetectionMode.DISABLED ->
                        stringResource(R.string.settings_foreground_app_disabled)
                }
                EngineChip(foregroundAppDetectionMode, mode, label, onSelect = onForegroundAppDetectionModeChange)
            }
        }
        }
        }
        SettingsSearchTarget(searchTargetRegistry, R.string.settings_grant_usage_access) {
        SettingsLinkCell(
            label = stringResource(R.string.settings_grant_usage_access),
            status = stringResource(
                if (usageAccessGranted) R.string.settings_permission_granted
                else R.string.settings_permission_not_granted
            ),
            statusGranted = usageAccessGranted,
            onClick = onOpenUsageAccess,
        )
        }
    HorizontalDivider()
}
