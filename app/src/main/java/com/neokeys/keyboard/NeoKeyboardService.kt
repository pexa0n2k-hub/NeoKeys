package com.neokeys.keyboard

import android.inputmethodservice.InputMethodService
import android.view.View

class NeoKeyboardService : InputMethodService() {
    private var keyboard: NeoKeyboardView? = null
    override fun onCreateInputView(): View {
        keyboard = NeoKeyboardView(this) { action ->
            val ic = currentInputConnection ?: return@NeoKeyboardView
            when (action) {
                "DELETE" -> ic.deleteSurroundingText(1, 0)
                "SPACE" -> ic.commitText(" ", 1)
                "ENTER" -> ic.commitText("\n", 1)
                "LANG" -> keyboard?.toggleLanguage()
                "SHIFT" -> keyboard?.toggleShift()
                else -> ic.commitText(action, 1)
            }
        }
        return keyboard!!
    }
}
