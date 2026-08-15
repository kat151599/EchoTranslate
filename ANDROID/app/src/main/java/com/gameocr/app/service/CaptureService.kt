package com.gameocr.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.view.Surface
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.ServiceCompat
import com.gameocr.app.R
import com.gameocr.app.capture.CaptureCoordinateRelation
import com.gameocr.app.capture.CaptureRegion
import com.gameocr.app.capture.FloatingWindowCaptureAction
import com.gameocr.app.capture.LoopFrameChangePolicy
import com.gameocr.app.capture.LoopFrameFingerprint
import com.gameocr.app.capture.LoopFrameFingerprintFactory
import com.gameocr.app.capture.LoopFramePreOcrDecision
import com.gameocr.app.capture.LoopFramePreOcrResult
import com.gameocr.app.capture.LoopFramePostOcrDecision
import com.gameocr.app.capture.LoopFrameStabilityDecision
import com.gameocr.app.capture.LoopFrameStabilityPolicy
import com.gameocr.app.capture.LoopFrameStabilityState
import com.gameocr.app.capture.LoopRoiFallbackEvent
import com.gameocr.app.capture.LoopRoiFallbackPolicy
import com.gameocr.app.capture.LoopRoiCoordinatePolicy
import com.gameocr.app.capture.LoopTextRect
import com.gameocr.app.capture.LoopTextRoiCandidate
import com.gameocr.app.capture.LoopTextRoiPolicy
import com.gameocr.app.capture.LoopActiveResultDecision
import com.gameocr.app.capture.LoopIndicatorMode
import com.gameocr.app.capture.LoopRuntimePolicy
import com.gameocr.app.capture.MediaProjectionScreenshotter
import com.gameocr.app.capture.MediaProjectionRequestActivity
import com.gameocr.app.capture.AccessibilityScreenshotService
import com.gameocr.app.capture.AccessibilityScreenshotter
import com.gameocr.app.capture.OverlayCaptureRect
import com.gameocr.app.capture.Screenshotter
import com.gameocr.app.capture.ShizukuScreenshotter
import com.gameocr.app.capture.diagnoseCaptureGeometry
import com.gameocr.app.capture.floatingWindowCaptureAction
import com.gameocr.app.capture.mapOverlayBoundsToCapture
import com.gameocr.app.capture.shouldHideFloatingButtonForCapture
import com.gameocr.app.shizuku.ShizukuCapabilities
import com.gameocr.app.data.LogRepository
import com.gameocr.app.data.LoopTriggerMode
import com.gameocr.app.data.LoopTextRegionMode
import com.gameocr.app.data.OverlayFontManager
import com.gameocr.app.data.OverlayStyleMode
import com.gameocr.app.data.adaptiveOverlayActive
import com.gameocr.app.data.effectiveOverlayRenderSettings
import com.gameocr.app.data.RenderMode
import com.gameocr.app.data.Settings
import com.gameocr.app.data.SettingsRepository
import com.gameocr.app.data.TranslationPreset
import com.gameocr.app.data.TranslationPresetCatalog
import com.gameocr.app.data.translationLanguageCodesConflict
import com.gameocr.app.data.needsRawBitmap
import com.gameocr.app.data.Languages
import com.gameocr.app.ocr.BitmapPreprocessor
import com.gameocr.app.ocr.MangaDelayedMaskDebugSessionManager
import com.gameocr.app.ocr.OrientationCoordinator
import com.gameocr.app.ocr.OrientationResult
import com.gameocr.app.ocr.OrientationRouting
import com.gameocr.app.ocr.PaddleTextLineOrientationClassifier
import com.gameocr.app.ocr.TextBlock
import com.gameocr.app.ocr.TextOrientation
import com.gameocr.app.ocr.TranslationOutputOrientationPolicy
import com.gameocr.app.ocr.findOcrResultQualityIssue
import com.gameocr.app.ocr.mapBlocksFromRotated180
import com.gameocr.app.ocr.orientationHintFromLayout
import com.gameocr.app.ocr.resolveTextBlockReadingOrientation
import com.gameocr.app.ocr.shouldRerunLowQualityChinesePaddleOcr
import com.gameocr.app.ocr.sortTextBlocksForReading
import com.gameocr.app.data.resolveTranslationOutputSettings
import com.gameocr.app.data.FloatingSkill
import com.gameocr.app.overlay.FloatingButtonManager
import com.gameocr.app.overlay.FloatingMenuTourPrefs
import com.gameocr.app.overlay.AdaptiveOverlayStyle
import com.gameocr.app.overlay.AdaptiveOverlayStyleAnalyzer
import com.gameocr.app.overlay.AdaptiveTextLayoutPhase
import com.gameocr.app.overlay.LanguageQuickSwitchOverlay
import com.gameocr.app.overlay.HistoryBlockPickerOverlay
import com.gameocr.app.overlay.HistoryCorrectionOverlay
import com.gameocr.app.overlay.OverlayManager
import com.gameocr.app.overlay.PresetQuickSwitchOverlay
import com.gameocr.app.overlay.RegionPickerOverlay
import com.gameocr.app.overlay.TranslationBlockCopyOverlay
import com.gameocr.app.overlay.TranslationCardOverlay
import com.gameocr.app.overlay.TranslationCorrectionDraft
import com.gameocr.app.ui.MainActivity
import com.gameocr.app.translate.BatchTranslationProgressState
import com.gameocr.app.translate.BatchTranslationUpdate
import com.gameocr.app.translate.CrossLineTranslationUnit
import com.gameocr.app.translate.TranslationException
import com.gameocr.app.translate.Translator
import com.gameocr.app.translate.RoutingTranslator
import com.gameocr.app.translate.individualTranslationUnits
import com.gameocr.app.translate.planCrossLineTranslationUnits
import com.gameocr.app.translate.reflowCrossLineTranslation
import com.gameocr.app.translate.crossLineContextTranslationEnabled
import com.gameocr.app.translate.shouldUseCrossLineContextTranslation
import com.gameocr.app.translate.WordHeuristic
import com.gameocr.app.translate.WordResult
import com.gameocr.app.translate.RemotePcTranslator
import com.gameocr.app.util.InferenceTiming
import com.gameocr.app.util.VerticalDiagnosticLog
import com.gameocr.app.util.physicalDisplaySize
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
/**
 * Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р В Р РЏР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’ВµР В Р’В Р В Р РЏР В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р вЂ™Р’В¶Р В Р Р‹Р РЋРІвЂћСћР В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљР’В°Р В Р’В Р В РІР‚в„–Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р Р‹Р Р†РІР‚С›РЎС›Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В¶Р В Р Р‹Р РЋРІвЂћСћР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р В Р РЏ + OCR + Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†Р вЂљРІР‚СњР В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљР Р‹Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В + Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В РІР‚в„ўР вЂ™Р’В¬Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р вЂ™Р’В·Р В Р’В Р Р†Р вЂљРЎвЂєР В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВР В Р Р‹Р Р†Р вЂљРЎС›Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В¤Р В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р Р†Р вЂљР’В¦Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В Р’В Р В РЎвЂњР В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р вЂ Р Р†Р вЂљРЎвЂєР РЋРЎвЂєР В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р В РІР‚В°Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р Р†Р вЂљР’В Р В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ
 *
 * Android 14+ Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В¦Р В Р’В Р РЋРІР‚СљР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В±Р В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ MediaProjection Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р В РІР‚в„–Р В РІР‚в„ўР вЂ™Р’В» (1) Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬ startForeground(..., FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°
 * (2) Р В Р’В Р СћРІР‚ВР В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р В Р вЂ°Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р Р†Р вЂљР’В¦Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р Р‹Р Р†Р вЂљРІР‚Сњ MediaProjectionManager.getMediaProjection(token)Р В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В Р’В Р вЂ™Р’В¶Р В Р Р‹Р РЋРІвЂћСћР В РІР‚в„ўР вЂ™Р’В¬Р В Р’В Р вЂ™Р’В¶Р В Р Р‹Р РЋРІвЂћСћР В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљР’В°Р В Р’В Р В РІР‚в„–Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В РЎС›Р РЋРІР‚в„ўР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В Р В Р Р‹Р вЂ™Р’ВР В Р’В Р вЂ™Р’В¶Р В Р’В Р В РІР‚В°Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р вЂ Р Р†Р вЂљРЎвЂєР РЋРЎвЂєР В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р В РІР‚в„–Р В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р В Р РЏР В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ
 */
