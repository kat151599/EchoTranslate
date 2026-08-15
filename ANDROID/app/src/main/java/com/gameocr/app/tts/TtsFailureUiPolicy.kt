package com.gameocr.app.tts

import android.content.Context
import com.gameocr.app.R

private const val MAX_TTS_FAILURE_DETAIL_LENGTH = 160

internal fun ttsFailureDetail(error: Throwable): String {
    val causes = generateSequence(error) { current -> current.cause }
    val rawDetail = causes
        .mapNotNull { cause -> cause.message?.takeIf(String::isNotBlank) }
        .firstOrNull()
        ?: error.javaClass.simpleName.takeIf(String::isNotBlank)
        ?: "Unknown error"
    val normalized = rawDetail.replace(Regex("\\s+"), " ").trim()
    val detail = if (normalized.length <= MAX_TTS_FAILURE_DETAIL_LENGTH) {
        normalized
    } else {
        normalized.take(MAX_TTS_FAILURE_DETAIL_LENGTH - 1) + "…"
    }
    return detail
}

internal fun Context.ttsFailureMessage(error: Throwable): String =
    getString(R.string.toast_tts_failed_format, ttsFailureDetail(error))
