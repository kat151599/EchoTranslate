package com.gameocr.app.ui
import android.content.Intent
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import android.text.format.Formatter
import android.util.TypedValue
import android.widget.Toast
import timber.log.Timber
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.HelpOutline
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.DisposableEffect
import com.gameocr.app.overlay.EdgeInsetPreviewOverlay
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.work.WorkInfo
import com.gameocr.app.R
import com.gameocr.app.capture.LoopFrameChangePolicy
import com.gameocr.app.capture.LoopFrameStabilityPolicy
import com.gameocr.app.appcontext.isUsageAccessGranted
import com.gameocr.app.data.FloatingMenu
import com.gameocr.app.data.FloatingSkill
import com.gameocr.app.data.Languages
import com.gameocr.app.data.LoopTriggerMode
import com.gameocr.app.data.LoopTextRegionMode
import com.gameocr.app.data.MenuItemId
import com.gameocr.app.data.OverlayFontImportError
import com.gameocr.app.data.OverlayFontImportResult
import com.gameocr.app.data.OverlayFontPolicy
import com.gameocr.app.data.OverlayFontEntry
import com.gameocr.app.data.OverlayPlacement
import com.gameocr.app.data.OverlayStyleMode
import com.gameocr.app.data.OverlayTheme
import com.gameocr.app.data.OverlayTextAlignment
import com.gameocr.app.data.OverlayTextStyle
import com.gameocr.app.download.ModelDownloadSpec
import com.gameocr.app.download.ModelDownloadTerminalRecord
import com.gameocr.app.download.ModelDownloadType
import com.gameocr.app.download.ModelDownloadWorkPolicy
import com.gameocr.app.download.latestUnresolvedModelDownloadFailure
import com.gameocr.app.data.RenderMode
import com.gameocr.app.data.Settings
import com.gameocr.app.data.SettingsBundlePreview
import com.gameocr.app.data.SettingsBundleTransfer
import com.gameocr.app.data.TranslationPreset
import com.gameocr.app.data.TranslationPresetCatalog
import com.gameocr.app.data.TranslationPresetImportPlan
import com.gameocr.app.data.TranslationBlockInteractionMode
import com.gameocr.app.data.TranslationPresetTransfer
import com.gameocr.app.data.TranslatorEngine
import com.gameocr.app.data.translationLanguageCodesConflict
import com.gameocr.app.data.swappedTranslationLanguagePair
import com.gameocr.app.data.resolveTranslationOutputSettings
import com.gameocr.app.data.manualOverlayLayoutControlsEnabled
import com.gameocr.app.data.settingsSearchEntryId
import com.gameocr.app.llm.LlmModelKind
import com.gameocr.app.overlay.StyledTranslationTextView
import com.gameocr.app.overlay.MenuItemRegistry
import com.gameocr.app.overlay.applyOverlayTextStyle
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.height
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class LanguageSwapRequestOrigin {
    SOURCE_PICKER,
    TARGET_PICKER,
}

@OptIn(ExperimentalFoundationApi::class)
private class SettingsSearchTargetRegistry {
    private val requesters = mutableMapOf<Int, LinkedHashSet<BringIntoViewRequester>>()

    fun register(targetIds: Set<Int>, requester: BringIntoViewRequester) {
        targetIds.forEach { targetId ->
            requesters.getOrPut(targetId) { linkedSetOf() }.add(requester)
        }
    }

    fun unregister(targetIds: Set<Int>, requester: BringIntoViewRequester) {
        targetIds.forEach { targetId ->
            requesters[targetId]?.let { registered ->
                registered.remove(requester)
                if (registered.isEmpty()) requesters.remove(targetId)
            }
        }
    }

    fun latest(targetId: Int): BringIntoViewRequester? = requesters[targetId]?.lastOrNull()
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun SettingsSearchTarget(
    registry: SettingsSearchTargetRegistry,
    vararg targetIds: Int,
    content: @Composable () -> Unit,
) {
    val requester = remember { BringIntoViewRequester() }
    val registeredIds = targetIds.toSet()
    DisposableEffect(registry, registeredIds) {
        registry.register(registeredIds, requester)
        onDispose { registry.unregister(registeredIds, requester) }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(requester),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        content()
    }
}

private fun openExternalBrowser(context: Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }.onFailure { error ->
        Timber.w(error, "Could not open external browser url=%s", url)
        Toast.makeText(context, R.string.settings_external_browser_unavailable, Toast.LENGTH_SHORT).show()
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    ExperimentalFoundationApi::class,
)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    listState: LazyListState,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val modelDownloadWorkInfos by viewModel.modelDownloadWorkInfos.collectAsState(initial = emptyList())
    val unfinishedModelDownloads = modelDownloadWorkInfos.filterNot { it.state.isFinished }
    val activeModelDownloads = unfinishedModelDownloads
        .sortedByDescending { it.state == WorkInfo.State.RUNNING }
        .map { info ->
            ActiveModelDownloadUi(
                id = info.id,
                spec = ModelDownloadSpec.decode(
                    info.progress.getString(
                        com.gameocr.app.download.ModelDownloadWorker.KEY_CURRENT_SPEC
                    ).orEmpty()
                ),
                status = info.progress.getString(
                    com.gameocr.app.download.ModelDownloadWorker.KEY_STATUS
                ).orEmpty(),
                downloaded = info.progress.getLong(
                    com.gameocr.app.download.ModelDownloadWorker.KEY_DOWNLOADED,
                    0L,
                ),
                total = info.progress.getLong(
                    com.gameocr.app.download.ModelDownloadWorker.KEY_TOTAL,
                    -1L,
                ),
                ownerPresetId = ModelDownloadWorkPolicy.ownerPresetId(info.tags),
            )
        }
    val backgroundModelDownloadActive = activeModelDownloads.isNotEmpty()
    val activeModelDownloadBySpec = activeModelDownloads
        .mapNotNull { item -> item.spec?.let { it to item } }
        .toMap()
    val backgroundModelDownloadOwnerPresetId = activeModelDownloads
        .mapNotNull { it.ownerPresetId }
        .distinct()
        .singleOrNull()
    val activeModelDownloadRequestKeys = activeModelDownloads
        .mapNotNull { it.spec }
        .map { ModelDownloadWorkPolicy.requestKey(listOf(it)) }
        .toSet()
    val terminalModelDownloads = modelDownloadWorkInfos.mapNotNull { info ->
        val succeeded = when (info.state) {
            WorkInfo.State.SUCCEEDED -> true
            WorkInfo.State.FAILED -> false
            else -> return@mapNotNull null
        }
        val specs = ModelDownloadSpec.decodeAll(
            info.outputData.getStringArray(
                com.gameocr.app.download.ModelDownloadWorker.KEY_SPECS
            ).orEmpty()
        ) ?: return@mapNotNull null
        ModelDownloadTerminalRecord(
            specs = specs,
            succeeded = succeeded,
            status = info.outputData.getString(
                com.gameocr.app.download.ModelDownloadWorker.KEY_STATUS
            ).orEmpty(),
            error = info.outputData.getString(
                com.gameocr.app.download.ModelDownloadWorker.KEY_ERROR
            ).orEmpty(),
            file = info.outputData.getString(
                com.gameocr.app.download.ModelDownloadWorker.KEY_FILE
            ).orEmpty(),
            downloaded = info.outputData.getLong(
                com.gameocr.app.download.ModelDownloadWorker.KEY_DOWNLOADED,
                0L,
            ),
            total = info.outputData.getLong(
                com.gameocr.app.download.ModelDownloadWorker.KEY_TOTAL,
                -1L,
            ),
            finishedAt = info.outputData.getLong(
                com.gameocr.app.download.ModelDownloadWorker.KEY_FINISHED_AT,
                0L,
            ),
            ownerPresetId = ModelDownloadWorkPolicy.ownerPresetId(info.tags),
        )
    }
    val unresolvedModelDownloadFailure =
        latestUnresolvedModelDownloadFailure(
            records = terminalModelDownloads,
            activeRequestKeys = activeModelDownloadRequestKeys,
        )
    fun statusDuringBackgroundDownload(spec: ModelDownloadSpec, fallback: String): String =
        activeModelDownloadBySpec[spec]?.status?.takeIf { it.isNotBlank() } ?: fallback
    val usageAccessGranted = rememberUsageAccessGranted(context)

    var baseUrl by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var anthropicBaseUrl by remember { mutableStateOf("") }
    var anthropicApiKey by remember { mutableStateOf("") }
    var anthropicModel by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    var targetLang by remember { mutableStateOf("zh-CN") }
    var sourceLang by remember { mutableStateOf("auto") }
    var translatorEngine by remember { mutableStateOf(TranslatorEngine.OPENAI) }
    var remotePcBaseUrl by remember { mutableStateOf("http://192.168.1.100:8765") }
    var remotePcApiKey by remember { mutableStateOf("") }
    var remotePcSessionId by remember { mutableStateOf("default") }
    var remotePcImageQuality by remember { mutableStateOf("85") }
    // 端侧 LLM 翻译：状态文本（"已就绪 · XX MB" / "未下载" / "下载中 …"）。仅在 LOCAL_* 引擎时显示。
    var llmModelStatus by remember { mutableStateOf("") }
    var llmDownloading by remember { mutableStateOf(false) }
    var llmModelReady by remember { mutableStateOf(false) }
    // 镜像选择：radio（HF / hf-mirror / 自定义）；仅 CUSTOM 时 llmMirror 才作为 base URL。
    var llmMirrorChoice by remember { mutableStateOf(com.gameocr.app.data.LlmMirrorChoice.HF_MIRROR) }
    var llmMirror by remember { mutableStateOf("") }
    var deeplKey by remember { mutableStateOf("") }
    var deeplPro by remember { mutableStateOf(false) }
    var deeplBaseUrl by remember { mutableStateOf("") }
    var deeplBearerAuth by remember { mutableStateOf(false) }
    var deeplCustomToken by remember { mutableStateOf("") }
    var deeplProtocol by remember { mutableStateOf(com.gameocr.app.data.DeeplProtocol.OFFICIAL) }
    var deeplAdvancedExpanded by remember { mutableStateOf(false) }
    // 有道智云一套 key（OCR + 图片翻译共用）
    var youdaoAppKey by remember { mutableStateOf("") }
    var youdaoAppSecret by remember { mutableStateOf("") }
    // 火山引擎机器翻译 AK/SK + region（SignV4）
    var volcAk by remember { mutableStateOf("") }
    var volcSk by remember { mutableStateOf("") }
    var volcRegion by remember { mutableStateOf("cn-north-1") }
    // 百度翻译开放平台 APPID + 密钥（与百度智能云 OCR 完全不是一回事）
    var baiduFanyiAppId by remember { mutableStateOf("") }
    var baiduFanyiSecret by remember { mutableStateOf("") }
    // 翻译引擎"测试连接"按钮的瞬时状态：testing / 结果文字 / 成功色 / OpenAI 拉到的 model 列表。
    // 不进 Settings，纯 UI 状态；切换 engine 不清空（用户切回去还能看到上次的结果）。
    var testRunning by remember { mutableStateOf(false) }
    var testMessage by remember { mutableStateOf<String?>(null) }
    var testSuccess by remember { mutableStateOf(false) }
    var mlKitModelDownloadRunning by remember { mutableStateOf(false) }
    var mlKitModelDownloadMessage by remember { mutableStateOf<String?>(null) }
    var mlKitModelStatePair by remember { mutableStateOf<Pair<String, String>?>(null) }
    var mlKitModelsReady by remember { mutableStateOf<Boolean?>(null) }
    var mlKitDownloadedLanguageModels by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showMlKitMoreLanguages by remember { mutableStateOf(false) }
    var fetchedModels by remember { mutableStateOf<List<String>>(emptyList()) }
    var modelPickerExpanded by remember { mutableStateOf(false) }
    var textSize by remember { mutableStateOf(14f) }
    var overlayTextStyle by remember { mutableStateOf(OverlayTextStyle()) }
    var alpha by remember { mutableStateOf(0.85f) }
    var overlayFontFileName by remember { mutableStateOf("") }
    var overlayFontDisplayName by remember { mutableStateOf("") }
    var overlayFontEntries by remember { mutableStateOf<List<OverlayFontEntry>>(emptyList()) }
    var overlayFontTypeface by remember { mutableStateOf<android.graphics.Typeface?>(null) }
    var overlayFontMessage by remember { mutableStateOf<String?>(null) }
    var overlayFontMessageIsError by remember { mutableStateOf(false) }
    var pendingOverlayFontDelete by remember { mutableStateOf<OverlayFontEntry?>(null) }
    var showOverlayFontDeleteTip by remember { mutableStateOf(false) }
    var overlayFontDeleteTipCountdown by remember { mutableStateOf(0) }
    var loopInterval by remember { mutableStateOf("1000") }
    var loopTriggerMode by remember { mutableStateOf(LoopTriggerMode.WAIT_FOR_TEXT_COMPLETE) }
    var loopTextStableDurationMs by remember {
        mutableStateOf(LoopFrameStabilityPolicy.DEFAULT_STABLE_DURATION_MS)
    }
    var loopSkipSimilarFrames by remember { mutableStateOf(true) }
    var loopFrameSimilarityThreshold by remember {
        mutableStateOf(LoopFrameChangePolicy.DEFAULT_SIMILARITY_THRESHOLD)
    }
    var loopTextRegionMode by remember { mutableStateOf(LoopTextRegionMode.AUTO) }
    var loopTranslateRegionOnly by remember { mutableStateOf(true) }
    var developerOptionsEnabled by remember { mutableStateOf(false) }
    var disableTranslationCache by remember { mutableStateOf(false) }
    var batchCumulativeCompletionTimeEnabled by remember { mutableStateOf(false) }
    var streaming by remember { mutableStateOf(true) }
    var retryEmptyTranslation by remember { mutableStateOf(false) }
    var renderMode by remember { mutableStateOf(RenderMode.BLOCKS) }
    var translationBlockInteractionMode by remember {
        mutableStateOf(TranslationBlockInteractionMode.COPY_BUTTON)
    }
    var floatingWindowContentMode by remember {
        mutableStateOf(com.gameocr.app.data.FloatingWindowContentMode.SRC_AND_DST)
    }
    var floatingWindowLocked by remember { mutableStateOf(false) }
    var customBorderStyle by remember {
        mutableStateOf(com.gameocr.app.data.BorderStyle.SOLID)
    }
    var placement by remember { mutableStateOf(OverlayPlacement.BELOW) }
    var overlayStyleMode by remember { mutableStateOf(OverlayStyleMode.FIXED) }
    var overlayTheme by remember { mutableStateOf(OverlayTheme.CLASSIC_DARK) }
    var customBg by remember { mutableStateOf(0xE6000000.toInt()) }
    var customFg by remember { mutableStateOf(0xFFFFFFFF.toInt()) }
    var customBorder by remember { mutableStateOf(0) }
    var customBorderW by remember { mutableStateOf(0f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var a11yVolume by remember { mutableStateOf(false) }
    var floatingSize by remember { mutableStateOf(56f) }
    var floatingSnapEdge by remember { mutableStateOf(true) }
    var floatingAutoDock by remember { mutableStateOf(false) }
    var floatingDockInset by remember { mutableStateOf(0f) }
    // 弧菜单按钮顺序 + 划词词典 prompt：拖动 / 编辑后即时通过 vm 的 saveArcMenuOrder / saveDictionaryPrompt
    // 单字段落盘，**不**走主 save 的 dirty 流程（用户期望立刻生效，无需点保存）。
    var menuOrder by remember { mutableStateOf<List<MenuItemId>>(emptyList()) }
    var arcMenuPageSize by remember { mutableStateOf(FloatingMenu.DEFAULT_PAGE_SIZE.toFloat()) }
    // 当前主球技能。技能槽（FULL_SCREEN_SKILL）那一行的文案要跟着它动态显示「切到对方」：
    // 当前 FULL_SCREEN → 显示「— 划词翻译」；当前 WORD_SELECT → 显示「— 全屏翻译」
    var currentSkill by remember { mutableStateOf(com.gameocr.app.data.FloatingSkill.FULL_SCREEN) }
    var dictionaryPrompt by remember { mutableStateOf("") }
    // 悬浮按钮"贴边距离" slider 的实时预览：屏幕两侧画 inset 宽度的半透粉条。
    // 默认 false——进设置就显示条带太突兀；用户在 slider 旁手动开启「预览」后才覆盖到屏幕上。
    var insetPreviewActive by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val insetPreview = remember { EdgeInsetPreviewOverlay(context) }
    LaunchedEffect(insetPreviewActive, floatingDockInset, floatingSnapEdge) {
        if (insetPreviewActive && floatingSnapEdge) {
            val px = with(density) { floatingDockInset.dp.roundToPx() }
            insetPreview.update(px)
        } else {
            insetPreview.hide()
        }
    }
    DisposableEffect(Unit) {
        onDispose { insetPreview.hide() }
    }
    var allowWrap by remember { mutableStateOf(true) }
    var avoidCollision by remember { mutableStateOf(true) }
    var apiTimeoutSec by remember { mutableStateOf(30f) }
    var mergeAdjacent by remember { mutableStateOf(true) }
    var mergeStrength by remember { mutableStateOf(com.gameocr.app.data.MergeStrength.STANDARD) }
    var crossLineContextTranslationEnabled by remember { mutableStateOf(true) }
    // 端侧 LLM 推理参数。Slider 切换即时落盘（saveLocalLlmInferenceParams），下次翻译生效。
    var localLlmContextSize by remember { mutableStateOf(2048) }
    var localLlmMaxNewTokens by remember { mutableStateOf(256) }
    var llmInferenceParamsExpanded by remember { mutableStateOf(false) }
    var translationOutputFollowRecognition by remember { mutableStateOf(true) }
    var translationOutputLayout by remember {
        mutableStateOf(com.gameocr.app.data.TranslationOutputLayout.HORIZONTAL)
    }
    var translationOutputDirection by remember {
        mutableStateOf(com.gameocr.app.data.TranslationOutputDirection.LEFT_TO_RIGHT)
    }
    var foregroundAppDetectionMode by remember {
        mutableStateOf(com.gameocr.app.data.ForegroundAppDetectionMode.AUTO)
    }
    var sendAppNameToTranslator by remember { mutableStateOf(false) }
    var translationPresets by remember { mutableStateOf<List<TranslationPreset>>(emptyList()) }
    var activeTranslationPresetId by remember { mutableStateOf("") }
    var presetMessage by remember { mutableStateOf<String?>(null) }
    var pendingPresetImportPlan by remember { mutableStateOf<TranslationPresetImportPlan?>(null) }
    var pendingSettingsImportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var pendingSettingsImportPreview by remember { mutableStateOf<SettingsBundlePreview?>(null) }
    var pendingSettingsExport by remember { mutableStateOf<Settings?>(null) }
    var presetLlmModelReady by remember { mutableStateOf<Map<LlmModelKind, Boolean>>(emptyMap()) }
    var downloadingPresetId by remember { mutableStateOf<String?>(null) }
    // 明文 HTTP 白名单：用户每行一个 host，UI 上用 String，保存时 split("\n")
    var cleartextHostsText by remember { mutableStateOf("") }
    // 星标语言：本地镜像。togglePinLanguage 立即落盘，下次 ON_RESUME / load() 拉回最新；
    // 这里也乐观更新一份本地状态，UI 立刻反映。
    var pinnedLanguages by remember { mutableStateOf<List<String>>(emptyList()) }

    // dirty 检测：load 时 capture 一份初始 Settings，之后跟 buildSnapshot() 比 equals。
    // 旧版手写两份 List<Any?>，每加 Settings 字段都要在两个 list 同步加，反复犯"忘改一边"的 bug。
    // 现在用 data class equals 自动覆盖所有字段——加字段只改 buildSnapshot() 一处。
    var initialSettings by remember { mutableStateOf<Settings?>(null) }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var showSakuraFallbackDialog by remember { mutableStateOf(false) }
    var showUnsupportedPresetDownloadDialog by remember { mutableStateOf(false) }
    var pendingLanguageSwapOrigin by remember {
        mutableStateOf<LanguageSwapRequestOrigin?>(null)
    }
    var pendingModelDownload by remember { mutableStateOf<PendingModelDownload?>(null) }
    val continueModelDownloadAfterNotificationPermission =
        rememberModelDownloadNotificationPermissionGate()

    fun requestModelDownload(modelLabel: String, onConfirmed: () -> Unit) {
        val warning = modelDownloadNetworkWarningFor(currentModelDownloadNetworkKind(context))
        if (warning == null) {
            continueModelDownloadAfterNotificationPermission(onConfirmed)
        } else {
            pendingModelDownload = PendingModelDownload(modelLabel, warning) {
                continueModelDownloadAfterNotificationPermission(onConfirmed)
            }
        }
    }

    val localLlmDeviceCapable = remember { viewModel.llmDeviceCapable() }
    suspend fun refreshLlmModelState(kind: LlmModelKind) {
        val state = withContext(Dispatchers.IO) { viewModel.llmModelUiState(kind) }
        llmModelStatus = state.status
        llmModelReady = state.ready
        presetLlmModelReady = presetLlmModelReady + (kind to state.ready)
    }
    suspend fun refreshPresetModelReadiness(customPresets: List<TranslationPreset> = translationPresets) {
        val llmKinds = TranslationPresetCatalog.all(customPresets)
            .mapNotNull { localLlmModelKindFor(it.translatorEngine) }
            .toSet()
        presetLlmModelReady = withContext(Dispatchers.IO) {
            llmKinds.associateWith { kind -> viewModel.llmModelReady(kind) }
        }
    }
    fun effectiveTranslatorEngine(): TranslatorEngine =
        if (isLocalLlmEngine(translatorEngine) && !localLlmDeviceCapable) {
            TranslatorEngine.OPENAI
        } else {
            translatorEngine
    }
    fun selectTranslatorEngine(engine: TranslatorEngine) {
        translatorEngine = engine
        if (engine == TranslatorEngine.LOCAL_SAKURA && !supportsSakuraLanguagePair(sourceLang, targetLang)) {
            showSakuraFallbackDialog = true
        }
    }

    /*
    fun selectMlKitSourceLanguage(languageTag: String) {
        if (translationLanguageCodesConflict(languageTag, targetLang)) return
        timber.log.Timber.tag("MlKitTrans").i(
            "[select-on-device-source] %s -> %s", sourceLang, languageTag
        )
        sourceLang = languageTag
        mlKitRecentSources = mlKitRecentSourceLanguages(mlKitRecentSources, languageTag)
        mlKitModelDownloadMessage = null
        selectTranslatorEngine(TranslatorEngine.GOOGLE_ML_KIT)
    }

    fun swapSelectedLanguages() {
        val swapped = swappedTranslationLanguagePair(sourceLang, targetLang) ?: return
        timber.log.Timber.tag("TranslationLanguage").i(
            "[swap] %s -> %s becomes %s -> %s",
            sourceLang,
            targetLang,
            swapped.first,
            swapped.second,
        )
        sourceLang = swapped.first
        targetLang = swapped.second
        mlKitModelDownloadMessage = null
        if (translatorEngine == TranslatorEngine.GOOGLE_ML_KIT) {
            mlKitRecentSources = mlKitRecentSourceLanguages(
                stored = mlKitRecentSources,
                selected = swapped.first,
            )
        }
        if (
            translatorEngine == TranslatorEngine.LOCAL_SAKURA &&
            !supportsSakuraLanguagePair(swapped.first, swapped.second)
        ) {
            showSakuraFallbackDialog = true
        }
    }

    fun startMlKitModelDownload(pair: Pair<String, String>) {
        if (mlKitModelDownloadRunning) return
        mlKitMissingModelsPrompt = null
        mlKitModelPromptDismissedPair = pair
        mlKitModelDownloadRunning = true
        mlKitModelDownloadMessage = null
        scope.launch {
            val result = runCatching {
                viewModel.downloadMlKitLanguagePair(pair.first, pair.second)
            }
            mlKitModelDownloadRunning = false
            if (translatorEngine != TranslatorEngine.GOOGLE_ML_KIT ||
                (sourceLang to targetLang) != pair
            ) {
                return@launch
            }
            if (result.isSuccess) {
                mlKitModelStatePair = pair
                mlKitModelsReady = true
                mlKitModelDownloadMessage = null
                mlKitDownloadedLanguageModels = runCatching {
                    viewModel.getDownloadedMlKitLanguageModels()
                }.getOrDefault(mlKitDownloadedLanguageModels)
            } else {
                val error = checkNotNull(result.exceptionOrNull())
                mlKitModelsReady = false
                mlKitModelDownloadMessage = context.getString(
                    R.string.settings_mlkit_model_download_failed,
                    error.message ?: error.javaClass.simpleName,
                )
            }
        }
    }

    */
    fun swapSelectedLanguages() {
        val swapped = swappedTranslationLanguagePair(sourceLang, targetLang) ?: return
        sourceLang = swapped.first
        targetLang = swapped.second
    }

    fun applyPresetSettingsToUi(s: Settings) {
        baseUrl = s.baseUrl
        apiKey = s.apiKey
        model = s.model
        anthropicBaseUrl = s.anthropicBaseUrl
        anthropicApiKey = s.anthropicApiKey
        anthropicModel = s.anthropicModel
        prompt = s.promptTemplate
        sourceLang = s.sourceLang
        targetLang = s.targetLang
        dictionaryPrompt = s.dictionaryPrompt
        translatorEngine = s.translatorEngine
        remotePcBaseUrl = s.remotePcBaseUrl
        remotePcApiKey = s.remotePcApiKey
        remotePcSessionId = s.remotePcSessionId
        remotePcImageQuality = s.remotePcImageQuality.toString()
        foregroundAppDetectionMode = s.foregroundAppDetectionMode
        sendAppNameToTranslator = s.sendAppNameToTranslator
        deeplKey = s.deeplApiKey
        deeplCustomToken = s.deeplCustomToken
        youdaoAppKey = s.youdaoAppKey
        youdaoAppSecret = s.youdaoAppSecret
        volcAk = s.volcAccessKeyId
        volcSk = s.volcSecretAccessKey
        volcRegion = s.volcRegion
        baiduFanyiAppId = s.baiduFanyiAppId
        baiduFanyiSecret = s.baiduFanyiSecretKey
        textSize = s.overlayTextSizeSp.toFloat()
        overlayTextStyle = s.overlayTextStyle.normalized()
        alpha = s.overlayAlpha
        overlayFontFileName = s.overlayFontFileName
        overlayFontDisplayName = s.overlayFontDisplayName
        overlayFontEntries = OverlayFontPolicy.upsertImportedFont(
            s.overlayFonts,
            s.overlayFontFileName,
            s.overlayFontDisplayName,
        )
        loopInterval = s.captureLoopIntervalMs.toString()
        loopTriggerMode = s.loopTriggerMode
        loopTextStableDurationMs = s.loopTextStableDurationMs
        loopSkipSimilarFrames = s.loopSkipSimilarFrames
        loopFrameSimilarityThreshold = s.loopFrameSimilarityThreshold
        loopTextRegionMode = s.loopTextRegionMode
        loopTranslateRegionOnly = s.loopTranslateRegionOnly
        developerOptionsEnabled = s.developerOptionsEnabled
        disableTranslationCache = s.disableTranslationCache
        batchCumulativeCompletionTimeEnabled = s.batchCumulativeCompletionTimeEnabled
        streaming = s.streamingTranslate
        retryEmptyTranslation = s.retryEmptyTranslation
        renderMode = s.renderMode
        translationBlockInteractionMode = s.translationBlockInteractionMode
        floatingWindowContentMode = s.floatingWindowContentMode
        floatingWindowLocked = s.floatingWindowLocked
        placement = s.overlayPlacement
        overlayStyleMode = s.overlayStyleMode
        overlayTheme = s.overlayTheme
        customBg = s.customBgColor
        customFg = s.customFgColor
        customBorder = s.customBorderColor
        customBorderW = s.customBorderWidth.toFloat()
        customBorderStyle = s.customBorderStyle
        offsetX = s.overlayOffsetX.toFloat()
        offsetY = s.overlayOffsetY.toFloat()
        allowWrap = s.overlayAllowWrap
        avoidCollision = s.overlayAvoidCollision
        deeplPro = s.deeplPro
        deeplProtocol = s.deeplProtocol
        deeplBaseUrl = s.deeplBaseUrl
        deeplBearerAuth = s.deeplBearerAuth
        llmMirrorChoice = s.localLlmMirror
        llmMirror = s.localLlmMirrorUrl
        a11yVolume = s.a11yVolumeTrigger
        floatingSize = s.floatingButtonSizeDp.toFloat()
        floatingSnapEdge = s.floatingButtonSnapToEdge
        floatingAutoDock = s.floatingButtonAutoDock
        floatingDockInset = s.floatingButtonDockInsetDp.toFloat()
        menuOrder = s.floatingMenuItemOrder
        arcMenuPageSize = s.arcMenuPageSize.toFloat()
        currentSkill = s.floatingButtonSkill
        apiTimeoutSec = s.apiTimeoutSeconds.toFloat()
        mergeAdjacent = s.mergeAdjacentBlocks
        mergeStrength = s.mergeStrength
        crossLineContextTranslationEnabled = !s.disableCrossLineContextTranslation
        resolveTranslationOutputSettings(
            s.translationOutputFollowRecognition,
            s.translationOutputLayout,
            s.translationOutputDirection,
        ).let { output ->
            translationOutputFollowRecognition = output.followRecognition
            translationOutputLayout = output.layout
            translationOutputDirection = output.direction
        }
        localLlmContextSize = s.localLlmContextSize
        localLlmMaxNewTokens = s.localLlmMaxNewTokens
        translationPresets = s.translationPresets
        activeTranslationPresetId = s.activeTranslationPresetId
        pinnedLanguages = s.pinnedLanguages
        cleartextHostsText = s.cleartextAllowedHosts.joinToString("\n")
    }
    fun presetDisplayNameForMessage(preset: TranslationPreset): String = preset.name

    // —— 搜索：顶部输入 → 下拉匹配项 → 点击 animateScrollTo 到对应 section 顶部 ——
    var settingsViewportTopInWindow by remember { mutableStateOf(Float.NaN) }
    var overlayPreviewTopInWindow by remember { mutableStateOf(Float.NaN) }
    var overlayPreviewHeightPx by remember { mutableStateOf(0) }
    var overlaySectionBottomInWindow by remember { mutableStateOf(Float.NaN) }
    val overlayPreviewSticky by remember {
        derivedStateOf {
            StickyOverlayPreviewPolicy.shouldStick(
                previewTopInWindow = overlayPreviewTopInWindow,
                sectionBottomInWindow = overlaySectionBottomInWindow,
                viewportTopInWindow = settingsViewportTopInWindow,
                previewHeightPx = overlayPreviewHeightPx,
            )
        }
    }
    val overlayFontImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val wasSystemDefaultOnly = shouldShowOverlayFontDeleteTipBeforeImport(
                currentFileName = overlayFontFileName,
                fonts = overlayFontEntries
            )
            when (val result = withContext(Dispatchers.IO) { viewModel.importOverlayFont(uri) }) {
                is OverlayFontImportResult.Success -> {
                    overlayFontFileName = result.fileName
                    overlayFontDisplayName = result.displayName
                    overlayFontEntries = OverlayFontPolicy.upsertImportedFont(
                        overlayFontEntries,
                        result.fileName,
                        result.displayName
                    )
                    overlayFontTypeface = withContext(Dispatchers.IO) {
                        viewModel.overlayTypefaceFor(result.fileName)
                    }
                    overlayFontMessage = context.getString(R.string.settings_overlay_font_import_success)
                    overlayFontMessageIsError = false
                    if (wasSystemDefaultOnly) {
                        showOverlayFontDeleteTip = true
                    }
                }
                is OverlayFontImportResult.Failure -> {
                    overlayFontMessage = overlayFontImportErrorMessage(context, result.error)
                    overlayFontMessageIsError = true
                }
            }
        }
    }

