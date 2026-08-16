package com.gameocr.app.ui

import android.content.Context
import android.util.TypedValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Palette
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gameocr.app.R
import com.gameocr.app.data.FloatingSkill
import com.gameocr.app.data.MenuItemId
import com.gameocr.app.data.OverlayFontImportError
import com.gameocr.app.data.OverlayFontPolicy
import com.gameocr.app.data.OverlayFontEntry
import com.gameocr.app.data.OverlayTheme
import com.gameocr.app.data.OverlayTextAlignment
import com.gameocr.app.data.OverlayTextStyle
import com.gameocr.app.data.Settings
import com.gameocr.app.overlay.StyledTranslationTextView
import com.gameocr.app.overlay.MenuItemRegistry
import com.gameocr.app.overlay.applyOverlayTextStyle
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.height

/**
 * 译文样式实时预览卡。展示一段假的"原文 + 译文"，按当前 theme/字号/透明度/自定义色/边框/边框样式渲染。
 *
 * 与 [com.gameocr.app.overlay.OverlayManager] / [com.gameocr.app.overlay.DraggableOverlayWindow] 的视觉保持一致：
 * - 主题颜色映射见 [overlayThemeColors]（务必与 OverlayManager 同步）
 * - alpha 整体应用到 box（模拟 view.setAlpha 的效果，叠加自身像素 alpha）
 * - 棋盘格底色用 linear gradient 模拟实际屏幕背景，让透明度变化肉眼可见
 * - 边框样式：仅 CUSTOM 主题下读 [customBorderStyle]，预设主题恒为 SOLID（与 DraggableOverlayWindow 一致）；
 *   DASH/DOT 间距、DOUBLE 间隙、GROOVE 明暗各 ±40% 全部复制 OverlayManager / DraggableOverlayWindow 的硬编码
 */
internal fun overlayFontImportErrorMessage(
    context: android.content.Context,
    error: OverlayFontImportError
): String = context.getString(
    when (error) {
        OverlayFontImportError.UNSUPPORTED_EXTENSION -> R.string.settings_overlay_font_error_extension
        OverlayFontImportError.EMPTY_FILE -> R.string.settings_overlay_font_error_empty
        OverlayFontImportError.TOO_LARGE -> R.string.settings_overlay_font_error_too_large
        OverlayFontImportError.UNREADABLE -> R.string.settings_overlay_font_error_unreadable
        OverlayFontImportError.INVALID_FONT -> R.string.settings_overlay_font_error_invalid
        OverlayFontImportError.COPY_FAILED -> R.string.settings_overlay_font_error_copy_failed
    }
)

