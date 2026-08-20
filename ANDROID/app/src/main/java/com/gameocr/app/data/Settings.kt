package com.gameocr.app.data

import androidx.annotation.StringRes
import com.gameocr.app.R
import com.gameocr.app.capture.CaptureRegion
import kotlinx.serialization.Serializable
import java.security.MessageDigest

/** 用户配置：capture、render 与 Remote PC 翻译相关。 */
@Serializable
data class Settings(
    /** BCP-47 源语言代码（如 "auto"/"ja"/"zh-CN"）。从全部 [Languages.ALL] 中选取。 */
    val sourceLang: String = Languages.AUTO.code,
    val targetLang: String = "zh-CN",
    val promptTemplate: String = DEFAULT_PROMPT,
    val captureLoopIntervalMs: Long = 2000L,
    val loopTriggerMode: LoopTriggerMode = LoopTriggerMode.WAIT_FOR_TEXT_COMPLETE,
    val loopTextStableDurationMs: Long = DEFAULT_LOOP_TEXT_STABLE_DURATION_MS,
    val loopSkipSimilarFrames: Boolean = true,
    val loopFrameSimilarityThreshold: Float = 0.95f,
    val loopTextRegionMode: LoopTextRegionMode = LoopTextRegionMode.AUTO,
    val loopTranslateRegionOnly: Boolean = true,
    val developerOptionsEnabled: Boolean = false,
    val disableTranslationCache: Boolean = false,
    val batchCumulativeCompletionTimeEnabled: Boolean = false,
    /** 兼容旧版持久化字段；界面以正向的“跨上下文翻译”开关展示。false 表示默认开启。 */
    val disableCrossLineContextTranslation: Boolean = false,
    val captureRegion: CaptureRegion? = null,
    /**
     * 保存 [captureRegion] 时的屏幕物理尺寸（px）。用于读取 region 时按当前屏幕尺寸自动 rescale，
     * 避免用户竖屏框完一区域，旋转横屏后 region 坐标错位。0 = 历史数据（没记录），跳过 rescale。
     */
    val captureRegionSavedScreenW: Int = 0,
    val captureRegionSavedScreenH: Int = 0,
    val overlayStyleMode: OverlayStyleMode = OverlayStyleMode.FIXED,
    val overlayTextSizeSp: Int = 14,
    val overlayTextStyle: OverlayTextStyle = OverlayTextStyle(),
    val overlayAlpha: Float = 0.85f,
    val overlayFontFileName: String = "",
    val overlayFontDisplayName: String = "",
    val overlayFonts: List<OverlayFontEntry> = emptyList(),
    val streamingTranslate: Boolean = true,
    val retryEmptyTranslation: Boolean = false,
    val renderMode: RenderMode = RenderMode.BLOCKS,
    val translationBlockInteractionMode: TranslationBlockInteractionMode =
        TranslationBlockInteractionMode.COPY_BUTTON,
    val overlayPlacement: OverlayPlacement = OverlayPlacement.OVERLAP,
    val overlayTheme: OverlayTheme = OverlayTheme.CLASSIC_DARK,
    /** CUSTOM 主题用：ARGB int，比如 0xE6000000.toInt() 半透明黑。 */
    val customBgColor: Int = 0xE6000000.toInt(),
    val customFgColor: Int = 0xFFFFFFFF.toInt(),
    val customBorderColor: Int = 0x00000000,
    /** 边框粗细（dp，0=无边）。 */
    val customBorderWidth: Int = 0,
    /** 译文相对原文 boundingBox 的水平偏移（px，负数=往左，正数=往右）。 */
    val overlayOffsetX: Int = 0,
    /** 译文相对原文 boundingBox 的垂直额外偏移（px，叠加到 placement 计算结果之上）。 */
    val overlayOffsetY: Int = 0,
    /**
     * OCR 调用前 / 后自动判别文本方向（横排 / 竖排 / 一字母一行的 logo），按判别结果在路由层
     * 动态切换 OCR 引擎（比如发现是日漫竖排自动切到 manga-ocr，发现是繁中竖排自动切到百度
     * 含位置版）。方向模型已随 APK 打包，默认开启以覆盖竖排 / 旋转屏译场景。
     *
     * Phase 1 实现为"OCR 后判别 → 不匹配则用更合适的引擎重跑"——横排场景零额外开销，竖排
     * 误用其它引擎时 OCR 跑 2 次。详见 [com.gameocr.app.ocr.OrientationCoordinator]。
     */
    /**
     * 用户手动锁定文本方向，覆盖自动判别。null = 走自动 / 关闭时无意义。
     * 通常仅在自动判别频繁误判某帧时由用户临时锁定。
     */
    val translationOutputFollowRecognition: Boolean = true,
    val translationOutputLayout: TranslationOutputLayout = TranslationOutputLayout.FOLLOW_RECOGNITION,
    val translationOutputDirection: TranslationOutputDirection = TranslationOutputDirection.FOLLOW_RECOGNITION,
    /** 百度 OCR 接口类型。默认含位置标准版，能让译文紧贴原文 boundingBox 渲染。 */
    /**
     * 百度 OCR 识别语种。默认 CHN_ENG（中英）等于不指定时的行为。
     * 注意：含位置版（general / accurate / webimage）实际不读取 language_type；
     * 想识别韩文 / 日文等小语种应当切到「标准版」或「高精度版」（无位置）。
    */
    /** Umi-OCR HTTP image OCR endpoint, e.g. http://192.168.0.2:1224/api/ocr. */
    /** LunaTranslator HTTP image OCR endpoint, e.g. http://192.168.0.2:2333/api/ocr. */
    /** 腾讯云 OCR 接口类型。三种选择各自有独立配额、价格、识别能力。 */
    /**
     * 腾讯云 OCR 识别语种。默认 auto 由后端按图片内容判断，多数场景体验最好。
     * GeneralAccurateOCR 只支持 auto / zh，RecognizeAgent 不读这个字段（引擎层会跳过）。
     */
    /**
     * manga-ocr 模型下载镜像 URL（可选）。l0wgear/manga-ocr-2025-onnx 没有公开 hf-mirror 代理
     * （实测 308 redirect 回 huggingface.co），用户可填自架镜像（如内网 NAS）。空 = 仅走 huggingface.co 原站。
     */
    /**
     * Uses verified bubble shapes for delayed text erasure, local repair, and translated layout.
     * Low-confidence regions always fall back to the existing adaptive rectangle renderer.
     */
    /** PaddleOCR doc-orientation ONNX model mirror. Empty = official HuggingFace source. */
    val preferShizukuCapture: Boolean = false,
    val a11yVolumeTrigger: Boolean = false,
    val translatorEngine: TranslatorEngine = TranslatorEngine.REMOTE_PC,
    /** Remote PC end-to-end pipeline. Android sends the captured image; PC performs OCR, context management and translation. */
    val remotePcBaseUrl: String = "http://192.168.1.100:8765",
    val remotePcApiKey: String = "",
    val remotePcSessionId: String = "default",
    val remotePcImageQuality: Int = 85,
    val translationGlossaryEnabled: Boolean = true,
    val foregroundAppDetectionMode: ForegroundAppDetectionMode = ForegroundAppDetectionMode.AUTO,
    val sendAppNameToTranslator: Boolean = false,
    /** 火山引擎机器翻译 AccessKey ID（SignV4 鉴权用）。 */
    val volcAccessKeyId: String = "",
    val volcSecretAccessKey: String = "",
    /** 火山引擎区域，国内默认 cn-north-1（目前火山翻译只开放这一个区域）。 */
    val volcRegion: String = "cn-north-1",
    /** 百度翻译开放平台 APPID（fanyi-api.baidu.com，**不是**百度智能云 OCR 那套）。 */
    val baiduFanyiAppId: String = "",
    /** 百度翻译开放平台密钥，签名用 md5(appid+q+salt+key)。 */
    val baiduFanyiSecretKey: String = "",
    /** 悬浮按钮直径（dp）。 */
    val floatingButtonSizeDp: Int = 40,
    /**
     * 悬浮按钮 X 坐标（px，gravity=TOP|START 参考左上角）。-1 表示未保存过，按代码默认值
     * `(16dp, screenH/4)` 初始化。松手吸边后由 [FloatingButtonManager] 写回。
     */
    val floatingButtonX: Int = -1,
    val floatingButtonY: Int = -1,
    /** 松手是否自动吸附最近边（贴边时 1/3 藏出屏外 + 半透明待机）。关时松手停在原位。 */
    val floatingButtonSnapToEdge: Boolean = true,
    /**
     * 长按菜单关闭 / 操作完悬浮按钮后，若 3 秒未再次触摸则自动吸附最近边。
     * 仅在 [floatingButtonSnapToEdge] 也开启时生效。默认关，避免吓到老用户。
     */
    val floatingButtonAutoDock: Boolean = false,
    /**
     * 吸附时距实际屏幕物理边的内偏移（dp，0–40）。0 = 紧贴系统边；> 0 时让出 inset 宽度，
     * 用来避开全面屏左右边手势触发区。
     */
    val floatingButtonDockInsetDp: Int = 0,
    /**
     * 悬浮窗口（[RenderMode.FLOATING_WINDOW]）位置 / 大小。-1 表示首次未保存过 → 居中并使用默认尺寸。
     * 拖动 / 缩放后由 [overlay.DraggableOverlayWindow] 写回。
     */
    val floatingWindowX: Int = -1,
    val floatingWindowY: Int = -1,
    val floatingWindowWidthDp: Int = 320,
    val floatingWindowHeightDp: Int = 180,
    /** 悬浮窗口内容形态：原文+译文 / 仅译文。 */
    val floatingWindowContentMode: FloatingWindowContentMode = FloatingWindowContentMode.SRC_AND_DST,
    /** 锁定悬浮窗口位置/大小：true 时不可拖拽 / 不可缩放（避免游戏中误触）。 */
    val floatingWindowLocked: Boolean = false,
    /**
     * 自定义主题的边框样式（仿 CSS border-style）。仅在 [overlayTheme] = CUSTOM 时生效，
     * 对 BLOCKS 模式 box + FLOATING_WINDOW 模式的悬浮窗都生效。0.3.x 字段名 floatingWindowBorderStyle
     * 已被 silent-migrate 到这里。
     */
    val customBorderStyle: BorderStyle = BorderStyle.SOLID,
    /** 译文允许换行（关闭后强制单行，可能横向溢出但更紧凑）。 */
    val overlayAllowWrap: Boolean = true,
    /** 启用碰撞检测：上下左右四个方向都避免遮挡其它原文 box。 */
    val overlayAvoidCollision: Boolean = true,
    /**
     * API 请求超时（秒），同时作用于 OCR（百度 / 腾讯）和翻译（OpenAI / DeepL）。
     * connect/read/write/call 都用这个值（call 是总超时上限）。
     * 模型下载（PaddleOCR 模型 ~20MB）不受这个限制，走默认 60s 的下载 client。
     */
    val apiTimeoutSeconds: Int = 30,
    /**
     * OCR 后合并相邻 box：把同一行内左右邻接的小 box 合并成一个，文本用空格拼接，
     * box 取 union。漫画 / 字幕场景百度等引擎经常把一句话拆成多段，开启后能让译文
     * 不再分裂成多个互相重叠的小框。默认关，按需在设置里开启。
     *
     * 阈值由 [mergeStrength] 选择：保守 / 标准 / 激进。
     */
    val mergeAdjacentBlocks: Boolean = false,
    /** 合并相邻 box 的强度档位，仅在 [mergeAdjacentBlocks] = true 时生效。 */
    val mergeStrength: MergeStrength = MergeStrength.STANDARD,
    /**
     * 用户在 LanguagePicker 里星标过的语言代码，按收藏顺序保存。
     * 列表里在最前，源语言 / 目标语言两个选择器共享同一份。
     */
    val pinnedLanguages: List<String> = emptyList(),
    /** ML Kit 端侧翻译最近使用的源语言，按最近使用顺序保存，最多四个。 */
    /**
     * 明文 HTTP 白名单 host 列表（仅 hostname / IP，不含 scheme / port / path）。
     * 默认严格模式仅放行私有/回环地址；这里追加的 host 也允许明文访问，用于无 HTTPS 的可信外网服务。
     * **安全提示**：明文可被中间人窃听/篡改，仅在你确认链路可信时启用。
     */
    val cleartextAllowedHosts: List<String> = emptyList(),
    /**
     * 悬浮球长按弧菜单按钮顺序。每页按钮数由 [arcMenuPageSize] 决定，范围为
     * [FloatingMenu.MIN_PAGE_SIZE]..[FloatingMenu.MAX_PAGE_SIZE]；超出时由 FloatingButtonManager
     * 自动在每页末位插入「下一组」翻页项，最后一页循环回第一页。新装用户 / 未自定义的旧默认顺序迁移到
     * [FloatingMenu.DEFAULT_ORDER]。
     *
     * `LOOP` 与 `FULL_SCREEN_SKILL` 共同构成两个稳定的模式切换槽；展开菜单时分别显示
     * 另外两种主球模式。order 无需迁移到三个新 ID，旧配置仍可直接读取。
     */
    val floatingMenuItemOrder: List<MenuItemId> = FloatingMenu.DEFAULT_ORDER,
    val arcMenuPageSize: Int = FloatingMenu.DEFAULT_PAGE_SIZE,
    val floatingButtonSkill: FloatingSkill = FloatingSkill.FULL_SCREEN,
    /**
     * 划词翻译：单词模式专用的 LLM 词典 prompt 模板（仅 OpenAI 兼容引擎生效）。
     * 用占位符 `{source}` / `{target}` 同 [promptTemplate]。返回 JSON 让卡片显示音标 / 词性 /
     * 释义 / 难点解释 / 例句；解析失败回退到 [promptTemplate]。读取时若 key 缺省，按 UI locale 给出本地化默认。
     */
    val dictionaryPrompt: String = DEFAULT_DICTIONARY_PROMPT,
    /**
     * PaddleOCR / MangaOCR 共用 DBNet 检测的二值化阈值。prob map > 此值视为前景。
     * 主线 PaddleOCR 默认 0.3；屏译降到 0.25 让漫画小气泡、淡色字、长竖排能稳定捕获。
     * 用户可在设置→OCR→"检测高级阈值"调到 0.15–0.4，过低引入噪声 box / 过高漏小字。
     */
    /**
     * DBNet 连通域平均概率阈值。连通域内像素的 prob 均值低于此值视为噪声丢弃。
     * 主线默认 0.6；屏译降到 0.5 配合 [dbnetProbThresh] 一起放宽，捕获概率响应在边界的小字。
     */
    /**
     * PaddleOCR DBNet 旋转矩形外扩比例。从二值连通域到最终 box 的 unclip 操作，
     * 越大 box 包得越宽。普通 PaddleOCR 保持 1.55，避免为了日漫 crop 需求改变通用 OCR 行为。
     */
    /**
     * manga-ocr 专用 DBNet 外扩比例。manga-ocr 识别整气泡 crop，竖排/手绘字体更怕首尾字被裁；
     * 1.65 比 PaddleOCR 常见 1.5 默认值多一点裁剪余量，同时仍避免过度吞邻泡。
     */
    val translationPresets: List<TranslationPreset> = emptyList(),
    val activeTranslationPresetId: String = "",
    @kotlinx.serialization.Transient
    val runtimeTranslationContext: String = "",
    /**
     * Request-scoped glossary/memory override. null resolves the foreground app as before;
     * an empty string explicitly selects global glossary entries and disables app memory.
     */
    @kotlinx.serialization.Transient
    val runtimeTranslationScopePackage: String? = null,
    @kotlinx.serialization.Transient
    val runtimeTranslationScopeLabel: String = "",
) {
    companion object {
        const val DEFAULT_LOOP_TEXT_STABLE_DURATION_MS: Long = 500L

        /**
         * 默认 prompt 用占位符 `{source}` / `{target}`，运行时替换为当前 source/target 语言名称。
         * 这样用户在设置里改语言 chip 后无需重写 prompt。
         *
         * 注意：本常量仅作为 [Settings.promptTemplate] 的兜底默认值，跟随中文（i18n 后 prompt 仍按
         * 中文 prompt 工作良好——多数 LLM 对中文 prompt 同样理解输出指定语言）。UI 里"恢复默认
         * prompt"按钮也用此值。如果将来要做 prompt 本地化，把这里改成根据 context 读 R.string.default_prompt。
         */
        const val DEFAULT_PROMPT: String = """你是一名专业的译者，把下面的{source}原文翻译成{target}。要求：
1. 保留人名、地名等专有名词；
2. 自然流畅，避免直译腔；
3. 只输出译文，不加解释、不加引号。
原文：
"""

        /**
         * 划词翻译的词典模式默认 prompt。要求 LLM 在输入是单词时返回严格 JSON——
         * 解析失败由 CaptureService 回退到纯翻译，不报错；解析成功则把 phonetic / pos /
         * definitions / inflections / synonyms / examples 显示在卡片字典区。
         */
        const val DEFAULT_DICTIONARY_PROMPT: String = """你是一名{source}→{target}的双语词典助手。请把用户输入当作一个单词或固定短语来处理，**只输出**下面格式的 JSON，不要加 markdown、代码块、解释。
{
  "phonetic": "音标或读音（{source}; 无则空串）",
  "pos": ["词性，{target}缩写，如 名/动/形 或 n./v./adj.; 无则空数组"],
  "definitions": ["{target}释义 1", "{target}释义 2"],
  "inflections": ["词形标签: {source}词形，如过去式、过去分词、复数、比较级或适用的变位；无则空数组"],
  "synonyms": ["{source}常用同义词或近义词；无则空数组"],
  "difficulty_notes": ["用{target}解释生僻含义、专业领域、缩写全称或易混淆用法；普通词为空数组"],
  "examples": [
    { "src": "{source}例句", "dst": "{target}译文" }
  ]
}
要求：
1. 必须是合法 JSON，键名与上面完全一致；
2. 没有信息的字段用空串或空数组占位；
3. 词形变化最多 6 项、同义词最多 5 项、例句最多 2 条；
4. 生僻词、专业名词、缩写、文化专名或易混淆用法必须给出难点解释，最多 3 条，不要重复释义；普通词用空数组；
5. 不要把整段当句子翻译，只做词典查询。
"""
    }
}