    val presetExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(SettingsBundleTransfer.MIME_TYPE)
    ) { uri ->
        val settingsToExport = pendingSettingsExport
        pendingSettingsExport = null
        if (uri == null || settingsToExport == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching { viewModel.exportSettingsBundle(uri, settingsToExport) }.onSuccess { result ->
                presetMessage = context.getString(
                    R.string.settings_bundle_exported_format,
                    result.presetCount,
                    result.fontCount,
                )
            }.onFailure { error ->
                presetMessage = context.getString(
                    R.string.settings_bundle_export_failed_format,
                    error.message ?: error.javaClass.simpleName
                )
            }
        }
    }
    val presetImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val preview = runCatching { viewModel.previewSettingsBundle(uri) }.getOrElse { error ->
                presetMessage = context.getString(
                    R.string.settings_bundle_import_failed_format,
                    error.message ?: error.javaClass.simpleName
                )
                return@launch
            }
            val plan = TranslationPresetTransfer.planImport(translationPresets, preview.presets)
            if (preview.legacyPresetOnly && plan.importedCount == 0) {
                presetMessage = context.getString(R.string.settings_translation_preset_import_empty)
            } else {
                pendingPresetImportPlan = plan
                pendingSettingsImportUri = uri
                pendingSettingsImportPreview = preview
            }
        }
    }

    val importPreview = pendingSettingsImportPreview
    val importUri = pendingSettingsImportUri
    pendingPresetImportPlan?.takeIf { importPreview != null && importUri != null }?.let { plan ->
        val overwritten = plan.overwrittenNames.joinToString(", ").ifBlank {
            stringResource(R.string.settings_translation_preset_import_none)
        }
        CatalystAlertDialog(
            onDismissRequest = {
                pendingPresetImportPlan = null
                pendingSettingsImportPreview = null
                pendingSettingsImportUri = null
            },
            title = {
                Text(
                    stringResource(
                        if (importPreview!!.legacyPresetOnly) {
                            R.string.settings_translation_preset_import_confirm_title
                        } else {
                            R.string.settings_bundle_import_confirm_title
                        }
                    )
                )
            },
            text = {
                Text(
                    if (importPreview!!.legacyPresetOnly) {
                        stringResource(
                            R.string.settings_translation_preset_import_confirm_message,
                            plan.importedCount,
                            overwritten,
                        )
                    } else {
                        stringResource(
                            R.string.settings_bundle_import_confirm_message,
                            plan.importedCount,
                            overwritten,
                            importPreview.fonts.size,
                            importPreview.skippedSettingFields.size,
                            importPreview.protectedLocalFieldCount,
                        )
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingPresetImportPlan = null
                    pendingSettingsImportPreview = null
                    pendingSettingsImportUri = null
                    scope.launch {
                        runCatching { viewModel.importSettingsBundle(importUri!!) }
                            .onSuccess { result ->
                                androidx.compose.runtime.snapshots.Snapshot.withMutableSnapshot {
                                    applyPresetSettingsToUi(result.settings)
                                    initialSettings = result.settings
                                }
                                refreshPresetModelReadiness(result.settings.translationPresets)
                                presetMessage = context.getString(
                                    R.string.settings_bundle_imported_format,
                                    result.importedPresetCount,
                                    result.overwrittenPresetNames.size,
                                    result.importedFontCount,
                                    result.skippedSettingFieldCount,
                                )
                            }
                            .onFailure { error ->
                                presetMessage = context.getString(
                                    R.string.settings_bundle_import_failed_format,
                                    error.message ?: error.javaClass.simpleName
                                )
                            }
                    }
                }) {
                    Text(stringResource(R.string.settings_translation_preset_import))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingPresetImportPlan = null
                    pendingSettingsImportPreview = null
                    pendingSettingsImportUri = null
                }) {
                    Text(stringResource(R.string.settings_model_delete_confirm_no))
                }
            }
        )
    }

    LaunchedEffect(showOverlayFontDeleteTip) {
        if (showOverlayFontDeleteTip) {
            for (remaining in 3 downTo 1) {
                overlayFontDeleteTipCountdown = remaining
                delay(1000L)
            }
            overlayFontDeleteTipCountdown = 0
        }
    }

    LaunchedEffect(overlayFontFileName) {
        overlayFontTypeface = withContext(Dispatchers.IO) {
            viewModel.overlayTypefaceFor(overlayFontFileName)
        }
    }

    var searchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }
    val searchTargetRegistry = remember { SettingsSearchTargetRegistry() }

    // 从完整持久化快照起步，再覆盖本页尚未保存的草稿值。不能从 Settings() 默认值起步，否则
    // 导出会把预设、固定语言、悬浮窗状态和本地 LLM 参数等非当前表单字段静默重置。
    // 类型转换跟 doSave 保持一致（textSize.toInt() / loopInterval.toLongOrNull() 等）。
    fun buildSnapshot(): Settings = (initialSettings ?: Settings()).copy(
        baseUrl = baseUrl,
        apiKey = apiKey,
        model = model,
        anthropicBaseUrl = anthropicBaseUrl,
        anthropicApiKey = anthropicApiKey,
        anthropicModel = anthropicModel,
        sourceLang = sourceLang,
        targetLang = targetLang,
        promptTemplate = prompt,
        captureLoopIntervalMs = loopInterval.toLongOrNull() ?: 2000L,
        loopTriggerMode = loopTriggerMode,
        loopTextStableDurationMs = loopTextStableDurationMs,
        loopSkipSimilarFrames = loopSkipSimilarFrames,
        loopFrameSimilarityThreshold = loopFrameSimilarityThreshold,
        loopTextRegionMode = loopTextRegionMode,
        loopTranslateRegionOnly = loopTranslateRegionOnly,
        developerOptionsEnabled = developerOptionsEnabled,
        disableTranslationCache = disableTranslationCache,
        batchCumulativeCompletionTimeEnabled = batchCumulativeCompletionTimeEnabled,
        overlayTextSizeSp = textSize.toInt(),
        overlayTextStyle = overlayTextStyle.normalized(),
        overlayAlpha = alpha,
        overlayFonts = overlayFontEntries,
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
        customBorderWidth = customBorderW.toInt(),
        overlayOffsetX = offsetX.toInt(),
        overlayOffsetY = offsetY.toInt(),
        a11yVolumeTrigger = a11yVolume,
        translatorEngine = effectiveTranslatorEngine(),
        remotePcBaseUrl = remotePcBaseUrl,
        remotePcApiKey = remotePcApiKey,
        remotePcSessionId = remotePcSessionId,
        remotePcImageQuality = remotePcImageQuality.toIntOrNull()?.coerceIn(50, 100) ?: 85,
        deeplApiKey = deeplKey,
        deeplPro = deeplPro,
        deeplProtocol = deeplProtocol,
        deeplBaseUrl = deeplBaseUrl,
        deeplBearerAuth = deeplBearerAuth,
        deeplCustomToken = deeplCustomToken,
        youdaoAppKey = youdaoAppKey,
        youdaoAppSecret = youdaoAppSecret,
        volcAccessKeyId = volcAk,
        volcSecretAccessKey = volcSk,
        volcRegion = volcRegion,
        baiduFanyiAppId = baiduFanyiAppId,
        baiduFanyiSecretKey = baiduFanyiSecret,
        floatingButtonSizeDp = floatingSize.toInt(),
        floatingButtonSnapToEdge = floatingSnapEdge,
        floatingButtonAutoDock = floatingAutoDock,
        floatingButtonDockInsetDp = floatingDockInset.toInt(),
        overlayAllowWrap = allowWrap,
        overlayAvoidCollision = avoidCollision,
        apiTimeoutSeconds = apiTimeoutSec.toInt(),
        mergeAdjacentBlocks = mergeAdjacent,
        mergeStrength = mergeStrength,
        disableCrossLineContextTranslation = !crossLineContextTranslationEnabled,
        cleartextAllowedHosts = parseCleartextHosts(cleartextHostsText)
    )

    fun buildTranslationPresetSnapshot(): Settings = buildSnapshot().copy(
        customBorderStyle = customBorderStyle,
        overlayFontFileName = overlayFontFileName,
        overlayFontDisplayName = overlayFontDisplayName,
        dictionaryPrompt = dictionaryPrompt,
        translationOutputFollowRecognition = translationOutputFollowRecognition,
        translationOutputLayout = translationOutputLayout,
        translationOutputDirection = translationOutputDirection,
        sendAppNameToTranslator = sendAppNameToTranslator,
    )

    fun buildSettingsTransferSnapshot(): Settings = buildTranslationPresetSnapshot().copy(
        foregroundAppDetectionMode = foregroundAppDetectionMode,
    )

    fun currentTranslationPresetHash(): String =
        TranslationPresetCatalog.hashForSettings(buildTranslationPresetSnapshot())

    fun currentMatchingTranslationPresetId(settingsHash: String = currentTranslationPresetHash()): String {
        val presets = TranslationPresetCatalog.all(translationPresets)
        val activeMatch = presets.firstOrNull {
            it.id == activeTranslationPresetId && TranslationPresetCatalog.matchesHash(it, settingsHash)
        }
        return activeMatch?.id
            ?: presets.firstOrNull { TranslationPresetCatalog.matchesHash(it, settingsHash) }?.id
            ?: ""
    }

    suspend fun downloadModelsForPreset(
        preset: TranslationPreset,
        issues: List<TranslationPresetModelIssue>,
    ) {
        val llmKinds = issues.mapNotNull { it.llmModelKind }.distinct()
        val specs = llmKinds.map(ModelDownloadSpec::llm)
        if (specs.isEmpty()) return
        llmDownloading = llmKinds.isNotEmpty()
        try {
            if (llmKinds.isNotEmpty()) viewModel.saveLlmMirror(llmMirrorChoice, llmMirror)
            viewModel.downloadModelsIndependently(
                specs = specs,
                onProgress = { msg -> presetMessage = msg },
                ownerPresetId = preset.id,
            )
            llmKinds.forEach { refreshLlmModelState(it) }
        } finally {
            llmDownloading = false
        }
        refreshPresetModelReadiness()
        presetMessage = context.getString(
            R.string.settings_translation_preset_download_models_done_format,
            presetDisplayNameForMessage(preset),
        )
    }
    // derivedStateOf 让 lambda 在依赖 state 变化时才重新计算 equals
    val dirty by remember {
        derivedStateOf {
            val initial = initialSettings ?: return@derivedStateOf false
            initial != buildSnapshot()
        }
    }

    val doSave: suspend () -> Unit = {
        viewModel.save(
            baseUrl = baseUrl, apiKey = apiKey, model = model,
            anthropicBaseUrl = anthropicBaseUrl,
            anthropicApiKey = anthropicApiKey,
            anthropicModel = anthropicModel,
            targetLang = targetLang, sourceLang = sourceLang, prompt = prompt,
            textSize = textSize.toInt(), alpha = alpha,
            overlayTextStyle = overlayTextStyle,
            loopMs = loopInterval.toLongOrNull() ?: 2000L,
            loopTriggerMode = loopTriggerMode,
            loopTextStableDurationMs = loopTextStableDurationMs,
            loopSkipSimilarFrames = loopSkipSimilarFrames,
            loopFrameSimilarityThreshold = loopFrameSimilarityThreshold,
            loopTextRegionMode = loopTextRegionMode,
            loopTranslateRegionOnly = loopTranslateRegionOnly,
            developerOptionsEnabled = developerOptionsEnabled,
            disableTranslationCache = disableTranslationCache,
            batchCumulativeCompletionTimeEnabled = batchCumulativeCompletionTimeEnabled,
            streaming = streaming,
            retryEmptyTranslation = retryEmptyTranslation,
            renderMode = renderMode,
            translationBlockInteractionMode = translationBlockInteractionMode,
            placement = placement,
            overlayStyleMode = overlayStyleMode,
            overlayTheme = overlayTheme,
            customBg = customBg, customFg = customFg,
            customBorder = customBorder, customBorderW = customBorderW.toInt(),
            offsetX = offsetX.toInt(), offsetY = offsetY.toInt(),
            a11yVolume = a11yVolume,
            floatingButtonSizeDp = floatingSize.toInt(),
            floatingButtonSnapToEdge = floatingSnapEdge,
            floatingButtonAutoDock = floatingAutoDock,
            floatingButtonDockInsetDp = floatingDockInset.toInt(),
            allowWrap = allowWrap,
            avoidCollision = avoidCollision,
            apiTimeoutSeconds = apiTimeoutSec.toInt(),
            mergeAdjacentBlocks = mergeAdjacent,
            mergeStrength = mergeStrength,
            disableCrossLineContextTranslation = !crossLineContextTranslationEnabled,
            cleartextAllowedHosts = parseCleartextHosts(cleartextHostsText),
            translatorEngine = effectiveTranslatorEngine(),
            remotePcBaseUrl = remotePcBaseUrl,
            remotePcApiKey = remotePcApiKey,
            remotePcSessionId = remotePcSessionId,
            remotePcImageQuality = remotePcImageQuality.toIntOrNull() ?: 85,
            deeplKey = deeplKey,
            deeplPro = deeplPro,
            deeplProtocol = deeplProtocol,
            deeplBaseUrl = deeplBaseUrl,
            deeplBearerAuth = deeplBearerAuth,
            deeplCustomToken = deeplCustomToken,
            youdaoAppKey = youdaoAppKey,
            youdaoAppSecret = youdaoAppSecret,
            volcAccessKeyId = volcAk,
            volcSecretAccessKey = volcSk,
            volcRegion = volcRegion,
            baiduFanyiAppId = baiduFanyiAppId,
            baiduFanyiSecretKey = baiduFanyiSecret,
            overlayFonts = overlayFontEntries,
            activeTranslationPresetId = currentMatchingTranslationPresetId()
        )
    }

    val modelDownloadBusy = backgroundModelDownloadActive || downloadingPresetId != null || llmDownloading
    val translationPresetSection: @Composable () -> Unit = {
        SectionCard(
            title = stringResource(R.string.settings_section_translation_presets),
            helpText = stringResource(R.string.settings_translation_preset_desc)
        ) {
            val presetSnapshot = buildTranslationPresetSnapshot()
            val presetHash = TranslationPresetCatalog.hashForSettings(presetSnapshot)
            val matchingPresetId = currentMatchingTranslationPresetId(presetHash)
            val unsavedPresetName = stringResource(R.string.settings_translation_preset_unsaved_name)
            val unsavedPreset = if (initialSettings != null && matchingPresetId.isBlank()) {
                TranslationPresetCatalog.fromSettings(
                    id = TranslationPresetCatalog.UNSAVED_DRAFT_ID,
                    name = unsavedPresetName,
                    shortName = unsavedPresetName.take(8),
                    settings = presetSnapshot
                )
            } else {
                null
            }
            TranslationPresetSection(
                customPresets = translationPresets,
                activeId = matchingPresetId,
                unsavedPreset = unsavedPreset,
                message = presetMessage,
                localLlmDeviceCapable = localLlmDeviceCapable,
                llmModelReady = { kind -> presetLlmModelReady[kind] == true },
                modelDownloading = modelDownloadBusy,
                downloadingPresetId = downloadingPresetId ?: backgroundModelDownloadOwnerPresetId,
                onExport = {
                    pendingSettingsExport = buildSettingsTransferSnapshot()
                    presetExportLauncher.launch(SettingsBundleTransfer.DEFAULT_FILE_NAME)
                },
                onImport = {
                    presetImportLauncher.launch(
                        arrayOf(
                            SettingsBundleTransfer.MIME_TYPE,
                            "application/json",
                            "application/octet-stream",
                            "text/plain",
                        )
                    )
                },
                onSaveUnsaved = { preset ->
                    scope.launch {
                        val saved = viewModel.saveTranslationPreset(preset)
                        translationPresets = TranslationPresetCatalog.upsertCustom(translationPresets, saved)
                        activeTranslationPresetId = saved.id
                        presetMessage = context.getString(
                            R.string.settings_translation_preset_saved_format,
                            saved.name
                        )
                    }
                },
                onApply = { preset ->
                    scope.launch {
                        val applied = viewModel.applyTranslationPreset(preset.id) ?: return@launch
                        applyPresetSettingsToUi(applied)
                        activeTranslationPresetId = preset.id
                        initialSettings = buildSnapshot()
                        presetMessage = context.getString(
                            R.string.settings_translation_preset_applied_format,
                            presetDisplayNameForMessage(preset)
                        )
                    }
                },
                onDownloadModels = { preset, issues ->
                    if (translationPresetDownloadRequiresAndroidUpgrade(issues)) {
                        showUnsupportedPresetDownloadDialog = true
                    } else if (modelDownloadBusy) {
                        presetMessage = context.getString(R.string.settings_translation_preset_other_download_busy)
                    } else {
                        val downloadModelLabel = translationPresetDownloadModelLabel(context, issues)
                        requestModelDownload(downloadModelLabel) {
                            scope.launch {
                                if (downloadingPresetId != null) {
                                    presetMessage = context.getString(
                                        R.string.settings_translation_preset_other_download_busy
                                    )
                                    return@launch
                                }
                                downloadingPresetId = preset.id
                                try {
                                    downloadModelsForPreset(preset, issues)
                                } catch (t: Throwable) {
                                    presetMessage = context.getString(
                                        R.string.settings_translation_preset_download_models_failed_format,
                                        t.message ?: t.javaClass.simpleName
                                    )
                                    refreshPresetModelReadiness()
                                } finally {
                                    downloadingPresetId = null
                                }
                            }
                        }
                    }
                },
                onDelete = { preset ->
                    scope.launch {
                        viewModel.deleteTranslationPreset(preset.id)
                        translationPresets = translationPresets.filterNot { it.id == preset.id }
                        if (activeTranslationPresetId == preset.id) activeTranslationPresetId = ""
                    }
                }
            )
        }
    }

    val textOrientationSection: @Composable () -> Unit = {
        SectionCard(
            title = stringResource(R.string.settings_text_orientation_section_title),
        ) {
            HorizontalDivider()
            SettingsSearchTarget(searchTargetRegistry, R.string.settings_translation_output_follow_title) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SwitchRow(
                stringResource(R.string.settings_translation_output_follow_title),
                translationOutputFollowRecognition,
                helpText = stringResource(R.string.settings_translation_output_follow_summary),
            ) { enabled ->
                translationOutputFollowRecognition = enabled
                scope.launch { viewModel.saveTranslationOutputFollowRecognition(enabled) }
            }
            if (!translationOutputFollowRecognition) {
            SettingsSearchTarget(searchTargetRegistry, R.string.settings_translation_output_layout_label) {
            Text(
                stringResource(R.string.settings_translation_output_layout_label),
                style = MaterialTheme.typography.labelLarge,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                com.gameocr.app.data.TranslationOutputLayout.entries
                    .filterNot { it == com.gameocr.app.data.TranslationOutputLayout.FOLLOW_RECOGNITION }
                    .forEach { layout ->
                    val label = when (layout) {
                        com.gameocr.app.data.TranslationOutputLayout.HORIZONTAL ->
                            stringResource(R.string.settings_translation_output_horizontal)
                        com.gameocr.app.data.TranslationOutputLayout.VERTICAL ->
                            stringResource(R.string.settings_translation_output_vertical)
                        com.gameocr.app.data.TranslationOutputLayout.FOLLOW_RECOGNITION -> return@forEach
                    }
                    EngineChip(translationOutputLayout, layout, label) {
                        translationOutputLayout = it
                        scope.launch { viewModel.saveTranslationOutputLayout(it) }
                    }
                }
            }
            Text(
                stringResource(R.string.settings_translation_output_direction_label),
                style = MaterialTheme.typography.labelLarge,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                com.gameocr.app.data.TranslationOutputDirection.entries
                    .filterNot { it == com.gameocr.app.data.TranslationOutputDirection.FOLLOW_RECOGNITION }
                    .forEach { direction ->
                    val label = when (direction) {
                        com.gameocr.app.data.TranslationOutputDirection.LEFT_TO_RIGHT ->
                            stringResource(R.string.settings_translation_output_ltr)
                        com.gameocr.app.data.TranslationOutputDirection.RIGHT_TO_LEFT ->
                            stringResource(R.string.settings_translation_output_rtl)
                        com.gameocr.app.data.TranslationOutputDirection.FOLLOW_RECOGNITION -> return@forEach
                    }
                    EngineChip(translationOutputDirection, direction, label) {
                        translationOutputDirection = it
                        scope.launch { viewModel.saveTranslationOutputDirection(it) }
                    }
                }
            }
            }
            }
            }
            }
        }
    }

    val tryBack: () -> Unit = {
        if (dirty) showUnsavedDialog = true else onBack()
    }

    BackHandler { tryBack() }

    val currentTranslationPresetHash = currentTranslationPresetHash()
    val matchingTranslationPresetId = currentMatchingTranslationPresetId(currentTranslationPresetHash)
    // TTS UI removed.
    LaunchedEffect(
        initialSettings,
        activeTranslationPresetId,
        translationPresets,
        currentTranslationPresetHash
    ) {
        if (initialSettings == null) return@LaunchedEffect
        if (activeTranslationPresetId != matchingTranslationPresetId) {
            activeTranslationPresetId = matchingTranslationPresetId
            viewModel.setActiveTranslationPreset(matchingTranslationPresetId)
        }
    }

    if (showUnsavedDialog) {
        CatalystAlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text(stringResource(R.string.settings_unsaved_title)) },
            text = { Text(stringResource(R.string.settings_unsaved_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    showUnsavedDialog = false
                    scope.launch { doSave(); onBack() }
                }) { Text(stringResource(R.string.settings_unsaved_save)) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showUnsavedDialog = false
                        onBack()
                    }) { Text(stringResource(R.string.settings_unsaved_discard)) }
                    TextButton(onClick = { showUnsavedDialog = false }) {
                        Text(stringResource(R.string.settings_unsaved_keep_editing))
                    }
                }
            }
        )
    }

    if (showUnsupportedPresetDownloadDialog) {
        CatalystAlertDialog(
            onDismissRequest = { showUnsupportedPresetDownloadDialog = false },
            title = {
                Text(stringResource(R.string.settings_translation_preset_android_unsupported_title))
            },
            text = {
                Text(
                    stringResource(
                        R.string.settings_translation_preset_android_unsupported_message,
                        Build.VERSION.RELEASE,
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { showUnsupportedPresetDownloadDialog = false }) {
                    Text(stringResource(R.string.settings_translation_preset_android_unsupported_confirm))
                }
            },
        )
    }

    // 源语言↔OCR 联动：检查能否识别当前源语言；不能则按"用户刚动的是哪一边"决定推荐方向。
    pendingModelDownload?.let { pending ->
        CatalystAlertDialog(
            onDismissRequest = { pendingModelDownload = null },
            title = { Text(stringResource(R.string.settings_model_download_network_warning_title)) },
            text = {
                Text(
                    stringResource(
                        modelDownloadNetworkWarningMessageRes(pending.warning),
                        pending.modelLabel
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val confirmed = pending.onConfirmed
                    pendingModelDownload = null
                    confirmed()
                }) {
                    Text(stringResource(R.string.settings_model_download_network_warning_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingModelDownload = null }) {
                    Text(stringResource(R.string.settings_model_download_network_warning_cancel))
                }
            }
        )
    }

    pendingLanguageSwapOrigin?.let { origin ->
        val swapAvailable =
            swappedTranslationLanguagePair(sourceLang, targetLang) != null
        val messageRes = when {
            !swapAvailable -> R.string.settings_language_conflict_cannot_swap_message
            origin == LanguageSwapRequestOrigin.SOURCE_PICKER ->
                R.string.settings_source_language_conflict_message
            else -> R.string.settings_target_language_conflict_message
        }
        CatalystAlertDialog(
            onDismissRequest = { pendingLanguageSwapOrigin = null },
            title = {
                Text(stringResource(R.string.settings_language_conflict_title))
            },
            text = { Text(stringResource(messageRes)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingLanguageSwapOrigin = null
                        if (swapAvailable) swapSelectedLanguages()
                    }
                ) {
                    Text(
                        stringResource(
                            if (swapAvailable) {
                                R.string.settings_language_swap_confirm
                            } else {
                                R.string.settings_language_conflict_acknowledge
                            }
                        )
                    )
                }
            },
            dismissButton = if (swapAvailable) {
                {
                    TextButton(onClick = { pendingLanguageSwapOrigin = null }) {
                        Text(stringResource(R.string.settings_language_swap_cancel))
                    }
                }
            } else {
                null
            },
        )
    }

    if (showOverlayFontDeleteTip) {
        CatalystAlertDialog(
            onDismissRequest = {
                if (overlayFontDeleteTipCountdown == 0) showOverlayFontDeleteTip = false
            },
            title = { Text(stringResource(R.string.settings_overlay_font_delete_tip_title)) },
            text = { Text(stringResource(R.string.settings_overlay_font_delete_tip_message)) },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        enabled = overlayFontDeleteTipCountdown == 0,
                        onClick = { showOverlayFontDeleteTip = false }
                    ) {
                        Text(
                            overlayFontDeleteTipAckLabel(
                                baseLabel = stringResource(R.string.settings_overlay_font_delete_tip_ack),
                                countdown = overlayFontDeleteTipCountdown
                            )
                        )
                    }
                }
            }
        )
    }

    pendingOverlayFontDelete?.let { font ->
        CatalystAlertDialog(
            onDismissRequest = { pendingOverlayFontDelete = null },
            title = { Text(stringResource(R.string.settings_overlay_font_delete_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.settings_overlay_font_delete_confirm_message,
                        font.displayName
                    )
                )
            },
            confirmButton = {
                DestructiveTextButton(
                    label = stringResource(R.string.settings_model_delete_confirm_yes),
                    onClick = {
                        pendingOverlayFontDelete = null
                        scope.launch {
                            val deleted = withContext(Dispatchers.IO) {
                                viewModel.deleteOverlayFont(font.fileName)
                            }
                            if (deleted) {
                                overlayFontEntries = OverlayFontPolicy.removeImportedFont(
                                    overlayFontEntries,
                                    font.fileName
                                )
                                if (overlayFontFileName == font.fileName) {
                                    overlayFontFileName = ""
                                    overlayFontDisplayName = ""
                                    overlayFontTypeface = null
                                }
                                overlayFontMessage = context.getString(
                                    R.string.settings_overlay_font_delete_success_format,
                                    font.displayName
                                )
                                overlayFontMessageIsError = false
                            } else {
                                overlayFontMessage = context.getString(R.string.settings_overlay_font_error_invalid)
                                overlayFontMessageIsError = true
                            }
                        }
                    },
                )
            },
            dismissButton = {
                TextButton(onClick = { pendingOverlayFontDelete = null }) {
                    Text(stringResource(R.string.settings_model_delete_confirm_no))
                }
            }
        )
    }

    /*
    mlKitMissingModelsPrompt?.let { prompt ->
        val sourceName = Languages.nameOf(context, prompt.pair.first)
        val targetName = Languages.nameOf(context, prompt.pair.second)
        val missingNames = prompt.missingLanguages.joinToString(", ") { languageTag ->
            Languages.nameOf(context, languageTag)
        }
        CatalystAlertDialog(
            onDismissRequest = {
                mlKitModelPromptDismissedPair = prompt.pair
                mlKitMissingModelsPrompt = null
            },
            title = { Text(stringResource(R.string.mlkit_missing_models_dialog_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.mlkit_missing_models_dialog_message,
                        sourceName,
                        targetName,
                        missingNames,
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { startMlKitModelDownload(prompt.pair) }) {
                    Text(stringResource(R.string.mlkit_missing_models_dialog_download))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    mlKitModelPromptDismissedPair = prompt.pair
                    mlKitMissingModelsPrompt = null
                    translatorEngine = TranslatorEngine.OPENAI
                }) {
                    Text(stringResource(R.string.mlkit_missing_models_dialog_switch_llm))
                }
            },
        )
    }

    }
    */
    if (showSakuraFallbackDialog) {
        val sourceName = com.gameocr.app.data.Languages.nameOf(context, sourceLang)
        val targetName = com.gameocr.app.data.Languages.nameOf(context, targetLang)
        val sakuraLanguageIssue = sakuraLanguageIssue(sourceLang, targetLang)
        val sakuraIssueMessage = when {
            sakuraLanguageIssue.sourceUnsupported && sakuraLanguageIssue.targetUnsupported -> stringResource(
                R.string.sakura_fallback_dialog_issue_pair,
                sourceName,
                targetName
            )
            sakuraLanguageIssue.sourceUnsupported -> stringResource(
                R.string.sakura_fallback_dialog_issue_source,
                sourceName
            )
            sakuraLanguageIssue.targetUnsupported -> stringResource(
                R.string.sakura_fallback_dialog_issue_target,
                targetName
            )
            else -> stringResource(
                R.string.sakura_fallback_dialog_issue_pair,
                sourceName,
                targetName
            )
        }
        val supportedLanguageActionLabel = when {
            sakuraLanguageIssue.sourceUnsupported && sakuraLanguageIssue.targetUnsupported ->
                R.string.sakura_fallback_dialog_set_supported_pair
            sakuraLanguageIssue.targetUnsupported -> R.string.sakura_fallback_dialog_set_target_zh_cn
            else -> R.string.sakura_fallback_dialog_set_japanese
        }
        val missingFallbackFields = missingOpenAiFallbackFields(baseUrl, apiKey, model)
        val fallbackBaseUrlLabel = stringResource(R.string.settings_base_url)
        val fallbackApiKeyLabel = stringResource(R.string.settings_api_key)
        val fallbackModelLabel = stringResource(R.string.settings_model)
        val missingFallbackLabels = missingFallbackFields.joinToString(", ") { field ->
            when (field) {
                OpenAiFallbackField.BASE_URL -> fallbackBaseUrlLabel
                OpenAiFallbackField.API_KEY -> fallbackApiKeyLabel
                OpenAiFallbackField.MODEL -> fallbackModelLabel
            }
        }
        CatalystAlertDialog(
            onDismissRequest = {
                showSakuraFallbackDialog = false
                if (missingFallbackFields.isNotEmpty()) {
                    translatorEngine = TranslatorEngine.OPENAI
                }
            },
            title = { Text(stringResource(R.string.sakura_fallback_dialog_title)) },
            text = {
                Text(
                    if (missingFallbackFields.isEmpty()) {
                        stringResource(R.string.sakura_fallback_dialog_message_configured, sakuraIssueMessage)
                    } else {
                        stringResource(
                            R.string.sakura_fallback_dialog_message_missing,
                            sakuraIssueMessage,
                            missingFallbackLabels
                        )
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showSakuraFallbackDialog = false
                    if (missingFallbackFields.isEmpty()) {
                        translatorEngine = TranslatorEngine.LOCAL_SAKURA
                    } else {
                        translatorEngine = TranslatorEngine.OPENAI
                    }
                }) {
                    Text(
                        stringResource(
                            if (missingFallbackFields.isEmpty()) {
                                R.string.sakura_fallback_dialog_use_fallback
                            } else {
                                R.string.sakura_fallback_dialog_configure_fallback
                            }
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSakuraFallbackDialog = false
                    if (sakuraLanguageIssue.sourceUnsupported) {
                        sourceLang = "ja"
                    }
                    if (sakuraLanguageIssue.targetUnsupported) {
                        targetLang = SAKURA_TARGET_LANG_ZH_CN
                    }
                    translatorEngine = TranslatorEngine.LOCAL_SAKURA
                }) {
                    Text(stringResource(supportedLanguageActionLabel))
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        val s = viewModel.load()
        // suspend 操作必须在 Snapshot 块外做完
        val migratedPrompt = viewModel.migrateDefaultPromptIfStale(context)
        // 关键性能：把 40+ state 写入封进同一个 mutable snapshot，原子 apply 后只触发
        // 一次 observer 通知，避免 Compose 在每个 state 变化时 schedule 一次 recomposition
        // / derivedStateOf 重算，进设置页那段"卡一下"主要来自这里。
        androidx.compose.runtime.snapshots.Snapshot.withMutableSnapshot {
            baseUrl = s.baseUrl
            apiKey = s.apiKey
            model = s.model
            anthropicBaseUrl = s.anthropicBaseUrl
            anthropicApiKey = s.anthropicApiKey
            anthropicModel = s.anthropicModel
            prompt = migratedPrompt
            targetLang = s.targetLang
            sourceLang = s.sourceLang
            translatorEngine = if (isLocalLlmEngine(s.translatorEngine) && !localLlmDeviceCapable) {
                TranslatorEngine.OPENAI
            } else {
                s.translatorEngine
            }
            // PC server fields are editable drafts just like the other Cloud LLM fields.
            // Restore them before the first save; otherwise their initial placeholder values
            // overwrite the address that is already persisted in DataStore.
            remotePcBaseUrl = s.remotePcBaseUrl
            remotePcApiKey = s.remotePcApiKey
            remotePcSessionId = s.remotePcSessionId
            remotePcImageQuality = s.remotePcImageQuality.toString()
            deeplKey = s.deeplApiKey
            youdaoAppKey = s.youdaoAppKey
            volcAk = s.volcAccessKeyId
            volcSk = s.volcSecretAccessKey
            volcRegion = s.volcRegion
            baiduFanyiAppId = s.baiduFanyiAppId
            baiduFanyiSecret = s.baiduFanyiSecretKey
            youdaoAppSecret = s.youdaoAppSecret
            deeplPro = s.deeplPro
            deeplProtocol = s.deeplProtocol
            deeplBaseUrl = s.deeplBaseUrl
            deeplBearerAuth = s.deeplBearerAuth
            deeplCustomToken = s.deeplCustomToken
            textSize = s.overlayTextSizeSp.toFloat()
            overlayTextStyle = s.overlayTextStyle.normalized()
            alpha = s.overlayAlpha
            overlayFontFileName = s.overlayFontFileName
            overlayFontDisplayName = s.overlayFontDisplayName
            overlayFontEntries = OverlayFontPolicy.upsertImportedFont(
                s.overlayFonts,
                s.overlayFontFileName,
                s.overlayFontDisplayName
            )
            loopInterval = s.captureLoopIntervalMs.toString()
            loopTriggerMode = s.loopTriggerMode
            loopTextStableDurationMs = s.loopTextStableDurationMs
            loopSkipSimilarFrames = s.loopSkipSimilarFrames
            loopFrameSimilarityThreshold = s.loopFrameSimilarityThreshold
            developerOptionsEnabled = s.developerOptionsEnabled
            disableTranslationCache = s.disableTranslationCache
            batchCumulativeCompletionTimeEnabled = s.batchCumulativeCompletionTimeEnabled
            streaming = s.streamingTranslate
            retryEmptyTranslation = s.retryEmptyTranslation
            renderMode = s.renderMode
            translationBlockInteractionMode = s.translationBlockInteractionMode
            floatingWindowContentMode = s.floatingWindowContentMode
            floatingWindowLocked = s.floatingWindowLocked
            customBorderStyle = s.customBorderStyle
            placement = s.overlayPlacement
            overlayStyleMode = s.overlayStyleMode
            overlayTheme = s.overlayTheme
            customBg = s.customBgColor
            customFg = s.customFgColor
            customBorder = s.customBorderColor
            customBorderW = s.customBorderWidth.toFloat()
            offsetX = s.overlayOffsetX.toFloat()
            offsetY = s.overlayOffsetY.toFloat()
            llmMirrorChoice = s.localLlmMirror
            llmMirror = s.localLlmMirrorUrl
            // 不阻塞主线程：file.exists() + file.length() 走 IO Dispatcher。先给占位
            // 文字，IO 完成后再覆盖；进设置的瞬间不卡顿。
            a11yVolume = s.a11yVolumeTrigger
            floatingSize = s.floatingButtonSizeDp.toFloat()
            floatingSnapEdge = s.floatingButtonSnapToEdge
            floatingAutoDock = s.floatingButtonAutoDock
            floatingDockInset = s.floatingButtonDockInsetDp.toFloat()
            menuOrder = s.floatingMenuItemOrder
            arcMenuPageSize = s.arcMenuPageSize.toFloat()
            currentSkill = s.floatingButtonSkill
            dictionaryPrompt = s.dictionaryPrompt
            translationPresets = s.translationPresets
            activeTranslationPresetId = s.activeTranslationPresetId
            pinnedLanguages = s.pinnedLanguages
            allowWrap = s.overlayAllowWrap
            avoidCollision = s.overlayAvoidCollision
            apiTimeoutSec = s.apiTimeoutSeconds.toFloat()
            mergeAdjacent = s.mergeAdjacentBlocks
            mergeStrength = s.mergeStrength
            crossLineContextTranslationEnabled = !s.disableCrossLineContextTranslation
            resolveTranslationOutputSettings(
                s.translationOutputFollowRecognition,
                s.translationOutputLayout,
                s.translationOutputDirection,
            ).let { output ->
                translationOutputFollowRecognition = output.followRecognition
                translationOutputLayout = output.layout
                translationOutputDirection = output.direction
            }
            foregroundAppDetectionMode = s.foregroundAppDetectionMode
            sendAppNameToTranslator = s.sendAppNameToTranslator
            cleartextHostsText = s.cleartextAllowedHosts.joinToString("\n")
            // Keep the complete repository value as the baseline. Rebuilding it while
            // initialSettings is still null would start from Settings defaults and lose fields
            // owned by services or other screens before the first export.
            initialSettings = s
        }
    }

    val settingsLoaded = initialSettings != null
    val modelDownloadStateKey = modelDownloadWorkInfos.map { it.id to it.state }
    val modelDownloadStageKey = unfinishedModelDownloads.map { info ->
        ModelDownloadSpec.decode(
            info.progress.getString(
                com.gameocr.app.download.ModelDownloadWorker.KEY_CURRENT_SPEC
            ).orEmpty()
        ) to info.progress.getInt(
            com.gameocr.app.download.ModelDownloadWorker.KEY_BATCH_INDEX,
            0,
        )
    }
    LaunchedEffect(settingsLoaded, modelDownloadStateKey, modelDownloadStageKey) {
        if (!settingsLoaded) return@LaunchedEffect
        localLlmModelKindFor(translatorEngine)?.let { refreshLlmModelState(it) }
        refreshPresetModelReadiness()
    }
    LaunchedEffect(settingsLoaded) {
        if (!settingsLoaded) return@LaunchedEffect
    }
    LaunchedEffect(settingsLoaded) {
        if (!settingsLoaded) return@LaunchedEffect
    }
    LaunchedEffect(settingsLoaded, translationPresets) {
        if (!settingsLoaded) return@LaunchedEffect
        refreshPresetModelReadiness()
    }

    val closeSearch: () -> Unit = {
        searchActive = false
        searchQuery = ""
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        if (searchActive) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text(stringResource(R.string.settings_search_placeholder)) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(searchFocusRequester),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.background,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                            LaunchedEffect(Unit) { searchFocusRequester.requestFocus() }
                        } else {
                            Text(stringResource(R.string.settings_title))
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = if (searchActive) closeSearch else tryBack) {
                            Icon(
                                if (searchActive) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(
                                    if (searchActive) R.string.settings_search_close else R.string.common_back
                                )
                            )
                        }
                    },
                    actions = {
                        if (!searchActive) {
                            IconButton(onClick = { searchActive = true }) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = stringResource(R.string.settings_search_btn)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.background
                    )
                )
                if (activeModelDownloads.isNotEmpty() || unresolvedModelDownloadFailure != null) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        activeModelDownloads.forEach { download ->
                            ModelDownloadProgressCard(
                                status = download.status
                                    .ifBlank { context.getString(R.string.model_download_waiting) },
                                downloaded = download.downloaded,
                                total = download.total,
                                onCancel = {
                                    viewModel.cancelModelDownload(download.id)
                                },
                            )
                        }
                        unresolvedModelDownloadFailure?.let { failure ->
                            ModelDownloadFailureCard(
                                failure = failure,
                                onRetry = {
                                    val modelLabel = failure.specs.joinToString(", ") {
                                        modelDownloadSpecDisplayName(context, it)
                                    }
                                    requestModelDownload(modelLabel) {
                                        scope.launch {
                                            try {
                                                viewModel.downloadModelsIndependently(
                                                    specs = failure.specs,
                                                    onProgress = {},
                                                    ownerPresetId = failure.ownerPresetId,
                                                )
                                            } catch (t: Throwable) {
                                                Timber.w(t, "Retry model download failed")
                                            }
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    // 防护：load 完成前 state 是默认占位值，此时保存会把空字符串 / 默认 enum
                    // 写入 DataStore，覆盖用户实际数据。LaunchedEffect 完成（~13ms）才把
                    // initialSettings 设值，那之后才允许保存。
                    if (initialSettings == null) return@ExtendedFloatingActionButton
                    scope.launch { doSave(); onBack() }
                },
                icon = { Icon(Icons.Default.Save, contentDescription = null) },
                text = { Text(stringResource(if (dirty) R.string.settings_save_btn else R.string.settings_saved_btn)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .onGloballyPositioned { coordinates ->
                    settingsViewportTopInWindow = coordinates.positionInWindow().y
                }
        ) {
            // 直接 inflate Column——不显示 spinner，避免"按下设置 → spinner → UI"那段空白卡顿感。
            // state 默认值（空字符串 / 默认 enum）会先短暂显示，LaunchedEffect 在 ~13ms 内 Snapshot
            // 原子更新所有 state 到实际保存值——肉眼几乎不察觉闪烁。代价：用户在 initialSettings
            // 还是 null 时点保存按钮会用默认值覆盖数据，所以下面 FAB 加了 enabled 防护。
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            // —— 应用语言 ——
            item(key = SectionKeys.APP_LANG) {
                SettingsSearchTarget(searchTargetRegistry, *SEARCH_TARGET_APP_LANGUAGE) {
                SectionCard(title = stringResource(R.string.settings_section_app_lang)) {
                    AppLanguageSelector()
                }
                }
            }

            // —— 主题模式 ——
            item(key = SectionKeys.THEME_MODE) {
                SettingsSearchTarget(searchTargetRegistry, *SEARCH_TARGET_THEME_MODE) {
                SectionCard(title = stringResource(R.string.settings_section_theme_mode)) {
                    ThemeModeSelector()
                }
                }
            }

            item(key = SectionKeys.PRESETS) {
                SettingsSearchTarget(searchTargetRegistry, *SEARCH_TARGET_PRESETS) {
                    translationPresetSection()
                }
            }

            // —— 翻译后端 ——
            item(key = SectionKeys.TRANSLATE) {
            SectionCard(title = stringResource(R.string.settings_section_translator)) {
                // LEGACY_COMPAT: retain provider configuration state until provider cleanup,
                // but do not expose retired engine selection or provider options.
                /*
                SettingsSearchTarget(searchTargetRegistry, *SEARCH_TARGET_TRANSLATOR_ENGINE) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.settings_label_translator_engine), style = MaterialTheme.typography.labelLarge)

                // All on-device options share one group; cloud engines remain split by API type.
                Text(
                    stringResource(R.string.settings_translator_group_local_llm),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val quickMlKitSources = mlKitRecentSourceLanguages(
                    stored = mlKitRecentSources,
                    selected = sourceLang.takeIf {
                        translatorEngine == TranslatorEngine.GOOGLE_ML_KIT
                    },
                )
                val downloadedMlKitPickerCodes = mlKitDownloadedPickerLanguageCodes(
                    mlKitDownloadedLanguageModels
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    quickMlKitSources.forEach { languageTag ->
                        val defaultOption = MlKitQuickSourceLanguage.fromLanguageTag(languageTag)
                        FilterChip(
                            selected = translatorEngine == TranslatorEngine.GOOGLE_ML_KIT &&
                                mlKitLanguageTagsMatch(languageTag, sourceLang),
                            onClick = { selectMlKitSourceLanguage(languageTag) },
                            label = {
                                Text(
                                    defaultOption?.let { stringResource(it.labelRes) }
                                        ?: Languages.nameOf(context, languageTag)
                                )
                            },
                        )
                    }
                    FilterChip(
                        selected = false,
                        onClick = {
                            showMlKitMoreLanguages = true
                            scope.launch {
                                mlKitDownloadedLanguageModels = runCatching {
                                    viewModel.getDownloadedMlKitLanguageModels()
                                }.getOrDefault(mlKitDownloadedLanguageModels)
                            }
                        },
                        label = { Text(stringResource(R.string.settings_on_device_translation_more)) },
                    )
                    EngineChip(
                        translatorEngine,
                        TranslatorEngine.LOCAL_SAKURA,
                        stringResource(R.string.settings_engine_local_sakura),
                        enabled = localLlmDeviceCapable
                    ) { selectTranslatorEngine(it) }
                    EngineChip(
                        translatorEngine,
                        TranslatorEngine.LOCAL_HY_MT2,
                        stringResource(R.string.settings_engine_local_hymt2),
                        enabled = localLlmDeviceCapable
                    ) { selectTranslatorEngine(it) }
                }
                if (showMlKitMoreLanguages) {
                    LanguagePickerSheet(
                        currentCode = sourceLang,
                        pinned = pinnedLanguages,
                        allowAuto = false,
                        allowedLanguageCodes = mlKitLanguagePickerCodes,
                        priorityCodes = downloadedMlKitPickerCodes,
                        badgedLanguageCodes = downloadedMlKitPickerCodes.toSet(),
                        badgeLabel = stringResource(R.string.settings_mlkit_model_downloaded_short),
                        unbadgedStatusLabel = stringResource(R.string.settings_mlkit_model_download_short),
                        disabledLanguageCodes = setOf(targetLang),
                        disabledStatusLabel = stringResource(R.string.lang_picker_already_target),
                        onDisabledSelect = {
                            pendingLanguageSwapOrigin = LanguageSwapRequestOrigin.SOURCE_PICKER
                            showMlKitMoreLanguages = false
                        },
                        onSelect = { languageTag ->
                            selectMlKitSourceLanguage(languageTag)
                            showMlKitMoreLanguages = false
                        },
                        onTogglePin = null,
                        onDismiss = { showMlKitMoreLanguages = false },
                    )
                }
                Text(
                    stringResource(R.string.settings_translator_group_cloud_llm),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    EngineChip(translatorEngine, TranslatorEngine.REMOTE_PC, stringResource(R.string.settings_engine_remote_pc)) { translatorEngine = it }
                    EngineChip(translatorEngine, TranslatorEngine.OPENAI, stringResource(R.string.settings_engine_openai_llm)) { translatorEngine = it }
                    EngineChip(translatorEngine, TranslatorEngine.ANTHROPIC, stringResource(R.string.settings_engine_anthropic_llm)) { translatorEngine = it }
                }
                Text(
                    stringResource(R.string.settings_translator_group_cloud),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    EngineChip(translatorEngine, TranslatorEngine.DEEPL, stringResource(R.string.settings_engine_deepl)) { translatorEngine = it }
                    EngineChip(translatorEngine, TranslatorEngine.GOOGLE, stringResource(R.string.settings_engine_google)) { translatorEngine = it }
                    EngineChip(translatorEngine, TranslatorEngine.VOLC, stringResource(R.string.settings_engine_volc)) { translatorEngine = it }
                    EngineChip(translatorEngine, TranslatorEngine.BAIDU_FANYI, stringResource(R.string.settings_engine_baidu_fanyi)) { translatorEngine = it }
                    EngineChip(translatorEngine, TranslatorEngine.TENCENT, stringResource(R.string.settings_engine_tencent)) { translatorEngine = it }
                    EngineChip(translatorEngine, TranslatorEngine.YOUDAO_PICTRANS, stringResource(R.string.settings_engine_youdao_pictrans)) { translatorEngine = it }
                }
                LaunchedEffect(translatorEngine) {
                    testMessage = null
                    testSuccess = false
                    fetchedModels = emptyList()
                    modelPickerExpanded = false
                    // 切到 LOCAL_* 时刷新模型状态文案；切走时不动（保留上次结果，少做无谓 IO）。
                    localLlmModelKindFor(translatorEngine)?.let { kind ->
                        refreshLlmModelState(kind)
                    } ?: run {
                        llmModelReady = false
                    }
                }

                // 端侧 LLM 翻译区块。当前支持 Sakura（日译中）和 Hy-MT2（多语种）。
                val currentKind = localLlmModelKindFor(translatorEngine)
                if (currentKind != null) {
                    LocalLlmSection(
                        currentKindLabel = currentKind.displayName,
                        deviceCapable = localLlmDeviceCapable,
                        modelReady = llmModelReady,
                        status = statusDuringBackgroundDownload(
                            ModelDownloadSpec.llm(currentKind),
                            llmModelStatus.ifBlank { stringResource(R.string.llm_status_missing) },
                        ),
                        downloading = llmDownloading || backgroundModelDownloadActive,
                        onDownload = {
                            requestModelDownload(currentKind.displayName) {
                                scope.launch {
                                    llmDownloading = true
                                    try {
                                        viewModel.saveLlmMirror(llmMirrorChoice, llmMirror)
                                        viewModel.downloadLlmModel(currentKind) { msg -> llmModelStatus = msg }
                                        refreshLlmModelState(currentKind)
                                    } catch (t: Throwable) {
                                        llmModelStatus = "${t.javaClass.simpleName}: ${t.message}"
                                        llmModelReady = withContext(Dispatchers.IO) {
                                            viewModel.llmModelReady(currentKind)
                                        }
                                    } finally {
                                        llmDownloading = false
                                    }
                                }
                            }
                        },
                        onImport = { uris ->
                            scope.launch {
                                llmDownloading = true
                                try {
                                    val n = viewModel.importLlmFromLocal(uris, currentKind)
                                    refreshLlmModelState(currentKind)
                                    Timber.i("imported $n LLM model file(s) from local")
                                } finally {
                                    llmDownloading = false
                                }
                            }
                        },
                        onDelete = {
                            scope.launch {
                                viewModel.deleteLlmModel(currentKind)
                                refreshLlmModelState(currentKind)
                            }
                        }
                    )
                }

                if (translatorEngine == TranslatorEngine.REMOTE_PC) {
                    OutlinedTextField(
                        value = remotePcBaseUrl,
                        onValueChange = { remotePcBaseUrl = it },
                        label = { Text(stringResource(R.string.settings_remote_pc_base_url)) },
                        placeholder = { Text("http://203.0.113.10:8765 or https://example.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    )
                    SecretTextField(
                        value = remotePcApiKey,
                        onValueChange = { remotePcApiKey = it },
                        label = stringResource(R.string.settings_remote_pc_api_key),
                        placeholder = stringResource(R.string.settings_remote_pc_api_key_placeholder),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = remotePcSessionId,
                        onValueChange = { remotePcSessionId = it },
                        label = { Text(stringResource(R.string.settings_remote_pc_session_id)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = remotePcImageQuality,
                        onValueChange = { remotePcImageQuality = it.filter(Char::isDigit).take(3) },
                        label = { Text(stringResource(R.string.settings_remote_pc_image_quality)) },
                        supportingText = { Text(stringResource(R.string.settings_remote_pc_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                } else if (translatorEngine == TranslatorEngine.OPENAI) {
                    SettingsSearchTarget(
                        searchTargetRegistry,
                        R.string.settings_search_item_base_url,
                        R.string.settings_search_item_api_key,
                        R.string.settings_search_item_model_name,
                    ) {
                    OutlinedTextField(
                        value = baseUrl, onValueChange = { baseUrl = it },
                        label = { Text(stringResource(R.string.settings_base_url)) },
                        placeholder = { Text(stringResource(R.string.settings_base_url_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    SecretTextField(
                        value = apiKey, onValueChange = { apiKey = it },
                        label = stringResource(R.string.settings_api_key),
                        placeholder = stringResource(R.string.settings_api_key_placeholder),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = model, onValueChange = { model = it },
                        label = { Text(stringResource(R.string.settings_model)) },
                        placeholder = { Text(stringResource(R.string.settings_model_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    // 测试连接成功时，下面这块允许从拉到的 model 列表里选一个回填到 model 字段。
                    if (fetchedModels.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = modelPickerExpanded,
                            onExpandedChange = { modelPickerExpanded = !modelPickerExpanded }
                        ) {
                            OutlinedTextField(
                                value = "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.settings_test_pick_model)) },
                                placeholder = { Text("${fetchedModels.size} models") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelPickerExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = modelPickerExpanded,
                                onDismissRequest = { modelPickerExpanded = false }
                            ) {
                                fetchedModels.forEach { id ->
                                    DropdownMenuItem(
                                        text = { Text(id) },
                                        onClick = {
                                            model = id
                                            modelPickerExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    }
                } else if (translatorEngine == TranslatorEngine.ANTHROPIC) {
                    SettingsSearchTarget(
                        searchTargetRegistry,
                        R.string.settings_search_item_anthropic_base_url,
                        R.string.settings_search_item_anthropic_api_key,
                        R.string.settings_search_item_anthropic_model,
                    ) {
                    OutlinedTextField(
                        value = anthropicBaseUrl,
                        onValueChange = { anthropicBaseUrl = it },
                        label = { Text(stringResource(R.string.settings_base_url)) },
                        placeholder = { Text(stringResource(R.string.settings_anthropic_base_url_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    SecretTextField(
                        value = anthropicApiKey,
                        onValueChange = { anthropicApiKey = it },
                        label = stringResource(R.string.settings_api_key),
                        placeholder = stringResource(R.string.settings_anthropic_api_key_placeholder),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = anthropicModel,
                        onValueChange = { anthropicModel = it },
                        label = { Text(stringResource(R.string.settings_model)) },
                        placeholder = { Text(stringResource(R.string.settings_anthropic_model_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Text(
                        stringResource(R.string.settings_anthropic_compatibility_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (fetchedModels.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = modelPickerExpanded,
                            onExpandedChange = { modelPickerExpanded = !modelPickerExpanded },
                        ) {
                            OutlinedTextField(
                                value = "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.settings_test_pick_model)) },
                                placeholder = { Text("${fetchedModels.size} models") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = modelPickerExpanded
                                    )
                                },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                            )
                            ExposedDropdownMenu(
                                expanded = modelPickerExpanded,
                                onDismissRequest = { modelPickerExpanded = false },
                            ) {
                                fetchedModels.forEach { id ->
                                    DropdownMenuItem(
                                        text = { Text(id) },
                                        onClick = {
                                            anthropicModel = id
                                            modelPickerExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                    }
                } else if (translatorEngine == TranslatorEngine.DEEPL) {
                    SettingsSearchTarget(
                        searchTargetRegistry,
                        R.string.settings_search_item_deepl_api_key,
                        R.string.settings_search_item_deepl_pro,
                        R.string.settings_search_item_deepl_advanced,
                    ) {
                    SecretTextField(
                        value = deeplKey, onValueChange = { deeplKey = it },
                        label = stringResource(R.string.settings_deepl_api_key),
                        placeholder = stringResource(R.string.settings_deepl_key_placeholder),
                        modifier = Modifier.fillMaxWidth()
                    )
                    SwitchRow(
                        stringResource(R.string.settings_deepl_use_pro),
                        deeplPro,
                        // OFFICIAL / AUTO 协议都会走官方端点（AUTO 用作 fallback），Pro 都生效；纯 DEEPLX 协议下 Pro 无意义
                        enabled = deeplProtocol != com.gameocr.app.data.DeeplProtocol.DEEPLX
                    ) { deeplPro = it }
                    Text(
                        stringResource(R.string.settings_deepl_tip),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // —— 高级（自架 / deeplx）——
                    // 折叠掉避免吓到只用官方 DeepL 的用户；展开有自定义 URL + Bearer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { deeplAdvancedExpanded = !deeplAdvancedExpanded }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            (if (deeplAdvancedExpanded) "▼ " else "▶ ") +
                                stringResource(R.string.settings_deepl_advanced_header),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (deeplAdvancedExpanded) {
                        Text(
                            stringResource(R.string.settings_deepl_protocol_label),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            EngineChip(deeplProtocol, com.gameocr.app.data.DeeplProtocol.OFFICIAL,
                                stringResource(R.string.settings_deepl_protocol_official)) { deeplProtocol = it }
                            EngineChip(deeplProtocol, com.gameocr.app.data.DeeplProtocol.DEEPLX,
                                stringResource(R.string.settings_deepl_protocol_deeplx)) { deeplProtocol = it }
                            EngineChip(deeplProtocol, com.gameocr.app.data.DeeplProtocol.AUTO,
                                stringResource(R.string.settings_deepl_protocol_auto)) { deeplProtocol = it }
                        }
                        Text(
                            stringResource(when (deeplProtocol) {
                                com.gameocr.app.data.DeeplProtocol.OFFICIAL -> R.string.settings_deepl_protocol_official_hint
                                com.gameocr.app.data.DeeplProtocol.DEEPLX -> R.string.settings_deepl_protocol_deeplx_hint
                                com.gameocr.app.data.DeeplProtocol.AUTO -> R.string.settings_deepl_protocol_auto_hint
                            }),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = deeplBaseUrl,
                            onValueChange = { deeplBaseUrl = it },
                            label = { Text(stringResource(R.string.settings_deepl_base_url)) },
                            placeholder = { Text(stringResource(R.string.settings_deepl_base_url_placeholder)) },
                            modifier = Modifier.fillMaxWidth(), singleLine = true
                        )
                        Text(
                            stringResource(R.string.settings_deepl_base_url_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SecretTextField(
                            value = deeplCustomToken,
                            onValueChange = { deeplCustomToken = it },
                            label = stringResource(R.string.settings_deepl_custom_token),
                            placeholder = stringResource(R.string.settings_deepl_custom_token_placeholder),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            stringResource(R.string.settings_deepl_custom_token_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SwitchRow(
                            stringResource(R.string.settings_deepl_bearer_label),
                            deeplBearerAuth,
                            // DEEPLX / AUTO 都用 customToken，Bearer 才有意义；OFFICIAL 不读
                            enabled = deeplProtocol != com.gameocr.app.data.DeeplProtocol.OFFICIAL
                        ) { deeplBearerAuth = it }
                        Text(
                            stringResource(R.string.settings_deepl_bearer_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.alpha(
                                if (deeplProtocol != com.gameocr.app.data.DeeplProtocol.OFFICIAL) 1f else 0.4f
                            )
                        )
                    }
                    }
                } else if (translatorEngine == TranslatorEngine.YOUDAO_PICTRANS) {
                    SettingsSearchTarget(searchTargetRegistry, R.string.settings_search_item_youdao_pictrans) {
                    SecretTextField(
                        value = youdaoAppKey, onValueChange = { youdaoAppKey = it },
                        label = stringResource(R.string.settings_youdao_app_key),
                        placeholder = stringResource(R.string.settings_youdao_app_key_placeholder),
                        modifier = Modifier.fillMaxWidth()
                    )
                    SecretTextField(
                        value = youdaoAppSecret, onValueChange = { youdaoAppSecret = it },
                        label = stringResource(R.string.settings_youdao_app_secret),
                        placeholder = stringResource(R.string.settings_youdao_app_secret_placeholder),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        stringResource(R.string.settings_youdao_pictrans_tip),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    }
                } else if (translatorEngine == TranslatorEngine.VOLC) {
                    SettingsSearchTarget(searchTargetRegistry, R.string.settings_search_item_volc) {
                    // 火山引擎机器翻译：AK + SK + region；SignV4 鉴权
                    SecretTextField(
                        value = volcAk, onValueChange = { volcAk = it },
                        label = stringResource(R.string.settings_volc_access_key_id),
                        placeholder = stringResource(R.string.settings_volc_ak_placeholder),
                        modifier = Modifier.fillMaxWidth()
                    )
                    SecretTextField(
                        value = volcSk, onValueChange = { volcSk = it },
                        label = stringResource(R.string.settings_volc_secret_access_key),
                        placeholder = stringResource(R.string.settings_volc_sk_placeholder),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = volcRegion, onValueChange = { volcRegion = it },
                        label = { Text(stringResource(R.string.settings_volc_region)) },
                        placeholder = { Text("cn-north-1") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        stringResource(R.string.settings_volc_tip),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    }
                } else if (translatorEngine == TranslatorEngine.BAIDU_FANYI) {
                    SettingsSearchTarget(searchTargetRegistry, R.string.settings_search_item_baidu_fanyi) {
                    // 百度翻译开放平台（fanyi-api.baidu.com）—— 与百度智能云 OCR 不是一回事
                    OutlinedTextField(
                        value = baiduFanyiAppId, onValueChange = { baiduFanyiAppId = it },
                        label = { Text(stringResource(R.string.settings_baidu_fanyi_app_id)) },
                        placeholder = { Text(stringResource(R.string.settings_baidu_fanyi_app_id_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    SecretTextField(
                        value = baiduFanyiSecret, onValueChange = { baiduFanyiSecret = it },
                        label = stringResource(R.string.settings_baidu_fanyi_secret_key),
                        placeholder = stringResource(R.string.settings_baidu_fanyi_secret_key_placeholder),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        stringResource(R.string.settings_baidu_fanyi_tip),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    }
                } else if (translatorEngine == TranslatorEngine.TENCENT) {
                    SettingsSearchTarget(searchTargetRegistry, R.string.settings_search_item_tencent_translator) {
                    // 腾讯云翻译：与 OCR 共用同一套 SecretId/Key/Region（state 双向绑定，
                    // 在这里改和在 OCR 区改完全等价）。region 默认 ap-guangzhou，TMT 各地域通用。
                    OutlinedTextField(
                        value = tencentId, onValueChange = { tencentId = it },
                        label = { Text(stringResource(R.string.settings_tencent_id_label)) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    SecretTextField(
                        value = tencentKey, onValueChange = { tencentKey = it },
                        label = stringResource(R.string.settings_tencent_key_label),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tencentRegion, onValueChange = { tencentRegion = it },
                        label = { Text(stringResource(R.string.settings_tencent_region)) },
                        placeholder = { Text("ap-guangzhou") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Text(
                        stringResource(R.string.settings_tencent_trans_tip),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    }
                } else if (translatorEngine == TranslatorEngine.GOOGLE_ML_KIT) {
                    SettingsSearchTarget(searchTargetRegistry, R.string.settings_search_item_google_mlkit) {
                    Text(
                        stringResource(R.string.settings_google_mlkit_tip),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        stringResource(R.string.settings_mlkit_data_disclosure),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val sourceSelected = sourceLang.isNotBlank() &&
                        !sourceLang.equals(Languages.AUTO.code, ignoreCase = true)
                    val targetSelected = targetLang.isNotBlank() &&
                        !targetLang.equals(Languages.AUTO.code, ignoreCase = true)
                    val sourceSupported = sourceSelected &&
                        MlKitLanguagePolicy.isSupportedLanguageTag(sourceLang)
                    val targetSupported = targetSelected &&
                        MlKitLanguagePolicy.isSupportedLanguageTag(targetLang)
                    val currentPair = sourceLang to targetLang
                    val currentPairReady = mlKitModelStatePair == currentPair &&
                        mlKitModelsReady == true
                    val currentPairChecked = mlKitModelStatePair == currentPair &&
                        mlKitModelsReady != null
                    when {
                        !sourceSupported -> Text(
                            text = stringResource(
                                R.string.settings_mlkit_unsupported_source_language,
                                Languages.nameOf(context, sourceLang),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        !targetSupported -> Text(
                            text = stringResource(
                                R.string.settings_mlkit_unsupported_target_language,
                                Languages.nameOf(context, targetLang),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        !currentPairChecked -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                        currentPairReady -> Text(
                            text = stringResource(R.string.settings_mlkit_model_ready),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        else -> Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                enabled = !mlKitModelDownloadRunning,
                                onClick = { startMlKitModelDownload(currentPair) },
                            ) {
                                Text(
                                    if (mlKitModelDownloadRunning) {
                                        stringResource(R.string.settings_mlkit_model_downloading)
                                    } else {
                                        stringResource(
                                            R.string.settings_mlkit_download_pair,
                                            Languages.nameOf(context, sourceLang),
                                            Languages.nameOf(context, targetLang),
                                        )
                                    }
                                )
                            }
                            if (mlKitModelDownloadRunning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                )
                            }
                        }
                    }
                    mlKitModelDownloadMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    }
                } else if (translatorEngine == TranslatorEngine.GOOGLE) {
                    SettingsSearchTarget(searchTargetRegistry, R.string.settings_search_item_google) {
                    // GOOGLE：无 key，仅提示风险。改 else if 明确匹配——避免后续新增枚举（如 LOCAL_*）
                    // 落入 else 兜底，错误显示 Google 文案。
                    Text(
                        stringResource(R.string.settings_google_tip),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    }
                }

                // —— 测试连接 ——
                // 验证 baseUrl/key/model（或 DeepL key/endpoint）能不能用；DeepL 顺便返回剩余额度，
                // OpenAI 顺便拉 model 列表回填到上方下拉。状态文字按成功/失败着色，下次点击覆盖。
                if (translatorEngine != TranslatorEngine.GOOGLE_ML_KIT) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        enabled = !testRunning,
                        onClick = {
                            testRunning = true
                            testMessage = null
                            scope.launch {
                                try {
                                    val result = viewModel.testTranslator(
                                        translatorEngine = translatorEngine,
                                        remotePcBaseUrl = remotePcBaseUrl,
                                        remotePcApiKey = remotePcApiKey,
                                        remotePcSessionId = remotePcSessionId,
                                        remotePcImageQuality = remotePcImageQuality.toIntOrNull() ?: 85,
                                        baseUrl = baseUrl,
                                        apiKey = apiKey,
                                        model = model,
                                        anthropicBaseUrl = anthropicBaseUrl,
                                        anthropicApiKey = anthropicApiKey,
                                        anthropicModel = anthropicModel,
                                        deeplKey = deeplKey,
                                        deeplPro = deeplPro,
                                        deeplProtocol = deeplProtocol,
                                        deeplBaseUrl = deeplBaseUrl,
                                        deeplBearerAuth = deeplBearerAuth,
                                        deeplCustomToken = deeplCustomToken,
                                        youdaoAppKey = youdaoAppKey,
                                        youdaoAppSecret = youdaoAppSecret,
                                        apiTimeoutSeconds = apiTimeoutSec.toInt(),
                                        volcAccessKeyId = volcAk,
                                        volcSecretAccessKey = volcSk,
                                        volcRegion = volcRegion,
                                        baiduFanyiAppId = baiduFanyiAppId,
                                        baiduFanyiSecretKey = baiduFanyiSecret,
                                        tencentSecretId = tencentId,
                                        tencentSecretKey = tencentKey,
                                        tencentRegion = tencentRegion
                                    )
                                    testSuccess = result.success
                                    testMessage = result.message
                                    if (result.success && result.models.isNotEmpty()) {
                                        fetchedModels = result.models
                                    }
                                } catch (error: CancellationException) {
                                    throw error
                                } catch (error: Exception) {
                                    testSuccess = false
                                    testMessage = error.message ?: error.javaClass.simpleName
                                } finally {
                                    testRunning = false
                                }
                            }
                        }
                    ) {
                        Text(
                            if (testRunning) stringResource(R.string.settings_test_testing)
                            else stringResource(R.string.settings_test_connection)
                        )
                    }
                    if (testRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
                testMessage?.let { msg ->
                    Text(
                        msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (testSuccess) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                }
                }
                    // 乐观更新本地 + 异步落盘。togglePinLanguage 内部用 repo.update 是原子的。
                // Prompt / 流式开关只对 LLM 类（OpenAI 兼容）翻译引擎有意义；
                // DeepL 是机器翻译 API，不读 prompt、也不走 SSE，隐藏避免误导。
                }
                }
                }
                */
                OutlinedTextField(
                    value = remotePcBaseUrl,
                    onValueChange = { remotePcBaseUrl = it },
                    label = { Text(stringResource(R.string.settings_remote_pc_base_url)) },
                    placeholder = { Text("http://203.0.113.10:8765 or https://example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                SecretTextField(
                    value = remotePcApiKey,
                    onValueChange = { remotePcApiKey = it },
                    label = stringResource(R.string.settings_remote_pc_api_key),
                    placeholder = stringResource(R.string.settings_remote_pc_api_key_placeholder),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = remotePcSessionId,
                    onValueChange = { remotePcSessionId = it },
                    label = { Text(stringResource(R.string.settings_remote_pc_session_id)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = remotePcImageQuality,
                    onValueChange = { remotePcImageQuality = it.filter(Char::isDigit).take(3) },
                    label = { Text(stringResource(R.string.settings_remote_pc_image_quality)) },
                    supportingText = { Text(stringResource(R.string.settings_remote_pc_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                val onTogglePin: (String) -> Unit = { code ->
                    pinnedLanguages = if (pinnedLanguages.contains(code)) {
                        pinnedLanguages - code
                    } else {
                        pinnedLanguages + code
                    }
                    scope.launch { viewModel.togglePinLanguage(code) }
                }
                SettingsSearchTarget(searchTargetRegistry, *SEARCH_TARGET_SOURCE_LANGUAGE) {
                LanguagePicker(
                    label = stringResource(R.string.settings_source_lang),
                    currentCode = sourceLang,
                    onSelect = {
                        if (translationLanguageCodesConflict(it, targetLang)) {
                            return@LanguagePicker
                        }
                        timber.log.Timber.tag("OcrLangLink").i(
                            "[user-select-source] %s -> %s", sourceLang, it
                        )
                        sourceLang = it
                        mlKitModelDownloadMessage = null
                        if (
                            translatorEngine == TranslatorEngine.LOCAL_SAKURA &&
                            !supportsSakuraLanguagePair(it, targetLang)
                        ) {
                            showSakuraFallbackDialog = true
                        }
                    },
                    pinned = pinnedLanguages,
                    onTogglePin = onTogglePin,
                    allowAuto = translatorEngine != TranslatorEngine.GOOGLE_ML_KIT,
                    disabledLanguageCodes = setOf(targetLang),
                    disabledStatusLabel = stringResource(R.string.lang_picker_already_target),
                    onDisabledSelect = {
                        pendingLanguageSwapOrigin = LanguageSwapRequestOrigin.SOURCE_PICKER
                    },
                    allowedLanguageCodes = null,
                )
                }
                SettingsSearchTarget(searchTargetRegistry, *SEARCH_TARGET_TARGET_LANGUAGE) {
                LanguagePicker(
                    label = stringResource(R.string.settings_target_lang),
                    currentCode = targetLang,
                    onSelect = {
                        if (translationLanguageCodesConflict(sourceLang, it)) {
                            return@LanguagePicker
                        }
                        targetLang = it
                        mlKitModelDownloadMessage = null
                        if (
                            translatorEngine == TranslatorEngine.LOCAL_SAKURA &&
                            !supportsSakuraLanguagePair(sourceLang, it)
                        ) {
                            showSakuraFallbackDialog = true
                        }
                    },
                    pinned = pinnedLanguages,
                    onTogglePin = onTogglePin,
                    allowAuto = false,
                    disabledLanguageCodes = setOf(sourceLang),
                    disabledStatusLabel = stringResource(R.string.lang_picker_already_source),
                    onDisabledSelect = {
                        pendingLanguageSwapOrigin = LanguageSwapRequestOrigin.TARGET_PICKER
                    },
                    allowedLanguageCodes = null,
                )
                }
                if (translatorEngine != TranslatorEngine.REMOTE_PC) {
                SettingsSearchTarget(searchTargetRegistry, *SEARCH_TARGET_TRANSLATION_ASSISTANCE) {
                TranslationAssistanceSettings(
                    searchTargetRegistry = searchTargetRegistry,
                    translatorEngine = translatorEngine,
                    streaming = streaming,
                    onStreamingChange = { streaming = it },
                    crossLineContextTranslationEnabled = crossLineContextTranslationEnabled,
                    onCrossLineContextTranslationEnabledChange = {
                        crossLineContextTranslationEnabled = it
                    },
                    foregroundAppDetectionMode = foregroundAppDetectionMode,
                    onForegroundAppDetectionModeChange = { foregroundAppDetectionMode = it },
                    usageAccessGranted = usageAccessGranted,
                    onOpenUsageAccess = {
                        val packageIntent = Intent(AndroidSettings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                            data = Uri.parse(usageAccessPackageUri(context.packageName))
                        }
                        runCatching { context.startActivity(packageIntent) }
                            .recoverCatching {
                                context.startActivity(Intent(AndroidSettings.ACTION_USAGE_ACCESS_SETTINGS))
                            }
                    },
                    retryEmptyTranslation = retryEmptyTranslation,
                    onRetryEmptyTranslationChange = { retryEmptyTranslation = it },
                )
                }
                }
                // 推理参数高级设置：仅端侧 LLM 引擎时显示，收起在术语库之后
                if (localLlmModelKindFor(translatorEngine) != null) {
                    val saveNow: () -> Unit = {
                        scope.launch {
                            viewModel.saveLocalLlmInferenceParams(localLlmContextSize, localLlmMaxNewTokens)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(role = Role.Button) {
                                llmInferenceParamsExpanded = !llmInferenceParamsExpanded
                            }
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.settings_llm_inference_params_header),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.graphicsLayer {
                                rotationZ = if (llmInferenceParamsExpanded) 180f else 0f
                            },
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (llmInferenceParamsExpanded) {
                        Text(
                            stringResource(R.string.settings_llm_context_size, localLlmContextSize),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Slider(
                            value = localLlmContextSize.toFloat(),
                            onValueChange = { localLlmContextSize = it.toInt() },
                            onValueChangeFinished = saveNow,
                            valueRange = 512f..4096f,
                            steps = 6,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            stringResource(R.string.settings_llm_max_new_tokens, localLlmMaxNewTokens),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Slider(
                            value = localLlmMaxNewTokens.toFloat(),
                            onValueChange = { localLlmMaxNewTokens = it.toInt() },
                            onValueChangeFinished = saveNow,
                            valueRange = 32f..512f,
                            steps = 14,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                if (translatorEngine == TranslatorEngine.OPENAI ||
                    translatorEngine == TranslatorEngine.ANTHROPIC
                ) {
                    SettingsSearchTarget(searchTargetRegistry, *SEARCH_TARGET_PROMPTS) {
                    OpenAiPromptSettings(
                        prompt = prompt,
                        onPromptChange = { prompt = it },
                        sourceLang = sourceLang,
                        targetLang = targetLang,
                        dictionaryPrompt = dictionaryPrompt,
                        onDictionaryPromptChange = { value ->
                            dictionaryPrompt = value
                            scope.launch { viewModel.saveDictionaryPrompt(value) }
                        },
                    )
                    }
                }
            }

            // —— OCR 引擎 ——
            // 端到端翻译引擎（有道图翻）会跳过 OCR 阶段，整个 OCR 设置区当前会被无视——
            // 灰显 + 禁用 chip 让用户一眼明白 + 不能误操作。
            }


            if (translatorEngine != TranslatorEngine.REMOTE_PC) {

            }

            item(key = SectionKeys.TEXT_ORIENTATION) {
                textOrientationSection()
            }

            // —— 显示 ——
            // 预览是本 section 第一项；滚过页面顶部后吸附，section 离开时自动解除。
            item(key = SectionKeys.OVERLAY) {
            SectionCard(
                title = stringResource(R.string.settings_section_overlay),
                onBoundsInWindow = { _, bottom -> overlaySectionBottomInWindow = bottom },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            overlayPreviewTopInWindow = coordinates.positionInWindow().y
                            overlayPreviewHeightPx = coordinates.size.height
                        }
                ) {
                    OverlayPreviewCard(
                        theme = overlayTheme,
                        customBg = customBg,
                        customFg = customFg,
                        customBorder = customBorder,
                        customBorderW = customBorderW,
                        customBorderStyle = customBorderStyle,
                        textSize = textSize,
                        alpha = alpha,
                        overlayTypeface = overlayFontTypeface,
                        textStyle = overlayTextStyle
                    )
                }

                // —— 影响预览的样式项 ——
                SettingsSearchTarget(searchTargetRegistry, *SEARCH_TARGET_OVERLAY_THEME) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.settings_overlay_theme_label), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EngineChip(overlayTheme, OverlayTheme.CLASSIC_DARK, stringResource(R.string.settings_theme_classic_dark)) { overlayTheme = it }
                    EngineChip(overlayTheme, OverlayTheme.AMBER_GOLD, stringResource(R.string.settings_theme_amber_gold)) { overlayTheme = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EngineChip(overlayTheme, OverlayTheme.PAPER_LIGHT, stringResource(R.string.settings_theme_paper_light)) { overlayTheme = it }
                    EngineChip(overlayTheme, OverlayTheme.FROST_GLASS, stringResource(R.string.settings_theme_frost_glass)) { overlayTheme = it }
                    EngineChip(overlayTheme, OverlayTheme.CUSTOM, stringResource(R.string.settings_theme_custom)) { overlayTheme = it }
                }

                if (overlayTheme == OverlayTheme.CUSTOM) {
                    CustomThemeEditor(
                        bg = customBg, onBgChange = { customBg = it },
                        fg = customFg, onFgChange = { customFg = it },
                        border = customBorder, onBorderChange = { customBorder = it },
                        borderW = customBorderW, onBorderWChange = { customBorderW = it }
                    )
                    // 边框样式：仅在 CUSTOM 主题下显示。SOLID/DASHED/DOTTED 一行，DOUBLE/GROOVE 一行（避开 ExperimentalLayoutApi）。
                    Text(stringResource(R.string.settings_floating_window_border_style_label), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EngineChip(customBorderStyle, com.gameocr.app.data.BorderStyle.SOLID, stringResource(R.string.settings_border_style_solid)) {
                            customBorderStyle = it
                            scope.launch { viewModel.saveCustomBorderStyle(it) }
                        }
                        EngineChip(customBorderStyle, com.gameocr.app.data.BorderStyle.DASHED, stringResource(R.string.settings_border_style_dashed)) {
                            customBorderStyle = it
                            scope.launch { viewModel.saveCustomBorderStyle(it) }
                        }
                        EngineChip(customBorderStyle, com.gameocr.app.data.BorderStyle.DOTTED, stringResource(R.string.settings_border_style_dotted)) {
                            customBorderStyle = it
                            scope.launch { viewModel.saveCustomBorderStyle(it) }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EngineChip(customBorderStyle, com.gameocr.app.data.BorderStyle.DOUBLE, stringResource(R.string.settings_border_style_double)) {
                            customBorderStyle = it
                            scope.launch { viewModel.saveCustomBorderStyle(it) }
                        }
                        EngineChip(customBorderStyle, com.gameocr.app.data.BorderStyle.GROOVE, stringResource(R.string.settings_border_style_groove)) {
                            customBorderStyle = it
                            scope.launch { viewModel.saveCustomBorderStyle(it) }
                        }
                    }
                }

                }
                }

                SettingsSearchTarget(searchTargetRegistry, *SEARCH_TARGET_OVERLAY_TEXT) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.settings_textsize_label_format, textSize.toInt()), style = MaterialTheme.typography.labelLarge)
                Slider(value = textSize, onValueChange = { textSize = it }, valueRange = 10f..28f, steps = 17)

                Text(stringResource(R.string.settings_overlay_font_label), style = MaterialTheme.typography.labelLarge)
                val defaultOverlayFontName = stringResource(R.string.settings_overlay_font_default)
                val overlayFontChipEntries = OverlayFontPolicy.upsertImportedFont(
                    overlayFontEntries,
                    overlayFontFileName,
                    overlayFontDisplayName
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    OverlayFontChip(
                        selected = overlayFontFileName.isBlank(),
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) { viewModel.resetOverlayFont() }
                                overlayFontFileName = ""
                                overlayFontDisplayName = ""
                                overlayFontTypeface = null
                                overlayFontMessage = context.getString(R.string.settings_overlay_font_reset_success)
                                overlayFontMessageIsError = false
                            }
                        },
                        label = defaultOverlayFontName,
                        onLongClick = {}
                    )
                    overlayFontChipEntries.forEach { font ->
                        OverlayFontChip(
                            selected = overlayFontFileName == font.fileName,
                            label = font.displayName,
                            onClick = {
                                scope.launch {
                                    val selected = withContext(Dispatchers.IO) {
                                        viewModel.selectOverlayFont(font.fileName, font.displayName)
                                    }
                                    if (selected) {
                                        overlayFontFileName = font.fileName
                                        overlayFontDisplayName = font.displayName
                                        overlayFontEntries = OverlayFontPolicy.upsertImportedFont(
                                            overlayFontEntries,
                                            font.fileName,
                                            font.displayName
                                        )
                                        overlayFontTypeface = withContext(Dispatchers.IO) {
                                            viewModel.overlayTypefaceFor(font.fileName)
                                        }
                                        overlayFontMessage = null
                                        overlayFontMessageIsError = false
                                    } else {
                                        overlayFontMessage = context.getString(R.string.settings_overlay_font_error_invalid)
                                        overlayFontMessageIsError = true
                                    }
                                }
                            },
                            onLongClick = { pendingOverlayFontDelete = font }
                        )
                    }
                }
                OutlinedButton(
                    onClick = {
                        overlayFontMessage = null
                        overlayFontImportLauncher.launch(OverlayFontPolicy.OPEN_DOCUMENT_MIME_TYPES)
                    }
                ) {
                    Text(stringResource(R.string.settings_overlay_font_import))
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 20.dp)
                ) {
                    overlayFontMessage?.let { message ->
                        Text(
                            message,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (overlayFontMessageIsError) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                }

                OverlayTextStyleEditor(
                    style = overlayTextStyle,
                    onChange = { overlayTextStyle = it.normalized() }
                )

                Text(stringResource(R.string.settings_alpha_label_format, (alpha * 100).toInt()), style = MaterialTheme.typography.labelLarge)
                Slider(value = alpha, onValueChange = { alpha = it }, valueRange = 0.3f..1f)
                }
                }

                // —— 几何项（预览看不到，只能实际触发翻译时看到效果）——
                SettingsSearchTarget(
                    searchTargetRegistry,
                    *(SEARCH_TARGET_OVERLAY_DISPLAY + SEARCH_TARGET_OVERLAY_WINDOW + SEARCH_TARGET_OVERLAY_LAYOUT),
                ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val layoutControlsEnabled =
                    manualOverlayLayoutControlsEnabled(overlayStyleMode, renderMode)
                Text(stringResource(R.string.settings_render_mode_label), style = MaterialTheme.typography.labelLarge)
                val renderModeOptions = listOf(
                    RenderMode.BLOCKS to R.string.settings_render_blocks_chip,
                    RenderMode.FLOATING_WINDOW to R.string.settings_render_floating_window_chip,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    renderModeOptions.forEachIndexed { index, (mode, labelRes) ->
                        SegmentedButton(
                            selected = renderMode == mode,
                            onClick = {
                                if (renderMode != mode) {
                                    renderMode = mode
                                }
                            },
                            enabled = mode != RenderMode.FLOATING_WINDOW || layoutControlsEnabled,
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = renderModeOptions.size,
                            ),
                            icon = {},
                            label = { Text(stringResource(labelRes)) },
                        )
                    }
                }
                InlineSwitchLabel(
                    label = stringResource(R.string.settings_overlay_style_adaptive),
                    checked = overlayStyleMode == OverlayStyleMode.ADAPTIVE,
                    enabled = renderMode == RenderMode.BLOCKS,
                    helpText = stringResource(R.string.settings_overlay_style_adaptive_desc),
                ) { enabled ->
                    overlayStyleMode = if (enabled) {
                        OverlayStyleMode.ADAPTIVE
                    } else {
                        OverlayStyleMode.FIXED
                    }
                }
                if (!layoutControlsEnabled) {
                    Text(
                        stringResource(R.string.settings_overlay_adaptive_layout_locked),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                if (renderMode == RenderMode.FLOATING_WINDOW) {
                    // 悬浮窗口内容形态：原文+译文 / 仅译文。立即生效，不进 save 流程。
                    Text(stringResource(R.string.settings_floating_window_content_label), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EngineChip(
                            floatingWindowContentMode,
                            com.gameocr.app.data.FloatingWindowContentMode.SRC_AND_DST,
                            stringResource(R.string.settings_floating_window_content_src_and_dst)
                        ) {
                            floatingWindowContentMode = it
                            scope.launch { viewModel.saveFloatingWindowContentMode(it) }
                        }
                        EngineChip(
                            floatingWindowContentMode,
                            com.gameocr.app.data.FloatingWindowContentMode.DST_ONLY,
                            stringResource(R.string.settings_floating_window_content_dst_only)
                        ) {
                            floatingWindowContentMode = it
                            scope.launch { viewModel.saveFloatingWindowContentMode(it) }
                        }
                    }
                    SwitchRow(stringResource(R.string.settings_floating_window_locked), floatingWindowLocked) {
                        floatingWindowLocked = it
                        scope.launch { viewModel.saveFloatingWindowLocked(it) }
                    }
                    Text(
                        stringResource(R.string.settings_floating_window_locked_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    androidx.compose.material3.OutlinedButton(onClick = {
                        scope.launch { viewModel.resetFloatingWindowGeometry() }
                    }) {
                        Text(stringResource(R.string.settings_floating_window_reset_geometry))
                    }
                }

                if (renderMode == RenderMode.BLOCKS) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.settings_translation_block_interaction_label),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        SettingHelpTooltip(
                            text = stringResource(
                                R.string.settings_translation_block_interaction_vertical_help
                            ),
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                    val translationBlockCopyOptions = listOf(
                        TranslationBlockInteractionMode.COPY_BUTTON to
                            R.string.settings_translation_block_interaction_copy_button,
                        TranslationBlockInteractionMode.OPEN_COPY_PANEL to
                            R.string.settings_translation_block_interaction_open_panel,
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        translationBlockCopyOptions.forEachIndexed { index, (mode, labelRes) ->
                            SegmentedButton(
                                selected = translationBlockInteractionMode == mode,
                                onClick = {
                                    if (translationBlockInteractionMode != mode) {
                                        translationBlockInteractionMode = mode
                                    }
                                },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = translationBlockCopyOptions.size,
                                ),
                                icon = {},
                                label = { Text(stringResource(labelRes)) },
                            )
                        }
                    }
                    Text(
                        stringResource(
                            if (translationBlockInteractionMode == TranslationBlockInteractionMode.COPY_BUTTON) {
                                R.string.settings_translation_block_interaction_copy_button_hint
                            } else {
                                R.string.settings_translation_block_interaction_open_panel_hint
                            },
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    val effectivePlacement =
                        if (layoutControlsEnabled) placement else OverlayPlacement.OVERLAP
                    val effectiveOffsetX = if (layoutControlsEnabled) offsetX else 0f
                    val effectiveOffsetY = if (layoutControlsEnabled) offsetY else 0f
                    Text(
                        stringResource(R.string.settings_placement_label),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.alpha(if (layoutControlsEnabled) 1f else 0.4f),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EngineChip(effectivePlacement, OverlayPlacement.BELOW, stringResource(R.string.settings_placement_below_chip), enabled = layoutControlsEnabled) { placement = it }
                        EngineChip(effectivePlacement, OverlayPlacement.OVERLAP, stringResource(R.string.settings_placement_overlap_chip), enabled = layoutControlsEnabled) { placement = it }
                        EngineChip(effectivePlacement, OverlayPlacement.ABOVE, stringResource(R.string.settings_placement_above_chip), enabled = layoutControlsEnabled) { placement = it }
                    }

                    Text(
                        stringResource(R.string.settings_offset_x_format, effectiveOffsetX.toInt()),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.alpha(if (layoutControlsEnabled) 1f else 0.4f),
                    )
                    Slider(
                        value = effectiveOffsetX,
                        onValueChange = { offsetX = it },
                        valueRange = -200f..200f,
                        enabled = layoutControlsEnabled,
                    )

                    Text(
                        stringResource(R.string.settings_offset_y_format, effectiveOffsetY.toInt()),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.alpha(if (layoutControlsEnabled) 1f else 0.4f),
                    )
                    Slider(
                        value = effectiveOffsetY,
                        onValueChange = { offsetY = it },
                        valueRange = -100f..100f,
                        enabled = layoutControlsEnabled,
                    )

                    SwitchRow(
                        stringResource(R.string.settings_allow_wrap),
                        checked = if (layoutControlsEnabled) allowWrap else true,
                        enabled = layoutControlsEnabled,
                    ) { allowWrap = it }
                    SwitchRow(
                        stringResource(R.string.settings_avoid_collision),
                        checked = if (layoutControlsEnabled) avoidCollision else false,
                        enabled = layoutControlsEnabled,
                    ) { avoidCollision = it }
                    Text(
                        stringResource(R.string.settings_avoid_collision_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.alpha(if (layoutControlsEnabled) 1f else 0.4f),
                    )

                    SwitchRow(stringResource(R.string.settings_merge_adjacent), mergeAdjacent) { mergeAdjacent = it }
                    if (mergeAdjacent) {
                        Text(
                            stringResource(R.string.settings_merge_strength_label),
                            style = MaterialTheme.typography.labelLarge
                        )
                        val mergeStrengthOptions = listOf(
                            com.gameocr.app.data.MergeStrength.CONSERVATIVE to
                                R.string.settings_merge_strength_conservative,
                            com.gameocr.app.data.MergeStrength.STANDARD to
                                R.string.settings_merge_strength_standard,
                            com.gameocr.app.data.MergeStrength.AGGRESSIVE to
                                R.string.settings_merge_strength_aggressive,
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            mergeStrengthOptions.forEachIndexed { index, (strength, labelRes) ->
                                SegmentedButton(
                                    selected = mergeStrength == strength,
                                    onClick = {
                                        if (mergeStrength != strength) {
                                            mergeStrength = strength
                                        }
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = mergeStrengthOptions.size,
                                    ),
                                    icon = {},
                                    label = { Text(stringResource(labelRes)) },
                                )
                            }
                        }
                        Text(
                            stringResource(when (mergeStrength) {
                                com.gameocr.app.data.MergeStrength.CONSERVATIVE -> R.string.settings_merge_strength_conservative_hint
                                com.gameocr.app.data.MergeStrength.STANDARD -> R.string.settings_merge_strength_standard_hint
                                com.gameocr.app.data.MergeStrength.AGGRESSIVE -> R.string.settings_merge_strength_aggressive_hint
                            }),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        stringResource(R.string.settings_merge_adjacent_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                }
                }

            }

            // —— 悬浮按钮 ——
            }

            // —— 循环触发器 ——
            item(key = SectionKeys.TRIGGER) {
            SettingsSearchTarget(searchTargetRegistry, *SEARCH_TARGET_TRIGGER) {
            SectionCard(title = stringResource(R.string.settings_section_trigger)) {
                SettingsSearchTarget(
                    searchTargetRegistry,
                    R.string.settings_search_item_loop_trigger_mode,
                    R.string.settings_search_item_loop_interval,
                ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.settings_loop_trigger_mode_label),
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EngineChip(
                        loopTriggerMode,
                        LoopTriggerMode.FIXED_INTERVAL,
                        stringResource(R.string.settings_loop_trigger_fixed),
                    ) { loopTriggerMode = it }
                    EngineChip(
                        loopTriggerMode,
                        LoopTriggerMode.WAIT_FOR_TEXT_COMPLETE,
                        stringResource(R.string.settings_loop_trigger_smart),
                    ) { loopTriggerMode = it }
                }
                if (loopTriggerMode == LoopTriggerMode.FIXED_INTERVAL) {
                    OutlinedTextField(
                        value = loopInterval,
                        onValueChange = { loopInterval = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.settings_loop_interval_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                } else {
                    Text(
                        stringResource(
                            R.string.settings_loop_text_stable_duration_format,
                            loopTextStableDurationMs,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Slider(
                        value = loopTextStableDurationMs.toFloat(),
                        onValueChange = {
                            loopTextStableDurationMs =
                                ((it / 100f).roundToInt() * 100L).coerceIn(
                                    LoopFrameStabilityPolicy.MIN_STABLE_DURATION_MS,
                                    LoopFrameStabilityPolicy.MAX_STABLE_DURATION_MS,
                                )
                        },
                        valueRange = LoopFrameStabilityPolicy.MIN_STABLE_DURATION_MS.toFloat()..
                            LoopFrameStabilityPolicy.MAX_STABLE_DURATION_MS.toFloat(),
                        steps = 17,
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(R.string.settings_loop_text_stable_faster),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            stringResource(R.string.settings_loop_text_stable_complete),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        stringResource(R.string.settings_loop_text_stable_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SettingsSearchTarget(searchTargetRegistry, R.string.settings_search_item_loop_region) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        stringResource(R.string.settings_loop_text_region_mode_label),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        EngineChip(
                            loopTextRegionMode,
                            LoopTextRegionMode.AUTO,
                            stringResource(R.string.settings_loop_text_region_auto),
                        ) { loopTextRegionMode = it }
                        EngineChip(
                            loopTextRegionMode,
                            LoopTextRegionMode.LOWER_SCREEN_FIRST,
                            stringResource(R.string.settings_loop_text_region_lower),
                        ) { loopTextRegionMode = it }
                        EngineChip(
                            loopTextRegionMode,
                            LoopTextRegionMode.ANYWHERE,
                            stringResource(R.string.settings_loop_text_region_anywhere),
                        ) { loopTextRegionMode = it }
                    }
                    Text(
                        stringResource(R.string.settings_loop_text_region_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SwitchRow(
                        label = stringResource(R.string.settings_loop_translate_region_only_label),
                        checked = loopTranslateRegionOnly,
                        helpText = stringResource(R.string.settings_loop_translate_region_only_hint),
                    ) { loopTranslateRegionOnly = it }
                    }
                    }
                }
                }
                }
                SettingsSearchTarget(searchTargetRegistry, R.string.settings_search_item_loop_similarity) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SwitchRow(
                    stringResource(R.string.settings_loop_skip_similar_label),
                    loopSkipSimilarFrames
                ) { loopSkipSimilarFrames = it }
                Text(
                    stringResource(
                        R.string.settings_loop_similarity_format,
                        (loopFrameSimilarityThreshold * 100f).roundToInt()
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (loopSkipSimilarFrames) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Slider(
                    value = loopFrameSimilarityThreshold,
                    onValueChange = {
                        loopFrameSimilarityThreshold =
                            ((it * 100f).roundToInt() / 100f).coerceIn(
                                LoopFrameChangePolicy.MIN_SIMILARITY_THRESHOLD,
                                LoopFrameChangePolicy.MAX_SIMILARITY_THRESHOLD
                            )
                    },
                    enabled = loopSkipSimilarFrames,
                    valueRange = LoopFrameChangePolicy.MIN_SIMILARITY_THRESHOLD..
                        LoopFrameChangePolicy.MAX_SIMILARITY_THRESHOLD,
                    steps = 48
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.settings_loop_similarity_resource_saving),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        stringResource(R.string.settings_loop_similarity_sensitive),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    stringResource(R.string.settings_loop_similarity_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                }
                }
                SettingsSearchTarget(searchTargetRegistry, R.string.settings_search_item_a11y_volume) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SwitchRow(stringResource(R.string.settings_a11y_volume_label), a11yVolume) { a11yVolume = it }
                Text(
                    stringResource(R.string.settings_a11y_volume_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = { context.startActivity(Intent(AndroidSettings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.settings_btn_open_a11y)) }
                }
                }
            }

            // —— 悬浮按钮 ——
            }

            }

            item(key = SectionKeys.FLOATING) {
            SettingsSearchTarget(searchTargetRegistry, *SEARCH_TARGET_FLOATING) {
            SectionCard(title = stringResource(R.string.settings_section_floating)) {
                SettingsSearchTarget(searchTargetRegistry, R.string.settings_search_item_floating_size) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.settings_floating_size_format, floatingSize.toInt()), style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = floatingSize,
                    onValueChange = { floatingSize = it },
                    valueRange = 32f..96f,
                    steps = (96 - 32) / 4 - 1
                )
                }
                }

                SettingsSearchTarget(searchTargetRegistry, R.string.settings_search_item_floating_snap) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SwitchRow(stringResource(R.string.settings_floating_snap_edge_label), floatingSnapEdge) { floatingSnapEdge = it }
                Text(
                    stringResource(R.string.settings_floating_snap_edge_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                }
                }

                SettingsSearchTarget(searchTargetRegistry, R.string.settings_search_item_floating_auto_dock) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SwitchRow(
                    stringResource(R.string.settings_floating_auto_dock_label),
                    floatingAutoDock,
                    enabled = floatingSnapEdge
                ) { floatingAutoDock = it }
                Text(
                    stringResource(R.string.settings_floating_auto_dock_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alpha(if (floatingSnapEdge) 1f else 0.4f)
                )
                }
                }

                SettingsSearchTarget(searchTargetRegistry, R.string.settings_search_item_floating_dock_inset) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.settings_floating_dock_inset_format, floatingDockInset.toInt()),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.alpha(if (floatingSnapEdge) 1f else 0.4f)
                )
                Slider(
                    value = floatingDockInset,
                    onValueChange = { floatingDockInset = it },
                    valueRange = 0f..40f,
                    steps = 39,
                    enabled = floatingSnapEdge
                )
                OutlinedButton(
                    onClick = { insetPreviewActive = !insetPreviewActive },
                    enabled = floatingSnapEdge,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(
                        if (insetPreviewActive) R.string.settings_floating_dock_inset_preview_stop
                        else R.string.settings_floating_dock_inset_preview_start
                    ))
                }
                Text(
                    stringResource(R.string.settings_floating_dock_inset_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alpha(if (floatingSnapEdge) 1f else 0.4f)
                )
                }
                }
            }

            // —— 弧菜单按钮顺序 ——
            }

            }

            item(key = SectionKeys.ARC_MENU) {
            SettingsSearchTarget(searchTargetRegistry, *SEARCH_TARGET_ARC_MENU) {
            SectionCard(
                title = stringResource(R.string.settings_section_arc_menu),
            ) {
                Text(
                    stringResource(R.string.settings_arc_menu_page_size, arcMenuPageSize.toInt()),
                    style = MaterialTheme.typography.labelMedium
                )
                Slider(
                    value = arcMenuPageSize,
                    onValueChange = { arcMenuPageSize = it.roundToInt().coerceIn(
                        FloatingMenu.MIN_PAGE_SIZE,
                        FloatingMenu.MAX_PAGE_SIZE
                    ).toFloat() },
                    onValueChangeFinished = {
                        scope.launch { viewModel.saveArcMenuPageSize(arcMenuPageSize.toInt()) }
                    },
                    valueRange = FloatingMenu.MIN_PAGE_SIZE.toFloat()..FloatingMenu.MAX_PAGE_SIZE.toFloat(),
                    steps = FloatingMenu.MAX_PAGE_SIZE - FloatingMenu.MIN_PAGE_SIZE - 1
                )
                Text(
                    stringResource(R.string.settings_arc_menu_page_size_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(
                        R.string.settings_arc_menu_order_desc,
                        arcMenuPageSize.toInt()
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ArcMenuOrderEditor(
                    order = menuOrder,
                    currentSkill = currentSkill,
                    onReorder = { next ->
                        menuOrder = next
                        scope.launch { viewModel.saveArcMenuOrder(next) }
                    }
                )
            }

            // —— 开发者诊断 ——
            }

            }

            item(key = SectionKeys.DEVELOPER) {
            SettingsSearchTarget(searchTargetRegistry, *SEARCH_TARGET_DEVELOPER) {
            SectionCard(
                title = stringResource(R.string.settings_section_developer),
            ) {
                SwitchRow(
                    label = stringResource(R.string.settings_developer_mode_label),
                    checked = developerOptionsEnabled,
                    helpText = stringResource(R.string.settings_developer_mode_hint),
                ) { developerOptionsEnabled = it }
                if (developerOptionsEnabled) {
                    Column(
                        modifier = Modifier.padding(start = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SwitchRow(
                            label = stringResource(
                                R.string.settings_batch_cumulative_completion_time_label
                            ),
                            checked = batchCumulativeCompletionTimeEnabled,
                            helpText = stringResource(
                                R.string.settings_batch_cumulative_completion_time_hint
                            ),
                        ) { batchCumulativeCompletionTimeEnabled = it }
                        SwitchRow(
                            label = stringResource(R.string.settings_disable_translation_cache_label),
                            checked = disableTranslationCache,
                            helpText = stringResource(R.string.settings_disable_translation_cache_hint),
                        ) { disableTranslationCache = it }
                    }
                }
            }

            // —— 网络（全局，跨 OCR / 翻译）——
            }

            }

            item(key = SectionKeys.NETWORK) {
            SettingsSearchTarget(searchTargetRegistry, *SEARCH_TARGET_NETWORK) {
            SectionCard(title = stringResource(R.string.settings_section_network)) {
                SettingsSearchTarget(searchTargetRegistry, R.string.settings_search_item_api_timeout) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.settings_api_timeout_format, apiTimeoutSec.toInt()),
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = apiTimeoutSec,
                    onValueChange = { apiTimeoutSec = it },
                    valueRange = 5f..120f,
                    steps = 22
                )
                Text(
                    stringResource(R.string.settings_api_timeout_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                }
                }

                SettingsSearchTarget(searchTargetRegistry, R.string.settings_search_item_cleartext_hosts) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.settings_cleartext_hosts_label),
                    style = MaterialTheme.typography.labelLarge
                )
                OutlinedTextField(
                    value = cleartextHostsText,
                    onValueChange = { cleartextHostsText = it },
                    placeholder = { Text(stringResource(R.string.settings_cleartext_hosts_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    minLines = 2
                )
                Text(
                    stringResource(R.string.settings_cleartext_hosts_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                }
                }

                // —— 端侧 LLM 下载源 ——
                // 放在网络 section（而非端侧 LLM section）：
                //   1) 选源是网络行为，跟"明文白名单 / 超时"是同一层抽象；
                //   2) 用户切 chip 时不必每次重看 radio 列表，UI 更紧凑。
                // Radio 切换立即落盘，避免用户切完忘点下载导致丢失；自定义 URL 字段编辑频繁，
                // 仅在切回别的 radio 或点下载时落盘。
                SettingsSearchTarget(searchTargetRegistry, R.string.settings_search_item_llm_mirror) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.llm_mirror_section_label),
                    style = MaterialTheme.typography.labelLarge
                )
                val saveLlmMirrorNow: (com.gameocr.app.data.LlmMirrorChoice) -> Unit = { c ->
                    llmMirrorChoice = c
                    scope.launch { viewModel.saveLlmMirror(c, llmMirror) }
                }
                LlmMirrorRadioRow(
                    label = stringResource(R.string.llm_mirror_hf_official),
                    selected = llmMirrorChoice == com.gameocr.app.data.LlmMirrorChoice.HF_OFFICIAL,
                    enabled = true
                ) { saveLlmMirrorNow(com.gameocr.app.data.LlmMirrorChoice.HF_OFFICIAL) }
                LlmMirrorRadioRow(
                    label = stringResource(R.string.llm_mirror_hf_mirror),
                    selected = llmMirrorChoice == com.gameocr.app.data.LlmMirrorChoice.HF_MIRROR,
                    enabled = true
                ) { saveLlmMirrorNow(com.gameocr.app.data.LlmMirrorChoice.HF_MIRROR) }
                LlmMirrorRadioRow(
                    label = stringResource(R.string.llm_mirror_custom),
                    selected = llmMirrorChoice == com.gameocr.app.data.LlmMirrorChoice.CUSTOM,
                    enabled = true
                ) { saveLlmMirrorNow(com.gameocr.app.data.LlmMirrorChoice.CUSTOM) }
                if (llmMirrorChoice == com.gameocr.app.data.LlmMirrorChoice.CUSTOM) {
                    OutlinedTextField(
                        value = llmMirror,
                        onValueChange = { llmMirror = it },
                        label = { Text(stringResource(R.string.llm_mirror_label)) },
                        placeholder = { Text(stringResource(R.string.llm_mirror_placeholder)) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                }
                }
                }
            }

            // 给 FAB 留出底部空间，避免最后一项被遮挡
            }

            }

            item(key = "bottom_spacer") {
                Box(modifier = Modifier.size(80.dp))
            }
            }

            if (overlayPreviewSticky && !searchActive) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .zIndex(1f),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 3.dp,
                ) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        OverlayPreviewCard(
                            theme = overlayTheme,
                            customBg = customBg,
                            customFg = customFg,
                            customBorder = customBorder,
                            customBorderW = customBorderW,
                            customBorderStyle = customBorderStyle,
                            textSize = textSize,
                            alpha = alpha,
                            overlayTypeface = overlayFontTypeface,
                            textStyle = overlayTextStyle
                        )
                    }
                }
            }

            // 搜索下拉：浮在 Column 之上。匹配项点击后滚到对应 section 顶部并关闭搜索。
            if (searchActive && searchQuery.isNotBlank()) {
                val searchCurrentValues = mapOf(
                    settingsSearchEntryId(R.string.settings_search_item_translator_engine) to translatorEngine.name,
                    settingsSearchEntryId(R.string.settings_search_item_source_lang) to sourceLang,
                    settingsSearchEntryId(R.string.settings_search_item_target_lang) to targetLang,
                    settingsSearchEntryId(R.string.settings_translation_output_layout_label) to
                        "${translationOutputLayout.name} ${translationOutputDirection.name}",
                    settingsSearchEntryId(R.string.settings_search_item_render_mode) to renderMode.name,
                    settingsSearchEntryId(R.string.settings_search_item_placement) to placement.name,
                    settingsSearchEntryId(R.string.settings_search_item_overlay_theme) to overlayTheme.name,
                    settingsSearchEntryId(R.string.settings_search_item_loop_trigger_mode) to loopTriggerMode.name,
                    settingsSearchEntryId(R.string.settings_search_item_loop_region) to loopTextRegionMode.name,
                    settingsSearchEntryId(R.string.settings_search_item_llm_mirror) to llmMirrorChoice.name,
                )
                val matches = remember(searchQuery, searchCurrentValues) {
                    SETTING_ITEMS.mapNotNull { entry ->
                        entry.score(context, searchQuery, searchCurrentValues[entry.entryId])
                            ?.let { score -> entry to score }
                    }.sortedWith(
                        compareByDescending<Pair<SearchEntry, Int>> { it.second }
                            .thenBy { context.getString(it.first.itemLabelRes) }
                    ).take(20).map { it.first }
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .heightIn(max = 320.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    if (matches.isEmpty()) {
                        Text(
                            stringResource(R.string.settings_search_no_match),
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn {
                            items(matches) { entry ->
                                ListItem(
                                    headlineContent = { Text(stringResource(entry.itemLabelRes)) },
                                    supportingContent = {
                                        Text(
                                            listOfNotNull(
                                                stringResource(entry.sectionLabelRes),
                                                searchCurrentValues[entry.entryId]?.takeIf(String::isNotBlank),
                                            ).joinToString(" · ")
                                        )
                                    },
                                    colors = ListItemDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    modifier = Modifier.clickable {
                                        closeSearch()
                                        scope.launch {
                                            settingsSectionIndex(entry.sectionKey)?.let { index ->
                                                listState.scrollToItem(index)
                                                repeat(4) {
                                                    withFrameNanos { }
                                                    val requester = searchTargetRegistry.latest(entry.targetId)
                                                    if (requester != null) {
                                                        requester.bringIntoView()
                                                        return@launch
                                                    }
                                                }
                                                if (entry.requiredTranslatorEngine != null) {
                                                    searchTargetRegistry.latest(
                                                        R.string.settings_search_item_translator_engine
                                                    )?.bringIntoView()
                                                }
                                            }
                                        }
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberUsageAccessGranted(context: Context): Boolean {
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
private fun TranslationAssistanceSettings(
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

@Composable
private fun OpenAiPromptSettings(
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

/**
 * 译文样式实时预览卡。展示一段假的"原文 + 译文"，按当前 theme/字号/透明度/自定义色/边框/边框样式渲染。
 *
 * 与 [com.gameocr.app.overlay.OverlayManager] / [com.gameocr.app.overlay.DraggableOverlayWindow] 的视觉保持一致：
 * - 主题颜色映射见 [overlayThemeColors]（务必与 OverlayManager 同步）
 * - alpha 整体应用到 box（模拟 view.setAlpha 的效果，叠加自身像素 alpha）
 * - 棋盘格底色用 linear gradient 模拟实际屏幕背景，让透明度变化肉眼可见
 * - 边框样式：仅 CUSTOM 主题下读 [customBorderStyle]，预设主题恒为 SOLID（与 DraggableOverlayWindow 一致）；
 *   DASH/DOT 间距、DOUBLE 间隙、GROOVE 明暗各 ±40% 全部复制 OverlayManager / DraggableOverlayWindow 的硬编码
 */
private fun overlayFontImportErrorMessage(
    context: android.content.Context,
    error: OverlayFontImportError
): String = context.getString(
    when (error) {
        OverlayFontImportError.UNSUPPORTED_EXTENSION -> R.string.settings_overlay_font_error_extension
        OverlayFontImportError.EMPTY_FILE -> R.string.settings_overlay_font_error_empty
        OverlayFontImportError.TOO_LARGE -> R.string.settings_overlay_font_error_too_large
        OverlayFontImportError.UNREADABLE -> R.string.settings_overlay_font_error_unreadable
        OverlayFontImportError.INVALID_FONT -> R.string.settings_overlay_font_error_invalid
        OverlayFontImportError.COPY_FAILED -> R.string.settings_overlay_font_error_copy_failed
    }
)

@Composable
private fun OverlayTextStyleEditor(
    style: OverlayTextStyle,
    onChange: (OverlayTextStyle) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.settings_text_style_label), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StyleIconToggle(
                checked = style.bold,
                icon = Icons.Default.FormatBold,
                label = stringResource(R.string.settings_text_style_bold),
                onCheckedChange = { onChange(style.copy(bold = it)) }
            )
            StyleIconToggle(
                checked = style.italic,
                icon = Icons.Default.FormatItalic,
                label = stringResource(R.string.settings_text_style_italic),
                onCheckedChange = { onChange(style.copy(italic = it)) }
            )
            StyleIconToggle(
                checked = style.underline,
                icon = Icons.Default.FormatUnderlined,
                label = stringResource(R.string.settings_text_style_underline),
                onCheckedChange = { onChange(style.copy(underline = it)) }
            )
        }

        Text(
            stringResource(R.string.settings_letter_spacing_format, style.letterSpacingEm),
            style = MaterialTheme.typography.labelLarge
        )
        Slider(
            value = style.letterSpacingEm,
            onValueChange = { value ->
                onChange(style.copy(letterSpacingEm = (value * 100f).roundToInt() / 100f))
            },
            valueRange = OverlayTextStyle.MIN_LETTER_SPACING_EM..OverlayTextStyle.MAX_LETTER_SPACING_EM,
            steps = 19
        )

        Text(
            stringResource(R.string.settings_line_spacing_format, style.lineSpacingMultiplier),
            style = MaterialTheme.typography.labelLarge
        )
        Slider(
            value = style.lineSpacingMultiplier,
            onValueChange = { value ->
                onChange(style.copy(lineSpacingMultiplier = (value * 20f).roundToInt() / 20f))
            },
            valueRange = OverlayTextStyle.MIN_LINE_SPACING..OverlayTextStyle.MAX_LINE_SPACING,
            steps = 19
        )

        Text(stringResource(R.string.settings_text_alignment_label), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StyleIconToggle(
                checked = style.alignment == OverlayTextAlignment.START,
                icon = Icons.AutoMirrored.Filled.FormatAlignLeft,
                label = stringResource(R.string.settings_text_alignment_start),
                onCheckedChange = { if (it) onChange(style.copy(alignment = OverlayTextAlignment.START)) }
            )
            StyleIconToggle(
                checked = style.alignment == OverlayTextAlignment.CENTER,
                icon = Icons.Default.FormatAlignCenter,
                label = stringResource(R.string.settings_text_alignment_center),
                onCheckedChange = { if (it) onChange(style.copy(alignment = OverlayTextAlignment.CENTER)) }
            )
            StyleIconToggle(
                checked = style.alignment == OverlayTextAlignment.END,
                icon = Icons.AutoMirrored.Filled.FormatAlignRight,
                label = stringResource(R.string.settings_text_alignment_end),
                onCheckedChange = { if (it) onChange(style.copy(alignment = OverlayTextAlignment.END)) }
            )
        }

        SwitchRow(stringResource(R.string.settings_text_stroke_enabled), style.strokeEnabled) {
            onChange(style.copy(strokeEnabled = it))
        }
        if (style.strokeEnabled) {
            Text(
                stringResource(R.string.settings_text_stroke_width_format, style.strokeWidthDp),
                style = MaterialTheme.typography.labelLarge
            )
            Slider(
                value = style.strokeWidthDp,
                onValueChange = { value ->
                    onChange(style.copy(strokeWidthDp = (value * 2f).roundToInt() / 2f))
                },
                valueRange = OverlayTextStyle.MIN_STROKE_WIDTH_DP..OverlayTextStyle.MAX_STROKE_WIDTH_DP,
                steps = 10
            )
            VisualColorPickerRow(
                stringResource(R.string.settings_text_stroke_color),
                style.strokeColor
            ) { onChange(style.copy(strokeColor = it)) }
        }

        SwitchRow(stringResource(R.string.settings_text_shadow_enabled), style.shadowEnabled) {
            onChange(style.copy(shadowEnabled = it))
        }
        if (style.shadowEnabled) {
            Text(
                stringResource(R.string.settings_text_shadow_radius_format, style.shadowRadiusDp),
                style = MaterialTheme.typography.labelLarge
            )
            Slider(
                value = style.shadowRadiusDp,
                onValueChange = { onChange(style.copy(shadowRadiusDp = it.roundToInt().toFloat())) },
                valueRange = OverlayTextStyle.MIN_SHADOW_RADIUS_DP..OverlayTextStyle.MAX_SHADOW_RADIUS_DP,
                steps = 11
            )
            Text(
                stringResource(R.string.settings_text_shadow_offset_x_format, style.shadowOffsetXDp),
                style = MaterialTheme.typography.labelLarge
            )
            Slider(
                value = style.shadowOffsetXDp,
                onValueChange = { onChange(style.copy(shadowOffsetXDp = it.roundToInt().toFloat())) },
                valueRange = OverlayTextStyle.MIN_SHADOW_OFFSET_DP..OverlayTextStyle.MAX_SHADOW_OFFSET_DP,
                steps = 15
            )
            Text(
                stringResource(R.string.settings_text_shadow_offset_y_format, style.shadowOffsetYDp),
                style = MaterialTheme.typography.labelLarge
            )
            Slider(
                value = style.shadowOffsetYDp,
                onValueChange = { onChange(style.copy(shadowOffsetYDp = it.roundToInt().toFloat())) },
                valueRange = OverlayTextStyle.MIN_SHADOW_OFFSET_DP..OverlayTextStyle.MAX_SHADOW_OFFSET_DP,
                steps = 15
            )
            VisualColorPickerRow(
                stringResource(R.string.settings_text_shadow_color),
                style.shadowColor
            ) { onChange(style.copy(shadowColor = it)) }
        }

        TextButton(onClick = { onChange(OverlayTextStyle()) }) {
            Text(stringResource(R.string.settings_text_style_reset))
        }
    }
}

@Composable
private fun StyleIconToggle(
    checked: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onCheckedChange: (Boolean) -> Unit
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState()
    ) {
        IconToggleButton(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (checked) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                    RoundedCornerShape(4.dp)
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (checked) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun OverlayPreviewCard(
    theme: OverlayTheme,
    customBg: Int,
    customFg: Int,
    customBorder: Int,
    customBorderW: Float,
    customBorderStyle: com.gameocr.app.data.BorderStyle,
    textSize: Float,
    alpha: Float,
    overlayTypeface: android.graphics.Typeface?,
    textStyle: OverlayTextStyle
) {
    val colors = overlayThemeColors(theme, customBg, customFg, customBorder, customBorderW.toInt())
    // 仅 CUSTOM 主题 + borderDp > 0 时让用户选的 borderStyle 生效；与 DraggableOverlayWindow 一致
    val effectiveBorderStyle = if (theme == OverlayTheme.CUSTOM)
        customBorderStyle else com.gameocr.app.data.BorderStyle.SOLID
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(R.string.settings_overlay_preview_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF1F2937), Color(0xFF374151), Color(0xFF1F2937))
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(alpha)
                    .background(
                        Color(colors.bg),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .borderStyleOverlay(
                        borderDp = colors.borderDp,
                        borderColor = colors.border,
                        borderStyle = effectiveBorderStyle,
                        cornerRadiusDp = 6f
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                val previewText = stringResource(R.string.settings_overlay_preview_sample)
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    factory = { ctx ->
                        StyledTranslationTextView(ctx).apply {
                            setIncludeFontPadding(true)
                            gravity = android.view.Gravity.CENTER_VERTICAL
                        }
                    },
                    update = { view ->
                        view.text = previewText
                        view.setTextColor(colors.fg)
                        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
                        view.applyOverlayTextStyle(textStyle, overlayTypeface)
                    }
                )
            }
        }
    }
}

/**
 * 按 [borderStyle] 在 box 上画一圈边框，五种样式行为复制
 * [com.gameocr.app.overlay.DraggableOverlayWindow.shellBackground]：
 * - SOLID：单条 stroke
 * - DASHED：dashPathEffect(8dp on, 5dp off)
 * - DOTTED：dashPathEffect(2dp on, 3dp off)
 * - DOUBLE：外圈 + 内圈两条同色 stroke，间距 = w + 3dp
 * - GROOVE：外圈暗色 (-40%)、内圈亮色 (+40%)，inset = w
 *
 * borderDp <= 0 时直接 noop。
 */
private fun Modifier.borderStyleOverlay(
    borderDp: Int,
    borderColor: Int,
    borderStyle: com.gameocr.app.data.BorderStyle,
    cornerRadiusDp: Float
): Modifier = this.then(
    Modifier.drawBehind {
        if (borderDp <= 0) return@drawBehind
        val w = borderDp.dp.toPx()
        val cornerPx = cornerRadiusDp.dp.toPx()
        val color = Color(borderColor)
        // stroke 居中绘制，rect 往内 inset w/2 才能让外缘正好贴 box 边
        val inset = w / 2f
        val outerRect = androidx.compose.ui.geometry.Rect(
            left = inset,
            top = inset,
            right = size.width - inset,
            bottom = size.height - inset
        )
        val outerRadius = (cornerPx - inset).coerceAtLeast(0f)
        when (borderStyle) {
            com.gameocr.app.data.BorderStyle.SOLID -> drawRoundRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(outerRect.left, outerRect.top),
                size = androidx.compose.ui.geometry.Size(outerRect.width, outerRect.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(outerRadius),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = w)
            )
            com.gameocr.app.data.BorderStyle.DASHED -> drawRoundRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(outerRect.left, outerRect.top),
                size = androidx.compose.ui.geometry.Size(outerRect.width, outerRect.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(outerRadius),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = w,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(8.dp.toPx(), 5.dp.toPx())
                    )
                )
            )
            com.gameocr.app.data.BorderStyle.DOTTED -> drawRoundRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(outerRect.left, outerRect.top),
                size = androidx.compose.ui.geometry.Size(outerRect.width, outerRect.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(outerRadius),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = w,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(2.dp.toPx(), 3.dp.toPx())
                    )
                )
            )
            com.gameocr.app.data.BorderStyle.DOUBLE -> {
                // 外圈
                drawRoundRect(
                    color = color,
                    topLeft = androidx.compose.ui.geometry.Offset(outerRect.left, outerRect.top),
                    size = androidx.compose.ui.geometry.Size(outerRect.width, outerRect.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(outerRadius),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = w)
                )
                // 内圈：间距 = w + 3dp（与 LayerDrawable.setLayerInset 一致）
                val gap = w + 3.dp.toPx()
                val innerInset = inset + gap
                val innerRect = androidx.compose.ui.geometry.Rect(
                    left = innerInset, top = innerInset,
                    right = size.width - innerInset, bottom = size.height - innerInset
                )
                if (innerRect.width > 0f && innerRect.height > 0f) {
                    drawRoundRect(
                        color = color,
                        topLeft = androidx.compose.ui.geometry.Offset(innerRect.left, innerRect.top),
                        size = androidx.compose.ui.geometry.Size(innerRect.width, innerRect.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            (outerRadius - gap).coerceAtLeast(0f)
                        ),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w)
                    )
                }
            }
            com.gameocr.app.data.BorderStyle.GROOVE -> {
                // 外圈暗色
                drawRoundRect(
                    color = Color(shadeArgb(borderColor, -0.4f)),
                    topLeft = androidx.compose.ui.geometry.Offset(outerRect.left, outerRect.top),
                    size = androidx.compose.ui.geometry.Size(outerRect.width, outerRect.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(outerRadius),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = w)
                )
                // 内圈亮色，inset = w
                val innerInset = inset + w
                val innerRect = androidx.compose.ui.geometry.Rect(
                    left = innerInset, top = innerInset,
                    right = size.width - innerInset, bottom = size.height - innerInset
                )
                if (innerRect.width > 0f && innerRect.height > 0f) {
                    drawRoundRect(
                        color = Color(shadeArgb(borderColor, 0.4f)),
                        topLeft = androidx.compose.ui.geometry.Offset(innerRect.left, innerRect.top),
                        size = androidx.compose.ui.geometry.Size(innerRect.width, innerRect.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            (outerRadius - w).coerceAtLeast(0f)
                        ),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w)
                    )
                }
            }
        }
    }
)

/** 与 [com.gameocr.app.overlay.DraggableOverlayWindow.shadeColor] 行为一致。factor>0 加亮、<0 加暗。 */
private fun shadeArgb(color: Int, factor: Float): Int {
    val a = (color shr 24) and 0xFF
    val r = (color shr 16) and 0xFF
    val g = (color shr 8) and 0xFF
    val b = color and 0xFF
    val nr = if (factor >= 0) r + ((255 - r) * factor).toInt() else (r * (1 + factor)).toInt()
    val ng = if (factor >= 0) g + ((255 - g) * factor).toInt() else (g * (1 + factor)).toInt()
    val nb = if (factor >= 0) b + ((255 - b) * factor).toInt() else (b * (1 + factor)).toInt()
    return (a shl 24) or
        (nr.coerceIn(0, 255) shl 16) or
        (ng.coerceIn(0, 255) shl 8) or
        nb.coerceIn(0, 255)
}

/** 主题 → ARGB 颜色映射。与 [com.gameocr.app.overlay.OverlayManager] 内的硬编码必须保持一致。 */
private data class ThemeColors(val bg: Int, val fg: Int, val border: Int, val borderDp: Int)

private fun overlayThemeColors(
    theme: OverlayTheme,
    customBg: Int,
    customFg: Int,
    customBorder: Int,
    customBorderW: Int
): ThemeColors = when (theme) {
    OverlayTheme.CLASSIC_DARK ->
        ThemeColors(bg = 0xE6000000.toInt(), fg = 0xFFFFFFFF.toInt(), border = 0, borderDp = 0)
    OverlayTheme.AMBER_GOLD ->
        ThemeColors(bg = 0xF0241608.toInt(), fg = 0xFFFFD27F.toInt(), border = 0xFFB8860B.toInt(), borderDp = 2)
    OverlayTheme.PAPER_LIGHT ->
        ThemeColors(bg = 0xF0F5EFE0.toInt(), fg = 0xFF3E2A1F.toInt(), border = 0xFFB68850.toInt(), borderDp = 1)
    OverlayTheme.FROST_GLASS ->
        ThemeColors(bg = 0xCC1E293B.toInt(), fg = 0xFFE0F2FE.toInt(), border = 0xFF60A5FA.toInt(), borderDp = 1)
    OverlayTheme.CUSTOM ->
        ThemeColors(bg = customBg, fg = customFg, border = customBorder, borderDp = customBorderW.coerceAtLeast(0))
}

/** 搜索可用的 section key 常量。和 [SETTING_ITEMS] 的 sectionKey 对齐。 */
private object SectionKeys {
    const val TRANSLATE = "translate"
    const val PRESETS = "presets"
    const val OCR = "ocr"
    const val TEXT_ORIENTATION = "text_orientation"
    const val OVERLAY = "overlay"
    const val FLOATING = "floating"
    const val ARC_MENU = "arc_menu"
    const val TRIGGER = "trigger"
    const val DEVELOPER = "developer"
    const val NETWORK = "network"
    const val APP_LANG = "app_lang"
    const val THEME_MODE = "theme_mode"
}

internal val SETTINGS_SECTION_KEYS_IN_ORDER = listOf(
    SectionKeys.APP_LANG,
    SectionKeys.THEME_MODE,
    SectionKeys.PRESETS,
    SectionKeys.TRANSLATE,
    SectionKeys.TEXT_ORIENTATION,
    SectionKeys.OVERLAY,
    SectionKeys.TRIGGER,
    SectionKeys.FLOATING,
    SectionKeys.ARC_MENU,
    SectionKeys.DEVELOPER,
    SectionKeys.NETWORK,
)

internal fun settingsSectionIndex(sectionKey: String): Int? =
    SETTINGS_SECTION_KEYS_IN_ORDER.indexOf(sectionKey).takeIf { it >= 0 }

private val SEARCH_TARGET_APP_LANGUAGE = intArrayOf(R.string.settings_section_app_lang)
private val SEARCH_TARGET_THEME_MODE = intArrayOf(R.string.settings_section_theme_mode)
private val SEARCH_TARGET_PRESETS = intArrayOf(
    R.string.settings_section_translation_presets,
    R.string.settings_search_item_preset_transfer,
)
private val SEARCH_TARGET_TRANSLATOR_ENGINE = intArrayOf(
    R.string.settings_search_item_translator_engine,
    R.string.settings_search_item_local_llm_model,
)
private val SEARCH_TARGET_TRANSLATOR_PROVIDERS = intArrayOf(
    R.string.settings_search_item_base_url,
    R.string.settings_search_item_api_key,
    R.string.settings_search_item_model_name,
    R.string.settings_search_item_anthropic_base_url,
    R.string.settings_search_item_anthropic_api_key,
    R.string.settings_search_item_anthropic_model,
    R.string.settings_search_item_deepl_api_key,
    R.string.settings_search_item_deepl_pro,
    R.string.settings_search_item_deepl_advanced,
    R.string.settings_search_item_youdao_pictrans,
    R.string.settings_search_item_google,
    R.string.settings_search_item_google_mlkit,
    R.string.settings_search_item_volc,
    R.string.settings_search_item_baidu_fanyi,
    R.string.settings_search_item_tencent_translator,
)
private val SEARCH_TARGET_SOURCE_LANGUAGE = intArrayOf(R.string.settings_search_item_source_lang)
private val SEARCH_TARGET_TARGET_LANGUAGE = intArrayOf(R.string.settings_search_item_target_lang)
private val SEARCH_TARGET_TRANSLATION_ASSISTANCE = intArrayOf(
    R.string.settings_search_item_streaming,
    R.string.settings_search_item_empty_translation_retry,
    R.string.settings_foreground_app_detection,
    R.string.settings_send_app_name,
    R.string.settings_grant_usage_access,
)
private val SEARCH_TARGET_PROMPTS = intArrayOf(
    R.string.settings_search_item_prompt,
    R.string.settings_search_item_dictionary_prompt,
)
private val SEARCH_TARGET_OCR_ENGINE = intArrayOf(
    R.string.settings_search_item_ocr_switch,
    R.string.settings_search_item_paddle_ai_studio,
    R.string.settings_search_item_paddle_download,
    R.string.settings_search_item_manga_ocr_download,
    R.string.settings_search_item_umi_ocr,
    R.string.settings_search_item_luna_ocr,
    R.string.settings_search_item_baidu_api_key,
    R.string.settings_search_item_baidu_endpoint,
    R.string.settings_search_item_baidu_lang,
    R.string.settings_search_item_tencent_secret,
    R.string.settings_search_item_tencent_endpoint,
    R.string.settings_search_item_tencent_lang,
    R.string.settings_search_item_tencent_region,
    R.string.settings_search_item_youdao_ocr,
    R.string.settings_search_item_dbnet_advanced,
    R.string.settings_search_item_upscale,
    R.string.settings_search_item_invert,
    R.string.settings_search_item_binarize,
)
private val SEARCH_TARGET_ORIENTATION_DETECTION = intArrayOf(
    R.string.settings_orient_auto_detect_title,
    R.string.settings_search_item_manual_orientation,
    R.string.settings_search_item_orientation_model,
)
private val SEARCH_TARGET_ORIENTATION_OUTPUT = intArrayOf(
    R.string.settings_translation_output_follow_title,
    R.string.settings_translation_output_layout_label,
)
private val SEARCH_TARGET_OVERLAY_DISPLAY = intArrayOf(
    R.string.settings_search_item_render_mode,
    R.string.settings_search_item_translation_block_interaction,
    R.string.settings_search_item_placement,
    R.string.settings_search_item_offset,
)
private val SEARCH_TARGET_OVERLAY_THEME = intArrayOf(
    R.string.settings_search_item_overlay_theme,
    R.string.settings_search_item_custom_theme,
    R.string.settings_search_item_border_style,
)
private val SEARCH_TARGET_OVERLAY_TEXT = intArrayOf(
    R.string.settings_search_item_text_size,
    R.string.settings_search_item_text_style,
    R.string.settings_search_item_overlay_font,
    R.string.settings_search_item_alpha,
)
private val SEARCH_TARGET_OVERLAY_WINDOW = intArrayOf(
    R.string.settings_search_item_floating_window_content,
    R.string.settings_search_item_floating_window_locked,
    R.string.settings_search_item_floating_window_reset,
)
private val SEARCH_TARGET_OVERLAY_LAYOUT = intArrayOf(
    R.string.settings_search_item_allow_wrap,
    R.string.settings_search_item_avoid_collision,
    R.string.settings_search_item_merge_adjacent,
    R.string.settings_search_item_merge_strength,
)
private val SEARCH_TARGET_FLOATING = intArrayOf(
    R.string.settings_search_item_floating_size,
    R.string.settings_search_item_floating_snap,
    R.string.settings_search_item_floating_auto_dock,
    R.string.settings_search_item_floating_dock_inset,
)
private val SEARCH_TARGET_ARC_MENU = intArrayOf(R.string.settings_search_item_arc_menu_order)
private val SEARCH_TARGET_TRIGGER = intArrayOf(
    R.string.settings_search_item_loop_interval,
    R.string.settings_search_item_loop_trigger_mode,
    R.string.settings_search_item_loop_similarity,
    R.string.settings_search_item_loop_region,
    R.string.settings_search_item_a11y_volume,
)
private val SEARCH_TARGET_DEVELOPER = intArrayOf(
    R.string.settings_search_item_developer_ocr,
    R.string.settings_search_item_cross_line_context,
)
private val SEARCH_TARGET_NETWORK = intArrayOf(
    R.string.settings_search_item_api_timeout,
    R.string.settings_search_item_llm_mirror,
    R.string.settings_search_item_cleartext_hosts,
)

internal val SETTINGS_SEARCH_TARGET_RES_IDS: Set<Int> = listOf(
    SEARCH_TARGET_APP_LANGUAGE,
    SEARCH_TARGET_THEME_MODE,
    SEARCH_TARGET_PRESETS,
    SEARCH_TARGET_TRANSLATOR_ENGINE,
    SEARCH_TARGET_TRANSLATOR_PROVIDERS,
    SEARCH_TARGET_SOURCE_LANGUAGE,
    SEARCH_TARGET_TARGET_LANGUAGE,
    SEARCH_TARGET_TRANSLATION_ASSISTANCE,
    SEARCH_TARGET_PROMPTS,
    SEARCH_TARGET_OCR_ENGINE,
    SEARCH_TARGET_ORIENTATION_DETECTION,
    SEARCH_TARGET_ORIENTATION_OUTPUT,
    SEARCH_TARGET_OVERLAY_DISPLAY,
    SEARCH_TARGET_OVERLAY_THEME,
    SEARCH_TARGET_OVERLAY_TEXT,
    SEARCH_TARGET_OVERLAY_WINDOW,
    SEARCH_TARGET_OVERLAY_LAYOUT,
    SEARCH_TARGET_FLOATING,
    SEARCH_TARGET_ARC_MENU,
    SEARCH_TARGET_TRIGGER,
    SEARCH_TARGET_DEVELOPER,
    SEARCH_TARGET_NETWORK,
).flatMap { it.asIterable() }.toSet()

/**
 * 搜索索引条目。sectionLabel/itemLabel 走 res id 跟随系统语言；keywords 同时塞中英文，
 * 让用户用任何一种语言搜索都能命中（i18n 后用户可能习惯输入哪种都说不定）。
 */
/** 把 UI 多行输入框文本拆成 host 列表，trim 每行、去空。保存 / snapshot 对比都走这里保证一致。 */
private fun parseCleartextHosts(text: String): List<String> =
    text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }

private data class SearchEntry(
    val sectionKey: String,
    @androidx.annotation.StringRes val sectionLabelRes: Int,
    @androidx.annotation.StringRes val itemLabelRes: Int,
    val keywords: List<String> = emptyList(),
    val entryId: String = settingsSearchEntryId(itemLabelRes),
    @androidx.annotation.StringRes val targetId: Int = itemLabelRes,
    val optionLabelResIds: List<Int> = emptyList(),
    val requiredTranslatorEngine: TranslatorEngine? = null,
) {
    fun score(context: android.content.Context, q: String, currentValue: String?): Int? {
        return settingsSearchScore(
            query = q,
            itemLabel = context.getString(itemLabelRes),
            sectionLabel = context.getString(sectionLabelRes),
            keywords = keywords,
            optionLabels = optionLabelResIds.map(context::getString),
            currentValue = currentValue,
        )
    }
}

private val SETTINGS_SEARCH_SEPARATOR = Regex("[^\\p{L}\\p{N}]+")

private fun normalizeSettingsSearchText(value: String): String = value
        .lowercase(Locale.ROOT)
        .replace(SETTINGS_SEARCH_SEPARATOR, " ")
        .trim()

internal fun settingsSearchScore(
    query: String,
    itemLabel: String,
    sectionLabel: String,
    keywords: List<String> = emptyList(),
    optionLabels: List<String> = emptyList(),
    currentValue: String? = null,
): Int? {
    val normalizedQuery = normalizeSettingsSearchText(query)
    val terms = normalizedQuery.split(' ').filter { it.isNotBlank() }
    if (terms.isEmpty()) return null
    val normalizedItem = normalizeSettingsSearchText(itemLabel)
    val normalizedSection = normalizeSettingsSearchText(sectionLabel)
    val normalizedCurrent = normalizeSettingsSearchText(currentValue.orEmpty())
    val normalizedOptions = optionLabels.joinToString(" ") { normalizeSettingsSearchText(it) }
    val normalizedKeywords = keywords.joinToString(" ") { normalizeSettingsSearchText(it) }
    val haystack = listOf(
        normalizedItem,
        normalizedSection,
        normalizedCurrent,
        normalizedOptions,
        normalizedKeywords,
    ).joinToString(" ")
    if (!terms.all(haystack::contains)) return null
    return when {
        normalizedItem == normalizedQuery -> 1_000
        normalizedItem.startsWith(normalizedQuery) -> 900
        terms.all(normalizedItem::contains) -> 800
        normalizedCurrent == normalizedQuery -> 700
        normalizedCurrent.contains(normalizedQuery) -> 650
        normalizedOptions.contains(normalizedQuery) -> 550
        normalizedKeywords.contains(normalizedQuery) -> 400
        normalizedSection.contains(normalizedQuery) -> 200
        else -> 100
    }
}

internal fun settingsSearchMatches(query: String, searchableTexts: List<String>): Boolean =
    settingsSearchScore(
        query = query,
        itemLabel = searchableTexts.firstOrNull().orEmpty(),
        sectionLabel = searchableTexts.getOrNull(1).orEmpty(),
        keywords = searchableTexts.drop(2),
    ) != null

/**
 * 设置项可搜索索引。新增设置项时同步加一行；匹配后跳到所在 section 顶部。
 * keywords 混合中英文：英文系统下用户用英文输入仍能搜到中文 section / 反之亦然。
 */
internal val SETTINGS_SEARCH_TRANSFER_KEYWORDS = listOf(
    "settings import", "settings export", "preset import", "preset export", "font backup",
    "configuration backup", "backup", "restore", "设置导入", "设置导出", "预设导入",
    "预设导出", "字体备份", "配置备份", "备份", "恢复",
)

internal val SETTINGS_SEARCH_COLOR_KEYWORDS = listOf(
    "custom", "color", "colour", "background color", "text color", "border color", "hue",
    "saturation", "brightness", "opacity", "border", "自定义", "配色", "颜色", "背景色",
    "文字色", "文字颜色", "边框色", "边框颜色", "色相", "鲜艳度", "饱和度", "亮度",
    "透明度", "边框",
)

internal val SETTINGS_SEARCH_EMPTY_TRANSLATION_RETRY_KEYWORDS = listOf(
    "empty translation", "blank translation", "empty response", "blank response", "retry",
    "空译文", "空翻译", "空响应", "自动重试", "重试",
)

internal val SETTINGS_SEARCH_TRANSLATION_BLOCK_INTERACTION_KEYWORDS = listOf(
    "translation block", "copy translation", "long press", "selection handles", "select text", "copy panel",
    "tap translation", "译文块", "复制译文", "长按", "选择把手", "选择文字", "选择复制", "点击译文",
)

internal val SETTINGS_SEARCH_LOOP_SIMILARITY_KEYWORDS = listOf(
    "loop", "frame", "image", "hash", "similarity", "threshold", "skip", "duplicate",
    "循环", "画面", "图片", "相似度", "阈值", "去重", "跳过", "重复翻译",
)

internal val SETTINGS_SEARCH_LOOP_TRIGGER_KEYWORDS = listOf(
    "loop trigger", "fixed interval", "wait for text", "text complete", "text stability",
    "stable duration", "typing", "dialogue", "subtitle", "循环触发", "固定间隔",
    "智能等待", "文字完成", "文字稳定", "稳定等待", "报幕", "对话", "字幕",
)

internal val SETTINGS_SEARCH_LOOP_REGION_KEYWORDS = listOf(
    "dialogue region", "subtitle region", "text region", "lower screen", "anywhere",
    "region only", "translate all text", "报幕区域", "字幕区域", "文字区域", "下半屏",
    "全屏自由", "仅翻译报幕区域", "翻译全部文字",
)

internal val SETTINGS_SEARCH_DEVELOPER_OCR_KEYWORDS = listOf(
    "developer", "developer mode", "debug", "diagnostic", "ocr box", "red box",
    "bounding box", "source text", "translation text", "screenshot", "save screenshot",
    "translation cache", "disable cache", "开发者", "开发者模式", "调试", "诊断",
    "OCR 红框", "红框", "边界框", "原文", "译文", "截图保存", "翻译缓存", "禁用缓存",
)

private val SETTING_ITEMS: List<SearchEntry> = listOf(
    SearchEntry(
        SectionKeys.TRANSLATE,
        R.string.settings_section_translator,
        R.string.settings_search_item_empty_translation_retry,
        SETTINGS_SEARCH_EMPTY_TRANSLATION_RETRY_KEYWORDS,
    ),
    SearchEntry(SectionKeys.PRESETS, R.string.settings_section_translation_presets, R.string.settings_section_translation_presets, listOf("preset", "presets", "profile", "mode", "系统预设方案", "翻译预设", "预设", "模式")),
    SearchEntry(SectionKeys.PRESETS, R.string.settings_section_translation_presets, R.string.settings_search_item_preset_transfer, SETTINGS_SEARCH_TRANSFER_KEYWORDS),

    // —— 翻译后端 ——
    SearchEntry(
        SectionKeys.TRANSLATE,
        R.string.settings_section_translator,
        R.string.settings_search_item_translator_engine,
        listOf("OpenAI", "DeepL", "LLM", "翻译引擎"),
        optionLabelResIds = listOf(
            R.string.settings_engine_openai_llm,
            R.string.settings_engine_anthropic_llm,
            R.string.settings_engine_deepl,
            R.string.settings_engine_youdao_pictrans,
            R.string.settings_engine_google,
            R.string.settings_on_device_translation_english,
            R.string.settings_ocr_chip_chinese,
            R.string.settings_ocr_chip_japanese,
            R.string.settings_ocr_chip_korean,
            R.string.settings_engine_volc,
            R.string.settings_engine_baidu_fanyi,
            R.string.settings_engine_tencent,
            R.string.settings_engine_local_sakura,
            R.string.settings_engine_local_hymt2,
        ),
    ),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_base_url, listOf("base url"), requiredTranslatorEngine = TranslatorEngine.OPENAI),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_api_key, listOf("api key"), requiredTranslatorEngine = TranslatorEngine.OPENAI),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_model_name, listOf("model", "模型名"), requiredTranslatorEngine = TranslatorEngine.OPENAI),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_anthropic_base_url, listOf("anthropic", "claude", "messages api", "base url"), requiredTranslatorEngine = TranslatorEngine.ANTHROPIC),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_anthropic_api_key, listOf("anthropic", "claude", "x-api-key"), requiredTranslatorEngine = TranslatorEngine.ANTHROPIC),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_anthropic_model, listOf("anthropic", "claude", "model", "模型名"), requiredTranslatorEngine = TranslatorEngine.ANTHROPIC),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_deepl_api_key, listOf("deepl"), requiredTranslatorEngine = TranslatorEngine.DEEPL),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_deepl_pro, listOf("deepl pro"), requiredTranslatorEngine = TranslatorEngine.DEEPL),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_deepl_advanced, listOf("deeplx", "bearer", "official", "protocol", "自架", "高级", "协议", "deepl base url"), requiredTranslatorEngine = TranslatorEngine.DEEPL),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_youdao_pictrans, listOf("youdao", "有道", "图片翻译", "pictrans", "ocrtransapi", "端到端"), requiredTranslatorEngine = TranslatorEngine.YOUDAO_PICTRANS),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_google, listOf("google", "谷歌", "translate"), requiredTranslatorEngine = TranslatorEngine.GOOGLE),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_google_mlkit, listOf("google ml kit", "mlkit", "on-device", "offline", "端侧", "离线"), requiredTranslatorEngine = TranslatorEngine.GOOGLE_ML_KIT),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_volc, listOf("volc", "volcengine", "火山", "字节", "doubao", "bytedance", "access key", "AK", "SK", "region", "区域"), requiredTranslatorEngine = TranslatorEngine.VOLC),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_baidu_fanyi, listOf("baidu fanyi", "百度翻译", "fanyi-api", "appid", "开放平台"), requiredTranslatorEngine = TranslatorEngine.BAIDU_FANYI),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_tencent_translator, listOf("tencent", "腾讯", "tmt", "tmtcloud", "腾讯云翻译"), requiredTranslatorEngine = TranslatorEngine.TENCENT),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_source_lang, listOf("source", "源语言")),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_target_lang, listOf("target", "目标语言")),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_prompt, listOf("prompt", "提示词", "system")),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_dictionary_prompt, listOf("dictionary", "词典", "划词", "word select", "phonetic", "音标", "释义", "definition", "prompt")),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_streaming, listOf("streaming", "流式")),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_cross_line_context, listOf("cross context", "cross line", "上下文", "跨上下文", "段落")),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_send_app_name, listOf("send app name", "prompt app context", "发送应用名称", "模型应用名称")),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_foreground_app_detection, listOf("app detection", "foreground app", "accessibility", "usage access", "应用识别", "前台应用")),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_grant_usage_access, listOf("usage permission", "usage access", "permission", "使用情况权限", "使用情况访问", "授权")),
    SearchEntry(
        SectionKeys.TEXT_ORIENTATION,
        R.string.settings_text_orientation_section_title,
        R.string.settings_translation_output_follow_title,
        listOf("follow recognition", "recognized layout", "跟随识别", "识别文字排列"),
        optionLabelResIds = listOf(R.string.settings_translation_output_follow),
    ),
    SearchEntry(
        SectionKeys.TEXT_ORIENTATION,
        R.string.settings_text_orientation_section_title,
        R.string.settings_translation_output_layout_label,
        listOf("output direction", "translation layout", "writing mode", "译文方向", "译文排列"),
        optionLabelResIds = listOf(
            R.string.settings_translation_output_follow_title,
            R.string.settings_translation_output_follow,
            R.string.settings_translation_output_horizontal,
            R.string.settings_translation_output_vertical,
            R.string.settings_translation_output_ltr,
            R.string.settings_translation_output_rtl,
        ),
    ),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_local_llm_model, listOf("local llm", "gguf", "hy-mt", "sakura", "model download", "model import", "端侧模型", "本地模型", "模型下载", "模型导入")),

    SearchEntry(SectionKeys.TEXT_ORIENTATION, R.string.settings_text_orientation_section_title, R.string.settings_orient_auto_detect_title, listOf("orientation", "text orientation", "direction", "vertical", "horizontal", "自动判别", "方向", "文本方向", "竖排", "横排")),
    SearchEntry(SectionKeys.TEXT_ORIENTATION, R.string.settings_text_orientation_section_title, R.string.settings_search_item_manual_orientation, listOf("manual", "lock", "orientation", "vertical", "horizontal", "stacked", "手动", "锁定", "方向", "竖排", "横排", "逐字")),
    SearchEntry(SectionKeys.TEXT_ORIENTATION, R.string.settings_text_orientation_section_title, R.string.settings_search_item_orientation_model, listOf("orientation model", "doc orientation", "direction model", "ONNX", "方向模型", "文本方向模型", "模型", "download", "下载", "本地导入", "local import", "导入", "delete", "删除")),

    // —— 图像预处理（在 OCR section 内）——

    // —— 显示 ——
    SearchEntry(
        SectionKeys.OVERLAY,
        R.string.settings_section_overlay,
        R.string.settings_search_item_render_mode,
        listOf("紧贴", "横幅", "banner", "render", "display mode", "floating window", "悬浮窗"),
        optionLabelResIds = listOf(
            R.string.settings_render_blocks_chip,
            R.string.settings_render_banner_chip,
            R.string.settings_render_floating_window_chip,
        ),
    ),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_translation_block_interaction, SETTINGS_SEARCH_TRANSLATION_BLOCK_INTERACTION_KEYWORDS),
    SearchEntry(
        SectionKeys.OVERLAY,
        R.string.settings_section_overlay,
        R.string.settings_search_item_placement,
        listOf("下方", "上方", "覆盖", "below", "above", "overlap", "placement"),
        optionLabelResIds = listOf(
            R.string.settings_placement_below_chip,
            R.string.settings_placement_overlap_chip,
            R.string.settings_placement_above_chip,
        ),
    ),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_offset, listOf("offset", "微调")),
    SearchEntry(
        SectionKeys.OVERLAY,
        R.string.settings_section_overlay,
        R.string.settings_search_item_overlay_theme,
        listOf("深色", "浅色", "纸张", "霜玻璃", "琥珀", "theme", "dark", "light", "frost", "amber"),
        optionLabelResIds = listOf(
            R.string.settings_theme_classic_dark,
            R.string.settings_theme_amber_gold,
            R.string.settings_theme_paper_light,
            R.string.settings_theme_frost_glass,
            R.string.settings_theme_custom,
        ),
    ),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_custom_theme, SETTINGS_SEARCH_COLOR_KEYWORDS),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_border_style, listOf("solid", "dashed", "dotted", "double", "groove", "实线", "虚线", "点线", "双线", "凹槽", "边框样式")),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_text_size, listOf("font size", "字号", "字体大小")),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_text_style, listOf("bold", "italic", "underline", "letter spacing", "line spacing", "alignment", "outline", "stroke", "shadow", "加粗", "倾斜", "下划线", "字符间距", "行距", "对齐", "描边", "阴影")),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_overlay_font, listOf("font", "ttf", "字体", "自定义字体", "译文字体")),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_alpha, listOf("alpha", "opacity", "透明度")),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_floating_window_content, listOf("floating window", "悬浮窗", "原文+译文", "仅译文", "src dst", "content mode")),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_floating_window_locked, listOf("lock", "锁定", "悬浮窗")),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_floating_window_reset, listOf("reset", "重置", "还原", "默认", "default", "floating window", "悬浮窗", "geometry", "几何", "位置", "尺寸", "size")),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_allow_wrap, listOf("wrap", "换行", "single line", "多行")),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_avoid_collision, listOf("collision", "碰撞", "避撞", "重叠")),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_merge_adjacent, listOf("merge", "合并", "重叠", "拆段")),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_merge_strength, listOf("strength", "强度", "保守", "标准", "激进", "conservative", "standard", "aggressive")),

    // —— 悬浮按钮 ——
    // 注意：floating_size 历史误指 OVERLAY，0.3.x 起改成 FLOATING（实际控件在 floating section）。
    SearchEntry(SectionKeys.FLOATING, R.string.settings_section_floating, R.string.settings_search_item_floating_size, listOf("floating", "圆球", "悬浮", "size", "大小")),
    SearchEntry(SectionKeys.FLOATING, R.string.settings_section_floating, R.string.settings_search_item_floating_snap, listOf("snap", "贴边", "edge")),
    SearchEntry(SectionKeys.FLOATING, R.string.settings_section_floating, R.string.settings_search_item_floating_auto_dock, listOf("auto dock", "自动停靠", "停靠", "藏边")),
    SearchEntry(SectionKeys.FLOATING, R.string.settings_section_floating, R.string.settings_search_item_floating_dock_inset, listOf("inset", "贴边距离", "手势", "全面屏", "gesture")),

    // —— 弧菜单按钮顺序 ——
    SearchEntry(SectionKeys.ARC_MENU, R.string.settings_section_arc_menu, R.string.settings_search_item_arc_menu_order, listOf("arc menu", "弧菜单", "弧形", "顺序", "order", "reorder", "排序", "拖动", "menu", "按钮", "page", "page size", "分页", "每页", "翻页", "loop", "region", "home", "skill", "技能", "划词", "language", "语言", "源语言", "目标语言")),

    // —— 划词翻译 ——

    // —— 触发器 ——
    SearchEntry(SectionKeys.TRIGGER, R.string.settings_section_trigger, R.string.settings_search_item_loop_interval, listOf("loop", "循环", "interval", "间隔")),
    SearchEntry(
        SectionKeys.TRIGGER,
        R.string.settings_section_trigger,
        R.string.settings_search_item_loop_trigger_mode,
        SETTINGS_SEARCH_LOOP_TRIGGER_KEYWORDS,
        optionLabelResIds = listOf(
            R.string.settings_loop_trigger_fixed,
            R.string.settings_loop_trigger_smart,
        ),
    ),
    SearchEntry(SectionKeys.TRIGGER, R.string.settings_section_trigger, R.string.settings_search_item_loop_similarity, SETTINGS_SEARCH_LOOP_SIMILARITY_KEYWORDS),
    SearchEntry(
        SectionKeys.TRIGGER,
        R.string.settings_section_trigger,
        R.string.settings_search_item_loop_region,
        SETTINGS_SEARCH_LOOP_REGION_KEYWORDS,
        optionLabelResIds = listOf(
            R.string.settings_loop_text_region_auto,
            R.string.settings_loop_text_region_lower,
            R.string.settings_loop_text_region_anywhere,
        ),
    ),
    SearchEntry(SectionKeys.TRIGGER, R.string.settings_section_trigger, R.string.settings_search_item_a11y_volume, listOf("无障碍", "a11y", "accessibility", "volume", "音量")),

    // —— 开发者诊断 ——
    SearchEntry(
        SectionKeys.DEVELOPER,
        R.string.settings_section_developer,
        R.string.settings_search_item_developer_ocr,
        SETTINGS_SEARCH_DEVELOPER_OCR_KEYWORDS,
    ),

    // —— 网络 ——
    SearchEntry(SectionKeys.NETWORK, R.string.settings_section_network, R.string.settings_search_item_api_timeout, listOf("timeout", "超时", "网络", "network")),
    SearchEntry(SectionKeys.NETWORK, R.string.settings_section_network, R.string.settings_search_item_llm_mirror, listOf("mirror", "hf-mirror", "huggingface", "download source", "模型下载源", "下载源", "镜像", "自定义 URL", "local llm", "llm model")),
    SearchEntry(SectionKeys.NETWORK, R.string.settings_section_network, R.string.settings_search_item_cleartext_hosts, listOf("cleartext", "http", "明文", "白名单", "host", "自架", "私有")),

    SearchEntry(SectionKeys.APP_LANG, R.string.settings_section_app_lang, R.string.settings_section_app_lang, listOf("language", "locale", "语言", "中文", "english", "i18n")),

    SearchEntry(SectionKeys.THEME_MODE, R.string.settings_section_theme_mode, R.string.settings_section_theme_mode, listOf("theme", "夜间", "白天", "深色", "浅色", "dark", "light", "night")),
)