@AndroidEntryPoint
class CaptureService : Service() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var overlayFontManager: OverlayFontManager
    @Inject lateinit var translator: Translator
    @Inject lateinit var remotePcTranslator: RemotePcTranslator
    @Inject lateinit var shizukuCapabilities: ShizukuCapabilities
    @Inject lateinit var logRepository: LogRepository
    // Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В«Р В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљРЎС›Р В РІР‚в„ўР вЂ™Р’В§ LLM Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†Р вЂљРІР‚СњР В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљР Р‹Р В Р вЂ Р В РІР‚С™Р вЂ™Р’ВР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљРЎСљР В РІР‚в„ўР вЂ™Р’В«Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В±Р В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В®Р В Р Р‹Р Р†Р вЂљРЎС›Р В Р’В Р вЂ™Р’В·Р В Р’В Р Р†Р вЂљР’В¦Р В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В° LOCAL_* Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРЎв„ўР В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™ Service Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В¶Р В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р РЋРІР‚СљР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В РІР‚в„ўР вЂ™Р’В¶Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљР’В°Р В Р’В Р В РЎвЂњ unload Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р Р†Р вЂљР’В°Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р Р‹Р Р†Р вЂљРЎС› ~500 MB Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В­Р В РІР‚в„ўР вЂ™Р’ВР В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ
    // Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р вЂ™Р’В¶Р В Р Р‹Р РЋРІвЂћСћР В РІР‚в„ўР вЂ™Р’В¬Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р вЂ Р В РІР‚С™Р вЂ™Р’ВР В Р’В Р РЋРІР‚ВР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљР’В°Р В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В¤Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В« + Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В·Р В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В Р’В Р СћРІР‚ВР В РІР‚в„ўР вЂ™Р’В»Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В Р’В Р В РЎвЂњ settings.textOrientationAutoDetect = true Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В РІР‚в„ўР вЂ™Р’В¶Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р В РЎвЂњР В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ
    @Inject lateinit var orientationCoordinator: OrientationCoordinator
    // Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р В РЎвЂњР В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В·Р В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В±Р В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В¤Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В РІР‚в„ўР вЂ™Р’В­"manga-ocr Р В Р’В Р вЂ™Р’В¶Р В Р’В Р В РЎвЂњР В Р’В Р В РІР‚в„–Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎвЂќР В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВР В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В РІР‚в„ўР вЂ™Р’В¦Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В·Р В Р’В Р Р†Р вЂљР’В Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљР’В¦Р В Р’В Р Р†Р вЂљР’В¦"Р В Р’В Р В РІР‚В Р В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р В РІР‚В Р В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р вЂ™Р’В¶Р В Р Р‹Р РЋРІвЂћСћР В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљР’В¦Р В Р’В Р Р†Р вЂљР’В¦Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В РІР‚в„ўР вЂ™Р’В¶ (VERTICAL_RTL, ja) Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р В Р вЂ°Р В Р’В Р СћРІР‚ВР В Р Р‹Р вЂ™Р’ВР В Р Р‹Р Р†РІР‚С›РЎС›Р В Р’В Р РЋРІР‚ВР В Р Р‹Р РЋРІР‚С”Р В РІР‚в„ўР вЂ™Р’В«Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В·Р В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В° manga

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val captureLock = Mutex()
    private val translationBatchGate = TranslationBatchGate()

    private var screenshotter: Screenshotter? = null
    @Volatile private var restartWithMediaProjectionOnDestroy = false
    private var projection: MediaProjection? = null
    private var floatingButton: FloatingButtonManager? = null
    private var overlay: OverlayManager? = null
    private var regionPicker: RegionPickerOverlay? = null
    private var languageQuickSwitch: LanguageQuickSwitchOverlay? = null
    private var presetQuickSwitch: PresetQuickSwitchOverlay? = null
    private var historyBlockPicker: HistoryBlockPickerOverlay? = null
    private var translationCard: TranslationCardOverlay? = null
    private var translationBlockCopyOverlay: TranslationBlockCopyOverlay? = null
    private var historyCorrectionOverlay: HistoryCorrectionOverlay? = null

    private var loopJob: Job? = null
    private var translationRenderJob: Job? = null
    private var ocrWarmupJob: Job? = null
    private var previousLoopFingerprint: LoopFrameFingerprint? = null
    private var previousLoopOcrText: String? = null
    private var loopFrameStabilityState = LoopFrameStabilityState()
    private var loopRoiTextFallbackActive: Boolean = false
    @Volatile private var loopTranslationInFlight = false
    @Volatile private var loopSessionId = 0L
    @Volatile private var lastLoopRuntimeLogState: String? = null
    // Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В®Р В Р Р‹Р РЋРІР‚С”Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В РІР‚в„ўР вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦ SettingsRepository.settings flowР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В®Р В РІР‚в„ўР вЂ™Р’В©Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В®Р В Р Р‹Р Р†Р вЂљРЎС›Р В Р’В Р вЂ™Р’В·Р В Р’В Р Р†Р вЂљР’В¦Р В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р В РІР‚в„–Р В РІР‚в„ўР вЂ™Р’ВµР В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р Р‹Р РЋРЎв„ўР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В­Р В РІР‚в„ўР вЂ™Р’ВР В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В¶Р В Р Р‹Р РЋРІвЂћСћР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВР В Р Р‹Р Р†Р вЂљРЎС›Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В¤Р В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р В РІР‚в„–Р В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В«Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р’В Р вЂ™Р’ВµР В Р’В Р В Р вЂ°Р В Р Р‹Р Р†Р вЂљРІР‚СљР В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р Р‹Р РЋРЎСџР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬
    // Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В РІР‚в„ўР вЂ™Р’В¬Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р вЂ™Р’В¶Р В Р’В Р В РІР‚В°Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РЎС›Р В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В¤Р В РІР‚в„ўР вЂ™Р’В§Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р В Р РЏР В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚СљР В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р’В Р В Р вЂ°Р В Р’В Р РЋРІР‚ВР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р Р†Р вЂљР’В Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р Р‹Р РЋРІР‚С”Р В РІР‚в„ўР вЂ™Р’ВР В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚СљР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В­Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В Р’В Р вЂ™Р’ВµР В Р’В Р В Р РЏР В РІР‚в„ўР вЂ™Р’В·Р В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚СљР В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р В Р РЏР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВР В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎСљР В РІР‚в„ўР вЂ™Р’В¦Р В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚СљР В Р’В Р вЂ™Р’В·Р В РЎС›Р Р†Р вЂљР’ВР В РІР‚в„ўР вЂ™Р’В§Р В Р’В Р РЋРІР‚ВР В РЎС›Р Р†Р вЂљР’ВР В РЎС›Р Р†Р вЂљР’ВР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р СћРІР‚ВР В Р’В Р Р†Р вЂљР’В¦Р В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’В·Р В Р’В Р Р†Р вЂљР’В¦Р В РІР‚в„ўР вЂ™Р’В® Р В Р’В Р В РІР‚В Р В Р’В Р Р†Р вЂљРЎв„ўР В РІР‚в„ўР вЂ™Р’В¦Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В Р Р‹Р РЋРЎСџР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р вЂ™Р’ВµР В Р’В Р В Р РЏР В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В Р’В Р В РЎвЂњ captureOnce
    // Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В РІР‚в„ўР вЂ™Р’В¶Р В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљР Р‹Р В РІР‚в„ўР вЂ™Р’В» settingsР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљР Р‹Р В Р Р‹Р вЂ™Р’ВР В Р’В Р РЋРІР‚ВР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В РЎС›Р Р†Р вЂљР’ВР В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В·Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р В РІР‚в„–Р В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р вЂ™Р’ВµР В Р’В Р РЋРІР‚СљР В Р Р‹Р РЋРІвЂћСћР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В­Р В Р Р‹Р РЋРІР‚С”/Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’В¶Р В Р Р‹Р РЋРІвЂћСћР В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљР’В°Р В Р’В Р В РІР‚в„–Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В§Р В РІР‚в„ўР вЂ™Р’В¦Р В Р’В Р вЂ™Р’ВµР В Р’В Р В Р РЏР В Р вЂ Р В РІР‚С™Р вЂ™Р’ВР В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В¬Р В Р’В Р В РІР‚в„–Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р В Р РЏР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р В Р вЂ°Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р Р†Р вЂљР’В¦Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р РЋРІвЂћСћР В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљР’В°Р В Р’В Р В РЎвЂњР В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ
    private var settingsCollectJob: Job? = null
    @Volatile private var loopMode: Boolean = false
    private val captureSequence = AtomicLong(0L)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        VerticalDiagnosticLog.i(
            "service configurationChanged orientation=${newConfig.orientation.toDiagOrientation()} " +
                "display=${currentDisplayGeometry().toDiagString()} projection=${projectionDiagnosticSummary()}"
        )
        resizeProjectionForCurrentDisplay("serviceConfigurationChanged")
        // Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р В Р РЏР В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р вЂ Р В РІР‚С™Р вЂ™Р’ВР В Р’В Р вЂ™Р’ВµР В Р’В Р В Р РЏР В РІР‚в„ўР вЂ™Р’ВР В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљРЎСљР В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р’В Р вЂ™Р’В¶Р В Р’В Р Р†Р вЂљР’В°Р В Р’В Р Р†Р вЂљР’В°Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р Р‹Р Р†Р вЂљРЎС™ + Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В РІР‚в„ўР вЂ™Р’В¬Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р вЂ™Р’В·Р В Р’В Р Р†Р вЂљРЎвЂєР В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В Р’В Р вЂ™Р’ВµР В Р’В Р В Р РЏР В Р’В Р Р†РІР‚С™Р’В¬ + Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р В Р РЏР В Р’В Р вЂ™Р’ВµР В Р’В Р В РІР‚В°Р В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРЎСџР В Р Р‹Р РЋРЎСџР В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р Р†Р вЂљР’В¦Р В Р’В Р вЂ™Р’В¶Р В Р’В Р В РІР‚В°Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р вЂ™Р’В¶Р В Р’В Р Р†Р вЂљР Р‹Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљРЎС›Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В®Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В Р’В Р СћРІР‚ВР В Р’В Р Р†Р вЂљР’В¦Р В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’В·Р В Р’В Р Р†Р вЂљР’В¦Р В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ
        // captureRegion Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В° SettingsRepository.rescaleCaptureRegionIfNeededР В Р’В Р В РІР‚В Р В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р В РІР‚В Р В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРЎС™
        // Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В®Р В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р В РЎвЂњ saved Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р вЂ™Р’В¶Р В Р’В Р В Р вЂ°Р В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р вЂ™Р’В¶Р В Р’В Р В РІР‚В°Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљР’В¦Р В Р вЂ Р В РІР‚С™Р РЋРЎв„ўР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р В Р РЏР В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљР Р‹Р В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р РЋРІР‚ВР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљР’В°Р В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В®Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В Р’В Р вЂ™Р’В¶Р В Р’В Р Р†Р вЂљР Р‹Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљРЎС›Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’В¶Р В Р’В Р Р†Р вЂљР Р‹Р В Р вЂ Р В РІР‚С™Р РЋРЎС™"Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р Р†Р вЂљР’В°Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В¬Р В Р’В Р В РІР‚в„– onConfigurationChanged Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†РІР‚С›РЎС›Р В Р вЂ Р В РІР‚С™Р РЋРІР‚С”Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљР Р‹Р В Р Р‹Р Р†Р вЂљР’В"Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В РЎС›Р Р†Р вЂљР’ВР В Р’В Р вЂ™Р’В·Р В Р’В Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРІР‚СљР В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ
        floatingButton?.onConfigurationChanged()
        mainScope.launch { overlay?.onConfigurationChanged() }
        scope.launch {
            val screen = physicalDisplaySize(this@CaptureService)
            settingsRepository.rescaleCaptureRegionIfNeeded(screen.width, screen.height)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart(intent)
            ACTION_STOP -> stopSelf()
            ACTION_TRIGGER_ONCE -> triggerOnce()
            ACTION_PICK_REGION -> showRegionPickerOverlay()
            ACTION_RUN_FLOATING_TOUR -> {
                FloatingMenuTourPrefs.reset(this)
                floatingButton?.requestFirstUseTour()
            }
        }
        return START_NOT_STICKY
    }

    private fun handleStart(intent: Intent) {
        // Service Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљР’В°Р В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В РІР‚в„ўР вЂ™Р’В¶Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљР’В°Р В Р’В Р В РЎвЂњ rescale Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В¬Р В Р’В Р В РІР‚в„– captureRegionР В Р’В Р В РІР‚В Р В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р В РІР‚В Р В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В¦Р В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В Р’В Р вЂ™Р’В¶Р В Р Р‹Р Р†Р вЂљРЎвЂќР В Р Р‹Р РЋРІвЂћСћР В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В·Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В Р’В Р В РЎвЂњ service Р В Р’В Р вЂ™Р’В¶Р В Р’В Р Р†Р вЂљР’В Р В Р’В Р В РІР‚в„–Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р вЂ™Р’ВР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В РІР‚в„ўР вЂ™Р’В¶Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљР’В¦Р В РІР‚в„ўР вЂ™Р’В¬Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљРЎСљР В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р В Р РЏР В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°
        // Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р вЂ Р Р†Р вЂљРЎвЂєР РЋРЎвЂєР В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’В¶Р В Р’В Р Р†Р вЂљР’В°Р В Р’В Р Р†Р вЂљР’В° region Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В Р В Р’В Р В РІР‚в„–Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В­Р В Р’В Р Р†РІР‚С™Р’В¬Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљР’В¦Р В Р вЂ Р В РІР‚С™Р РЋРЎв„ўР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р В Р РЏР В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р вЂ Р В РІР‚С™Р вЂ™Р’ВР В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ
        scope.launch {
            val screen = physicalDisplaySize(this@CaptureService)
            settingsRepository.rescaleCaptureRegionIfNeeded(screen.width, screen.height)
        }
        // Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В¦Р В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В Р’В Р вЂ™Р’В¶Р В Р Р‹Р Р†Р вЂљРЎвЂќР В Р Р‹Р РЋРІвЂћСћР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В·Р В Р’В Р Р†Р вЂљР’В Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р В Р РЏР В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљР’В°Р В Р’В Р В РЎвЂњР В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬ cleanup Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В РІР‚в„ўР вЂ™Р’В§Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р РЋРІР‚С”Р В Р’В Р вЂ™Р’В¶Р В Р Р‹Р Р†Р вЂљРЎСљР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р РЋРІР‚СљР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В РІР‚в„ўР вЂ™Р’В¬Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р вЂ™Р’В·Р В Р’В Р Р†Р вЂљРЎвЂєР В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В Р’В Р вЂ™Р’ВµР В Р’В Р В Р РЏР В РІР‚в„ўР вЂ™Р’В Р В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљР’В°Р В РІР‚в„ўР вЂ™Р’В  / Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р В Р РЏР В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р РЋРЎв„ўР В Р Р‹Р Р†Р вЂљРЎС›Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В·Р В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’В¶Р В Р Р‹Р Р†Р вЂљРІР‚СљР В Р вЂ Р В РІР‚С™Р РЋРІР‚С”Р В Р’В Р вЂ™Р’В¶Р В Р Р‹Р вЂ™Р’ВР В Р’В Р В Р РЏР В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ
        // Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В·Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В¤Р В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚Сљ"Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљР’В°Р В Р’В Р В РЎвЂњ"Р В Р’В Р вЂ™Р’В¶Р В Р’В Р В РІР‚В°Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РЎС›Р В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚СљР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р вЂ™Р’В¶Р В Р’В Р В Р вЂ°Р В Р Р‹Р РЋРІР‚С” Shizuku Р В Р’В Р В РІР‚В Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р вЂ Р В РІР‚С™Р РЋРЎС™ MediaProjection Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В·Р В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎС›Р В Р вЂ Р В РІР‚С™Р РЋРІР‚С”Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р Р†Р вЂљР’В¦Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р вЂ Р Р†Р вЂљРЎвЂєР РЋРЎвЂєР В Р’В Р вЂ™Р’В¶Р В Р Р‹Р РЋРЎв„ўР В Р’В Р В РІР‚в„–Р В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ
        cleanupCapture()

        // Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В¤Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В РІР‚в„ўР вЂ™Р’В­Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В¦Р В Р’В Р РЋРІР‚СљР В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†РІР‚С›РЎС›Р В Р вЂ Р В РІР‚С™Р РЋРІР‚С”Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р В Р РЏР В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В·Р В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎС›Р В Р вЂ Р В РІР‚С™Р РЋРІР‚С”Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р Р‹Р Р†РІР‚С›РЎС›ASB / Shizuku Р В Р’В Р вЂ™Р’ВµР В Р’В Р В Р РЏР В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В РІР‚в„ўР вЂ™Р’В¶Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎСљР В Р вЂ Р В РІР‚С™Р РЋРЎС™ backendР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В РІР‚в„ўР вЂ™Р’В¦Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р вЂ Р Р†Р вЂљРЎвЂєР РЋРЎвЂє MediaProjectionР В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ
        val useAsb = intent.getBooleanExtra(EXTRA_USE_ASB, false) &&
            AccessibilityScreenshotService.isEnabled(this)
        val useShizuku = intent.getBooleanExtra(EXTRA_USE_SHIZUKU, false) &&
            shizukuCapabilities.availability(this) == ShizukuCapabilities.Availability.READY

        // Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’ВµР В Р’В Р В Р РЏР В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р вЂ™Р’В¶Р В Р Р‹Р РЋРІвЂћСћР В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљР’В°Р В Р’В Р В РІР‚в„–Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р Р‹Р Р†РІР‚С›РЎС›Android 14+ Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р В РІР‚в„–Р В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВР В Р Р‹Р Р†Р вЂљРЎС›Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р вЂ™Р’ВР В Р’В Р В Р РЏР В Р’В Р СћРІР‚ВР В Р Р‹Р вЂ™Р’ВР В РІР‚в„ўР вЂ™Р’В Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р Р‹Р РЋРЎв„ўР В Р Р‹Р Р†Р вЂљРЎвЂќР В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В РІР‚в„ўР вЂ™Р’В¶ typeР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В РІР‚в„ўР вЂ™Р’В¦Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р вЂ Р Р†Р вЂљРЎвЂєР РЋРЎвЂє InvalidForegroundServiceTypeExceptionР В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ
        // MediaProjection Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В·Р В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎС›Р В Р вЂ Р В РІР‚С™Р РЋРІР‚С”Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В° MEDIA_PROJECTIONР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р РЋРІР‚СњShizuku Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В·Р В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎС›Р В Р вЂ Р В РІР‚С™Р РЋРІР‚С”Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В° SPECIAL_USEР В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ
        val fgType = when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q -> 0
            useAsb || useShizuku -> android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            else -> android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        }
        // Android 14+ HyperOS/MIUI Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р Р†Р вЂљР’В°Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљР’ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В§Р В Р’В Р РЋРІР‚Сљ raceР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р Р‹Р Р†РІР‚С›РЎС›MediaProjectionRequestActivity onActivityResult
        // Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В РІР‚в„ўР вЂ™Р’В¶Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В° RESULT_OK Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В«Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р’В Р вЂ™Р’ВµР В Р’В Р В Р вЂ°Р В Р Р‹Р Р†Р вЂљРІР‚Сљ startForegroundServiceР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В­Р В РІР‚в„ўР вЂ™Р’В¤Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В РІР‚в„ўР вЂ™Р’В¶ `android:project_media` app-op
        // grant Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В°Р В Р Р‹Р Р†РІР‚С›РЎС›Р В Р’В Р вЂ™Р’В¶Р В Р Р‹Р РЋРІвЂћСћР В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р вЂ™Р’ВµР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В­Р В РЎС›Р РЋРІР‚в„ўР В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р Р†Р вЂљР’В¦Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°startForeground Р В Р’В Р вЂ™Р’В¶Р В Р’В Р Р†Р вЂљР’В°Р В Р вЂ Р В РІР‚С™Р РЋРІР‚Сњ SecurityException Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ
        // workaroundР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р Р‹Р Р†РІР‚С›РЎС›Р В Р’В Р вЂ™Р’В¶Р В Р’В Р В Р вЂ°Р В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В РІР‚в„ўР вЂ™Р’В·Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљР’ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р Р†Р вЂљРІвЂћвЂ“ postDelayed Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р В Р вЂ°Р В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљР Р‹Р В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В¬Р В Р’В Р В РІР‚в„–Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В»Р В Р вЂ Р Р†Р вЂљРЎвЂєР РЋРЎвЂє op 200ms Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р Р†Р вЂљР’В¦Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В РІР‚в„ўР вЂ™Р’В¶Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В РЎС›Р Р†Р вЂљР’ВР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В Р’В Р СћРІР‚ВР В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В¤Р В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р РЋРІР‚ВР В РЎС›Р Р†Р вЂљР’ВР В РЎС›Р РЋРІР‚в„ўР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р’В Р В Р вЂ° stopSelfР В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ
        if (!startForegroundCompat(fgType, intent)) {
            return
        }

        if (useAsb) {
            screenshotter = AccessibilityScreenshotter()
            Timber.i("CAPTURE BACKEND = ASB")
        } else if (useShizuku) {
            screenshotter = ShizukuScreenshotter()
            Timber.i("CaptureService started with Shizuku path")
        } else {
            val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
            val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
            } else {
                @Suppress("DEPRECATION") intent.getParcelableExtra(EXTRA_RESULT_DATA)
            }
            if (data == null) {
                Timber.w("MediaProjection result data is null")
                stopSelf()
                return
            }
            val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = mpm.getMediaProjection(resultCode, data)
            val mp = projection
            if (mp == null) {
                Timber.w("getMediaProjection returned null")
                stopSelf()
                return
            }
            screenshotter = MediaProjectionScreenshotter(this, mp)
            Timber.i("CaptureService started with MediaProjection path")
        }

        VerticalDiagnosticLog.i(
            "service capture path=${screenshotter?.javaClass?.simpleName ?: "null"} " +
                "display=${currentDisplayGeometry().toDiagString()} projection=${projectionDiagnosticSummary()}"
        )

        overlay = OverlayManager(
            context = this,
            settingsRepository = settingsRepository,
            ioScope = scope,
            onTranslationBlockDetailRequested = ::showTranslationBlockCopyPanel,
            onFloatingWindowDismissed = {},
            onTranslationOverlayShown = { floatingButton?.bringToFront() },
        )
        floatingButton = FloatingButtonManager(
            this,
            // Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р вЂ™Р’ВµР В Р’В Р В Р вЂ°Р В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р Р‹Р Р†РІР‚С›РЎС›Р В Р’В Р вЂ™Р’В¶Р В Р’В Р В РІР‚В°Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљР’В¦Р В Р вЂ Р В РІР‚С™Р РЋРЎв„ўР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р В Р вЂ° skill Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В·Р В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћFloatingButtonManager.skill Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В РІР‚в„ўР вЂ™Р’В± settings collect Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В­Р В РЎС›Р РЋРІР‚в„ўР В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р Р‹Р РЋРЎв„ўР В Р’В Р вЂ™Р’В¶Р В Р’В Р В РІР‚В°Р В Р’В Р РЋРІР‚СљР В Р’В Р вЂ™Р’В¶Р В Р Р‹Р РЋРІвЂћСћР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р РЋРІР‚Сњ
            // Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р вЂ Р Р†Р вЂљРЎвЂєР РЋРЎвЂєР В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р В РІР‚В°Р В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљР Р‹Р В РІР‚в„ўР вЂ™Р’В» floatingButton?.skill Р В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р В РІР‚В°Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВР В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’В¶Р В Р Р‹Р Р†Р вЂљРЎвЂќР В Р вЂ Р В РІР‚С™Р РЋРІР‚С”Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В РІР‚в„ўР вЂ™Р’В Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В РІР‚в„ўР вЂ™Р’В¶Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІР‚СњР В РІР‚в„ўР вЂ™Р’В«Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В РІР‚в„ўР вЂ™Р’В§Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р Р‹Р РЋРЎв„ўР В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р РЋРІР‚СљР В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В·Р В Р’В Р СћРІР‚ВР В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р РЋРІР‚ВР В Р’В Р В Р РЏР В Р Р‹Р РЋРІвЂћСћР В Р’В Р вЂ™Р’ВµР В Р’В Р В Р вЂ°Р В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р вЂ™Р’В¶Р В Р’В Р Р†Р вЂљР’В°Р В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р Р†Р вЂљР’В¦Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В«Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р Р‹Р РЋРЎСџР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ
            onSingleTap = {
                when (floatingButton?.skill ?: FloatingSkill.FULL_SCREEN) {
                    FloatingSkill.FULL_SCREEN -> triggerOnce()
                    // LEGACY_COMPAT: remove after dependency cleanup. Old persisted value falls back.
                    FloatingSkill.WORD_SELECT -> triggerOnce()
                    FloatingSkill.LOOP -> toggleLoopMode()
                }
            },
            onSwitchToLoop = { applyFloatingSkill(FloatingSkill.LOOP) },
            settingsRepository = settingsRepository,
            ioScope = scope
        ).also {
            it.firstUseTourPending =
                FloatingMenuTourPrefs.shouldShow(this@CaptureService)
            it.onFirstUseTourCompleted = {
                FloatingMenuTourPrefs.markCompleted(this@CaptureService)
            }
            // Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В Р Р‹Р Р†Р вЂљРЎС›Р В Р’В Р вЂ™Р’ВµР В Р’В Р В РІР‚В°Р В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРЎСџР В Р Р‹Р РЋРЎСџР В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В РЎС›Р Р†Р вЂљР’ВР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р Р‹Р Р†РІР‚С›РЎС›Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В РІР‚в„ўР вЂ™Р’В¬Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р вЂ™Р’В·Р В Р’В Р Р†Р вЂљРЎвЂєР В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р’В Р СћРІР‚ВР В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р Р†РІР‚С™Р’В¬Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В РІР‚в„ўР вЂ™Р’В§Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†РІР‚С›РЎС›Р В Р вЂ Р В РІР‚С™Р РЋРІР‚С” Activity Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В·Р В Р Р‹Р Р†Р вЂљРІР‚СљР В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљР’В¦Р В РІР‚в„ўР вЂ™Р’В¬Р В Р’В Р В РІР‚В Р В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р В РІР‚В Р В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р вЂ™Р’В¶Р В Р Р‹Р Р†Р вЂљР’ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р В Р РЏ / Р В Р’В Р вЂ™Р’В¶Р В Р Р‹Р вЂ™Р’ВР В РІР‚в„ўР вЂ™Р’В«Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В¤Р В Р’В Р В Р вЂ°
            // Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р’В Р РЋРІР‚ВР В Р’В Р В Р РЏР В Р Р‹Р РЋРІвЂћСћР В Р’В Р вЂ™Р’ВµР В Р’В Р В Р вЂ°Р В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В Р’В Р СћРІР‚ВР В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р Р‹Р РЋРЎСџР В Р’В Р вЂ™Р’ВµР В Р’В Р В Р РЏР В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р вЂ™Р’ВµР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В¬Р В Р’В Р В РІР‚в„–Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬show Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р В Р вЂ°Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ
            it.onMenuPickRegion = { showRegionPickerOverlay() }
            it.onMenuLanguagePair = { showLanguageQuickSwitchOverlay() }
            it.onMenuPresetSwitch = { showPresetQuickSwitchOverlay() }
            it.onMenuRetranslateHistory = { selectHistoryBlock(::retranslateHistoryBlock) }
            it.onMenuDeleteHistory = { selectHistoryBlock(::deleteHistoryBlock) }
            it.onMenuSuggestHistoryCorrection = { selectDisplayedBlockForCorrection() }
            it.onMenuRestartCapture = {
                restartWithMediaProjectionOnDestroy = true
                stopSelf()
            }
            it.onMenuOpenSettings = {
                startActivity(
                    Intent(this, MainActivity::class.java)
                        .putExtra(MainActivity.EXTRA_START_ROUTE, MainActivity.ROUTE_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                )
            }
            it.onMenuOpenMainActivity = { startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            ) }
            // Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р вЂ™Р’В¶Р В Р’В Р Р†Р вЂљР’В°Р В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р Р†Р вЂљР’В¦Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р вЂ™Р’В¶Р В Р’В Р В Р вЂ°Р В Р Р‹Р РЋРІР‚С”Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р Р‹Р Р†РІР‚С›РЎС›Р В Р’В Р РЋРІР‚ВР В Р’В Р В Р РЏР В Р Р‹Р РЋРІвЂћСћР В Р’В Р вЂ™Р’ВµР В Р’В Р В Р вЂ°Р В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљРЎСљР В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РЎС›Р В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†Р вЂљРІР‚СњР В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљР Р‹Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В / Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р В Р РЏР В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†Р вЂљРІР‚СњР В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљР Р‹Р В Р вЂ Р В РІР‚С™Р вЂ™Р’ВР В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В РІР‚в„ўР вЂ™Р’В¶Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В РІР‚в„ўР вЂ™Р’В± FloatingButtonManager Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В§Р В РІР‚в„ўР вЂ™Р’В¦Р В Р’В Р вЂ™Р’ВµР В Р’В Р В Р РЏР В Р вЂ Р В РІР‚С™Р вЂ™Р’ВР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°
            // Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р вЂ Р Р†Р вЂљРЎвЂєР РЋРЎвЂєР В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р В РІР‚В°Р В Р’В Р РЋРІР‚ВР В РЎС›Р Р†Р вЂљР’ВР В Р Р‹Р РЋРЎСџР В Р’В Р РЋРІР‚ВР В РЎС›Р Р†Р вЂљР’ВР В Р’В Р Р†РІР‚С™Р’В¬Р В Р’В Р вЂ™Р’В¶Р В Р’В Р В РІР‚В°Р В Р’В Р РЋРІР‚СљР В Р’В Р СћРІР‚ВР В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р’В Р вЂ™Р’ВµР В Р’В Р В РІР‚В°Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В° Settings + Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р Р†Р вЂљРЎвЂє info Р В Р’В Р вЂ™Р’В¶Р В Р’В Р В Р РЏР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В¤Р В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’ВР В Р’В Р Р†Р вЂљР’В°Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р РЋРЎСџР В РЎС›Р РЋРІР‚в„ўР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р РЋРІР‚ВР В Р’В Р В РІР‚в„–Р В Р’В Р В РІР‚В°Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В Р Р‹Р Р†Р вЂљРЎС›Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В Р В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р вЂ™Р’В¶Р В Р’В Р В Р вЂ°Р В Р Р‹Р РЋРІР‚С”Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В РІР‚в„ўР вЂ™Р’В± FloatingButtonManager
            // Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В Р’В Р В РЎвЂњ applySkillIcon Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В­Р В РЎС›Р РЋРІР‚в„ўР В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ
            it.onSwitchSkill = { newSkill -> applyFloatingSkill(newSkill) }
        }
        // Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В­Р В РЎС›Р РЋРІР‚в„ўР В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљР Р‹Р В РІР‚в„ўР вЂ™Р’В» settings Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎСљР В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В¤Р В РІР‚в„ўР вЂ™Р’В§Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р В Р РЏ + Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРІР‚СњР В РІР‚в„ўР вЂ™Р’ВР В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В Р Р‹Р РЋРЎСџР В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р Р†Р вЂљР’В°Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В¬Р В Р’В Р В РІР‚в„–Р В Р’В Р вЂ™Р’В¶Р В Р Р‹Р РЋРЎв„ўР В Р Р‹Р Р†Р вЂљРЎС›Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р’В Р СћРІР‚ВР В Р’В Р Р†Р вЂљР’В¦Р В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’В·Р В Р’В Р Р†Р вЂљР’В¦Р В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р’В Р В Р вЂ° showР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р РЋРІР‚СљР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р’В Р В Р вЂ°Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В РІР‚в„ўР вЂ™Р’ВР В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р вЂ™Р’ВµР В Р’В Р В РІР‚в„–Р В Р Р‹Р Р†Р вЂљРЎвЂќ startForeground Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВµР В Р’В Р РЋРІР‚СљР В Р’В Р вЂ™Р’В·Р В Р’В Р В РЎвЂњР В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњ
        scope.launch {
            val s = settingsRepository.get()
            floatingButton?.sizeDp = s.floatingButtonSizeDp
            floatingButton?.initialX = s.floatingButtonX
            floatingButton?.initialY = s.floatingButtonY
            floatingButton?.snapToEdgeEnabled = s.floatingButtonSnapToEdge
            floatingButton?.autoDockEnabled = s.floatingButtonAutoDock
            floatingButton?.dockEdgeInsetPx = (s.floatingButtonDockInsetDp * resources.displayMetrics.density).toInt()
            floatingButton?.menuItemOrder = s.floatingMenuItemOrder
            floatingButton?.arcMenuPageSize = s.arcMenuPageSize
            floatingButton?.skill = s.floatingButtonSkill
            mainScope.launch { floatingButton?.show() }
        }

        // Settings flow Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВР В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В РІР‚в„ўР вЂ™Р’В¬Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р вЂ™Р’В·Р В Р’В Р Р†Р вЂљРЎвЂєР В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р РЋРІР‚СљР В Р’В Р вЂ™Р’В·Р В Р’В Р Р†Р вЂљР’В°Р В РІР‚в„ўР вЂ™Р’В¶Р В Р’В Р вЂ™Р’В¶Р В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚СљР В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†РІР‚С›РЎС›Р В Р вЂ Р В РІР‚С™Р РЋРІР‚С”Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В¶Р В Р Р‹Р РЋРЎв„ўР В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р вЂ™Р’ВµР В Р’В Р В РЎвЂњР В Р’В Р РЋРІР‚СљР В Р’В Р вЂ™Р’В¶Р В Р Р‹Р РЋРЎв„ўР В РЎС›Р РЋРІР‚в„ўР В Р’В Р вЂ™Р’В¶Р В Р Р‹Р Р†Р вЂљРЎСљР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В РІР‚в„ўР вЂ™Р’В¦Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В¬Р В Р’В Р В РІР‚в„– emit Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В Р В РІР‚в„ўР вЂ™Р’В·Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р Р‹Р РЋРІвЂћСћР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В¦Р В Р’В Р РЋРІР‚СљР В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎСљР В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р Р‹Р РЋРЎв„ўР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В§Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р’В Р вЂ™Р’В·Р В Р’В Р Р†Р вЂљР’В°Р В РІР‚в„ўР вЂ™Р’В¶Р В Р’В Р вЂ™Р’В¶Р В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚СљР В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ
        // Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎС›Р В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р вЂ™Р’В·Р В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В Р Р‹Р Р†Р вЂљРЎС›Р В Р’В Р вЂ™Р’ВµР В Р’В Р В Р РЏР В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎС›Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†РІР‚С›РЎС›Р В Р вЂ Р В РІР‚С™Р РЋРІР‚С” Settings Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІР‚СњР В РІР‚в„ўР вЂ™Р’В«Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В РІР‚в„ўР вЂ™Р’В§Р В Р’В Р вЂ™Р’ВµР В Р’В Р В Р РЏР В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р Р†Р вЂљР’В¦Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В·Р В Р’В Р Р†Р вЂљР’В Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р В Р РЏР В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р вЂ™Р’В¶Р В Р Р‹Р РЋРІвЂћСћР В Р Р‹Р РЋРЎСџР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’ВµР В Р’В Р В Р РЏР В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В·Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р вЂ™Р’В¶Р В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р Р†Р вЂљР’В Р В Р’В Р вЂ™Р’В¶Р В Р Р‹Р РЋРЎСџР В Р вЂ Р В РІР‚С™Р РЋРЎв„ўР В Р’В Р вЂ™Р’ВµР В Р’В Р В Р РЏР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р В Р вЂ°Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р Р†Р вЂљР’В¦Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В¦Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р Р‹Р Р†РІР‚С›РЎС›Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р вЂ™Р’В¶Р В Р’В Р В Р вЂ°Р В Р Р‹Р РЋРІР‚С”Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†РІР‚С›РЎС›Р В Р вЂ Р В РІР‚С™Р РЋРІР‚С”Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р РЋРІР‚СљР В Р’В Р вЂ™Р’В·Р В Р’В Р Р†Р вЂљР’В°Р В РІР‚в„ўР вЂ™Р’В¶Р В Р’В Р вЂ™Р’В¶Р В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚СљР В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ
        settingsCollectJob?.cancel()
        settingsCollectJob = scope.launch {
            var lastEngine: com.gameocr.app.data.TranslatorEngine? = null
            settingsRepository.settings.collect { s ->
                applyOverlayConfig(s, syncFloatingWindowLock = true)
                // Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В«Р В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљРЎС›Р В РІР‚в„ўР вЂ™Р’В§ LLM Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРЎв„ўР В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В РІР‚в„ўР вЂ™Р’В¶Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљР’В°Р В Р’В Р В РЎвЂњР В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р Р†Р вЂљР’В°Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р Р‹Р Р†Р вЂљРЎС›Р В Р’В Р вЂ™Р’В¶Р В Р Р‹Р РЋРЎв„ўР В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р В Р вЂ°Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р Р‹Р Р†РІР‚С›РЎС›Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р РЋРІР‚СљР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р’В Р В Р вЂ° 500MB+ Р В Р’В Р вЂ™Р’В¶Р В Р’В Р В РЎвЂњР В Р’В Р В РІР‚в„–Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎвЂќР В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљР’ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р Р†РІР‚С›РІР‚вЂњР В РІР‚в„ўР вЂ™Р’В©Р В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В­Р В РІР‚в„ўР вЂ™Р’ВР В Р’В Р вЂ™Р’В¶Р В Р’В Р Р†Р вЂљР’В°Р В Р Р‹Р РЋРІР‚С” Bitmap / OCR Р В Р’В Р вЂ™Р’В¶Р В Р’В Р В РЎвЂњР В Р’В Р В РІР‚в„–Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎвЂќР В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В©Р В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В РЎС›Р Р†Р вЂљР’ВР В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ
                lastEngine = s.translatorEngine
            }
        }

        CaptureServiceState.setRunning(true)

        // Shizuku Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В·Р В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎС›Р В Р вЂ Р В РІР‚С™Р РЋРІР‚С” dry-runР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р Р‹Р Р†РІР‚С›РЎС›Р В Р’В Р вЂ™Р’ВµР В Р’В Р В Р вЂ°Р В Р Р‹Р Р†Р вЂљРІР‚СљР В Р’В Р СћРІР‚ВР В Р’В Р Р†Р вЂљР’В¦Р В Р Р‹Р Р†Р вЂљРІР‚Сњ availability == READYР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’В¶Р В Р Р‹Р РЋРІвЂћСћР В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р Р‹Р Р†РІР‚С›РЎС›Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р вЂ Р В РІР‚С™Р В Р вЂ№ ADB / root Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљР Р‹Р В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†РІР‚С›РЎС›Р В Р вЂ Р В РІР‚С™Р РЋРІР‚С” Shizuku Р В Р’В Р СћРІР‚ВР В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р Р‹Р РЋРЎСџР В Р’В Р СћРІР‚ВР В Р Р‹Р вЂ™Р’ВР В Р Р‹Р Р†РІР‚С›РЎС›
        // Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В®Р В РІР‚в„ўР вЂ™Р’В© shell Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р В Р РЏР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В¤Р В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р РЋРІР‚ВР В РЎС›Р Р†Р вЂљР’ВР В РЎС›Р РЋРІР‚в„ўР В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В«Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р вЂ™Р’ВР В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В¬Р В Р’В Р В РІР‚в„–Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р В Р РЏР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В¤Р В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р РЋРІР‚ВР В РЎС›Р Р†Р вЂљР’ВР В РЎС›Р РЋРІР‚в„ўР В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р вЂ Р Р†Р вЂљРЎвЂєР РЋРЎвЂєР В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В РІР‚в„ўР вЂ™Р’В¬Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р вЂ Р Р†Р вЂљРЎвЂєР РЋРЎвЂєР В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’В¶Р В Р Р‹Р РЋРЎв„ўР В Р’В Р В РІР‚в„–Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљР Р‹Р В Р Р‹Р вЂ™Р’ВР В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В·Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚Сљ
        // Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р В РЎвЂњ MediaProjection Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В РІР‚в„ўР вЂ™Р’В¶ stopSelfР В Р’В Р В РІР‚В Р В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р В РІР‚В Р В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р вЂ™Р’В¶Р В Р’В Р Р†Р вЂљР Р‹Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В®Р В РІР‚в„ўР вЂ™Р’В©Р В Р’В Р СћРІР‚ВР В РІР‚в„ўР вЂ™Р’В»Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р РЋРІвЂћСћР В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р Р‹Р Р†РІР‚С›РЎС›Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р В РЎвЂњР В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р В Р РЏР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В¤Р В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р РЋРІР‚ВР В РЎС›Р Р†Р вЂљР’ВР В РЎС›Р РЋРІР‚в„ўР В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’ВµР В Р’В Р В Р РЏР В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В¤Р В Р’В Р В Р вЂ°Р В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљР Р‹Р В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р вЂ Р Р†Р вЂљРЎвЂєР РЋРЎвЂєР В Р’В Р вЂ™Р’ВµР В РЎС›Р РЋРІР‚в„ўР В Р’В Р Р†Р вЂљР’В¦Р В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ
        if (useShizuku) {
            scope.launch {
                val shotter = screenshotter ?: return@launch
                val test = shotter.capture()
                if (test == null) {
                    Timber.w("Shizuku dry-run failed; stopping service")
                    logRepository.error(
                        LogRepository.Category.CAPTURE,
                        getString(R.string.log_msg_shizuku_dry_run_failed)
                    )
                    mainScope.launch {
                        overlay?.showErrorHint(
                            getString(R.string.toast_shizuku_dry_run_failed),
                            durationMs = 8000L
                        )
                    }
                    kotlinx.coroutines.delay(8500L)
                    stopSelf()
                } else {
                    test.recycle()
                }
            }
        }
    }

    /*
    private fun startLocalLlmWarmupIfNeeded() {
        localLlmWarmupJob?.cancel()
        localLlmWarmupJob = scope.launch {
            // Avoid running two large cold-start inference workloads against each other. If Manga
            // OCR is selected, let its short real-inference warmup finish before loading the LLM.
            ocrWarmupJob?.join()
            val routing = translator as? RoutingTranslator
            if (routing == null) {
                Timber.tag("LocalLlmPerf").i("prewarm skipped decision=SKIP_ROUTER_UNAVAILABLE")
                return@launch
            }
            val settings = settingsRepository.get()
            val startedAt = SystemClock.elapsedRealtime()
            runCatching { routing.prewarmLocalModel(settings) }
                .onSuccess { result ->
                    Timber.tag("LocalLlmPerf").i(
                        "prewarm decision=%s kind=%s totalMs=%d",
                        result.decision.name,
                        result.modelKind ?: "none",
                        InferenceTiming.elapsedMs(startedAt, SystemClock.elapsedRealtime()),
                    )
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    Timber.tag("LocalLlmPerf").w(
                        error,
                        "prewarm failed engine=%s totalMs=%d",
                        settings.translatorEngine.name,
                        InferenceTiming.elapsedMs(startedAt, SystemClock.elapsedRealtime()),
                    )
                }
        }
    }

    */
    private suspend fun prepareCleanCaptureFrame(
        hideFloatingButton: Boolean
    ) {
        cancelActiveTranslationBatch("prepareCleanCaptureFrame")
        mainScope.launch {
            overlay?.clear()
            translationCard?.dismiss()
            translationBlockCopyOverlay?.dismiss()
            if (hideFloatingButton) floatingButton?.hide()
        }.join()
        Timber.d(
            "Capture chrome hidden before screenshot loadingHidden=true floatingButtonHidden=%s",
            hideFloatingButton,
        )
        delay(CAPTURE_CHROME_SETTLE_MS)
    }

    private fun restoreCaptureChrome(showLoading: Boolean, restoreFloatingButton: Boolean) {
        mainScope.launch {
            if (restoreFloatingButton) floatingButton?.show()
            if (showLoading) overlay?.showLoadingHint()
            Timber.d(
                "Capture chrome restored after screenshot loading=%s floatingButton=%s",
                showLoading,
                restoreFloatingButton,
            )
        }
    }

    /**
     * Р В Р’В Р вЂ™Р’ВµР В Р’В Р В РІР‚В°Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р’В Р РЋРІР‚ВР В Р’В Р Р†РІР‚С™Р’В¬Р В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚Сљ [ServiceCompat.startForeground]Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В¤Р В Р вЂ Р В РІР‚С™Р РЋРІР‚С”Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р вЂ Р В РІР‚С™Р вЂ™Р’В  Android 14+ HyperOS/MIUI Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р Р†Р вЂљР’В°Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†РІР‚С›РЎС›Р В Р вЂ Р В РІР‚С™Р РЋРІР‚С” `android:project_media`
     * app-op raceР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р Р‹Р Р†РІР‚С›РЎС›MediaProjectionRequestActivity Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В° RESULT_OK Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р Р†Р вЂљРІвЂћвЂ“ op grant Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВР В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В­Р В РЎС›Р РЋРІР‚в„ўР В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†РІР‚С›РЎС›Р В Р вЂ Р В РІР‚С™Р РЋРІР‚С”Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В«Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В» startForeground
     * Р В Р’В Р вЂ™Р’ВµР В Р’В Р В Р РЏР В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р Р†Р вЂљР’В¦Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В Р’В Р В РЎвЂњ op Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРІР‚СњР В РІР‚в„ўР вЂ™Р’ВР В Р’В Р вЂ™Р’В¶Р В Р’В Р Р†Р вЂљР’В Р В Р’В Р В РІР‚в„–Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р Р†Р вЂљР’В¦Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В РІР‚в„ўР вЂ™Р’В¶Р В Р’В Р вЂ™Р’В¶Р В Р’В Р Р†Р вЂљР’В°Р В Р вЂ Р В РІР‚С™Р РЋРІР‚Сњ `SecurityException`Р В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ
     *
     * Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВµР В Р’В Р РЋРІР‚СљР В Р’В Р вЂ™Р’В·Р В Р’В Р В РЎвЂњР В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р Р‹Р Р†РІР‚С›РЎС›
     *  1) Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В РЎС›Р Р†Р вЂљР’ВР В Р’В Р вЂ™Р’В¶Р В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В РЎС›Р РЋРІР‚в„ўР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В°Р В Р Р‹Р РЋРЎв„ўР В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљР Р‹Р В Р вЂ Р В РІР‚С™Р РЋРЎвЂє startForeground Р В Р’В Р В РІР‚В Р В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р В РІР‚В Р В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРЎС™ Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В¤Р В Р Р‹Р Р†РІР‚С›РЎС›Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В РІР‚в„ўР вЂ™Р’В° ROM Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В­Р В Р’В Р Р†РІР‚С™Р’В¬Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљР’ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В·Р В РЎС›Р РЋРІР‚в„ўР В Р’В Р СћРІР‚ВР В Р’В Р Р†Р вЂљР’В¦Р В Р Р‹Р РЋРІвЂћСћ
     *  2) Р В Р’В Р вЂ™Р’В¶Р В Р’В Р Р†Р вЂљР’В°Р В Р вЂ Р В РІР‚С™Р РЋРІР‚Сњ SecurityException Р В Р’В Р В РІР‚В Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РЎС› postDelayed 200ms Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р В Р вЂ°Р В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљР Р‹Р В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В¬Р В Р’В Р В РІР‚в„–Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В»Р В Р вЂ Р Р†Р вЂљРЎвЂєР РЋРЎвЂє op Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р Р†Р вЂљР’В¦Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В РІР‚в„ўР вЂ™Р’В¶Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В РЎС›Р Р†Р вЂљР’ВР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°
     *  3) Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р В Р вЂ°Р В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљР Р‹Р В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљР’В°Р В Р Р‹Р РЋРЎСџ Р В Р’В Р В РІР‚В Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РЎС› Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р вЂ™Р’ВР В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р РЋРІР‚СљР В Р’В Р В Р вЂ° handleStartР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬cleanupCapture Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В­Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В РЎС›Р РЋРІР‚в„ўР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В®Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р’В Р В РЎвЂњР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°
     *  4) Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р В Р вЂ°Р В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљР Р‹Р В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В¤Р В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р РЋРІР‚ВР В РЎС›Р Р†Р вЂљР’ВР В РЎС›Р РЋРІР‚в„ў Р В Р’В Р В РІР‚В Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РЎС› Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚Сљ Toast Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљР Р‹Р В Р Р‹Р вЂ™Р’В + stopSelf
     *
     * @return true Р В Р’В Р СћРІР‚ВР В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р Р†РІР‚С™Р’В¬Р В Р’В Р РЋРІР‚ВР В Р’В Р В РІР‚в„–Р В Р’В Р В РЎвЂњ startForeground Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В­Р В РЎС›Р РЋРІР‚в„ўР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљР’В°Р В Р Р‹Р РЋРЎСџР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р’В Р вЂ™Р’ВµР В Р’В Р В Р РЏР В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В»Р В РІР‚в„ўР вЂ™Р’В§Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В»Р В РІР‚в„ўР вЂ™Р’В­Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В»Р В РІР‚в„ўР вЂ™Р’В­Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р Р‹Р РЋРЎв„ўР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В§Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р’В Р вЂ™Р’ВµР В Р’В Р В РІР‚В°Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р РЋРІР‚Сњfalse Р В Р’В Р СћРІР‚ВР В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р Р†РІР‚С™Р’В¬Р В Р’В Р РЋРІР‚ВР В Р’В Р В РІР‚в„–Р В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В·Р В Р’В Р Р†Р вЂљР’В Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В РЎС›Р РЋРІР‚в„ўР В Р’В Р вЂ™Р’ВµР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В­Р В РЎС›Р РЋРІР‚в„ў
     *  Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р В Р вЂ°Р В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљР Р‹Р В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В Р’В Р вЂ™Р’В¶Р В Р’В Р В РЎвЂњР В Р’В Р В РІР‚в„–Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р вЂ™Р’ВР В Р’В Р В Р РЏР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎСљР В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В«Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В» returnР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р РЋРІР‚СљР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’ВµР В Р’В Р В Р РЏР В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р вЂ™Р’В¶Р В Р Р‹Р РЋРІвЂћСћР В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р вЂ™Р’В·Р В Р’В Р В РІР‚в„–Р В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В«Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В РІР‚в„ўР вЂ™Р’В¶Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р Р‹Р РЋРЎв„ўР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В§Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р’В Р вЂ™Р’ВµР В Р’В Р В РІР‚В°Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р В Р РЏ / Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В РІР‚в„ўР вЂ™Р’В¬Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р вЂ™Р’В·Р В Р’В Р Р†Р вЂљРЎвЂєР В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ
     */
    private fun startForegroundCompat(fgType: Int, originalIntent: Intent): Boolean {
        val tryStart = {
            ServiceCompat.startForeground(
                this,
                CaptureNotification.NOTIF_ID,
                CaptureNotification.build(this),
                fgType
            )
        }
        return try {
            tryStart()
            true
        } catch (se: SecurityException) {
            Timber.w(se, "startForeground SecurityException; retry in 200ms")
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    tryStart()
                    Timber.i("startForeground retry succeeded; rerunning handleStart")
                    handleStart(originalIntent)
                } catch (e2: SecurityException) {
                    Timber.e(e2, "startForeground retry also failed")
                    logRepository.error(
                        LogRepository.Category.CAPTURE,
                        "startForeground SecurityException after retry: ${e2.message}",
                        e2
                    )
                    stopSelf()
                }
            }, 200L)
            false
        }
    }

    private fun triggerOnce() {
        if (captureLock.isLocked) {
            Timber.i("Skip manual trigger because capture is already running")
            return
        }
        // Give immediate feedback, but remove every app overlay before MediaProjection
        // captures the frame. The floating button and loading indicator return only
        // after the screenshot bitmap has been acquired.
        scope.launch {
            mainScope.async { overlay?.showLoadingHint() }.await()
            prepareCleanCaptureFrame(hideFloatingButton = true)
            captureOnce(
                showLoadingAfterScreenshot = true,
                restoreFloatingButtonAfterScreenshot = true
            )
        }
    }

    /** Р В Р’В Р вЂ™Р’В¶Р В Р’В Р В РІР‚В°Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В° rect Р В Р’В Р РЋРІР‚ВР В Р’В Р Р†РІР‚С™Р’В¬Р В Р’В Р РЋРІР‚СљР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р Р‹Р Р†Р вЂљРЎСљ bitmapР В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћrect Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В¶Р В Р’В Р Р†Р вЂљР’В°Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В Р’В Р В РІР‚В°Р В Р’В Р РЋРІР‚ВР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљР’В°Р В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В¤Р В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В Р Р‹Р Р†Р вЂљРЎвЂќР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В Р’В Р РЋРІР‚ВР В Р’В Р Р†РІР‚С™Р’В¬Р В Р’В Р РЋРІР‚СљР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р Р†Р вЂљРІвЂћвЂ“ Р В Р’В Р В РІР‚В Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В РІР‚в„ўР вЂ™Р’В¤ 8px Р В Р’В Р СћРІР‚ВР В РІР‚в„ўР вЂ™Р’В»Р В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎС›Р В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В§Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В РІР‚в„ўР вЂ™Р’В Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В Р Р‹Р Р†Р вЂљРЎвЂќ nullР В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ */
    private fun cropRect(src: Bitmap, rect: android.graphics.Rect): Bitmap? {
        val l = rect.left.coerceIn(0, src.width)
        val t = rect.top.coerceIn(0, src.height)
        val r = rect.right.coerceIn(0, src.width)
        val b = rect.bottom.coerceIn(0, src.height)
        if (r - l <= 8 || b - t <= 8) return null
        return Bitmap.createBitmap(src, l, t, r - l, b - t)
    }

    private fun shouldRerunForTextLine180(result: OrientationResult): Boolean =
        result.source == PaddleTextLineOrientationClassifier.SOURCE && result.rawAngle == 180


    private fun rotateBitmap180(bitmap: Bitmap): Bitmap {
        val matrix = Matrix().apply { postRotate(180f) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р вЂ™Р’В¶Р В Р’В Р В Р вЂ°Р В Р Р‹Р РЋРІР‚С”Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р вЂ™Р’В¶Р В Р’В Р Р†Р вЂљР’В°Р В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р Р†Р вЂљР’В¦Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р Р‹Р Р†РІР‚С›РЎС›Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р вЂ Р Р†Р вЂљРЎвЂєР РЋРЎвЂє Settings Р В Р’В Р вЂ™Р’В¶Р В Р’В Р В РІР‚В°Р В Р’В Р РЋРІР‚СљР В Р’В Р СћРІР‚ВР В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р’В Р вЂ™Р’ВµР В Р’В Р В РІР‚В°Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™ + Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В­Р В РЎС›Р РЋРІР‚в„ў floatingButton Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В­Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В®Р В РІР‚в„ўР вЂ™Р’Вµ + Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р Р†Р вЂљРЎвЂє info Р В Р’В Р вЂ™Р’В¶Р В Р’В Р В Р РЏР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В¤Р В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’ВР В Р’В Р Р†Р вЂљР’В°Р В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљР Р‹Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В·
     * Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р РЋРІР‚ВР В Р’В Р В РІР‚в„–Р В Р’В Р В РІР‚В°Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћsettings flow collect Р В Р’В Р СћРІР‚ВР В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р Р‹Р РЋРЎСџР В Р’В Р СћРІР‚ВР В Р Р‹Р вЂ™Р’ВР В Р Р‹Р Р†РІР‚С›РЎС›Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В§Р В РІР‚в„ўР вЂ™Р’В¦Р В Р’В Р вЂ™Р’ВµР В Р’В Р В Р РЏР В Р вЂ Р В РІР‚С™Р вЂ™Р’В applyOverlayConfig Р В Р’В Р вЂ™Р’В¶Р В Р’В Р Р†Р вЂљР’В°Р В Р’В Р Р†Р вЂљР’В°Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В Р Р‹Р Р†Р вЂљРЎС›Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В Р В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В­Р В РЎС›Р РЋРІР‚в„ўР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р СћРІР‚ВР В Р’В Р Р†Р вЂљР’В¦Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р вЂ Р Р†Р вЂљРЎвЂєР РЋРЎвЂєР В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В РЎС›Р Р†Р вЂљР’ВР В Р’В Р вЂ™Р’В¶Р В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В РЎС›Р РЋРІР‚в„ўР В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎС™
     * applySkillIcon Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р РЋРІР‚СљР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р’В Р В Р вЂ°Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В¬Р В Р’В Р В РІР‚в„–Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В·Р В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†Р вЂљРЎСљР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р’В Р вЂ™Р’В·Р В Р’В Р В РЎвЂњР В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В»Р В РІР‚в„ўР вЂ™Р’В¶Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р Р‹Р РЋРЎСџР В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ
     */
    private fun applyFloatingSkill(newSkill: FloatingSkill) {
        if (newSkill != FloatingSkill.LOOP && loopMode) toggleLoopMode()
        scope.launch {
            settingsRepository.update { it.copy(floatingButtonSkill = newSkill) }
        }
        floatingButton?.skill = newSkill
        mainScope.launch { floatingButton?.applySkillIcon() }
        val msgRes = when (newSkill) {
            FloatingSkill.FULL_SCREEN -> R.string.toast_skill_switched_full_screen
            FloatingSkill.WORD_SELECT -> R.string.toast_skill_switched_full_screen
            FloatingSkill.LOOP -> R.string.toast_skill_switched_loop
        }
        mainScope.launch { overlay?.showInfoHint(getString(msgRes)) }
    }

    private fun showLanguageQuickSwitchOverlay() {
        val panel = languageQuickSwitch ?: LanguageQuickSwitchOverlay(this).also {
            languageQuickSwitch = it
        }
        if (panel.isShown()) return
        scope.launch {
            val settings = settingsRepository.get()
            mainScope.launch {
                panel.show(settings) { source, target ->
                    if (!translationLanguageCodesConflict(source, target)) {
                        scope.launch {
                            settingsRepository.update {
                                it.copy(sourceLang = source, targetLang = target)
                            }
                        }
                        overlay?.showInfoHint(
                            getString(
                                R.string.language_quick_updated_format,
                                Languages.nameOf(this@CaptureService, source),
                                Languages.nameOf(this@CaptureService, target)
                            )
                        )
                    }
                }
            }
        }
    }

    private fun showPresetQuickSwitchOverlay() {
        val panel = presetQuickSwitch ?: PresetQuickSwitchOverlay(this).also {
            presetQuickSwitch = it
        }
        if (panel.isShown()) return
        scope.launch {
            val settings = settingsRepository.get()
            mainScope.launch {
                panel.show(settings) { preset ->
                    scope.launch {
                        settingsRepository.update { current ->
                            preset.applyTo(current).copy(activeTranslationPresetId = preset.id)
                        }
                    }
                    overlay?.showInfoHint(
                        getString(
                            R.string.preset_quick_applied_format,
                            presetDisplayName(preset)
                        )
                    )
                }
            }
        }
    }

    private fun selectHistoryBlock(action: (OverlayManager.HistoryBlock) -> Unit) {
        val items = overlay?.currentHistoryBlocks().orEmpty()
        when (items.size) {
            0 -> overlay?.showInfoHint(getString(R.string.history_action_no_blocks))
            1 -> action(items.single())
            else -> {
                val picker = historyBlockPicker ?: HistoryBlockPickerOverlay(this).also { historyBlockPicker = it }
                picker.show(items, action)
            }
        }
    }

    private fun selectDisplayedBlockForCorrection() {
        val items = overlay?.currentDisplayedTranslationBlocks().orEmpty()
        when (items.size) {
            0 -> overlay?.showInfoHint(getString(R.string.history_action_no_blocks))
            1 -> showHistoryCorrectionEditor(items.single())
            else -> {
                val picker = historyBlockPicker ?: HistoryBlockPickerOverlay(this).also { historyBlockPicker = it }
                picker.showDisplayed(items, ::showHistoryCorrectionEditor)
            }
        }
    }

    private fun showHistoryCorrectionEditor(block: OverlayManager.DisplayedTranslationBlock) {
        val historyId = block.historyId ?: run {
            overlay?.showInfoHint(getString(R.string.history_correction_missing_id))
            return
        }
        val editor = historyCorrectionOverlay ?: HistoryCorrectionOverlay(this).also {
            historyCorrectionOverlay = it
        }
        editor.show(block) { source, translation, requestId, completed ->
            scope.launch {
                val settings = settingsRepository.get()
                runCatching {
                    remotePcTranslator.submitHistoryCorrection(
                        historyId = historyId,
                        source = source,
                        translation = translation,
                        clientRequestId = requestId,
                        settings = settings,
                    )
                }.onSuccess { result ->
                    mainScope.launch {
                        if (result.status == "pending") {
                            overlay?.replaceDisplayedBlock(block.overlayIndex, source, translation)
                            overlay?.showInfoHint(getString(R.string.history_correction_sent))
                            completed(Result.success(Unit))
                        } else {
                            completed(Result.failure(TranslationException("Unexpected status: ${result.status}")))
                            overlay?.showErrorHint(getString(R.string.history_correction_failed, result.status))
                        }
                    }
                }.onFailure { error ->
                    mainScope.launch {
                        completed(Result.failure(error))
                        overlay?.showErrorHint(getString(R.string.history_correction_failed, shortError(error)))
                    }
                }
            }
        }
    }

    private fun retranslateHistoryBlock(block: OverlayManager.HistoryBlock) {
        Timber.i("HISTORY ACTION retranslate id=${block.historyId}")
        scope.launch {
            val settings = settingsRepository.get()
            mainScope.launch { overlay?.showLoadingHint() }
            runCatching { remotePcTranslator.retranslateHistory(block.historyId, settings) }
                .onSuccess { result ->
                    mainScope.launch {
                        overlay?.clearLoading()
                        overlay?.replaceHistoryBlockTranslation(block.overlayIndex, result.translation)
                    }
                }
                .onFailure { error ->
                    Timber.w(error, "HISTORY ACTION retranslate failed id=${block.historyId}")
                    mainScope.launch {
                        overlay?.clearLoading()
                        overlay?.showErrorHint(getString(R.string.history_action_failed, shortError(error)))
                    }
                }
        }
    }

    private fun deleteHistoryBlock(block: OverlayManager.HistoryBlock) {
        Timber.i("HISTORY ACTION delete id=${block.historyId}")
        scope.launch {
            val settings = settingsRepository.get()
            runCatching { remotePcTranslator.deleteHistory(block.historyId, settings) }
                .onSuccess { mainScope.launch { overlay?.removeHistoryBlock(block.overlayIndex) } }
                .onFailure { error ->
                    Timber.w(error, "HISTORY ACTION delete failed id=${block.historyId}")
                    mainScope.launch {
                        overlay?.showErrorHint(getString(R.string.history_action_failed, shortError(error)))
                    }
                }
        }
    }

    private fun presetDisplayName(preset: TranslationPreset): String = when (preset.id) {
        TranslationPresetCatalog.BUILTIN_MANGA_JA_ZH ->
            getString(R.string.settings_translation_preset_builtin_manga)
        else -> preset.name
    }

    private fun toggleLoopMode() {
        if (loopMode) {
            loopMode = false
            loopJob?.cancel()
            loopJob = null
            resetLoopFrameHistory()
            resetLoopRuntimeState()
            mainScope.launch {
                // Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В РІР‚в„ўР вЂ™Р’В­Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В Р Р‹Р Р†Р вЂљРЎвЂќ OFFР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р Р‹Р Р†РІР‚С›РЎС›Р В Р’В Р РЋРІР‚ВР В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В РЎС›Р РЋРІР‚в„ўР В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РЎС›Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р В РІР‚в„–Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В РІР‚в„ўР вЂ™Р’В¶Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРІР‚СњР В РІР‚в„ўР вЂ™Р’ВР В Р’В Р вЂ™Р’В¶Р В Р’В Р Р†Р вЂљР’В Р В Р’В Р В РІР‚в„–Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В¤Р В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В«Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РЎС›Р В РІР‚в„ўР вЂ™Р’В¤Р В Р’В Р вЂ™Р’В¶Р В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В°
                overlay?.cancelStartCountdown()
                floatingButton?.setLoopActive(false, 0L)
            }
            Timber.i("Loop mode OFF")
            logRepository.info(LogRepository.Category.CAPTURE, getString(R.string.log_msg_loop_off))
            val msg = getString(R.string.toast_loop_off)
            mainScope.launch { overlay?.showInfoHint(msg) }
        } else {
            resetLoopFrameHistory()
            resetLoopRuntimeState()
            loopMode = true
            // Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р В Р РЏР В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В РІР‚в„ўР вЂ™Р’В­Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В¤Р В РІР‚в„ўР вЂ™Р’В® 3-2-1 Р В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РЎС›Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р В РІР‚в„–Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В РІР‚в„ўР вЂ™Р’В¶Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬ removeView + ~80ms VSYNC Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р РЋРЎв„ўР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р’В Р Р†Р вЂљР’В Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљР’В°Р В Р’В Р В РЎвЂњ loopJobР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°
            // Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р Р‹Р РЋРЎв„ўР В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р РЋРІР‚СљР В Р’В Р Р†РІР‚С›РІР‚вЂњР В РІР‚в„ўР вЂ™Р’В¦Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В¬Р В Р’В Р В РІР‚в„– captureOnce Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р В Р РЏР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В РІР‚в„ўР вЂ™Р’В¶Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р Р‹Р РЋРЎв„ўР В Р Р‹Р РЋРІР‚С”Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р’В Р Р†Р вЂљР’В Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В·Р В Р’В Р Р†Р вЂљР’В Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В¤Р В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћshowInfoHint Р В Р’В Р вЂ™Р’В¶Р В Р’В Р В Р РЏР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В¤Р В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р вЂ™Р’В¶Р В Р Р‹Р РЋРЎв„ўР В Р’В Р В РІР‚в„–Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В Р’В Р В РЎвЂњР В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В РІР‚в„ўР вЂ™Р’В» OCR Р В Р’В Р вЂ™Р’ВµР В Р’В Р В РІР‚В°Р В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРЎСџР В Р Р‹Р РЋРЎСџР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°
            // Р В Р’В Р вЂ™Р’ВµР В Р’В Р В Р РЏР В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р Р†Р вЂљР’В¦Р В Р’В Р РЋРІР‚ВР В Р Р‹Р РЋРІР‚С”Р В РІР‚в„ўР вЂ™Р’В«Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р СћРІР‚ВР В Р’В Р Р†Р вЂљР’В¦Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В¦Р В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В Р’В Р вЂ™Р’В·Р В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р СћРІР‚ВР В Р’В Р Р†Р вЂљР’В¦Р В Р’В Р Р†Р вЂљРІвЂћвЂ“ & Р В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљР’В¦Р В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р РЋРЎв„ўР В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р В Р РЏР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р Р‹Р РЋРЎв„ўР В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В Р вЂ Р Р†Р вЂљРЎвЂєР РЋРЎвЂєР В Р’В Р СћРІР‚ВР В РІР‚в„ўР вЂ™Р’В»Р В РЎС›Р РЋРІР‚в„ўР В Р’В Р вЂ™Р’В¶Р В Р’В Р В Р РЏР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљРЎС›Р В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В РЎС›Р Р†Р вЂљР’ВР В Р’В Р вЂ™Р’ВµР В Р’В Р В Р РЏР В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р Р‹Р РЋРЎв„ўР В РІР‚в„ўР вЂ™Р’В Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†РІР‚С›РЎС›Р В Р вЂ Р В РІР‚С™Р РЋРІР‚С”Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В§Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В§Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р вЂ™Р’ВµР В Р’В Р В Р РЏР В Р’В Р В Р вЂ°Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В РІР‚в„ўР вЂ™Р’В¦Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ
            scope.launch {
                val s = settingsRepository.get()
                val interval = if (s.captureLoopIntervalMs <= 0) 2000L else s.captureLoopIntervalMs
                val secsStr = if (interval % 1000L == 0L) {
                    (interval / 1000L).toString()
                } else {
                    String.format(java.util.Locale.US, "%.1f", interval / 1000.0)
                }
                val smartTrigger = s.loopTriggerMode == LoopTriggerMode.WAIT_FOR_TEXT_COMPLETE
                val msg = if (smartTrigger) {
                    getString(R.string.toast_loop_on_smart, s.loopTextStableDurationMs)
                } else {
                    getString(R.string.toast_loop_on, secsStr)
                }
                val indicator = LoopRuntimePolicy.indicatorSpec(interval, smartTrigger)
                VerticalDiagnosticLog.i(
                    "loop start mode=${s.loopTriggerMode.name} intervalMs=$interval " +
                        "pollMs=${LoopFrameStabilityPolicy.pollingIntervalMs(interval, smartTrigger)} " +
                        "stableMs=${s.loopTextStableDurationMs} skipSimilar=${s.loopSkipSimilarFrames} " +
                        "similarity=${s.loopFrameSimilarityThreshold.toDiagFloat()} " +
                        "textRegion=${s.loopTextRegionMode.name} regionOnly=${s.loopTranslateRegionOnly} " +
                        "indicator=${indicator.mode.name}/${indicator.periodMs}ms"
                )
                mainScope.launch {
                    // Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В РІР‚в„ўР вЂ™Р’В¬Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р вЂ™Р’В¶Р В Р’В Р В Р РЏР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В¤Р В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р Р‹Р Р†Р вЂљРЎС›Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РЎС›Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р В РІР‚в„–Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В РІР‚в„ўР вЂ™Р’В¶Р В Р’В Р СћРІР‚ВР В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р В Р вЂ°Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р Р‹Р Р†РІР‚С›РЎС›Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В®Р В РІР‚в„ўР вЂ™Р’В©Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В·Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р РЋРІвЂћСћР В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р В РІР‚В°Р В Р’В Р РЋРІР‚ВР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљР’В°Р В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†Р вЂљРІР‚СњР В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљР Р‹Р В Р вЂ Р В РІР‚С™Р вЂ™Р’ВР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В·Р В Р’В Р Р†Р вЂљР’В Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р вЂ™Р’ВР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р вЂ™Р’В¶Р В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р В Р РЏ xx Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В§Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РЎС›Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В¬Р В Р’В Р В РІР‚в„–Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†РІР‚С›РЎС›Р В Р вЂ Р В РІР‚С™Р РЋРІР‚С”Р В Р’В Р вЂ™Р’ВµР В Р’В Р В Р РЏР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р вЂ™Р’В·Р В Р’В Р В РІР‚в„–Р В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В®Р В РІР‚в„ўР вЂ™Р’В¤Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°
                    // Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р РЋРІвЂћСћР В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњ 3-2-1 Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћshowInfoHint Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В РІР‚в„ўР вЂ™Р’В»Р В РІР‚в„ўР вЂ™Р’ВР В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В®Р В РІР‚в„ўР вЂ™Р’В¤ 1800ms Р В Р’В Р РЋРІР‚ВР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљР’В°Р В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В¤Р В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РЎС›Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р В РІР‚в„–Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В РІР‚в„ўР вЂ™Р’В¶Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРІР‚СњР В РІР‚в„ўР вЂ™Р’ВР В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В Р’В Р В РЎвЂњР В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В Р’В Р РЋРІР‚ВР В Р’В Р В РІР‚в„–Р В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В РІР‚в„ўР вЂ™Р’В¶Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В°Р В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В·Р В Р’В Р Р†Р вЂљР’В Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р В Р РЏР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В·Р В Р’В Р В РІР‚в„–Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°
                    // Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р В Р вЂ°Р В Р’В Р СћРІР‚ВР В Р Р‹Р вЂ™Р’ВР В Р Р‹Р Р†РІР‚С›РЎС›Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В·Р В Р Р‹Р РЋРЎСџР В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В§Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В§Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р вЂ Р В РІР‚С™Р РЋРЎв„ўР В Р’В Р вЂ™Р’В¶Р В Р Р‹Р Р†Р вЂљРЎвЂќР В РІР‚в„ўР вЂ™Р’В¶Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р вЂ Р В РІР‚С™Р РЋРЎС™ OCR Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РЎС›Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р В РІР‚в„–Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В РІР‚в„ўР вЂ™Р’В¶Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В»Р В Р вЂ Р В РІР‚С™Р РЋРЎв„ўР В Р’В Р вЂ™Р’В¶Р В Р Р‹Р РЋРЎв„ўР В Р Р‹Р РЋРЎСџ +80ms Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р вЂ™Р’ВР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В§Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р В Р вЂ°Р В Р’В Р СћРІР‚ВР В Р Р‹Р вЂ™Р’ВР В Р Р‹Р Р†РІР‚С›РЎС›Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р вЂ™Р’В¶Р В Р’В Р В Р РЏР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В¤Р В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р вЂ™Р’В¶Р В Р Р‹Р РЋРЎв„ўР В Р’В Р В РІР‚в„–Р В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ
                    overlay?.showInfoHint(msg)
                    overlay?.showStartCountdown(
                        seconds = 3,
                        hintText = getString(R.string.loop_countdown_hint)
                    ) {
                        // onFinish Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В РІР‚в„ўР вЂ™Р’В¶Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В·Р В Р’В Р Р†Р вЂљР’В Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р В Р РЏ removeView Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р вЂ Р В РІР‚С™Р РЋРЎС™ ~80ms VSYNC Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р РЋРЎв„ўР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р’В Р Р†Р вЂљР’В Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р вЂ Р В РІР‚С™Р В Р вЂ№
                        if (!loopMode) return@showStartCountdown // Р В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РЎС›Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р В РІР‚в„–Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В РІР‚в„ўР вЂ™Р’В¶Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В РІР‚в„ўР вЂ™Р’В­Р В Р’В Р РЋРІР‚ВР В Р Р‹Р РЋРІР‚С”Р В РІР‚в„ўР вЂ™Р’В«Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р Р‹Р Р†Р вЂљРІР‚СљР В Р’В Р вЂ™Р’В¶Р В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В°
                        floatingButton?.setLoopActive(
                            active = true,
                            intervalMs = indicator.periodMs,
                            indeterminate = indicator.mode == LoopIndicatorMode.INDETERMINATE,
                        )
                        loopJob = scope.launch {
                            while (isActive && loopMode) {
                                captureOnce()
                                val s2 = settingsRepository.get()
                                val ivl = LoopFrameStabilityPolicy.pollingIntervalMs(
                                    configuredLoopIntervalMs = s2.captureLoopIntervalMs,
                                    enabled = s2.loopTriggerMode == LoopTriggerMode.WAIT_FOR_TEXT_COMPLETE,
                                )
                                delay(ivl)
                            }
                        }
                    }
                }
            }
            Timber.i("Loop mode ON")
            logRepository.info(LogRepository.Category.CAPTURE, getString(R.string.log_msg_loop_on))
        }
    }

    private fun showTranslationBlockCopyPanel(source: String, translation: String) {
        scope.launch {
            val settings = settingsRepository.get()
            mainScope.launch {
                translationCard?.dismiss()
                val copyOverlay = translationBlockCopyOverlay
                    ?: TranslationBlockCopyOverlay(
                        context = this@CaptureService,
                        onDismissed = {},
                    ).also {
                        translationBlockCopyOverlay = it
                    }
                copyOverlay.show(
                    sourceText = source,
                    translation = translation,
                    settings = settings,
                )
            }
        }
    }

    private fun resetLoopFrameHistory() {
        previousLoopFingerprint = null
        previousLoopOcrText = null
        loopFrameStabilityState = LoopFrameStabilityState()
        loopRoiTextFallbackActive = LoopRoiFallbackPolicy.transition(
            loopRoiTextFallbackActive,
            LoopRoiFallbackEvent.RESET,
        )
    }

    private fun resetLoopRuntimeState() {
        loopSessionId += 1L
        loopTranslationInFlight = false
        lastLoopRuntimeLogState = null
    }

    private fun beginTranslationBatch(batchId: Long) {
        val previousBatchId = translationBatchGate.activate(batchId)
        translationRenderJob?.cancel()
        translationRenderJob = null
        logVerticalDiag(
            batchId,
            "translation batch activated previous=${previousBatchId ?: "none"}",
        )
    }

    private fun cancelActiveTranslationBatch(reason: String) {
        val previousBatchId = translationBatchGate.invalidate()
        translationRenderJob?.cancel()
        translationRenderJob = null
        previousBatchId?.let {
            logVerticalDiag(it, "translation batch invalidated reason=$reason")
        }
    }

    private fun ensureCurrentTranslationBatch(batchId: Long?) {
        if (!translationBatchGate.accepts(batchId)) {
            throw CancellationException("Stale translation batch")
        }
    }

    private fun launchTranslationBatch(
        batchId: Long?,
        block: suspend CoroutineScope.() -> Unit,
    ) {
        if (!translationBatchGate.accepts(batchId)) return
        translationRenderJob?.cancel()
        translationRenderJob = scope.launch(block = block)
    }

    private fun beginLoopTranslation(diagId: Long?): Long? {
        if (!loopMode) return null
        val sessionId = loopSessionId
        loopTranslationInFlight = true
        lastLoopRuntimeLogState = "translation_started"
        diagId?.let { logVerticalDiag(it, "loop runtime translation started session=$sessionId") }
        return sessionId
    }

    private fun finishLoopTranslation(
        diagId: Long?,
        sessionId: Long?,
    ) {
        if (sessionId == null || sessionId != loopSessionId || !loopMode) return
        loopTranslationInFlight = false
        lastLoopRuntimeLogState = "translation_finished"
        diagId?.let {
            logVerticalDiag(it, "loop runtime translation finished manualDismissRequired=true")
        }
    }

    private fun markLoopResultVisible(diagId: Long?) {
        if (!loopMode) return
        loopTranslationInFlight = false
        lastLoopRuntimeLogState = "result_displayed"
        diagId?.let { logVerticalDiag(it, "loop runtime result visible manualDismissRequired=true") }
    }

    private fun logLoopRuntimeTransition(diagId: Long, state: String, message: String) {
        if (lastLoopRuntimeLogState == state) return
        lastLoopRuntimeLogState = state
        logVerticalDiag(diagId, message)
    }

    private fun createLoopFrameFingerprint(
        bitmap: Bitmap,
        settings: Settings,
    ): LoopFrameFingerprint? {
        val needsFingerprint = settings.loopSkipSimilarFrames ||
            settings.loopTriggerMode == LoopTriggerMode.WAIT_FOR_TEXT_COMPLETE
        if (!loopMode || !needsFingerprint) {
            if (loopMode) resetLoopFrameHistory()
            return null
        }
        val exclusion = floatingButton?.captureExclusionRect()?.let { screenRect ->
            val offsetX = settings.captureRegion?.left ?: 0
            val offsetY = settings.captureRegion?.top ?: 0
            Rect(screenRect).apply { offset(-offsetX, -offsetY) }
                .takeIf { it.intersect(0, 0, bitmap.width, bitmap.height) }
        }
        return LoopFrameFingerprintFactory.create(
            bitmap = bitmap,
            contextId = settings.hashCode(),
            excludedRect = exclusion,
        )
    }

    private fun selectLoopTextRoi(
        blocks: List<TextBlock>,
        bitmap: Bitmap,
        mode: LoopTextRegionMode,
    ): LoopTextRect? = LoopTextRoiPolicy.select(
        candidates = blocks.map { block ->
            val box = block.boundingBox
            LoopTextRoiCandidate(
                text = block.text,
                rect = LoopTextRect(box.left, box.top, box.right, box.bottom),
            )
        },
        imageWidth = bitmap.width,
        imageHeight = bitmap.height,
        mode = mode,
    )

    private fun commitLoopFrame(
        fingerprint: LoopFrameFingerprint?,
        normalizedOcrText: String,
    ) {
        if (!loopMode || fingerprint == null) return
        previousLoopFingerprint = fingerprint
        previousLoopOcrText = normalizedOcrText
    }

    /**
     * Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљР’В°Р В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В РІР‚в„ўР вЂ™Р’В¬Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р вЂ™Р’В·Р В Р’В Р Р†Р вЂљРЎвЂєР В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р вЂ™Р’ВµР В Р’В Р В РІР‚В°Р В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРЎСџР В Р Р‹Р РЋРЎСџР В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В РІР‚в„ўР вЂ™Р’В©Р В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВµР В Р’В Р РЋРІР‚СљР В Р’В Р вЂ™Р’В·Р В Р’В Р В РЎвЂњР В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р Р‹Р Р†РІР‚С›РЎС›
     *  1) Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р вЂ™Р’В¶Р В Р’В Р В РІР‚в„–Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р В Р вЂ°Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р Р‹Р Р†РІР‚С›РЎС›Р В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р РЋРІР‚ВР В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В Р’В Р В Р РЏР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В РІР‚в„ўР вЂ™Р’В¬Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р РЋРІР‚СљР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р’В Р В Р вЂ°Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р РЋРІР‚СљР В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р вЂ™Р’В¶Р В Р’В Р В РІР‚В°Р В Р’В Р В РІР‚в„– + Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В РІР‚в„ўР вЂ™Р’ВР В Р’В Р Р†Р вЂљР’В Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В­Р В Р Р‹Р РЋРІР‚С”Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В®Р В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р вЂ™Р’ВР В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р вЂ™Р’ВµР В Р’В Р В РІР‚В°Р В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљР’В¦Р В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р РЋРЎв„ўР В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В»Р В РІР‚в„ўР вЂ™Р’В­Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р В Р РЏ OCRР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°
     *  2) Р В Р’В Р вЂ™Р’В¶Р В Р’В Р Р†Р вЂљР’В°Р В Р’В Р Р†Р вЂљР’В°Р В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљР’В¦Р В Р вЂ Р В РІР‚С™Р РЋРЎв„ўР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р В Р вЂ° captureRegion Р В Р’В Р СћРІР‚ВР В Р’В Р Р†Р вЂљР’В¦Р В Р Р‹Р РЋРІвЂћСћР В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р Р‹Р РЋРЎв„ўР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В§Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р’В Р вЂ™Р’В¶Р В Р’В Р В РІР‚в„–Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р’В Р СћРІР‚ВР В Р Р‹Р вЂ™Р’ВР В РІР‚в„ўР вЂ™Р’В Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В РЎС›Р РЋРІР‚в„ўР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В·Р В Р’В Р вЂ™Р’ВµР В Р’В Р В Р РЏР В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В Р Р‹Р РЋРЎСџР В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРЎСџР В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р вЂ™Р’В·Р В Р’В Р В РІР‚в„–Р В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р Р†Р вЂљР’В°Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В РЎС›Р Р†Р вЂљР’В
     *  3) Р В Р’В Р вЂ™Р’В·Р В Р’В Р В РІР‚в„–Р В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В®Р В РІР‚в„ўР вЂ™Р’В¤ Р В Р’В Р В РІР‚В Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РЎС› Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р вЂ Р Р†Р вЂљРЎвЂєР РЋРЎвЂєР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В Р Р‹Р Р†Р вЂљРЎвЂќ SettingsР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В Р’В Р вЂ™Р’ВµР В Р’В Р В Р РЏР В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬ Р В Р’В Р В РІР‚В Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РЎС› Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р вЂ Р Р†Р вЂљРЎвЂєР РЋРЎвЂє
     *  4) Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р В РІР‚в„–Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р РЋРЎв„ўР В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В§Р В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В»Р В Р вЂ Р В РІР‚С™Р РЋРЎв„ўР В Р’В Р вЂ™Р’В¶Р В Р Р‹Р РЋРЎв„ўР В Р Р‹Р РЋРЎСџР В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р Р†Р вЂљР’В¦Р В Р’В Р вЂ™Р’В¶Р В Р’В Р РЋРІР‚СљР В Р Р‹Р РЋРІР‚С”Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В¤Р В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В РІР‚в„ўР вЂ™Р’В¬Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р Р‹Р Р†Р вЂљРЎС™
     *
     * Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В¤Р В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р’В Р РЋРІР‚ВР В Р’В Р В Р РЏР В Р Р‹Р РЋРІвЂћСћР В Р’В Р вЂ™Р’ВµР В Р’В Р В Р вЂ°Р В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В РІР‚в„ўР вЂ™Р’В¶Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р Р‹Р Р†РІР‚С›РЎС›Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р вЂ Р В РІР‚С™Р В Р вЂ№ [RegionPickerOverlay.isShown] Р В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р В Р вЂ°Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р Р‹Р РЋРІвЂћСћР В Р’В Р вЂ™Р’ВµР В Р’В Р В Р РЏР В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р вЂ™Р’В¶Р В Р Р‹Р РЋРІвЂћСћР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р Р†Р вЂљРЎвЂє pickerР В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ
     */
    private fun showRegionPickerOverlay() {
        val picker = regionPicker ?: RegionPickerOverlay(this).also { regionPicker = it }
        if (picker.isShown()) return
        mainScope.launch {
            // Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљР’В¦Р В Р вЂ Р В РІР‚С™Р РЋРЎв„ўР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р В Р РЏР В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљР Р‹Р В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р вЂ™Р’В¶Р В Р’В Р Р†Р вЂљР’В°Р В Р’В Р Р†Р вЂљР’В° region rescale Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р РЋРІР‚СљР В Р’В Р В Р вЂ°Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’В·Р В Р’В Р В РІР‚в„–Р В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р Р‹Р РЋРЎв„ў initial Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВР В Р Р‹Р Р†Р вЂљРЎС›Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В¤Р В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљР’В¦Р В Р вЂ Р В РІР‚С™Р РЋРЎв„ўР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р вЂ Р В РІР‚С™Р вЂ™Р’ВР В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†РІР‚С›РЎС›Р В Р вЂ Р В РІР‚С™Р РЋРІР‚С”Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В­Р В Р’В Р Р†РІР‚С™Р’В¬Р В Р’В Р вЂ™Р’В·Р В Р’В Р В РІР‚в„–Р В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р СћРІР‚ВР В Р’В Р Р†Р вЂљР’В¦Р В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’В·Р В Р’В Р Р†Р вЂљР’В¦Р В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ
            val screen = physicalDisplaySize(this@CaptureService)
            settingsRepository.rescaleCaptureRegionIfNeeded(screen.width, screen.height)
            val initial = settingsRepository.get().captureRegion?.let {
                android.graphics.Rect(it.left, it.top, it.right, it.bottom)
            }
            floatingButton?.hide()
            picker.show(
                initial = initial,
                onConfirm = { rect ->
                    scope.launch {
                        val savedScreen = physicalDisplaySize(this@CaptureService)
                        settingsRepository.update {
                            it.copy(
                                captureRegion = CaptureRegion(rect.left, rect.top, rect.right, rect.bottom),
                                captureRegionSavedScreenW = savedScreen.width,
                                captureRegionSavedScreenH = savedScreen.height
                            )
                        }
                    }
                    mainScope.launch { floatingButton?.show() }
                },
                onCancel = {
                    mainScope.launch { floatingButton?.show() }
                },
                onClearAll = {
                    // Р В Р’В Р вЂ™Р’ВµР В Р’В Р В Р РЏР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В РІР‚в„ўР вЂ™Р’В» = Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В РІР‚в„ўР вЂ™Р’В©Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В РЎС›Р Р†Р вЂљР’ВР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р В Р РЏР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р Р‹Р Р†РІР‚С›РЎС›Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В·Р В Р Р‹Р РЋРЎСџР В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р В Р РЏР В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’В¶Р В Р Р‹Р Р†Р вЂљР’ВР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р Р†Р вЂљРЎвЂєР РЋРЎвЂєР В РІР‚в„ўР вЂ™Р’В¤Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р вЂ™Р’В¶Р В Р’В Р В РІР‚в„–Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’В¶Р В Р’В Р В РІР‚В°Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РЎС›Р В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р’В Р В РЎвЂњР В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚ВР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В РЎС›Р Р†Р вЂљР’ВР В Р’В Р В РІР‚В Р В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р В РІР‚В Р В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРЎС™captureRegion=nullР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В¬Р В Р’В Р В РІР‚в„–Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р В Р РЏР В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В РЎС›Р Р†Р вЂљР’ВР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р В Р РЏР В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ
                    scope.launch {
                        settingsRepository.update { it.copy(captureRegion = null) }
                    }
                    mainScope.launch { floatingButton?.show() }
                }
            )
        }
    }

    /** Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В РІР‚в„ўР вЂ™Р’В­Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†РІР‚С›РЎС›Р В Р вЂ Р В РІР‚С™Р РЋРІР‚С”Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р вЂ Р Р†Р вЂљРЎвЂєР РЋРЎвЂєР В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р’В Р В РІР‚в„–Р В Р’В Р вЂ™Р’В¶Р В Р’В Р РЋРІР‚СљР В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р Р‹Р Р†РІР‚С›РЎС›Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В РЎС›Р Р†Р вЂљР’В stack trace Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В РІР‚в„ўР вЂ™Р’В¬Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р вЂ Р Р†Р вЂљРЎвЂєР РЋРЎвЂєР В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’В¶Р В Р Р‹Р РЋРЎв„ўР В Р’В Р В РІР‚в„–Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р В РІР‚В°Р В Р’В Р СћРІР‚ВР В Р’В Р Р†Р вЂљР’В¦Р В Р вЂ Р В РІР‚С™Р РЋРЎв„ўР В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В° ~140 Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В­Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В Р’В Р СћРІР‚ВР В РІР‚в„ўР вЂ™Р’В»Р В РЎС›Р РЋРІР‚в„ўР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р’В Р вЂ™Р’ВµР В Р’В Р В Р РЏР В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљР Р‹Р В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р вЂ™Р’ВµР В Р’В Р В Р вЂ°Р В Р Р‹Р Р†Р вЂљРІР‚СљР В Р’В Р вЂ™Р’ВµР В Р’В Р В Р РЏР В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ */
    private fun shortError(t: Throwable): String {
        val raw = t.message?.takeIf { it.isNotBlank() } ?: t.javaClass.simpleName
        return if (raw.length > 140) raw.take(140) + "Р В Р’В Р В РІР‚В Р В Р’В Р Р†Р вЂљРЎв„ўР В РІР‚в„ўР вЂ™Р’В¦" else raw
    }

    private fun elapsedSince(startMs: Long): Long =
        (System.currentTimeMillis() - startMs).coerceAtLeast(0L)

    private suspend fun captureOnce(
        showLoadingAfterScreenshot: Boolean = false,
        restoreFloatingButtonAfterScreenshot: Boolean = false
    ) {
        if (!captureLock.tryLock()) {
            restoreCaptureChrome(
                showLoading = false,
                restoreFloatingButton = restoreFloatingButtonAfterScreenshot
            )
            mainScope.launch { overlay?.dismissLoading() }
            return
        }
        val diagId = captureSequence.incrementAndGet()
        var captureAttemptStarted = false
        var captureChromeRestored = false
        var floatingButtonHiddenForCapture = false
        var floatingWindowHiddenForCapture = false
        var floatingWindowBoundsForCapture: OverlayCaptureRect? = null
        fun restoreCaptureChromeOnce(showLoading: Boolean) {
            if (captureChromeRestored) return
            captureChromeRestored = true
            restoreCaptureChrome(
                showLoading = showLoading,
                restoreFloatingButton = restoreFloatingButtonAfterScreenshot
            )
        }
        suspend fun restoreFloatingWindowAfterCapture() {
            if (!floatingWindowHiddenForCapture) return
            floatingWindowHiddenForCapture = false
            withContext(Dispatchers.Main) {
                overlay?.setFloatingWindowHiddenForCapture(hidden = false)
            }
        }
        suspend fun restoreFloatingButtonAfterCapture() {
            if (!floatingButtonHiddenForCapture) return
            floatingButtonHiddenForCapture = false
            withContext(Dispatchers.Main) {
                floatingButton?.setHiddenForCapture(hidden = false)
            }
        }
        try {
            if (loopMode) {
                val loopSettings = settingsRepository.get()
                val hasBlockingResult = overlay?.hasBlockingLoopResult() == true
                val activeResultDecision = LoopRuntimePolicy.activeResultDecision(
                    hasBlockingResult = hasBlockingResult,
                    translationInFlight = loopTranslationInFlight,
                )
                when (activeResultDecision) {
                    LoopActiveResultDecision.CAPTURE -> lastLoopRuntimeLogState = null
                    LoopActiveResultDecision.KEEP_TRANSLATING -> {
                        logLoopRuntimeTransition(
                            diagId,
                            state = "translation_in_flight",
                            message = "loop wait reason=translation_in_flight blockingResult=$hasBlockingResult",
                        )
                        return
                    }
                    LoopActiveResultDecision.KEEP_VISIBLE -> {
                        logLoopRuntimeTransition(
                            diagId,
                            state = "result_visible",
                            message = "loop wait reason=result_visible manualDismissRequired=true " +
                                "mode=${loopSettings.loopTriggerMode.name}",
                        )
                        return
                    }
                }
            }
            captureAttemptStarted = true
            beginTranslationBatch(diagId)
            logVerticalDiag(diagId, "start loopMode=$loopMode")
            val shotter = screenshotter ?: run {
                restoreCaptureChromeOnce(showLoading = false)
                return
            }
            if (loopMode) {
                val loopSettings = settingsRepository.get()
                var captureChromeChanged = false
                val floatingButtonShown = withContext(Dispatchers.Main) {
                    floatingButton?.isShown() == true
                }
                if (
                    shouldHideFloatingButtonForCapture(
                        loopMode = true,
                        isFloatingButtonShown = floatingButtonShown,
                    )
                ) {
                    floatingButtonHiddenForCapture = withContext(Dispatchers.Main) {
                        floatingButton?.setHiddenForCapture(hidden = true) == true
                    }
                    captureChromeChanged = floatingButtonHiddenForCapture
                }
                val floatingWindowState = withContext(Dispatchers.Main) {
                    val manager = overlay
                    val shown = manager?.isFloatingWindowShown() == true
                    shown to manager?.currentFloatingWindowBounds()
                }
                when (
                    floatingWindowCaptureAction(
                        loopMode = true,
                        renderMode = loopSettings.renderMode,
                        isFloatingWindowShown = floatingWindowState.first,
                    )
                ) {
                    FloatingWindowCaptureAction.NONE -> Unit
                    FloatingWindowCaptureAction.HIDE_TEMPORARILY -> {
                        floatingWindowHiddenForCapture = withContext(Dispatchers.Main) {
                            overlay?.setFloatingWindowHiddenForCapture(hidden = true) == true
                        }
                        captureChromeChanged =
                            captureChromeChanged || floatingWindowHiddenForCapture
                    }
                    FloatingWindowCaptureAction.PRESERVE_AND_MASK -> {
                        floatingWindowBoundsForCapture = floatingWindowState.second?.let { bounds ->
                            OverlayCaptureRect(
                                left = bounds.left,
                                top = bounds.top,
                                right = bounds.right,
                                bottom = bounds.bottom,
                            )
                        }
                    }
                }
                if (captureChromeChanged) {
                    delay(CAPTURE_CHROME_SETTLE_MS)
                }
            }
            var full = shotter.capture()
            restoreFloatingButtonAfterCapture()
            restoreFloatingWindowAfterCapture()
            if (full == null) {
                // Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р В Р РЏР В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р РЋРЎв„ўР В Р Р‹Р Р†Р вЂљРЎС›Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В·Р В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В Р Р‹Р Р†Р вЂљРЎвЂќ nullР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬MediaProjection token Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В¤Р В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬ / Shizuku Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В¤Р В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р РЋРІР‚ВР В РЎС›Р Р†Р вЂљР’ВР В РЎС›Р РЋРІР‚в„ўР В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В­Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°
                // Р В Р’В Р СћРІР‚ВР В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В РЎС›Р Р†Р вЂљР’ВР В Р’В Р вЂ™Р’В¶Р В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В РЎС›Р РЋРІР‚в„ў returnР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В·Р В Р’В Р вЂ™Р’ВµР В Р’В Р В Р РЏР В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р вЂ™Р’В·Р В Р Р‹Р РЋРІвЂћСћР В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљР’В¦Р В РІР‚в„ўР вЂ™Р’В¬Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В Р’В Р вЂ™Р’В·Р В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВР В Р Р‹Р Р†Р вЂљРЎС›Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р вЂ™Р’ВР В Р’В Р В Р РЏР В Р’В Р вЂ™Р’В¶Р В Р’В Р В Р РЏР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В¤Р В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ
                val mediaProjectionScreenshotter = shotter as? MediaProjectionScreenshotter
                val mediaProjectionFailureReason = mediaProjectionScreenshotter?.lastCaptureFailureReason()
                if (mediaProjectionFailureReason != null) {
                    Timber.w(
                        "CAPTURE FAILED backend=MediaProjection reason=$mediaProjectionFailureReason " +
                            "ready=${mediaProjectionScreenshotter.isReady}"
                    )
                } else {
                    Timber.w("Screenshot capture returned null")
                }
                restoreCaptureChromeOnce(showLoading = false)
                val msg = mediaProjectionFailureReason?.let { "Capture failed: $it" }
                    ?: getString(R.string.toast_capture_failed)
                logRepository.error(LogRepository.Category.CAPTURE, msg)
                mainScope.launch { overlay?.showErrorHint(msg) }
                return
            }
            // Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р Р‹Р Р†Р вЂљРІР‚Сњ settings Р В Р’В Р СћРІР‚ВР В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬ rescale regionР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎС›Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†РІР‚С›РЎС›Р В Р вЂ Р В РІР‚С™Р РЋРІР‚С”Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВР В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В РІР‚в„ўР вЂ™Р’В§Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р В Р РЏР В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р вЂ Р В РІР‚С™Р вЂ™Р’ВР В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†РІР‚С›РЎС›Р В Р вЂ Р В РІР‚С™Р РЋРІР‚С”Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРЎв„ўР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В Р В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ
            restoreCaptureChromeOnce(showLoading = showLoadingAfterScreenshot)
            val screenNow = physicalDisplaySize(this@CaptureService)
            val captureMask = mapOverlayBoundsToCapture(
                bounds = floatingWindowBoundsForCapture,
                overlayWidth = screenNow.width,
                overlayHeight = screenNow.height,
                captureWidth = full.width,
                captureHeight = full.height,
            )
            if (captureMask != null) {
                full = maskFloatingWindowFromCapture(full, captureMask)
                logVerticalDiag(
                    diagId,
                    "floating window preserved during capture and masked bounds=$captureMask"
                )
            }
            settingsRepository.rescaleCaptureRegionIfNeeded(screenNow.width, screenNow.height)
            val settings = settingsRepository.get()
            val fullStats = sampleBitmapFrameStats(full)
            logVerticalDiag(
                diagId,
                "screenshot full=${full.width}x${full.height} stats=${fullStats.toDiagString()}"
            )
            logCaptureGeometry(diagId, "fullScreen", full)
            logBlankLikeFrame(diagId, "screenshot", fullStats)
            applyOverlayConfig(settings, syncFloatingWindowLock = false)
            logVerticalSettings(diagId, settings, screenNow.width, screenNow.height)

            val region = settings.captureRegion
            val workBitmap = cropIfNeeded(full, region) ?: run {
                logVerticalDiag(diagId, "crop skipped: invalid bitmap from region=${region.toDiagString()}")
                full.recycle()
                return
            }
            logVerticalDiag(
                diagId,
                "workBitmap=${workBitmap.width}x${workBitmap.height} region=${region.toDiagString()}"
            )
            val workStats = if (workBitmap === full) {
                fullStats
            } else {
                sampleBitmapFrameStats(workBitmap)
            }
            logVerticalDiag(
                diagId,
                "workBitmap stats=${workStats.toDiagString()}"
            )
            logBlankLikeFrame(diagId, "workBitmap", workStats)
            if (workBitmap !== full) full.recycle()

            val redBoxActive = DeveloperOcrDebugPolicy.redBoxActive(
                settings.developerOptionsEnabled,
                settings.ocrRedBoxModeEnabled,
            )
            val debugShouldTranslate = DeveloperOcrDebugPolicy.shouldTranslate(
                settings.developerOptionsEnabled,
                settings.ocrRedBoxModeEnabled,
                settings.ocrRedBoxShowTranslation,
            )
            val routingT = translator as? com.gameocr.app.translate.RoutingTranslator
            val configuredEndToEnd = routingT?.isEndToEndFor(settings) ?: translator.isEndToEnd
            // Source-only red-box mode needs the regular OCR pipeline so no translator/API is called.
            val isEndToEnd = configuredEndToEnd && debugShouldTranslate

            val smartLoopEnabled = loopMode &&
                settings.loopTriggerMode == LoopTriggerMode.WAIT_FOR_TEXT_COMPLETE
            var forceTextStabilityFallback = loopRoiTextFallbackActive

            val currentLoopFingerprint = createLoopFrameFingerprint(workBitmap, settings)
            val stabilityResult = if (forceTextStabilityFallback) {
                null
            } else currentLoopFingerprint?.let { current ->
                LoopFrameStabilityPolicy.beforeOcr(
                    state = loopFrameStabilityState,
                    current = current,
                    enabled = smartLoopEnabled,
                    allowTextStabilityProbe = !configuredEndToEnd,
                    skipAlreadyProcessed = settings.loopSkipSimilarFrames,
                    stableDurationMs = settings.loopTextStableDurationMs,
                    nowElapsedMs = SystemClock.elapsedRealtime(),
                )
            }
            stabilityResult?.let { loopFrameStabilityState = it.state }
            val stabilityTrigger = when {
                forceTextStabilityFallback -> LoopFrameStabilityDecision.PROBE_TEXT_STABILITY
                stabilityResult != null -> stabilityResult.decision
                else -> LoopFrameStabilityDecision.PROCESS
            }
            if (stabilityTrigger == LoopFrameStabilityDecision.WAIT_FOR_STABLE_FRAME ||
                stabilityTrigger == LoopFrameStabilityDecision.SKIP_ALREADY_PROCESSED
            ) {
                logVerticalDiag(
                    diagId,
                    "skip loop frame stability=${stabilityTrigger.name} " +
                        "waitMs=${settings.loopTextStableDurationMs}"
                )
                workBitmap.recycle()
                return
            }

            val loopPreOcrResult = currentLoopFingerprint?.let { current ->
                LoopFrameChangePolicy.beforeOcr(
                    previous = previousLoopFingerprint,
                    current = current,
                    enabled = settings.loopSkipSimilarFrames,
                    similarityThreshold = settings.loopFrameSimilarityThreshold,
                )
            } ?: LoopFramePreOcrResult(LoopFramePreOcrDecision.PROCESS)
            if (loopPreOcrResult.decision == LoopFramePreOcrDecision.SKIP_EXACT_FRAME) {
                logVerticalDiag(diagId, "skip loop frame reason=exact_hash similarity=1.000")
                Timber.i("Skip loop frame: exact hash match")
                workBitmap.recycle()
                return
            }
            loopPreOcrResult.similarity?.let { similarity ->
                logVerticalDiag(
                    diagId,
                    "loop frame comparison decision=${loopPreOcrResult.decision.name} " +
                        "similarity=${String.format(Locale.US, "%.3f", similarity)} " +
                        "threshold=${String.format(Locale.US, "%.2f", settings.loopFrameSimilarityThreshold)}"
                )
            }

            // Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В«Р В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В«Р В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРЎв„ўР В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р вЂ™Р’В¶Р В Р Р‹Р РЋРІвЂћСћР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р РЋРІР‚СљР В Р вЂ Р В РІР‚С™Р РЋРЎв„ўР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В Р Р‹Р Р†Р вЂљРЎС›Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†Р вЂљРІР‚СњР В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљР Р‹Р В Р вЂ Р В РІР‚С™Р вЂ™Р’ВР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р Р‹Р Р†РІР‚С›РЎС›Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В·Р В Р Р‹Р Р†Р вЂљРІР‚СљР В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р вЂ Р В РІР‚С™Р В Р вЂ№ OCR Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В РІР‚в„ўР вЂ™Р’ВР В РІР‚в„ўР вЂ™Р’В¶Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В®Р В РІР‚в„ўР вЂ™Р’ВµР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В РЎС›Р Р†Р вЂљР’ВР В Р’В Р вЂ™Р’В¶Р В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В РЎС›Р РЋРІР‚в„ўР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљР’ВР В РІР‚в„ўР вЂ™Р’В¦Р В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљР Р‹Р В Р вЂ Р В РІР‚С™Р вЂ™Р’ВР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†РІР‚С›РЎС›Р В Р вЂ Р В РІР‚С™Р РЋРІР‚С” boxР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р В Р вЂ°Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В° mergeAdjacentBlocks
            // Р В Р’В Р СћРІР‚ВР В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р Р‹Р РЋРЎСџР В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р В Р вЂ°Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В»Р В РІР‚в„ўР вЂ™Р’В­ translateOneР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В РІР‚в„ўР вЂ™Р’В Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљР Р‹Р В Р вЂ Р В РІР‚С™Р вЂ™Р’ВР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В·Р В Р’В Р Р†Р вЂљР’В Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р В Р РЏР В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В Р’В Р В РЎвЂњ region Р В Р’В Р вЂ™Р’В·Р В Р’В Р Р†Р вЂљР’В Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РЎС›Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎСљР В РІР‚в„ўР вЂ™Р’В¦Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р Р†Р вЂљР’В°Р В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљР Р‹Р В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р Р†Р вЂљР’В¦Р В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р вЂ™Р’ВµР В РЎС›Р РЋРІР‚в„ўР В Р’В Р Р†Р вЂљР’В¦Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљРЎСљР В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ
            logVerticalDiag(
                diagId,
                "translator=${settings.translatorEngine.name} isEndToEnd=$isEndToEnd " +
                    "configuredEndToEnd=$configuredEndToEnd redBox=$redBoxActive " +
                    "debugTranslate=$debugShouldTranslate stability=${stabilityTrigger.name} " +
                    "renderMode=${settings.renderMode.name}"
            )
            val ocrStartedAt = System.currentTimeMillis()
            if (isEndToEnd) {
                val translatedBlocks = try {
                    translator.ocrAndTranslate(workBitmap, settings)
                } catch (ce: kotlinx.coroutines.CancellationException) {
                    workBitmap.recycle()
                    throw ce
                } catch (t: Throwable) {
                    Timber.w(t, "End-to-end OCR+translate failed")
                    logRepository.error(
                        LogRepository.Category.OCR,
                        getString(R.string.log_msg_ocr_failed_format, settings.translatorEngine.name),
                        t,
                        elapsedMs = elapsedSince(ocrStartedAt)
                    )
                    val msg = getString(R.string.toast_ocr_failed_format, settings.translatorEngine.name, shortError(t))
                    mainScope.launch { overlay?.showErrorHint(msg) }
                    workBitmap.recycle()
                    return
                }
                val adaptiveStyles = analyzeAdaptiveOverlayStyles(
                    workBitmap,
                    translatedBlocks.map { it.first },
                    settings,
                    diagId,
                )
                workBitmap.recycle()
                val normalizedEndToEndText = LoopFrameChangePolicy.normalizeOcrText(
                    translatedBlocks.map { (block, _) -> block.text }
                )
                val endToEndStability = LoopFrameStabilityPolicy.afterOcr(
                    state = loopFrameStabilityState,
                    current = currentLoopFingerprint,
                    trigger = stabilityTrigger,
                    normalizedOcrText = normalizedEndToEndText,
                    enabled = smartLoopEnabled,
                    skipAlreadyProcessed = settings.loopSkipSimilarFrames,
                    stableDurationMs = settings.loopTextStableDurationMs,
                    nowElapsedMs = SystemClock.elapsedRealtime(),
                )
                loopFrameStabilityState = endToEndStability.state
                commitLoopFrame(
                    currentLoopFingerprint,
                    normalizedEndToEndText,
                )
                logVerticalTranslatedBlocks(diagId, "endToEnd", translatedBlocks)
                if (translatedBlocks.isNotEmpty()) {
                    val joined = translatedBlocks.mapIndexed { i, (b, dst) ->
                        "#${i + 1} ${b.text} Р В Р’В Р В РІР‚В Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РЎС› $dst"
                    }.joinToString(" | ")
                    logRepository.info(
                        LogRepository.Category.OCR,
                        "[${settings.translatorEngine.name}] ${translatedBlocks.size} Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В®Р В РІР‚в„ўР вЂ™Р’Вµ: $joined",
                        elapsedMs = elapsedSince(ocrStartedAt)
                    )
                } else {
                    logRepository.info(
                        LogRepository.Category.OCR,
                        "[${settings.translatorEngine.name}] Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В РІР‚в„ўР вЂ™Р’В Р В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљР Р‹Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В«Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В»Р В Р вЂ Р В РІР‚С™Р РЋРЎв„ўР В Р’В Р вЂ™Р’В¶Р В Р Р‹Р Р†Р вЂљРЎвЂќР В Р Р‹Р РЋРІвЂћСћ",
                        elapsedMs = elapsedSince(ocrStartedAt)
                    )
                    return
                }
                renderTranslatedBlocks(
                    translatedBlocks,
                    settings,
                    diagId,
                    translationElapsedMs = elapsedSince(ocrStartedAt),
                    adaptiveStyles = adaptiveStyles,
                )
                return
            }

        } finally {
            restoreFloatingButtonAfterCapture()
            restoreFloatingWindowAfterCapture()
            if (captureAttemptStarted) {
                restoreCaptureChromeOnce(showLoading = false)
                logVerticalDiag(diagId, "finish")
                mainScope.launch { overlay?.dismissLoading() }
            }
            captureLock.unlock()
        }
    }

    private fun cropIfNeeded(src: Bitmap, region: CaptureRegion?): Bitmap? {
        if (region == null || !region.isValid()) return src
        val l = region.left.coerceIn(0, src.width)
        val t = region.top.coerceIn(0, src.height)
        val r = region.right.coerceIn(0, src.width)
        val b = region.bottom.coerceIn(0, src.height)
        if (r - l <= 8 || b - t <= 8) return src
        return Bitmap.createBitmap(src, l, t, r - l, b - t)
    }

    private fun maskFloatingWindowFromCapture(
        source: Bitmap,
        bounds: OverlayCaptureRect,
    ): Bitmap {
        val target = if (source.isMutable) {
            source
        } else {
            source.copy(Bitmap.Config.ARGB_8888, true) ?: return source
        }
        Canvas(target).drawRect(
            bounds.left.toFloat(),
            bounds.top.toFloat(),
            bounds.right.toFloat(),
            bounds.bottom.toFloat(),
            Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
                isAntiAlias = false
            },
        )
        if (target !== source) source.recycle()
        return target
    }

    private fun analyzeAdaptiveOverlayStyles(
        bitmap: Bitmap,
        blocks: List<TextBlock>,
        settings: Settings,
        diagId: Long?,
    ): List<AdaptiveOverlayStyle> {
        if (!adaptiveOverlayActive(settings.overlayStyleMode, settings.renderMode) ||
            blocks.isEmpty()
        ) return emptyList()

        val startedAt = SystemClock.elapsedRealtimeNanos()
        val styles = runCatching {
            AdaptiveOverlayStyleAnalyzer.analyze(
                bitmap = bitmap,
                blocks = blocks,
                scaledDensity = resources.displayMetrics.density * resources.configuration.fontScale,
            )
        }.getOrElse { error ->
            Timber.tag("AdaptiveStyle").w(error, "Adaptive style analysis failed; use safe overlay fallback")
            diagId?.let {
                logVerticalDiag(error, it, "adaptive style analysis failed; use safe overlay fallback")
            }
            emptyList()
        }
        val elapsedMs = (SystemClock.elapsedRealtimeNanos() - startedAt) / 1_000_000.0
        val fallbackCount = styles.count { it.usedFallback }
        val summary = "adaptive style analyzed blocks=${styles.size} fallbacks=$fallbackCount " +
            "elapsedMs=${String.format(Locale.US, "%.2f", elapsedMs)}"
        if (diagId != null) logVerticalDiag(diagId, summary) else Timber.tag("AdaptiveStyle").i(summary)
        return styles
    }

    /**
     * Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В«Р В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В«Р В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРЎв„ўР В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р вЂ™Р’В¶Р В Р Р‹Р РЋРІвЂћСћР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р РЋРІР‚СљР В Р вЂ Р В РІР‚С™Р РЋРЎв„ўР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В Р Р‹Р Р†Р вЂљРЎС›Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†Р вЂљРІР‚СњР В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљР Р‹Р В Р вЂ Р В РІР‚С™Р вЂ™Р’ВР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р вЂ Р В РІР‚С™Р РЋРЎв„ўР В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’В¶Р В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р Р†Р вЂљР’В Р В Р’В Р вЂ™Р’В¶Р В Р Р‹Р РЋРЎСџР В Р вЂ Р В РІР‚С™Р РЋРЎв„ўР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р Р‹Р Р†РІР‚С›РЎС›bitmap Р В Р’В Р В РІР‚В Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РЎС› Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В·Р В Р’В Р Р†Р вЂљР’В Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р В Р РЏР В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљР’ВР В РІР‚в„ўР вЂ™Р’В¦Р В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљР Р‹Р В Р вЂ Р В РІР‚С™Р вЂ™Р’ВР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†РІР‚С›РЎС›Р В Р вЂ Р В РІР‚С™Р РЋРІР‚С” box Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В Р’В Р РЋРІР‚ВР В Р’В Р В РІР‚в„–Р В Р’В Р В РЎвЂњР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В РІР‚в„ўР вЂ™Р’В Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р Р‹Р РЋРІвЂћСћР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р’В Р В Р вЂ°Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†Р вЂљРІР‚СњР В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљР Р‹Р В Р вЂ Р В РІР‚С™Р вЂ™Р’ВР В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ
     * Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В РЎС›Р Р†Р вЂљР’ВР В Р’В Р вЂ™Р’В¶Р В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В РЎС›Р РЋРІР‚в„ўР В Р’В Р вЂ™Р’В¶Р В Р’В Р В РІР‚В°Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В° renderMode Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В¬Р В Р’В Р В РІР‚в„–Р В Р’В Р вЂ™Р’В¶Р В Р’В Р Р†Р вЂљРЎв„ўР В РІР‚в„ўР вЂ™Р’В§Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В° overlayР В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В Р’В Р вЂ™Р’В¶Р В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р В Р РЏР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В®Р В РІР‚в„ўР вЂ™Р’ВµР В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В Р Р‹Р РЋРЎСџР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В Р вЂ Р В РІР‚С™Р В Р вЂ№/Р В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљР Р‹Р В Р вЂ Р В РІР‚С™Р вЂ™Р’ВР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р вЂ Р Р†Р вЂљРЎвЂєР РЋРЎвЂєР В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В¶Р В Р Р‹Р РЋРЎв„ўР В Р’В Р В РІР‚в„– LogRepository pairР В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ
     */
    private suspend fun renderTranslatedBlocks(
        items: List<Pair<TextBlock, String>>,
        settings: Settings,
        diagId: Long? = null,
        translationElapsedMs: Long? = null,
        adaptiveStyles: List<AdaptiveOverlayStyle> = emptyList(),
    ) {
        val recognizedOrientation = resolveTextBlockReadingOrientation(
            items.map { it.first },
        )
        val translationOutput = resolveTranslationOutputSettings(
            settings.translationOutputFollowRecognition,
            settings.translationOutputLayout,
            settings.translationOutputDirection,
        )
        val outputOrientation = TranslationOutputOrientationPolicy.resolve(
            recognized = recognizedOrientation,
            followRecognition = translationOutput.followRecognition,
            layout = translationOutput.layout,
            direction = translationOutput.direction,
        )
        diagId?.let { logVerticalTranslatedBlocks(it, "renderTranslatedBlocks", items) }
        items.forEach { (b, dst) ->
            logRepository.pair(
                LogRepository.Category.TRANSLATE,
                b.text,
                dst,
                elapsedMs = translationElapsedMs
            )
        }
        when {
            DeveloperOcrDebugPolicy.redBoxActive(
                settings.developerOptionsEnabled,
                settings.ocrRedBoxModeEnabled,
            ) -> withContext(Dispatchers.Main) {
                overlay?.showBlocks(
                    items,
                    outputOrientation,
                    diagnosticId = diagId,
                    followBlockOrientations = translationOutput.followRecognition,
                )
            }
            settings.renderMode == RenderMode.BLOCKS -> withContext(Dispatchers.Main) {
                overlay?.showBlocks(
                    items,
                    outputOrientation,
                    diagnosticId = diagId,
                    adaptiveStyles = adaptiveStyles,
                    followBlockOrientations = translationOutput.followRecognition,
                )
            }
            else -> withContext(Dispatchers.Main) {
                overlay?.showFullScreen(items.map { (b, dst) -> b.text to dst }, historyItems = items)
            }
        }
        markLoopResultVisible(diagId)
    }

    private suspend fun renderBlocks(
        blocks: List<TextBlock>,
        settings: Settings,
        orientation: TextOrientation = TextOrientation.HORIZONTAL_LTR,
        diagId: Long? = null,
        adaptiveStyles: List<AdaptiveOverlayStyle> = emptyList(),
        delayedMaskDebugBatch: MangaDelayedMaskDebugSessionManager.Batch? = null,
    ) {
        val translationOutput = resolveTranslationOutputSettings(
            settings.translationOutputFollowRecognition,
            settings.translationOutputLayout,
            settings.translationOutputDirection,
        )
        val followBlockOrientations = translationOutput.followRecognition
        val delayedLayoutOrientation = TranslationOutputOrientationPolicy.resolve(
            recognized = orientation,
            followRecognition = translationOutput.followRecognition,
            layout = translationOutput.layout,
            direction = translationOutput.direction,
        )
        diagId?.let {
            logVerticalDiag(
                it,
                "renderBlocks show placeholders orientation=$orientation count=${blocks.size} " +
                    "followBlockOrientations=$followBlockOrientations"
            )
        }
        // Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р вЂ™Р’В¶Р В Р’В Р Р†Р вЂљР’В°Р В Р’В Р Р†Р вЂљР’В°Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В¶Р В Р Р‹Р РЋРІвЂћСћР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В Р Р‹Р РЋРЎСџР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРЎв„ўР В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В Р’В Р СћРІР‚ВР В РІР‚в„ўР вЂ™Р’В»Р В РЎС›Р РЋРІР‚в„ўР В Р’В Р вЂ™Р’ВµР В Р’В Р В Р вЂ°Р В РІР‚в„ўР вЂ™Р’В Р В Р’В Р СћРІР‚ВР В Р’В Р Р†Р вЂљР’В¦Р В Р’В Р В Р вЂ°"Р В Р’В Р В РІР‚В Р В Р’В Р Р†Р вЂљРЎв„ўР В РІР‚в„ўР вЂ™Р’В¦"Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВР В Р Р‹Р Р†Р вЂљРЎС›Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В¤Р В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В Р Р‹Р РЋРЎСџР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћorientation Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р Р‹Р Р†Р вЂљРІР‚СљР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В®Р В Р Р‹Р Р†РІР‚С›РЎС›Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р В РЎвЂњ TextView Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРІР‚СњР В РІР‚в„ўР вЂ™Р’ВР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВР В Р’В Р Р†Р вЂљР Р‹ VerticalTextView
        withContext(Dispatchers.Main) {
            overlay?.showBlocks(
                blocks.map { it to "Р В Р’В Р В РІР‚В Р В Р’В Р Р†Р вЂљРЎв„ўР В РІР‚в„ўР вЂ™Р’В¦" },
                orientation,
                diagnosticId = diagId,
                adaptiveStyles = adaptiveStyles,
            )
        }
        // Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРЎв„ўР В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’В¶Р В Р’В Р В РІР‚В°Р В Р’В Р РЋРІР‚СљР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В¤Р В Р вЂ Р В РІР‚С™Р РЋРІР‚С”Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В¦Р В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ DeepLР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р В РІР‚В Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РЎС› Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В¬Р В Р’В Р В РІР‚в„– HTTP Р В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљР Р‹Р В Р вЂ Р В РІР‚С™Р вЂ™Р’ВР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В¤Р В Р Р‹Р Р†РІР‚С›РЎС›Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В®Р В РІР‚в„ўР вЂ™Р’ВµР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р РЋРІР‚СљР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р’В Р В Р вЂ°Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р Р†Р вЂљРЎвЂєР РЋРЎвЂєР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р Р‹Р РЋРІР‚С”Р В Р вЂ Р В РІР‚С™Р вЂ™Р’ВР В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В РІР‚в„ўР вЂ™Р’В¦Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р вЂ Р Р†Р вЂљРЎвЂєР РЋРЎвЂєР В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р Р‹Р РЋРЎв„ўР В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В Р вЂ Р Р†Р вЂљРЎвЂєР РЋРЎвЂєР В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В®Р В РІР‚в„ўР вЂ™Р’ВµР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВµР В Р’В Р РЋРІР‚СљР В Р’В Р вЂ™Р’ВµР В Р Р‹Р вЂ™Р’ВР В Р’В Р В Р РЏ
        // Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р В РЎвЂњ translateOneР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬OpenAI Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р Р‹Р вЂ™Р’ВР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В®Р В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚Сљ LLM Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В·Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљРЎС›Р В Р Р‹Р РЋРЎв„ўР В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р Р‹Р Р†Р вЂљРІвЂћСћ token Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВµР В Р’В Р РЋРІР‚СљР В Р’В Р вЂ™Р’ВµР В Р Р‹Р вЂ™Р’ВР В Р’В Р В Р РЏР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В РЎС›Р Р†Р вЂљР’ВР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р СћРІР‚ВР В Р’В Р Р†Р вЂљР’В¦Р В Р вЂ Р В РІР‚С™Р РЋРЎв„ўР В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р В РІР‚В°Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ
        val routing = translator as? com.gameocr.app.translate.RoutingTranslator
        val useBatch = routing?.prefersBatchFor(settings) ?: translator.prefersBatch
        val useCrossLineContext = shouldUseCrossLineContextTranslation(
            enabled = crossLineContextTranslationEnabled(
                disableCrossLineContextTranslation = settings.disableCrossLineContextTranslation,
            ),
            mergeAdjacentBlocks = settings.mergeAdjacentBlocks,
        )
        val translationUnits = if (useCrossLineContext) {
            planCrossLineTranslationUnits(blocks, settings.sourceLang)
        } else {
            individualTranslationUnits(blocks)
        }
        diagId?.let {
            logVerticalDiag(
                it,
                "renderBlocks translate useBatch=$useBatch engine=${settings.translatorEngine.name} " +
                    "streaming=${settings.streamingTranslate} crossLine=$useCrossLineContext " +
                    "blocks=${blocks.size} units=${translationUnits.size}"
            )
            translationUnits.forEachIndexed { index, unit ->
                logVerticalDiag(
                    it,
                    "contextUnit#${index + 1} blocks=${unit.blockIndexes.map { blockIndex -> blockIndex + 1 }} " +
                        "src=${unit.sourceText.toDiagText()}"
                )
            }
        }
        val loopSession = beginLoopTranslation(diagId)
        val successfulMaskBlockIndices = java.util.concurrent.ConcurrentHashMap.newKeySet<Int>()
        val translatedMaskBlockTexts = java.util.concurrent.ConcurrentHashMap<Int, String>()
        fun recordTranslatedUnit(
            unit: CrossLineTranslationUnit,
            translatedText: String,
        ) {
            val chunks = reflowCrossLineTranslation(
                translatedText = translatedText,
                unit = unit,
                blocks = blocks,
                targetLanguageTag = settings.targetLang,
            )
            unit.blockIndexes.zip(chunks).forEach { (blockIndex, chunk) ->
                translatedMaskBlockTexts[blockIndex] = chunk
            }
        }
        if (useBatch) {
            launchTranslationBatch(diagId) {
                var completed = false
                try {
                    batchTranslateBlocks(
                        blocks = blocks,
                        translationUnits = translationUnits,
                        settings = settings,
                        diagId = diagId,
                        onSuccessfulUnit = { unit, translatedText ->
                            successfulMaskBlockIndices.addAll(unit.blockIndexes)
                            recordTranslatedUnit(unit, translatedText)
                        },
                    )
                    completed = true
                } finally {
                    finishLoopTranslation(diagId, loopSession)
                }
            }
        } else {
            launchTranslationBatch(diagId) {
                var completed = false
                try {
                    translationUnits.mapIndexed { idx, unit ->
                        async {
                            val succeeded = translateOne(
                                unit.sourceText,
                                settings,
                                diagId,
                                idx,
                            ) { partial, phase ->
                                if (phase == AdaptiveTextLayoutPhase.FINAL) {
                                    recordTranslatedUnit(unit, partial)
                                }
                                withContext(Dispatchers.Main) {
                                    updateTranslationUnit(
                                        blocks = blocks,
                                        unit = unit,
                                        translatedText = partial,
                                        settings = settings,
                                        phase = phase,
                                        translationBatchId = diagId,
                                    )
                                }
                            }
                            if (succeeded) {
                                successfulMaskBlockIndices.addAll(unit.blockIndexes)
                            }
                        }
                    }.awaitAll()
                    completed = true
                } finally {
                    finishLoopTranslation(diagId, loopSession)
                }
            }
        }
    }

    private suspend fun batchTranslateBlocks(
        blocks: List<TextBlock>,
        translationUnits: List<CrossLineTranslationUnit>,
        settings: Settings,
        diagId: Long? = null,
        onSuccessfulUnit: (CrossLineTranslationUnit, String) -> Unit = { _, _ -> },
    ) = coroutineScope {
        val sources = translationUnits.map { it.sourceText }
        diagId?.let {
            logVerticalDiag(
                it,
                "batchTranslate begin engine=${settings.translatorEngine.name} count=${sources.size} " +
                    "${settings.sourceLang}->${settings.targetLang}"
            )
            sources.forEachIndexed { idx, source ->
                logVerticalDiag(it, "batchTranslate src#${idx + 1} ${source.toDiagText()}")
            }
        }
        val translateStartedAt = System.currentTimeMillis()
        val updates = Channel<BatchTranslationUpdate>(capacity = Channel.UNLIMITED)
        val progress = BatchTranslationProgressState(translationUnits.size)
        val consumer = launch {
            for (update in updates) {
                if (!progress.accept(update.index)) {
                    diagId?.let {
                        logVerticalDiag(
                            it,
                            "batchTranslate incremental ignored index=${update.index} " +
                                "duplicate=${progress.isEmitted(update.index)}"
                        )
                    }
                    continue
                }
                publishBatchTranslation(
                    index = update.index,
                    blocks = blocks,
                    unit = translationUnits[update.index],
                    initialOutput = update.text,
                    settings = settings,
                    diagId = diagId,
                    elapsedMs = TranslationLogElapsedPolicy.resolve(
                        developerOptionsEnabled = settings.developerOptionsEnabled,
                        batchCumulativeCompletionTimeEnabled =
                            settings.batchCumulativeCompletionTimeEnabled,
                        itemElapsedMs = update.elapsedMs,
                        batchElapsedMs = elapsedSince(translateStartedAt),
                    ),
                    phase = "incremental",
                    onSuccessful = { finalText ->
                        onSuccessfulUnit(translationUnits[update.index], finalText)
                    },
                )
            }
        }
        val translated = try {
            withContext(Dispatchers.IO) {
                translator.translateBatchIncremental(sources, settings) { update ->
                    if (updates.trySend(update).isFailure) {
                        diagId?.let {
                            logVerticalDiag(
                                it,
                                "batchTranslate incremental enqueue failed index=${update.index}"
                            )
                        }
                    }
                }
            }
        } catch (ce: CancellationException) {
            consumer.cancel()
            throw ce
        } catch (t: Throwable) {
            ensureCurrentTranslationBatch(diagId)
            updates.close()
            consumer.join()
            val translateElapsedMs = elapsedSince(translateStartedAt)
            Timber.w(t, "Batch translate failed")
            diagId?.let { logVerticalDiag(t, it, "batchTranslate failed engine=${settings.translatorEngine.name}") }
            logRepository.error(
                LogRepository.Category.TRANSLATE,
                getString(R.string.log_msg_batch_translate_failed_format, settings.translatorEngine.name),
                t,
                elapsedMs = translateElapsedMs
            )
            // Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В РЎС›Р Р†Р вЂљР’ВР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В¤Р В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р РЋРІР‚ВР В РЎС›Р Р†Р вЂљР’ВР В РЎС›Р РЋРІР‚в„ўР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р Р‹Р Р†РІР‚С›РЎС›Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В¶Р В Р Р‹Р РЋРІвЂћСћР В Р вЂ Р В РІР‚С™Р вЂ™Р’В° box Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р Р†Р вЂљР’В°Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВР В Р Р‹Р Р†Р вЂљРЎС›Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В¤Р В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В¤Р В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р РЋРІР‚ВР В РЎС›Р Р†Р вЂљР’ВР В РЎС›Р РЋРІР‚в„ўР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В Р В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В®Р В РІР‚в„ўР вЂ™Р’В°
            withContext(Dispatchers.Main) {
                progress.pendingIndexes().forEach { idx ->
                    updateTranslationUnit(
                        blocks = blocks,
                        unit = translationUnits[idx],
                        translatedText = "[!] " + (t.message ?: ""),
                        settings = settings,
                        phase = AdaptiveTextLayoutPhase.FINAL,
                        translationBatchId = diagId,
                    )
                }
            }
            return@coroutineScope
        } finally {
            updates.close()
        }
        consumer.join()
        val translateElapsedMs = elapsedSince(translateStartedAt)
        translationUnits.forEachIndexed { idx, unit ->
            if (progress.isEmitted(idx)) return@forEachIndexed
            publishBatchTranslation(
                index = idx,
                blocks = blocks,
                unit = unit,
                initialOutput = translated.getOrNull(idx),
                settings = settings,
                diagId = diagId,
                elapsedMs = TranslationLogElapsedPolicy.resolve(
                    developerOptionsEnabled = settings.developerOptionsEnabled,
                    batchCumulativeCompletionTimeEnabled =
                        settings.batchCumulativeCompletionTimeEnabled,
                    itemElapsedMs = null,
                    batchElapsedMs = translateElapsedMs,
                ),
                phase = "final",
                onSuccessful = { finalText -> onSuccessfulUnit(unit, finalText) },
            )
        }
    }

    private suspend fun publishBatchTranslation(
        index: Int,
        blocks: List<TextBlock>,
        unit: CrossLineTranslationUnit,
        initialOutput: String?,
        settings: Settings,
        diagId: Long?,
        elapsedMs: Long,
        phase: String,
        onSuccessful: (String) -> Unit = {},
    ) {
        ensureCurrentTranslationBatch(diagId)
        val src = unit.sourceText
        val display = resolveTranslationOutput(
            initialOutput = initialOutput,
            source = src,
            settings = settings,
            diagId = diagId,
            label = "batch#${index + 1}",
        )
        ensureCurrentTranslationBatch(diagId)
        val finalText = display.text
        diagId?.let {
            logVerticalDiag(
                it,
                "batchTranslate dst#${index + 1} phase=$phase " +
                    "failed=${display.failed} ${finalText.toDiagText()}"
            )
        }
        withContext(Dispatchers.Main) {
            updateTranslationUnit(
                blocks = blocks,
                unit = unit,
                translatedText = finalText,
                settings = settings,
                phase = AdaptiveTextLayoutPhase.FINAL,
                translationBatchId = diagId,
            )
        }
        ensureCurrentTranslationBatch(diagId)
        if (!display.failed) {
            onSuccessful(finalText)
            logRepository.pair(
                LogRepository.Category.TRANSLATE,
                src,
                finalText,
                elapsedMs = elapsedMs
            )
        } else {
            logRepository.warn(
                LogRepository.Category.TRANSLATE,
                getString(R.string.log_msg_translate_failed_format, settings.translatorEngine.name),
                elapsedMs = elapsedMs,
            )
        }
    }

    private fun updateTranslationUnit(
        blocks: List<TextBlock>,
        unit: CrossLineTranslationUnit,
        translatedText: String,
        settings: Settings,
        phase: AdaptiveTextLayoutPhase,
        translationBatchId: Long?,
    ) {
        if (!translationBatchGate.accepts(translationBatchId)) return
        val chunks = reflowCrossLineTranslation(
            translatedText = translatedText,
            unit = unit,
            blocks = blocks,
            targetLanguageTag = settings.targetLang,
        )
        unit.blockIndexes.zip(chunks).forEach { (blockIndex, chunk) ->
            overlay?.updateBlockText(blockIndex, chunk, phase)
        }
    }

    private suspend fun batchTranslateFloatingWindow(
        translationUnits: List<CrossLineTranslationUnit>,
        settings: Settings,
        diagId: Long? = null,
    ) = coroutineScope {
        val sources = translationUnits.map { it.sourceText }
        withContext(Dispatchers.Main) {
            if (translationBatchGate.accepts(diagId)) {
                overlay?.prepareFloatingWindow(sources)
            }
        }
        ensureCurrentTranslationBatch(diagId)
        diagId?.let {
            sources.forEachIndexed { idx, source ->
                logVerticalDiag(it, "floatingBatch src#${idx + 1} ${source.toDiagText()}")
            }
        }

        val translateStartedAt = System.currentTimeMillis()
        val updates = Channel<BatchTranslationUpdate>(capacity = Channel.UNLIMITED)
        val progress = BatchTranslationProgressState(translationUnits.size)
        val consumer = launch {
            for (update in updates) {
                if (!progress.accept(update.index)) {
                    diagId?.let {
                        logVerticalDiag(
                            it,
                            "floatingBatch incremental ignored index=${update.index} " +
                                "duplicate=${progress.isEmitted(update.index)}"
                        )
                    }
                    continue
                }
                publishFloatingBatchTranslation(
                    index = update.index,
                    unit = translationUnits[update.index],
                    initialOutput = update.text,
                    settings = settings,
                    diagId = diagId,
                    elapsedMs = TranslationLogElapsedPolicy.resolve(
                        developerOptionsEnabled = settings.developerOptionsEnabled,
                        batchCumulativeCompletionTimeEnabled =
                            settings.batchCumulativeCompletionTimeEnabled,
                        itemElapsedMs = update.elapsedMs,
                        batchElapsedMs = elapsedSince(translateStartedAt),
                    ),
                    phase = "incremental",
                )
            }
        }
        val translated = try {
            withContext(Dispatchers.IO) {
                translator.translateBatchIncremental(sources, settings) { update ->
                    if (updates.trySend(update).isFailure) {
                        diagId?.let {
                            logVerticalDiag(
                                it,
                                "floatingBatch incremental enqueue failed index=${update.index}"
                            )
                        }
                    }
                }
            }
        } catch (ce: CancellationException) {
            consumer.cancel()
            throw ce
        } catch (t: Throwable) {
            ensureCurrentTranslationBatch(diagId)
            updates.close()
            consumer.join()
            val translateElapsedMs = elapsedSince(translateStartedAt)
            diagId?.let {
                logVerticalDiag(
                    t,
                    it,
                    "floatingBatch failed engine=${settings.translatorEngine.name}"
                )
            }
            logRepository.error(
                LogRepository.Category.TRANSLATE,
                getString(R.string.log_msg_batch_translate_failed_format, settings.translatorEngine.name),
                t,
                elapsedMs = translateElapsedMs
            )
            withContext(Dispatchers.Main) {
                if (translationBatchGate.accepts(diagId)) {
                    progress.pendingIndexes().forEach { idx ->
                        overlay?.updateFloatingWindowText(idx, "[!] " + (t.message ?: ""))
                    }
                }
            }
            return@coroutineScope
        } finally {
            updates.close()
        }
        consumer.join()
        val translateElapsedMs = elapsedSince(translateStartedAt)
        translationUnits.forEachIndexed { idx, unit ->
            if (progress.isEmitted(idx)) return@forEachIndexed
            publishFloatingBatchTranslation(
                index = idx,
                unit = unit,
                initialOutput = translated.getOrNull(idx),
                settings = settings,
                diagId = diagId,
                elapsedMs = TranslationLogElapsedPolicy.resolve(
                    developerOptionsEnabled = settings.developerOptionsEnabled,
                    batchCumulativeCompletionTimeEnabled =
                        settings.batchCumulativeCompletionTimeEnabled,
                    itemElapsedMs = null,
                    batchElapsedMs = translateElapsedMs,
                ),
                phase = "final",
            )
        }
    }

    private suspend fun publishFloatingBatchTranslation(
        index: Int,
        unit: CrossLineTranslationUnit,
        initialOutput: String?,
        settings: Settings,
        diagId: Long?,
        elapsedMs: Long,
        phase: String,
    ) {
        ensureCurrentTranslationBatch(diagId)
        val display = resolveTranslationOutput(
            initialOutput = initialOutput,
            source = unit.sourceText,
            settings = settings,
            diagId = diagId,
            label = "floatingBatch#${index + 1}",
        )
        ensureCurrentTranslationBatch(diagId)
        val finalText = display.text
        diagId?.let {
            logVerticalDiag(
                it,
                "floatingBatch dst#${index + 1} phase=$phase " +
                    "failed=${display.failed} ${finalText.toDiagText()}"
            )
        }
        withContext(Dispatchers.Main) {
            if (translationBatchGate.accepts(diagId)) {
                overlay?.updateFloatingWindowText(index, finalText)
            }
        }
        ensureCurrentTranslationBatch(diagId)
        if (display.failed) {
            logRepository.warn(
                LogRepository.Category.TRANSLATE,
                getString(R.string.log_msg_translate_failed_format, settings.translatorEngine.name),
                elapsedMs = elapsedMs,
            )
        } else {
            logRepository.pair(
                LogRepository.Category.TRANSLATE,
                unit.sourceText,
                finalText,
                elapsedMs = elapsedMs,
            )
        }
    }

    /**
     * Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В РІР‚в„ўР вЂ™Р’В¬Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р вЂ™Р’В·Р В Р’В Р Р†Р вЂљРЎвЂєР В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В Р’В Р вЂ™Р’ВµР В Р’В Р В Р РЏР В Р’В Р Р†РІР‚С™Р’В¬Р В Р’В Р вЂ™Р’В¶Р В Р’В Р В РЎвЂњР В Р’В Р В РІР‚в„–Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р вЂ™Р’ВР В Р’В Р В Р РЏР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬[RenderMode.FLOATING_WINDOW]Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р Р‹Р Р†РІР‚С›РЎС›
     * - Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р’В Р вЂ™Р’ВµР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРЎв„ўР В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В¦Р В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ DeepL `prefersBatchFor=true`Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р Р‹Р Р†РІР‚С›РЎС›Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В­Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚Сљ HTTP Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В РЎС›Р Р†Р вЂљР’ВР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚Сљ `showFullScreen`
     * - Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВµР В Р’В Р РЋРІР‚СљР В Р’В Р вЂ™Р’ВµР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРЎв„ўР В Р’В Р Р†Р вЂљРІвЂћвЂ“ + `streamingTranslate=true`Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р Р‹Р Р†РІР‚С›РЎС›Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬ `prepareFloatingWindow` Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р РЋРЎв„ўР В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р вЂ™Р’ВµР В Р’В Р В Р вЂ°Р В РІР‚в„ўР вЂ™Р’В Р В Р’В Р СћРІР‚ВР В Р’В Р Р†Р вЂљР’В¦Р В Р’В Р В Р вЂ°"Р В Р’В Р В РІР‚В Р В Р’В Р Р†Р вЂљРЎв„ўР В РІР‚в„ўР вЂ™Р’В¦"Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°
     *   Р В Р’В Р вЂ™Р’В¶Р В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р В Р РЏР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В®Р В РІР‚в„ўР вЂ™Р’Вµ `translateOne` Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВµР В Р’В Р РЋРІР‚СљР В Р’В Р вЂ™Р’ВµР В Р Р‹Р вЂ™Р’ВР В Р’В Р В Р РЏР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В Р Р‹Р Р†Р вЂљРЎвЂќР В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В° `updateFloatingWindowText`Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р Р†Р вЂљРІвЂћвЂ“ BLOCKS Р В Р’В Р вЂ™Р’В¶Р В Р’В Р В РЎвЂњР В Р’В Р В РІР‚в„–Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р вЂ™Р’ВР В Р’В Р В Р РЏР В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В­Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р СћРІР‚ВР В Р’В Р Р†Р вЂљР’В¦Р В Р вЂ Р В РІР‚С™Р РЋРЎв„ўР В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р В РІР‚В°
     * - Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВµР В Р’В Р РЋРІР‚СљР В Р’В Р вЂ™Р’ВµР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРЎв„ўР В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р СћРІР‚ВР В Р’В Р Р†Р вЂљР’В¦Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р Р‹Р Р†Р вЂљРІР‚СљР В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљРЎСљР В Р вЂ Р В РІР‚С™Р вЂ™Р’В  streamingР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р Р‹Р Р†РІР‚С›РЎС›Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В®Р В РІР‚в„ўР вЂ™Р’ВµР В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В­Р В РЎС›Р РЋРІР‚в„ў `translate()`Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’В¶Р В Р Р‹Р РЋРІвЂћСћР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В РЎС›Р Р†Р вЂљР’ВР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВР В Р Р‹Р Р†Р вЂљРЎС›Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В¤Р В Р Р‹Р Р†Р вЂљРЎСљ
     */
    private suspend fun renderFloatingWindow(
        blocks: List<TextBlock>,
        settings: Settings,
        diagId: Long? = null
    ) {
        val routing = translator as? com.gameocr.app.translate.RoutingTranslator
        val useBatch = routing?.prefersBatchFor(settings) ?: translator.prefersBatch
        val useCrossLineContext = shouldUseCrossLineContextTranslation(
            enabled = crossLineContextTranslationEnabled(
                disableCrossLineContextTranslation = settings.disableCrossLineContextTranslation,
            ),
            mergeAdjacentBlocks = settings.mergeAdjacentBlocks,
        )
        val translationUnits = if (useCrossLineContext) {
            planCrossLineTranslationUnits(blocks, settings.sourceLang)
        } else {
            individualTranslationUnits(blocks)
        }
        diagId?.let {
            logVerticalDiag(
                it,
                "renderFloatingWindow useBatch=$useBatch engine=${settings.translatorEngine.name} " +
                    "streaming=${settings.streamingTranslate} crossLine=$useCrossLineContext " +
                    "blocks=${blocks.size} units=${translationUnits.size}"
            )
            translationUnits.forEachIndexed { index, unit ->
                logVerticalDiag(
                    it,
                    "floatingContextUnit#${index + 1} " +
                        "blocks=${unit.blockIndexes.map { blockIndex -> blockIndex + 1 }} " +
                        "src=${unit.sourceText.toDiagText()}"
                )
            }
        }
        if (useBatch) {
            val loopSession = beginLoopTranslation(diagId)
            try {
                batchTranslateFloatingWindow(translationUnits, settings, diagId)
            } finally {
                finishLoopTranslation(diagId, loopSession)
            }
            return
        }
        // Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВµР В Р’В Р РЋРІР‚СљР В Р’В Р вЂ™Р’В¶Р В Р’В Р В РЎвЂњР В Р’В Р В РІР‚в„–Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р вЂ™Р’ВР В Р’В Р В Р РЏР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р Р‹Р Р†РІР‚С›РЎС›Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р РЋРЎв„ўР В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р вЂ™Р’ВµР В Р’В Р В Р вЂ°Р В РІР‚в„ўР вЂ™Р’В Р В Р’В Р СћРІР‚ВР В Р’В Р Р†Р вЂљР’В¦Р В Р’В Р В Р вЂ° Р В Р’В Р В РІР‚В Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РЎС› Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎС›Р В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†Р вЂљРІР‚СњР В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљР Р‹Р В Р вЂ Р В РІР‚С™Р вЂ™Р’ВР В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎС›Р В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В РЎС›Р Р†Р вЂљР’ВР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћstreamingTranslate Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р Р‹Р Р†Р вЂљРІР‚СљР В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В РІР‚в„ўР вЂ™Р’В­Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В РІР‚в„ўР вЂ™Р’В¶ translateOne Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р В РЎвЂњР В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В°
        // Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В¬Р В Р’В Р В РІР‚в„–Р В Р’В Р вЂ™Р’В¶Р В Р’В Р Р†Р вЂљРЎв„ўР В РІР‚в„ўР вЂ™Р’В§ translate()Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В Р Р‹Р Р†Р вЂљРЎвЂќР В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В¬Р В Р’В Р В РІР‚в„–Р В Р’В Р вЂ™Р’В¶Р В Р Р‹Р РЋРІвЂћСћР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В»Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљРЎв„ўР В Р Р‹Р вЂ™Р’ВР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р РЋРІР‚ВР В Р’В Р В РІР‚в„–Р В Р’В Р В РІР‚В°Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’ВµР В Р’В Р В РІР‚В°Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р Р‹Р Р†Р вЂљРЎСљ"Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В®Р В РІР‚в„ўР вЂ™Р’ВµР В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В­Р В РЎС›Р РЋРІР‚в„ўР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВР В Р Р‹Р Р†Р вЂљРЎС›Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В¤Р В Р Р‹Р Р†Р вЂљРЎСљ"Р В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ
        withContext(Dispatchers.Main) {
            overlay?.prepareFloatingWindow(translationUnits.map { it.sourceText })
        }
        val loopSession = beginLoopTranslation(diagId)
        launchTranslationBatch(diagId) {
            try {
                translationUnits.mapIndexed { idx, unit ->
                    async {
                        translateOne(unit.sourceText, settings, diagId, idx) { partial, phase ->
                            withContext(Dispatchers.Main) {
                                if (translationBatchGate.accepts(diagId)) {
                                    overlay?.updateFloatingWindowText(idx, partial, phase)
                                }
                            }
                        }
                    }
                }.awaitAll()
            } finally {
                finishLoopTranslation(diagId, loopSession)
            }
        }
    }

    private suspend fun translateOne(
        text: String,
        settings: Settings,
        diagId: Long? = null,
        blockIndex: Int? = null,
        onUpdate: suspend (String, AdaptiveTextLayoutPhase) -> Unit
    ): Boolean {
        ensureCurrentTranslationBatch(diagId)
        diagId?.let {
            logVerticalDiag(
                it,
                "translateOne begin ${blockIndex.toDiagBlockLabel()} engine=${settings.translatorEngine.name} " +
                    "streaming=${settings.streamingTranslate} ${settings.sourceLang}->${settings.targetLang} " +
                    "src=${text.toDiagText()}"
            )
        }
        val translateStartedAt = System.currentTimeMillis()
        var succeeded = false
        try {
            if (settings.streamingTranslate) {
                // Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВµР В Р’В Р РЋРІР‚СљР В Р’В Р вЂ™Р’ВµР В Р Р‹Р вЂ™Р’ВР В Р’В Р В Р РЏР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р Р‹Р Р†РІР‚С›РЎС›Р В Р’В Р вЂ™Р’В·Р В РЎС›Р Р†Р вЂљР’ВР В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р В РІР‚в„– partial Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р В РЎвЂњР В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р Р†Р вЂљР’В¦Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В РЎС›Р РЋРІР‚в„ўР В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВµР В Р’В Р РЋРІР‚СљР В Р’В Р вЂ™Р’В¶Р В Р Р‹Р РЋРІвЂћСћР В РІР‚в„ўР вЂ™Р’В«Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎС›Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†РІР‚С›РЎС›Р В Р вЂ Р В РІР‚С™Р РЋРІР‚С” partial Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВР В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В®Р В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В РЎС›Р Р†Р вЂљР’ВР В Р’В Р РЋРІР‚ВР В Р’В Р Р†Р вЂљР Р‹Р В Р вЂ Р В РІР‚С™Р вЂ™Р’ВР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎС™Р В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°
                var lastPartial = ""
                var streamFailed = false
                translator.translateStream(text, settings)
                    .catch { e ->
                        ensureCurrentTranslationBatch(diagId)
                        streamFailed = true
                        diagId?.let {
                            logVerticalDiag(e, it, "translateStream failed ${blockIndex.toDiagBlockLabel()}")
                        }
                        onUpdate(
                            "[!] " + (e.message ?: ""),
                            AdaptiveTextLayoutPhase.FINAL,
                        )
                        logRepository.error(
                            LogRepository.Category.TRANSLATE,
                            getString(R.string.log_msg_stream_translate_failed_format, settings.translatorEngine.name),
                            e,
                            elapsedMs = elapsedSince(translateStartedAt)
                        )
                    }
                    .onEach {
                        ensureCurrentTranslationBatch(diagId)
                        lastPartial = it
                        onUpdate(it, AdaptiveTextLayoutPhase.STREAMING)
                    }
                    .collect()
                ensureCurrentTranslationBatch(diagId)
                if (streamFailed) {
                    return false
                }
                val display = resolveTranslationOutput(
                    initialOutput = lastPartial,
                    source = text,
                    settings = settings,
                    diagId = diagId,
                    label = blockIndex.toDiagBlockLabel(),
                )
                ensureCurrentTranslationBatch(diagId)
                // The final update is intentional even when the text is unchanged: Overlay uses
                // this phase transition to emit exactly one resolved-size diagnostic snapshot.
                onUpdate(display.text, AdaptiveTextLayoutPhase.FINAL)
                ensureCurrentTranslationBatch(diagId)
                if (!display.failed) {
                    succeeded = true
                    diagId?.let {
                        logVerticalDiag(
                            it,
                            "translateOne final ${blockIndex.toDiagBlockLabel()} ${display.text.toDiagText()}"
                        )
                    }
                    logRepository.pair(
                        LogRepository.Category.TRANSLATE,
                        text,
                        display.text,
                        elapsedMs = elapsedSince(translateStartedAt)
                    )
                } else {
                    diagId?.let {
                        logVerticalDiag(it, "translateOne final ${blockIndex.toDiagBlockLabel()} blank")
                    }
                    logRepository.warn(
                        LogRepository.Category.TRANSLATE,
                        getString(R.string.log_msg_translate_failed_format, settings.translatorEngine.name),
                        elapsedMs = elapsedSince(translateStartedAt),
                    )
                }
            } else {
                ensureCurrentTranslationBatch(diagId)
                val display = resolveTranslationOutput(
                    initialOutput = translator.translate(text, settings),
                    source = text,
                    settings = settings,
                    diagId = diagId,
                    label = blockIndex.toDiagBlockLabel(),
                )
                ensureCurrentTranslationBatch(diagId)
                val dst = display.text
                diagId?.let {
                    logVerticalDiag(
                        it,
                        "translateOne final ${blockIndex.toDiagBlockLabel()} ${dst.toDiagText()}"
                    )
                }
                onUpdate(dst, AdaptiveTextLayoutPhase.FINAL)
                ensureCurrentTranslationBatch(diagId)
                if (display.failed) {
                    logRepository.warn(
                        LogRepository.Category.TRANSLATE,
                        getString(R.string.log_msg_translate_failed_format, settings.translatorEngine.name),
                        elapsedMs = elapsedSince(translateStartedAt),
                    )
                } else {
                    succeeded = true
                    logRepository.pair(
                        LogRepository.Category.TRANSLATE,
                        text,
                        dst,
                        elapsedMs = elapsedSince(translateStartedAt)
                    )
                }
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: TranslationException) {
            ensureCurrentTranslationBatch(diagId)
            diagId?.let {
                logVerticalDiag(e, it, "translateOne translation error ${blockIndex.toDiagBlockLabel()}")
            }
            onUpdate(
                "[!] " + (e.message ?: ""),
                AdaptiveTextLayoutPhase.FINAL,
            )
            logRepository.error(
                LogRepository.Category.TRANSLATE,
                getString(R.string.log_msg_translate_failed_format, settings.translatorEngine.name),
                e,
                elapsedMs = elapsedSince(translateStartedAt)
            )
        } catch (t: Throwable) {
            ensureCurrentTranslationBatch(diagId)
            Timber.w(t, "Translate unexpected error")
            diagId?.let {
                logVerticalDiag(t, it, "translateOne unexpected error ${blockIndex.toDiagBlockLabel()}")
            }
            onUpdate("[!]", AdaptiveTextLayoutPhase.FINAL)
            logRepository.error(
                LogRepository.Category.TRANSLATE,
                getString(R.string.log_msg_translate_exception_format, settings.translatorEngine.name),
                t,
                elapsedMs = elapsedSince(translateStartedAt)
            )
        }
        return succeeded
    }

    private suspend fun resolveTranslationOutput(
        initialOutput: String?,
        source: String,
        settings: Settings,
        diagId: Long?,
        label: String,
    ): TranslationOutputDecision {
        ensureCurrentTranslationBatch(diagId)
        val failureText = "[!] " + getString(R.string.process_text_translate_failed)
        if (
            TranslationOutputPolicy.action(
                output = initialOutput,
                retryEnabled = settings.retryEmptyTranslation,
                attempt = 0,
            ) != EmptyTranslationAction.RETRY
        ) {
            return TranslationOutputPolicy.resolve(initialOutput, failureText)
        }

        diagId?.let { logVerticalDiag(it, "emptyTranslation retry begin $label") }
        val retryOutput = try {
            withContext(Dispatchers.IO) { translator.translate(source, settings) }
        } catch (ce: kotlinx.coroutines.CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Timber.w(t, "Empty translation retry failed")
            diagId?.let { logVerticalDiag(t, it, "emptyTranslation retry error $label") }
            logRepository.error(
                LogRepository.Category.TRANSLATE,
                getString(R.string.log_msg_translate_failed_format, settings.translatorEngine.name),
                t,
            )
            null
        }
        val display = TranslationOutputPolicy.resolve(retryOutput, failureText)
        ensureCurrentTranslationBatch(diagId)
        diagId?.let {
            logVerticalDiag(
                it,
                "emptyTranslation retry final $label failed=${display.failed} ${display.text.toDiagText()}"
            )
        }
        return display
    }

    private data class DisplayGeometrySnapshot(
        val overlayWidth: Int,
        val overlayHeight: Int,
        val resourceWidth: Int,
        val resourceHeight: Int,
        val currentBounds: String,
        val maximumBounds: String,
        val rotation: Int,
        val configurationOrientation: Int,
        val densityDpi: Int
    ) {
        fun toDiagString(): String =
            "physicalOverlay=${overlayWidth}x$overlayHeight " +
                "resources=${resourceWidth}x$resourceHeight currentBounds=$currentBounds " +
                "maximumBounds=$maximumBounds rotation=${rotation.toDiagRotation()} " +
                "config=${configurationOrientation.toDiagOrientation()} densityDpi=$densityDpi"
    }

    private fun currentDisplayGeometry(): DisplayGeometrySnapshot {
        val dm = resources.displayMetrics
        val physicalSize = physicalDisplaySize(this)
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val currentBounds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { wm.currentWindowMetrics.bounds.toDiagString() }.getOrElse { "unavailable" }
        } else {
            "legacy"
        }
        val maximumBounds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { wm.maximumWindowMetrics.bounds.toDiagString() }.getOrElse { "unavailable" }
        } else {
            "legacy"
        }
        return DisplayGeometrySnapshot(
            overlayWidth = physicalSize.width,
            overlayHeight = physicalSize.height,
            resourceWidth = dm.widthPixels,
            resourceHeight = dm.heightPixels,
            currentBounds = currentBounds,
            maximumBounds = maximumBounds,
            rotation = currentDisplayRotation(wm),
            configurationOrientation = resources.configuration.orientation,
            densityDpi = resources.configuration.densityDpi
        )
    }

    @Suppress("DEPRECATION")
    private fun currentDisplayRotation(wm: WindowManager): Int = wm.defaultDisplay.rotation

    private fun projectionDiagnosticSummary(): String =
        (screenshotter as? MediaProjectionScreenshotter)?.diagnosticSummary()
            ?: "type=${screenshotter?.javaClass?.simpleName ?: "null"} ready=${screenshotter?.isReady ?: false}"

    @Suppress("DEPRECATION")
    private fun resizeProjectionForCurrentDisplay(reason: String) {
        val projectionScreenshotter = screenshotter as? MediaProjectionScreenshotter ?: return
        val target = physicalDisplaySize(this)
        projectionScreenshotter.resizeProjection(target.width, target.height, reason)
    }

    private fun logCaptureGeometry(diagId: Long, stage: String, bitmap: Bitmap) {
        val display = currentDisplayGeometry()
        val diagnostic = diagnoseCaptureGeometry(
            frameWidth = bitmap.width,
            frameHeight = bitmap.height,
            overlayWidth = display.overlayWidth,
            overlayHeight = display.overlayHeight
        )
        val message =
            "capture#$diagId coordinateSpace stage=$stage ${diagnostic.toDiagString()} " +
                "display=${display.toDiagString()} projection=${projectionDiagnosticSummary()}"
        if (diagnostic.relation == CaptureCoordinateRelation.MATCH) {
            VerticalDiagnosticLog.i(message)
        } else {
            VerticalDiagnosticLog.w("$message COORDINATE_SPACE_WARNING")
        }
    }

    private fun logVerticalDiag(diagId: Long, message: String) {
        VerticalDiagnosticLog.i("capture#$diagId $message")
    }

    private fun logVerticalDiag(t: Throwable, diagId: Long, message: String) {
        VerticalDiagnosticLog.w(t, "capture#$diagId $message")
    }

    private fun logBlankLikeFrame(diagId: Long, label: String, stats: BitmapFrameStats) {
        if (!stats.blankLike) return
        logVerticalDiag(
            diagId,
            "$label blank-like frame; MediaProjection may be seeing a protected or empty surface"
        )
    }

    private fun logVerticalSettings(
        diagId: Long,
        settings: Settings,
        screenW: Int,
        screenH: Int
    ) {
        logVerticalDiag(
            diagId,
            "settings screen=${screenW}x${screenH} region=${settings.captureRegion.toDiagString()} " +
                "source=${settings.sourceLang} target=${settings.targetLang} " +
                "translator=${settings.translatorEngine.name} " +
                "paddleVersion=${settings.paddleModelVersion.name} " +
                "baiduEndpoint=${settings.baiduOcrEndpoint.name} baiduLanguage=${settings.baiduOcrLanguage.name} " +
                "dbnet=prob:${settings.dbnetProbThresh.toDiagFloat()},box:${settings.dbnetBoxScoreThresh.toDiagFloat()},unclip:${settings.dbnetUnclipRatio.toDiagFloat()} " +
                "render=${settings.renderMode.name} streaming=${settings.streamingTranslate} " +
                "retryEmptyTranslation=${settings.retryEmptyTranslation} " +
                "loopTrigger=${settings.loopTriggerMode.name} loopIntervalMs=${settings.captureLoopIntervalMs} " +
                "loopPollMs=${LoopFrameStabilityPolicy.pollingIntervalMs(
                    settings.captureLoopIntervalMs,
                    settings.loopTriggerMode == LoopTriggerMode.WAIT_FOR_TEXT_COMPLETE,
                )} loopStableMs=${settings.loopTextStableDurationMs} " +
                "loopSkipSimilar=${settings.loopSkipSimilarFrames} " +
                "loopSimilarity=${settings.loopFrameSimilarityThreshold.toDiagFloat()} " +
                "loopTextRegion=${settings.loopTextRegionMode.name} " +
                "loopTranslateRegionOnly=${settings.loopTranslateRegionOnly} " +
                "autoOrient=${settings.textOrientationAutoDetect} manualOrient=${settings.manualTextOrientation?.name ?: "null"} " +
                "preprocess=${settings.preprocess.toDiagString()} merge=${settings.mergeAdjacentBlocks}/${settings.mergeStrength.name} " +
                "overlayPlacement=${settings.overlayPlacement.name} overlayTextSizeSp=${settings.overlayTextSizeSp} " +
                "allowWrap=${settings.overlayAllowWrap} avoidCollision=${settings.overlayAvoidCollision}"
        )
    }

    private fun logVerticalOrientation(
        diagId: Long,
        stage: String,
        result: OrientationResult
    ) {
        logVerticalDiag(
            diagId,
            "orientation-$stage orientation=${result.orientation.name} conf=${result.confidence.toDiagFloat()} " +
                "raw=${result.rawAngle} source=${result.source}"
        )
    }

    private fun logVerticalBlocks(
        diagId: Long,
        label: String,
        blocks: List<TextBlock>
    ) {
        logVerticalDiag(diagId, "$label count=${blocks.size}")
        blocks.forEachIndexed { index, block ->
            val r = block.boundingBox
            logVerticalDiag(
                diagId,
                "$label #${index + 1} box=${r.toDiagString()} size=${r.width()}x${r.height()} " +
                    "conf=${block.confidence.toDiagFloat()} lang=${block.recognizedLanguage ?: "null"} " +
                    "layout=${block.layoutOrientation?.name ?: "null"} ${block.text.toDiagText()}"
            )
        }
    }

    private fun logVerticalTranslatedBlocks(
        diagId: Long,
        label: String,
        items: List<Pair<TextBlock, String>>
    ) {
        logVerticalDiag(diagId, "$label translated count=${items.size}")
        items.forEachIndexed { index, (block, dst) ->
            logVerticalDiag(
                diagId,
                "$label #${index + 1} src=${block.text.toDiagText()} dst=${dst.toDiagText()} " +
                    "box=${block.boundingBox.toDiagString()} layout=${block.layoutOrientation?.name ?: "null"}"
            )
        }
    }

    private fun String.toDiagText(): String =
        "len=$length text=\"${VerticalDiagnosticLog.text(this)}\""

    private fun Int?.toDiagBlockLabel(): String =
        this?.let { "block#${it + 1}" } ?: "block#?"

    private fun Float.toDiagFloat(): String =
        String.format(Locale.US, "%.3f", this)

    private fun com.gameocr.app.data.PreprocessOptions.toDiagString(): String =
        "upscale2x=$upscale2x,invert=$invert,binarize=$binarize"

    private fun CaptureRegion?.toDiagString(): String =
        this?.let { "(${it.left},${it.top},${it.right},${it.bottom})" } ?: "full"

    private fun android.graphics.Rect.toDiagString(): String =
        "($left,$top,$right,$bottom)"

    private fun logOcrInfo(message: String) {
        Timber.i(message)
    }

    private suspend fun applyOverlayConfig(
        settings: Settings,
        syncFloatingWindowLock: Boolean,
    ) {
        val typeface = overlayFontManager.typefaceFor(settings)
        val dockEdgeInsetPx = (settings.floatingButtonDockInsetDp * resources.displayMetrics.density).toInt()
        val adaptiveBlocksEnabled =
            adaptiveOverlayActive(settings.overlayStyleMode, settings.renderMode)
        val effectiveOverlaySettings = settings.effectiveOverlayRenderSettings()
        withContext(Dispatchers.Main) {
            overlay?.apply {
                overlayStyleMode = if (adaptiveBlocksEnabled) {
                    OverlayStyleMode.ADAPTIVE
                } else {
                    OverlayStyleMode.FIXED
                }
                textSizeSp = effectiveOverlaySettings.overlayTextSizeSp
                alpha = effectiveOverlaySettings.overlayAlpha
                regionOffset = settings.captureRegion?.let { Point(it.left, it.top) } ?: Point(0, 0)
                placement = effectiveOverlaySettings.overlayPlacement
                offsetX = effectiveOverlaySettings.overlayOffsetX
                offsetY = effectiveOverlaySettings.overlayOffsetY
                theme = effectiveOverlaySettings.overlayTheme
                customBg = settings.customBgColor
                customFg = settings.customFgColor
                customBorder = settings.customBorderColor
                customBorderWidthDp = settings.customBorderWidth
                allowWrap = effectiveOverlaySettings.overlayAllowWrap
                avoidCollision = effectiveOverlaySettings.overlayAvoidCollision
                translationBlockInteractionMode = settings.translationBlockInteractionMode
                floatingWindowContentMode = settings.floatingWindowContentMode
                customBorderStyle = settings.customBorderStyle
                overlayTypeface = typeface
                overlayTextStyle = effectiveOverlaySettings.overlayTextStyle.normalized()
                ocrDebugRedBoxActive = DeveloperOcrDebugPolicy.redBoxActive(
                    settings.developerOptionsEnabled,
                    settings.ocrRedBoxModeEnabled,
                )
                ocrDebugShowSourceText = settings.ocrRedBoxShowSourceText
                ocrDebugShowTranslation = settings.ocrRedBoxShowTranslation
                syncFloatingWindowFromSettings(
                    effectiveOverlaySettings,
                    syncLockedState = syncFloatingWindowLock,
                )
            }
            // Overlay / floating button both own Android Views; keep every visible update on main.
            floatingButton?.let {
                if (it.sizeDp != settings.floatingButtonSizeDp) {
                    it.sizeDp = settings.floatingButtonSizeDp
                    it.applyResize()
                }
                it.applySnapPreference(settings.floatingButtonSnapToEdge)
                it.autoDockEnabled = settings.floatingButtonAutoDock
                it.dockEdgeInsetPx = dockEdgeInsetPx
                it.menuItemOrder = settings.floatingMenuItemOrder
                it.arcMenuPageSize = settings.arcMenuPageSize
                if (it.skill != settings.floatingButtonSkill) {
                    it.skill = settings.floatingButtonSkill
                    it.applySkillIcon()
                }
            }
        }
    }

    /** Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р Р†Р вЂљР’В°Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р Р‹Р Р†Р вЂљРЎС›Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р В Р РЏР В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р Р‹Р Р†Р вЂљРІР‚СљР В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р РЋРІР‚С”Р В Р’В Р вЂ™Р’В¶Р В Р Р‹Р Р†Р вЂљРЎСљР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’ВµР В Р’В Р РЋРІР‚СљР В Р Р‹Р РЋРІвЂћСћ ServiceР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р В РЎвЂњР В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р Р†Р вЂљРІвЂћвЂ“ handleStart Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р В Р вЂ°Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В РЎС›Р РЋРІР‚в„ўР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В РІР‚в„ўР вЂ™Р’В¶Р В Р’В Р вЂ™Р’ВµР В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р В Р вЂ° + onDestroy Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р Р‹Р РЋРІвЂћСћР В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎСљР В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ */
    private fun cleanupCapture() {
        cancelActiveTranslationBatch("cleanupCapture")
        loopMode = false
        loopJob?.cancel()
        loopJob = null
        ocrWarmupJob?.cancel()
        ocrWarmupJob = null
        resetLoopFrameHistory()
        resetLoopRuntimeState()
        settingsCollectJob?.cancel()
        settingsCollectJob = null
        overlay?.clear()
        overlay = null
        floatingButton?.hide()
        floatingButton = null
        regionPicker?.dismiss()
        regionPicker = null
        languageQuickSwitch?.dismiss()
        languageQuickSwitch = null
        presetQuickSwitch?.dismiss()
        presetQuickSwitch = null
        translationCard?.dismiss()
        translationCard = null
        translationBlockCopyOverlay?.dismiss()
        translationBlockCopyOverlay = null
        screenshotter?.release()
        screenshotter = null
        projection?.stop()
        projection = null
    }

    override fun onDestroy() {
        historyBlockPicker?.dismiss()
        historyCorrectionOverlay?.dismiss()
        historyBlockPicker = null
        cleanupCapture()
        // Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р Р†Р вЂљР’В°Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р Р‹Р Р†Р вЂљРЎС›Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В«Р В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљРЎС›Р В РІР‚в„ўР вЂ™Р’В§ LLM Р В Р’В Р вЂ™Р’В¶Р В Р Р‹Р РЋРЎв„ўР В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В Р’В Р В Р вЂ°Р В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћrunBlocking Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРІвЂћСћР В Р’В Р В РЎвЂњ onDestroy Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВР В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’ВµР В Р’В Р В Р РЏР В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’В¶Р В Р’В Р Р†Р вЂљРІвЂћвЂ“Р В РЎС›Р РЋРІР‚в„ўР В Р’В Р вЂ™Р’ВµР В Р’В Р В Р РЏР В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†РІР‚С›РЎС›Р В Р вЂ Р В РІР‚С™Р РЋРІР‚С”Р В Р’В Р В РІР‚В Р В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р В РІР‚В Р В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРЎС™cleanUp Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В¦Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р В РЎвЂњР В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’ВР В Р’В Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРІвЂћСћР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’В¶Р В РІР‚в„ўР вЂ™Р’В­Р В РЎС›Р РЋРІР‚в„ў JNI Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р вЂ™Р’В·Р В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р В РЎвЂњР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°
        // Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р В Р вЂ№Р В РІР‚в„ўР вЂ™Р’В Р В Р’В Р вЂ™Р’ВµР В Р’В Р В Р вЂ°Р В Р’В Р РЋРІР‚СљР В Р’В Р вЂ™Р’В¶Р В Р’В Р Р†Р вЂљР Р‹Р В РІР‚в„ўР вЂ™Р’В«Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В§Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РЎС›Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†Р вЂљРЎСљР В РІР‚в„ўР вЂ™Р’В§Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р В Р вЂ°Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В РІР‚в„ўР вЂ™Р’ВР В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р вЂ™Р’ВµР В Р’В Р В РІР‚в„–Р В Р Р‹Р Р†Р вЂљРЎвЂќР В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†Р вЂљРЎСљР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р’В Р вЂ™Р’В·Р В Р’В Р В РЎвЂњР В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р’В Р вЂ™Р’В¶Р В Р’В Р Р†Р вЂљР’В Р В Р’В Р В РІР‚в„–Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р РЋРІР‚С”Р В Р’В Р В Р РЏР В Р’В Р СћРІР‚ВР В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В­Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В° Mutex Р В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р РЋРІР‚СљР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В°Р В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В«Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р’В Р вЂ™Р’ВµР В Р вЂ Р Р†Р вЂљРЎв„ўР вЂ™Р’В¬Р В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р вЂ™Р’ВµР В Р вЂ Р В РІР‚С™Р РЋРІР‚СњР В Р Р‹Р Р†Р вЂљРЎвЂќР В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ
        scope.cancel()
        mainScope.cancel()
        CaptureServiceState.setRunning(false)
        Timber.i("CaptureService destroyed")
        if (restartWithMediaProjectionOnDestroy) {
            startActivity(MediaProjectionRequestActivity.newIntent(this))
        }
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.gameocr.app.action.START"
        const val ACTION_STOP = "com.gameocr.app.action.STOP"
        const val ACTION_TRIGGER_ONCE = "com.gameocr.app.action.TRIGGER_ONCE"
        const val ACTION_RUN_FLOATING_TOUR =
            "com.gameocr.app.action.RUN_FLOATING_TOUR"
        /** Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљР’ВР В РІР‚в„ўР вЂ™Р’В»Р В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р В Р РЏР В Р’В Р вЂ™Р’В¶Р В Р’В Р В РІР‚в„–Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р вЂ™Р’В°Р В Р’В Р вЂ™Р’ВµР В Р’В Р В РІР‚В°Р В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р вЂ™Р’ВµР В Р Р‹Р РЋРЎСџР В Р Р‹Р РЋРЎСџР В Р’В Р вЂ™Р’В¶Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљР В РІР‚в„ўР вЂ™Р’В¶Р В Р’В Р РЋРІР‚ВР В РІР‚в„ўР вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРІР‚СњР В Р вЂ Р Р†Р вЂљРЎвЂєР РЋРЎвЂєР В Р’В Р вЂ™Р’В¶Р В Р Р‹Р РЋРЎв„ўР В Р’В Р В РІР‚в„–Р В Р’В Р В РІР‚В Р В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРЎС™Р В Р’В Р В РІР‚В Р В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРЎС™floating window Р В Р’В Р вЂ™Р’В¶Р В Р’В Р В РЎвЂњР В Р’В Р В РІР‚в„–Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р вЂ™Р’ВР В Р’В Р В Р РЏР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р вЂ™Р’ВР В Р’В Р В РІР‚В°Р В Р’В Р вЂ™Р’В·Р В РІР‚в„ўР вЂ™Р’В»Р В Р вЂ Р В РІР‚С™Р РЋРЎвЂєР В Р’В Р вЂ™Р’ВµР В Р Р‹Р вЂ™Р’ВР В Р’В Р Р†Р вЂљРЎв„ў Activity Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†РІР‚С›РЎС›Р В Р вЂ Р В РІР‚С™Р РЋРІР‚С”Р В Р’В Р вЂ™Р’В¶Р В Р’В Р В РЎвЂњР В Р’В Р Р†Р вЂљРЎвЂєР В Р’В Р вЂ™Р’ВµР В РІР‚в„ўР вЂ™Р’В±Р В Р’В Р В Р РЏ long-edge cutout letterboxР В Р’В Р РЋРІР‚вЂњР В Р’В Р Р†Р вЂљРЎв„ўР В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћ */
        const val ACTION_PICK_REGION = "com.gameocr.app.action.PICK_REGION"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val EXTRA_USE_SHIZUKU = "extra_use_shizuku"
        const val EXTRA_USE_ASB = "extra_use_asb"
        private const val CAPTURE_CHROME_SETTLE_MS = 80L

        fun stopIntent(context: Context): Intent =
            Intent(context, CaptureService::class.java).apply { action = ACTION_STOP }

        fun runFloatingTourIntent(context: Context): Intent =
            Intent(context, CaptureService::class.java).apply {
                action = ACTION_RUN_FLOATING_TOUR
            }
    }
}

private fun Int.toDiagRotation(): String = when (this) {
    Surface.ROTATION_0 -> "ROTATION_0"
    Surface.ROTATION_90 -> "ROTATION_90"
    Surface.ROTATION_180 -> "ROTATION_180"
    Surface.ROTATION_270 -> "ROTATION_270"
    else -> "UNKNOWN($this)"
}

private fun Int.toDiagOrientation(): String = when (this) {
    android.content.res.Configuration.ORIENTATION_PORTRAIT -> "PORTRAIT"
    android.content.res.Configuration.ORIENTATION_LANDSCAPE -> "LANDSCAPE"
    android.content.res.Configuration.ORIENTATION_UNDEFINED -> "UNDEFINED"
    else -> "UNKNOWN($this)"
}