@Composable
internal fun OverlayTextStyleEditor(
    style: OverlayTextStyle,
    onChange: (OverlayTextStyle) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.settings_text_style_label), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StyleIconToggle(
                checked = style.bold,
                icon = Icons.Default.FormatBold,
                label = stringResource(R.string.settings_text_style_bold),
                onCheckedChange = { onChange(style.copy(bold = it)) }
            )
            StyleIconToggle(
                checked = style.italic,
                icon = Icons.Default.FormatItalic,
                label = stringResource(R.string.settings_text_style_italic),
                onCheckedChange = { onChange(style.copy(italic = it)) }
            )
            StyleIconToggle(
                checked = style.underline,
                icon = Icons.Default.FormatUnderlined,
                label = stringResource(R.string.settings_text_style_underline),
                onCheckedChange = { onChange(style.copy(underline = it)) }
            )
        }

        Text(
            stringResource(R.string.settings_letter_spacing_format, style.letterSpacingEm),
            style = MaterialTheme.typography.labelLarge
        )
        Slider(
            value = style.letterSpacingEm,
            onValueChange = { value ->
                onChange(style.copy(letterSpacingEm = (value * 100f).roundToInt() / 100f))
            },
            valueRange = OverlayTextStyle.MIN_LETTER_SPACING_EM..OverlayTextStyle.MAX_LETTER_SPACING_EM,
            steps = 19
        )

        Text(
            stringResource(R.string.settings_line_spacing_format, style.lineSpacingMultiplier),
            style = MaterialTheme.typography.labelLarge
        )
        Slider(
            value = style.lineSpacingMultiplier,
            onValueChange = { value ->
                onChange(style.copy(lineSpacingMultiplier = (value * 20f).roundToInt() / 20f))
            },
            valueRange = OverlayTextStyle.MIN_LINE_SPACING..OverlayTextStyle.MAX_LINE_SPACING,
            steps = 19
        )

        Text(stringResource(R.string.settings_text_alignment_label), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StyleIconToggle(
                checked = style.alignment == OverlayTextAlignment.START,
                icon = Icons.AutoMirrored.Filled.FormatAlignLeft,
                label = stringResource(R.string.settings_text_alignment_start),
                onCheckedChange = { if (it) onChange(style.copy(alignment = OverlayTextAlignment.START)) }
            )
            StyleIconToggle(
                checked = style.alignment == OverlayTextAlignment.CENTER,
                icon = Icons.Default.FormatAlignCenter,
                label = stringResource(R.string.settings_text_alignment_center),
                onCheckedChange = { if (it) onChange(style.copy(alignment = OverlayTextAlignment.CENTER)) }
            )
            StyleIconToggle(
                checked = style.alignment == OverlayTextAlignment.END,
                icon = Icons.AutoMirrored.Filled.FormatAlignRight,
                label = stringResource(R.string.settings_text_alignment_end),
                onCheckedChange = { if (it) onChange(style.copy(alignment = OverlayTextAlignment.END)) }
            )
        }

        SwitchRow(stringResource(R.string.settings_text_stroke_enabled), style.strokeEnabled) {
            onChange(style.copy(strokeEnabled = it))
        }
        if (style.strokeEnabled) {
            Text(
                stringResource(R.string.settings_text_stroke_width_format, style.strokeWidthDp),
                style = MaterialTheme.typography.labelLarge
            )
            Slider(
                value = style.strokeWidthDp,
                onValueChange = { value ->
                    onChange(style.copy(strokeWidthDp = (value * 2f).roundToInt() / 2f))
                },
                valueRange = OverlayTextStyle.MIN_STROKE_WIDTH_DP..OverlayTextStyle.MAX_STROKE_WIDTH_DP,
                steps = 10
            )
            VisualColorPickerRow(
                stringResource(R.string.settings_text_stroke_color),
                style.strokeColor
            ) { onChange(style.copy(strokeColor = it)) }
        }

        SwitchRow(stringResource(R.string.settings_text_shadow_enabled), style.shadowEnabled) {
            onChange(style.copy(shadowEnabled = it))
        }
        if (style.shadowEnabled) {
            Text(
                stringResource(R.string.settings_text_shadow_radius_format, style.shadowRadiusDp),
                style = MaterialTheme.typography.labelLarge
            )
            Slider(
                value = style.shadowRadiusDp,
                onValueChange = { onChange(style.copy(shadowRadiusDp = it.roundToInt().toFloat())) },
                valueRange = OverlayTextStyle.MIN_SHADOW_RADIUS_DP..OverlayTextStyle.MAX_SHADOW_RADIUS_DP,
                steps = 11
            )
            Text(
                stringResource(R.string.settings_text_shadow_offset_x_format, style.shadowOffsetXDp),
                style = MaterialTheme.typography.labelLarge
            )
            Slider(
                value = style.shadowOffsetXDp,
                onValueChange = { onChange(style.copy(shadowOffsetXDp = it.roundToInt().toFloat())) },
                valueRange = OverlayTextStyle.MIN_SHADOW_OFFSET_DP..OverlayTextStyle.MAX_SHADOW_OFFSET_DP,
                steps = 15
            )
            Text(
                stringResource(R.string.settings_text_shadow_offset_y_format, style.shadowOffsetYDp),
                style = MaterialTheme.typography.labelLarge
            )
            Slider(
                value = style.shadowOffsetYDp,
                onValueChange = { onChange(style.copy(shadowOffsetYDp = it.roundToInt().toFloat())) },
                valueRange = OverlayTextStyle.MIN_SHADOW_OFFSET_DP..OverlayTextStyle.MAX_SHADOW_OFFSET_DP,
                steps = 15
            )
            VisualColorPickerRow(
                stringResource(R.string.settings_text_shadow_color),
                style.shadowColor
            ) { onChange(style.copy(shadowColor = it)) }
        }

        TextButton(onClick = { onChange(OverlayTextStyle()) }) {
            Text(stringResource(R.string.settings_text_style_reset))
        }
    }
}

