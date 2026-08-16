package com.gameocr.app.ui

import android.content.Intent
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import timber.log.Timber
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
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
import androidx.compose.runtime.DisposableEffect
import com.gameocr.app.overlay.EdgeInsetPreviewOverlay
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gameocr.app.R
import com.gameocr.app.capture.LoopFrameChangePolicy
import com.gameocr.app.capture.LoopFrameStabilityPolicy
import com.gameocr.app.data.FloatingMenu
import com.gameocr.app.data.FloatingSkill
import com.gameocr.app.data.Languages
import com.gameocr.app.data.LoopTriggerMode
import com.gameocr.app.data.LoopTextRegionMode
import com.gameocr.app.data.MenuItemId
import com.gameocr.app.data.OverlayFontImportResult
import com.gameocr.app.data.OverlayFontPolicy
import com.gameocr.app.data.OverlayFontEntry
import com.gameocr.app.data.OverlayPlacement
import com.gameocr.app.data.OverlayStyleMode
import com.gameocr.app.data.OverlayTheme
import com.gameocr.app.data.OverlayTextStyle
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
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

internal class TranslationPresetModelIssue

internal fun translationPresetCanApply(issues: List<TranslationPresetModelIssue>): Boolean =
    issues.isEmpty()

internal fun translationPresetModelIssues(
    preset: TranslationPreset,
    localLlmDeviceCapable: Boolean,
    llmModelReady: (Any) -> Boolean,
): List<TranslationPresetModelIssue> = emptyList()

