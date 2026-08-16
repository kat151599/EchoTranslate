package com.gameocr.app.overlay

data class TranslationCorrectionRequest(
    val observedSource: String,
    val translation: String,
)

data class TranslationCorrectionDraft(
    val observedSource: String,
    val correctedSource: String,
    val correctedTranslation: String,
    val rememberTranslation: Boolean,
    val glossary: TranslationCorrectionGlossaryDraft?,
)

data class TranslationCorrectionGlossaryDraft(
    val sourceTerm: String,
    val targetTerm: String,
)

internal fun isTranslationCorrectionActionAvailable(
    isFinal: Boolean,
    source: String?,
    translation: String?,
): Boolean =
    isFinal &&
        !source.isNullOrBlank() &&
        isTranslationBlockTextActionable(translation)
