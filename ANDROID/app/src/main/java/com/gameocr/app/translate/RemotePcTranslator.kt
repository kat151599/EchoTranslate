package com.gameocr.app.translate

import android.graphics.Bitmap
import android.graphics.Rect
import com.gameocr.app.appcontext.ForegroundAppResolver
import com.gameocr.app.data.Settings
import com.gameocr.app.data.withApiTimeout
import com.gameocr.app.ocr.TextBlock
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber

/**
 * Thin Android client for the companion PC server.
 *
 * Wire contract:
 * POST {baseUrl}/v1/screen/translate as multipart/form-data
 *   image       JPEG binary
 *   source_lang BCP-47 / auto
 *   target_lang BCP-47
 *   session_id  server-side context session
 *
 * Optional header: Authorization: Bearer <remotePcApiKey>
 *
 * Response:
 * {
 *   "blocks": [
 *     {"source":"...","translation":"...","confidence":0.99,
 *      "box":[left,top,right,bottom],"language":"ja"}
 *   ]
 * }
 *
 * Coordinates are relative to the uploaded image. CaptureService already maps its crop back to
 * screen coordinates using the same end-to-end path used by Youdao image translation.
 */
@Singleton
class RemotePcTranslator @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
    private val foregroundAppResolver: ForegroundAppResolver,
) : Translator {
    override val isEndToEnd: Boolean get() = true

    override suspend fun translate(source: String, settings: Settings): String? =
        throw TranslationException("Remote PC mode accepts screenshots, not text")

    override fun translateStream(source: String, settings: Settings): Flow<String> = emptyFlow()

    override suspend fun testConnection(settings: Settings): TestResult {
        val base = normalizedBaseUrl(settings.remotePcBaseUrl)
            ?: return TestResult(false, "PC server URL is empty or invalid")
        val request = Request.Builder()
            .url("$base/health")
            .get()
            .header("X-GameOCR-Remote-PC", "1")
            .applyAuth(settings)
            .build()
        return runCatching {
            withContext(Dispatchers.IO) {
                client.withApiTimeout(settings.apiTimeoutSeconds).newCall(request).execute().use { r ->
                    val body = r.body?.string().orEmpty()
                    if (r.isSuccessful) TestResult(true, body.ifBlank { "OK · PC server available" }.take(180))
                    else TestResult(false, "HTTP ${r.code}: ${body.take(180)}")
                }
            }
        }.getOrElse { TestResult(false, it.message ?: it.javaClass.simpleName) }
    }

    override suspend fun ocrAndTranslate(
        bitmap: Bitmap,
        settings: Settings,
    ): List<Pair<TextBlock, String>> {
        val base = normalizedBaseUrl(settings.remotePcBaseUrl)
            ?: throw TranslationException("PC server URL is empty or invalid")
        val jpeg = withContext(Dispatchers.Default) {
            ByteArrayOutputStream().use { out ->
                val quality = settings.remotePcImageQuality.coerceIn(50, 100)
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)) {
                    throw TranslationException("Failed to encode screenshot as JPEG")
                }
                out.toByteArray()
            }
        }
        val foregroundApp = foregroundAppResolver.latestAccessibilityApp()
        Timber.i(
            "TRANSLATE APP package=${foregroundApp?.packageName.orEmpty()} " +
                "name=${foregroundApp?.displayName.orEmpty()}",
        )
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "image",
                "screen.jpg",
                jpeg.toRequestBody("image/jpeg".toMediaType()),
            )
            .addFormDataPart("source_lang", settings.sourceLang)
            .addFormDataPart("target_lang", settings.targetLang)
            .addFormDataPart("session_id", settings.remotePcSessionId.ifBlank { "default" })
            .addFormDataPart("app_package", foregroundApp?.packageName.orEmpty())
            .addFormDataPart("app_name", foregroundApp?.displayName.orEmpty())
            .build()
        val request = Request.Builder()
            .url("$base/v1/screen/translate")
            .post(body)
            .header("X-GameOCR-Remote-PC", "1")
            .applyAuth(settings)
            .build()
        return withContext(Dispatchers.IO) {
            client.withApiTimeout(settings.apiTimeoutSeconds).newCall(request).execute().use { r ->
                val raw = r.body?.string().orEmpty()
                if (!r.isSuccessful) {
                    throw TranslationException("PC server HTTP ${r.code}: ${raw.take(300)}")
                }
                val response = runCatching { json.decodeFromString<RemoteScreenResponse>(raw) }
                    .getOrElse { throw TranslationException("PC server response parse failed: ${raw.take(300)}", it) }
                response.blocks.mapNotNull { block ->
                    val box = block.box
                    if (box.size < 4 || block.translation.isBlank()) return@mapNotNull null
                    val left = box[0].coerceIn(0, bitmap.width)
                    val top = box[1].coerceIn(0, bitmap.height)
                    val right = box[2].coerceIn(left, bitmap.width)
                    val bottom = box[3].coerceIn(top, bitmap.height)
                    TextBlock(
                        text = block.source,
                        boundingBox = Rect(left, top, right, bottom),
                        confidence = block.confidence.coerceIn(0f, 1f),
                        recognizedLanguage = block.language ?: settings.sourceLang,
                        historyId = block.historyId,
                    ) to block.translation
                }
            }
        }
    }

    suspend fun retranslateHistory(historyId: Long, settings: Settings): RemoteHistoryTranslation {
        val base = normalizedBaseUrl(settings.remotePcBaseUrl)
            ?: throw TranslationException("PC server URL is empty or invalid")
        val request = Request.Builder()
            .url("$base/v1/history/$historyId/retranslate")
            .post(ByteArray(0).toRequestBody(null))
            .header("X-GameOCR-Remote-PC", "1")
            .applyAuth(settings)
            .build()
        return executeHistoryRequest(request, settings)
    }

    suspend fun deleteHistory(historyId: Long, settings: Settings) {
        val base = normalizedBaseUrl(settings.remotePcBaseUrl)
            ?: throw TranslationException("PC server URL is empty or invalid")
        val request = Request.Builder()
            .url("$base/v1/history/$historyId")
            .delete()
            .header("X-GameOCR-Remote-PC", "1")
            .applyAuth(settings)
            .build()
        withContext(Dispatchers.IO) {
            client.withApiTimeout(settings.apiTimeoutSeconds).newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw TranslationException("PC server HTTP ${response.code}: ${raw.take(300)}")
            }
        }
    }

    private suspend fun executeHistoryRequest(request: Request, settings: Settings): RemoteHistoryTranslation =
        withContext(Dispatchers.IO) {
            client.withApiTimeout(settings.apiTimeoutSeconds).newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw TranslationException("PC server HTTP ${response.code}: ${raw.take(300)}")
                runCatching { json.decodeFromString<RemoteHistoryTranslation>(raw) }
                    .getOrElse { throw TranslationException("PC server response parse failed: ${raw.take(300)}", it) }
            }
        }

    private fun Request.Builder.applyAuth(settings: Settings): Request.Builder = apply {
        settings.remotePcApiKey.trim().takeIf { it.isNotEmpty() }?.let {
            header("Authorization", "Bearer $it")
        }
    }

    private fun normalizedBaseUrl(value: String): String? {
        val v = value.trim().trimEnd('/')
        if (!(v.startsWith("http://") || v.startsWith("https://"))) return null
        return v
    }

    @Serializable
    private data class RemoteScreenResponse(
        val blocks: List<RemoteTranslatedBlock> = emptyList(),
    )

    @Serializable
    private data class RemoteTranslatedBlock(
        val source: String = "",
        val translation: String = "",
        val confidence: Float = 1f,
        val box: List<Int> = emptyList(),
        val language: String? = null,
        @SerialName("history_id") val historyId: Long? = null,
    )

    @Serializable
    data class RemoteHistoryTranslation(
        @SerialName("history_id") val historyId: Long,
        val source: String,
        val translation: String,
    )
}