internal fun settingsSearchSectionKeys(): Set<String> = SETTING_ITEMS.mapTo(linkedSetOf()) { it.sectionKey }
internal fun settingsSearchItemLabelResIds(): Set<Int> = SETTING_ITEMS.mapTo(linkedSetOf()) { it.itemLabelRes }
internal fun settingsSearchTargetResIds(): Set<Int> = SETTING_ITEMS.mapTo(linkedSetOf()) { it.targetId }
internal fun settingsSearchEntryIds(): Set<String> = SETTING_ITEMS.mapTo(linkedSetOf()) { it.entryId }
internal fun settingsSearchEntryCount(): Int = SETTING_ITEMS.size

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun TranslationPresetSection(
    customPresets: List<TranslationPreset>,
    activeId: String,
    unsavedPreset: TranslationPreset?,
    message: String?,
    localLlmDeviceCapable: Boolean,
    llmModelReady: (LlmModelKind) -> Boolean,
    modelDownloading: Boolean,
    downloadingPresetId: String?,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onSaveUnsaved: (TranslationPreset) -> Unit,
    onApply: (TranslationPreset) -> Unit,
    onDownloadModels: (TranslationPreset, List<TranslationPresetModelIssue>) -> Unit,
    onDelete: (TranslationPreset) -> Unit,
) {
    message?.let { Text(it) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onImport) { Text(stringResource(R.string.settings_translation_preset_import)) }
        OutlinedButton(onClick = onExport) { Text(stringResource(R.string.settings_translation_preset_export)) }
    }
}
internal fun translationPresetCanApply(issues: List<TranslationPresetModelIssue>): Boolean =
    issues.isEmpty()

