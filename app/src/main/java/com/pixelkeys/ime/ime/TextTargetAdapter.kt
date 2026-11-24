package com.pixelkeys.ime.ime

import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.runtime.MutableState
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

interface TextTargetAdapter {
    fun commitText(text: String)
    fun setComposingText(text: String)
    fun finishComposingText()
    fun deleteBackward(count: Int = 1)
    fun moveCursor(delta: Int)
    fun performEditorAction()
}

class InputConnectionTargetAdapter(
    private val inputConnectionProvider: () -> InputConnection?,
    private val editorInfoProvider: () -> EditorInfo?
) : TextTargetAdapter {

    override fun commitText(text: String) {
        inputConnectionProvider()?.commitText(text, 1)
    }

    override fun setComposingText(text: String) {
        inputConnectionProvider()?.setComposingText(text, 1)
    }

    override fun finishComposingText() {
        inputConnectionProvider()?.finishComposingText()
    }

    override fun deleteBackward(count: Int) {
        val ic = inputConnectionProvider() ?: return
        val selected = ic.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            ic.commitText("", 1)
        } else {
            ic.deleteSurroundingText(count, 0)
        }
    }

    override fun moveCursor(delta: Int) {
        val ic = inputConnectionProvider() ?: return
        val keyCode = if (delta < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
        val steps = kotlin.math.abs(delta)
        repeat(steps) {
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        }
    }

    override fun performEditorAction() {
        val ic = inputConnectionProvider() ?: return
        val actionId = (editorInfoProvider()?.imeOptions ?: 0) and EditorInfo.IME_MASK_ACTION
        if (actionId != EditorInfo.IME_ACTION_NONE && actionId != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(actionId)
        } else {
            ic.commitText("\n", 1)
        }
    }
}

class PlaygroundTargetAdapter(
    private val state: MutableState<TextFieldValue>
) : TextTargetAdapter {

    private var composingStart: Int? = null
    private var composingEnd: Int? = null

    override fun commitText(text: String) {
        val current = state.value
        val full = current.text
        val start = composingStart ?: current.selection.min
        val end = composingEnd ?: current.selection.max
        val safeStart = start.coerceIn(0, full.length)
        val safeEnd = end.coerceIn(safeStart, full.length)

        val updated = full.substring(0, safeStart) + text + full.substring(safeEnd)
        val newCursor = safeStart + text.length
        state.value = TextFieldValue(text = updated, selection = TextRange(newCursor))
        composingStart = null
        composingEnd = null
    }

    override fun setComposingText(text: String) {
        val current = state.value
        val full = current.text
        val start = composingStart ?: current.selection.min
        val end = composingEnd ?: current.selection.max
        val safeStart = start.coerceIn(0, full.length)
        val safeEnd = end.coerceIn(safeStart, full.length)

        val updated = full.substring(0, safeStart) + text + full.substring(safeEnd)
        val newEnd = safeStart + text.length
        state.value = TextFieldValue(text = updated, selection = TextRange(newEnd))
        composingStart = safeStart
        composingEnd = newEnd
    }

    override fun finishComposingText() {
        composingStart = null
        composingEnd = null
    }

    override fun deleteBackward(count: Int) {
        composingStart = null
        composingEnd = null
        val current = state.value
        val full = current.text
        val selMin = current.selection.min.coerceIn(0, full.length)
        val selMax = current.selection.max.coerceIn(selMin, full.length)

        if (selMin != selMax) {
            val updated = full.substring(0, selMin) + full.substring(selMax)
            state.value = TextFieldValue(text = updated, selection = TextRange(selMin))
        } else if (selMin > 0) {
            val deleteFrom = (selMin - count).coerceAtLeast(0)
            val updated = full.substring(0, deleteFrom) + full.substring(selMin)
            state.value = TextFieldValue(text = updated, selection = TextRange(deleteFrom))
        }
    }

    override fun moveCursor(delta: Int) {
        composingStart = null
        composingEnd = null
        val current = state.value
        val newPos = (current.selection.end + delta).coerceIn(0, current.text.length)
        state.value = current.copy(selection = TextRange(newPos))
    }

    override fun performEditorAction() {
        commitText("\n")
    }
}
