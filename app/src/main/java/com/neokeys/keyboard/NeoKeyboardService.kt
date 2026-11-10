package com.neokeys.keyboard

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.View
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import java.util.Locale

class NeoKeyboardService : InputMethodService(), SpellCheckerSession.SpellCheckerSessionListener {
    private var keyboard: NeoKeyboardView? = null
    private var spellCheckerSession: SpellCheckerSession? = null
    private var pendingWord: String? = null
    private var spellCheckPending = false

    override fun onCreateInputView(): View {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window?.window?.setDecorFitsSystemWindows(true)
        }
        ensureSpanishSpellChecker()
        val inputView = NeoKeyboardView(this) { action ->
            when (action) {
                "SETTINGS" -> {
                    val intent = Intent(this, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
                else -> {
                    val ic = currentInputConnection ?: return@NeoKeyboardView
                    when (action) {
                        "DELETE" -> ic.deleteSurroundingText(1, 0)
                        "SPACE" -> {
                            ic.commitText(" ", 1)
                            requestSpanishCorrection()
                        }
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

    private fun ensureSpanishSpellChecker() {
        if (spellCheckerSession != null) return
        try {
            val manager = getSystemService(TEXT_SERVICES_MANAGER_SERVICE) as TextServicesManager
            spellCheckerSession = manager.newSpellCheckerSession(null, Locale("es", "MX"), this, false)
        } catch (_: Exception) {
            spellCheckerSession = null
        }
    }

    private fun requestSpanishCorrection() {
        if (!getSharedPreferences("neokeys_settings", MODE_PRIVATE)
                .getBoolean("autocorrect_enabled", true)) return
        val session = spellCheckerSession ?: return
        if (spellCheckPending) return

        val before = currentInputConnection?.getTextBeforeCursor(64, 0)?.toString() ?: return
        val match = Regex("([\\p{L}]{3,}) $").find(before) ?: return
        val word = match.groupValues[1]
        if (word.length > 30) return

        pendingWord = word
        spellCheckPending = true
        try {
            session.getSuggestions(arrayOf(TextInfo(word)), 5, false)
        } catch (_: Exception) {
            spellCheckPending = false
            pendingWord = null
        }
    }

    override fun onGetSuggestions(results: Array<SuggestionsInfo>) {
        val original = pendingWord
        spellCheckPending = false
        pendingWord = null
        if (original == null || results.isEmpty()) return

        val suggestion = results[0]
        val looksLikeTypo = suggestion.suggestionsAttributes and
            SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO != 0
        if (!looksLikeTypo || suggestion.suggestionsCount <= 0) return

        var replacement = suggestion.getSuggestionAt(0) ?: return
        if (replacement.isBlank() || replacement.any { it.isWhitespace() }) return
        if (original.firstOrNull()?.isUpperCase() == true) {
            replacement = replacement.replaceFirstChar { it.uppercase(Locale("es", "MX")) }
        }
        if (replacement.equals(original, ignoreCase = true)) return

        val ic = currentInputConnection ?: return
        val expectedSuffix = "$original "
        val actualSuffix = ic.getTextBeforeCursor(expectedSuffix.length, 0)?.toString()
        if (actualSuffix == expectedSuffix) {
            ic.deleteSurroundingText(expectedSuffix.length, 0)
            ic.commitText("$replacement ", 1)
        }
    }

    override fun onGetSentenceSuggestions(results: Array<SentenceSuggestionsInfo>) {
        // Sentence suggestions are not used in this first Spanish autocorrect version.
    }

    override fun onDestroy() {
        spellCheckerSession?.close()
        spellCheckerSession = null
        keyboard = null
        super.onDestroy()
    }
}
