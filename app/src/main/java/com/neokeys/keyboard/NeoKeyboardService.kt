package com.neokeys.keyboard

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.View
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import java.util.Locale

class NeoKeyboardService : InputMethodService(),
    SpellCheckerSession.SpellCheckerSessionListener {
    private var keyboard: NeoKeyboardView? = null
    private var spellSession: SpellCheckerSession? = null
    private var pendingSpellWord: String? = null
    private val wordAtCursor = Regex("([\\p{L}]{1,40})$")

    override fun onCreateInputView(): View {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window?.window?.setDecorFitsSystemWindows(true)
        }
        ensureSpellChecker()
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
        if (!word.isNullOrBlank()) {
            pendingSpellWord = word
            try { spellSession?.getSuggestions(arrayOf(TextInfo(word)), 5, false) }
            catch (_: Exception) { }
        }
    }

    private fun ensureSpellChecker() {
        if (spellSession != null) return
        try {
            val manager = getSystemService(TEXT_SERVICES_MANAGER_SERVICE) as TextServicesManager
            spellSession = manager.newSpellCheckerSession(
                null, Locale("es", "MX"), this, false
            )
        } catch (_: Exception) {
            spellSession = null
        }
    }

    override fun onGetSuggestions(results: Array<SuggestionsInfo>) {
        val original = pendingSpellWord ?: return
        if (results.isEmpty() || currentWord() != original) return
        val info = results[0]
        val looksLikeTypo = info.suggestionsAttributes and
            SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO != 0
        if (!looksLikeTypo || info.suggestionsCount <= 0) return
        val alternatives = mutableListOf(original)
        for (i in 0 until minOf(info.suggestionsCount, 5)) {
            val candidate = info.getSuggestionAt(i) ?: continue
            if (candidate.isNotBlank() && candidate.all { it.isLetter() }) {
                alternatives.add(matchCase(original, candidate))
            }
        }
        keyboard?.updateSuggestions(alternatives.distinct().take(3))
    }

    override fun onGetSentenceSuggestions(
        results: Array<android.view.textservice.SentenceSuggestionsInfo>
    ) {
        // Sentence-level suggestions are not needed for this first iteration.
    }

    private fun applySuggestion(selected: String) {
        val ic = currentInputConnection ?: return
        val word = currentWord() ?: return
        if (selected.isBlank() || selected.any { !it.isLetter() }) return
        ic.beginBatchEdit()
        ic.deleteSurroundingText(word.length, 0)
        // Selecting a suggestion accepts the word and immediately inserts a space.
        ic.commitText(matchCase(word, selected) + " ", 1)
        ic.endBatchEdit()
        refreshSuggestions()
    }

    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        refreshSuggestions()
    }

    override fun onDestroy() {
        spellSession?.close()
        spellSession = null
        keyboard = null
        super.onDestroy()
    }
}
