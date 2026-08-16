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
    // Р¶СљвЂ°Р№РѓвЂњР¶в„ўС”РґС”вЂРґС‘Р‚РµТђвЂ” keyРїСв‚¬OCR + РµвЂєС•Р·вЂ°вЂЎР·С—В»РёР‡вЂРµвЂ¦В±Р·вЂќРЃРїСвЂ°
    var youdaoAppKey by remember { mutableStateOf("") }
    var youdaoAppSecret by remember { mutableStateOf("") }
    // Р·РѓВ«РµВ±В±РµСвЂўР¶вЂњР‹Р¶СљС”Рµв„ўРЃР·С—В»РёР‡вЂ AK/SK + regionРїСв‚¬SignV4РїСвЂ°
    var volcAk by remember { mutableStateOf("") }
    var volcSk by remember { mutableStateOf("") }
    var volcRegion by remember { mutableStateOf("cn-north-1") }
    // Р·в„ўС•РµС”В¦Р·С—В»РёР‡вЂРµСР‚Р¶вЂќС•Рµв„–С–РµРЏВ° APPID + РµР‡вЂ Р№вЂ™ТђРїСв‚¬РґС‘Р‹Р·в„ўС•РµС”В¦Р¶в„ўС”РёС“Р…РґС”вЂ OCR РµВ®РЉРµвЂ¦РЃРґС‘РЊР¶ВР‡РґС‘Р‚РµвЂєС›РґС”вЂ№РїСвЂ°
    var baiduFanyiAppId by remember { mutableStateOf("") }
    var baiduFanyiSecret by remember { mutableStateOf("") }
    // Р·С—В»РёР‡вЂРµСвЂўР¶вЂњР‹"Р¶ВµвЂ№РёР‡вЂўРёС—С›Р¶Р‹Тђ"Р¶РЉвЂ°Р№вЂ™В®Р·С™вЂћР·С›В¬Р¶вЂ”В¶Р·Р‰В¶Р¶Р‚РѓРїСС™testing / Р·В»вЂњР¶С›СљР¶вЂ“вЂЎРµВ­вЂ” / Р¶в‚¬С’РµР‰СџРёвЂ°Р† / OpenAI Р¶вЂ№вЂ°Рµв‚¬В°Р·С™вЂћ model Рµв‚¬вЂ”РёРЋРЃРіР‚вЂљ
    // РґС‘РЊРёС—вЂє SettingsРїСРЉР·С”Р‡ UI Р·Р‰В¶Р¶Р‚РѓРїСвЂєРµв‚¬вЂЎР¶РЊСћ engine РґС‘РЊР¶С‘вЂ¦Р·В©С”РїСв‚¬Р·вЂќРЃР¶в‚¬В·Рµв‚¬вЂЎРµвЂєС›РµР‹В»РёС—ВРёС“Р…Р·СљвЂ№Рµв‚¬В°РґС‘Р‰Р¶В¬РЋР·С™вЂћР·В»вЂњР¶С›СљРїСвЂ°РіР‚вЂљ
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
    // РµСВ§РёРЏСљРµРЊвЂўР¶РЉвЂ°Р№вЂ™В®Р№РЋС”РµС”РЏ + Рµв‚¬вЂ™РёР‡РЊРёР‡РЊРµвЂ¦С‘ promptРїСС™Р¶вЂ№вЂ“РµР‰РЃ / Р·СвЂ“РёС•вЂРµС’Р‹РµРЊС–Р¶вЂ”В¶Р№Р‚С™РёС—вЂЎ vm Р·С™вЂћ saveArcMenuOrder / saveDictionaryPrompt
    // РµРЊвЂўРµВ­вЂ”Р¶В®ВµРёС’Р…Р·вЂєВРїСРЉ**РґС‘РЊ**РёВµВ°РґС‘В» save Р·С™вЂћ dirty Р¶ВµРѓР·РЃвЂ№РїСв‚¬Р·вЂќРЃР¶в‚¬В·Р¶СљСџР¶СљвЂєР·В«вЂ№Рµв‚¬В»Р·вЂќСџР¶вЂўв‚¬РїСРЉР¶вЂ”В Р№СљР‚Р·вЂљв„–РґС—СњРµВ­ВРїСвЂ°РіР‚вЂљ
    var menuOrder by remember { mutableStateOf<List<MenuItemId>>(emptyList()) }
    var arcMenuPageSize by remember { mutableStateOf(FloatingMenu.DEFAULT_PAGE_SIZE.toFloat()) }
    // РµР…вЂњРµвЂ°РЊРґС‘В»Р·С’С“Р¶Р‰Р‚РёС“Р…РіР‚вЂљР¶Р‰Р‚РёС“Р…Р¶В§Р…РїСв‚¬FULL_SCREEN_SKILLРїСвЂ°Р№вЂљР€РґС‘Р‚РёРЋРЉР·С™вЂћР¶вЂ“вЂЎР¶РЋв‚¬РёВ¦РѓРёВ·СџР·СњР‚РµВ®С“РµР‰РЃР¶Р‚РѓР¶ВС•Р·В¤С”РіР‚РЉРµв‚¬вЂЎРµв‚¬В°РµР‡в„–Р¶вЂ“в„–РіР‚РЊРїСС™
    // РµР…вЂњРµвЂ°РЊ FULL_SCREEN РІвЂ вЂ™ Р¶ВС•Р·В¤С”РіР‚РЉРІР‚вЂќ Рµв‚¬вЂ™РёР‡РЊР·С—В»РёР‡вЂРіР‚РЊРїСвЂєРµР…вЂњРµвЂ°РЊ WORD_SELECT РІвЂ вЂ™ Р¶ВС•Р·В¤С”РіР‚РЉРІР‚вЂќ РµвЂ¦РЃРµВ±РЏР·С—В»РёР‡вЂРіР‚РЊ
    var currentSkill by remember { mutableStateOf(com.gameocr.app.data.FloatingSkill.FULL_SCREEN) }
    var dictionaryPrompt by remember { mutableStateOf("") }
    // Р¶вЂљВ¬Р¶ВµВ®Р¶РЉвЂ°Р№вЂ™В®"РёТ‘Т‘РёС•в„–РёВ·СњР·В¦В»" slider Р·С™вЂћРµВ®С›Р¶вЂ”В¶Р№СћвЂћРёВ§в‚¬РїСС™РµВ±РЏРµв„–вЂўРґС‘В¤РґС•В§Р·вЂќВ» inset РµВ®Р…РµС”В¦Р·С™вЂћРµРЊР‰Р№Р‚РЏР·Р†вЂ°Р¶СњРЋРіР‚вЂљ
    // Р№В»ВРёВ®В¤ falseРІР‚вЂќРІР‚вЂќРёС—вЂєРёВ®С•Р·Р…В®РµВ°В±Р¶ВС•Р·В¤С”Р¶СњРЋРµС‘В¦РµВ¤Р„Р·Р„РѓРµвЂ¦Р‚РїСвЂєР·вЂќРЃР¶в‚¬В·РµСљРЃ slider Р¶вЂ”РѓР¶вЂ°вЂ№РµР‰РЃРµСР‚РµС’Р‡РіР‚РЉР№СћвЂћРёВ§в‚¬РіР‚РЊРµС’Р‹Р¶вЂ°РЊРёВ¦вЂ Р·вЂєвЂ“Рµв‚¬В°РµВ±РЏРµв„–вЂўРґС‘Р‰РіР‚вЂљ
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
    // Р¶ВР‹Р¶вЂ“вЂЎ HTTP Р·в„ўР…РµС’РЊРµРЊвЂўРїСС™Р·вЂќРЃР¶в‚¬В·Р¶Р‡РЏРёРЋРЉРґС‘Р‚РґС‘Р„ hostРїСРЉUI РґС‘Р‰Р·вЂќРЃ StringРїСРЉРґС—СњРµВ­ВР¶вЂ”В¶ split("\n")
    var cleartextHostsText by remember { mutableStateOf("") }
    // Р¶ВСџР¶В вЂЎРёР‡В­РёРЃР‚РїСС™Р¶СљВ¬РµСљВ°Р№вЂўСљРµС“РЏРіР‚вЂљtogglePinLanguage Р·В«вЂ№РµРЊС–РёС’Р…Р·вЂєВРїСРЉРґС‘вЂ№Р¶В¬РЋ ON_RESUME / load() Р¶вЂ№вЂ°РµвЂєС›Р¶СљР‚Р¶вЂ“В°РїСвЂє
    // РёС—в„ўР№вЂЎРЉРґв„–СџРґв„–С’РёВ§вЂљР¶вЂєТ‘Р¶вЂ“В°РґС‘Р‚РґВ»Р…Р¶СљВ¬РµСљВ°Р·Р‰В¶Р¶Р‚РѓРїСРЉUI Р·В«вЂ№Рµв‚¬В»РµРЏРЊР¶ВВ РіР‚вЂљ
    var pinnedLanguages by remember { mutableStateOf<List<String>>(emptyList()) }

    // dirty Р¶Р€Р‚Р¶ВµвЂ№РїСС™load Р¶вЂ”В¶ capture РґС‘Р‚РґВ»Р…Рµв‚¬СњРµВ§вЂ№ SettingsРїСРЉРґв„–вЂ№РµС’Р‹РёВ·Сџ buildSnapshot() Р¶Р‡вЂќ equalsРіР‚вЂљ
    // Р¶вЂ”В§Р·вЂ°в‚¬Р¶вЂ°вЂ№РµвЂ в„ўРґС‘В¤РґВ»Р… List<Any?>РїСРЉР¶Р‡РЏРµР‰В  Settings РµВ­вЂ”Р¶В®ВµР№С“Р…РёВ¦РѓРµСљРЃРґС‘В¤РґС‘Р„ list РµС’РЉР¶В­ТђРµР‰В РїСРЉРµРЏРЊРµВ¤РЊР·Р‰Р‡"РµС—ВР¶вЂќв„–РґС‘Р‚РёС•в„–"Р·С™вЂћ bugРіР‚вЂљ
    // Р·Р‹В°РµСљРЃР·вЂќРЃ data class equals РёвЂЎР„РµР‰РЃРёВ¦вЂ Р·вЂєвЂ“Р¶вЂ°Р‚Р¶СљвЂ°РµВ­вЂ”Р¶В®ВµРІР‚вЂќРІР‚вЂќРµР‰В РµВ­вЂ”Р¶В®ВµРµРЏР„Р¶вЂќв„– buildSnapshot() РґС‘Р‚РµВ¤вЂћРіР‚вЂљ
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

    // РІР‚вЂќРІР‚вЂќ Р¶С’СљР·Т‘СћРїСС™Р№РЋВ¶Р№С“РЃРёС•вЂњРµвЂ¦Тђ РІвЂ вЂ™ РґС‘вЂ№Р¶вЂ№вЂ°РµРЉв„–Р№вЂ¦РЊР№РЋв„– РІвЂ вЂ™ Р·вЂљв„–РµвЂЎВ» animateScrollTo Рµв‚¬В°РµР‡в„–РµС”вЂќ section Р№РЋВ¶Р№С“РЃ РІР‚вЂќРІР‚вЂќ
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

    // РґВ»Р‹РµВ®РЉР¶вЂўТ‘Р¶РЉРѓРґв„–вЂ¦РµРЉвЂ“РµС—В«Р·вЂ¦В§РёВµВ·Р¶В­ТђРїСРЉРµвЂ РЊРёВ¦вЂ Р·вЂєвЂ“Р¶СљВ¬Р№РЋВµРµВ°С™Р¶СљР„РґС—СњРµВ­ВР·С™вЂћРёРЊвЂ°Р·РЃС—РµР‚СРіР‚вЂљРґС‘РЊРёС“Р…РґВ»Р‹ Settings() Р№В»ВРёВ®В¤РµР‚СРёВµВ·Р¶В­ТђРїСРЉРµС’В¦Рµв‚¬в„ў
    // РµР‡СРµвЂЎС”РґСС™Р¶Р‰Р‰Р№СћвЂћРёВ®С•РіР‚РѓРµвЂєС”РµВ®С™РёР‡В­РёРЃР‚РіР‚РѓР¶вЂљВ¬Р¶ВµВ®Р·Р„вЂ”Р·Р‰В¶Р¶Р‚РѓРµвЂ™РЉР¶СљВ¬РµСљВ° LLM РµРЏвЂљР¶вЂўВ°Р·В­вЂ°Р№СњС›РµР…вЂњРµвЂ°РЊРёРЋРЃРµРЊвЂўРµВ­вЂ”Р¶В®ВµР№Сњв„ўР№В»ВР№вЂЎРЊР·Р…В®РіР‚вЂљ
    // Р·В±В»РµС›вЂ№РёР…В¬Р¶РЊСћРёВ·Сџ doSave РґС—СњР¶РЉРѓРґС‘Р‚РёвЂЎТ‘РїСв‚¬textSize.toInt() / loopInterval.toLongOrNull() Р·В­вЂ°РїСвЂ°РіР‚вЂљ
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

    // derivedStateOf РёВ®В© lambda РµСљРЃРґС•СњРёВµвЂ“ state РµРЏВРµРЉвЂ“Р¶вЂ”В¶Р¶вЂ°РЊР№вЂЎРЊР¶вЂ“В°РёВ®РЋР·В®вЂ” equals
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

    // Р¶С”С’РёР‡В­РёРЃР‚РІвЂ вЂќOCR РёРѓвЂќРµР‰РЃРїСС™Р¶Р€Р‚Р¶СџТђРёС“Р…РµС’В¦РёР‡вЂ Рµв‚¬В«РµР…вЂњРµвЂ°РЊР¶С”С’РёР‡В­РёРЃР‚РїСвЂєРґС‘РЊРёС“Р…Рµв‚¬в„ўР¶РЉвЂ°"Р·вЂќРЃР¶в‚¬В·Рµв‚¬С™РµР‰РЃР·С™вЂћР¶ВР‡РµвЂњР„РґС‘Р‚РёС•в„–"РµвЂ С–РµВ®С™Р¶Р‹РЃРёРЊС’Р¶вЂ“в„–РµС’вЂРіР‚вЂљ
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
        // suspend Р¶вЂњРЊРґР…СљРµС—вЂ¦Р№РЋВ»РµСљРЃ Snapshot РµСњвЂ”РµВ¤вЂ“РµРѓС™РµВ®РЉ
        val migratedPrompt = viewModel.migrateDefaultPromptIfStale(context)
        // РµвЂ¦С–Р№вЂќВ®Р¶Р‚В§РёС“Р…РїСС™Р¶Р‰Р‰ 40+ state РµвЂ в„ўРµвЂ¦ТђРµВ°РѓРёС—вЂєРµС’РЉРґС‘Р‚РґС‘Р„ mutable snapshotРїСРЉРµР‹СџРµВ­С’ apply РµС’Р‹РµРЏР„РёВ§В¦РµРЏвЂ
        // РґС‘Р‚Р¶В¬РЋ observer Р№Р‚С™Р·СџТђРїСРЉР№РѓС—РµвЂ¦РЊ Compose РµСљРЃР¶Р‡РЏРґС‘Р„ state РµРЏВРµРЉвЂ“Р¶вЂ”В¶ schedule РґС‘Р‚Р¶В¬РЋ recomposition
        // / derivedStateOf Р№вЂЎРЊР·В®вЂ”РїСРЉРёС—вЂєРёВ®С•Р·Р…В®Р№РЋВµР№вЂљР€Р¶В®Вµ"РµРЊРЋРґС‘Р‚РґС‘вЂ№"РґС‘В»РёВ¦РѓР¶СњТђРёвЂЎР„РёС—в„ўР№вЂЎРЉРіР‚вЂљ
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
            // РґС‘РЊР№ВВ»РµРЋС›РґС‘В»Р·С”С—Р·РЃвЂ№РїСС™file.exists() + file.length() РёВµВ° IO DispatcherРіР‚вЂљРµвЂ¦в‚¬Р·В»в„ўРµРЊВ РґР…РЊ
            // Р¶вЂ“вЂЎРµВ­вЂ”РїСРЉIO РµВ®РЉР¶в‚¬С’РµС’Р‹РµвЂ РЊРёВ¦вЂ Р·вЂєвЂ“РїСвЂєРёС—вЂєРёВ®С•Р·Р…В®Р·С™вЂћР·С›В¬Р№вЂ”Т‘РґС‘РЊРµРЊРЋР№РЋС—РіР‚вЂљ
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
                    // Р№ВР†Р¶Р‰В¤РїСС™load РµВ®РЉР¶в‚¬С’РµвЂ°РЊ state Р¶ВР‡Р№В»ВРёВ®В¤РµРЊВ РґР…РЊРµР‚СРїСРЉР¶В­В¤Р¶вЂ”В¶РґС—СњРµВ­ВРґСС™Р¶Р‰Р‰Р·В©С”РµВ­вЂ”Р·В¬В¦РґС‘Р† / Р№В»ВРёВ®В¤ enum
                    // РµвЂ в„ўРµвЂ¦Тђ DataStoreРїСРЉРёВ¦вЂ Р·вЂєвЂ“Р·вЂќРЃР¶в‚¬В·РµВ®С›Р№в„ўвЂ¦Р¶вЂўВ°Р¶РЊВ®РіР‚вЂљLaunchedEffect РµВ®РЉР¶в‚¬С’РїСв‚¬~13msРїСвЂ°Р¶вЂ°РЊР¶Р‰Р‰
                    // initialSettings РёВ®С•РµР‚СРїСРЉР№вЂљР€Рґв„–вЂ№РµС’Р‹Р¶вЂ°РЊРµвЂ¦РѓРёВ®С‘РґС—СњРµВ­ВРіР‚вЂљ
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
            // Р·вЂєТ‘Р¶Р‹Тђ inflate ColumnРІР‚вЂќРІР‚вЂќРґС‘РЊР¶ВС•Р·В¤С” spinnerРїСРЉР№РѓС—РµвЂ¦РЊ"Р¶РЉвЂ°РґС‘вЂ№РёВ®С•Р·Р…В® РІвЂ вЂ™ spinner РІвЂ вЂ™ UI"Р№вЂљР€Р¶В®ВµР·В©С”Р·в„ўР…РµРЊРЋР№РЋС—Р¶вЂћСџРіР‚вЂљ
            // state Р№В»ВРёВ®В¤РµР‚СРїСв‚¬Р·В©С”РµВ­вЂ”Р·В¬В¦РґС‘Р† / Р№В»ВРёВ®В¤ enumРїСвЂ°РґСС™РµвЂ¦в‚¬Р·СџВ­Р¶С™вЂљР¶ВС•Р·В¤С”РїСРЉLaunchedEffect РµСљРЃ ~13ms РµвЂ вЂ¦ Snapshot
            // РµР‹СџРµВ­С’Р¶вЂєТ‘Р¶вЂ“В°Р¶вЂ°Р‚Р¶СљвЂ° state Рµв‚¬В°РµВ®С›Р№в„ўвЂ¦РґС—СњРµВ­ВРµР‚СРІР‚вЂќРІР‚вЂќРёвЂљвЂ°Р·СљСРµвЂЎВ Рґв„–Р‹РґС‘РЊРµР‡СџРёВ§вЂ°Р№вЂ”Р„Р·С“РѓРіР‚вЂљРґВ»Р€РґВ»В·РїСС™Р·вЂќРЃР¶в‚¬В·РµСљРЃ initialSettings
            // РёС—ВР¶ВР‡ null Р¶вЂ”В¶Р·вЂљв„–РґС—СњРµВ­ВР¶РЉвЂ°Р№вЂ™В®РґСС™Р·вЂќРЃР№В»ВРёВ®В¤РµР‚СРёВ¦вЂ Р·вЂєвЂ“Р¶вЂўВ°Р¶РЊВ®РїСРЉР¶вЂ°Р‚РґВ»ТђРґС‘вЂ№Р№СњСћ FAB РµР‰В РґС”вЂ  enabled Р№ВР†Р¶Р‰В¤РіР‚вЂљ
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            // РІР‚вЂќРІР‚вЂќ РµС”вЂќР·вЂќРЃРёР‡В­РёРЃР‚ РІР‚вЂќРІР‚вЂќ
            item(key = SectionKeys.APP_LANG) {
                SettingsSearchTarget(searchTargetRegistry, *SEARCH_TARGET_APP_LANGUAGE) {
                SectionCard(title = stringResource(R.string.settings_section_app_lang)) {
                    AppLanguageSelector()
                }
                }
            }

            // РІР‚вЂќРІР‚вЂќ РґС‘В»Р№СћВР¶РЃРЋРµСРЏ РІР‚вЂќРІР‚вЂќ
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

            // РІР‚вЂќРІР‚вЂќ Р·С—В»РёР‡вЂРµС’Р‹Р·В«Р‡ РІР‚вЂќРІР‚вЂќ
            item(key = SectionKeys.TRANSLATE) {
            SectionCard(title = stringResource(R.string.settings_section_translator)) {
                // LEGACY_COMPAT: retain provider configuration state until provider cleanup,
                // but do not expose retired engine selection or provider options.
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
                    allowAuto = true,
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

            // РІР‚вЂќРІР‚вЂќ OCR РµСвЂўР¶вЂњР‹ РІР‚вЂќРІР‚вЂќ
            // Р·В«Р‡Рµв‚¬В°Р·В«Р‡Р·С—В»РёР‡вЂРµСвЂўР¶вЂњР‹РїСв‚¬Р¶СљвЂ°Р№РѓвЂњРµвЂєС•Р·С—В»РїСвЂ°РґСС™РёВ·С–РёС—вЂЎ OCR Р№ВВ¶Р¶В®ВµРїСРЉР¶вЂўТ‘РґС‘Р„ OCR РёВ®С•Р·Р…В®РµРЉС”РµР…вЂњРµвЂ°РЊРґСС™РёСћВ«Р¶вЂ”В РёВ§вЂ РІР‚вЂќРІР‚вЂќ
            // Р·РѓВ°Р¶ВС• + Р·В¦РѓР·вЂќРЃ chip РёВ®В©Р·вЂќРЃР¶в‚¬В·РґС‘Р‚Р·СљСР¶ВР‹Р·в„ўР… + РґС‘РЊРёС“Р…РёР‡Р‡Р¶вЂњРЊРґР…СљРіР‚вЂљ
            }


            item(key = SectionKeys.TEXT_ORIENTATION) {
                textOrientationSection()
            }

            // РІР‚вЂќРІР‚вЂќ Р¶ВС•Р·В¤С” РІР‚вЂќРІР‚вЂќ
            // Р№СћвЂћРёВ§в‚¬Р¶ВР‡Р¶СљВ¬ section Р·В¬В¬РґС‘Р‚Р№РЋв„–РїСвЂєР¶В»С™РёС—вЂЎР№РЋВµР№СњСћР№РЋВ¶Р№С“РЃРµС’Р‹РµС’С‘Р№в„ўвЂћРїСРЉsection Р·В¦В»РµСР‚Р¶вЂ”В¶РёвЂЎР„РµР‰РЃРёВ§Р€Р№в„ўВ¤РіР‚вЂљ
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

                // РІР‚вЂќРІР‚вЂќ РµР…В±РµвЂњРЊР№СћвЂћРёВ§в‚¬Р·С™вЂћР¶В В·РµСРЏР№РЋв„– РІР‚вЂќРІР‚вЂќ
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
                    // РёС•в„–Р¶РЋвЂ Р¶В В·РµСРЏРїСС™РґВ»вЂ¦РµСљРЃ CUSTOM РґС‘В»Р№СћВРґС‘вЂ№Р¶ВС•Р·В¤С”РіР‚вЂљSOLID/DASHED/DOTTED РґС‘Р‚РёРЋРЉРїСРЉDOUBLE/GROOVE РґС‘Р‚РёРЋРЉРїСв‚¬Р№РѓС—РµСР‚ ExperimentalLayoutApiРїСвЂ°РіР‚вЂљ
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

                // РІР‚вЂќРІР‚вЂќ РµвЂЎВ РґР…вЂўР№РЋв„–РїСв‚¬Р№СћвЂћРёВ§в‚¬Р·СљвЂ№РґС‘РЊРµв‚¬В°РїСРЉРµРЏР„РёС“Р…РµВ®С›Р№в„ўвЂ¦РёВ§В¦РµРЏвЂР·С—В»РёР‡вЂР¶вЂ”В¶Р·СљвЂ№Рµв‚¬В°Р¶вЂўв‚¬Р¶С›СљРїСвЂ°РІР‚вЂќРІР‚вЂќ
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
                    // Р¶вЂљВ¬Р¶ВµВ®Р·Р„вЂ”РµРЏР€РµвЂ вЂ¦РµВ®в„–РµР…СћР¶Р‚РѓРїСС™РµР‹СџР¶вЂ“вЂЎ+РёР‡вЂР¶вЂ“вЂЎ / РґВ»вЂ¦РёР‡вЂР¶вЂ“вЂЎРіР‚вЂљР·В«вЂ№РµРЊС–Р·вЂќСџР¶вЂўв‚¬РїСРЉРґС‘РЊРёС—вЂє save Р¶ВµРѓР·РЃвЂ№РіР‚вЂљ
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

            // РІР‚вЂќРІР‚вЂќ Р¶вЂљВ¬Р¶ВµВ®Р¶РЉвЂ°Р№вЂ™В® РІР‚вЂќРІР‚вЂќ
            }

            // РІР‚вЂќРІР‚вЂќ РµС•Р„Р·Р‹Р‡РёВ§В¦РµРЏвЂРµв„ўРЃ РІР‚вЂќРІР‚вЂќ
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

            // РІР‚вЂќРІР‚вЂќ Р¶вЂљВ¬Р¶ВµВ®Р¶РЉвЂ°Р№вЂ™В® РІР‚вЂќРІР‚вЂќ
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

            // РІР‚вЂќРІР‚вЂќ РµСВ§РёРЏСљРµРЊвЂўР¶РЉвЂ°Р№вЂ™В®Р№РЋС”РµС”РЏ РІР‚вЂќРІР‚вЂќ
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

            // РІР‚вЂќРІР‚вЂќ РµСР‚РµРЏвЂРёР‚вЂ¦РёР‡Р‰Р¶вЂ“В­ РІР‚вЂќРІР‚вЂќ
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

            // РІР‚вЂќРІР‚вЂќ Р·Р…вЂР·В»СљРїСв‚¬РµвЂ¦РЃРµВ±Р‚РїСРЉРёВ·РЃ OCR / Р·С—В»РёР‡вЂРїСвЂ°РІР‚вЂќРІР‚вЂќ
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

            // Р·В»в„ў FAB Р·вЂўв„ўРµвЂЎС”РµС”вЂўР№С“РЃР·В©С”Р№вЂ”Т‘РїСРЉР№РѓС—РµвЂ¦РЊР¶СљР‚РµС’Р‹РґС‘Р‚Р№РЋв„–РёСћВ«Р№РѓВ®Р¶РЉРЋ
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

            // Р¶С’СљР·Т‘СћРґС‘вЂ№Р¶вЂ№вЂ°РїСС™Р¶ВµВ®РµСљРЃ Column Рґв„–вЂ№РґС‘Р‰РіР‚вЂљРµРЉв„–Р№вЂ¦РЊР№РЋв„–Р·вЂљв„–РµвЂЎВ»РµС’Р‹Р¶В»С™Рµв‚¬В°РµР‡в„–РµС”вЂќ section Р№РЋВ¶Р№С“РЃРµв„–В¶РµвЂ¦С–Р№вЂ”В­Р¶С’СљР·Т‘СћРіР‚вЂљ
            if (searchActive && searchQuery.isNotBlank()) {
                val searchCurrentValues = mapOf(
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
                                            ).joinToString(" Р’В· ")
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
 * Р¶С’СљР·Т‘СћР·Т‘СћРµСвЂўР¶СњРЋР·вЂєВ®РіР‚вЂљsectionLabel/itemLabel РёВµВ° res id РёВ·СџР№С™РЏР·С–В»Р·В»СџРёР‡В­РёРЃР‚РїСвЂєkeywords РµС’РЉР¶вЂ”В¶РµРЋС›РґС‘В­РёвЂ№В±Р¶вЂ“вЂЎРїСРЉ
 * РёВ®В©Р·вЂќРЃР¶в‚¬В·Р·вЂќРЃРґВ»В»РґР…вЂўРґС‘Р‚Р·В§РЊРёР‡В­РёРЃР‚Р¶С’СљР·Т‘СћР№С“Р…РёС“Р…РµвЂР…РґС‘В­РїСв‚¬i18n РµС’Р‹Р·вЂќРЃР¶в‚¬В·РµРЏР‡РёС“Р…Рґв„–В Р¶С“Р‡РёС•вЂњРµвЂ¦ТђРµвЂњР„Р·В§РЊР№С“Р…РёР‡Т‘РґС‘РЊРµВ®С™РїСвЂ°РіР‚вЂљ
 */
/** Р¶Р‰Р‰ UI РµВ¤С™РёРЋРЉРёС•вЂњРµвЂ¦ТђР¶РЋвЂ Р¶вЂ“вЂЎР¶СљВ¬Р¶вЂ№вЂ Р¶в‚¬С’ host Рµв‚¬вЂ”РёРЋРЃРїСРЉtrim Р¶Р‡РЏРёРЋРЉРіР‚РѓРµР‹В»Р·В©С”РіР‚вЂљРґС—СњРµВ­В / snapshot РµР‡в„–Р¶Р‡вЂќР№С“Р…РёВµВ°РёС—в„ўР№вЂЎРЉРґС—СњРёР‡РѓРґС‘Р‚РёвЂЎТ‘РіР‚вЂљ */
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
 * РёВ®С•Р·Р…В®Р№РЋв„–РµРЏР‡Р¶С’СљР·Т‘СћР·Т‘СћРµСвЂўРіР‚вЂљР¶вЂ“В°РµСћС›РёВ®С•Р·Р…В®Р№РЋв„–Р¶вЂ”В¶РµС’РЉР¶В­ТђРµР‰В РґС‘Р‚РёРЋРЉРїСвЂєРµРЉв„–Р№вЂ¦РЊРµС’Р‹РёВ·С–Рµв‚¬В°Р¶вЂ°Р‚РµСљРЃ section Р№РЋВ¶Р№С“РЃРіР‚вЂљ
 * keywords Р¶В·В·РµС’в‚¬РґС‘В­РёвЂ№В±Р¶вЂ“вЂЎРїСС™РёвЂ№В±Р¶вЂ“вЂЎР·С–В»Р·В»СџРґС‘вЂ№Р·вЂќРЃР¶в‚¬В·Р·вЂќРЃРёвЂ№В±Р¶вЂ“вЂЎРёС•вЂњРµвЂ¦ТђРґВ»РЊРёС“Р…Р¶С’СљРµв‚¬В°РґС‘В­Р¶вЂ“вЂЎ section / РµРЏРЊРґв„–вЂ№РґС”В¦Р·вЂћВ¶РіР‚вЂљ
 */
internal val SETTINGS_SEARCH_TRANSFER_KEYWORDS = listOf(
    "settings import", "settings export", "preset import", "preset export", "font backup",
    "configuration backup", "backup", "restore", "РёВ®С•Р·Р…В®РµР‡СРµвЂ¦Тђ", "РёВ®С•Р·Р…В®РµР‡СРµвЂЎС”", "Р№СћвЂћРёВ®С•РµР‡СРµвЂ¦Тђ",
    "Р№СћвЂћРёВ®С•РµР‡СРµвЂЎС”", "РµВ­вЂ”РґР…вЂњРµВ¤вЂЎРґВ»Р…", "Р№вЂ¦РЊР·Р…В®РµВ¤вЂЎРґВ»Р…", "РµВ¤вЂЎРґВ»Р…", "Р¶РѓСћРµВ¤РЊ",
)

internal val SETTINGS_SEARCH_COLOR_KEYWORDS = listOf(
    "custom", "color", "colour", "background color", "text color", "border color", "hue",
    "saturation", "brightness", "opacity", "border", "РёвЂЎР„РµВ®С™Рґв„–вЂ°", "Р№вЂ¦РЊРёвЂ°Р†", "Р№СћСљРёвЂ°Р†", "РёС“РЉР¶в„ўР‡РёвЂ°Р†",
    "Р¶вЂ“вЂЎРµВ­вЂ”РёвЂ°Р†", "Р¶вЂ“вЂЎРµВ­вЂ”Р№СћСљРёвЂ°Р†", "РёС•в„–Р¶РЋвЂ РёвЂ°Р†", "РёС•в„–Р¶РЋвЂ Р№СћСљРёвЂ°Р†", "РёвЂ°Р†Р·вЂєС‘", "Р№Р†СљРёвЂ°С–РµС”В¦", "Р№ТђВ±РµвЂ™РЉРµС”В¦", "РґС”В®РµС”В¦",
    "Р№Р‚РЏР¶ВР‹РµС”В¦", "РёС•в„–Р¶РЋвЂ ",
)

internal val SETTINGS_SEARCH_EMPTY_TRANSLATION_RETRY_KEYWORDS = listOf(
    "empty translation", "blank translation", "empty response", "blank response", "retry",
    "Р·В©С”РёР‡вЂР¶вЂ“вЂЎ", "Р·В©С”Р·С—В»РёР‡вЂ", "Р·В©С”РµвЂњРЊРµС”вЂќ", "РёвЂЎР„РµР‰РЃР№вЂЎРЊРёР‡вЂў", "Р№вЂЎРЊРёР‡вЂў",
)

internal val SETTINGS_SEARCH_TRANSLATION_BLOCK_INTERACTION_KEYWORDS = listOf(
    "translation block", "copy translation", "long press", "selection handles", "select text", "copy panel",
    "tap translation", "РёР‡вЂР¶вЂ“вЂЎРµСњвЂ”", "РµВ¤РЊРµв‚¬В¶РёР‡вЂР¶вЂ“вЂЎ", "Р№вЂўС—Р¶РЉвЂ°", "Р№Р‚вЂ°Р¶вЂ№В©Р¶Р‰Р‰Р¶вЂ°вЂ№", "Р№Р‚вЂ°Р¶вЂ№В©Р¶вЂ“вЂЎРµВ­вЂ”", "Р№Р‚вЂ°Р¶вЂ№В©РµВ¤РЊРµв‚¬В¶", "Р·вЂљв„–РµвЂЎВ»РёР‡вЂР¶вЂ“вЂЎ",
)

internal val SETTINGS_SEARCH_LOOP_SIMILARITY_KEYWORDS = listOf(
    "loop", "frame", "image", "hash", "similarity", "threshold", "skip", "duplicate",
    "РµС•Р„Р·Р‹Р‡", "Р·вЂќВ»Р№СњСћ", "РµвЂєС•Р·вЂ°вЂЎ", "Р·вЂєС‘РґССРµС”В¦", "Р№Вв‚¬РµР‚С", "РµР‹В»Р№вЂЎРЊ", "РёВ·С–РёС—вЂЎ", "Р№вЂЎРЊРµВ¤РЊР·С—В»РёР‡вЂ",
)

internal val SETTINGS_SEARCH_LOOP_TRIGGER_KEYWORDS = listOf(
    "loop trigger", "fixed interval", "wait for text", "text complete", "text stability",
    "stable duration", "typing", "dialogue", "subtitle", "РµС•Р„Р·Р‹Р‡РёВ§В¦РµРЏвЂ", "РµвЂєС”РµВ®С™Р№вЂ”Т‘Р№С™вЂќ",
    "Р¶в„ўС”РёС“Р…Р·В­вЂ°РµС•вЂ¦", "Р¶вЂ“вЂЎРµВ­вЂ”РµВ®РЉР¶в‚¬С’", "Р¶вЂ“вЂЎРµВ­вЂ”Р·РЃС–РµВ®С™", "Р·РЃС–РµВ®С™Р·В­вЂ°РµС•вЂ¦", "Р¶Р‰ТђРµв„–вЂў", "РµР‡в„–РёР‡Сњ", "РµВ­вЂ”Рµв„–вЂў",
)

internal val SETTINGS_SEARCH_LOOP_REGION_KEYWORDS = listOf(
    "dialogue region", "subtitle region", "text region", "lower screen", "anywhere",
    "region only", "translate all text", "Р¶Р‰ТђРµв„–вЂўРµРЉС”РµСџСџ", "РµВ­вЂ”Рµв„–вЂўРµРЉС”РµСџСџ", "Р¶вЂ“вЂЎРµВ­вЂ”РµРЉС”РµСџСџ", "РґС‘вЂ№РµРЊР‰РµВ±РЏ",
    "РµвЂ¦РЃРµВ±РЏРёвЂЎР„Р·вЂќВ±", "РґВ»вЂ¦Р·С—В»РёР‡вЂР¶Р‰ТђРµв„–вЂўРµРЉС”РµСџСџ", "Р·С—В»РёР‡вЂРµвЂ¦РЃР№С“РЃР¶вЂ“вЂЎРµВ­вЂ”",
)

internal val SETTINGS_SEARCH_DEVELOPER_OCR_KEYWORDS = listOf(
    "developer", "developer mode", "debug", "diagnostic", "ocr box", "red box",
    "bounding box", "source text", "translation text", "screenshot", "save screenshot",
    "translation cache", "disable cache", "РµСР‚РµРЏвЂРёР‚вЂ¦", "РµСР‚РµРЏвЂРёР‚вЂ¦Р¶РЃРЋРµСРЏ", "РёВ°С“РёР‡вЂў", "РёР‡Р‰Р¶вЂ“В­",
    "OCR Р·С”СћР¶РЋвЂ ", "Р·С”СћР¶РЋвЂ ", "РёС•в„–Р·вЂўРЉР¶РЋвЂ ", "РµР‹СџР¶вЂ“вЂЎ", "РёР‡вЂР¶вЂ“вЂЎ", "Р¶в‚¬Р„РµвЂєС•РґС—СњРµВ­В", "Р·С—В»РёР‡вЂР·СвЂњРµВ­В", "Р·В¦РѓР·вЂќРЃР·СвЂњРµВ­В",
)

private val SETTING_ITEMS: List<SearchEntry> = listOf(
    SearchEntry(
        SectionKeys.TRANSLATE,
        R.string.settings_section_translator,
        R.string.settings_search_item_empty_translation_retry,
        SETTINGS_SEARCH_EMPTY_TRANSLATION_RETRY_KEYWORDS,
    ),
    SearchEntry(SectionKeys.PRESETS, R.string.settings_section_translation_presets, R.string.settings_section_translation_presets, listOf("preset", "presets", "profile", "mode", "Р·С–В»Р·В»СџР№СћвЂћРёВ®С•Р¶вЂ“в„–Р¶РЋв‚¬", "Р·С—В»РёР‡вЂР№СћвЂћРёВ®С•", "Р№СћвЂћРёВ®С•", "Р¶РЃРЋРµСРЏ")),
    SearchEntry(SectionKeys.PRESETS, R.string.settings_section_translation_presets, R.string.settings_search_item_preset_transfer, SETTINGS_SEARCH_TRANSFER_KEYWORDS),

    // РІР‚вЂќРІР‚вЂќ Р·С—В»РёР‡вЂРµС’Р‹Р·В«Р‡ РІР‚вЂќРІР‚вЂќ
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_base_url, listOf("base url"), requiredTranslatorEngine = TranslatorEngine.OPENAI),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_api_key, listOf("api key"), requiredTranslatorEngine = TranslatorEngine.OPENAI),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_model_name, listOf("model", "Р¶РЃРЋРµС›вЂ№РµС’РЊ"), requiredTranslatorEngine = TranslatorEngine.OPENAI),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_anthropic_base_url, listOf("anthropic", "claude", "messages api", "base url"), requiredTranslatorEngine = TranslatorEngine.ANTHROPIC),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_anthropic_api_key, listOf("anthropic", "claude", "x-api-key"), requiredTranslatorEngine = TranslatorEngine.ANTHROPIC),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_anthropic_model, listOf("anthropic", "claude", "model", "Р¶РЃРЋРµС›вЂ№РµС’РЊ"), requiredTranslatorEngine = TranslatorEngine.ANTHROPIC),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_deepl_api_key, listOf("deepl"), requiredTranslatorEngine = TranslatorEngine.DEEPL),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_deepl_pro, listOf("deepl pro"), requiredTranslatorEngine = TranslatorEngine.DEEPL),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_deepl_advanced, listOf("deeplx", "bearer", "official", "protocol", "РёвЂЎР„Р¶С›В¶", "Р№В«ВР·С”В§", "РµРЊРЏРёВ®В®", "deepl base url"), requiredTranslatorEngine = TranslatorEngine.DEEPL),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_youdao_pictrans, listOf("youdao", "Р¶СљвЂ°Р№РѓвЂњ", "РµвЂєС•Р·вЂ°вЂЎР·С—В»РёР‡вЂ", "pictrans", "ocrtransapi", "Р·В«Р‡Рµв‚¬В°Р·В«Р‡"), requiredTranslatorEngine = TranslatorEngine.YOUDAO_PICTRANS),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_google, listOf("google", "РёВ°В·Р¶В­РЉ", "translate"), requiredTranslatorEngine = TranslatorEngine.GOOGLE),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_google_mlkit, listOf("google ml kit", "mlkit", "on-device", "offline", "Р·В«Р‡РґС•В§", "Р·В¦В»Р·С”С—"), requiredTranslatorEngine = TranslatorEngine.GOOGLE_ML_KIT),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_volc, listOf("volc", "volcengine", "Р·РѓВ«РµВ±В±", "РµВ­вЂ”РёР‰вЂљ", "doubao", "bytedance", "access key", "AK", "SK", "region", "РµРЉС”РµСџСџ"), requiredTranslatorEngine = TranslatorEngine.VOLC),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_baidu_fanyi, listOf("baidu fanyi", "Р·в„ўС•РµС”В¦Р·С—В»РёР‡вЂ", "fanyi-api", "appid", "РµСР‚Р¶вЂќС•Рµв„–С–РµРЏВ°"), requiredTranslatorEngine = TranslatorEngine.BAIDU_FANYI),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_tencent_translator, listOf("tencent", "РёвЂ¦С•РёВ®Р‡", "tmt", "tmtcloud", "РёвЂ¦С•РёВ®Р‡РґС”вЂР·С—В»РёР‡вЂ"), requiredTranslatorEngine = TranslatorEngine.TENCENT),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_source_lang, listOf("source", "Р¶С”С’РёР‡В­РёРЃР‚")),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_target_lang, listOf("target", "Р·вЂєВ®Р¶В вЂЎРёР‡В­РёРЃР‚")),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_prompt, listOf("prompt", "Р¶РЏС’Р·В¤С”РёР‡РЊ", "system")),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_dictionary_prompt, listOf("dictionary", "РёР‡РЊРµвЂ¦С‘", "Рµв‚¬вЂ™РёР‡РЊ", "word select", "phonetic", "Р№СџС–Р¶В вЂЎ", "Р№вЂЎР‰Рґв„–вЂ°", "definition", "prompt")),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_streaming, listOf("streaming", "Р¶ВµРѓРµСРЏ")),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_search_item_cross_line_context, listOf("cross context", "cross line", "РґС‘Р‰РґС‘вЂ№Р¶вЂ“вЂЎ", "РёВ·РЃРґС‘Р‰РґС‘вЂ№Р¶вЂ“вЂЎ", "Р¶В®ВµРёС’Р…")),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_send_app_name, listOf("send app name", "prompt app context", "РµРЏвЂР№Р‚РѓРµС”вЂќР·вЂќРЃРµС’РЊР·В§В°", "Р¶РЃРЋРµС›вЂ№РµС”вЂќР·вЂќРЃРµС’РЊР·В§В°")),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_foreground_app_detection, listOf("app detection", "foreground app", "accessibility", "usage access", "РµС”вЂќР·вЂќРЃРёР‡вЂ Рµв‚¬В«", "РµвЂ°РЊРµРЏВ°РµС”вЂќР·вЂќРЃ")),
    SearchEntry(SectionKeys.TRANSLATE, R.string.settings_section_translator, R.string.settings_grant_usage_access, listOf("usage permission", "usage access", "permission", "РґР…С—Р·вЂќРЃР¶С“вЂ¦РµвЂ ВµР¶СњС“Р№в„ўС’", "РґР…С—Р·вЂќРЃР¶С“вЂ¦РµвЂ ВµРёВ®С—Р№вЂ”В®", "Р¶Р‹в‚¬Р¶СњС“")),
    SearchEntry(
        SectionKeys.TEXT_ORIENTATION,
        R.string.settings_text_orientation_section_title,
        R.string.settings_translation_output_follow_title,
        listOf("follow recognition", "recognized layout", "РёВ·СџР№С™РЏРёР‡вЂ Рµв‚¬В«", "РёР‡вЂ Рµв‚¬В«Р¶вЂ“вЂЎРµВ­вЂ”Р¶Р‹вЂ™Рµв‚¬вЂ”"),
        optionLabelResIds = listOf(R.string.settings_translation_output_follow),
    ),
    SearchEntry(
        SectionKeys.TEXT_ORIENTATION,
        R.string.settings_text_orientation_section_title,
        R.string.settings_translation_output_layout_label,
        listOf("output direction", "translation layout", "writing mode", "РёР‡вЂР¶вЂ“вЂЎР¶вЂ“в„–РµС’вЂ", "РёР‡вЂР¶вЂ“вЂЎР¶Р‹вЂ™Рµв‚¬вЂ”"),
        optionLabelResIds = listOf(
            R.string.settings_translation_output_follow_title,
            R.string.settings_translation_output_follow,
            R.string.settings_translation_output_horizontal,
            R.string.settings_translation_output_vertical,
            R.string.settings_translation_output_ltr,
            R.string.settings_translation_output_rtl,
        ),
    ),

    SearchEntry(SectionKeys.TEXT_ORIENTATION, R.string.settings_text_orientation_section_title, R.string.settings_orient_auto_detect_title, listOf("orientation", "text orientation", "direction", "vertical", "horizontal", "РёвЂЎР„РµР‰РЃРµв‚¬В¤Рµв‚¬В«", "Р¶вЂ“в„–РµС’вЂ", "Р¶вЂ“вЂЎР¶СљВ¬Р¶вЂ“в„–РµС’вЂ", "Р·В«вЂ“Р¶Р‹вЂ™", "Р¶РЃР„Р¶Р‹вЂ™")),
    SearchEntry(SectionKeys.TEXT_ORIENTATION, R.string.settings_text_orientation_section_title, R.string.settings_search_item_manual_orientation, listOf("manual", "lock", "orientation", "vertical", "horizontal", "stacked", "Р¶вЂ°вЂ№РµР‰РЃ", "Р№вЂќРѓРµВ®С™", "Р¶вЂ“в„–РµС’вЂ", "Р·В«вЂ“Р¶Р‹вЂ™", "Р¶РЃР„Р¶Р‹вЂ™", "Р№Р‚С’РµВ­вЂ”")),
    SearchEntry(SectionKeys.TEXT_ORIENTATION, R.string.settings_text_orientation_section_title, R.string.settings_search_item_orientation_model, listOf("orientation model", "doc orientation", "direction model", "ONNX", "Р¶вЂ“в„–РµС’вЂР¶РЃРЋРµС›вЂ№", "Р¶вЂ“вЂЎР¶СљВ¬Р¶вЂ“в„–РµС’вЂР¶РЃРЋРµС›вЂ№", "Р¶РЃРЋРµС›вЂ№", "download", "РґС‘вЂ№РёР…Р…", "Р¶СљВ¬РµСљВ°РµР‡СРµвЂ¦Тђ", "local import", "РµР‡СРµвЂ¦Тђ", "delete", "Рµв‚¬В Р№в„ўВ¤")),

    // РІР‚вЂќРІР‚вЂќ РµвЂєС•РµС“РЏР№СћвЂћРµВ¤вЂћР·С’вЂ РїСв‚¬РµСљРЃ OCR section РµвЂ вЂ¦РїСвЂ°РІР‚вЂќРІР‚вЂќ

    // РІР‚вЂќРІР‚вЂќ Р¶ВС•Р·В¤С” РІР‚вЂќРІР‚вЂќ
    SearchEntry(
        SectionKeys.OVERLAY,
        R.string.settings_section_overlay,
        R.string.settings_search_item_render_mode,
        listOf("Р·Т‘В§РёТ‘Т‘", "Р¶РЃР„Рµв„–вЂ¦", "banner", "render", "display mode", "floating window", "Р¶вЂљВ¬Р¶ВµВ®Р·Р„вЂ”"),
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
        listOf("РґС‘вЂ№Р¶вЂ“в„–", "РґС‘Р‰Р¶вЂ“в„–", "РёВ¦вЂ Р·вЂєвЂ“", "below", "above", "overlap", "placement"),
        optionLabelResIds = listOf(
            R.string.settings_placement_below_chip,
            R.string.settings_placement_overlap_chip,
            R.string.settings_placement_above_chip,
        ),
    ),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_offset, listOf("offset", "РµС•В®РёВ°С“")),
    SearchEntry(
        SectionKeys.OVERLAY,
        R.string.settings_section_overlay,
        R.string.settings_search_item_overlay_theme,
        listOf("Р¶В·В±РёвЂ°Р†", "Р¶ВµвЂ¦РёвЂ°Р†", "Р·С”С‘РµСВ ", "Р№СљСљР·Р‹В»Р·вЂ™С“", "Р·С’ТђР·РЏР‚", "theme", "dark", "light", "frost", "amber"),
        optionLabelResIds = listOf(
            R.string.settings_theme_classic_dark,
            R.string.settings_theme_amber_gold,
            R.string.settings_theme_paper_light,
            R.string.settings_theme_frost_glass,
            R.string.settings_theme_custom,
        ),
    ),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_custom_theme, SETTINGS_SEARCH_COLOR_KEYWORDS),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_border_style, listOf("solid", "dashed", "dotted", "double", "groove", "РµВ®С›Р·С”С—", "Рёв„ўС™Р·С”С—", "Р·вЂљв„–Р·С”С—", "РµРЏРЉР·С”С—", "РµвЂЎв„–Р¶В§Р…", "РёС•в„–Р¶РЋвЂ Р¶В В·РµСРЏ")),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_text_size, listOf("font size", "РµВ­вЂ”РµРЏВ·", "РµВ­вЂ”РґР…вЂњРµВ¤В§РµВ°РЏ")),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_text_style, listOf("bold", "italic", "underline", "letter spacing", "line spacing", "alignment", "outline", "stroke", "shadow", "РµР‰В Р·Р†вЂ”", "РµР‚С•Р¶вЂ“Сљ", "РґС‘вЂ№Рµв‚¬вЂ™Р·С”С—", "РµВ­вЂ”Р·В¬В¦Р№вЂ”Т‘РёВ·Сњ", "РёРЋРЉРёВ·Сњ", "РµР‡в„–Р№Р…С’", "Р¶РЏРЏРёС•в„–", "Р№ВТ‘РµР…В±")),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_overlay_font, listOf("font", "ttf", "РµВ­вЂ”РґР…вЂњ", "РёвЂЎР„РµВ®С™Рґв„–вЂ°РµВ­вЂ”РґР…вЂњ", "РёР‡вЂР¶вЂ“вЂЎРµВ­вЂ”РґР…вЂњ")),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_alpha, listOf("alpha", "opacity", "Р№Р‚РЏР¶ВР‹РµС”В¦")),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_floating_window_content, listOf("floating window", "Р¶вЂљВ¬Р¶ВµВ®Р·Р„вЂ”", "РµР‹СџР¶вЂ“вЂЎ+РёР‡вЂР¶вЂ“вЂЎ", "РґВ»вЂ¦РёР‡вЂР¶вЂ“вЂЎ", "src dst", "content mode")),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_floating_window_locked, listOf("lock", "Р№вЂќРѓРµВ®С™", "Р¶вЂљВ¬Р¶ВµВ®Р·Р„вЂ”")),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_floating_window_reset, listOf("reset", "Р№вЂЎРЊР·Р…В®", "РёС—ВРµР‹Сџ", "Р№В»ВРёВ®В¤", "default", "floating window", "Р¶вЂљВ¬Р¶ВµВ®Р·Р„вЂ”", "geometry", "РµвЂЎВ РґР…вЂў", "РґР…РЊР·Р…В®", "РµВ°С”РµР‡С‘", "size")),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_allow_wrap, listOf("wrap", "Р¶РЊСћРёРЋРЉ", "single line", "РµВ¤С™РёРЋРЉ")),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_avoid_collision, listOf("collision", "Р·СћВ°Р¶вЂ™С›", "Р№РѓС—Р¶вЂ™С›", "Р№вЂЎРЊРµРЏВ ")),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_merge_adjacent, listOf("merge", "РµС’в‚¬Рµв„–В¶", "Р№вЂЎРЊРµРЏВ ", "Р¶вЂ№вЂ Р¶В®Вµ")),
    SearchEntry(SectionKeys.OVERLAY, R.string.settings_section_overlay, R.string.settings_search_item_merge_strength, listOf("strength", "РµСС”РµС”В¦", "РґС—СњРµВ®в‚¬", "Р¶В вЂЎРµвЂЎвЂ ", "Р¶С—Р‚РёС—вЂє", "conservative", "standard", "aggressive")),

    // РІР‚вЂќРІР‚вЂќ Р¶вЂљВ¬Р¶ВµВ®Р¶РЉвЂ°Р№вЂ™В® РІР‚вЂќРІР‚вЂќ
    // Р¶С–РЃР¶вЂћРЏРїСС™floating_size РµР‹вЂ РµРЏР†РёР‡Р‡Р¶РЉвЂЎ OVERLAYРїСРЉ0.3.x РёВµВ·Р¶вЂќв„–Р¶в‚¬С’ FLOATINGРїСв‚¬РµВ®С›Р№в„ўвЂ¦Р¶Р‹В§РґВ»В¶РµСљРЃ floating sectionРїСвЂ°РіР‚вЂљ
    SearchEntry(SectionKeys.FLOATING, R.string.settings_section_floating, R.string.settings_search_item_floating_size, listOf("floating", "РµСљвЂ Р·С’С“", "Р¶вЂљВ¬Р¶ВµВ®", "size", "РµВ¤В§РµВ°РЏ")),
    SearchEntry(SectionKeys.FLOATING, R.string.settings_section_floating, R.string.settings_search_item_floating_snap, listOf("snap", "РёТ‘Т‘РёС•в„–", "edge")),
    SearchEntry(SectionKeys.FLOATING, R.string.settings_section_floating, R.string.settings_search_item_floating_auto_dock, listOf("auto dock", "РёвЂЎР„РµР‰РЃРµРѓСљР№СњВ ", "РµРѓСљР№СњВ ", "РёвЂ”РЏРёС•в„–")),
    SearchEntry(SectionKeys.FLOATING, R.string.settings_section_floating, R.string.settings_search_item_floating_dock_inset, listOf("inset", "РёТ‘Т‘РёС•в„–РёВ·СњР·В¦В»", "Р¶вЂ°вЂ№РµР‰С—", "РµвЂ¦РЃР№СњСћРµВ±РЏ", "gesture")),

    // РІР‚вЂќРІР‚вЂќ РµСВ§РёРЏСљРµРЊвЂўР¶РЉвЂ°Р№вЂ™В®Р№РЋС”РµС”РЏ РІР‚вЂќРІР‚вЂќ
    SearchEntry(SectionKeys.ARC_MENU, R.string.settings_section_arc_menu, R.string.settings_search_item_arc_menu_order, listOf("arc menu", "РµСВ§РёРЏСљРµРЊвЂў", "РµСВ§РµР…Сћ", "Р№РЋС”РµС”РЏ", "order", "reorder", "Р¶Р‹вЂ™РµС”РЏ", "Р¶вЂ№вЂ“РµР‰РЃ", "menu", "Р¶РЉвЂ°Р№вЂ™В®", "page", "page size", "Рµв‚¬вЂ Р№РЋВµ", "Р¶Р‡РЏР№РЋВµ", "Р·С—В»Р№РЋВµ", "loop", "region", "home", "skill", "Р¶Р‰Р‚РёС“Р…", "Рµв‚¬вЂ™РёР‡РЊ", "language", "РёР‡В­РёРЃР‚", "Р¶С”С’РёР‡В­РёРЃР‚", "Р·вЂєВ®Р¶В вЂЎРёР‡В­РёРЃР‚")),

    // РІР‚вЂќРІР‚вЂќ Рµв‚¬вЂ™РёР‡РЊР·С—В»РёР‡вЂ РІР‚вЂќРІР‚вЂќ

    // РІР‚вЂќРІР‚вЂќ РёВ§В¦РµРЏвЂРµв„ўРЃ РІР‚вЂќРІР‚вЂќ
    SearchEntry(SectionKeys.TRIGGER, R.string.settings_section_trigger, R.string.settings_search_item_loop_interval, listOf("loop", "РµС•Р„Р·Р‹Р‡", "interval", "Р№вЂ”Т‘Р№С™вЂќ")),
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
    SearchEntry(SectionKeys.TRIGGER, R.string.settings_section_trigger, R.string.settings_search_item_a11y_volume, listOf("Р¶вЂ”В Р№С™СљР·СћРЊ", "a11y", "accessibility", "volume", "Р№СџС–Р№вЂЎРЏ")),

    // РІР‚вЂќРІР‚вЂќ РµСР‚РµРЏвЂРёР‚вЂ¦РёР‡Р‰Р¶вЂ“В­ РІР‚вЂќРІР‚вЂќ
    SearchEntry(
        SectionKeys.DEVELOPER,
        R.string.settings_section_developer,
        R.string.settings_search_item_developer_ocr,
        SETTINGS_SEARCH_DEVELOPER_OCR_KEYWORDS,
    ),

    // РІР‚вЂќРІР‚вЂќ Р·Р…вЂР·В»Сљ РІР‚вЂќРІР‚вЂќ
    SearchEntry(SectionKeys.NETWORK, R.string.settings_section_network, R.string.settings_search_item_api_timeout, listOf("timeout", "РёВ¶вЂ¦Р¶вЂ”В¶", "Р·Р…вЂР·В»Сљ", "network")),
    SearchEntry(SectionKeys.NETWORK, R.string.settings_section_network, R.string.settings_search_item_cleartext_hosts, listOf("cleartext", "http", "Р¶ВР‹Р¶вЂ“вЂЎ", "Р·в„ўР…РµС’РЊРµРЊвЂў", "host", "РёвЂЎР„Р¶С›В¶", "Р·В§РѓР¶СљвЂ°")),

    SearchEntry(SectionKeys.APP_LANG, R.string.settings_section_app_lang, R.string.settings_section_app_lang, listOf("language", "locale", "РёР‡В­РёРЃР‚", "РґС‘В­Р¶вЂ“вЂЎ", "english", "i18n")),

    SearchEntry(SectionKeys.THEME_MODE, R.string.settings_section_theme_mode, R.string.settings_section_theme_mode, listOf("theme", "РµВ¤СљР№вЂ”Т‘", "Р·в„ўР…РµВ¤В©", "Р¶В·В±РёвЂ°Р†", "Р¶ВµвЂ¦РёвЂ°Р†", "dark", "light", "night")),
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
    // РµР…вЂ™РґС‘Р‚РµРЉвЂ“Р·С–В»Р·В»СџРёС—вЂќРµвЂєС›Р·С™вЂћ BCP-47РїСв‚¬"zh-Hans-CN" / "zh" / "en-US" Р·В­вЂ°РїСвЂ°Рµв‚¬В° options Р№вЂЎРЉР·Р†С•Р·РЋВ® tagРіР‚вЂљ
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
            // РёвЂЎР„Р·В®РЋР¶РЉРѓРґв„–вЂ¦РµРЉвЂ“РїСС™MainActivity.attachBaseContext РґСС™РµСљРЃ recreate РµС’Р‹РёР‡В» prefs Рµв„–В¶РµРЉвЂ¦РёР€вЂ¦
            // Configuration localeРїСРЉР·В»вЂўРµСР‚ AppCompatDelegate РµСљРЃ ComponentActivity РґС‘Р‰Р·С™вЂћР¶РЉРѓРґв„–вЂ¦РµРЉвЂ“РґС‘РЊР·РЃС–Р№вЂ”В®Р№СћВРіР‚вЂљ
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
