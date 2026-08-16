package com.gameocr.app.overlay

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.view.WindowManager
import com.gameocr.app.R
import java.util.UUID

/** Small editor for a correction proposed against an already displayed Remote PC block. */
class HistoryCorrectionOverlay(private val context: Context) {
    private var dialog: AlertDialog? = null

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }

    fun show(
        block: OverlayManager.DisplayedTranslationBlock,
        onSubmit: (
            source: String,
            translation: String,
            clientRequestId: String,
            completed: (Result<Unit>) -> Unit,
        ) -> Unit,
    ) {
        dismiss()
        val density = context.resources.displayMetrics.density
        fun input(value: String) = EditText(context).apply {
            setText(value)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 2
            maxLines = 5
        }
        val source = input(block.source)
        val translation = input(block.translation)
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val margin = (20 * density).toInt()
            setPadding(margin, 0, margin, 0)
            addView(TextView(context).apply { setText(R.string.history_correction_source) })
            addView(source, LinearLayout.LayoutParams(-1, -2))
            addView(TextView(context).apply { setText(R.string.history_correction_translation) })
            addView(translation, LinearLayout.LayoutParams(-1, -2))
        }
        val requestId = UUID.randomUUID().toString()
        val host = AlertDialog.Builder(context)
            .setTitle(R.string.history_correction_title)
            .setView(content)
            .setNegativeButton(R.string.history_correction_cancel, null)
            .setPositiveButton(R.string.history_correction_submit, null)
            .create()
        host.window?.setType(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
        )
        host.setOnShowListener {
            val submit = host.getButton(AlertDialog.BUTTON_POSITIVE)
            submit.setOnClickListener {
                val editedSource = source.text.toString()
                val editedTranslation = translation.text.toString()
                if (editedSource == block.source && editedTranslation == block.translation) return@setOnClickListener
                submit.isEnabled = false
                onSubmit(editedSource, editedTranslation, requestId) { result ->
                    if (result.isSuccess) {
                        host.dismiss()
                    } else {
                        submit.isEnabled = true
                    }
                }
            }
        }
        host.setOnDismissListener { if (dialog === host) dialog = null }
        dialog = host
        host.show()
    }
}
