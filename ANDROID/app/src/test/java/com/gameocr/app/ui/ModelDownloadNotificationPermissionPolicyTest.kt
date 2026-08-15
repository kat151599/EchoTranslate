package com.gameocr.app.ui

import com.gameocr.app.download.shouldRequestModelDownloadNotificationPermission
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelDownloadNotificationPermissionPolicyTest {
    @Test
    fun shouldRequestModelDownloadNotificationPermission_isTableDriven() {
        data class Case(
            val name: String,
            val sdkInt: Int,
            val permissionGranted: Boolean,
            val expected: Boolean,
        )

        val cases = listOf(
            Case("Android 12 denied does not require runtime request", 32, false, false),
            Case("Android 13 denied requests permission", 33, false, true),
            Case("Android 15 denied requests permission", 35, false, true),
            Case("Android 13 granted continues directly", 33, true, false),
            Case("Android 15 granted continues directly", 35, true, false),
        )

        cases.forEach { case ->
            assertEquals(
                case.name,
                case.expected,
                shouldRequestModelDownloadNotificationPermission(
                    sdkInt = case.sdkInt,
                    permissionGranted = case.permissionGranted,
                ),
            )
        }
    }

    @Test
    fun modelDownloadEntryPoints_shareNotificationPermissionGate_tableDriven() {
        data class Case(
            val name: String,
            val sourcePath: String,
            val expectedMarker: String,
        )

        val cases = listOf(
            Case(
                "settings model downloads",
                "src/main/java/com/gameocr/app/ui/SettingsScreen.kt",
                "rememberModelDownloadNotificationPermissionGate()",
            ),
            Case(
                "onboarding PaddleOCR download",
                "src/main/java/com/gameocr/app/onboarding/OnboardingScreen.kt",
                "continueModelDownloadAfterNotificationPermission(::downloadPaddleOcrModel)",
            ),
            Case(
                "onboarding manga offline download",
                "src/main/java/com/gameocr/app/onboarding/OnboardingScreen.kt",
                "continueModelDownloadAfterNotificationPermission(::downloadMangaOfflineModels)",
            ),
        )

        cases.forEach { case ->
            val source = File(case.sourcePath).readText()
            assertTrue(
                "${case.name} should use the shared notification permission gate",
                case.expectedMarker in source,
            )
        }
    }
}