@Serializable
data class OverlayFontEntry(
    val fileName: String,
    val displayName: String
)

@Serializable
data class TranslationPreset(
    val id: String,
    val name: String,
    val shortName: String = name.take(8),
    val sourceLang: String = Languages.AUTO.code,
    val targetLang: String = "zh-CN",
    val promptTemplate: String = Settings.DEFAULT_PROMPT,
    val dictionaryPrompt: String = Settings.DEFAULT_DICTIONARY_PROMPT,
    val renderMode: RenderMode = RenderMode.BLOCKS,
    val translationBlockInteractionMode: TranslationBlockInteractionMode =
        TranslationBlockInteractionMode.COPY_BUTTON,
    val overlayPlacement: OverlayPlacement = OverlayPlacement.OVERLAP,
    val overlayStyleMode: OverlayStyleMode = OverlayStyleMode.FIXED,
    val overlayTheme: OverlayTheme = OverlayTheme.CLASSIC_DARK,
    val customBgColor: Int = 0xE6000000.toInt(),
    val customFgColor: Int = 0xFFFFFFFF.toInt(),
    val customBorderColor: Int = 0x00000000,
    val customBorderWidth: Int = 0,
    val customBorderStyle: BorderStyle = BorderStyle.SOLID,
    val overlayTextSizeSp: Int = 14,
    val overlayTextStyle: OverlayTextStyle = OverlayTextStyle(),
    val overlayAlpha: Float = 0.85f,
    val overlayFontFileName: String = "",
    val overlayFontDisplayName: String = "",
    val overlayOffsetX: Int = 0,
    val overlayOffsetY: Int = 0,
    val overlayAllowWrap: Boolean = true,
    val overlayAvoidCollision: Boolean = true,
    val streamingTranslate: Boolean = true,
    val retryEmptyTranslation: Boolean = false,
    val translatorEngine: TranslatorEngine = TranslatorEngine.REMOTE_PC,
    val apiTimeoutSeconds: Int = 30,
    val mergeAdjacentBlocks: Boolean = false,
    val mergeStrength: MergeStrength = MergeStrength.STANDARD,
    val translationOutputFollowRecognition: Boolean = true,
    val translationOutputLayout: TranslationOutputLayout = TranslationOutputLayout.FOLLOW_RECOGNITION,
    val translationOutputDirection: TranslationOutputDirection = TranslationOutputDirection.FOLLOW_RECOGNITION,
    val translationGlossaryEnabled: Boolean = true,
    val sendAppNameToTranslator: Boolean = false,
    val settingsHash: String = ""
) {
    fun applyTo(settings: Settings): Settings {
        val output = resolveTranslationOutputSettings(
            translationOutputFollowRecognition,
            translationOutputLayout,
            translationOutputDirection,
        )
        return settings.copy(
        sourceLang = sourceLang,
        targetLang = targetLang,
        promptTemplate = promptTemplate,
        dictionaryPrompt = dictionaryPrompt,
        renderMode = renderMode,
        translationBlockInteractionMode = translationBlockInteractionMode,
        overlayPlacement = overlayPlacement,
        overlayStyleMode = overlayStyleMode,
        overlayTheme = overlayTheme,
        customBgColor = customBgColor,
        customFgColor = customFgColor,
        customBorderColor = customBorderColor,
        customBorderWidth = customBorderWidth,
        customBorderStyle = customBorderStyle,
        overlayTextSizeSp = overlayTextSizeSp,
        overlayTextStyle = overlayTextStyle.normalized(),
        overlayAlpha = overlayAlpha,
        overlayFontFileName = overlayFontFileName,
        overlayFontDisplayName = overlayFontDisplayName,
        overlayOffsetX = overlayOffsetX,
        overlayOffsetY = overlayOffsetY,
        overlayAllowWrap = overlayAllowWrap,
        overlayAvoidCollision = overlayAvoidCollision,
        streamingTranslate = streamingTranslate,
        retryEmptyTranslation = retryEmptyTranslation,
        // LEGACY_COMPAT: presets may contain a retired engine, but always apply Remote PC.
        translatorEngine = TranslatorEngine.REMOTE_PC,
        apiTimeoutSeconds = apiTimeoutSeconds,
        mergeAdjacentBlocks = mergeAdjacentBlocks,
        mergeStrength = mergeStrength,
        translationOutputFollowRecognition = output.followRecognition,
        translationOutputLayout = output.layout,
        translationOutputDirection = output.direction,
        translationGlossaryEnabled = translationGlossaryEnabled,
        sendAppNameToTranslator = sendAppNameToTranslator,
        )
    }
}

