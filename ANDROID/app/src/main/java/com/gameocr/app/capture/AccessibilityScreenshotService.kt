package com.gameocr.app.capture

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityEvent
import com.gameocr.app.appcontext.ForegroundAppState
import java.util.concurrent.Executor
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

/** Accessibility-backed, on-demand screenshot provider (Android 11+). */
class AccessibilityScreenshotService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Timber.i("Accessibility screenshot service connected")
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            ForegroundAppState.record(applicationContext, event.packageName)
        }
    }

    override fun onInterrupt() = Unit

    suspend fun captureBitmap(): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return suspendCancellableCoroutine { continuation ->
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                Executor { Handler(Looper.getMainLooper()).post(it) },
                object : TakeScreenshotCallback {
                    override fun onSuccess(result: ScreenshotResult) {
                        if (!continuation.isActive) return
                        val bitmap = runCatching {
                            val buffer = result.hardwareBuffer
                                ?: error("Accessibility screenshot returned no HardwareBuffer")
                            try {
                                Bitmap.wrapHardwareBuffer(buffer, result.colorSpace)
                                    ?.copy(Bitmap.Config.ARGB_8888, false)
                                    ?: error("Unable to create Bitmap from HardwareBuffer")
                            } finally {
                                buffer.close()
                            }
                        }.onFailure { Timber.w(it, "ASB SCREENSHOT FAILED reason=%s", it.message) }
                            .getOrNull()
                        continuation.resume(bitmap)
                    }

                    override fun onFailure(errorCode: Int) {
                        Timber.w("ASB SCREENSHOT FAILED reason=errorCode=%d", errorCode)
                        if (continuation.isActive) continuation.resume(null)
                    }
                },
            )
        }
    }

    companion object {
        @Volatile private var instance: AccessibilityScreenshotService? = null

        internal fun current(): AccessibilityScreenshotService? = instance

        fun isEnabled(context: Context): Boolean {
            val component = ComponentName(context, AccessibilityScreenshotService::class.java)
                .flattenToString()
            val manager = context.getSystemService(AccessibilityManager::class.java)
            return manager.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            ).any { it.id == component }
        }
    }
}
