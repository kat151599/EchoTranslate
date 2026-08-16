package com.gameocr.app.ui

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.gameocr.app.R
import com.gameocr.app.data.FloatingMenu
import com.gameocr.app.data.OverlayFontEntry
import com.gameocr.app.data.OverlayFontCommit
import com.gameocr.app.data.OverlayTextStyle
import com.gameocr.app.data.OverlayFontImportResult
import com.gameocr.app.data.OverlayFontManager
import com.gameocr.app.data.OverlayPlacement
import com.gameocr.app.data.OverlayTheme
import com.gameocr.app.data.RenderMode
import com.gameocr.app.data.Settings
import com.gameocr.app.data.SettingsBundleExportResult
import com.gameocr.app.data.SettingsBundleImportResult
import com.gameocr.app.data.SettingsBundlePreview
import com.gameocr.app.data.SettingsBundleTransfer
import com.gameocr.app.data.SettingsRepository
import com.gameocr.app.data.StagedOverlayFont
import com.gameocr.app.data.TranslationPreset
import com.gameocr.app.data.TranslationBlockInteractionMode
import com.gameocr.app.data.TranslationPresetCatalog
import com.gameocr.app.data.TranslationPresetImportResult
import com.gameocr.app.data.TranslationPresetTransfer
import com.gameocr.app.data.TranslatorEngine
import com.gameocr.app.translate.RoutingTranslator
import com.gameocr.app.translate.TestResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val repo: SettingsRepository,
    private val routingTranslator: RoutingTranslator,
    private val overlayFontManager: OverlayFontManager,
) : ViewModel() {

    suspend fun load(): Settings = repo.get()

    suspend fun exportSettingsBundle(
        uri: Uri,
        settings: Settings,
    ): SettingsBundleExportResult = withContext(Dispatchers.IO) {
        val output = appContext.contentResolver.openOutputStream(uri, "w")
            ?: error("Could not open the selected export file.")
        output.use {
            SettingsBundleTransfer.write(
                output = it,
                settings = settings,
                resolveFontFile = overlayFontManager::transferFileFor,
            )
        }
    }

    suspend fun previewSettingsBundle(uri: Uri): SettingsBundlePreview = withContext(Dispatchers.IO) {
        val input = appContext.contentResolver.openInputStream(uri)
            ?: error("Could not open the selected settings file.")
        input.use(SettingsBundleTransfer::readPreview)
    }

    suspend fun importSettingsBundle(uri: Uri): SettingsBundleImportResult = withContext(Dispatchers.IO) {
        val stagingDir = File(appContext.cacheDir, "settings-import-${System.nanoTime()}")
        require(stagingDir.mkdirs()) { "Could not create settings import staging storage." }
        val stagedFonts = mutableListOf<StagedOverlayFont>()
        val commits = mutableListOf<OverlayFontCommit>()
        try {
            val input = appContext.contentResolver.openInputStream(uri)
                ?: error("Could not open the selected settings file.")
            val preview = input.use { source ->
                SettingsBundleTransfer.read(source) { font, fontInput ->
                    stagedFonts += overlayFontManager.stageTransferredFont(font, fontInput, stagingDir)
                }
            }
            if (preview.legacyPresetOnly) {
                val imported = importTranslationPresets(preview.presets)
                return@withContext SettingsBundleImportResult(
                    settings = repo.get(),
                    importedPresetCount = imported.importedCount,
                    overwrittenPresetNames = imported.overwrittenNames,
                    importedFontCount = 0,
                    legacyPresetOnly = true,
                    skippedSettingFieldCount = 0,
                )
            }

            val importedSettings = requireNotNull(preview.settings)
            val beforeSettings = repo.get()
            var settingsCommitted = false
            try {
                stagedFonts.forEach { commits += overlayFontManager.commitTransferredFont(it) }
                val installedFonts = commits.map(OverlayFontCommit::entry)
                val availableFonts = overlayFontManager.existingFontEntries(
                    beforeSettings.overlayFonts + importedSettings.overlayFonts + installedFonts,
                )
                val merged = SettingsBundleTransfer.mergeImportedSettings(
                    current = beforeSettings,
                    imported = importedSettings,
                    availableFonts = availableFonts,
                )
                repo.update { merged.settings }
                settingsCommitted = true
                commits.forEach(overlayFontManager::finishTransferredFont)
                SettingsBundleImportResult(
                    settings = merged.settings,
                    importedPresetCount = merged.presetResult.importedCount,
                    overwrittenPresetNames = merged.presetResult.overwrittenNames,
                    importedFontCount = installedFonts.size,
                    legacyPresetOnly = false,
                    skippedSettingFieldCount = preview.skippedSettingFields.size,
                )
            } catch (error: Throwable) {
                if (settingsCommitted) {
                    runCatching { repo.update { beforeSettings } }.exceptionOrNull()?.let(error::addSuppressed)
                }
                commits.asReversed().forEach { commit ->
                    runCatching { overlayFontManager.rollbackTransferredFont(commit) }
                        .exceptionOrNull()?.let(error::addSuppressed)
                }
                throw error
            }
        } finally {
            stagingDir.deleteRecursively()
        }
    }

    suspend fun importOverlayFont(uri: Uri): OverlayFontImportResult =
        overlayFontManager.importFont(uri)

    suspend fun resetOverlayFont() {
        overlayFontManager.resetFont()
    }

    suspend fun selectOverlayFont(fileName: String, displayName: String): Boolean =
        overlayFontManager.selectFont(fileName, displayName)

    suspend fun deleteOverlayFont(fileName: String): Boolean =
        overlayFontManager.deleteFont(fileName)

    fun overlayTypefaceFor(fileName: String): Typeface? =
        overlayFontManager.typefaceFor(fileName)

    @Suppress("LongParameterList")
    suspend fun save(
        targetLang: String,
        sourceLang: String,
        prompt: String,
        textSize: Int,
        overlayTextStyle: OverlayTextStyle,
        alpha: Float,
        loopMs: Long,
        loopTriggerMode: com.gameocr.app.data.LoopTriggerMode,
        loopTextStableDurationMs: Long,
        loopSkipSimilarFrames: Boolean,
        loopFrameSimilarityThreshold: Float,
        loopTextRegionMode: com.gameocr.app.data.LoopTextRegionMode,
        loopTranslateRegionOnly: Boolean,
        developerOptionsEnabled: Boolean,
        disableTranslationCache: Boolean,
        batchCumulativeCompletionTimeEnabled: Boolean,
        streaming: Boolean,
        retryEmptyTranslation: Boolean,
        renderMode: RenderMode,
        translationBlockInteractionMode: TranslationBlockInteractionMode,
        placement: OverlayPlacement,
        overlayStyleMode: com.gameocr.app.data.OverlayStyleMode,
        overlayTheme: OverlayTheme,
        customBg: Int,
        customFg: Int,
        customBorder: Int,
        customBorderW: Int,
        offsetX: Int,
        offsetY: Int,
        a11yVolume: Boolean,
        floatingButtonSizeDp: Int,
        floatingButtonSnapToEdge: Boolean,
        floatingButtonAutoDock: Boolean,
        floatingButtonDockInsetDp: Int,
        allowWrap: Boolean,
        avoidCollision: Boolean,
        apiTimeoutSeconds: Int,
        mergeAdjacentBlocks: Boolean,
        mergeStrength: com.gameocr.app.data.MergeStrength,
        disableCrossLineContextTranslation: Boolean,
        cleartextAllowedHosts: List<String>,
        translatorEngine: TranslatorEngine,
        remotePcBaseUrl: String,
        remotePcApiKey: String,
        remotePcSessionId: String,
        remotePcImageQuality: Int,
        volcAccessKeyId: String,
        volcSecretAccessKey: String,
        volcRegion: String,
        baiduFanyiAppId: String,
        baiduFanyiSecretKey: String,
        overlayFonts: List<OverlayFontEntry>,
        activeTranslationPresetId: String
    ) {
        repo.update {
            it.copy(
                targetLang = targetLang.trim(),
                sourceLang = sourceLang.trim(),
                promptTemplate = prompt,
                overlayTextSizeSp = textSize.coerceIn(10, 28),
                overlayTextStyle = overlayTextStyle.normalized(),
                overlayAlpha = alpha.coerceIn(0.3f, 1f),
                captureLoopIntervalMs = loopMs.coerceAtLeast(200),
                loopTriggerMode = loopTriggerMode,
                loopTextStableDurationMs = loopTextStableDurationMs.coerceIn(200L, 2000L),
                loopSkipSimilarFrames = loopSkipSimilarFrames,
                loopFrameSimilarityThreshold = loopFrameSimilarityThreshold.coerceIn(0.50f, 0.99f),
                loopTextRegionMode = loopTextRegionMode,
                loopTranslateRegionOnly = loopTranslateRegionOnly,
                developerOptionsEnabled = developerOptionsEnabled,
                disableTranslationCache = disableTranslationCache,
                batchCumulativeCompletionTimeEnabled = batchCumulativeCompletionTimeEnabled,
                streamingTranslate = streaming,
                retryEmptyTranslation = retryEmptyTranslation,
                renderMode = renderMode,
                translationBlockInteractionMode = translationBlockInteractionMode,
                overlayPlacement = placement,
                overlayStyleMode = overlayStyleMode,
                overlayTheme = overlayTheme,
                customBgColor = customBg,
                customFgColor = customFg,
                customBorderColor = customBorder,
                customBorderWidth = customBorderW,
                overlayOffsetX = offsetX,
                overlayOffsetY = offsetY,
                a11yVolumeTrigger = a11yVolume,
                floatingButtonSizeDp = floatingButtonSizeDp.coerceIn(32, 96),
                floatingButtonSnapToEdge = floatingButtonSnapToEdge,
                floatingButtonAutoDock = floatingButtonAutoDock,
                floatingButtonDockInsetDp = floatingButtonDockInsetDp.coerceIn(0, 40),
                overlayAllowWrap = allowWrap,
                overlayAvoidCollision = avoidCollision,
                apiTimeoutSeconds = apiTimeoutSeconds.coerceIn(5, 300),
                mergeAdjacentBlocks = mergeAdjacentBlocks,
                mergeStrength = mergeStrength,
                disableCrossLineContextTranslation = disableCrossLineContextTranslation,
                cleartextAllowedHosts = cleartextAllowedHosts,
                translatorEngine = translatorEngine,
                remotePcBaseUrl = remotePcBaseUrl.trim().trimEnd('/'),
                remotePcApiKey = remotePcApiKey.trim(),
                remotePcSessionId = remotePcSessionId.trim().ifBlank { "default" },
                remotePcImageQuality = remotePcImageQuality.coerceIn(50, 100),
                volcAccessKeyId = volcAccessKeyId.trim(),
                volcSecretAccessKey = volcSecretAccessKey.trim(),
                volcRegion = volcRegion.trim().ifBlank { "cn-north-1" },
                baiduFanyiAppId = baiduFanyiAppId.trim(),
                baiduFanyiSecretKey = baiduFanyiSecretKey.trim(),
                overlayFonts = overlayFonts,
                activeTranslationPresetId = activeTranslationPresetId
            )
        }
    }

    suspend fun setActiveTranslationPreset(id: String) {
        repo.update { it.copy(activeTranslationPresetId = id) }
    }

    /**
     * 单独保存「悬浮球吸附边缘」开关。切换时立即落盘 + 立即触发 CaptureService 响应，
     * 不走 [save] 的 dirty/save 流程——用户切了开关期望立即生效，不需要再点保存。
     */
    suspend fun saveFloatingSnapEdge(enabled: Boolean) {
        repo.update { it.copy(floatingButtonSnapToEdge = enabled) }
    }

    /** 悬浮窗口内容形态（原文+译文 / 仅译文）。立即落盘 + 即时生效，不走 [save] 流程。 */
    suspend fun saveFloatingWindowContentMode(mode: com.gameocr.app.data.FloatingWindowContentMode) {
        repo.update { it.copy(floatingWindowContentMode = mode) }
    }

    /** 悬浮窗口锁定开关：锁定后禁用拖拽 / resize。 */
    suspend fun saveFloatingWindowLocked(locked: Boolean) {
        repo.update { it.copy(floatingWindowLocked = locked) }
    }

    /** CUSTOM 主题的边框样式（SOLID / DASHED / DOTTED / DOUBLE / GROOVE），立即生效。 */
    suspend fun saveCustomBorderStyle(style: com.gameocr.app.data.BorderStyle) {
        repo.update { it.copy(customBorderStyle = style) }
    }

    /** 弧菜单按钮顺序：拖拽完即时落盘 + 生效，不走主 [save] 流程的 dirty 判定。 */
    suspend fun saveArcMenuOrder(order: List<com.gameocr.app.data.MenuItemId>) {
        repo.update { it.copy(floatingMenuItemOrder = order) }
    }

    suspend fun saveArcMenuPageSize(size: Int) {
        repo.update { it.copy(arcMenuPageSize = FloatingMenu.coercePageSize(size)) }
    }

    suspend fun createTranslationPresetFromCurrent(
        name: String,
        shortName: String
    ): TranslationPreset {
        var saved: TranslationPreset? = null
        repo.update { current ->
            val preset = TranslationPresetCatalog.fromSettings(
                id = "custom_${System.currentTimeMillis()}",
                name = name.trim().ifBlank { "Custom preset" },
                shortName = shortName.trim().ifBlank { name.trim().take(8).ifBlank { "Custom" } },
                settings = current
            )
            saved = preset
            current.copy(
                translationPresets = TranslationPresetCatalog.upsertCustom(
                    current.translationPresets,
                    preset
                ),
                activeTranslationPresetId = preset.id
            )
        }
        return saved ?: repo.get().translationPresets.last()
    }

    suspend fun duplicateTranslationPreset(
        id: String,
        name: String,
        shortName: String
    ): TranslationPreset? {
        var saved: TranslationPreset? = null
        repo.update { current ->
            val source = TranslationPresetCatalog.find(current.translationPresets, id)
                ?: return@update current
            val preset = source.copy(
                id = "custom_${System.currentTimeMillis()}",
                name = name.trim().ifBlank { "${source.name} Copy" },
                shortName = shortName.trim().ifBlank { source.shortName }
            )
            saved = preset
            current.copy(
                translationPresets = TranslationPresetCatalog.upsertCustom(
                    current.translationPresets,
                    preset
                )
            )
        }
        return saved
    }

    suspend fun saveTranslationPreset(preset: TranslationPreset): TranslationPreset {
        repo.update { current ->
            current.copy(
                translationPresets = TranslationPresetCatalog.upsertCustom(
                    current.translationPresets,
                    preset
                ),
                activeTranslationPresetId = preset.id
            )
        }
        return preset
    }

    suspend fun deleteTranslationPreset(id: String) {
        if (TranslationPresetCatalog.isBuiltIn(id)) return
        repo.update { current ->
            current.copy(
                translationPresets = current.translationPresets.filterNot { it.id == id },
                activeTranslationPresetId = current.activeTranslationPresetId.takeIf { it != id }.orEmpty()
            )
        }
    }

    suspend fun importTranslationPresets(
        imported: List<TranslationPreset>
    ): TranslationPresetImportResult {
        var importResult: TranslationPresetImportResult? = null
        repo.update { current ->
            val result = TranslationPresetTransfer.mergeImportedPresets(
                existing = current.translationPresets,
                imported = imported,
            )
            importResult = result
            val activeId = current.activeTranslationPresetId.takeIf { id ->
                id.isNotBlank() && TranslationPresetCatalog.find(result.presets, id) != null
            }.orEmpty()
            current.copy(
                translationPresets = result.presets,
                activeTranslationPresetId = activeId,
            )
        }
        return requireNotNull(importResult)
    }

    suspend fun applyTranslationPreset(id: String): Settings? {
        var applied: Settings? = null
        repo.update { current ->
            val preset = TranslationPresetCatalog.find(current.translationPresets, id)
                ?: return@update current
            val next = preset.applyTo(current).copy(activeTranslationPresetId = preset.id)
            applied = next
            next
        }
        return applied
    }

    /** 划词翻译词典 Prompt（仅 OpenAI 兼容引擎用），即时落盘。 */
    suspend fun saveDictionaryPrompt(prompt: String) {
        repo.update { it.copy(dictionaryPrompt = prompt) }
    }

    suspend fun saveTranslationOutputLayout(layout: com.gameocr.app.data.TranslationOutputLayout) {
        repo.update { it.copy(translationOutputLayout = layout) }
    }

    suspend fun saveTranslationOutputFollowRecognition(enabled: Boolean) {
        repo.update { it.copy(translationOutputFollowRecognition = enabled) }
    }

    suspend fun saveTranslationOutputDirection(direction: com.gameocr.app.data.TranslationOutputDirection) {
        repo.update { it.copy(translationOutputDirection = direction) }
    }

    /** 重置悬浮窗口位置 / 大小到默认（X=Y=-1 居中，W/H 回默认）。 */
    suspend fun resetFloatingWindowGeometry() {
        repo.update {
            it.copy(
                floatingWindowX = -1,
                floatingWindowY = -1,
                floatingWindowWidthDp = 320,
                floatingWindowHeightDp = 180
            )
        }
    }

    /**
     * 用户切换 UI 语言后，如果当前 promptTemplate 仍是"上一个 locale 的默认 prompt"
     * （即用户从没改过），把它迁移到当前 locale 的默认。这样英文用户不会看到中文 prompt
     * 又苦于不知道该点"恢复默认"。已自定义的 prompt 不动。
     *
     * 用 [activityContext] 而不是 application context 取 [R.string.default_prompt]：
     * Activity context 的 Configuration 由 framework 保证跟 LocaleManager 同步，最稳。
     *
     * 返回当前应展示的 prompt（迁移后或原值）。
     */
    suspend fun migrateDefaultPromptIfStale(activityContext: Context): String {
        val current = repo.get().promptTemplate
        val currentDefault = activityContext.getString(R.string.default_prompt)
        if (current == currentDefault) return current

        // 列出所有已知 locale 下的 default_prompt；当前 prompt 命中任一即视为"未定制"
        val supportedTags = listOf("zh-CN", "en")
        val knownDefaults = supportedTags.map { tag ->
            val cfg = android.content.res.Configuration(activityContext.resources.configuration)
                .apply { setLocale(java.util.Locale.forLanguageTag(tag)) }
            activityContext.createConfigurationContext(cfg).getString(R.string.default_prompt)
        }
        if (current !in knownDefaults) return current

        repo.update { it.copy(promptTemplate = currentDefault) }
        return currentDefault
    }

    /**
     * 切换语言星标。已收藏则移除；未收藏则追加到末尾。立即落盘，绕过 SettingsScreen
     * 的 dirty 检测——星标是用户的小操作，不应该等"保存"按钮。
     */
    suspend fun togglePinLanguage(code: String) {
        repo.update { current ->
            val list = current.pinnedLanguages
            val next = if (list.contains(code)) list - code else list + code
            current.copy(pinnedLanguages = next)
        }
    }

    /**
     * 测试当前 UI 上未保存的翻译引擎配置是否可用。基于已存档的 Settings，把用户在设置页
     * 改但未保存的几个字段（baseUrl/key/model/deeplKey/deeplPro/engine/timeout）覆盖进去，
     * 避免要求用户必须先点"保存"才能测。
     */
    suspend fun testTranslator(
        translatorEngine: TranslatorEngine,
        remotePcBaseUrl: String,
        remotePcApiKey: String,
        remotePcSessionId: String,
        remotePcImageQuality: Int,
        apiTimeoutSeconds: Int,
        volcAccessKeyId: String = "",
        volcSecretAccessKey: String = "",
        volcRegion: String = "cn-north-1",
        baiduFanyiAppId: String = "",
        baiduFanyiSecretKey: String = "",
        tencentSecretId: String = "",
        tencentSecretKey: String = "",
        tencentRegion: String = ""
    ): TestResult {
        val base = repo.get()
        val normalizedRemotePcBaseUrl = remotePcBaseUrl.trim().trimEnd('/')
        val temp = base.copy(
            translatorEngine = translatorEngine,
            remotePcBaseUrl = normalizedRemotePcBaseUrl,
            remotePcApiKey = remotePcApiKey.trim(),
            remotePcSessionId = remotePcSessionId.trim().ifBlank { "default" },
            remotePcImageQuality = remotePcImageQuality.coerceIn(50, 100),
            cleartextAllowedHosts = cleartextHostsWithLocalOcrUrls(
                base.cleartextAllowedHosts,
                normalizedRemotePcBaseUrl,
            ),
            volcAccessKeyId = volcAccessKeyId.trim().ifBlank { base.volcAccessKeyId },
            volcSecretAccessKey = volcSecretAccessKey.trim().ifBlank { base.volcSecretAccessKey },
            volcRegion = volcRegion.trim().ifBlank { base.volcRegion },
            baiduFanyiAppId = baiduFanyiAppId.trim().ifBlank { base.baiduFanyiAppId },
            baiduFanyiSecretKey = baiduFanyiSecretKey.trim().ifBlank { base.baiduFanyiSecretKey },
            apiTimeoutSeconds = apiTimeoutSeconds.coerceIn(5, 300)
        )
        return routingTranslator.testConnection(temp)
    }
}

internal fun remotePcHttpHostOrNull(baseUrl: String): String? {
    val value = baseUrl.trim().trimEnd('/')
    if (!value.startsWith("http://", ignoreCase = true)) return null
    return runCatching { java.net.URI(value).host }
        .getOrNull()
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
}

internal fun cleartextHostsWithLocalOcrUrls(
    hosts: List<String>,
    remotePcBaseUrl: String,
): List<String> {
    val normalized = hosts.map { it.trim() }.filter { it.isNotEmpty() }
    val remotePcHost = remotePcHttpHostOrNull(remotePcBaseUrl)
    return (normalized + listOfNotNull(remotePcHost))
        .distinctBy { it.lowercase() }
}