object TranslationPresetCatalog {
    const val UNSAVED_DRAFT_ID: String = "custom_unsaved_translation_preset"

    fun builtIns(): List<TranslationPreset> = emptyList()

    fun all(custom: List<TranslationPreset>): List<TranslationPreset> =
        builtIns() + custom.filterNot { it.id in builtInIds }

    fun find(custom: List<TranslationPreset>, id: String): TranslationPreset? =
        all(custom).firstOrNull { it.id == id }

    fun fromSettings(
        id: String,
        name: String,
        shortName: String,
        settings: Settings
    ): TranslationPreset {
        val output = resolveTranslationOutputSettings(
            settings.translationOutputFollowRecognition,
            settings.translationOutputLayout,
            settings.translationOutputDirection,
        )
        val preset = TranslationPreset(
            id = id,
            name = name,
            shortName = shortName,
            sourceLang = settings.sourceLang,
            targetLang = settings.targetLang,
            promptTemplate = settings.promptTemplate,
            dictionaryPrompt = settings.dictionaryPrompt,
            renderMode = settings.renderMode,
            translationBlockInteractionMode = settings.translationBlockInteractionMode,
            overlayPlacement = settings.overlayPlacement,
            overlayStyleMode = settings.overlayStyleMode,
            overlayTheme = settings.overlayTheme,
            customBgColor = settings.customBgColor,
            customFgColor = settings.customFgColor,
            customBorderColor = settings.customBorderColor,
            customBorderWidth = settings.customBorderWidth,
            customBorderStyle = settings.customBorderStyle,
            overlayTextSizeSp = settings.overlayTextSizeSp,
            overlayTextStyle = settings.overlayTextStyle.normalized(),
            overlayAlpha = settings.overlayAlpha,
            overlayFontFileName = settings.overlayFontFileName,
            overlayFontDisplayName = settings.overlayFontDisplayName,
            overlayOffsetX = settings.overlayOffsetX,
            overlayOffsetY = settings.overlayOffsetY,
            overlayAllowWrap = settings.overlayAllowWrap,
            overlayAvoidCollision = settings.overlayAvoidCollision,
            streamingTranslate = settings.streamingTranslate,
            retryEmptyTranslation = settings.retryEmptyTranslation,
            translatorEngine = TranslatorEngine.REMOTE_PC,
            apiTimeoutSeconds = settings.apiTimeoutSeconds,
            mergeAdjacentBlocks = settings.mergeAdjacentBlocks,
            mergeStrength = settings.mergeStrength,
            translationOutputFollowRecognition = output.followRecognition,
            translationOutputLayout = output.layout,
            translationOutputDirection = output.direction,
            translationGlossaryEnabled = settings.translationGlossaryEnabled,
            sendAppNameToTranslator = settings.sendAppNameToTranslator,
        )
        return preset.copy(settingsHash = settingsHash(preset))
    }