internal fun translationPresetDownloadRequiresAndroidUpgrade(
    issues: List<TranslationPresetModelIssue>,
): Boolean = issues.any { it.kind == TranslationPresetModelIssueKind.LOCAL_LLM_UNSUPPORTED }

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

internal enum class TranslationPresetModelDownloadState {
    IDLE,
    CURRENT_PRESET,
    BLOCKED_BY_OTHER_DOWNLOAD,
}

private data class ActiveModelDownloadUi(
    val id: java.util.UUID,
    val spec: ModelDownloadSpec?,
    val status: String,
    val downloaded: Long,
    val total: Long,
    val ownerPresetId: String?,
)

internal fun translationPresetModelDownloadState(
    presetId: String,
    activePresetDownloadId: String?,
    modelDownloading: Boolean,
): TranslationPresetModelDownloadState = when {
    activePresetDownloadId == presetId -> TranslationPresetModelDownloadState.CURRENT_PRESET
    modelDownloading -> TranslationPresetModelDownloadState.BLOCKED_BY_OTHER_DOWNLOAD
    else -> TranslationPresetModelDownloadState.IDLE
}

internal fun translationPresetModelDownloadEnabled(
    issues: List<TranslationPresetModelIssue>,
    downloadState: TranslationPresetModelDownloadState,
): Boolean = downloadState == TranslationPresetModelDownloadState.IDLE && issues.any { it.downloadable }