@Composable
private fun StyleIconToggle(
    checked: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onCheckedChange: (Boolean) -> Unit
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState()
    ) {
        IconToggleButton(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (checked) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                    RoundedCornerShape(4.dp)
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (checked) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
internal fun OverlayPreviewCard(
    theme: OverlayTheme,
    customBg: Int,
    customFg: Int,
    customBorder: Int,
    customBorderW: Float,
    customBorderStyle: com.gameocr.app.data.BorderStyle,
    textSize: Float,
    alpha: Float,
    overlayTypeface: android.graphics.Typeface?,
    textStyle: OverlayTextStyle
) {
    val colors = overlayThemeColors(theme, customBg, customFg, customBorder, customBorderW.toInt())
    // 仅 CUSTOM 主题 + borderDp > 0 时让用户选的 borderStyle 生效；与 DraggableOverlayWindow 一致
    val effectiveBorderStyle = if (theme == OverlayTheme.CUSTOM)
        customBorderStyle else com.gameocr.app.data.BorderStyle.SOLID
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(R.string.settings_overlay_preview_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF1F2937), Color(0xFF374151), Color(0xFF1F2937))
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(alpha)
                    .background(
                        Color(colors.bg),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .borderStyleOverlay(
                        borderDp = colors.borderDp,
                        borderColor = colors.border,
                        borderStyle = effectiveBorderStyle,
                        cornerRadiusDp = 6f
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                val previewText = stringResource(R.string.settings_overlay_preview_sample)
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    factory = { ctx ->
                        StyledTranslationTextView(ctx).apply {
                            setIncludeFontPadding(true)
                            gravity = android.view.Gravity.CENTER_VERTICAL
                        }
                    },
                    update = { view ->
                        view.text = previewText
                        view.setTextColor(colors.fg)
                        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
                        view.applyOverlayTextStyle(textStyle, overlayTypeface)
                    }
                )
            }
        }
    }
}

/**
 * 按 [borderStyle] 在 box 上画一圈边框，五种样式行为复制
 * [com.gameocr.app.overlay.DraggableOverlayWindow.shellBackground]：
 * - SOLID：单条 stroke
 * - DASHED：dashPathEffect(8dp on, 5dp off)
 * - DOTTED：dashPathEffect(2dp on, 3dp off)
 * - DOUBLE：外圈 + 内圈两条同色 stroke，间距 = w + 3dp
 * - GROOVE：外圈暗色 (-40%)、内圈亮色 (+40%)，inset = w
 *
 * borderDp <= 0 时直接 noop。
 */
