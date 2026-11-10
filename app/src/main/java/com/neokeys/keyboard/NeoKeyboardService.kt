package com.neokeys.keyboard

import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.View

class NeoKeyboardService : InputMethodService() {
    private var keyboard: NeoKeyboardView? = null

    override fun onCreateInputView(): View {
        // Let the IME window fit its content above Android system bars.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window?.window?.setDecorFitsSystemWindows(true)
        }

        val inputView = NeoKeyboardView(this) { action ->
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
        keyboard = inputView
        return inputView
    }

    override fun onDestroy() {
        keyboard = null
        super.onDestroy()
    }
}
