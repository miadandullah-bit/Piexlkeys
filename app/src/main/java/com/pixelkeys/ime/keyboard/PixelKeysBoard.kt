package com.pixelkeys.ime.keyboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelkeys.ime.data.local.ClipboardDao
import com.pixelkeys.ime.data.local.ClipboardItemEntity
import com.pixelkeys.ime.data.prefs.KeyboardPreferences
import com.pixelkeys.ime.data.prefs.OneHandedMode
import com.pixelkeys.ime.data.prefs.PreferencesRepository
import com.pixelkeys.ime.engine.VoiceTypingController
import com.pixelkeys.ime.theme.KeyboardThemeSpec

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
    val uiState by controller.uiState.collectAsState()
    val rows = remember(prefs.activeLanguage, uiState.layer, uiState.shiftState, prefs.showNumberRow) {
        KeyboardLayouts.getRows(
            language = prefs.activeLanguage,
            layer = uiState.layer,
            shiftState = uiState.shiftState,
            showNumberRow = prefs.showNumberRow
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
            .padding(bottom = 6.dp),
        contentAlignment = boardAlignment
    ) {
        Column(modifier = Modifier.fillMaxWidth(boardWidthFraction)) {
            // Top Suggestion & Utility Bar
            TopSuggestionBar(
                theme = theme,
                suggestions = uiState.suggestions,
                isDrawerOpen = uiState.activeDrawer != UtilityDrawer.NONE,
                onToggleUtility = { controller.toggleUtilityStrip() },
                onSuggestionClick = { controller.onSuggestionSelected(it) },
                onClipboardClick = { controller.openDrawer(UtilityDrawer.CLIPBOARD) }
            )

            // Expandable Utility Strip or Active Drawer
            AnimatedContent(targetState = uiState.activeDrawer, label = "drawer_anim") { drawer ->
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
                    UtilityDrawer.TRANSLATE -> TranslateDrawer(theme) {
                        controller.commitTextDirectly(it)
                    }
                }
            }

            // Key Grid + Swipe Canvas
            var gridSize by remember { mutableStateOf(IntSize.Zero) }
            val keyHeight = (46f * prefs.heightScale).dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 5.dp, vertical = 4.dp)
                    .onSizeChanged { gridSize = it }
                    .pointerInput(prefs.swipeTypingEnabled, uiState.layer) {
                        if (prefs.swipeTypingEnabled && uiState.layer == KeyboardLayer.ALPHA) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    if (gridSize.width > 0 && gridSize.height > 0) {
                                        controller.onSwipeUpdate(
                                            offset.x / gridSize.width,
                                            offset.y / gridSize.height
                                        )
                                    }
                                },
                                onDrag = { change, _ ->
                                    if (gridSize.width > 0 && gridSize.height > 0) {
                                        controller.onSwipeUpdate(
                                            (change.position.x / gridSize.width).coerceIn(0f, 1f),
                                            (change.position.y / gridSize.height).coerceIn(0f, 1f)
                                        )
                                    }
                                },
                                onDragEnd = { controller.onSwipeEnd() },
                                onDragCancel = { controller.onSwipeEnd() }
                            )
                        }
                    }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(theme.keySpacingDp.dp)) {
                    rows.forEach { rowKeys ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(theme.keySpacingDp.dp)
                        ) {
                            rowKeys.forEach { key ->
                                KeyCap(
                                    key = key,
                                    theme = theme,
                                    isTrackpadMode = uiState.isTrackpadMode,
                                    shiftState = uiState.shiftState,
                                    modifier = Modifier
                                        .weight(key.weight)
                                        .height(keyHeight),
                                    onTap = { controller.onKeyTap(key) },
                                    onTrackpadStart = { controller.onTrackpadStart() },
                                    onTrackpadDrag = { controller.onTrackpadDrag(it) },
                                    onTrackpadEnd = { controller.onTrackpadEnd() }
                                )
                            }
                        }
                    }
                }

                // Glowing Swipe Trail Overlay
                if (uiState.swipePoints.size >= 2 && gridSize.width > 0 && gridSize.height > 0) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val path = Path()
                        uiState.swipePoints.forEachIndexed { idx, pt ->
                            val px = Offset(pt.x * size.width, pt.y * size.height)
                            if (idx == 0) path.moveTo(px.x, px.y) else path.lineTo(px.x, px.y)
                        }
                        drawPath(
                            path = path,
                            color = theme.accentColor.copy(alpha = 0.75f),
                            style = Stroke(width = 7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
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
            .height(40.dp)
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
                            fontSize = 13.sp,
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
    isTrackpadMode: Boolean,
    shiftState: ShiftState,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    onTrackpadStart: () -> Unit,
    onTrackpadDrag: (Float) -> Unit,
    onTrackpadEnd: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.94f else 1f, label = "key_scale")

    val shape = if (key.type == KeyType.ACTION) CircleShape else RoundedCornerShape(theme.keyCornerRadiusDp.dp)
    val bgColor = when (key.type) {
        KeyType.ACTION -> theme.actionSurface
        KeyType.SHIFT -> if (shiftState != ShiftState.OFF) theme.accentColor.copy(alpha = 0.25f) else theme.modifierSurface
        KeyType.BACKSPACE, KeyType.SYMBOLS, KeyType.LANGUAGE -> theme.modifierSurface
        KeyType.SPACE -> if (isTrackpadMode) theme.accentColor.copy(alpha = 0.2f) else theme.keySurface
        KeyType.CHAR -> theme.keySurface
    }
    val textColor = if (key.type == KeyType.ACTION) androidx.compose.ui.graphics.Color.White else theme.keyText

    val spaceTrackpadModifier = if (key.type == KeyType.SPACE) {
        Modifier.pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
                onDragStart = { onTrackpadStart() },
                onDrag = { change, dragAmount ->
                    change.consume()
                    onTrackpadDrag(dragAmount.x)
                },
                onDragEnd = { onTrackpadEnd() },
                onDragCancel = { onTrackpadEnd() }
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(bgColor)
            .border(
                width = if (theme.isHardwareBevel) 1.2.dp else 0.7.dp,
                color = if (key.type == KeyType.ACTION) theme.actionSurface else theme.borderColor,
                shape = shape
            )
            .then(spaceTrackpadModifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onTap
            ),
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
                        fontSize = 8.sp,
                        fontFamily = theme.keyFontFamily(),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 2.dp)
                            .alpha(0.75f)
                    )
                }
                Text(
                    text = if (isTrackpadMode && key.type == KeyType.SPACE) "◀  trackpad  ▶" else key.label,
                    color = textColor,
                    fontSize = when (key.type) {
                        KeyType.SPACE -> 11.sp
                        KeyType.SYMBOLS, KeyType.LANGUAGE -> 12.sp
                        else -> 17.sp
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