@OptIn(ExperimentalFoundationApi::class)
internal class SettingsSearchTargetRegistry {
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
internal fun SettingsSearchTarget(
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
    var deeplKey by remember { mutableStateOf("") }
    var deeplPro by remember { mutableStateOf(false) }
    var deeplBaseUrl by remember { mutableStateOf("") }
    var deeplBearerAuth by remember { mutableStateOf(false) }
    var deeplCustomToken by remember { mutableStateOf("") }
    var deeplProtocol by remember { mutableStateOf(com.gameocr.app.data.DeeplProtocol.OFFICIAL) }
    var deeplAdvancedExpanded by remember { mutableStateOf(false) }
    // жњ‰йЃ“ж™єдє‘дёЂеҐ— keyпј€OCR + е›ѕз‰‡зї»иЇ‘е…±з”Ёпј‰
    var youdaoAppKey by remember { mutableStateOf("") }
    var youdaoAppSecret by remember { mutableStateOf("") }
    // зЃ«е±±еј•ж“Ћжњєе™Ёзї»иЇ‘ AK/SK + regionпј€SignV4пј‰
    var volcAk by remember { mutableStateOf("") }
    var volcSk by remember { mutableStateOf("") }
    var volcRegion by remember { mutableStateOf("cn-north-1") }
    // з™ѕеє¦зї»иЇ‘ејЂж”ѕе№іеЏ° APPID + еЇ†й’Ґпј€дёЋз™ѕеє¦ж™єиѓЅдє‘ OCR е®Ње…ЁдёЌжЇдёЂе›ћдє‹пј‰
    var baiduFanyiAppId by remember { mutableStateOf("") }
    var baiduFanyiSecret by remember { mutableStateOf("") }
    // зї»иЇ‘еј•ж“Ћ"жµ‹иЇ•иїћжЋҐ"жЊ‰й’®зљ„зћ¬ж—¶зЉ¶жЂЃпјљtesting / з»“жћњж–‡е­— / ж€ђеЉџи‰І / OpenAI ж‹‰е€°зљ„ model е€—иЎЁгЂ‚
    // дёЌиї› SettingsпјЊзєЇ UI зЉ¶жЂЃпј›е€‡жЌў engine дёЌжё…з©єпј€з”Ёж€·е€‡е›ћеЋ»иїиѓЅзњ‹е€°дёЉж¬Ўзљ„з»“жћњпј‰гЂ‚
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
    // еј§иЏњеЌ•жЊ‰й’®йЎєеєЏ + е€’иЇЌиЇЌе…ё promptпјљж‹–еЉЁ / зј–иѕ‘еђЋеЌіж—¶йЂљиї‡ vm зљ„ saveArcMenuOrder / saveDictionaryPrompt
    // еЌ•е­—ж®µиђЅз›пјЊ**дёЌ**иµ°дё» save зљ„ dirty жµЃзЁ‹пј€з”Ёж€·жњџжњ›з«‹е€»з”џж•€пјЊж— йњЂз‚№дїќе­пј‰гЂ‚
    var menuOrder by remember { mutableStateOf<List<MenuItemId>>(emptyList()) }
    var arcMenuPageSize by remember { mutableStateOf(FloatingMenu.DEFAULT_PAGE_SIZE.toFloat()) }
    // еЅ“е‰Ќдё»зђѓжЉЂиѓЅгЂ‚жЉЂиѓЅж§Ѕпј€FULL_SCREEN_SKILLпј‰й‚ЈдёЂиЎЊзљ„ж–‡жЎ€и¦Ѓи·џзќЂе®ѓеЉЁжЂЃжѕз¤єгЂЊе€‡е€°еЇ№ж–№гЂЌпјљ
    // еЅ“е‰Ќ FULL_SCREEN в†’ жѕз¤єгЂЊвЂ” е€’иЇЌзї»иЇ‘гЂЌпј›еЅ“е‰Ќ WORD_SELECT в†’ жѕз¤єгЂЊвЂ” е…Ёе±Џзї»иЇ‘гЂЌ
    var currentSkill by remember { mutableStateOf(com.gameocr.app.data.FloatingSkill.FULL_SCREEN) }
    var dictionaryPrompt by remember { mutableStateOf("") }
    // ж‚¬жµ®жЊ‰й’®"иґґиѕ№и·ќз¦»" slider зљ„е®ћж—¶йў„и§€пјље±Џе№•дё¤дѕ§з”» inset е®Ѕеє¦зљ„еЌЉйЂЏзІ‰жќЎгЂ‚
    // й»и®¤ falseвЂ”вЂ”иї›и®ѕзЅ®е°±жѕз¤єжќЎеё¦е¤ЄзЄЃе…Ђпј›з”Ёж€·ењЁ slider ж—Ѓж‰‹еЉЁејЂеђЇгЂЊйў„и§€гЂЌеђЋж‰Ќи¦†з›–е€°е±Џе№•дёЉгЂ‚
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
    // жЋж–‡ HTTP з™ЅеђЌеЌ•пјљз”Ёж€·жЇЏиЎЊдёЂдёЄ hostпјЊUI дёЉз”Ё StringпјЊдїќе­ж—¶ split("\n")
    var cleartextHostsText by remember { mutableStateOf("") }
    // жџж ‡иЇ­иЁЂпјљжњ¬ењ°й•њеѓЏгЂ‚togglePinLanguage з«‹еЌіиђЅз›пјЊдё‹ж¬Ў ON_RESUME / load() ж‹‰е›ћжњЂж–°пј›
    // иї™й‡Њд№џд№ђи§‚ж›ґж–°дёЂд»Ѕжњ¬ењ°зЉ¶жЂЃпјЊUI з«‹е€»еЏЌж гЂ‚
    var pinnedLanguages by remember { mutableStateOf<List<String>>(emptyList()) }

    // dirty жЈЂжµ‹пјљload ж—¶ capture дёЂд»Ѕе€ќе§‹ SettingsпјЊд№‹еђЋи·џ buildSnapshot() жЇ” equalsгЂ‚
    // ж—§з‰€ж‰‹е†™дё¤д»Ѕ List<Any?>пјЊжЇЏеЉ  Settings е­—ж®µйѓЅи¦ЃењЁдё¤дёЄ list еђЊж­ҐеЉ пјЊеЏЌе¤ЌзЉЇ"еїж”№дёЂиѕ№"зљ„ bugгЂ‚
    // зЋ°ењЁз”Ё data class equals и‡ЄеЉЁи¦†з›–ж‰Ђжњ‰е­—ж®µвЂ”вЂ”еЉ е­—ж®µеЏЄж”№ buildSnapshot() дёЂе¤„гЂ‚
    var initialSettings by remember { mutableStateOf<Settings?>(null) }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var showUnsupportedPresetDownloadDialog by remember { mutableStateOf(false) }
    var pendingLanguageSwapOrigin by remember {
        mutableStateOf<LanguageSwapRequestOrigin?>(null)
    }

    suspend fun refreshPresetModelReadiness(
        customPresets: List<TranslationPreset> = translationPresets,
    ) = Unit

    fun effectiveTranslatorEngine(): TranslatorEngine = translatorEngine

    fun selectTranslatorEngine(engine: TranslatorEngine) {
        translatorEngine = engine
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
        translationPresets = s.translationPresets
        activeTranslationPresetId = s.activeTranslationPresetId
        pinnedLanguages = s.pinnedLanguages
        cleartextHostsText = s.cleartextAllowedHosts.joinToString("\n")
    }
    fun presetDisplayNameForMessage(preset: TranslationPreset): String = preset.name

    // вЂ”вЂ” жђњзґўпјљйЎ¶йѓЁиѕ“е…Ґ в†’ дё‹ж‹‰еЊ№й…ЌйЎ№ в†’ з‚№е‡» animateScrollTo е€°еЇ№еє” section йЎ¶йѓЁ вЂ”вЂ”
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

    // д»Ће®Њж•ґжЊЃд№…еЊ–еї«з…§иµ·ж­ҐпјЊе†Ќи¦†з›–жњ¬йЎµе°љжњЄдїќе­зљ„иЌ‰зЁїеЂјгЂ‚дёЌиѓЅд»Ћ Settings() й»и®¤еЂјиµ·ж­ҐпјЊеђ¦е€™
    // еЇје‡єдјљжЉЉйў„и®ѕгЂЃе›єе®љиЇ­иЁЂгЂЃж‚¬жµ®зЄ—зЉ¶жЂЃе’Њжњ¬ењ° LLM еЏ‚ж•°з­‰йќћеЅ“е‰ЌиЎЁеЌ•е­—ж®µйќ™й»й‡ЌзЅ®гЂ‚
    // з±»ећ‹иЅ¬жЌўи·џ doSave дїќжЊЃдёЂи‡ґпј€textSize.toInt() / loopInterval.toLongOrNull() з­‰пј‰гЂ‚
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

    // derivedStateOf и®© lambda ењЁдѕќиµ– state еЏеЊ–ж—¶ж‰Ќй‡Ќж–°и®Ўз®— equals
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

    // жєђиЇ­иЁЂв†”OCR иЃ”еЉЁпјљжЈЂжџҐиѓЅеђ¦иЇ†е€«еЅ“е‰ЌжєђиЇ­иЁЂпј›дёЌиѓЅе€™жЊ‰"з”Ёж€·е€љеЉЁзљ„жЇе“ЄдёЂиѕ№"е†іе®љжЋЁиЌђж–№еђ‘гЂ‚
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

    LaunchedEffect(Unit) {
        val s = viewModel.load()
        // suspend ж“ЌдЅњеї…йЎ»ењЁ Snapshot еќ—е¤–еЃље®Њ
        val migratedPrompt = viewModel.migrateDefaultPromptIfStale(context)
        // е…ій”®жЂ§иѓЅпјљжЉЉ 40+ state е†™е…Ґе°Ѓиї›еђЊдёЂдёЄ mutable snapshotпјЊеЋџе­ђ apply еђЋеЏЄи§¦еЏ‘
        // дёЂж¬Ў observer йЂљзџҐпјЊйЃїе…Ќ Compose ењЁжЇЏдёЄ state еЏеЊ–ж—¶ schedule дёЂж¬Ў recomposition
        // / derivedStateOf й‡Ќз®—пјЊиї›и®ѕзЅ®йЎµй‚Јж®µ"еЌЎдёЂдё‹"дё»и¦ЃжќҐи‡Єиї™й‡ЊгЂ‚
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
            // дёЌй»еЎћдё»зєїзЁ‹пјљfile.exists() + file.length() иµ° IO DispatcherгЂ‚е…€з»™еЌ дЅЌ
            // ж–‡е­—пјЊIO е®Њж€ђеђЋе†Ќи¦†з›–пј›иї›и®ѕзЅ®зљ„зћ¬й—ґдёЌеЌЎйЎїгЂ‚
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
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    // йІжЉ¤пјљload е®Њж€ђе‰Ќ state жЇй»и®¤еЌ дЅЌеЂјпјЊж­¤ж—¶дїќе­дјљжЉЉз©єе­—з¬¦дёІ / й»и®¤ enum
                    // е†™е…Ґ DataStoreпјЊи¦†з›–з”Ёж€·е®ћй™…ж•°жЌ®гЂ‚LaunchedEffect е®Њж€ђпј€~13msпј‰ж‰ЌжЉЉ
                    // initialSettings и®ѕеЂјпјЊй‚Јд№‹еђЋж‰Ќе…Ѓи®ёдїќе­гЂ‚
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
            // з›ґжЋҐ inflate ColumnвЂ”вЂ”дёЌжѕз¤є spinnerпјЊйЃїе…Ќ"жЊ‰дё‹и®ѕзЅ® в†’ spinner в†’ UI"й‚Јж®µз©єз™ЅеЌЎйЎїж„џгЂ‚
            // state й»и®¤еЂјпј€з©єе­—з¬¦дёІ / й»и®¤ enumпј‰дјље…€зџ­жљ‚жѕз¤єпјЊLaunchedEffect ењЁ ~13ms е†… Snapshot
            // еЋџе­ђж›ґж–°ж‰Ђжњ‰ state е€°е®ћй™…дїќе­еЂјвЂ”вЂ”и‚‰зњје‡ д№ЋдёЌеЇџи§‰й—ЄзѓЃгЂ‚д»Јд»·пјљз”Ёж€·ењЁ initialSettings
            // иїжЇ null ж—¶з‚№дїќе­жЊ‰й’®дјљз”Ёй»и®¤еЂји¦†з›–ж•°жЌ®пјЊж‰Ђд»Ґдё‹йќў FAB еЉ дє† enabled йІжЉ¤гЂ‚
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            // вЂ”вЂ” еє”з”ЁиЇ­иЁЂ вЂ”вЂ”
            item(key = SectionKeys.APP_LANG) {
                SettingsSearchTarget(searchTargetRegistry, *SEARCH_TARGET_APP_LANGUAGE) {
                SectionCard(title = stringResource(R.string.settings_section_app_lang)) {
                    AppLanguageSelector()
                }
                }
            }

            // вЂ”вЂ” дё»йўжЁЎејЏ вЂ”вЂ”
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

            // вЂ”вЂ” зї»иЇ‘еђЋз«Ї вЂ”вЂ”
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
                    // жµ‹иЇ•иїћжЋҐж€ђеЉџж—¶пјЊдё‹йќўиї™еќ—е…Ѓи®ёд»Ћж‹‰е€°зљ„ model е€—иЎЁй‡ЊйЂ‰дёЂдёЄе›ћеЎ«е€° model е­—ж®µгЂ‚
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
                        // OFFICIAL / AUTO еЌЏи®®йѓЅдјљиµ°е®ж–№з«Їз‚№пј€AUTO з”ЁдЅњ fallbackпј‰пјЊPro йѓЅз”џж•€пј›зєЇ DEEPLX еЌЏи®®дё‹ Pro ж— ж„Џд№‰
                        enabled = deeplProtocol != com.gameocr.app.data.DeeplProtocol.DEEPLX
                    ) { deeplPro = it }
                    Text(
                        stringResource(R.string.settings_deepl_tip),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // вЂ”вЂ” й«зє§пј€и‡Єжћ¶ / deeplxпј‰вЂ”вЂ”
                    // жЉеЏ жЋ‰йЃїе…Ќеђ“е€°еЏЄз”Ёе®ж–№ DeepL зљ„з”Ёж€·пј›е±•ејЂжњ‰и‡Єе®љд№‰ URL + Bearer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { deeplAdvancedExpanded = !deeplAdvancedExpanded }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            (if (deeplAdvancedExpanded) "в–ј " else "в–¶ ") +
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
                            // DEEPLX / AUTO йѓЅз”Ё customTokenпјЊBearer ж‰Ќжњ‰ж„Џд№‰пј›OFFICIAL дёЌиЇ»
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
                    // зЃ«е±±еј•ж“Ћжњєе™Ёзї»иЇ‘пјљAK + SK + regionпј›SignV4 й‰ґжќѓ
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
                    // з™ѕеє¦зї»иЇ‘ејЂж”ѕе№іеЏ°пј€fanyi-api.baidu.comпј‰вЂ”вЂ” дёЋз™ѕеє¦ж™єиѓЅдє‘ OCR дёЌжЇдёЂе›ћдє‹
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
                    // и…ѕи®Їдє‘зї»иЇ‘пјљдёЋ OCR е…±з”ЁеђЊдёЂеҐ— SecretId/Key/Regionпј€state еЏЊеђ‘з»‘е®љпјЊ
                    // ењЁиї™й‡Њж”№е’ЊењЁ OCR еЊєж”№е®Ње…Ёз­‰д»·пј‰гЂ‚region й»и®¤ ap-guangzhouпјЊTMT еђ„ењ°еџџйЂљз”ЁгЂ‚
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
                    // GOOGLEпјљж—  keyпјЊд»…жЏђз¤єйЈЋй™©гЂ‚ж”№ else if жЋзЎ®еЊ№й…ЌвЂ”вЂ”йЃїе…ЌеђЋз»­ж–°еўћжћљдёѕпј€е¦‚ LOCAL_*пј‰
                    // иђЅе…Ґ else е…њеє•пјЊй”™иЇЇжѕз¤є Google ж–‡жЎ€гЂ‚
                    Text(
                        stringResource(R.string.settings_google_tip),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    }
                }

                // вЂ”вЂ” жµ‹иЇ•иїћжЋҐ вЂ”вЂ”
                // йЄЊиЇЃ baseUrl/key/modelпј€ж€– DeepL key/endpointпј‰иѓЅдёЌиѓЅз”Ёпј›DeepL йЎєдѕїиї”е›ће‰©дЅ™йўќеє¦пјЊ
                // OpenAI йЎєдѕїж‹‰ model е€—иЎЁе›ћеЎ«е€°дёЉж–№дё‹ж‹‰гЂ‚зЉ¶жЂЃж–‡е­—жЊ‰ж€ђеЉџ/е¤±иґҐзќЂи‰ІпјЊдё‹ж¬Ўз‚№е‡»и¦†з›–гЂ‚
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
                    // д№ђи§‚ж›ґж–°жњ¬ењ° + еј‚ж­ҐиђЅз›гЂ‚togglePinLanguage е†…йѓЁз”Ё repo.update жЇеЋџе­ђзљ„гЂ‚
                // Prompt / жµЃејЏејЂе…іеЏЄеЇ№ LLM з±»пј€OpenAI е…је®№пј‰зї»иЇ‘еј•ж“Ћжњ‰ж„Џд№‰пј›
                // DeepL жЇжњєе™Ёзї»иЇ‘ APIпјЊдёЌиЇ» promptгЂЃд№џдёЌиµ° SSEпјЊйљђи—ЏйЃїе…ЌиЇЇеЇјгЂ‚
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

            // вЂ”вЂ” OCR еј•ж“Ћ вЂ”вЂ”
            // з«Їе€°з«Їзї»иЇ‘еј•ж“Ћпј€жњ‰йЃ“е›ѕзї»пј‰дјљи·іиї‡ OCR й¶ж®µпјЊж•ґдёЄ OCR и®ѕзЅ®еЊєеЅ“е‰Ќдјљиў«ж— и§†вЂ”вЂ”
            // зЃ°жѕ + з¦Ѓз”Ё chip и®©з”Ёж€·дёЂзњјжЋз™Ѕ + дёЌиѓЅиЇЇж“ЌдЅњгЂ‚
            }


