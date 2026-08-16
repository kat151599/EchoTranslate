# EchoTranslate Android rules

## Thin-client scope

Keep and protect: Remote PC translation; MediaProjection / Shizuku / ASB; Floating Ball; Arc Menu; translation overlay; foreground-app detection; `history_id`; correction API and correction UI; Remote PC, capture, overlay, and language settings.

Remove only in explicit tasks: TTS; Word Select; every translation engine except Remote PC; Local LLM / llama-android; Gallery Translation; local Glossary; Translation Memory; then local OCR once no remaining legacy feature uses it.

## Scope and patches

- Start narrow: use `rg` for the exact symbol, resource, endpoint, or class named by the task; open only direct matches.
- Do not inspect the whole project, large Kotlin trees, or all tests without a task-specific reason.
- Make the smallest patch. Do not reformat, refactor adjacent code, or change architecture outside the request.
- For tests, search only by the changed symbol/class/resource; update only directly related tests when an API changes.

## Protected behavior

Do not change without an explicit request: Remote PC translation, `/v1/screen/translate`, capture backends, Floating Ball, Arc Menu, translation overlay, foreground-app detection, `history_id`, correction API, or app localization.

## Server authority

The Windows Remote PC server is the source of truth for OCR, translation, LLM, history/context, and glossary behavior. Do not add Android-local equivalents.

## Resources

When removing a feature, remove only resources/settings proven unused with `rg`. Preserve referenced resources and migration/default settings unless the task explicitly changes them.

## Build rule

Never run Gradle, Kotlin compilation, Gradle-backed tests, or APK builds unless the user explicitly asks. The user builds APKs manually in Android Studio.

## Response

End with only: a brief result, changed/deleted files, and any discovered dependency or risk.