internal fun translationPresetDownloadModelLabels(
    issues: List<TranslationPresetModelIssue>,
): List<String> = issues.asSequence()
    .filter { it.kind == TranslationPresetModelIssueKind.LOCAL_LLM_MISSING }
    .mapNotNull { it.llmModelKind?.displayName }
    .distinct()
    .toList()


internal fun translationPresetOtherDownloadHintVisible(
    issues: List<TranslationPresetModelIssue>,
    downloadState: TranslationPresetModelDownloadState,
): Boolean = downloadState == TranslationPresetModelDownloadState.BLOCKED_BY_OTHER_DOWNLOAD &&
    issues.any { it.downloadable }

internal fun missingOpenAiFallbackFields(
    baseUrl: String,
    apiKey: String,
    model: String,
): List<OpenAiFallbackField> = buildList {
    if (baseUrl.isBlank()) add(OpenAiFallbackField.BASE_URL)
    if (apiKey.isBlank()) add(OpenAiFallbackField.API_KEY)
    if (model.isBlank()) add(OpenAiFallbackField.MODEL)
}

@Composable
private fun CustomThemeEditor(
    bg: Int, onBgChange: (Int) -> Unit,
    fg: Int, onFgChange: (Int) -> Unit,
    border: Int, onBorderChange: (Int) -> Unit,
    borderW: Float, onBorderWChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        VisualColorPickerRow(stringResource(R.string.settings_custom_color_bg), bg, onBgChange)
        VisualColorPickerRow(stringResource(R.string.settings_custom_color_fg), fg, onFgChange)
        VisualColorPickerRow(stringResource(R.string.settings_custom_color_border), border, onBorderChange)
        Text(stringResource(R.string.settings_custom_color_border_w_format, borderW.toInt()), style = MaterialTheme.typography.labelLarge)
        Slider(value = borderW, onValueChange = onBorderWChange, valueRange = 0f..6f, steps = 5)
    }
}