            if (translatorEngine != TranslatorEngine.REMOTE_PC) {

            }

            item(key = SectionKeys.TEXT_ORIENTATION) {
                textOrientationSection()
            }

            // вЂ”вЂ” жѕз¤є вЂ”вЂ”
            // йў„и§€жЇжњ¬ section з¬¬дёЂйЎ№пј›ж»љиї‡йЎµйќўйЎ¶йѓЁеђЋеђёй™„пјЊsection з¦»ејЂж—¶и‡ЄеЉЁи§Јй™¤гЂ‚
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

                // вЂ”вЂ” еЅ±е“Ќйў„и§€зљ„ж ·ејЏйЎ№ вЂ”вЂ”
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
                    // иѕ№жЎ†ж ·ејЏпјљд»…ењЁ CUSTOM дё»йўдё‹жѕз¤єгЂ‚SOLID/DASHED/DOTTED дёЂиЎЊпјЊDOUBLE/GROOVE дёЂиЎЊпј€йЃїејЂ ExperimentalLayoutApiпј‰гЂ‚
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

                // вЂ”вЂ” е‡ дЅ•йЎ№пј€йў„и§€зњ‹дёЌе€°пјЊеЏЄиѓЅе®ћй™…и§¦еЏ‘зї»иЇ‘ж—¶зњ‹е€°ж•€жћњпј‰вЂ”вЂ”
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
                    // ж‚¬жµ®зЄ—еЏЈе†…е®№еЅўжЂЃпјљеЋџж–‡+иЇ‘ж–‡ / д»…иЇ‘ж–‡гЂ‚з«‹еЌіз”џж•€пјЊдёЌиї› save жµЃзЁ‹гЂ‚
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

