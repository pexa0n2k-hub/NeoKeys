package com.neokeys.keyboard

import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.View
import android.view.WindowInsets

class NeoKeyboardService : InputMethodService() {

    private var keyboard: NeoKeyboardView? = null

    override fun onCreateInputView(): View {
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

        window?.window?.decorView?.let { decor ->
            decor.setOnApplyWindowInsetsListener { _, insets ->

                val bottomInset = if (Build.VERSION.SDK_INT >= 30) {
                    insets.getInsets(
                        WindowInsets.Type.navigationBars()
                    ).bottom
                } else {
                    @Suppress("DEPRECATION")
                    insets.systemWindowInsetBottom
                }

                keyboard?.setNavigationInsetPx(bottomInset)

                insets
            }

            decor.requestApplyInsets()
        }

        return inputView
    }

    override fun onDestroy() {
        window?.window?.decorView?.setOnApplyWindowInsetsListener(null)
        keyboard = null
        super.onDestroy()
    }
}
