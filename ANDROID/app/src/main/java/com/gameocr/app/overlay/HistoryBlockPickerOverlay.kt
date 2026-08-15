package com.gameocr.app.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class HistoryBlockPickerOverlay(private val context: Context) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var root: View? = null

    fun dismiss() {
        root?.let { runCatching { wm.removeView(it) } }
        root = null
    }

    fun show(items: List<OverlayManager.HistoryBlock>, onSelected: (OverlayManager.HistoryBlock) -> Unit) {
        dismiss()
        val density = context.resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()
        val list = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = GradientDrawable().apply {
                setColor(0xee202124.toInt())
                cornerRadius = dp(14).toFloat()
            }
        }
        items.forEach { item ->
            list.addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(10), dp(9), dp(10), dp(9))
                addView(TextView(context).apply {
                    text = item.source
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    maxLines = 2
                    ellipsize = TextUtils.TruncateAt.END
                })
                if (item.translation.isNotBlank()) addView(TextView(context).apply {
                    text = item.translation
                    setTextColor(0xffbdc1c6.toInt())
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                })
                setOnClickListener { dismiss(); onSelected(item) }
            }, LinearLayout.LayoutParams(dp(300), LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        val panel = ScrollView(context).apply {
            addView(list)
            setOnClickListener { dismiss() }
        }
        val container = FrameLayout(context).apply {
            setBackgroundColor(0x55000000)
            setOnClickListener { dismiss() }
            addView(panel, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, dp(320), Gravity.CENTER))
        }
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        wm.addView(container, WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, type,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ))
        root = container
    }
}
