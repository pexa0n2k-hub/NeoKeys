package com.neokeys.keyboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import java.util.Locale

class NeoKeyboardService : InputMethodService(), SpellCheckerSession.SpellCheckerSessionListener {
    private var keyboard: NeoKeyboardView? = null
    private var spellSession: SpellCheckerSession? = null
    private var pendingSpellWord: String? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var voiceActive = false
    private val spellHandler = Handler(Looper.getMainLooper())
    private var spellRequest: Runnable? = null
    private val wordAtCursor = Regex("([\\p{L}]{1,40})$")

    override fun onCreateInputView(): View {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) window?.window?.setDecorFitsSystemWindows(true)
        ensureSpellChecker()
        val view = NeoKeyboardView(this) { action ->
            when {
                action == "SETTINGS" -> startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                action == "VOICE" -> startVoiceRecognition()
                action.startsWith("SUGGEST:") -> applySuggestion(action.removePrefix("SUGGEST:"))
                else -> {
                    val ic = currentInputConnection ?: return@NeoKeyboardView
                    when (action) {
                        "DELETE" -> ic.deleteSurroundingText(1, 0)
                        "SPACE" -> {
                            val original = currentWord()
                            val correction = if (enabled() && original != null) SpanishWordPredictor.correction(original) else null
                            if (original != null && correction != null) {
                                ic.beginBatchEdit(); ic.deleteSurroundingText(original.length, 0)
                                ic.commitText(matchCase(original, correction) + " ", 1); ic.endBatchEdit()
                            } else ic.commitText(" ", 1)
                            refreshSuggestions(requestSpellCheck = true)
                        }
                        "ENTER" -> performEditorActionOrNewline()
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


    /**
     * Executes the action requested by the focused app (Search, Go, Send,
     * Next, Done, etc.). If the field is a normal multiline editor, inserts
     * a line break instead.
     */
    private fun performEditorActionOrNewline() {
        val ic = currentInputConnection ?: return
        val editorInfo = currentInputEditorInfo
        val action = (editorInfo?.imeOptions ?: EditorInfo.IME_ACTION_UNSPECIFIED) and
            EditorInfo.IME_MASK_ACTION

        // Give an explicit app action priority over the multiline flag.
        // Some search fields (Google, YouTube, Play Store) can also advertise
        // multiline input, but ENTER must still execute Search/Go.
        when (action) {
            EditorInfo.IME_ACTION_SEARCH,
            EditorInfo.IME_ACTION_GO,
            EditorInfo.IME_ACTION_SEND,
            EditorInfo.IME_ACTION_NEXT,
            EditorInfo.IME_ACTION_DONE,
            EditorInfo.IME_ACTION_PREVIOUS -> {
                ic.performEditorAction(action)
                return
            }
        }

        val inputType = editorInfo?.inputType ?: InputType.TYPE_NULL
        val isMultiLine = (inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0

        // No explicit IME action: preserve newline behavior in multiline editors.
        if (isMultiLine) {
            ic.commitText("\n", 1)
        } else {
            // Fallback for ordinary text fields without an advertised action.
            ic.commitText("\n", 1)
        }
    }

    private fun viewLanguage() { keyboard?.toggleLanguage(); keyboard?.updateSuggestions(emptyList()) }
    private fun enabled() = getSharedPreferences("neokeys_settings", MODE_PRIVATE).getBoolean("autocorrect_enabled", true)
    private fun currentWord(): String? {
        val text = currentInputConnection?.getTextBeforeCursor(80, 0)?.toString() ?: return null
        return wordAtCursor.find(text)?.groupValues?.get(1)
    }
    private fun matchCase(original: String, replacement: String): String =
        if (original.firstOrNull()?.isUpperCase() == true) replacement.replaceFirstChar { it.uppercase(Locale("es", "MX")) } else replacement

    // Local suggestions remain immediate; the system spell checker is queried only at word boundaries.
    private fun refreshSuggestions(requestSpellCheck: Boolean = false) {
        val word = if (enabled()) currentWord() else null
        keyboard?.updateSuggestions(if (word.isNullOrBlank()) emptyList() else SpanishWordPredictor.suggestions(word))
        if (!word.isNullOrBlank()) {
            // Wait briefly for typing to pause; never run the system spell checker on every tap.
            spellRequest?.let { spellHandler.removeCallbacks(it) }
            val candidate = word
            val task = Runnable {
                if (currentWord() == candidate) {
                    pendingSpellWord = candidate
                    try { spellSession?.getSuggestions(arrayOf(TextInfo(candidate)), 5, false) } catch (_: Exception) { }
                }
            }
            spellRequest = task
            spellHandler.postDelayed(task, 350L)
        } else {
            spellRequest?.let { spellHandler.removeCallbacks(it) }
            spellRequest = null
        }
    }

    private fun ensureSpellChecker() {
        if (spellSession != null) return
        try {
            val manager = getSystemService(TEXT_SERVICES_MANAGER_SERVICE) as TextServicesManager
            spellSession = manager.newSpellCheckerSession(null, Locale("es", "MX"), this, false)
        } catch (_: Exception) { spellSession = null }
    }

    private fun startVoiceRecognition() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            keyboard?.setVoiceListening(false)
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            keyboard?.setVoiceListening(false)
            return
        }
        try {
            if (speechRecognizer == null) speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).also { recognizer ->
                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: android.os.Bundle?) { voiceActive = true; keyboard?.setVoiceListening(true) }
                    override fun onBeginningOfSpeech() { }
                    override fun onRmsChanged(rmsdB: Float) { }
                    override fun onBufferReceived(buffer: ByteArray?) { }
                    override fun onEndOfSpeech() { }
                    override fun onError(error: Int) { voiceActive = false; keyboard?.setVoiceListening(false) }
                    override fun onResults(results: android.os.Bundle?) {
                        voiceActive = false; keyboard?.setVoiceListening(false)
                        val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim().orEmpty()
                        if (spoken.isNotEmpty()) {
                            val before = currentInputConnection?.getTextBeforeCursor(1, 0)?.toString().orEmpty()
                            val insertion = if (before.isNotEmpty() && !before.last().isWhitespace()) " $spoken" else spoken
                            currentInputConnection?.commitText(insertion, 1)
                            refreshSuggestions()
                        }
                    }
                    override fun onPartialResults(partialResults: android.os.Bundle?) { }
                    override fun onEvent(eventType: Int, params: android.os.Bundle?) { }
                })
            }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-MX")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-MX")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            keyboard?.setVoiceListening(true)
            speechRecognizer?.startListening(intent)
        } catch (_: Exception) {
            voiceActive = false; keyboard?.setVoiceListening(false)
        }
    }

    override fun onGetSuggestions(results: Array<SuggestionsInfo>) {
        val original = pendingSpellWord ?: return
        if (results.isEmpty() || currentWord() != original) return
        val info = results[0]
        val looksLikeTypo = info.suggestionsAttributes and SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO != 0
        if (!looksLikeTypo || info.suggestionsCount <= 0) return
        val alternatives = mutableListOf(original)
        for (i in 0 until minOf(info.suggestionsCount, 5)) {
            val candidate = info.getSuggestionAt(i) ?: continue
            if (candidate.isNotBlank() && candidate.all { it.isLetter() }) alternatives.add(matchCase(original, candidate))
        }
        keyboard?.updateSuggestions(alternatives.distinct().take(3))
    }
    override fun onGetSentenceSuggestions(results: Array<android.view.textservice.SentenceSuggestionsInfo>) { }

    private fun applySuggestion(selected: String) {
        val ic = currentInputConnection ?: return
        val word = currentWord() ?: return
        if (selected.isBlank() || selected.any { !it.isLetter() }) return
        ic.beginBatchEdit(); ic.deleteSurroundingText(word.length, 0)
        ic.commitText(matchCase(word, selected) + " ", 1); ic.endBatchEdit()
        refreshSuggestions(requestSpellCheck = true)
    }

    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting); refreshSuggestions()
    }
    override fun onDestroy() {
        try { speechRecognizer?.cancel(); speechRecognizer?.destroy() } catch (_: Exception) { }
        speechRecognizer = null; spellRequest?.let { spellHandler.removeCallbacks(it) }; spellRequest = null
        spellSession?.close(); spellSession = null; keyboard = null
        super.onDestroy()
    }
}