    fun matchesSettings(preset: TranslationPreset, settings: Settings): Boolean {
        return matchesHash(preset, hashForSettings(settings))
    }

    fun hashForSettings(settings: Settings): String = fromSettings(
        id = UNSAVED_DRAFT_ID,
        name = "",
        shortName = "",
        settings = settings
    ).settingsHash

    fun matchesHash(preset: TranslationPreset, settingsHash: String): Boolean =
        preset.settingsHash == settingsHash || settingsHash(preset) == settingsHash

    private fun settingsHash(preset: TranslationPreset): String {
        val textStyle = preset.overlayTextStyle.normalized()
        val output = resolveTranslationOutputSettings(
            preset.translationOutputFollowRecognition,
            preset.translationOutputLayout,
            preset.translationOutputDirection,
        )
        return sha256(
            preset.sourceLang,
            preset.targetLang,
            preset.promptTemplate,
            preset.dictionaryPrompt,
            preset.renderMode.name,
            preset.translationBlockInteractionMode.name,
            preset.overlayPlacement.name,
            preset.overlayStyleMode.name,
            preset.overlayTheme.name,
            preset.customBgColor,
            preset.customFgColor,
            preset.customBorderColor,
            preset.customBorderWidth,
            preset.customBorderStyle.name,
            preset.overlayTextSizeSp,
            textStyle.bold,
            textStyle.italic,
            textStyle.underline,
            textStyle.letterSpacingEm.toBits(),
            textStyle.lineSpacingMultiplier.toBits(),
            textStyle.alignment.name,
            textStyle.strokeEnabled,
            textStyle.strokeWidthDp.toBits(),
            textStyle.strokeColor,
            textStyle.shadowEnabled,
            textStyle.shadowRadiusDp.toBits(),
            textStyle.shadowOffsetXDp.toBits(),
            textStyle.shadowOffsetYDp.toBits(),
            textStyle.shadowColor,
            preset.overlayAlpha.toBits(),
            preset.overlayFontFileName,
            preset.overlayFontDisplayName,
            preset.overlayOffsetX,
            preset.overlayOffsetY,
            preset.overlayAllowWrap,
            preset.overlayAvoidCollision,
            preset.streamingTranslate,
            preset.retryEmptyTranslation,
            preset.translatorEngine.name,
            preset.apiTimeoutSeconds,
            preset.mergeAdjacentBlocks,
            preset.mergeStrength.name,
            output.followRecognition,
            output.layout.name,
            output.direction.name,
            preset.translationGlossaryEnabled,
            preset.sendAppNameToTranslator,
        )
    }

