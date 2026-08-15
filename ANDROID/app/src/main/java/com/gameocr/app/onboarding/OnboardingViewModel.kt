package com.gameocr.app.onboarding

import androidx.lifecycle.ViewModel
import com.gameocr.app.data.PaddleModelVersion
import com.gameocr.app.data.SettingsRepository
import com.gameocr.app.download.ModelDownloadManager
import com.gameocr.app.download.ModelDownloadSpec
import com.gameocr.app.llm.LlmModelInstaller
import com.gameocr.app.llm.LlmModelKind
import com.gameocr.app.ocr.MangaOcrModelInstaller
import com.gameocr.app.ocr.PaddleModelInstaller
import com.gameocr.app.translate.RoutingTranslator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val routingTranslator: RoutingTranslator,
    private val mangaOcrModelInstaller: MangaOcrModelInstaller,
    private val paddleModelInstaller: PaddleModelInstaller,
    private val llmModelInstaller: LlmModelInstaller,
    private val modelDownloadManager: ModelDownloadManager,
) : ViewModel() {
    suspend fun loadDraft(firstRun: Boolean): OnboardingDraft =
        if (firstRun) OnboardingDraft()
        else OnboardingPolicy.fromSettings(settingsRepository.get())

    suspend fun save(draft: OnboardingDraft) {
        settingsRepository.update { current -> OnboardingPolicy.apply(current, draft) }
    }

    suspend fun downloadMlKitLanguagePair(sourceLang: String, targetLang: String) {
        routingTranslator.downloadMlKitLanguagePair(sourceLang, targetLang)
    }

    fun paddleV5ModelReady(): Boolean =
        paddleModelInstaller.checkInstalled(PaddleModelVersion.V5_MOBILE) != null

    suspend fun downloadPaddleV5Model(onProgress: (String) -> Unit) {
        val specs = paddleOcrDownloadSpecs(paddleV5ModelReady())
        if (specs.isNotEmpty()) {
            modelDownloadManager.enqueueIndependentlyAndAwait(specs, onProgress)
        }
    }

    suspend fun missingMlKitLanguageModels(
        sourceLang: String,
        targetLang: String,
    ): Set<String> = routingTranslator.getMissingMlKitLanguageModels(sourceLang, targetLang)

    fun mangaOfflineModelReadiness(): MangaOfflineModelReadiness =
        MangaOfflineModelReadiness(
            mangaOcrReady = mangaOcrModelInstaller.checkInstalled() != null,
            sakuraReady =
                llmModelInstaller.checkInstalled(LlmModelKind.SAKURA_1_5B_Q4) != null,
        )

    suspend fun downloadMissingMangaOfflineModels(onProgress: (String) -> Unit) {
        val specs = mangaOfflineDownloadSpecs(mangaOfflineModelReadiness())
        if (specs.isNotEmpty()) {
            modelDownloadManager.enqueueIndependentlyAndAwait(specs, onProgress)
        }
    }
}

data class MangaOfflineModelReadiness(
    val mangaOcrReady: Boolean,
    val sakuraReady: Boolean,
) {
    val allReady: Boolean
        get() = mangaOcrReady && sakuraReady
}

internal fun mangaOfflineDownloadSpecs(
    readiness: MangaOfflineModelReadiness,
): List<ModelDownloadSpec> = buildList {
    if (!readiness.mangaOcrReady) add(ModelDownloadSpec.mangaOcr())
    if (!readiness.sakuraReady) {
        add(ModelDownloadSpec.llm(LlmModelKind.SAKURA_1_5B_Q4))
    }
}

internal fun paddleOcrDownloadSpecs(modelReady: Boolean): List<ModelDownloadSpec> =
    if (modelReady) {
        emptyList()
    } else {
        listOf(ModelDownloadSpec.paddle(PaddleModelVersion.V5_MOBILE))
    }