@Composable
private fun VisualColorPickerRow(label: String, argb: Int, onChange: (Int) -> Unit) {
    var pickerOpen by remember { mutableStateOf(false) }
    var draft by remember(argb, pickerOpen) { mutableStateOf(VisualColorPickerState.fromArgb(argb)) }
    val chooseColorLabel = stringResource(R.string.settings_color_choose)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable(role = Role.Button) { pickerOpen = true }
            .padding(horizontal = 4.dp, vertical = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(
                chooseColorLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(argb), RoundedCornerShape(4.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
        )
        Icon(
            imageVector = Icons.Default.Palette,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 8.dp)
                .size(24.dp),
        )
    }

    if (pickerOpen) {
        VisualColorPickerDialog(
            label = label,
            state = draft,
            onStateChange = { draft = it.normalized() },
            onDismiss = { pickerOpen = false },
            onApply = {
                onChange(draft.toArgb())
                pickerOpen = false
            },
        )
    }
}

@Composable
private fun VisualColorPickerDialog(
    label: String,
    state: VisualColorPickerState,
    onStateChange: (VisualColorPickerState) -> Unit,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
) {
    val previewArgb = state.toArgb()
    val opacityLabel = stringResource(R.string.settings_color_opacity)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(16.dp),
        ) {
            val dialogBounds = visualColorDialogBounds(maxWidth.value, maxHeight.value)
            Surface(
                shape = RoundedCornerShape(8.dp),
                tonalElevation = 6.dp,
                modifier = Modifier
                    .width(dialogBounds.widthDp.dp)
                    .heightIn(max = dialogBounds.maxHeightDp.dp),
            ) {
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(
                            start = 24.dp,
                            top = 20.dp,
                            end = 24.dp,
                            bottom = 12.dp,
                        ),
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .background(Color(previewArgb), RoundedCornerShape(4.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                        )
                        SaturationValuePicker(
                            state = state,
                            label = stringResource(R.string.settings_color_visual_area),
                            stateText = stringResource(
                                R.string.settings_color_visual_state_format,
                                (state.saturation * 100f).roundToInt(),
                                (state.value * 100f).roundToInt(),
                            ),
                            increaseSaturationLabel = stringResource(R.string.settings_color_saturation_increase),
                            decreaseSaturationLabel = stringResource(R.string.settings_color_saturation_decrease),
                            increaseValueLabel = stringResource(R.string.settings_color_brightness_increase),
                            decreaseValueLabel = stringResource(R.string.settings_color_brightness_decrease),
                            onChange = onStateChange,
                        )
                        Text(
                            stringResource(R.string.settings_color_hue),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        VisualHueSlider(
                            hue = state.hue,
                            label = stringResource(R.string.settings_color_hue),
                            onChange = { onStateChange(state.copy(hue = it).normalized()) },
                        )
                        Text(
                            stringResource(
                                R.string.settings_color_opacity_format,
                                (state.alpha * 100f).roundToInt(),
                            ),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Slider(
                            value = state.alpha,
                            onValueChange = { onStateChange(state.copy(alpha = it).normalized()) },
                            valueRange = 0f..1f,
                            modifier = Modifier.semantics {
                                contentDescription = opacityLabel
                            },
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.settings_color_cancel))
                        }
                        TextButton(onClick = onApply) {
                            Text(stringResource(R.string.settings_color_apply))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SaturationValuePicker(
    state: VisualColorPickerState,
    label: String,
    stateText: String,
    increaseSaturationLabel: String,
    decreaseSaturationLabel: String,
    increaseValueLabel: String,
    decreaseValueLabel: String,
    onChange: (VisualColorPickerState) -> Unit,
) {
    val hueArgb = android.graphics.Color.HSVToColor(floatArrayOf(state.hue, 1f, 1f))
    val selectorArgb = android.graphics.Color.HSVToColor(
        floatArrayOf(state.hue, state.saturation, state.value)
    )
    fun updatePosition(x: Float, y: Float, width: Float, height: Float) {
        val selection = saturationValueFromPosition(x, y, width, height)
        onChange(
            state.copy(
                saturation = selection.saturation,
                value = selection.value,
            )
        )
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.45f)
            .clip(RoundedCornerShape(4.dp))
            .pointerInput(state.hue, state.alpha) {
                detectTapGestures { offset ->
                    updatePosition(
                        offset.x,
                        offset.y,
                        size.width.toFloat(),
                        size.height.toFloat(),
                    )
                }
            }
            .pointerInput(state.hue, state.alpha) {
                detectDragGestures(
                    onDragStart = { offset ->
                        updatePosition(
                            offset.x,
                            offset.y,
                            size.width.toFloat(),
                            size.height.toFloat(),
                        )
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        updatePosition(
                            change.position.x,
                            change.position.y,
                            size.width.toFloat(),
                            size.height.toFloat(),
                        )
                    },
                )
            }
            .semantics {
                contentDescription = label
                stateDescription = stateText
                customActions = listOf(
                    CustomAccessibilityAction(increaseSaturationLabel) {
                        onChange(state.copy(saturation = state.saturation + 0.05f).normalized())
                        true
                    },
                    CustomAccessibilityAction(decreaseSaturationLabel) {
                        onChange(state.copy(saturation = state.saturation - 0.05f).normalized())
                        true
                    },
                    CustomAccessibilityAction(increaseValueLabel) {
                        onChange(state.copy(value = state.value + 0.05f).normalized())
                        true
                    },
                    CustomAccessibilityAction(decreaseValueLabel) {
                        onChange(state.copy(value = state.value - 0.05f).normalized())
                        true
                    },
                )
            }
    ) {
        drawRect(
            brush = Brush.horizontalGradient(
                listOf(Color.White, Color(hueArgb))
            )
        )
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color.Transparent, Color.Black)
            )
        )
        val center = Offset(
            x = state.saturation * size.width,
            y = (1f - state.value) * size.height,
        )
        drawCircle(Color.Black, radius = 11.dp.toPx(), center = center)
        drawCircle(Color.White, radius = 8.dp.toPx(), center = center)
        drawCircle(Color(selectorArgb), radius = 5.dp.toPx(), center = center)
    }
}