    private fun sha256(vararg parts: Any?): String {
        val source = buildString {
            parts.forEach { part ->
                val value = part?.toString().orEmpty()
                append(value.length)
                append(':')
                append(value)
                append('|')
            }
        }
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(source.toByteArray(Charsets.UTF_8))
        return buildString(bytes.size * 2) {
            bytes.forEach { byte ->
                val value = byte.toInt() and 0xff
                append("0123456789abcdef"[value ushr 4])
                append("0123456789abcdef"[value and 0x0f])
            }
        }
    }

    fun upsertCustom(
        custom: List<TranslationPreset>,
        preset: TranslationPreset
    ): List<TranslationPreset> {
        val cleaned = custom.filterNot { it.id == preset.id || it.id in builtInIds }
        return if (preset.id in builtInIds) cleaned else cleaned + preset
    }

    fun isBuiltIn(id: String): Boolean = id in builtInIds

    private val builtInIds: Set<String> = emptySet()
}

/**
 * 主球单击技能。FULL_SCREEN 走 CaptureService.triggerOnce()（全屏 OCR+翻译）；
 * Legacy persisted value falls back to full-screen translation.
 */
@Serializable
enum class FloatingSkill {
    FULL_SCREEN,
    WORD_SELECT,
    LOOP,
}

