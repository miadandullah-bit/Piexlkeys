package com.pixelkeys.ime.keyboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.pixelkeys.ime.data.local.ClipboardDao
import com.pixelkeys.ime.data.local.ClipboardItemEntity
import com.pixelkeys.ime.data.prefs.KeyboardPreferences
import com.pixelkeys.ime.data.prefs.OneHandedMode
import com.pixelkeys.ime.data.prefs.PreferencesRepository
import com.pixelkeys.ime.engine.VoiceTypingController
import com.pixelkeys.ime.theme.KeyboardThemeSpec
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun PixelKeysBoard(
    controller: KeyboardController,
    prefs: KeyboardPreferences,
    prefsRepo: PreferencesRepository,
    theme: KeyboardThemeSpec,
    clipboardItems: List<ClipboardItemEntity>,
    clipboardDao: ClipboardDao,
    voiceController: VoiceTypingController,
    modifier: Modifier = Modifier
) {
    // Scope state slices so typing does not recompose the entire key grid unnecessarily
    val suggestions by remember(controller) {
        controller.uiState.map { it.suggestions }.distinctUntilChanged()
    }.collectAsState(initial = emptyList())

    val activeDrawer by remember(controller) {
        controller.uiState.map { it.activeDrawer }.distinctUntilChanged()
    }.collectAsState(initial = UtilityDrawer.NONE)

    val layer by remember(controller) {
        controller.uiState.map { it.layer }.distinctUntilChanged()
    }.collectAsState(initial = KeyboardLayer.ALPHA)

    val shiftState by remember(controller) {
        controller.uiState.map { it.shiftState }.distinctUntilChanged()
    }.collectAsState(initial = ShiftState.OFF)

    val isTrackpadMode by remember(controller) {
        controller.uiState.map { it.isTrackpadMode }.distinctUntilChanged()
    }.collectAsState(initial = false)

    val rows = remember(prefs.activeLanguage, layer, shiftState, prefs.showNumberRow, prefs.showEmojiKey) {
        KeyboardLayouts.getRows(
            language = prefs.activeLanguage,
            layer = layer,
            shiftState = shiftState,
            showNumberRow = prefs.showNumberRow,
            showEmojiKey = prefs.showEmojiKey
        )
    }

    val boardWidthFraction = if (prefs.oneHandedMode == OneHandedMode.NONE) 1f else 0.86f
    val boardAlignment = when (prefs.oneHandedMode) {
        OneHandedMode.LEFT -> Alignment.CenterStart
        OneHandedMode.RIGHT -> Alignment.CenterEnd
        OneHandedMode.NONE -> Alignment.Center
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(theme.boardBackground)
            .padding(bottom = 8.dp),
        contentAlignment = boardAlignment
    ) {
        Column(modifier = Modifier.fillMaxWidth(boardWidthFraction)) {
            TopSuggestionBar(
                theme = theme,
                suggestions = suggestions,
                isDrawerOpen = activeDrawer != UtilityDrawer.NONE,
                onToggleUtility = { controller.toggleUtilityStrip() },
                onSuggestionClick = { controller.onSuggestionSelected(it) },
                onClipboardClick = { controller.openDrawer(UtilityDrawer.CLIPBOARD) }
            )

            AnimatedContent(targetState = activeDrawer, label = "drawer_anim") { drawer ->
                when (drawer) {
                    UtilityDrawer.NONE -> Spacer(Modifier.height(0.dp))
                    UtilityDrawer.UTILITIES -> UtilityStripDrawer(theme, prefs, prefsRepo, controller)
                    UtilityDrawer.EMOJI_KAOMOJI -> EmojiKaomojiDrawer(theme) {
                        controller.commitTextDirectly(it)
                    }
                    UtilityDrawer.CLIPBOARD -> ClipboardDrawer(theme, clipboardItems, clipboardDao) {
                        controller.commitTextDirectly(it)
                    }
                    UtilityDrawer.RESIZE -> ResizeDrawer(theme, prefs, prefsRepo)
                    UtilityDrawer.VOICE -> VoiceDrawer(theme, prefs, voiceController)
                    UtilityDrawer.TRANSLATE -> TranslateDrawer(theme, prefs) {
                        controller.commitTextDirectly(it)
                    }
                }
            }

            // Default key height increased to 54.dp for comfortable typing
            val keyHeight = (54f * prefs.heightScale).dp

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 5.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rows.forEach { rowKeys ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        rowKeys.forEach { key ->
                            KeyCap(
                                key = key,
                                theme = theme,
                                keyFontSizeSp = prefs.keyFontSizeSp,
                                isTrackpadMode = isTrackpadMode,
                                shiftState = shiftState,
                                modifier = Modifier
                                    .weight(key.weight)
                                    .height(keyHeight),
                                onTap = { controller.onKeyTap(key) },
                                onSecondaryTap = { sec -> controller.onSecondaryCharTap(sec) },
                                onBackspaceRepeatTick = { controller.onBackspaceRepeatTick() },
                                onTrackpadStart = { controller.onTrackpadStart() },
                                onTrackpadDrag = { controller.onTrackpadDrag(it) },
                                onTrackpadEnd = { controller.onTrackpadEnd() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopSuggestionBar(
    theme: KeyboardThemeSpec,
    suggestions: List<String>,
    isDrawerOpen: Boolean,
    onToggleUtility: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onClipboardClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .background(theme.boardBackground)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onToggleUtility, modifier = Modifier.size(34.dp)) {
            Icon(
                imageVector = if (isDrawerOpen) Icons.Outlined.GridView else Icons.Outlined.ChevronRight,
                contentDescription = "Utilities",
                tint = if (isDrawerOpen) theme.accentColor else theme.secondaryText,
                modifier = Modifier.size(20.dp)
            )
        }

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (suggestions.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "•  •  •",
                        color = theme.secondaryText.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontFamily = theme.keyFontFamily()
                    )
                }
            } else {
                suggestions.take(3).forEachIndexed { index, word ->
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(16.dp)
                                .background(theme.borderColor)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onSuggestionClick(word) }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = word,
                            color = if (index == 0) theme.keyText else theme.secondaryText,
                            fontSize = 14.sp,
                            fontWeight = if (index == 0) FontWeight.SemiBold else FontWeight.Normal,
                            fontFamily = theme.keyFontFamily(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        IconButton(onClick = onClipboardClick, modifier = Modifier.size(34.dp)) {
            Icon(
                imageVector = Icons.Outlined.ContentPaste,
                contentDescription = "Clipboard",
                tint = theme.secondaryText,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun KeyCap(
    key: KeySpec,
    theme: KeyboardThemeSpec,
    keyFontSizeSp: Float,
    isTrackpadMode: Boolean,
    shiftState: ShiftState,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    onSecondaryTap: (String) -> Unit,
    onBackspaceRepeatTick: () -> Unit,
    onTrackpadStart: () -> Unit,
    onTrackpadDrag: (Float) -> Unit,
    onTrackpadEnd: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }
    var isLongPressed by remember { mutableStateOf(false) }

    val shape = if (key.type == KeyType.ACTION) CircleShape else RoundedCornerShape(theme.keyCornerRadiusDp.dp)
    val baseBg = when (key.type) {
        KeyType.ACTION -> theme.actionSurface
        KeyType.SHIFT -> if (shiftState != ShiftState.OFF) theme.accentColor.copy(alpha = 0.25f) else theme.modifierSurface
        KeyType.BACKSPACE, KeyType.SYMBOLS, KeyType.EMOJI, KeyType.LANGUAGE -> theme.modifierSurface
        KeyType.SPACE -> if (isTrackpadMode) theme.accentColor.copy(alpha = 0.22f) else theme.keySurface
        KeyType.CHAR -> theme.keySurface
    }
    val bgColor = if (isPressed) {
        if (key.type == KeyType.ACTION) theme.actionSurface.copy(alpha = 0.8f)
        else theme.accentColor.copy(alpha = 0.28f)
    } else {
        baseBg
    }
    val textColor = if (key.type == KeyType.ACTION) Color.White else theme.keyText

    Box(
        modifier = modifier
            .clip(shape)
            .background(bgColor)
            .border(
                width = if (isPressed) 1.5.dp else if (theme.isHardwareBevel) 1.2.dp else 0.7.dp,
                color = when {
                    isPressed -> theme.accentColor
                    key.type == KeyType.ACTION -> theme.actionSurface
                    else -> theme.borderColor
                },
                shape = shape
            )
            .pointerInput(key.id, key.label, key.secondaryLabel) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    isLongPressed = false

                    var trackpadActive = false
                    var lastX = down.position.x

                    val longPressJob = scope.launch {
                        when {
                            key.type == KeyType.BACKSPACE -> {
                                delay(360L)
                                isLongPressed = true
                                while (true) {
                                    onBackspaceRepeatTick()
                                    delay(50L)
                                }
                            }
                            key.type == KeyType.CHAR && key.secondaryLabel != null -> {
                                delay(320L)
                                isLongPressed = true
                            }
                            key.type == KeyType.SPACE -> {
                                delay(280L)
                                isLongPressed = true
                                trackpadActive = true
                                onTrackpadStart()
                            }
                        }
                    }

                    if (key.type == KeyType.SPACE) {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null || !change.pressed) {
                                break
                            }
                            val dx = change.position.x - lastX
                            lastX = change.position.x
                            if (trackpadActive && dx != 0f) {
                                change.consume()
                                onTrackpadDrag(dx)
                            }
                        }
                    } else {
                        waitForUpOrCancellation()
                    }

                    longPressJob.cancel()
                    val wasLongPressed = isLongPressed
                    isPressed = false
                    isLongPressed = false

                    if (trackpadActive) {
                        onTrackpadEnd()
                    } else if (wasLongPressed && key.type == KeyType.CHAR && key.secondaryLabel != null) {
                        onSecondaryTap(key.secondaryLabel)
                    } else if (!wasLongPressed) {
                        onTap()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Floating Key Preview Popup for instant interactive feedback
        if (isPressed && key.type == KeyType.CHAR) {
            val previewText = if (isLongPressed && key.secondaryLabel != null) key.secondaryLabel else key.label
            Popup(
                alignment = Alignment.TopCenter,
                offset = IntOffset(0, -140)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 50.dp, height = 56.dp)
                        .shadow(8.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isLongPressed) theme.accentColor else theme.keySurface)
                        .border(1.5.dp, theme.accentColor, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = previewText,
                        color = if (isLongPressed) Color.White else theme.keyText,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = theme.keyFontFamily()
                    )
                }
            }
        }

        if (isTrackpadMode && key.type != KeyType.SPACE) {
            Text(
                text = "•",
                color = theme.secondaryText.copy(alpha = 0.45f),
                fontSize = 14.sp
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(3.dp)) {
                if (key.secondaryLabel != null) {
                    Text(
                        text = key.secondaryLabel,
                        color = theme.secondaryText,
                        fontSize = 9.sp,
                        fontFamily = theme.keyFontFamily(),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 2.dp)
                            .alpha(0.8f)
                    )
                }
                Text(
                    text = if (isTrackpadMode && key.type == KeyType.SPACE) "◀  trackpad  ▶" else key.label,
                    color = textColor,
                    fontSize = when (key.type) {
                        KeyType.SPACE -> 12.sp
                        KeyType.SYMBOLS, KeyType.LANGUAGE -> 13.sp
                        else -> keyFontSizeSp.sp
                    },
                    fontWeight = if (key.type == KeyType.ACTION) FontWeight.Bold else FontWeight.Medium,
                    fontFamily = theme.keyFontFamily(),
                    maxLines = 1,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