            // вЂ”вЂ” ж‚¬жµ®жЊ‰й’® вЂ”вЂ”
            }

            // вЂ”вЂ” еѕЄзЋЇи§¦еЏ‘е™Ё вЂ”вЂ”
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

            // вЂ”вЂ” ж‚¬жµ®жЊ‰й’® вЂ”вЂ”
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

            // вЂ”вЂ” еј§иЏњеЌ•жЊ‰й’®йЎєеєЏ вЂ”вЂ”
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

            // вЂ”вЂ” ејЂеЏ‘иЂ…иЇЉж–­ вЂ”вЂ”
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

            // вЂ”вЂ” зЅ‘з»њпј€е…Ёе±ЂпјЊи·Ё OCR / зї»иЇ‘пј‰вЂ”вЂ”
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

            }

            // з»™ FAB з•™е‡єеє•йѓЁз©єй—ґпјЊйЃїе…ЌжњЂеђЋдёЂйЎ№иў«йЃ®жЊЎ
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

            // жђњзґўдё‹ж‹‰пјљжµ®ењЁ Column д№‹дёЉгЂ‚еЊ№й…ЌйЎ№з‚№е‡»еђЋж»ље€°еЇ№еє” section йЎ¶йѓЁе№¶е…ій—­жђњзґўгЂ‚
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
                                            ).joinToString(" В· ")
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