/**
 * 悬浮球弧菜单按钮 ID。在 `overlay/MenuItemRegistry.kt` 集中绑定到图标 / 文案 / 回调。
 *
 * `LOOP` 与 `FULL_SCREEN_SKILL` 是两个稳定的模式槽位。registry 根据当前 [FloatingSkill]
 * 将它们映射为另外两种模式；这样旧版菜单顺序无需迁移到三个新 ID。
 */
@Serializable
enum class MenuItemId {
    LOOP,
    REGION,
    LANGUAGE_PAIR,
    PRESET_SWITCH,
    SETTINGS,
    HOME,
    RESTART_CAPTURE,
    FULL_SCREEN_SKILL
}

/** 弧菜单分页 / 默认顺序常量。 */
object FloatingMenu {
    /** 每页按钮数范围；该数量包含「下一组」翻页键。 */
    const val MIN_PAGE_SIZE: Int = 2
    const val MAX_PAGE_SIZE: Int = 6
    const val DEFAULT_PAGE_SIZE: Int = 5
    /** 旧调用点兼容别名，等同 [DEFAULT_PAGE_SIZE]。 */
    const val PAGE_SIZE: Int = DEFAULT_PAGE_SIZE

    fun coercePageSize(value: Int): Int = value.coerceIn(MIN_PAGE_SIZE, MAX_PAGE_SIZE)

    /** IDs currently exposed in the Arc Menu. Kept separate from [MenuItemId] for old DataStore values. */
    val ARC_MENU_ITEM_IDS: List<MenuItemId> = listOf(
        MenuItemId.LOOP,
        MenuItemId.REGION,
        MenuItemId.RESTART_CAPTURE,
    )

    val ALL_ORDER: List<MenuItemId> = ARC_MENU_ITEM_IDS

    /** Default Arc Menu order for a new installation. */
    val DEFAULT_ORDER: List<MenuItemId> = listOf(
        MenuItemId.LOOP,
        MenuItemId.REGION,
        MenuItemId.RESTART_CAPTURE,
    )

    val LEGACY_DEFAULT_ORDER_BEFORE_PRESET_LANGUAGE_SWAP: List<MenuItemId> = listOf(
        MenuItemId.LOOP,
        MenuItemId.REGION,
        MenuItemId.FULL_SCREEN_SKILL,
        MenuItemId.PRESET_SWITCH,
        MenuItemId.LANGUAGE_PAIR,
        MenuItemId.SETTINGS,
        MenuItemId.HOME
    )