private fun Modifier.borderStyleOverlay(
    borderDp: Int,
    borderColor: Int,
    borderStyle: com.gameocr.app.data.BorderStyle,
    cornerRadiusDp: Float
): Modifier = this.then(
    Modifier.drawBehind {
        if (borderDp <= 0) return@drawBehind
        val w = borderDp.dp.toPx()
        val cornerPx = cornerRadiusDp.dp.toPx()
        val color = Color(borderColor)
        // stroke 居中绘制，rect 往内 inset w/2 才能让外缘正好贴 box 边
        val inset = w / 2f
        val outerRect = androidx.compose.ui.geometry.Rect(
            left = inset,
            top = inset,
            right = size.width - inset,
            bottom = size.height - inset
        )
        val outerRadius = (cornerPx - inset).coerceAtLeast(0f)
        when (borderStyle) {
            com.gameocr.app.data.BorderStyle.SOLID -> drawRoundRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(outerRect.left, outerRect.top),
                size = androidx.compose.ui.geometry.Size(outerRect.width, outerRect.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(outerRadius),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = w)
            )
            com.gameocr.app.data.BorderStyle.DASHED -> drawRoundRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(outerRect.left, outerRect.top),
                size = androidx.compose.ui.geometry.Size(outerRect.width, outerRect.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(outerRadius),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = w,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(8.dp.toPx(), 5.dp.toPx())
                    )
                )
            )
            com.gameocr.app.data.BorderStyle.DOTTED -> drawRoundRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(outerRect.left, outerRect.top),
                size = androidx.compose.ui.geometry.Size(outerRect.width, outerRect.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(outerRadius),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = w,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(2.dp.toPx(), 3.dp.toPx())
                    )
                )
            )
            com.gameocr.app.data.BorderStyle.DOUBLE -> {
                // 外圈
                drawRoundRect(
                    color = color,
                    topLeft = androidx.compose.ui.geometry.Offset(outerRect.left, outerRect.top),
                    size = androidx.compose.ui.geometry.Size(outerRect.width, outerRect.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(outerRadius),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = w)
                )
                // 内圈：间距 = w + 3dp（与 LayerDrawable.setLayerInset 一致）
                val gap = w + 3.dp.toPx()
                val innerInset = inset + gap
                val innerRect = androidx.compose.ui.geometry.Rect(
                    left = innerInset, top = innerInset,
                    right = size.width - innerInset, bottom = size.height - innerInset
                )
                if (innerRect.width > 0f && innerRect.height > 0f) {
                    drawRoundRect(
                        color = color,
                        topLeft = androidx.compose.ui.geometry.Offset(innerRect.left, innerRect.top),
                        size = androidx.compose.ui.geometry.Size(innerRect.width, innerRect.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            (outerRadius - gap).coerceAtLeast(0f)
                        ),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w)
                    )
                }
            }
            com.gameocr.app.data.BorderStyle.GROOVE -> {
                // 外圈暗色
                drawRoundRect(
                    color = Color(shadeArgb(borderColor, -0.4f)),
                    topLeft = androidx.compose.ui.geometry.Offset(outerRect.left, outerRect.top),
                    size = androidx.compose.ui.geometry.Size(outerRect.width, outerRect.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(outerRadius),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = w)
                )
                // 内圈亮色，inset = w
                val innerInset = inset + w
                val innerRect = androidx.compose.ui.geometry.Rect(
                    left = innerInset, top = innerInset,
                    right = size.width - innerInset, bottom = size.height - innerInset
                )
                if (innerRect.width > 0f && innerRect.height > 0f) {
                    drawRoundRect(
                        color = Color(shadeArgb(borderColor, 0.4f)),
                        topLeft = androidx.compose.ui.geometry.Offset(innerRect.left, innerRect.top),
                        size = androidx.compose.ui.geometry.Size(innerRect.width, innerRect.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            (outerRadius - w).coerceAtLeast(0f)
                        ),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w)
                    )
                }
            }
        }
    }
)

/** 与 [com.gameocr.app.overlay.DraggableOverlayWindow.shadeColor] 行为一致。factor>0 加亮、<0 加暗。 */
private fun shadeArgb(color: Int, factor: Float): Int {
    val a = (color shr 24) and 0xFF
    val r = (color shr 16) and 0xFF
    val g = (color shr 8) and 0xFF
    val b = color and 0xFF
    val nr = if (factor >= 0) r + ((255 - r) * factor).toInt() else (r * (1 + factor)).toInt()
    val ng = if (factor >= 0) g + ((255 - g) * factor).toInt() else (g * (1 + factor)).toInt()
    val nb = if (factor >= 0) b + ((255 - b) * factor).toInt() else (b * (1 + factor)).toInt()
    return (a shl 24) or
        (nr.coerceIn(0, 255) shl 16) or
        (ng.coerceIn(0, 255) shl 8) or
        nb.coerceIn(0, 255)
}

/** 主题 → ARGB 颜色映射。与 [com.gameocr.app.overlay.OverlayManager] 内的硬编码必须保持一致。 */
private data class ThemeColors(val bg: Int, val fg: Int, val border: Int, val borderDp: Int)

