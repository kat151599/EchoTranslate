package com.gameocr.app.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.gameocr.app.R
import com.gameocr.app.capture.CaptureRegion
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore("game_ocr_settings")

private fun normalizeLoopFrameSimilarity(value: Float): Float =
    if (value.isFinite()) value.coerceIn(0.50f, 0.99f) else 0.95f

private fun normalizeLoopTextStableDuration(value: Long): Long = value.coerceIn(200L, 2000L)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    secretCipher: SettingsSecretCipher
) {
    private object Keys {
        val BaseUrl = stringPreferencesKey("base_url")
        val ApiKey = stringPreferencesKey("api_key")
        val Model = stringPreferencesKey("model")
        val AnthropicBaseUrl = stringPreferencesKey("anthropic_base_url")
        val AnthropicApiKey = stringPreferencesKey("anthropic_api_key")
        val AnthropicModel = stringPreferencesKey("anthropic_model")
        val SourceLang = stringPreferencesKey("source_lang")
        val TargetLang = stringPreferencesKey("target_lang")
        val Prompt = stringPreferencesKey("prompt")
        val LoopInterval = longPreferencesKey("loop_interval_ms")
        val LoopTriggerMode = stringPreferencesKey("loop_trigger_mode")
        val DeveloperOptionsEnabled = booleanPreferencesKey("developer_options_enabled")
        val DisableTranslationCache = booleanPreferencesKey("disable_translation_cache")
        val BatchCumulativeCompletionTimeEnabled =
            booleanPreferencesKey("batch_cumulative_completion_time_enabled")
        val DisableCrossLineContextTranslation =
            booleanPreferencesKey("disable_cross_line_context_translation")
        val OverlayStyleMode = stringPreferencesKey("overlay_style_mode")
        val TextSize = intPreferencesKey("overlay_text_size")
        val OverlayTextStyle = stringPreferencesKey("overlay_text_style_json")
        val Alpha = floatPreferencesKey("overlay_alpha")
        val OverlayFontFileName = stringPreferencesKey("overlay_font_file_name")
        val OverlayFontDisplayName = stringPreferencesKey("overlay_font_display_name")
        val OverlayFonts = stringPreferencesKey("overlay_fonts_json")
        val Region = stringPreferencesKey("capture_region_json")
        val RegionSavedW = intPreferencesKey("capture_region_saved_screen_w")
        val RegionSavedH = intPreferencesKey("capture_region_saved_screen_h")
        val Streaming = booleanPreferencesKey("streaming_translate")
        val RetryEmptyTranslation = booleanPreferencesKey("retry_empty_translation")
        val RenderModeKey = stringPreferencesKey("render_mode")
        val TranslationBlockInteractionMode = stringPreferencesKey("translation_block_interaction_mode")
        val A11yVolume = booleanPreferencesKey("a11y_volume_trigger")
        val PreferShizuku = booleanPreferencesKey("prefer_shizuku")
        val Placement = stringPreferencesKey("overlay_placement")
        val OffsetX = intPreferencesKey("overlay_offset_x")
        val OffsetY = intPreferencesKey("overlay_offset_y")
        val ThemeKey = stringPreferencesKey("overlay_theme")
        val CustomBg = intPreferencesKey("overlay_custom_bg")
        val CustomFg = intPreferencesKey("overlay_custom_fg")
        val CustomBorder = intPreferencesKey("overlay_custom_border")
        val CustomBorderW = intPreferencesKey("overlay_custom_border_w")
        val TranslatorEng = stringPreferencesKey("translator_engine")
        val RemotePcBaseUrl = stringPreferencesKey("remote_pc_base_url")
        val RemotePcApiKey = stringPreferencesKey("remote_pc_api_key")
        val RemotePcSessionId = stringPreferencesKey("remote_pc_session_id")
        val RemotePcImageQuality = intPreferencesKey("remote_pc_image_quality")
        val DeeplKey = stringPreferencesKey("deepl_key")
        val DeeplPro = booleanPreferencesKey("deepl_pro")
        val DeeplProtocol = stringPreferencesKey("deepl_protocol")
        val DeeplBaseUrl = stringPreferencesKey("deepl_base_url")
        val DeeplBearerAuth = booleanPreferencesKey("deepl_bearer_auth")
        val DeeplCustomToken = stringPreferencesKey("deepl_custom_token")
        val FloatingSize = intPreferencesKey("floating_button_size_dp")
        val FloatingX = intPreferencesKey("floating_button_x")
        val FloatingY = intPreferencesKey("floating_button_y")
        val FloatingSnapEdge = booleanPreferencesKey("floating_button_snap_edge")
        val FloatingAutoDock = booleanPreferencesKey("floating_button_auto_dock")
        val FloatingDockInset = intPreferencesKey("floating_button_dock_inset_dp")
        val FloatingWindowX = intPreferencesKey("floating_window_x")
        val FloatingWindowY = intPreferencesKey("floating_window_y")
        val FloatingWindowW = intPreferencesKey("floating_window_width_dp")
        val FloatingWindowH = intPreferencesKey("floating_window_height_dp")
        val FloatingWindowContentMode = stringPreferencesKey("floating_window_content_mode")
        val FloatingWindowLocked = booleanPreferencesKey("floating_window_locked")
        val CustomBorderStyle = stringPreferencesKey("custom_border_style")
        /** 0.3.x 旧 key，silent migrate 到 CustomBorderStyle。 */
        val LegacyFloatingWindowBorderStyle = stringPreferencesKey("floating_window_border_style")
        // 收藏的语言代码列表，逗号分隔（"ja,zh-CN,en"）。逗号不可能出现在 BCP-47 tag 里，分隔安全。
        val PinnedLangs = stringPreferencesKey("pinned_languages")
        val OverlayWrap = booleanPreferencesKey("overlay_allow_wrap")
        val OverlayCollision = booleanPreferencesKey("overlay_avoid_collision")
        val ApiTimeoutSec = intPreferencesKey("api_timeout_seconds")
        val TranslationOutputFollowRecognition =
            booleanPreferencesKey("translation_output_follow_recognition")
        val TranslationOutputLayout = stringPreferencesKey("translation_output_layout")
        val TranslationOutputDirection = stringPreferencesKey("translation_output_direction")
        val TranslationGlossaryEnabled = booleanPreferencesKey("translation_glossary_enabled")
        val ForegroundAppDetectionMode = stringPreferencesKey("foreground_app_detection_mode")
        val SendAppNameToTranslator = booleanPreferencesKey("send_app_name_to_translator")
        val VolcAccessKeyId = stringPreferencesKey("volc_access_key_id")
        val VolcSecretAccessKey = stringPreferencesKey("volc_secret_access_key")
        val VolcRegion = stringPreferencesKey("volc_region")
        val BaiduFanyiAppId = stringPreferencesKey("baidu_fanyi_app_id")
        val BaiduFanyiSecretKey = stringPreferencesKey("baidu_fanyi_secret_key")
        // 明文 HTTP 白名单 host，以 \n 分隔保存（hostname 不含 \n，分隔安全）
        val CleartextHosts = stringPreferencesKey("cleartext_allowed_hosts")
        // 弧菜单按钮顺序：逗号分隔 MenuItemId.name 列表。MenuItemId.name 不含逗号，分隔安全。
        val FloatingMenuOrder = stringPreferencesKey("floating_menu_item_order")
        val ArcMenuPageSize = intPreferencesKey("arc_menu_page_size")
        val TranslationPresets = stringPreferencesKey("translation_presets_json")
        val ActiveTranslationPresetId = stringPreferencesKey("active_translation_preset_id")
        val FloatingSkillKey = stringPreferencesKey("floating_button_skill")
        // 划词翻译词典 prompt
        val DictionaryPrompt = stringPreferencesKey("dictionary_prompt")
        // 端侧 LLM 推理参数
        val LocalLlmCtxSize = intPreferencesKey("local_llm_ctx_size")
        val LocalLlmMaxNewTokens = intPreferencesKey("local_llm_max_new_tokens")
        val LocalLlmMirrorChoice = stringPreferencesKey("local_llm_mirror_choice")
        val LocalLlmMirror = stringPreferencesKey("local_llm_mirror_url")
        val SharePromptMainEntryCount = intPreferencesKey("share_prompt_main_entry_count")
        val SharePromptShown = booleanPreferencesKey("share_prompt_shown")
        val MainStatusPresetSeen = booleanPreferencesKey("main_status_preset_seen")
    }

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private val secretCodec = SettingsSecretCodec(secretCipher)
    private var defaultPromptProvider: () -> String = { context.getString(R.string.default_prompt) }
    private var defaultDictionaryPromptProvider: () -> String = {
        context.getString(R.string.default_dictionary_prompt)
    }
    private val secureStringKeys = listOf(
        Keys.BaseUrl,
        Keys.ApiKey,
        Keys.Prompt,
        Keys.RemotePcBaseUrl,
        Keys.RemotePcApiKey,
        Keys.DeeplKey,
        Keys.DeeplBaseUrl,
        Keys.DeeplCustomToken,
        Keys.VolcAccessKeyId,
        Keys.VolcSecretAccessKey,
        Keys.BaiduFanyiAppId,
        Keys.BaiduFanyiSecretKey,
        Keys.CleartextHosts,
        Keys.DictionaryPrompt,
        Keys.LocalLlmMirror,
        Keys.TranslationPresets
    )

    val settings: Flow<Settings> = context.dataStore.data.map { prefs -> prefs.toSettings() }

    suspend fun get(): Settings = settings.first()

    suspend fun recordMainScreenEntryForSharePrompt(): Boolean {
        var eligibleToShow = false
        context.dataStore.edit { prefs ->
            val decision = SharePromptPolicy.onMainScreenEntry(
                storedEntryCount = prefs[Keys.SharePromptMainEntryCount] ?: 0,
                promptAlreadyShown = prefs[Keys.SharePromptShown] ?: false,
            )
            prefs[Keys.SharePromptMainEntryCount] = decision.nextEntryCount
            eligibleToShow = decision.eligibleToShow
        }
        return eligibleToShow
    }

    suspend fun markSharePromptShown() {
        context.dataStore.edit { prefs ->
            prefs[Keys.SharePromptShown] = true
        }
    }

    suspend fun hasSeenMainStatusPreset(): Boolean =
        context.dataStore.data.first()[Keys.MainStatusPresetSeen] ?: false

    suspend fun markMainStatusPresetSeen() {
        context.dataStore.edit { prefs ->
            prefs[Keys.MainStatusPresetSeen] = true
        }
    }

    internal fun setDefaultPromptProvidersForTest(
        prompt: () -> String,
        dictionaryPrompt: () -> String,
    ) {
        defaultPromptProvider = prompt
        defaultDictionaryPromptProvider = dictionaryPrompt
    }

    suspend fun migratePlaintextSecretsIfNeeded(): Int {
        var migrated = 0
        context.dataStore.edit { prefs ->
            secureStringKeys.forEach { key ->
                val raw = prefs[key]
                if (secretCodec.needsMigration(raw)) {
                    prefs[key] = secretCodec.encryptPlainText(raw.orEmpty())
                    migrated++
                }
            }
        }
        return migrated
    }

    /**
     * 任何用到 [Settings.captureRegion] 的入口都应该先调一次：把上次保存时屏幕尺寸跟当前
     * 屏幕尺寸比较，不同就按比例 rescale region 写回，更新 saved 字段。这样旋转屏幕时
     * 无论 Service 在没在跑、Activity 重建多少次，region 都保持「相对位置」语义一致。
     *
     * - 历史数据（saved=0）：当前作为新的 savedScreen 写回，region 不动（用户原来在哪屏幕方向
     *   保存的不知道，保守当成「当前方向就是原方向」）。
     * - savedScreen == currentScreen：不动。
     * - 不同：按 currentW/savedW、currentH/savedH 线性 rescale。
     *
     * 返回 rescale 后的 region（已经写回 DataStore）。
     */
    suspend fun rescaleCaptureRegionIfNeeded(currentW: Int, currentH: Int) {
        if (currentW <= 0 || currentH <= 0) return
        val s = get()
        val region = s.captureRegion ?: return
        val savedW = s.captureRegionSavedScreenW
        val savedH = s.captureRegionSavedScreenH
        if (savedW <= 0 || savedH <= 0) {
            update { it.copy(
                captureRegionSavedScreenW = currentW,
                captureRegionSavedScreenH = currentH
            ) }
            return
        }
        if (savedW == currentW && savedH == currentH) return
        val scaleX = currentW.toFloat() / savedW
        val scaleY = currentH.toFloat() / savedH
        val newRegion = CaptureRegion(
            left = (region.left * scaleX).toInt().coerceIn(0, currentW),
            top = (region.top * scaleY).toInt().coerceIn(0, currentH),
            right = (region.right * scaleX).toInt().coerceIn(0, currentW),
            bottom = (region.bottom * scaleY).toInt().coerceIn(0, currentH)
        )
        update { it.copy(
            captureRegion = newRegion,
            captureRegionSavedScreenW = currentW,
            captureRegionSavedScreenH = currentH
        ) }
    }

    private fun MutablePreferences.putSecure(key: Preferences.Key<String>, value: String) {
        this[key] = secretCodec.encryptPlainText(value)
    }

    private fun Preferences.secureString(
        key: Preferences.Key<String>,
        defaultValue: String
    ): String = this[key]?.let { secretCodec.decodeStored(it) } ?: defaultValue

    suspend fun update(transform: (Settings) -> Settings) {
        context.dataStore.edit { prefs ->
            val current = prefs.toSettings()
            val requested = transform(current)
            val languageSafe = if (
                translationLanguageCodesConflict(requested.sourceLang, requested.targetLang)
            ) {
                requested.copy(
                    sourceLang = current.sourceLang,
                    targetLang = current.targetLang,
                )
            } else {
                requested
            }
            val next = languageSafe
            prefs.putSecure(Keys.BaseUrl, next.baseUrl)
            prefs.putSecure(Keys.ApiKey, next.apiKey)
            prefs[Keys.Model] = next.model
            prefs.putSecure(Keys.AnthropicBaseUrl, next.anthropicBaseUrl)
            prefs.putSecure(Keys.AnthropicApiKey, next.anthropicApiKey)
            prefs[Keys.AnthropicModel] = next.anthropicModel
            prefs[Keys.SourceLang] = next.sourceLang
            prefs[Keys.TargetLang] = next.targetLang
            prefs.putSecure(Keys.Prompt, next.promptTemplate)
            prefs[Keys.LoopInterval] = next.captureLoopIntervalMs
            prefs[Keys.LoopTriggerMode] = next.loopTriggerMode.name
            prefs[Keys.DeveloperOptionsEnabled] = next.developerOptionsEnabled
            prefs[Keys.DisableTranslationCache] = next.disableTranslationCache
            prefs[Keys.BatchCumulativeCompletionTimeEnabled] =
                next.batchCumulativeCompletionTimeEnabled
            prefs[Keys.DisableCrossLineContextTranslation] = next.disableCrossLineContextTranslation
            prefs[Keys.OverlayStyleMode] = next.overlayStyleMode.name
            prefs[Keys.TextSize] = next.overlayTextSizeSp
            prefs[Keys.OverlayTextStyle] = json.encodeToString(next.overlayTextStyle.normalized())
            prefs[Keys.Alpha] = next.overlayAlpha
            prefs[Keys.OverlayFontFileName] = next.overlayFontFileName
            prefs[Keys.OverlayFontDisplayName] = next.overlayFontDisplayName
            prefs[Keys.OverlayFonts] = json.encodeToString(
                OverlayFontPolicy.normalizeImportedFonts(next.overlayFonts)
            )
            prefs[Keys.Region] = next.captureRegion?.let { json.encodeToString(it) } ?: ""
            prefs[Keys.RegionSavedW] = next.captureRegionSavedScreenW
            prefs[Keys.RegionSavedH] = next.captureRegionSavedScreenH
            prefs[Keys.Streaming] = next.streamingTranslate
            prefs[Keys.RetryEmptyTranslation] = next.retryEmptyTranslation
            prefs[Keys.RenderModeKey] = next.renderMode.name
            prefs[Keys.TranslationBlockInteractionMode] = next.translationBlockInteractionMode.name
            prefs[Keys.A11yVolume] = next.a11yVolumeTrigger
            prefs[Keys.PreferShizuku] = next.preferShizukuCapture
            prefs[Keys.Placement] = next.overlayPlacement.name
            prefs[Keys.OffsetX] = next.overlayOffsetX
            prefs[Keys.OffsetY] = next.overlayOffsetY
            prefs[Keys.ThemeKey] = next.overlayTheme.name
            prefs[Keys.CustomBg] = next.customBgColor
            prefs[Keys.CustomFg] = next.customFgColor
            prefs[Keys.CustomBorder] = next.customBorderColor
            prefs[Keys.CustomBorderW] = next.customBorderWidth
            prefs[Keys.TranslatorEng] = next.translatorEngine.name
            prefs.putSecure(Keys.RemotePcBaseUrl, next.remotePcBaseUrl)
            prefs.putSecure(Keys.RemotePcApiKey, next.remotePcApiKey)
            prefs[Keys.RemotePcSessionId] = next.remotePcSessionId
            prefs[Keys.RemotePcImageQuality] = next.remotePcImageQuality.coerceIn(50, 100)
            prefs[Keys.TranslationGlossaryEnabled] = next.translationGlossaryEnabled
            prefs[Keys.ForegroundAppDetectionMode] = next.foregroundAppDetectionMode.name
            prefs[Keys.SendAppNameToTranslator] = next.sendAppNameToTranslator
            prefs.putSecure(Keys.DeeplKey, next.deeplApiKey)
            prefs[Keys.DeeplPro] = next.deeplPro
            prefs[Keys.DeeplProtocol] = next.deeplProtocol.name
            prefs.putSecure(Keys.DeeplBaseUrl, next.deeplBaseUrl)
            prefs[Keys.DeeplBearerAuth] = next.deeplBearerAuth
            prefs.putSecure(Keys.DeeplCustomToken, next.deeplCustomToken)
            prefs[Keys.FloatingSize] = next.floatingButtonSizeDp
            prefs[Keys.FloatingX] = next.floatingButtonX
            prefs[Keys.FloatingY] = next.floatingButtonY
            prefs[Keys.FloatingSnapEdge] = next.floatingButtonSnapToEdge
            prefs[Keys.FloatingAutoDock] = next.floatingButtonAutoDock
            prefs[Keys.FloatingDockInset] = next.floatingButtonDockInsetDp
            prefs[Keys.FloatingWindowX] = next.floatingWindowX
            prefs[Keys.FloatingWindowY] = next.floatingWindowY
            prefs[Keys.FloatingWindowW] = next.floatingWindowWidthDp
            prefs[Keys.FloatingWindowH] = next.floatingWindowHeightDp
            prefs[Keys.FloatingWindowContentMode] = next.floatingWindowContentMode.name
            prefs[Keys.FloatingWindowLocked] = next.floatingWindowLocked
            prefs[Keys.CustomBorderStyle] = next.customBorderStyle.name
            prefs[Keys.PinnedLangs] = next.pinnedLanguages.joinToString(",")
            prefs[Keys.OverlayWrap] = next.overlayAllowWrap
            prefs[Keys.OverlayCollision] = next.overlayAvoidCollision
            prefs[Keys.ApiTimeoutSec] = next.apiTimeoutSeconds
            val translationOutput = resolveTranslationOutputSettings(
                next.translationOutputFollowRecognition,
                next.translationOutputLayout,
                next.translationOutputDirection,
            )
            prefs[Keys.TranslationOutputFollowRecognition] = translationOutput.followRecognition
            prefs[Keys.TranslationOutputLayout] = translationOutput.layout.name
            prefs[Keys.TranslationOutputDirection] = translationOutput.direction.name
            prefs.putSecure(Keys.VolcAccessKeyId, next.volcAccessKeyId)
            prefs.putSecure(Keys.VolcSecretAccessKey, next.volcSecretAccessKey)
            prefs[Keys.VolcRegion] = next.volcRegion
            prefs.putSecure(Keys.BaiduFanyiAppId, next.baiduFanyiAppId)
            prefs.putSecure(Keys.BaiduFanyiSecretKey, next.baiduFanyiSecretKey)
            prefs.putSecure(Keys.CleartextHosts, next.cleartextAllowedHosts.joinToString("\n"))
            prefs[Keys.FloatingMenuOrder] = next.floatingMenuItemOrder.joinToString(",") { it.name }
            prefs[Keys.ArcMenuPageSize] = FloatingMenu.coercePageSize(next.arcMenuPageSize)
            prefs.putSecure(
                Keys.TranslationPresets,
                json.encodeToString(
                    next.translationPresets.filterNot { TranslationPresetCatalog.isBuiltIn(it.id) }
                )
            )
            prefs[Keys.ActiveTranslationPresetId] = next.activeTranslationPresetId
            prefs[Keys.FloatingSkillKey] = next.floatingButtonSkill.name
            prefs.putSecure(Keys.DictionaryPrompt, next.dictionaryPrompt)
            prefs[Keys.LocalLlmCtxSize] = next.localLlmContextSize
            prefs[Keys.LocalLlmMaxNewTokens] = next.localLlmMaxNewTokens
            prefs[Keys.LocalLlmMirrorChoice] = next.localLlmMirror.name
            prefs.putSecure(Keys.LocalLlmMirror, next.localLlmMirrorUrl)
        }
    }

    private fun Preferences.toSettings(): Settings {
        val default = Settings()
        val storedTranslationOutputLayout = runCatching {
            TranslationOutputLayout.valueOf(this[Keys.TranslationOutputLayout] ?: "")
        }.getOrDefault(default.translationOutputLayout)
        val storedTranslationOutputDirection = runCatching {
            TranslationOutputDirection.valueOf(this[Keys.TranslationOutputDirection] ?: "")
        }.getOrDefault(default.translationOutputDirection)
        val translationOutput = resolveTranslationOutputSettings(
            storedFollowRecognition = this[Keys.TranslationOutputFollowRecognition],
            layout = storedTranslationOutputLayout,
            direction = storedTranslationOutputDirection,
        )
        return Settings(
            baseUrl = secureString(Keys.BaseUrl, default.baseUrl),
            apiKey = secureString(Keys.ApiKey, default.apiKey),
            model = this[Keys.Model] ?: default.model,
            anthropicBaseUrl = secureString(Keys.AnthropicBaseUrl, default.anthropicBaseUrl),
            anthropicApiKey = secureString(Keys.AnthropicApiKey, default.anthropicApiKey),
            anthropicModel = this[Keys.AnthropicModel] ?: default.anthropicModel,
            // 兼容 0.1.x 旧用户：那时 sourceLang 用 enum.name（"AUTO"/"JA"/...）保存。
            // 新版改为 BCP-47 tag（"auto"/"ja"/...）。读出时若是旧大写值，按 mapping 转回。
            sourceLang = (this[Keys.SourceLang] ?: default.sourceLang).let { raw ->
                when (raw) {
                    "AUTO" -> "auto"; "JA" -> "ja"; "ZH" -> "zh-CN"
                    "EN" -> "en"; "KO" -> "ko"
                    else -> raw
                }
            },
            targetLang = this[Keys.TargetLang] ?: default.targetLang,
            // 首次启动（Keys.Prompt 不存在）使用资源里的本地化默认 prompt（中文系统给中文，英文给英文）。
            // 用户保存过自己的 prompt 后这里读到自己的，不会被覆盖。
            promptTemplate = secureString(Keys.Prompt, defaultPromptProvider()),
            captureLoopIntervalMs = this[Keys.LoopInterval] ?: default.captureLoopIntervalMs,
            loopTriggerMode = this[Keys.LoopTriggerMode]
                ?.let { runCatching { LoopTriggerMode.valueOf(it) }.getOrNull() }
                ?: default.loopTriggerMode,
            developerOptionsEnabled = this[Keys.DeveloperOptionsEnabled]
                ?: default.developerOptionsEnabled,
            disableTranslationCache = this[Keys.DisableTranslationCache]
                ?: default.disableTranslationCache,
            batchCumulativeCompletionTimeEnabled =
                this[Keys.BatchCumulativeCompletionTimeEnabled]
                    ?: default.batchCumulativeCompletionTimeEnabled,
            disableCrossLineContextTranslation = this[Keys.DisableCrossLineContextTranslation]
                ?: default.disableCrossLineContextTranslation,
            overlayStyleMode = runCatching {
                OverlayStyleMode.valueOf(this[Keys.OverlayStyleMode] ?: "")
            }.getOrDefault(default.overlayStyleMode),
            overlayTextSizeSp = this[Keys.TextSize] ?: default.overlayTextSizeSp,
            overlayTextStyle = this[Keys.OverlayTextStyle]
                ?.takeIf { it.isNotBlank() }
                ?.let { raw ->
                    runCatching { json.decodeFromString<OverlayTextStyle>(raw).normalized() }.getOrNull()
                }
                ?: default.overlayTextStyle,
            overlayAlpha = this[Keys.Alpha] ?: default.overlayAlpha,
            overlayFontFileName = this[Keys.OverlayFontFileName] ?: default.overlayFontFileName,
            overlayFontDisplayName = this[Keys.OverlayFontDisplayName] ?: default.overlayFontDisplayName,
            overlayFonts = this[Keys.OverlayFonts]
                ?.takeIf { it.isNotBlank() }
                ?.let { raw ->
                    runCatching {
                        OverlayFontPolicy.normalizeImportedFonts(
                            json.decodeFromString<List<OverlayFontEntry>>(raw)
                        )
                    }.getOrNull()
                }
                ?: default.overlayFonts,
            captureRegion = this[Keys.Region]?.takeIf { it.isNotBlank() }?.let {
                runCatching { json.decodeFromString<CaptureRegion>(it) }.getOrNull()
            },
            captureRegionSavedScreenW = this[Keys.RegionSavedW] ?: default.captureRegionSavedScreenW,
            captureRegionSavedScreenH = this[Keys.RegionSavedH] ?: default.captureRegionSavedScreenH,
            streamingTranslate = this[Keys.Streaming] ?: default.streamingTranslate,
            retryEmptyTranslation = this[Keys.RetryEmptyTranslation] ?: default.retryEmptyTranslation,
            // 0.3.x 之前 RenderMode 叫 BANNER，0.4 改名为 FLOATING_WINDOW。silent migrate 老值。
            renderMode = (this[Keys.RenderModeKey] ?: "").let { raw ->
                runCatching { RenderMode.valueOf(raw) }.getOrElse {
                    if (raw == "BANNER") RenderMode.FLOATING_WINDOW else default.renderMode
                }
            },
            translationBlockInteractionMode = runCatching {
                TranslationBlockInteractionMode.valueOf(this[Keys.TranslationBlockInteractionMode] ?: "")
            }.getOrDefault(default.translationBlockInteractionMode),
            a11yVolumeTrigger = this[Keys.A11yVolume] ?: default.a11yVolumeTrigger,
            preferShizukuCapture = this[Keys.PreferShizuku] ?: default.preferShizukuCapture,
            overlayPlacement = runCatching { OverlayPlacement.valueOf(this[Keys.Placement] ?: "") }
                .getOrDefault(default.overlayPlacement),
            overlayOffsetX = this[Keys.OffsetX] ?: default.overlayOffsetX,
            overlayOffsetY = this[Keys.OffsetY] ?: default.overlayOffsetY,
            overlayTheme = runCatching { OverlayTheme.valueOf(this[Keys.ThemeKey] ?: "") }
                .getOrDefault(default.overlayTheme),
            customBgColor = this[Keys.CustomBg] ?: default.customBgColor,
            customFgColor = this[Keys.CustomFg] ?: default.customFgColor,
            customBorderColor = this[Keys.CustomBorder] ?: default.customBorderColor,
            customBorderWidth = this[Keys.CustomBorderW] ?: default.customBorderWidth,
            // LEGACY_COMPAT: old persisted values are ignored and normalized to Remote PC.
            translatorEngine = TranslatorEngine.REMOTE_PC,
            remotePcBaseUrl = secureString(Keys.RemotePcBaseUrl, default.remotePcBaseUrl),
            remotePcApiKey = secureString(Keys.RemotePcApiKey, default.remotePcApiKey),
            remotePcSessionId = this[Keys.RemotePcSessionId] ?: default.remotePcSessionId,
            remotePcImageQuality = (this[Keys.RemotePcImageQuality] ?: default.remotePcImageQuality).coerceIn(50, 100),
            translationGlossaryEnabled = this[Keys.TranslationGlossaryEnabled]
                ?: default.translationGlossaryEnabled,
            foregroundAppDetectionMode = runCatching {
                ForegroundAppDetectionMode.valueOf(this[Keys.ForegroundAppDetectionMode] ?: "")
            }.getOrDefault(default.foregroundAppDetectionMode),
            sendAppNameToTranslator = this[Keys.SendAppNameToTranslator]
                ?: default.sendAppNameToTranslator,
            deeplApiKey = secureString(Keys.DeeplKey, default.deeplApiKey),
            deeplPro = this[Keys.DeeplPro] ?: default.deeplPro,
            deeplProtocol = runCatching { DeeplProtocol.valueOf(this[Keys.DeeplProtocol] ?: "") }
                .getOrDefault(default.deeplProtocol),
            deeplBaseUrl = secureString(Keys.DeeplBaseUrl, default.deeplBaseUrl),
            deeplBearerAuth = this[Keys.DeeplBearerAuth] ?: default.deeplBearerAuth,
            deeplCustomToken = secureString(Keys.DeeplCustomToken, default.deeplCustomToken),
            floatingButtonSizeDp = this[Keys.FloatingSize] ?: default.floatingButtonSizeDp,
            floatingButtonX = this[Keys.FloatingX] ?: default.floatingButtonX,
            floatingButtonY = this[Keys.FloatingY] ?: default.floatingButtonY,
            floatingButtonSnapToEdge = this[Keys.FloatingSnapEdge] ?: default.floatingButtonSnapToEdge,
            floatingButtonAutoDock = this[Keys.FloatingAutoDock] ?: default.floatingButtonAutoDock,
            floatingButtonDockInsetDp = this[Keys.FloatingDockInset] ?: default.floatingButtonDockInsetDp,
            floatingWindowX = this[Keys.FloatingWindowX] ?: default.floatingWindowX,
            floatingWindowY = this[Keys.FloatingWindowY] ?: default.floatingWindowY,
            floatingWindowWidthDp = this[Keys.FloatingWindowW] ?: default.floatingWindowWidthDp,
            floatingWindowHeightDp = this[Keys.FloatingWindowH] ?: default.floatingWindowHeightDp,
            floatingWindowContentMode = runCatching {
                FloatingWindowContentMode.valueOf(this[Keys.FloatingWindowContentMode] ?: "")
            }.getOrDefault(default.floatingWindowContentMode),
            floatingWindowLocked = this[Keys.FloatingWindowLocked] ?: default.floatingWindowLocked,
            customBorderStyle = runCatching {
                BorderStyle.valueOf(this[Keys.CustomBorderStyle] ?: this[Keys.LegacyFloatingWindowBorderStyle] ?: "")
            }.getOrDefault(default.customBorderStyle),
            pinnedLanguages = this[Keys.PinnedLangs]
                ?.split(',')
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?: default.pinnedLanguages,
            overlayAllowWrap = this[Keys.OverlayWrap] ?: default.overlayAllowWrap,
            overlayAvoidCollision = this[Keys.OverlayCollision] ?: default.overlayAvoidCollision,
            apiTimeoutSeconds = this[Keys.ApiTimeoutSec] ?: default.apiTimeoutSeconds,
            translationOutputFollowRecognition = translationOutput.followRecognition,
            translationOutputLayout = translationOutput.layout,
            translationOutputDirection = translationOutput.direction,
            volcAccessKeyId = secureString(Keys.VolcAccessKeyId, default.volcAccessKeyId),
            volcSecretAccessKey = secureString(Keys.VolcSecretAccessKey, default.volcSecretAccessKey),
            volcRegion = this[Keys.VolcRegion] ?: default.volcRegion,
            baiduFanyiAppId = secureString(Keys.BaiduFanyiAppId, default.baiduFanyiAppId),
            baiduFanyiSecretKey = secureString(Keys.BaiduFanyiSecretKey, default.baiduFanyiSecretKey),
            cleartextAllowedHosts = secureString(
                Keys.CleartextHosts,
                default.cleartextAllowedHosts.joinToString("\n")
            )
                .split('\n')
                .map { it.trim() }
                .filter { it.isNotEmpty() },
            // 弧菜单按钮顺序：脏数据 / 未知 id silently 丢弃；丢失的已知 id 自动补齐到末尾，
            // 保证 ALL_ORDER 里所有 id 都出现一次。这样后续新版本加新菜单项，老用户也能看到。
            floatingMenuItemOrder = run {
                val raw = this[Keys.FloatingMenuOrder]
                if (raw.isNullOrBlank()) return@run default.floatingMenuItemOrder
                val parsed = raw.split(',')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .mapNotNull { tok -> runCatching { MenuItemId.valueOf(tok) }.getOrNull() }
                    .filter { it in FloatingMenu.ARC_MENU_ITEM_IDS }
                    .distinct()
                if (parsed.isEmpty()) return@run default.floatingMenuItemOrder
                // 补齐缺失的已知 id
                val missing = FloatingMenu.ALL_ORDER.filter { it !in parsed }
                val normalized = parsed + missing
                if (
                    normalized == FloatingMenu.LEGACY_DEFAULT_ORDER_BEFORE_SKILL_SWAP ||
                    normalized == FloatingMenu.LEGACY_DEFAULT_ORDER_BEFORE_PRESET_SKILL_SWAP ||
                    normalized == FloatingMenu.LEGACY_DEFAULT_ORDER_BEFORE_PRESET_LANGUAGE_SWAP
                ) {
                    default.floatingMenuItemOrder
                } else {
                    normalized
                }
            },
            arcMenuPageSize = FloatingMenu.coercePageSize(
                this[Keys.ArcMenuPageSize] ?: default.arcMenuPageSize
            ),
            translationPresets = secureString(Keys.TranslationPresets, "")
                .takeIf { it.isNotBlank() }
                ?.let { raw ->
                    runCatching { json.decodeFromString<List<TranslationPreset>>(raw) }
                        .getOrDefault(emptyList())
                        .filterNot { TranslationPresetCatalog.isBuiltIn(it.id) }
                }
                ?: default.translationPresets,
            activeTranslationPresetId = this[Keys.ActiveTranslationPresetId]
                ?: default.activeTranslationPresetId,
            floatingButtonSkill = runCatching { FloatingSkill.valueOf(this[Keys.FloatingSkillKey] ?: "") }
                .getOrDefault(default.floatingButtonSkill),
            dictionaryPrompt = secureString(
                Keys.DictionaryPrompt,
                defaultDictionaryPromptProvider()
            ),
            localLlmContextSize = this[Keys.LocalLlmCtxSize] ?: default.localLlmContextSize,
            localLlmMaxNewTokens = this[Keys.LocalLlmMaxNewTokens] ?: default.localLlmMaxNewTokens,
            localLlmMirror = runCatching { LlmMirrorChoice.valueOf(this[Keys.LocalLlmMirrorChoice] ?: "") }
                .getOrDefault(default.localLlmMirror),
            localLlmMirrorUrl = secureString(Keys.LocalLlmMirror, default.localLlmMirrorUrl),
            // runtimeTranslationContext, runtimeTranslationScopePackage, and
            // runtimeTranslationScopeLabel are request-scoped and deliberately never persisted.
        )
    }
}