    val LEGACY_DEFAULT_ORDER_BEFORE_PRESET_SKILL_SWAP: List<MenuItemId> = listOf(
        MenuItemId.LOOP,
        MenuItemId.REGION,
        MenuItemId.PRESET_SWITCH,
        MenuItemId.FULL_SCREEN_SKILL,
        MenuItemId.LANGUAGE_PAIR,
        MenuItemId.SETTINGS,
        MenuItemId.HOME
    )

    val LEGACY_DEFAULT_ORDER_BEFORE_SKILL_SWAP: List<MenuItemId> = listOf(
        MenuItemId.LOOP,
        MenuItemId.REGION,
        MenuItemId.PRESET_SWITCH,
        MenuItemId.SETTINGS,
        MenuItemId.LANGUAGE_PAIR,
        MenuItemId.FULL_SCREEN_SKILL,
        MenuItemId.HOME
    )
}

/**
 * OCR 合并相邻 box 的强度档位。从保守到激进——保守宁可让 OCR 输出散一些不误合，
 * 激进容忍更大间距 / 行高差，适合漫画气泡内多行被切碎的情形。
 */
@Serializable
enum class MergeStrength {
    /** 漫画 / 字幕短句：宽松阈值（gap 1.8x、垂直 1.3x、相交 15%），最容易合，可能误合相邻气泡。 */
    AGGRESSIVE,
    /** 默认：当前调优好的中间值（gap 1.2x、垂直 0.8x、相交 30%）。 */
    STANDARD,
    /** 视觉小说 / 长段密集场景：严格阈值（gap 0.8x、垂直 0.5x、相交 50%），少误合但段落易拆开。 */
    CONSERVATIVE
}

@Serializable
enum class DeeplProtocol {
    /**
     * DeepL 官方 v2/translate 协议：`Authorization: DeepL-Auth-Key`，body 是 form-urlencoded
     * (`text=...&target_lang=...`)，响应 `{translations:[{text,...}]}`。
     */
    OFFICIAL,
    /**
     * deeplx 协议（OwO-Network/DeepLX 及其常见 fork）：body 是 JSON
     * (`{text, source_lang, target_lang}`)，响应 `{code, data, ...}`，不支持 batch。
     */
    DEEPLX,
    /**
     * 混合：先用 deeplx 翻译，若 deeplx 失败 / 返回空，则用 DeepL 官方 key 补译。
     * 需要 deeplx Base URL（必填）+ DeepL 官方 API Key（用作 fallback）同时配置。
     */
    AUTO
}

@Serializable
enum class TranslatorEngine {
    /** Thin-client mode: screenshot -> PC server -> PaddleOCR/context/LLM -> translated blocks. */
    REMOTE_PC,
    /** OpenAI 兼容 LLM（DeepSeek / SiliconFlow / GPT / 自架 Ollama 等）。 */
    OPENAI,
    /** Anthropic Messages API 兼容 LLM（官方 Claude / 标准兼容网关）。 */
    ANTHROPIC,
    /** DeepL 翻译 API（专业翻译质量，对日/英/中等 30+ 语言对）。 */
    DEEPL,
    /**
     * 有道智云图片翻译（ocrtransapi）。**端到端引擎**：传整张截图，直接拿回带 box 的译文，
     */
    YOUDAO_PICTRANS,
    /**
     * Google 翻译（非官方端点，无需 key）。谷歌可能随时限流 / 改端点 / 拒绝。国内需代理。
     */
    GOOGLE,
    /**
     * 端侧翻译的内部路由值，UI 仅显示拉丁 / 中文 / 日文 / 韩文四个源语言入口。
     * 底层使用 Google ML Kit，要求明确的 sourceLang；首次实际翻译时按需下载模型。
     */
    GOOGLE_ML_KIT,
    /**
     * 火山引擎机器翻译（open.volcengineapi.com）。原生支持 TextList 批量；走 Volcengine SignV4
     * 鉴权（service=translate / region=cn-north-1）。需要在火山控制台开通"机器翻译"并拿 AK/SK。
     */
    VOLC,
    /**
     * 百度翻译开放平台（fanyi-api.baidu.com）。**与 [Settings.baiduOcrApiKey] 完全不是同一个产品**
     * （那是百度智能云 OCR）。签名简单：md5(appid+q+salt+key)；个人免费档 1QPS / 5万字符/月。
     */
    BAIDU_FANYI,
    /**
     * 腾讯云翻译 TMT（tmt.tencentcloudapi.com）。**复用 [Settings.tencentSecretId] /
     * [Settings.tencentSecretKey] / [Settings.tencentRegion]** 同一套腾讯云子账号——
     * 因为属于同一个腾讯云账号体系，让用户填两遍只会困惑。
     */
    TENCENT,
    /**
     * 端侧 LLM 翻译 —— SakuraLLM Qwen2.5-1.5B Q5KS（约 1.26 GB），日译中 ACGN 专用。
     * 走 [com.gameocr.app.llm.LlamaEngineHolder] + llama.cpp（com.arm.aichat binding）。
     * 仅 Android 13+ 可用（binding minSdk=33）。模型按需下载，5 分钟空闲自动 unload。
     * 选中后强制目标语种为简体中文；源语种非日文时 RoutingTranslator 回退到 OpenAI 兼容引擎。
     *
     * 历史：曾同时支持 LOCAL_HUNYUAN_MT（HY-MT1.5 1.25bit/2bit GGUF），但腾讯 AngelSlim 的
     * STQ1_0 / Q2_0c 量化都依赖未合入主线的 llama.cpp PR（#22836 / #19357），主线 master
     * 无法加载，已从枚举里移除避免误导用户。旧 settings 里残留的 "LOCAL_HUNYUAN_MT" 字符串
     * 由 SettingsRepository.toSettings() 的 runCatching{...}.getOrDefault(OPENAI) 兜底。
     */
    LOCAL_SAKURA,
    /**
     * 端侧 LLM 翻译 —— Tencent Hy-MT2-1.8B Q4_K_M（约 1.13 GB），多语种翻译专用。
     * 走 [com.gameocr.app.llm.LlamaEngineHolder] + llama.cpp（com.arm.aichat binding）。
     * 仅 Android 13+ 可用；语言方向跟随 [sourceLang] / [targetLang]。
     */
    LOCAL_HY_MT2
}