private fun overlayThemeColors(
    theme: OverlayTheme,
    customBg: Int,
    customFg: Int,
    customBorder: Int,
    customBorderW: Int
): ThemeColors = when (theme) {
    OverlayTheme.CLASSIC_DARK ->
        ThemeColors(bg = 0xE6000000.toInt(), fg = 0xFFFFFFFF.toInt(), border = 0, borderDp = 0)
    OverlayTheme.AMBER_GOLD ->
        ThemeColors(bg = 0xF0241608.toInt(), fg = 0xFFFFD27F.toInt(), border = 0xFFB8860B.toInt(), borderDp = 2)
    OverlayTheme.PAPER_LIGHT ->
        ThemeColors(bg = 0xF0F5EFE0.toInt(), fg = 0xFF3E2A1F.toInt(), border = 0xFFB68850.toInt(), borderDp = 1)
    OverlayTheme.FROST_GLASS ->
        ThemeColors(bg = 0xCC1E293B.toInt(), fg = 0xFFE0F2FE.toInt(), border = 0xFF60A5FA.toInt(), borderDp = 1)
    OverlayTheme.CUSTOM ->
        ThemeColors(bg = customBg, fg = customFg, border = customBorder, borderDp = customBorderW.coerceAtLeast(0))
}

/** 搜索可用的 section key 常量。和 [SETTING_ITEMS] 的 sectionKey 对齐。 */
@Composable
internal fun CustomThemeEditor(
    bg: Int, onBgChange: (Int) -> Unit,
    fg: Int, onFgChange: (Int) -> Unit,
    border: Int, onBorderChange: (Int) -> Unit,
    borderW: Float, onBorderWChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        VisualColorPickerRow(stringResource(R.string.settings_custom_color_bg), bg, onBgChange)
        VisualColorPickerRow(stringResource(R.string.settings_custom_color_fg), fg, onFgChange)
        VisualColorPickerRow(stringResource(R.string.settings_custom_color_border), border, onBorderChange)
        Text(stringResource(R.string.settings_custom_color_border_w_format, borderW.toInt()), style = MaterialTheme.typography.labelLarge)
        Slider(value = borderW, onValueChange = onBorderWChange, valueRange = 0f..6f, steps = 5)
    }
}

@Composable
private fun VisualColorPickerRow(label: String, argb: Int, onChange: (Int) -> Unit) {
    var pickerOpen by remember { mutableStateOf(false) }
    var draft by remember(argb, pickerOpen) { mutableStateOf(VisualColorPickerState.fromArgb(argb)) }
    val chooseColorLabel = stringResource(R.string.settings_color_choose)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable(role = Role.Button) { pickerOpen = true }
            .padding(horizontal = 4.dp, vertical = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(
                chooseColorLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(argb), RoundedCornerShape(4.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
        )
        Icon(
            imageVector = Icons.Default.Palette,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 8.dp)
                .size(24.dp),
        )
    }

    if (pickerOpen) {
        VisualColorPickerDialog(
            label = label,
            state = draft,
            onStateChange = { draft = it.normalized() },
            onDismiss = { pickerOpen = false },
            onApply = {
                onChange(draft.toArgb())
                pickerOpen = false
            },
        )
    }
}

