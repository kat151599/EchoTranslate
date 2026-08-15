package com.gameocr.app.overlay

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import com.gameocr.app.R

internal fun TextView.enableSelectionCorrection(
    correctionLabel: String? = null,
    correctionAction: () -> (() -> Unit)? = { null },
) {
    customSelectionActionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            correctionLabel?.let { actionLabel ->
                menu.add(Menu.NONE, R.id.action_correct_translation, 101, actionLabel).apply {
                    setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                    isVisible = selectedText() != null && correctionAction() != null
                }
            }
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            menu.findItem(R.id.action_correct_translation)?.isVisible =
                selectedText() != null && correctionAction() != null
            return true
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.action_correct_translation -> {
                    val action = selectedText()?.let { correctionAction() } ?: return false
                    mode.finish()
                    action()
                    true
                }

                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode) = Unit
    }
}

private fun TextView.selectedText(): String? =
    selectedTextForSpeech(text, selectionStart, selectionEnd)