/** 常用目标语言预设（也允许 settings.targetLang 自由填）。 */
object TargetLangPresets {
    val ALL: List<Pair<String, String>> = listOf(
        "中文（简体）" to "zh-CN",
        "中文（繁体）" to "zh-TW",
        "English" to "en",
        "日本語" to "ja",
        "한국어" to "ko"
    )
}

@Serializable
enum class LoopTriggerMode {
    FIXED_INTERVAL,
    WAIT_FOR_TEXT_COMPLETE,
}

@Serializable
enum class LoopTextRegionMode {
    AUTO,
    LOWER_SCREEN_FIRST,
    ANYWHERE,
}

@Serializable
enum class RenderMode {
    BLOCKS,
    FLOATING_WINDOW,
}

@Serializable
enum class TranslationOutputLayout {
    FOLLOW_RECOGNITION,
    HORIZONTAL,
    VERTICAL,
}

@Serializable
enum class TranslationOutputDirection {
    FOLLOW_RECOGNITION,
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT,
}

@Serializable
enum class ForegroundAppDetectionMode {
    AUTO,
    ACCESSIBILITY,
    USAGE_ACCESS,
    DISABLED,
}

/** [RenderMode.BLOCKS] 下译文块提供复制能力的交互方式。 */
@Serializable
enum class TranslationBlockInteractionMode {
    /** 长按译文块使用 Android 原生文本选择。 */
    COPY_BUTTON,
    /** 点击译文块打开可选择局部文字、也可整段复制的结果浮层。 */
    OPEN_COPY_PANEL,
}

/** 悬浮窗口（[RenderMode.FLOATING_WINDOW]）的内容形态。 */
@Serializable
enum class FloatingWindowContentMode {
    /** 每段「原文 + 译文」上下排列。 */
    SRC_AND_DST,
    /** 仅显示译文，段间用分隔线。更紧凑。 */
    DST_ONLY
}

/**
 * 悬浮窗口边框样式。SOLID 是默认（跟 CSS `border-style: solid` 等价）。
 * 仅在主题本身有 stroke 时生效（AMBER_GOLD / PAPER_LIGHT / FROST_GLASS / CUSTOM with width>0）；
 * CLASSIC_DARK 默认无边，选啥样式都不画。
 */
@Serializable
enum class BorderStyle {
    SOLID,
    DASHED,
    DOTTED,
    DOUBLE,
    GROOVE
}

@Serializable
enum class OverlayPlacement {
    /** 紧贴原文下方，不遮挡原文（默认）。 */
    BELOW,
    /** 覆盖在原文上方，彻底替换显示。 */
    OVERLAP,
    /** 紧贴原文上方（适合下方有 UI 元素时）。 */
    ABOVE
}

@Serializable
enum class OverlayStyleMode {
    /** Use the colors, font size and effects configured by the user. */
    FIXED,
    /** Derive block colors and a safe maximum font size from the captured image. */
    ADAPTIVE
}

internal fun adaptiveOverlayActive(
    mode: OverlayStyleMode,
    renderMode: RenderMode,
): Boolean =
    mode == OverlayStyleMode.ADAPTIVE && renderMode == RenderMode.BLOCKS

internal fun manualOverlayLayoutControlsEnabled(
    mode: OverlayStyleMode,
    renderMode: RenderMode,
): Boolean =
    !adaptiveOverlayActive(mode, renderMode)

internal fun Settings.effectiveOverlayRenderSettings(): Settings =
    if (adaptiveOverlayActive(overlayStyleMode, renderMode)) {
        copy(
            overlayTextSizeSp = 14,
            overlayTextStyle = OverlayTextStyle(),
            overlayAlpha = 1f,
            overlayTheme = OverlayTheme.CLASSIC_DARK,
            overlayPlacement = OverlayPlacement.OVERLAP,
            overlayOffsetX = 0,
            overlayOffsetY = 0,
            overlayAllowWrap = true,
            overlayAvoidCollision = false,
        )
    } else {
        this
    }

@Serializable
enum class OverlayTheme {
    /** 经典深色：黑底白字。 */
    CLASSIC_DARK,
    /** 琥珀黑金：深棕底 + 暖金字（galgame 老派对话框感）。 */
    AMBER_GOLD,
    /** 浅色纸张：米色底 + 深褐字（漫画译文风）。 */
    PAPER_LIGHT,
    /** 半透明霜玻璃：蓝灰底 + 浅蓝字。 */
    FROST_GLASS,
    /** 自定义：bg/fg/border/border 粗细全由用户设置。 */
    CUSTOM
}