@Composable
private fun VisualHueSlider(
    hue: Float,
    label: String,
    onChange: (Float) -> Unit,
) {
    val hueColors = listOf(
        Color(0xFFFF0000),
        Color(0xFFFFFF00),
        Color(0xFF00FF00),
        Color(0xFF00FFFF),
        Color(0xFF0000FF),
        Color(0xFFFF00FF),
        Color(0xFFFF0000),
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
        ) {
            drawRect(brush = Brush.horizontalGradient(hueColors))
        }
        Slider(
            value = hue,
            onValueChange = onChange,
            valueRange = 0f..VisualColorPickerState.MAX_HUE,
            colors = SliderDefaults.colors(
                thumbColor = Color(
                    android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
                ),
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = label },
        )
    }
}
@Composable
private fun ArcMenuOrderEditor(
    order: List<MenuItemId>,
    currentSkill: com.gameocr.app.data.FloatingSkill,
    onReorder: (List<MenuItemId>) -> Unit
) {
    val itemHeight = 56.dp
    val itemSpacing = 6.dp
    // 一个槽 = item + 行间距，落位 / 让位都按这个步长算
    val slotHeightPx = with(LocalDensity.current) { (itemHeight + itemSpacing).toPx() }
    var draggedIdx by remember { mutableStateOf(-1) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    val targetIdx = if (draggedIdx < 0) -1
        else (draggedIdx + (dragOffsetY / slotHeightPx).roundToInt())
            .coerceIn(0, order.size - 1)

    Column(modifier = Modifier.fillMaxWidth()) {
        order.forEachIndexed { idx, id ->
            val isDragged = idx == draggedIdx
            // 让位规则：拖动项 idx → 目标 targetIdx，途经的项整体向反方向挪 1 个 slot
            val displacementPx = when {
                draggedIdx < 0 || isDragged -> 0f
                // 向下拖：原本在拖动项下方、且 ≤ targetIdx 的项要往上让一格
                draggedIdx < idx && idx <= targetIdx -> -slotHeightPx
                // 向上拖：原本在拖动项上方、且 ≥ targetIdx 的项要往下让一格
                draggedIdx > idx && idx >= targetIdx -> slotHeightPx
                else -> 0f
            }
            val animatedDisplacement by animateFloatAsState(
                targetValue = displacementPx,
                label = "arc_menu_displace_$idx"
            )
            val translation = if (isDragged) dragOffsetY else animatedDisplacement
            val bgColor = if (isDragged) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    // 被拖项浮到最上层，避免后绘制的 Row 把它盖住
                    .zIndex(if (isDragged) 1f else 0f)
                    .graphicsLayer { translationY = translation }
                    .background(bgColor, shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp)
                    .pointerInput(order, idx) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggedIdx = idx
                                dragOffsetY = 0f
                            },
                            onDrag = { _, drag ->
                                dragOffsetY += drag.y
                            },
                            onDragEnd = {
                                val slots = (dragOffsetY / slotHeightPx).roundToInt()
                                val target = (idx + slots).coerceIn(0, order.size - 1)
                                if (target != idx) {
                                    val next = order.toMutableList().apply {
                                        val moved = removeAt(idx)
                                        add(target, moved)
                                    }
                                    onReorder(next)
                                }
                                draggedIdx = -1
                                dragOffsetY = 0f
                            },
                            onDragCancel = {
                                draggedIdx = -1
                                dragOffsetY = 0f
                            }
                        )
                    }
            ) {
                // 左侧：三横杠拖动手柄（自绘 vector），明示「这一行可拖动」
                Icon(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.ic_drag_handle),
                    contentDescription = stringResource(R.string.settings_arc_menu_drag_handle),
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(modifier = Modifier.width(12.dp))
                // 中间：菜单项真实图标
                Icon(
                    painter = androidx.compose.ui.res.painterResource(menuItemIconRes(id, currentSkill)),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Box(modifier = Modifier.width(12.dp))
                Text(
                    menuItemLabel(id, currentSkill),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
            }
            Box(modifier = Modifier.height(itemSpacing))
        }
    }
}

