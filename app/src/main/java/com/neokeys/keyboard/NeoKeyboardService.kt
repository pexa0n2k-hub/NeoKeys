package com.neokeys.keyboard

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.View

class NeoKeyboardService : InputMethodService() {
    private var keyboard: NeoKeyboardView? = null

    override fun onCreateInputView(): View {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window?.window?.setDecorFitsSystemWindows(true)
        }
        val inputView = NeoKeyboardView(this) { action ->
            when (action) {
                "SETTINGS" -> {
                    val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
                else -> {
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