@Composable
private fun VisualColorPickerDialog(
    label: String,
    state: VisualColorPickerState,
    onStateChange: (VisualColorPickerState) -> Unit,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
) {
    val previewArgb = state.toArgb()
    val opacityLabel = stringResource(R.string.settings_color_opacity)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(16.dp),
        ) {
            val dialogBounds = visualColorDialogBounds(maxWidth.value, maxHeight.value)
            Surface(
                shape = RoundedCornerShape(8.dp),
                tonalElevation = 6.dp,
                modifier = Modifier
                    .width(dialogBounds.widthDp.dp)
                    .heightIn(max = dialogBounds.maxHeightDp.dp),
            ) {
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(
                            start = 24.dp,
                            top = 20.dp,
                            end = 24.dp,
                            bottom = 12.dp,
                        ),
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .background(Color(previewArgb), RoundedCornerShape(4.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                        )
                        SaturationValuePicker(
                            state = state,
                            label = stringResource(R.string.settings_color_visual_area),
                            stateText = stringResource(
                                R.string.settings_color_visual_state_format,
                                (state.saturation * 100f).roundToInt(),
                                (state.value * 100f).roundToInt(),
                            ),
                            increaseSaturationLabel = stringResource(R.string.settings_color_saturation_increase),
                            decreaseSaturationLabel = stringResource(R.string.settings_color_saturation_decrease),
                            increaseValueLabel = stringResource(R.string.settings_color_brightness_increase),
                            decreaseValueLabel = stringResource(R.string.settings_color_brightness_decrease),
                            onChange = onStateChange,
                        )
                        Text(
                            stringResource(R.string.settings_color_hue),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        VisualHueSlider(
                            hue = state.hue,
                            label = stringResource(R.string.settings_color_hue),
                            onChange = { onStateChange(state.copy(hue = it).normalized()) },
                        )
                        Text(
                            stringResource(
                                R.string.settings_color_opacity_format,
                                (state.alpha * 100f).roundToInt(),
                            ),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Slider(
                            value = state.alpha,
                            onValueChange = { onStateChange(state.copy(alpha = it).normalized()) },
                            valueRange = 0f..1f,
                            modifier = Modifier.semantics {
                                contentDescription = opacityLabel
                            },
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.settings_color_cancel))
                        }
                        TextButton(onClick = onApply) {
                            Text(stringResource(R.string.settings_color_apply))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SaturationValuePicker(
    state: VisualColorPickerState,
    label: String,
    stateText: String,
    increaseSaturationLabel: String,
    decreaseSaturationLabel: String,
    increaseValueLabel: String,
    decreaseValueLabel: String,
    onChange: (VisualColorPickerState) -> Unit,
) {
    val hueArgb = android.graphics.Color.HSVToColor(floatArrayOf(state.hue, 1f, 1f))
    val selectorArgb = android.graphics.Color.HSVToColor(
        floatArrayOf(state.hue, state.saturation, state.value)
    )
    fun updatePosition(x: Float, y: Float, width: Float, height: Float) {
        val selection = saturationValueFromPosition(x, y, width, height)
        onChange(
            state.copy(
                saturation = selection.saturation,
                value = selection.value,
            )
        )
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.45f)
            .clip(RoundedCornerShape(4.dp))
            .pointerInput(state.hue, state.alpha) {
                detectTapGestures { offset ->
                    updatePosition(
                        offset.x,
                        offset.y,
                        size.width.toFloat(),
                        size.height.toFloat(),
                    )
                }
            }
            .pointerInput(state.hue, state.alpha) {
                detectDragGestures(
                    onDragStart = { offset ->
                        updatePosition(
                            offset.x,
                            offset.y,
                            size.width.toFloat(),
                            size.height.toFloat(),
                        )
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        updatePosition(
                            change.position.x,
                            change.position.y,
                            size.width.toFloat(),
                            size.height.toFloat(),
                        )
                    },
                )
            }
            .semantics {
                contentDescription = label
                stateDescription = stateText
                customActions = listOf(
                    CustomAccessibilityAction(increaseSaturationLabel) {
                        onChange(state.copy(saturation = state.saturation + 0.05f).normalized())
                        true
                    },
                    CustomAccessibilityAction(decreaseSaturationLabel) {
                        onChange(state.copy(saturation = state.saturation - 0.05f).normalized())
                        true
                    },
                    CustomAccessibilityAction(increaseValueLabel) {
                        onChange(state.copy(value = state.value + 0.05f).normalized())
                        true
                    },
                    CustomAccessibilityAction(decreaseValueLabel) {
                        onChange(state.copy(value = state.value - 0.05f).normalized())
                        true
                    },
                )
            }
    ) {
        drawRect(
            brush = Brush.horizontalGradient(
                listOf(Color.White, Color(hueArgb))
            )
        )
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color.Transparent, Color.Black)
            )
        )
        val center = Offset(
            x = state.saturation * size.width,
            y = (1f - state.value) * size.height,
        )
        drawCircle(Color.Black, radius = 11.dp.toPx(), center = center)
        drawCircle(Color.White, radius = 8.dp.toPx(), center = center)
        drawCircle(Color(selectorArgb), radius = 5.dp.toPx(), center = center)
    }
}

