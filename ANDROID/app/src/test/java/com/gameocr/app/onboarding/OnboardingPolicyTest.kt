package com.gameocr.app.onboarding

import com.gameocr.app.data.MergeStrength
import com.gameocr.app.data.OverlayPlacement
import com.gameocr.app.data.OverlayStyleMode
import com.gameocr.app.data.RenderMode
import com.gameocr.app.data.Settings
import com.gameocr.app.data.TranslationOutputDirection
import com.gameocr.app.data.TranslationOutputLayout
import com.gameocr.app.data.TranslatorEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingPolicyTest {
    @Test
    fun steps_areTableDrivenByUsageAndTranslationMethod() {
        data class Case(
            val usage: OnboardingUsage,
            val method: OnboardingTranslationMethod,
            val expected: List<OnboardingStep>,
        )

        val commonStart = listOf(
            OnboardingStep.WELCOME,
            OnboardingStep.SOURCE_LANGUAGE,
            OnboardingStep.TARGET_LANGUAGE,
            OnboardingStep.USAGE,
        )
        val dailyStart = commonStart + OnboardingStep.DISPLAY_MODE
        val mangaStart = commonStart + OnboardingStep.MANGA_DIRECTION
        val cases = listOf(
            Case(
                OnboardingUsage.DAILY,
                OnboardingTranslationMethod.OFFLINE,
                dailyStart + OnboardingStep.TRANSLATION_METHOD + OnboardingStep.SUMMARY,
            ),
            Case(
                OnboardingUsage.DAILY,
                OnboardingTranslationMethod.CLOUD_LLM,
                dailyStart + OnboardingStep.TRANSLATION_METHOD + OnboardingStep.CLOUD_CONFIG + OnboardingStep.SUMMARY,
            ),
            Case(
                OnboardingUsage.MANGA,
                OnboardingTranslationMethod.OFFLINE,
                mangaStart + OnboardingStep.TRANSLATION_METHOD + OnboardingStep.SUMMARY,
            ),
            Case(
                OnboardingUsage.MANGA,
                OnboardingTranslationMethod.CLOUD_LLM,
                mangaStart + OnboardingStep.TRANSLATION_METHOD + OnboardingStep.CLOUD_CONFIG + OnboardingStep.SUMMARY,
            ),
        )

        cases.forEach { case ->
            assertEquals(
                "${case.usage}/${case.method}",
                case.expected,
                OnboardingPolicy.stepsFor(
                    OnboardingDraft(
                        usage = case.usage,
                        translationMethod = case.method,
                    )
                ),
            )
        }
    }

    @Test
    fun dailyDisplayModes_mapToExpectedSettings() {
        data class Case(
            val display: OnboardingDisplayMode,
            val renderMode: RenderMode,
            val styleMode: OverlayStyleMode,
            val placement: OverlayPlacement,
        )
        val cases = listOf(
            Case(
                OnboardingDisplayMode.ADAPTIVE_OVERLAY,
                RenderMode.BLOCKS,
                OverlayStyleMode.ADAPTIVE,
                OverlayPlacement.OVERLAP,
            ),
            Case(
                OnboardingDisplayMode.BELOW_SOURCE,
                RenderMode.BLOCKS,
                OverlayStyleMode.FIXED,
                OverlayPlacement.BELOW,
            ),
            Case(
                OnboardingDisplayMode.FLOATING_WINDOW,
                RenderMode.FLOATING_WINDOW,
                OverlayStyleMode.FIXED,
                OverlayPlacement.ABOVE,
            ),
        )

        cases.forEach { case ->
            val actual = OnboardingPolicy.apply(
                Settings(
                    overlayPlacement = OverlayPlacement.ABOVE,
                    mergeAdjacentBlocks = true,
                    translationOutputFollowRecognition = false,
                    translationOutputLayout = TranslationOutputLayout.VERTICAL,
                    translationOutputDirection = TranslationOutputDirection.RIGHT_TO_LEFT,
                ),
                OnboardingDraft(
                    usage = OnboardingUsage.DAILY,
                    displayMode = case.display,
                ),
            )
            assertEquals(case.display.name, case.renderMode, actual.renderMode)
            assertEquals(case.display.name, case.styleMode, actual.overlayStyleMode)
            assertEquals(case.display.name, case.placement, actual.overlayPlacement)
            assertEquals(false, actual.mergeAdjacentBlocks)
            assertTrue(actual.translationOutputFollowRecognition)
            assertEquals(
                TranslationOutputLayout.FOLLOW_RECOGNITION,
                actual.translationOutputLayout,
            )
            assertEquals(
                TranslationOutputDirection.FOLLOW_RECOGNITION,
                actual.translationOutputDirection,
            )
        }
    }

    @Test
    fun mangaDirections_forceAdaptiveStandardMangaBaseline() {
        data class Case(
            val direction: OnboardingMangaDirection,
            val follow: Boolean,
            val layout: TranslationOutputLayout,
            val outputDirection: TranslationOutputDirection,
        )
        val cases = listOf(
            Case(
                OnboardingMangaDirection.FOLLOW_RECOGNITION,
                true,
                TranslationOutputLayout.FOLLOW_RECOGNITION,
                TranslationOutputDirection.FOLLOW_RECOGNITION,
            ),
            Case(
                OnboardingMangaDirection.HORIZONTAL_LEFT_TO_RIGHT,
                false,
                TranslationOutputLayout.HORIZONTAL,
                TranslationOutputDirection.LEFT_TO_RIGHT,
            ),
            Case(
                OnboardingMangaDirection.VERTICAL_RIGHT_TO_LEFT,
                false,
                TranslationOutputLayout.VERTICAL,
                TranslationOutputDirection.RIGHT_TO_LEFT,
            ),
        )

        cases.forEach { case ->
            val actual = OnboardingPolicy.apply(
                Settings(),
                OnboardingDraft(
                    usage = OnboardingUsage.MANGA,
                    displayMode = OnboardingDisplayMode.FLOATING_WINDOW,
                    mangaDirection = case.direction,
                ),
            )
            assertEquals(RenderMode.BLOCKS, actual.renderMode)
            assertEquals(OverlayStyleMode.ADAPTIVE, actual.overlayStyleMode)
            assertEquals(OverlayPlacement.OVERLAP, actual.overlayPlacement)
            assertTrue(actual.mergeAdjacentBlocks)
            assertEquals(MergeStrength.STANDARD, actual.mergeStrength)
            assertEquals(case.follow, actual.translationOutputFollowRecognition)
            assertEquals(case.layout, actual.translationOutputLayout)
            assertEquals(case.outputDirection, actual.translationOutputDirection)
        }
    }

    @Test
    fun cloudProviderPresets_haveVerifiedNonBlankConfiguration() {
        data class Case(
            val provider: CloudProvider,
            val url: String,
            val model: String,
            val protocol: CloudApiProtocol,
        )
        val cases = listOf(
            Case(
                CloudProvider.DEEPSEEK,
                "https://api.deepseek.com/v1/",
                "deepseek-v4-flash",
                CloudApiProtocol.OPENAI,
            ),
            Case(
                CloudProvider.KIMI,
                "https://api.moonshot.cn/v1/",
                "kimi-k3",
                CloudApiProtocol.OPENAI,
            ),
            Case(
                CloudProvider.MINIMAX,
                "https://api.minimaxi.com/v1/",
                "MiniMax-M3",
                CloudApiProtocol.OPENAI,
            ),
            Case(
                CloudProvider.GLM,
                "https://open.bigmodel.cn/api/paas/v4/",
                "glm-5.2",
                CloudApiProtocol.OPENAI,
            ),
            Case(
                CloudProvider.MIMO,
                "https://api.xiaomimimo.com/v1/",
                "mimo-v2.5-pro",
                CloudApiProtocol.OPENAI,
            ),
            Case(
                CloudProvider.OPENAI,
                "https://api.openai.com/v1/",
                "gpt-4.1-mini",
                CloudApiProtocol.OPENAI,
            ),
            Case(
                CloudProvider.CLAUDE,
                "https://api.anthropic.com",
                "claude-sonnet-4-5",
                CloudApiProtocol.ANTHROPIC,
            ),
            Case(
                CloudProvider.GEMINI,
                "https://generativelanguage.googleapis.com/v1beta/openai/",
                "gemini-3.6-flash",
                CloudApiProtocol.OPENAI,
            ),
            Case(
                CloudProvider.CUSTOM,
                "",
                "",
                CloudApiProtocol.OPENAI,
            ),
        )

        assertEquals(CloudProvider.entries.size, cases.size)
        cases.forEach { case ->
            assertEquals(case.provider.name, case.url, case.provider.baseUrl)
            assertEquals(case.provider.name, case.model, case.provider.model)
            assertEquals(case.provider.name, case.protocol, case.provider.protocol)
        }
    }

    @Test
    fun cloudValidation_coversAllFieldFailuresAndSuccess() {
        data class Case(
            val url: String,
            val key: String,
            val model: String,
            val expected: CloudConfigError?,
        )
        val cases = listOf(
            Case("", "key", "model", CloudConfigError.BASE_URL_REQUIRED),
            Case("not a url", "key", "model", CloudConfigError.BASE_URL_INVALID),
            Case("ftp://example.com", "key", "model", CloudConfigError.BASE_URL_INVALID),
            Case("https://example.com/v1", "", "model", CloudConfigError.API_KEY_REQUIRED),
            Case("https://example.com/v1", "key", "", CloudConfigError.MODEL_REQUIRED),
            Case("https://example.com/v1", "key", "model", null),
        )

        cases.forEach { case ->
            assertEquals(
                case.url,
                case.expected,
                OnboardingPolicy.cloudConfigError(
                    OnboardingDraft(
                        cloudBaseUrl = case.url,
                        cloudApiKey = case.key,
                        cloudModel = case.model,
                    )
                ),
            )
        }
    }

    @Test
    fun translationMethods_mapToCurrentTranslatorEngine() {
        // OFFLINE -> GOOGLE_ML_KIT by default
        val offline = OnboardingPolicy.apply(
            Settings(),
            OnboardingDraft(translationMethod = OnboardingTranslationMethod.OFFLINE),
        )
        assertEquals(TranslatorEngine.GOOGLE_ML_KIT, offline.translatorEngine)

        // MANGA + OFFLINE -> LOCAL_SAKURA
        val mangaOffline = OnboardingPolicy.apply(
            Settings(),
            OnboardingDraft(usage = OnboardingUsage.MANGA, translationMethod = OnboardingTranslationMethod.OFFLINE),
        )
        assertEquals(TranslatorEngine.LOCAL_SAKURA, mangaOffline.translatorEngine)

        // CLOUD_LLM + provider protocol OPENAI -> OPENAI engine
        val openAi = OnboardingPolicy.apply(
            Settings(),
            OnboardingDraft(
                translationMethod = OnboardingTranslationMethod.CLOUD_LLM,
                cloudProvider = CloudProvider.GEMINI,
            ),
        )
        assertEquals(TranslatorEngine.OPENAI, openAi.translatorEngine)

        // CLOUD_LLM + provider protocol ANTHROPIC -> ANTHROPIC engine
        val anthropic = OnboardingPolicy.apply(
            Settings(),
            OnboardingDraft(
                translationMethod = OnboardingTranslationMethod.CLOUD_LLM,
                cloudProvider = CloudProvider.CLAUDE,
            ),
        )
        assertEquals(TranslatorEngine.ANTHROPIC, anthropic.translatorEngine)
    }

    @Test
    fun mangaOfflinePairSupport_isTableDriven() {
        data class Case(val source: String, val target: String, val supported: Boolean)
        val cases = listOf(
            Case("ja", "zh-CN", true),
            Case("ja", "en", false),
            Case("en", "zh-CN", false),
            Case("ja", "zh-TW", false),
        )

        cases.forEach { case ->
            assertEquals(
                "${case.source}->${case.target}",
                case.supported,
                OnboardingPolicy.isSakuraPairSupported(case.source, case.target),
            )
        }
    }

    @Test
    fun validCloudConfigurationReturnsNoError() {
        assertNull(
            OnboardingPolicy.cloudConfigError(
                OnboardingDraft(
                    cloudBaseUrl = CloudProvider.DEEPSEEK.baseUrl,
                    cloudApiKey = "secret",
                    cloudModel = CloudProvider.DEEPSEEK.model,
                )
            )
        )
    }
}