private class PendingModelDownload(
    val modelLabel: String,
    val warning: ModelDownloadNetworkWarning,
    val onConfirmed: () -> Unit,
)

internal fun modelDownloadNetworkWarningMessageRes(
    warning: ModelDownloadNetworkWarning,
): Int = when (warning) {
    ModelDownloadNetworkWarning.CELLULAR ->
        R.string.settings_model_download_network_warning_cellular_format
    ModelDownloadNetworkWarning.UNKNOWN ->
        R.string.settings_model_download_network_warning_unknown_format
}

@Composable
private fun menuItemLabel(id: MenuItemId, currentSkill: FloatingSkill): String = when (id) {
        MenuItemId.LOOP,
        MenuItemId.FULL_SCREEN_SKILL -> {
            val targetSkill = checkNotNull(MenuItemRegistry.targetSkill(id, currentSkill))
            if (targetSkill == FloatingSkill.LOOP) {
                stringResource(R.string.settings_arc_menu_item_loop)
            } else {
                val targetSkillName = stringResource(
                    when (targetSkill) {
                        FloatingSkill.FULL_SCREEN -> R.string.menu_full_screen_skill
                        FloatingSkill.WORD_SELECT -> R.string.menu_full_screen_skill
                        FloatingSkill.LOOP -> error("Handled above")
                    }
                )
                stringResource(R.string.settings_arc_menu_item_skill_format, targetSkillName)
            }
        }
        MenuItemId.REGION -> stringResource(R.string.settings_arc_menu_item_region)
        MenuItemId.LANGUAGE_PAIR -> stringResource(R.string.settings_arc_menu_item_language_pair)
        MenuItemId.PRESET_SWITCH -> stringResource(R.string.settings_arc_menu_item_preset)
        MenuItemId.SETTINGS -> stringResource(R.string.settings_arc_menu_item_settings)
        MenuItemId.HOME -> stringResource(R.string.settings_arc_menu_item_home)
        MenuItemId.RESTART_CAPTURE -> stringResource(R.string.menu_restart_capture)
    // 技能槽：跟弧菜单实际显示一致，文案 = 「切换主球操作 — <切换目标>」。
    // 未来加新 FloatingSkill 值时只需扩展 menuItemIconRes / 这里的 when，无需改文案模板。
}

private fun menuItemIconRes(id: MenuItemId, currentSkill: FloatingSkill): Int = when (id) {
    MenuItemId.LOOP,
    MenuItemId.FULL_SCREEN_SKILL -> when (checkNotNull(MenuItemRegistry.targetSkill(id, currentSkill))) {
        FloatingSkill.FULL_SCREEN -> R.drawable.ic_menu_full_screen
        FloatingSkill.WORD_SELECT -> R.drawable.ic_menu_full_screen
        FloatingSkill.LOOP -> R.drawable.ic_menu_loop
    }
        MenuItemId.REGION -> R.drawable.ic_menu_region
        MenuItemId.LANGUAGE_PAIR -> R.drawable.ic_menu_language_pair
        MenuItemId.PRESET_SWITCH -> R.drawable.ic_menu_preset
        MenuItemId.SETTINGS -> R.drawable.ic_menu_settings
        MenuItemId.HOME -> R.drawable.ic_menu_home
        MenuItemId.RESTART_CAPTURE -> R.drawable.ic_menu_restart
    // 技能槽预览要跟实际弧形菜单一致：图标表示“点击后切到的技能”。
}

/**
 * 模型名 → 描述 → 状态行 → 主按钮对（下载 / 本地导入）→ 镜像 URL → 删除按钮。
 * 与 OCR 两个 Section 唯一差异：不再单独印 "powered by" / license 文案——许可信息走关于页统一展示。
 */
@Composable
private fun LocalLlmSection(
    currentKindLabel: String,
    deviceCapable: Boolean,
    modelReady: Boolean,
    status: String,
    downloading: Boolean,
    onDownload: () -> Unit,
    onImport: (List<android.net.Uri>) -> Unit,
    onDelete: () -> Unit
) {
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris -> if (uris.isNotEmpty()) onImport(uris) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                stringResource(R.string.llm_section_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                currentKindLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
        Text(
            stringResource(R.string.llm_section_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!deviceCapable) {
            Text(
                stringResource(R.string.err_llm_device_unsupported),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (downloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Box(modifier = Modifier.size(8.dp))
            }
            Text(status, style = MaterialTheme.typography.bodyMedium)
        }

        // 下载源选择（含自定义 URL）放在「网络」section 内，跨 OCR / 翻译共用；此处不再重复展示。
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                enabled = localLlmDownloadEnabled(
                    downloading = downloading,
                    deviceCapable = deviceCapable,
                    modelReady = modelReady,
                ),
                onClick = onDownload,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(
                    if (downloading) R.string.settings_paddle_btn_processing else R.string.llm_download_button
                ))
            }
            OutlinedButton(
                enabled = downloadableModelImportEnabled(downloading, modelReady),
                onClick = { importLauncher.launch("*/*") },
                modifier = Modifier.weight(1f)
            ) { Text(stringResource(R.string.settings_paddle_btn_local_import)) }
        }

        var showDeleteConfirm by remember { mutableStateOf(false) }
        OutlinedButton(
            onClick = { showDeleteConfirm = true },
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.llm_delete_button)) }
        if (showDeleteConfirm) {
            CatalystAlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text(stringResource(R.string.settings_model_delete_confirm_title)) },
                text = { Text(stringResource(R.string.settings_model_delete_confirm_message)) },
                confirmButton = {
                    DestructiveTextButton(
                        label = stringResource(R.string.settings_model_delete_confirm_yes),
                        onClick = {
                            showDeleteConfirm = false
                            onDelete()
                        },
                    )
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text(stringResource(R.string.settings_model_delete_confirm_no))
                    }
                }
            )
        }
    }
}

internal fun checkingPlaceholderIfUnresolved(currentStatus: String, checkingPlaceholder: String): String =
    currentStatus.ifBlank { checkingPlaceholder }

internal fun localLlmDownloadEnabled(
    downloading: Boolean,
    deviceCapable: Boolean,
    modelReady: Boolean,
): Boolean = !downloading && deviceCapable && !modelReady

internal fun downloadableModelDownloadEnabled(
    downloading: Boolean,
    modelReady: Boolean,
): Boolean = !downloading && !modelReady

internal fun downloadableModelImportEnabled(
    downloading: Boolean,
    modelReady: Boolean,
): Boolean = !downloading && !modelReady

@Composable
private fun LlmMirrorRadioRow(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onSelect)
    ) {
        androidx.compose.material3.RadioButton(
            selected = selected,
            onClick = onSelect,
            enabled = enabled
        )
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 4.dp))
    }
}



private data class AppLanguageOption(
    val tag: String,
    @androidx.annotation.StringRes val labelRes: Int,
)

private val APP_LANGUAGE_OPTIONS = listOf(
    AppLanguageOption("", R.string.settings_app_lang_follow_system),
    AppLanguageOption("zh-CN", R.string.settings_app_lang_zh),
    AppLanguageOption("en", R.string.settings_app_lang_en),
    AppLanguageOption("ru", R.string.settings_app_lang_ru),
)

@Composable
private fun AppLanguageSelector() {
    // 归一化系统返回的 BCP-47（"zh-Hans-CN" / "zh" / "en-US" 等）到 options 里精确 tag。
    fun normalize(raw: String): String {
        if (raw.isEmpty()) return ""
        val exact = APP_LANGUAGE_OPTIONS.firstOrNull {
            it.tag.isNotEmpty() && raw.equals(it.tag, ignoreCase = true)
        }
        if (exact != null) return exact.tag
        val primary = raw.substringBefore('-').lowercase()
        return APP_LANGUAGE_OPTIONS
            .firstOrNull { it.tag.startsWith(primary, ignoreCase = true) && it.tag.isNotEmpty() }
            ?.tag
            ?: ""
    }

    val context = LocalContext.current
    val initial = remember { normalize(com.gameocr.app.data.AppLocalePrefs.read(context)) }
    var tag by remember { mutableStateOf(initial) }
    var expanded by remember { mutableStateOf(false) }

    val currentOption = APP_LANGUAGE_OPTIONS.firstOrNull { it.tag == tag } ?: APP_LANGUAGE_OPTIONS.first()
    val currentLabel = stringResource(currentOption.labelRes)

    val apply: (String) -> Unit = { newTag ->
        if (newTag != tag) {
            tag = newTag
            // 自管持久化：MainActivity.attachBaseContext 会在 recreate 后读 prefs 并包装
            // Configuration locale，绕开 AppCompatDelegate 在 ComponentActivity 上的持久化不稳问题。
            com.gameocr.app.data.AppLocalePrefs.write(context, newTag)
            (context as? android.app.Activity)?.recreate()
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = currentLabel,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            APP_LANGUAGE_OPTIONS.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(stringResource(opt.labelRes)) },
                    onClick = {
                        expanded = false
                        apply(opt.tag)
                    }
                )
            }
        }
    }
    Text(
        stringResource(R.string.settings_app_lang_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ThemeModeSelector() {
    val controller = com.gameocr.app.ui.theme.LocalThemeMode.current
    val mode = controller.mode
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        EngineChip(mode, com.gameocr.app.ui.theme.ThemeMode.FOLLOW_SYSTEM, stringResource(R.string.settings_theme_follow_system)) { controller.setMode(it) }
        EngineChip(mode, com.gameocr.app.ui.theme.ThemeMode.LIGHT, stringResource(R.string.settings_theme_light)) { controller.setMode(it) }
        EngineChip(mode, com.gameocr.app.ui.theme.ThemeMode.DARK, stringResource(R.string.settings_theme_dark)) { controller.setMode(it) }
    }
}

internal fun localLlmModelKindFor(engine: TranslatorEngine): LlmModelKind? = when (engine) {
    TranslatorEngine.LOCAL_SAKURA -> LlmModelKind.SAKURA_1_5B_Q4
    TranslatorEngine.LOCAL_HY_MT2 -> LlmModelKind.HY_MT2_1_8B_Q4_K_M
    else -> null
}

internal fun isLocalLlmEngine(engine: TranslatorEngine): Boolean = when (engine) {
    TranslatorEngine.LOCAL_SAKURA,
    TranslatorEngine.LOCAL_HY_MT2 -> true
    else -> false
}

internal fun supportsSakuraSource(sourceLang: String): Boolean =
    com.gameocr.app.translate.RoutingTranslator.supportsSakuraSource(sourceLang)

internal fun supportsSakuraTarget(targetLang: String): Boolean =
    com.gameocr.app.translate.RoutingTranslator.supportsSakuraTarget(targetLang)

internal fun supportsSakuraLanguagePair(sourceLang: String, targetLang: String): Boolean {
    return supportsSakuraSource(sourceLang) && supportsSakuraTarget(targetLang)
}

internal enum class OpenAiFallbackField {
    BASE_URL,
    API_KEY,
    MODEL,
}

internal const val SAKURA_TARGET_LANG_ZH_CN = "zh-CN"

internal data class SakuraLanguageIssue(
    val sourceUnsupported: Boolean,
    val targetUnsupported: Boolean,
)

internal fun sakuraLanguageIssue(sourceLang: String, targetLang: String): SakuraLanguageIssue {
    return SakuraLanguageIssue(
        sourceUnsupported = !supportsSakuraSource(sourceLang),
        targetUnsupported = !supportsSakuraTarget(targetLang),
    )
}

internal enum class TranslationPresetModelIssueKind { LOCAL_LLM_UNSUPPORTED, LOCAL_LLM_MISSING }
internal data class TranslationPresetModelIssue(val kind: TranslationPresetModelIssueKind, val llmModelKind: LlmModelKind? = null) { val downloadable get() = kind != TranslationPresetModelIssueKind.LOCAL_LLM_UNSUPPORTED }
internal fun translationPresetModelIssues(preset: TranslationPreset, localLlmDeviceCapable: Boolean, llmModelReady: (LlmModelKind) -> Boolean): List<TranslationPresetModelIssue> = buildList { localLlmModelKindFor(preset.translatorEngine)?.let { kind -> if (!localLlmDeviceCapable) add(TranslationPresetModelIssue(TranslationPresetModelIssueKind.LOCAL_LLM_UNSUPPORTED)) else if (!llmModelReady(kind)) add(TranslationPresetModelIssue(TranslationPresetModelIssueKind.LOCAL_LLM_MISSING, kind)) } }
internal fun translationPresetDisplayName(preset: TranslationPreset): String = preset.name
internal fun translationPresetSummary(preset: TranslationPreset): String = "${preset.name} · ${preset.sourceLang} → ${preset.targetLang}"
internal fun newCustomPresetId(): String = "custom_${System.currentTimeMillis()}"
private fun translationPresetDownloadModelLabel(context: Context, issues: List<TranslationPresetModelIssue>): String = translationPresetDownloadModelLabels(issues).joinToString().ifBlank { context.getString(R.string.settings_translation_preset_download_models) }


@Composable
private fun SectionCard(
    title: String,
    onBoundsInWindow: ((top: Float, bottom: Float) -> Unit)? = null,
    helpText: String? = null,
    content: @Composable () -> Unit
) {
    val cardModifier = Modifier
        .fillMaxWidth()
        .onGloballyPositioned { coordinates ->
            onBoundsInWindow?.let { callback ->
                val top = coordinates.positionInWindow().y
                callback(top, top + coordinates.size.height)
            }
        }
    Card(
        modifier = cardModifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                helpText?.let { SettingHelpTooltip(text = it) }
            }
            content()
        }
    }
}


@Composable
internal fun SwitchRow(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    helpText: String? = null,
    onChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedBorderColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surface,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
        Text(
            text = label,
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
                .alpha(if (enabled) 1f else 0.4f)
        )
        helpText?.let { SettingHelpTooltip(text = it) }
    }
}


@Composable
internal fun <T> EngineChip(
    current: T,
    target: T,
    label: String,
    enabled: Boolean = true,
    onSelect: (T) -> Unit
) {
    FilterChip(
        selected = current == target,
        onClick = { onSelect(target) },
        label = { Text(label) },
        enabled = enabled
    )
}

@Composable
private fun OverlayFontChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val bg = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val fg = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val border = if (selected) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.outline
    }
    Box(
        modifier = Modifier
            .height(36.dp)
            .widthIn(max = 180.dp)
            .background(bg, RoundedCornerShape(8.dp))
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .pointerInput(label, selected) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = fg,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


@Composable
private fun SettingsLinkCell(
    label: String,
    status: String? = null,
    statusGranted: Boolean? = null,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick),
        headlineContent = {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                status?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (statusGranted) {
                            true -> MaterialTheme.colorScheme.primary
                            false -> MaterialTheme.colorScheme.error
                            null -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}


@Composable
private fun InlineSwitchLabel(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    helpText: String? = null,
    onChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedBorderColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surface,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline,
            ),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .padding(start = 8.dp)
                .alpha(if (enabled) 1f else 0.4f),
        )
        helpText?.let { SettingHelpTooltip(text = it) }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingHelpTooltip(
    text: String,
    modifier: Modifier = Modifier
) {
    val state = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.widthIn(max = 280.dp)
                )
            }
        },
        state = state,
        modifier = modifier
    ) {
        IconButton(
            onClick = {
                if (state.isVisible) {
                    state.dismiss()
                } else {
                    scope.launch { state.show() }
                }
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.HelpOutline,
                contentDescription = stringResource(R.string.settings_help_content_description),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
private fun SecretTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { p -> { Text(p) } },
        singleLine = true,
        modifier = modifier,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = stringResource(
                        if (visible) R.string.secret_hide else R.string.secret_show
                    )
                )
            }
        }
    )
}


@Composable
private fun ModelDownloadProgressCard(
    status: String,
    downloaded: Long,
    total: Long,
    onCancel: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.model_download_in_app_title),
                style = MaterialTheme.typography.titleSmall,
            )
            val progress = modelDownloadProgressFraction(downloaded, total)
            if (progress == null) {
                androidx.compose.material3.LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.model_download_cancel))
            }
        }
    }
}


@Composable
private fun ModelDownloadFailureCard(
    failure: ModelDownloadTerminalRecord,
    onRetry: () -> Unit,
) {
    val context = LocalContext.current
    val modelLabel = failure.specs.joinToString(", ") {
        modelDownloadSpecDisplayName(context, it)
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.model_download_failure_card_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                stringResource(R.string.model_download_failure_model_format, modelLabel),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                failure.status.ifBlank { failure.error },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                modelDownloadByteSummary(context, failure.downloaded, failure.total),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.model_download_retry))
            }
        }
    }
}


private fun modelDownloadSpecDisplayName(
    context: Context,
    spec: ModelDownloadSpec,
): String = runCatching {
    when (spec.type) {
        ModelDownloadType.LLM -> LlmModelKind.valueOf(spec.variant).displayName
        else -> spec.encode()
    }
}.getOrDefault(spec.encode())

private fun modelDownloadByteSummary(
    context: Context,
    downloaded: Long,
    total: Long,
): String {
    val downloadedLabel = Formatter.formatFileSize(context, downloaded.coerceAtLeast(0L))
    return if (total > 0L) {
        context.getString(
            R.string.model_download_failure_downloaded_total_format,
            downloadedLabel,
            Formatter.formatFileSize(context, total),
        )
    } else {
        context.getString(R.string.model_download_failure_downloaded_format, downloadedLabel)
    }
}

internal fun modelDownloadProgressFraction(downloaded: Long, total: Long): Float? =
    total.takeIf { it > 0L }
        ?.let { (downloaded.toDouble() / it).coerceIn(0.0, 1.0).toFloat() }




internal fun shouldShowOverlayFontDeleteTipBeforeImport(
    currentFileName: String,
    fonts: List<OverlayFontEntry>
): Boolean = currentFileName.isBlank() && OverlayFontPolicy.normalizeImportedFonts(fonts).isEmpty()

internal fun overlayFontDeleteTipAckLabel(
    baseLabel: String,
    countdown: Int
): String = if (countdown > 0) "($countdown) $baseLabel" else baseLabel