@Composable
private fun VisualHueSlider(
    hue: Float,
    label: String,
    onChange: (Float) -> Unit,
) {
    val hueColors = listOf(
        Color(0xFFFF0000),
        Color(0xFFFFFF00),
        Color(0xFF00FF00),
        Color(0xFF00FFFF),
        Color(0xFF0000FF),
        Color(0xFFFF00FF),
        Color(0xFFFF0000),
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
        ) {
            drawRect(brush = Brush.horizontalGradient(hueColors))
        }
        Slider(
            value = hue,
            onValueChange = onChange,
            valueRange = 0f..VisualColorPickerState.MAX_HUE,
            colors = SliderDefaults.colors(
                thumbColor = Color(
                    android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
                ),
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = label },
        )
    }
}
@Composable
internal fun ArcMenuOrderEditor(
    order: List<MenuItemId>,
    currentSkill: com.gameocr.app.data.FloatingSkill,
    onReorder: (List<MenuItemId>) -> Unit
) {
    val itemHeight = 56.dp
    val itemSpacing = 6.dp
    // 一个槽 = item + 行间距，落位 / 让位都按这个步长算
    val slotHeightPx = with(LocalDensity.current) { (itemHeight + itemSpacing).toPx() }
    var draggedIdx by remember { mutableStateOf(-1) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    val targetIdx = if (draggedIdx < 0) -1
        else (draggedIdx + (dragOffsetY / slotHeightPx).roundToInt())
            .coerceIn(0, order.size - 1)

    Column(modifier = Modifier.fillMaxWidth()) {
        order.forEachIndexed { idx, id ->
            val isDragged = idx == draggedIdx
            // 让位规则：拖动项 idx → 目标 targetIdx，途经的项整体向反方向挪 1 个 slot
            val displacementPx = when {
                draggedIdx < 0 || isDragged -> 0f
                // 向下拖：原本在拖动项下方、且 ≤ targetIdx 的项要往上让一格
                draggedIdx < idx && idx <= targetIdx -> -slotHeightPx
                // 向上拖：原本在拖动项上方、且 ≥ targetIdx 的项要往下让一格
                draggedIdx > idx && idx >= targetIdx -> slotHeightPx
                else -> 0f
            }
            val animatedDisplacement by animateFloatAsState(
                targetValue = displacementPx,
                label = "arc_menu_displace_$idx"
            )
            val translation = if (isDragged) dragOffsetY else animatedDisplacement
            val bgColor = if (isDragged) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    // 被拖项浮到最上层，避免后绘制的 Row 把它盖住
                    .zIndex(if (isDragged) 1f else 0f)
                    .graphicsLayer { translationY = translation }
                    .background(bgColor, shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp)
                    .pointerInput(order, idx) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggedIdx = idx
                                dragOffsetY = 0f
                            },
                            onDrag = { _, drag ->
                                dragOffsetY += drag.y
                            },
                            onDragEnd = {
                                val slots = (dragOffsetY / slotHeightPx).roundToInt()
                                val target = (idx + slots).coerceIn(0, order.size - 1)
                                if (target != idx) {
                                    val next = order.toMutableList().apply {
                                        val moved = removeAt(idx)
                                        add(target, moved)
                                    }
                                    onReorder(next)
                                }
                                draggedIdx = -1
                                dragOffsetY = 0f
                            },
                            onDragCancel = {
                                draggedIdx = -1
                                dragOffsetY = 0f
                            }
                        )
                    }
            ) {
                // 左侧：三横杠拖动手柄（自绘 vector），明示「这一行可拖动」
                Icon(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.ic_drag_handle),
                    contentDescription = stringResource(R.string.settings_arc_menu_drag_handle),
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(modifier = Modifier.width(12.dp))
                // 中间：菜单项真实图标
                Icon(
                    painter = androidx.compose.ui.res.painterResource(menuItemIconRes(id, currentSkill)),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Box(modifier = Modifier.width(12.dp))
                Text(
                    menuItemLabel(id, currentSkill),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
            }
            Box(modifier = Modifier.height(itemSpacing))
        }
    }
}

