package com.neokeys.keyboard

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.View
import java.util.Locale

class NeoKeyboardService : InputMethodService() {
    private var keyboard: NeoKeyboardView? = null
    private val wordAtCursor = Regex("([\\p{L}]{1,40})$")

    override fun onCreateInputView(): View {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window?.window?.setDecorFitsSystemWindows(true)
        }
        val view = NeoKeyboardView(this) { action ->
            when {
                action == "SETTINGS" -> startActivity(
                    Intent(this, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                action.startsWith("SUGGEST:") -> applySuggestion(action.removePrefix("SUGGEST:"))
                else -> {
                    val ic = currentInputConnection ?: return@NeoKeyboardView
                    when (action) {
                        "DELETE" -> ic.deleteSurroundingText(1, 0)
                        "SPACE" -> {
                            val original = currentWord()
                            val correction = if (enabled() && original != null)
                                SpanishWordPredictor.correction(original) else null
                            if (original != null && correction != null) {
                                ic.beginBatchEdit()
                                ic.deleteSurroundingText(original.length, 0)
                                ic.commitText(matchCase(original, correction) + " ", 1)
                                ic.endBatchEdit()
                            } else ic.commitText(" ", 1)
                        }
                        "ENTER" -> ic.commitText("\n", 1)
                        "LANG" -> viewLanguage()
                        "SHIFT" -> keyboard?.toggleShift()
                        else -> ic.commitText(action, 1)
                    }
                    refreshSuggestions()
                }
            }
        }
        keyboard = view
        refreshSuggestions()
        return view
    }

    private fun viewLanguage() {
        keyboard?.toggleLanguage()
        keyboard?.updateSuggestions(emptyList())
    }

    private fun enabled() = getSharedPreferences("neokeys_settings", MODE_PRIVATE)
        .getBoolean("autocorrect_enabled", true)

    private fun currentWord(): String? {
        val text = currentInputConnection?.getTextBeforeCursor(80, 0)?.toString() ?: return null
        return wordAtCursor.find(text)?.groupValues?.get(1)
    }

    private fun matchCase(original: String, replacement: String): String =
        if (original.firstOrNull()?.isUpperCase() == true)
            replacement.replaceFirstChar { it.uppercase(Locale("es", "MX")) }
        else replacement

    private fun refreshSuggestions() {
        val word = if (enabled()) currentWord() else null
        keyboard?.updateSuggestions(
            if (word.isNullOrBlank()) emptyList()
            else SpanishWordPredictor.suggestions(word)
        )
    }

    private fun applySuggestion(selected: String) {
        val ic = currentInputConnection ?: return
        val word = currentWord() ?: return
        if (selected.isBlank() || selected.any { !it.isLetter() }) return
        ic.beginBatchEdit()
        ic.deleteSurroundingText(word.length, 0)
        ic.commitText(matchCase(word, selected), 1)
        ic.endBatchEdit()
        refreshSuggestions()
    }

    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        refreshSuggestions()
    }

    override fun onDestroy() {
        keyboard = null
        super.onDestroy()
    }
}
