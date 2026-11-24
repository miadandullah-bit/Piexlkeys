package com.pixelkeys.ime.keyboard

import com.pixelkeys.ime.data.prefs.InputLanguage
import com.pixelkeys.ime.data.prefs.KeyboardPreferences
import com.pixelkeys.ime.data.prefs.PreferencesRepository
import com.pixelkeys.ime.engine.AvroPhoneticEngine
import com.pixelkeys.ime.engine.EnglishEngine
import com.pixelkeys.ime.engine.SwipePoint
import com.pixelkeys.ime.haptics.HapticController
import com.pixelkeys.ime.ime.TextTargetAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class KeyboardUiState(
    val layer: KeyboardLayer = KeyboardLayer.ALPHA,
    val shiftState: ShiftState = ShiftState.OFF,
    val activeDrawer: UtilityDrawer = UtilityDrawer.NONE,
    val composingWord: String = "",
    val suggestions: List<String> = emptyList(),
    val isTrackpadMode: Boolean = false,
    val swipePoints: List<SwipePoint> = emptyList(),
    val userWords: List<String> = emptyList()
)

class KeyboardController(
    private val scope: CoroutineScope,
    private val prefsRepo: PreferencesRepository,
    private val hapticController: HapticController,
    private var target: TextTargetAdapter
) {
    private val _uiState = MutableStateFlow(KeyboardUiState())
    val uiState: StateFlow<KeyboardUiState> = _uiState.asStateFlow()

    private var currentPrefs = KeyboardPreferences()
    private var lastShiftTapMs = 0L
    private var trackpadAccumulatedPx = 0f

    fun updateTarget(newTarget: TextTargetAdapter) {
        target = newTarget
    }

    fun updatePreferences(prefs: KeyboardPreferences) {
        currentPrefs = prefs
    }

    fun updateUserWords(words: List<String>) {
        _uiState.update { it.copy(userWords = words) }
    }

    fun onKeyTap(key: KeySpec) {
        hapticController.performKeyPress(
            durationMs = currentPrefs.hapticStrengthMs,
            style = currentPrefs.hapticStyle,
            soundEnabled = currentPrefs.soundEnabled
        )

        when (key.type) {
            KeyType.CHAR -> handleCharInput(key.output)
            KeyType.SHIFT -> handleShiftTap()
            KeyType.BACKSPACE -> handleBackspace()
            KeyType.SYMBOLS -> handleSymbolsToggle(key.id)
            KeyType.LANGUAGE -> handleLanguageCycle()
            KeyType.SPACE -> handleSpaceTap()
            KeyType.ACTION -> handleActionTap()
        }
    }

    private fun handleCharInput(charText: String) {
        val lang = currentPrefs.activeLanguage
        if (_uiState.value.layer == KeyboardLayer.ALPHA &&
            (lang == InputLanguage.EN_US || lang == InputLanguage.AVRO_BN) &&
            charText.length == 1 &&
            charText[0].isLetter()
        ) {
            val newComposing = _uiState.value.composingWord + charText
            val displayComposing = if (lang == InputLanguage.AVRO_BN) {
                AvroPhoneticEngine.transliterate(newComposing)
            } else {
                newComposing
            }
            target.setComposingText(displayComposing)
            val newSuggestions = computeSuggestions(newComposing, lang)
            _uiState.update {
                it.copy(
                    composingWord = newComposing,
                    suggestions = newSuggestions,
                    shiftState = if (it.shiftState == ShiftState.ONCE) ShiftState.OFF else it.shiftState
                )
            }
        } else {
            commitPendingComposing()
            target.commitText(charText)
            if (_uiState.value.shiftState == ShiftState.ONCE) {
                _uiState.update { it.copy(shiftState = ShiftState.OFF) }
            }
        }
    }

    private fun handleShiftTap() {
        val now = System.currentTimeMillis()
        val next = when (_uiState.value.shiftState) {
            ShiftState.OFF -> ShiftState.ONCE
            ShiftState.ONCE -> if (now - lastShiftTapMs < 350L) ShiftState.CAPS_LOCK else ShiftState.OFF
            ShiftState.CAPS_LOCK -> ShiftState.OFF
        }
        lastShiftTapMs = now
        _uiState.update { it.copy(shiftState = next) }
    }

    private fun handleBackspace() {
        val composing = _uiState.value.composingWord
        if (composing.isNotEmpty()) {
            val trimmed = composing.dropLast(1)
            if (trimmed.isEmpty()) {
                target.setComposingText("")
                target.finishComposingText()
                _uiState.update { it.copy(composingWord = "", suggestions = emptyList()) }
            } else {
                val lang = currentPrefs.activeLanguage
                val display = if (lang == InputLanguage.AVRO_BN) {
                    AvroPhoneticEngine.transliterate(trimmed)
                } else {
                    trimmed
                }
                target.setComposingText(display)
                _uiState.update {
                    it.copy(
                        composingWord = trimmed,
                        suggestions = computeSuggestions(trimmed, lang)
                    )
                }
            }
        } else {
            target.deleteBackward(1)
        }
    }

    private fun handleSymbolsToggle(keyId: String) {
        _uiState.update { state ->
            val nextLayer = when {
                keyId == "sym_page" && state.layer == KeyboardLayer.SYMBOLS_1 -> KeyboardLayer.SYMBOLS_2
                keyId == "sym_page" && state.layer == KeyboardLayer.SYMBOLS_2 -> KeyboardLayer.SYMBOLS_1
                state.layer == KeyboardLayer.ALPHA -> KeyboardLayer.SYMBOLS_1
                else -> KeyboardLayer.ALPHA
            }
            state.copy(layer = nextLayer)
        }
    }

    private fun handleLanguageCycle() {
        commitPendingComposing()
        scope.launch {
            prefsRepo.cycleLanguage(currentPrefs.activeLanguage)
        }
    }

    private fun handleSpaceTap() {
        val state = _uiState.value
        val composing = state.composingWord
        if (composing.isNotEmpty()) {
            val best = state.suggestions.firstOrNull()
            val toCommit = if (currentPrefs.activeLanguage == InputLanguage.AVRO_BN) {
                best ?: AvroPhoneticEngine.transliterate(composing)
            } else if (currentPrefs.autocorrectEnabled && best != null) {
                best
            } else {
                composing
            }
            target.commitText("$toCommit ")
            _uiState.update { it.copy(composingWord = "", suggestions = emptyList()) }
        } else {
            target.commitText(" ")
        }
    }

    private fun handleActionTap() {
        commitPendingComposing()
        target.performEditorAction()
    }

    fun onSuggestionSelected(word: String) {
        hapticController.performKeyPress(
            durationMs = currentPrefs.hapticStrengthMs,
            style = currentPrefs.hapticStyle,
            soundEnabled = currentPrefs.soundEnabled
        )
        target.commitText("$word ")
        _uiState.update { it.copy(composingWord = "", suggestions = emptyList()) }
    }

    fun commitTextDirectly(text: String) {
        commitPendingComposing()
        target.commitText(text)
    }

    private fun commitPendingComposing() {
        val composing = _uiState.value.composingWord
        if (composing.isNotEmpty()) {
            val text = if (currentPrefs.activeLanguage == InputLanguage.AVRO_BN) {
                AvroPhoneticEngine.transliterate(composing)
            } else {
                composing
            }
            target.commitText(text)
            _uiState.update { it.copy(composingWord = "", suggestions = emptyList()) }
        }
    }

    // Spacebar Cursor Trackpad
    fun onTrackpadStart() {
        commitPendingComposing()
        trackpadAccumulatedPx = 0f
        hapticController.performTrackpadPulse()
        _uiState.update { it.copy(isTrackpadMode = true) }
    }

    fun onTrackpadDrag(deltaX: Float) {
        trackpadAccumulatedPx += deltaX
        val stepPx = 18f
        while (trackpadAccumulatedPx >= stepPx) {
            target.moveCursor(1)
            trackpadAccumulatedPx -= stepPx
        }
        while (trackpadAccumulatedPx <= -stepPx) {
            target.moveCursor(-1)
            trackpadAccumulatedPx += stepPx
        }
    }

    fun onTrackpadEnd() {
        trackpadAccumulatedPx = 0f
        _uiState.update { it.copy(isTrackpadMode = false) }
    }

    // Swipe Typing
    fun onSwipeUpdate(normalizedX: Float, normalizedY: Float) {
        if (!currentPrefs.swipeTypingEnabled || _uiState.value.layer != KeyboardLayer.ALPHA) return
        _uiState.update {
            it.copy(swipePoints = it.swipePoints + SwipePoint(normalizedX, normalizedY))
        }
    }

    fun onSwipeEnd() {
        val points = _uiState.value.swipePoints
        _uiState.update { it.copy(swipePoints = emptyList()) }
        if (points.size < 4 || currentPrefs.activeLanguage != InputLanguage.EN_US) return

        val decoded = EnglishEngine.decodeSwipe(points, _uiState.value.userWords)
        val best = decoded.firstOrNull() ?: return
        target.commitText("$best ")
        _uiState.update { it.copy(composingWord = "", suggestions = decoded) }
    }

    // Utility Drawers
    fun toggleUtilityStrip() {
        _uiState.update {
            val next = if (it.activeDrawer == UtilityDrawer.NONE) UtilityDrawer.UTILITIES else UtilityDrawer.NONE
            it.copy(activeDrawer = next)
        }
    }

    fun openDrawer(drawer: UtilityDrawer) {
        _uiState.update {
            val next = if (it.activeDrawer == drawer) UtilityDrawer.NONE else drawer
            it.copy(activeDrawer = next)
        }
    }

    private fun computeSuggestions(composing: String, lang: InputLanguage): List<String> {
        return when (lang) {
            InputLanguage.EN_US -> EnglishEngine.suggest(
                composing = composing,
                userWords = _uiState.value.userWords,
                autocorrectEnabled = currentPrefs.autocorrectEnabled
            )
            InputLanguage.AVRO_BN -> AvroPhoneticEngine.getCandidates(composing)
            InputLanguage.BENGALI_NATIVE -> emptyList()
        }
    }
}