internal val SEARCH_TARGET_APP_LANGUAGE = intArrayOf(R.string.settings_section_app_lang)
internal val SEARCH_TARGET_THEME_MODE = intArrayOf(R.string.settings_section_theme_mode)
internal val SEARCH_TARGET_PRESETS = intArrayOf(
    R.string.settings_section_translation_presets,
    R.string.settings_search_item_preset_transfer,
)
internal val SEARCH_TARGET_TRANSLATOR_ENGINE = intArrayOf(
    R.string.settings_search_item_translator_engine,
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
internal val SEARCH_TARGET_SOURCE_LANGUAGE = intArrayOf(R.string.settings_search_item_source_lang)
internal val SEARCH_TARGET_TARGET_LANGUAGE = intArrayOf(R.string.settings_search_item_target_lang)
internal val SEARCH_TARGET_TRANSLATION_ASSISTANCE = intArrayOf(
    R.string.settings_search_item_streaming,
    R.string.settings_search_item_empty_translation_retry,
    R.string.settings_foreground_app_detection,
    R.string.settings_send_app_name,
    R.string.settings_grant_usage_access,
)
internal val SEARCH_TARGET_PROMPTS = intArrayOf(
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
internal val SEARCH_TARGET_OVERLAY_DISPLAY = intArrayOf(
    R.string.settings_search_item_render_mode,
    R.string.settings_search_item_translation_block_interaction,
    R.string.settings_search_item_placement,
    R.string.settings_search_item_offset,
)
internal val SEARCH_TARGET_OVERLAY_THEME = intArrayOf(
    R.string.settings_search_item_overlay_theme,
    R.string.settings_search_item_custom_theme,
    R.string.settings_search_item_border_style,
)
internal val SEARCH_TARGET_OVERLAY_TEXT = intArrayOf(
    R.string.settings_search_item_text_size,
    R.string.settings_search_item_text_style,
    R.string.settings_search_item_overlay_font,
    R.string.settings_search_item_alpha,
)
internal val SEARCH_TARGET_OVERLAY_WINDOW = intArrayOf(
    R.string.settings_search_item_floating_window_content,
    R.string.settings_search_item_floating_window_locked,
    R.string.settings_search_item_floating_window_reset,
)
internal val SEARCH_TARGET_OVERLAY_LAYOUT = intArrayOf(
    R.string.settings_search_item_allow_wrap,
    R.string.settings_search_item_avoid_collision,
    R.string.settings_search_item_merge_adjacent,
    R.string.settings_search_item_merge_strength,
)
internal val SEARCH_TARGET_FLOATING = intArrayOf(
    R.string.settings_search_item_floating_size,
    R.string.settings_search_item_floating_snap,
    R.string.settings_search_item_floating_auto_dock,
    R.string.settings_search_item_floating_dock_inset,
)
internal val SEARCH_TARGET_ARC_MENU = intArrayOf(R.string.settings_search_item_arc_menu_order)
internal val SEARCH_TARGET_TRIGGER = intArrayOf(
    R.string.settings_search_item_loop_interval,
    R.string.settings_search_item_loop_trigger_mode,
    R.string.settings_search_item_loop_similarity,
    R.string.settings_search_item_loop_region,
    R.string.settings_search_item_a11y_volume,
)
internal val SEARCH_TARGET_DEVELOPER = intArrayOf(
    R.string.settings_search_item_developer_ocr,
    R.string.settings_search_item_cross_line_context,
)
internal val SEARCH_TARGET_NETWORK = intArrayOf(
    R.string.settings_search_item_api_timeout,
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
 * жђњзґўзґўеј•жќЎз›®гЂ‚sectionLabel/itemLabel иµ° res id и·џйљЏзі»з»џиЇ­иЁЂпј›keywords еђЊж—¶еЎћдё­и‹±ж–‡пјЊ
 * и®©з”Ёж€·з”Ёд»»дЅ•дёЂз§ЌиЇ­иЁЂжђњзґўйѓЅиѓЅе‘Ѕдё­пј€i18n еђЋз”Ёж€·еЏЇиѓЅд№ жѓЇиѕ“е…Ґе“Єз§ЌйѓЅиЇґдёЌе®љпј‰гЂ‚
 */
/** жЉЉ UI е¤љиЎЊиѕ“е…ҐжЎ†ж–‡жњ¬ж‹†ж€ђ host е€—иЎЁпјЊtrim жЇЏиЎЊгЂЃеЋ»з©єгЂ‚дїќе­ / snapshot еЇ№жЇ”йѓЅиµ°иї™й‡ЊдїќиЇЃдёЂи‡ґгЂ‚ */
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
 * и®ѕзЅ®йЎ№еЏЇжђњзґўзґўеј•гЂ‚ж–°еўћи®ѕзЅ®йЎ№ж—¶еђЊж­ҐеЉ дёЂиЎЊпј›еЊ№й…ЌеђЋи·іе€°ж‰ЂењЁ section йЎ¶йѓЁгЂ‚
 * keywords ж··еђ€дё­и‹±ж–‡пјљи‹±ж–‡зі»з»џдё‹з”Ёж€·з”Ёи‹±ж–‡иѕ“е…Ґд»ЌиѓЅжђње€°дё­ж–‡ section / еЏЌд№‹дє¦з„¶гЂ‚
 */
internal val SETTINGS_SEARCH_TRANSFER_KEYWORDS = listOf(
    "settings import", "settings export", "preset import", "preset export", "font backup",
    "configuration backup", "backup", "restore", "и®ѕзЅ®еЇје…Ґ", "и®ѕзЅ®еЇје‡є", "йў„и®ѕеЇје…Ґ",
    "йў„и®ѕеЇје‡є", "е­—дЅ“е¤‡д»Ѕ", "й…ЌзЅ®е¤‡д»Ѕ", "е¤‡д»Ѕ", "жЃўе¤Ќ",
)

internal val SETTINGS_SEARCH_COLOR_KEYWORDS = listOf(
    "custom", "color", "colour", "background color", "text color", "border color", "hue",
    "saturation", "brightness", "opacity", "border", "и‡Єе®љд№‰", "й…Ќи‰І", "йўњи‰І", "иѓЊж™Їи‰І",
    "ж–‡е­—и‰І", "ж–‡е­—йўњи‰І", "иѕ№жЎ†и‰І", "иѕ№жЎ†йўњи‰І", "и‰Із›ё", "йІњи‰іеє¦", "йҐ±е’Њеє¦", "дє®еє¦",
    "йЂЏжЋеє¦", "иѕ№жЎ†",
)

internal val SETTINGS_SEARCH_EMPTY_TRANSLATION_RETRY_KEYWORDS = listOf(
    "empty translation", "blank translation", "empty response", "blank response", "retry",
    "з©єиЇ‘ж–‡", "з©єзї»иЇ‘", "з©єе“Ќеє”", "и‡ЄеЉЁй‡ЌиЇ•", "й‡ЌиЇ•",
)

internal val SETTINGS_SEARCH_TRANSLATION_BLOCK_INTERACTION_KEYWORDS = listOf(
    "translation block", "copy translation", "long press", "selection handles", "select text", "copy panel",
    "tap translation", "иЇ‘ж–‡еќ—", "е¤Ќе€¶иЇ‘ж–‡", "й•їжЊ‰", "йЂ‰ж‹©жЉЉж‰‹", "йЂ‰ж‹©ж–‡е­—", "йЂ‰ж‹©е¤Ќе€¶", "з‚№е‡»иЇ‘ж–‡",
)

internal val SETTINGS_SEARCH_LOOP_SIMILARITY_KEYWORDS = listOf(
    "loop", "frame", "image", "hash", "similarity", "threshold", "skip", "duplicate",
    "еѕЄзЋЇ", "з”»йќў", "е›ѕз‰‡", "з›ёдјјеє¦", "й€еЂј", "еЋ»й‡Ќ", "и·іиї‡", "й‡Ќе¤Ќзї»иЇ‘",
)

internal val SETTINGS_SEARCH_LOOP_TRIGGER_KEYWORDS = listOf(
    "loop trigger", "fixed interval", "wait for text", "text complete", "text stability",
    "stable duration", "typing", "dialogue", "subtitle", "еѕЄзЋЇи§¦еЏ‘", "е›єе®љй—ґйљ”",
    "ж™єиѓЅз­‰еѕ…", "ж–‡е­—е®Њж€ђ", "ж–‡е­—зЁіе®љ", "зЁіе®љз­‰еѕ…", "жЉҐе№•", "еЇ№иЇќ", "е­—е№•",
)

internal val SETTINGS_SEARCH_LOOP_REGION_KEYWORDS = listOf(
    "dialogue region", "subtitle region", "text region", "lower screen", "anywhere",
    "region only", "translate all text", "жЉҐе№•еЊєеџџ", "е­—е№•еЊєеџџ", "ж–‡е­—еЊєеџџ", "дё‹еЌЉе±Џ",
    "е…Ёе±Џи‡Єз”±", "д»…зї»иЇ‘жЉҐе№•еЊєеџџ", "зї»иЇ‘е…ЁйѓЁж–‡е­—",
)

internal val SETTINGS_SEARCH_DEVELOPER_OCR_KEYWORDS = listOf(
    "developer", "developer mode", "debug", "diagnostic", "ocr box", "red box",
    "bounding box", "source text", "translation text", "screenshot", "save screenshot",
    "translation cache", "disable cache", "ејЂеЏ‘иЂ…", "ејЂеЏ‘иЂ…жЁЎејЏ", "и°ѓиЇ•", "иЇЉж–­",
    "OCR зєўжЎ†", "зєўжЎ†", "иѕ№з•ЊжЎ†", "еЋџж–‡", "иЇ‘ж–‡", "ж€Єе›ѕдїќе­", "зї»иЇ‘зј“е­", "з¦Ѓз”Ёзј“е­",
)

private val SETTING_ITEMS: List<SearchEntry> = listOf(
    SearchEntry(
        SectionKeys.TRANSLATE,
        R.string.settings_section_translator,
        R.string.settings_search_item_empty_translation_retry,
        SETTINGS_SEARCH_EMPTY_TRANSLATION_RETRY_KEYWORDS,
    ),
    SearchEntry(SectionKeys.PRESETS, R.string.settings_section_translation_presets, R.string.settings_section_translation_presets, listOf("preset", "presets", "profile", "mode", "зі»з»џйў„и®ѕж–№жЎ€", "зї»иЇ‘йў„и®ѕ", "йў„и®ѕ", "жЁЎејЏ")),
    SearchEntry(SectionKeys.PRESETS, R.string.settings_section_translation_presets, R.string.settings_search_item_preset_transfer, SETTINGS_SEARCH_TRANSFER_KEYWORDS),

    // вЂ”вЂ” зї»иЇ‘еђЋз«Ї вЂ”вЂ”
    SearchEntry(
        SectionKeys.TRANSLATE,
        R.string.settings_section_translator,
        R.string.settings_search_item_translator_engine,
        listOf("OpenAI", "DeepL", "LLM", "зї»иЇ‘еј•ж“Ћ"),
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
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_model_name, listOf("model", "жЁЎећ‹еђЌ"), requiredTranslatorEngine = TranslatorEngine.OPENAI),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_anthropic_base_url, listOf("anthropic", "claude", "messages api", "base url"), requiredTranslatorEngine = TranslatorEngine.ANTHROPIC),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_anthropic_api_key, listOf("anthropic", "claude", "x-api-key"), requiredTranslatorEngine = TranslatorEngine.ANTHROPIC),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_anthropic_model, listOf("anthropic", "claude", "model", "жЁЎећ‹еђЌ"), requiredTranslatorEngine = TranslatorEngine.ANTHROPIC),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_deepl_api_key, listOf("deepl"), requiredTranslatorEngine = TranslatorEngine.DEEPL),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_deepl_pro, listOf("deepl pro"), requiredTranslatorEngine = TranslatorEngine.DEEPL),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_deepl_advanced, listOf("deeplx", "bearer", "official", "protocol", "и‡Єжћ¶", "й«зє§", "еЌЏи®®", "deepl base url"), requiredTranslatorEngine = TranslatorEngine.DEEPL),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_youdao_pictrans, listOf("youdao", "жњ‰йЃ“", "е›ѕз‰‡зї»иЇ‘", "pictrans", "ocrtransapi", "з«Їе€°з«Ї"), requiredTranslatorEngine = TranslatorEngine.YOUDAO_PICTRANS),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_google, listOf("google", "и°·ж­Њ", "translate"), requiredTranslatorEngine = TranslatorEngine.GOOGLE),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_google_mlkit, listOf("google ml kit", "mlkit", "on-device", "offline", "з«Їдѕ§", "з¦»зєї"), requiredTranslatorEngine = TranslatorEngine.GOOGLE_ML_KIT),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_volc, listOf("volc", "volcengine", "зЃ«е±±", "е­—иЉ‚", "doubao", "bytedance", "access key", "AK", "SK", "region", "еЊєеџџ"), requiredTranslatorEngine = TranslatorEngine.VOLC),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_baidu_fanyi, listOf("baidu fanyi", "з™ѕеє¦зї»иЇ‘", "fanyi-api", "appid", "ејЂж”ѕе№іеЏ°"), requiredTranslatorEngine = TranslatorEngine.BAIDU_FANYI),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_tencent_translator, listOf("tencent", "и…ѕи®Ї", "tmt", "tmtcloud", "и…ѕи®Їдє‘зї»иЇ‘"), requiredTranslatorEngine = TranslatorEngine.TENCENT),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_source_lang, listOf("source", "жєђиЇ­иЁЂ")),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_target_lang, listOf("target", "з›®ж ‡иЇ­иЁЂ")),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_prompt, listOf("prompt", "жЏђз¤єиЇЌ", "system")),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_dictionary_prompt, listOf("dictionary", "иЇЌе…ё", "е€’иЇЌ", "word select", "phonetic", "йџіж ‡", "й‡Љд№‰", "definition", "prompt")),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_streaming, listOf("streaming", "жµЃејЏ")),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_cross_line_context, listOf("cross context", "cross line", "дёЉдё‹ж–‡", "и·ЁдёЉдё‹ж–‡", "ж®µиђЅ")),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_send_app_name, listOf("send app name", "prompt app context", "еЏ‘йЂЃеє”з”ЁеђЌз§°", "жЁЎећ‹еє”з”ЁеђЌз§°")),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_foreground_app_detection, listOf("app detection", "foreground app", "accessibility", "usage access", "еє”з”ЁиЇ†е€«", "е‰ЌеЏ°еє”з”Ё")),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_grant_usage_access, listOf("usage permission", "usage access", "permission", "дЅїз”Ёжѓ…е†µжќѓй™ђ", "дЅїз”Ёжѓ…е†µи®їй—®", "жЋ€жќѓ")),
    SearchEntry(
        SectionKeys.TEXT_ORIENTATION,
        R.string.settings_text_orientation_section_title,
        R.string.settings_translation_output_follow_title,
        listOf("follow recognition", "recognized layout", "и·џйљЏиЇ†е€«", "иЇ†е€«ж–‡е­—жЋ’е€—"),
        optionLabelResIds = listOf(R.string.settings_translation_output_follow),
    ),
    SearchEntry(
        SectionKeys.TEXT_ORIENTATION,
        R.string.settings_text_orientation_section_title,
        R.string.settings_translation_output_layout_label,
        listOf("output direction", "translation layout", "writing mode", "иЇ‘ж–‡ж–№еђ‘", "иЇ‘ж–‡жЋ’е€—"),
        optionLabelResIds = listOf(
            R.string.settings_translation_output_follow_title,
            R.string.settings_translation_output_follow,
            R.string.settings_translation_output_horizontal,
            R.string.settings_translation_output_vertical,
            R.string.settings_translation_output_ltr,
            R.string.settings_translation_output_rtl,
        ),
    ),

    SearchEntry(SectionKeys.TEXT_ORIENTATION, R.string.settings_text_orientation_section_title, R.string.settings_orient_auto_detect_title, listOf("orientation", "text orientation", "direction", "vertical", "horizontal", "и‡ЄеЉЁе€¤е€«", "ж–№еђ‘", "ж–‡жњ¬ж–№еђ‘", "з«–жЋ’", "жЁЄжЋ’")),
    SearchEntry(SectionKeys.TEXT_ORIENTATION, R.string.settings_text_orientation_section_title, R.string.settings_search_item_manual_orientation, listOf("manual", "lock", "orientation", "vertical", "horizontal", "stacked", "ж‰‹еЉЁ", "й”Ѓе®љ", "ж–№еђ‘", "з«–жЋ’", "жЁЄжЋ’", "йЂђе­—")),
    SearchEntry(SectionKeys.TEXT_ORIENTATION, R.string.settings_text_orientation_section_title, R.string.settings_search_item_orientation_model, listOf("orientation model", "doc orientation", "direction model", "ONNX", "ж–№еђ‘жЁЎећ‹", "ж–‡жњ¬ж–№еђ‘жЁЎећ‹", "жЁЎећ‹", "download", "дё‹иЅЅ", "жњ¬ењ°еЇје…Ґ", "local import", "еЇје…Ґ", "delete", "е€ й™¤")),

    // вЂ”вЂ” е›ѕеѓЏйў„е¤„зђ†пј€ењЁ OCR section е†…пј‰вЂ”вЂ”

    // вЂ”вЂ” жѕз¤є вЂ”вЂ”
    SearchEntry(
        SectionKeys.OVERLAY,
        R.string.settings_section_overlay,
        R.string.settings_search_item_render_mode,
        listOf("зґ§иґґ", "жЁЄе№…", "banner", "render", "display mode", "floating window", "ж‚¬жµ®зЄ—"),
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
        listOf("дё‹ж–№", "дёЉж–№", "и¦†з›–", "below", "above", "overlap", "placement"),
        optionLabelResIds = listOf(
            R.string.settings_placement_below_chip,
            R.string.settings_placement_overlap_chip,
            R.string.settings_placement_above_chip,
        ),
    ),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_offset, listOf("offset", "еѕ®и°ѓ")),
    SearchEntry(
        SectionKeys.OVERLAY,
        R.string.settings_section_overlay,
        R.string.settings_search_item_overlay_theme,
        listOf("ж·±и‰І", "жµ…и‰І", "зєёеј ", "йњњзЋ»з’ѓ", "зђҐзЏЂ", "theme", "dark", "light", "frost", "amber"),
        optionLabelResIds = listOf(
            R.string.settings_theme_classic_dark,
            R.string.settings_theme_amber_gold,
            R.string.settings_theme_paper_light,
            R.string.settings_theme_frost_glass,
            R.string.settings_theme_custom,
        ),
    ),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_custom_theme, SETTINGS_SEARCH_COLOR_KEYWORDS),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_border_style, listOf("solid", "dashed", "dotted", "double", "groove", "е®ћзєї", "и™љзєї", "з‚№зєї", "еЏЊзєї", "е‡№ж§Ѕ", "иѕ№жЎ†ж ·ејЏ")),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_text_size, listOf("font size", "е­—еЏ·", "е­—дЅ“е¤§е°Џ")),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_text_style, listOf("bold", "italic", "underline", "letter spacing", "line spacing", "alignment", "outline", "stroke", "shadow", "еЉ зІ—", "еЂѕж–њ", "дё‹е€’зєї", "е­—з¬¦й—ґи·ќ", "иЎЊи·ќ", "еЇ№йЅђ", "жЏЏиѕ№", "йґеЅ±")),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_overlay_font, listOf("font", "ttf", "е­—дЅ“", "и‡Єе®љд№‰е­—дЅ“", "иЇ‘ж–‡е­—дЅ“")),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_alpha, listOf("alpha", "opacity", "йЂЏжЋеє¦")),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_floating_window_content, listOf("floating window", "ж‚¬жµ®зЄ—", "еЋџж–‡+иЇ‘ж–‡", "д»…иЇ‘ж–‡", "src dst", "content mode")),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_floating_window_locked, listOf("lock", "й”Ѓе®љ", "ж‚¬жµ®зЄ—")),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_floating_window_reset, listOf("reset", "й‡ЌзЅ®", "иїеЋџ", "й»и®¤", "default", "floating window", "ж‚¬жµ®зЄ—", "geometry", "е‡ дЅ•", "дЅЌзЅ®", "е°єеЇё", "size")),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_allow_wrap, listOf("wrap", "жЌўиЎЊ", "single line", "е¤љиЎЊ")),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_avoid_collision, listOf("collision", "зў°ж’ћ", "йЃїж’ћ", "й‡ЌеЏ ")),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_merge_adjacent, listOf("merge", "еђ€е№¶", "й‡ЌеЏ ", "ж‹†ж®µ")),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_merge_strength, listOf("strength", "ејєеє¦", "дїќе®€", "ж ‡е‡†", "жїЂиї›", "conservative", "standard", "aggressive")),

    // вЂ”вЂ” ж‚¬жµ®жЊ‰й’® вЂ”вЂ”
    // жіЁж„Џпјљfloating_size еЋ†еЏІиЇЇжЊ‡ OVERLAYпјЊ0.3.x иµ·ж”№ж€ђ FLOATINGпј€е®ћй™…жЋ§д»¶ењЁ floating sectionпј‰гЂ‚
    SearchEntry(SectionKeys.FLOATING, R.string.settings_section_floating, R.string.settings_search_item_floating_size, listOf("floating", "ењ†зђѓ", "ж‚¬жµ®", "size", "е¤§е°Џ")),
    SearchEntry(SectionKeys.FLOATING, R.string.settings_section_floating, R.string.settings_search_item_floating_snap, listOf("snap", "иґґиѕ№", "edge")),
    SearchEntry(SectionKeys.FLOATING, R.string.settings_section_floating, R.string.settings_search_item_floating_auto_dock, listOf("auto dock", "и‡ЄеЉЁеЃњйќ ", "еЃњйќ ", "и—Џиѕ№")),
    SearchEntry(SectionKeys.FLOATING, R.string.settings_section_floating, R.string.settings_search_item_floating_dock_inset, listOf("inset", "иґґиѕ№и·ќз¦»", "ж‰‹еЉї", "е…Ёйќўе±Џ", "gesture")),

    // вЂ”вЂ” еј§иЏњеЌ•жЊ‰й’®йЎєеєЏ вЂ”вЂ”
    SearchEntry(SectionKeys.ARC_MENU, R.string.settings_section_arc_menu, R.string.settings_search_item_arc_menu_order, listOf("arc menu", "еј§иЏњеЌ•", "еј§еЅў", "йЎєеєЏ", "order", "reorder", "жЋ’еєЏ", "ж‹–еЉЁ", "menu", "жЊ‰й’®", "page", "page size", "е€†йЎµ", "жЇЏйЎµ", "зї»йЎµ", "loop", "region", "home", "skill", "жЉЂиѓЅ", "е€’иЇЌ", "language", "иЇ­иЁЂ", "жєђиЇ­иЁЂ", "з›®ж ‡иЇ­иЁЂ")),

    // вЂ”вЂ” е€’иЇЌзї»иЇ‘ вЂ”вЂ”

    // вЂ”вЂ” и§¦еЏ‘е™Ё вЂ”вЂ”
    SearchEntry(SectionKeys.TRIGGER, R.string.settings_section_trigger, R.string.settings_search_item_loop_interval, listOf("loop", "еѕЄзЋЇ", "interval", "й—ґйљ”")),
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
    SearchEntry(SectionKeys.TRIGGER, R.string.settings_section_trigger, R.string.settings_search_item_a11y_volume, listOf("ж— йљњзўЌ", "a11y", "accessibility", "volume", "йџій‡Џ")),

    // вЂ”вЂ” ејЂеЏ‘иЂ…иЇЉж–­ вЂ”вЂ”
    SearchEntry(
        SectionKeys.DEVELOPER,
        R.string.settings_section_developer,
        R.string.settings_search_item_developer_ocr,
        SETTINGS_SEARCH_DEVELOPER_OCR_KEYWORDS,
    ),

    // вЂ”вЂ” зЅ‘з»њ вЂ”вЂ”
    SearchEntry(SectionKeys.NETWORK, R.string.settings_section_network, R.string.settings_search_item_api_timeout, listOf("timeout", "и¶…ж—¶", "зЅ‘з»њ", "network")),
    SearchEntry(SectionKeys.NETWORK, R.string.settings_section_network, R.string.settings_search_item_cleartext_hosts, listOf("cleartext", "http", "жЋж–‡", "з™ЅеђЌеЌ•", "host", "и‡Єжћ¶", "з§Ѓжњ‰")),

    SearchEntry(SectionKeys.APP_LANG, R.string.settings_section_app_lang, R.string.settings_section_app_lang, listOf("language", "locale", "иЇ­иЁЂ", "дё­ж–‡", "english", "i18n")),

    SearchEntry(SectionKeys.THEME_MODE, R.string.settings_section_theme_mode, R.string.settings_section_theme_mode, listOf("theme", "е¤њй—ґ", "з™Ѕе¤©", "ж·±и‰І", "жµ…и‰І", "dark", "light", "night")),
)

