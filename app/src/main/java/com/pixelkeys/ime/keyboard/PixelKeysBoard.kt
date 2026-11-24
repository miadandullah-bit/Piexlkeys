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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
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

private data class KeyPreviewState(
    val keyId: String,
    val text: String,
    val centerX: Int,
    val topY: Int,
    val isSecondary: Boolean
)

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

    var rootCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var activePreview by remember { mutableStateOf<KeyPreviewState?>(null) }
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(theme.boardBackground)
            .padding(bottom = 8.dp)
            .onGloballyPositioned { rootCoords = it },
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
                                rootCoordsProvider = { rootCoords },
                                modifier = Modifier
                                    .weight(key.weight)
                                    .height(keyHeight),
                                onTap = { controller.onKeyTap(key) },
                                onSecondaryTap = { sec -> controller.onSecondaryCharTap(sec) },
                                onBackspaceRepeatTick = { controller.onBackspaceRepeatTick() },
                                onTrackpadStart = { controller.onTrackpadStart() },
                                onTrackpadDrag = { controller.onTrackpadDrag(it) },
                                onTrackpadEnd = { controller.onTrackpadEnd() },
                                onPreviewShow = { preview -> activePreview = preview },
                                onPreviewHide = { id ->
                                    if (activePreview?.keyId == id) {
                                        activePreview = null
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Single zero-lag Compose key preview bubble (no Android WindowManager Popup overhead)
        activePreview?.let { preview ->
            val bubbleW = 46.dp
            val bubbleH = 46.dp
            val bubbleWPx = with(density) { bubbleW.roundToPx() }
            val bubbleHPx = with(density) { bubbleH.roundToPx() }
            val gapPx = with(density) { 4.dp.roundToPx() }
            val rootW = rootCoords?.size?.width ?: 1080
            val offsetX = (preview.centerX - bubbleWPx / 2).coerceIn(4, (rootW - bubbleWPx - 4).coerceAtLeast(4))
            val offsetY = (preview.topY - bubbleHPx - gapPx).coerceAtLeast(2)

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset { IntOffset(offsetX, offsetY) }
                    .zIndex(20f)
                    .size(width = bubbleW, height = bubbleH)
                    .shadow(6.dp, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (preview.isSecondary) theme.accentColor else theme.keySurface)
                    .border(1.5.dp, theme.accentColor, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = preview.text,
                    color = if (preview.isSecondary) Color.White else theme.keyText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = theme.keyFontFamily()
                )
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
    rootCoordsProvider: () -> LayoutCoordinates?,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    onSecondaryTap: (String) -> Unit,
    onBackspaceRepeatTick: () -> Unit,
    onTrackpadStart: () -> Unit,
    onTrackpadDrag: (Float) -> Unit,
    onTrackpadEnd: () -> Unit,
    onPreviewShow: (KeyPreviewState) -> Unit,
    onPreviewHide: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }
    var keyCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val currentKey by rememberUpdatedState(key)
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnSecondaryTap by rememberUpdatedState(onSecondaryTap)
    val currentOnBackspaceRepeatTick by rememberUpdatedState(onBackspaceRepeatTick)
    val currentOnTrackpadStart by rememberUpdatedState(onTrackpadStart)
    val currentOnTrackpadDrag by rememberUpdatedState(onTrackpadDrag)
    val currentOnTrackpadEnd by rememberUpdatedState(onTrackpadEnd)
    val currentOnPreviewShow by rememberUpdatedState(onPreviewShow)
    val currentOnPreviewHide by rememberUpdatedState(onPreviewHide)

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

    fun updatePreview(isSecondary: Boolean) {
        val kSpec = currentKey
        if (kSpec.type != KeyType.CHAR) return
        val kCoords = keyCoords
        val rCoords = rootCoordsProvider()
        if (kCoords != null && rCoords != null && kCoords.isAttached && rCoords.isAttached) {
            val pos = rCoords.localPositionOf(kCoords, Offset.Zero)
            val text = if (isSecondary && kSpec.secondaryLabel != null) kSpec.secondaryLabel else kSpec.label
            currentOnPreviewShow(
                KeyPreviewState(
                    keyId = kSpec.id,
                    text = text,
                    centerX = (pos.x + kCoords.size.width / 2f).roundToInt(),
                    topY = pos.y.roundToInt(),
                    isSecondary = isSecondary
                )
            )
        }
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { keyCoords = it }
            .graphicsLayer {
                val scale = if (isPressed) 1.04f else 1f
                scaleX = scale
                scaleY = scale
                translationY = if (isPressed) -3f else 0f
            }
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
            .pointerInput(key.id) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    var isLongPressed = false
                    var trackpadActive = false
                    var lastX = down.position.x
                    updatePreview(isSecondary = false)

                    val longPressJob = scope.launch {
                        val kSpec = currentKey
                        when {
                            kSpec.type == KeyType.BACKSPACE -> {
                                delay(340L)
                                isLongPressed = true
                                while (true) {
                                    currentOnBackspaceRepeatTick()
                                    delay(45L)
                                }
                            }
                            kSpec.type == KeyType.CHAR && kSpec.secondaryLabel != null -> {
                                delay(300L)
                                isLongPressed = true
                                updatePreview(isSecondary = true)
                            }
                            kSpec.type == KeyType.SPACE -> {
                                delay(260L)
                                isLongPressed = true
                                trackpadActive = true
                                currentOnTrackpadStart()
                            }
                        }
                    }

                    try {
                        if (currentKey.type == KeyType.SPACE) {
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
                                    currentOnTrackpadDrag(dx)
                                }
                            }
                        } else {
                            waitForUpOrCancellation()
                        }
                    } finally {
                        longPressJob.cancel()
                        isPressed = false
                        currentOnPreviewHide(currentKey.id)
                    }

                    val secLabel = currentKey.secondaryLabel
                    if (trackpadActive) {
                        currentOnTrackpadEnd()
                    } else if (isLongPressed && currentKey.type == KeyType.CHAR && secLabel != null) {
                        currentOnSecondaryTap(secLabel)
                    } else if (!isLongPressed) {
                        currentOnTap()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
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
