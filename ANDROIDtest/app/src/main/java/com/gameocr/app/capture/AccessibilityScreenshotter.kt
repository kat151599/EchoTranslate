package com.gameocr.app.capture

import android.graphics.Bitmap
import java.util.concurrent.atomic.AtomicBoolean
import timber.log.Timber

/** [Screenshotter] implementation backed by [AccessibilityService.takeScreenshot]. */
class AccessibilityScreenshotter : Screenshotter {
    private val released = AtomicBoolean(false)

    override val isReady: Boolean
        get() = !released.get() && AccessibilityScreenshotService.current() != null

    override suspend fun capture(): Bitmap? {
        if (!isReady) {
            Timber.w("ASB SCREENSHOT FAILED reason=Accessibility Service is not connected")
            return null
        }
        return runCatching { AccessibilityScreenshotService.current()?.captureBitmap() }
            .onFailure { Timber.w(it, "ASB SCREENSHOT FAILED reason=%s", it.message) }
            .getOrNull()
            ?.also { Timber.i("ASB SCREENSHOT OK") }
            ?: run {
                Timber.w("ASB SCREENSHOT FAILED reason=screenshot unavailable")
                null
            }
    }

    override fun release() {
        released.set(true)
    }
}