internal fun settingsSearchSectionKeys(): Set<String> = SETTING_ITEMS.mapTo(linkedSetOf()) { it.sectionKey }
internal fun settingsSearchItemLabelResIds(): Set<Int> = SETTING_ITEMS.mapTo(linkedSetOf()) { it.itemLabelRes }
internal fun settingsSearchTargetResIds(): Set<Int> = SETTING_ITEMS.mapTo(linkedSetOf()) { it.targetId }
internal fun settingsSearchEntryIds(): Set<String> = SETTING_ITEMS.mapTo(linkedSetOf()) { it.entryId }
internal fun settingsSearchEntryCount(): Int = SETTING_ITEMS.size


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
    // еЅ’дёЂеЊ–зі»з»џиї”е›ћзљ„ BCP-47пј€"zh-Hans-CN" / "zh" / "en-US" з­‰пј‰е€° options й‡ЊзІѕзЎ® tagгЂ‚
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
            // и‡Єз®ЎжЊЃд№…еЊ–пјљMainActivity.attachBaseContext дјљењЁ recreate еђЋиЇ» prefs е№¶еЊ…иЈ…
            // Configuration localeпјЊз»•ејЂ AppCompatDelegate ењЁ ComponentActivity дёЉзљ„жЊЃд№…еЊ–дёЌзЁій—®йўгЂ‚
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
internal fun SettingsLinkCell(
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
