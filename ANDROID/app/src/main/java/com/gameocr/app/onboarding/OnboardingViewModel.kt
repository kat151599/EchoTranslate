package com.gameocr.app.onboarding

import androidx.lifecycle.ViewModel
import com.gameocr.app.data.SettingsRepository
import com.gameocr.app.translate.RoutingTranslator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val routingTranslator: RoutingTranslator,
) : ViewModel() {
    suspend fun loadDraft(firstRun: Boolean): OnboardingDraft =
        if (firstRun) OnboardingDraft()
        else OnboardingPolicy.fromSettings(settingsRepository.get())

    suspend fun save(draft: OnboardingDraft) {
        settingsRepository.update { current -> OnboardingPolicy.apply(current, draft) }
    }

}
