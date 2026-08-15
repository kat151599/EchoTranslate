package com.gameocr.app.tts

import com.gameocr.app.data.Settings
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface TtsEngine {
    val playbackState: StateFlow<TtsPlaybackState>

    suspend fun speak(text: String, settings: Settings, playbackId: String = text)

    suspend fun toggle(text: String, settings: Settings, playbackId: String) {
        when (ttsPlaybackCommand(playbackState.value, playbackId)) {
            TtsPlaybackCommand.START -> speak(text, settings, playbackId)
            TtsPlaybackCommand.PAUSE -> pause()
            TtsPlaybackCommand.RESUME -> resume()
        }
    }

    fun pause()
    fun resume()
    fun stop()
}

/** LEGACY_COMPAT: remove after dependency cleanup. TTS is intentionally unavailable. */
@Singleton
class NoOpTtsEngine @Inject constructor() : TtsEngine {
    override val playbackState: StateFlow<TtsPlaybackState> = MutableStateFlow(TtsPlaybackState())
    override suspend fun speak(text: String, settings: Settings, playbackId: String) = Unit
    override fun pause() = Unit
    override fun resume() = Unit
    override fun stop() = Unit
}