internal class PendingModelDownload(
    val modelLabel: String,
    val warning: ModelDownloadNetworkWarning,
    val onConfirmed: () -> Unit,
)

@Composable
private fun menuItemLabel(id: MenuItemId, currentSkill: FloatingSkill): String = when (id) {
        MenuItemId.LOOP,
        MenuItemId.FULL_SCREEN_SKILL -> {
            val targetSkill = checkNotNull(MenuItemRegistry.targetSkill(id, currentSkill))
            if (targetSkill == FloatingSkill.LOOP) {
                stringResource(R.string.settings_arc_menu_item_loop)
            } else {
                val targetSkillName = stringResource(
                    when (targetSkill) {
                        FloatingSkill.FULL_SCREEN -> R.string.menu_full_screen_skill
                        FloatingSkill.WORD_SELECT -> R.string.menu_full_screen_skill
                        FloatingSkill.LOOP -> error("Handled above")
                    }
                )
                stringResource(R.string.settings_arc_menu_item_skill_format, targetSkillName)
            }
        }
        MenuItemId.REGION -> stringResource(R.string.settings_arc_menu_item_region)
        MenuItemId.LANGUAGE_PAIR -> stringResource(R.string.settings_arc_menu_item_language_pair)
        MenuItemId.PRESET_SWITCH -> stringResource(R.string.settings_arc_menu_item_preset)
        MenuItemId.SETTINGS -> stringResource(R.string.settings_arc_menu_item_settings)
        MenuItemId.HOME -> stringResource(R.string.settings_arc_menu_item_home)
        MenuItemId.RESTART_CAPTURE -> stringResource(R.string.menu_restart_capture)
    // 技能槽：跟弧菜单实际显示一致，文案 = 「切换主球操作 — <切换目标>」。
    // 未来加新 FloatingSkill 值时只需扩展 menuItemIconRes / 这里的 when，无需改文案模板。
}

private fun menuItemIconRes(id: MenuItemId, currentSkill: FloatingSkill): Int = when (id) {
    MenuItemId.LOOP,
    MenuItemId.FULL_SCREEN_SKILL -> when (checkNotNull(MenuItemRegistry.targetSkill(id, currentSkill))) {
        FloatingSkill.FULL_SCREEN -> R.drawable.ic_menu_full_screen
        FloatingSkill.WORD_SELECT -> R.drawable.ic_menu_full_screen
        FloatingSkill.LOOP -> R.drawable.ic_menu_loop
    }
        MenuItemId.REGION -> R.drawable.ic_menu_region
        MenuItemId.LANGUAGE_PAIR -> R.drawable.ic_menu_language_pair
        MenuItemId.PRESET_SWITCH -> R.drawable.ic_menu_preset
        MenuItemId.SETTINGS -> R.drawable.ic_menu_settings
        MenuItemId.HOME -> R.drawable.ic_menu_home
        MenuItemId.RESTART_CAPTURE -> R.drawable.ic_menu_restart
    // 技能槽预览要跟实际弧形菜单一致：图标表示“点击后切到的技能”。
}

/**
 * 模型名 → 描述 → 状态行 → 主按钮对（下载 / 本地导入）→ 镜像 URL → 删除按钮。
 * 与 OCR 两个 Section 唯一差异：不再单独印 "powered by" / license 文案——许可信息走关于页统一展示。
 */
@Composable
internal fun OverlayFontChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val bg = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val fg = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val border = if (selected) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.outline
    }
    Box(
        modifier = Modifier
            .height(36.dp)
            .widthIn(max = 180.dp)
            .background(bg, RoundedCornerShape(8.dp))
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .pointerInput(label, selected) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = fg,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
internal fun shouldShowOverlayFontDeleteTipBeforeImport(
    currentFileName: String,
    fonts: List<OverlayFontEntry>
): Boolean = currentFileName.isBlank() && OverlayFontPolicy.normalizeImportedFonts(fonts).isEmpty()

internal fun overlayFontDeleteTipAckLabel(
    baseLabel: String,
    countdown: Int
): String = if (countdown > 0) "($countdown) $baseLabel" else baseLabel
