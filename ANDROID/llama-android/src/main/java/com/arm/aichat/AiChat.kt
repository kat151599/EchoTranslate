package com.arm.aichat

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

/**
 * Compile-time compatibility shim for the upstream llama.android binding.
 *
 * This Remote-PC build intentionally has no on-device LLM runtime. If a legacy
 * local-LLM code path is invoked, methods fail with a clear exception instead
 * of trying to load native llama.cpp libraries.
 */
object AiChat {
    fun getInferenceEngine(context: Context): InferenceEngine = InferenceEngine()
}

class UnsupportedArchitectureException(message: String? = null) : RuntimeException(message)

class InferenceEngine {
    sealed class State {
        data object Initialized : State()
        data object ModelReady : State()
        data class Error(val message: String? = null) : State()
    }

    private val _state = MutableStateFlow<State>(State.Initialized)
    val state: StateFlow<State> = _state

    fun loadModel(path: String) {
        _state.value = State.Error(REMOTE_ONLY_MESSAGE)
        throw UnsupportedArchitectureException(REMOTE_ONLY_MESSAGE)
    }

    fun setSystemPrompt(prompt: String) = Unit

    fun sendUserPrompt(prompt: String, predictLength: Int): Flow<String> = flow {
        _state.value = State.Error(REMOTE_ONLY_MESSAGE)
        throw IllegalStateException(REMOTE_ONLY_MESSAGE)
    }

    fun cleanUp() {
        _state.value = State.Initialized
    }

    fun destroy() {
        _state.value = State.Initialized
    }

    companion object {
        private const val REMOTE_ONLY_MESSAGE =
            "On-device llama.cpp is disabled in the Remote PC build. Use PC server translation mode."
    }
}
