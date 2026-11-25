package com.pixelkeys.ime.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Height
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelkeys.ime.ai.AiRepository
import com.pixelkeys.ime.data.local.ClipboardDao
import com.pixelkeys.ime.data.local.ClipboardItemEntity
import com.pixelkeys.ime.data.local.ClipboardSyncManager
import com.pixelkeys.ime.data.prefs.InputLanguage
import com.pixelkeys.ime.data.prefs.KeyboardPreferences
import com.pixelkeys.ime.data.prefs.OneHandedMode
import com.pixelkeys.ime.data.prefs.PreferencesRepository
import com.pixelkeys.ime.engine.VoiceTypingController
import com.pixelkeys.ime.theme.KeyboardThemeSpec
import com.pixelkeys.ime.theme.contrastTextOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map

@Composable
fun UtilityStripDrawer(
    theme: KeyboardThemeSpec,
    prefs: KeyboardPreferences,
    prefsRepo: PreferencesRepository,
    controller: KeyboardController
) {
    val scope = rememberCoroutineScope()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.boardBackground)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        UtilityActionChip(Icons.Outlined.EmojiEmotions, "Emoji", theme) {
            controller.openDrawer(UtilityDrawer.EMOJI_KAOMOJI)
        }
        UtilityActionChip(Icons.Outlined.ContentPaste, "Clips", theme) {
            controller.openDrawer(UtilityDrawer.CLIPBOARD)
        }
        UtilityActionChip(Icons.Outlined.Height, "Resize", theme) {
            controller.openDrawer(UtilityDrawer.RESIZE)
        }
        UtilityActionChip(Icons.Outlined.Mic, "Voice", theme) {
            controller.openDrawer(UtilityDrawer.VOICE)
        }
        UtilityActionChip(Icons.Outlined.Translate, "Translate", theme) {
            controller.openDrawer(UtilityDrawer.TRANSLATE)
        }
        UtilityActionChip(Icons.Outlined.AutoFixHigh, "AI Refine", theme) {
            controller.openDrawer(UtilityDrawer.AI_REFINE)
        }
        UtilityActionChip(
            icon = Icons.Outlined.Numbers,
            label = if (prefs.showNumberRow) "123 On" else "123 Off",
            theme = theme
        ) {
            scope.launch { prefsRepo.setShowNumberRow(!prefs.showNumberRow) }
        }
        UtilityActionChip(Icons.Outlined.Language, prefs.activeLanguage.shortLabel, theme) {
            scope.launch { prefsRepo.cycleLanguage(prefs.activeLanguage) }
        }
    }
}

@Composable
private fun UtilityActionChip(
    icon: ImageVector,
    label: String,
    theme: KeyboardThemeSpec,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(theme.modifierSurface)
            .border(1.dp, theme.borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = theme.keyText, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = theme.secondaryText,
            fontSize = 10.sp,
            fontFamily = theme.keyFontFamily()
        )
    }
}

@Composable
fun EmojiKaomojiDrawer(
    theme: KeyboardThemeSpec,
    onSelect: (String) -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    var selectedCategoryIdx by remember { mutableIntStateOf(0) }
    val categories = EmojiCatalog.categories
    val kaomojis = remember {
        listOf(
            "¯\\_(ツ)_/¯", "(•‿•)", "(╯°□°)╯︵ ┻━┻", "┬─┬ノ( º _ ºノ)",
            "(づ｡◕‿‿◕｡)づ", "ʕ•ᴥ•ʔ", "(¬‿¬)", "ಠ_ಠ",
            "(ง'̀-'́)ง", "✧*｡٩(ˊᗜˋ*)و✧*｡", "(｡•́︿•̀｡)", "(⌐■_■)"
        )
    }
    val symbols = remember {
        listOf("→", "←", "↑", "↓", "✓", "✗", "•", "—", "°", "±", "×", "÷", "₹", "৳", "€", "£")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .background(theme.boardBackground)
            .padding(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Emoji", "Kaomoji", "Symbols").forEachIndexed { idx, title ->
                val selected = tab == idx
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) theme.accentColor else theme.modifierSurface)
                        .clickable { tab = idx }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = title,
                        color = if (selected) contrastTextOn(theme.accentColor) else theme.secondaryText,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = theme.keyFontFamily()
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        when (tab) {
            0 -> {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEachIndexed { idx, cat ->
                        val active = selectedCategoryIdx == idx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (active) theme.accentColor.copy(alpha = 0.25f) else theme.keySurface)
                                .border(
                                    1.dp,
                                    if (active) theme.accentColor else theme.borderColor,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { selectedCategoryIdx = idx }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${cat.icon} ${cat.id}",
                                color = theme.keyText,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    contentPadding = PaddingValues(2.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    items(categories[selectedCategoryIdx].emojis) { emoji ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(theme.keySurface)
                                .clickable { onSelect(emoji) }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 20.sp)
                        }
                    }
                }
            }
            1 -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(kaomojis) { item ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(theme.keySurface)
                            .clickable { onSelect(item) }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(item, color = theme.keyText, fontSize = 13.sp)
                    }
                }
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                contentPadding = PaddingValues(4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(symbols) { sym ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(theme.keySurface)
                            .clickable { onSelect(sym) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(sym, color = theme.keyText, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ClipboardDrawer(
    theme: KeyboardThemeSpec,
    items: List<ClipboardItemEntity>,
    clipboardDao: ClipboardDao,
    clipboardSyncManager: ClipboardSyncManager,
    onPaste: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val systemClip by clipboardSyncManager.lastSystemClip.collectAsState()

    LaunchedEffect(Unit) {
        clipboardSyncManager.syncNow(forceCapture = false)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(225.dp)
            .background(theme.boardBackground)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CLIPBOARD (${items.size})",
                color = theme.secondaryText,
                fontSize = 11.sp,
                fontFamily = theme.keyFontFamily()
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { clipboardSyncManager.syncNow(forceCapture = true) }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = "Sync clipboard",
                        tint = theme.keyText,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text("Sync", color = theme.keyText, fontSize = 11.sp)
                }
                Text(
                    text = "Clear Unpinned",
                    color = theme.accentColor,
                    fontSize = 11.sp,
                    modifier = Modifier.clickable {
                        scope.launch { clipboardDao.clearUnpinned() }
                    }
                )
            }
        }

        if (!systemClip.isNullOrBlank() && items.none { it.text == systemClip }) {
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(theme.modifierSurface)
                    .border(1.dp, theme.accentColor, RoundedCornerShape(8.dp))
                    .clickable {
                        val clipText = systemClip!!
                        scope.launch { clipboardSyncManager.saveClipText(clipText) }
                        onPaste(clipText)
                    }
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SYSTEM: $systemClip",
                    color = theme.keyText,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "PASTE",
                    color = theme.accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = "No saved clips yet. Copy any text or tap Sync.",
                    color = theme.secondaryText,
                    fontSize = 12.sp
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(items, key = { it.id }) { clip ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(theme.keySurface)
                            .border(
                                width = 1.dp,
                                color = if (clip.isPinned) theme.accentColor else theme.borderColor,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onPaste(clip.text) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = clip.text,
                            color = theme.keyText,
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { scope.launch { clipboardDao.setPinned(clip.id, !clip.isPinned) } },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Outlined.PushPin,
                                contentDescription = "Pin",
                                tint = if (clip.isPinned) theme.accentColor else theme.secondaryText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = { scope.launch { clipboardDao.deleteById(clip.id) } },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                contentDescription = "Delete",
                                tint = theme.secondaryText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResizeDrawer(
    theme: KeyboardThemeSpec,
    prefs: KeyboardPreferences,
    prefsRepo: PreferencesRepository
) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.boardBackground)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "KEY HEIGHT: ${(prefs.heightScale * 100).toInt()}%",
            color = theme.keyText,
            fontSize = 12.sp,
            fontFamily = theme.keyFontFamily()
        )
        Slider(
            value = prefs.heightScale,
            onValueChange = { scope.launch { prefsRepo.setHeightScale(it) } },
            valueRange = 0.85f..1.45f,
            colors = SliderDefaults.colors(thumbColor = theme.accentColor, activeTrackColor = theme.accentColor)
        )
        Text(
            text = "KEY LABEL SIZE: ${prefs.keyFontSizeSp.toInt()}sp",
            color = theme.keyText,
            fontSize = 12.sp,
            fontFamily = theme.keyFontFamily()
        )
        Slider(
            value = prefs.keyFontSizeSp,
            onValueChange = { scope.launch { prefsRepo.setKeyFontSizeSp(it) } },
            valueRange = 15f..24f,
            colors = SliderDefaults.colors(thumbColor = theme.accentColor, activeTrackColor = theme.accentColor)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OneHandedMode.entries.forEach { mode ->
                val selected = prefs.oneHandedMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) theme.accentColor else theme.modifierSurface)
                        .clickable { scope.launch { prefsRepo.setOneHandedMode(mode) } }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.name,
                        color = if (selected) contrastTextOn(theme.accentColor) else theme.keyText,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = theme.keyFontFamily()
                    )
                }
            }
        }
    }
}

@Composable
fun VoiceDrawer(
    theme: KeyboardThemeSpec,
    prefs: KeyboardPreferences,
    voiceController: VoiceTypingController
) {
    val state by voiceController.state.collectAsState()
    val locale = if (prefs.activeLanguage == InputLanguage.EN_US) "en-US" else "bn-BD"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(theme.boardBackground)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = when {
                state.isListening -> state.partialText.ifBlank { "Listening ($locale)..." }
                state.errorMessage != null -> state.errorMessage!!
                else -> "Tap mic to dictate ($locale)"
            },
            color = theme.keyText,
            fontSize = 13.sp,
            fontFamily = theme.keyFontFamily()
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                if (state.isListening) voiceController.stopListening()
                else voiceController.startListening(locale)
            },
            colors = ButtonDefaults.buttonColors(containerColor = theme.actionSurface)
        ) {
            Icon(Icons.Outlined.Mic, contentDescription = "Mic")
            Spacer(Modifier.width(6.dp))
            Text(if (state.isListening) "Stop" else "Speak")
        }
    }
}

@Composable
fun TranslateDrawer(
    theme: KeyboardThemeSpec,
    prefs: KeyboardPreferences,
    onInsert: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.boardBackground)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "${prefs.aiProvider.displayName.uppercase()} · EN <-> BN TRANSLATOR",
            color = theme.secondaryText,
            fontSize = 11.sp,
            fontFamily = theme.keyFontFamily()
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                textStyle = TextStyle(color = theme.keyText, fontSize = 14.sp),
                cursorBrush = SolidColor(theme.accentColor),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(theme.keySurface)
                    .border(1.dp, theme.borderColor, RoundedCornerShape(8.dp))
                    .padding(10.dp),
                decorationBox = { inner ->
                    if (input.isEmpty()) Text("Type English, Banglish, or বাংলা...", color = theme.secondaryText, fontSize = 13.sp)
                    inner()
                }
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    if (input.isNotBlank() && !loading) {
                        loading = true
                        errorMsg = null
                        scope.launch {
                            AiRepository.translate(input, prefs)
                                .onSuccess { output = it }
                                .onFailure { errorMsg = it.message }
                            loading = false
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = theme.actionSurface)
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text("Go", fontWeight = FontWeight.Bold)
                }
            }
        }
        if (errorMsg != null) {
            Text(errorMsg!!, color = theme.accentColor, fontSize = 12.sp)
        }
        if (output.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(theme.modifierSurface)
                    .clickable { onInsert(output) }
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(output, color = theme.keyText, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text("INSERT", color = theme.accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── MODULE 4: AI Text Tone Refiner Drawer ────────────────────────────────────

/**
 * Full-featured AI Tone Refiner & Translator drawer.
 *
 * Provides tone chips: Translate, Fix Grammar, Formal, Casual, Polite, Clean Bangla.
 * - Reads text before cursor from the InputConnection via [controller.getTextBeforeCursor]
 *   (or the user may type/paste text manually in the input box).
 * - Shows an inline preview of the AI result.
 * - "Replace" commits via InputConnection; "Discard" closes the preview.
 * - Displays a loading spinner and error messages from the AI call.
 */
@Composable
fun AiRefineDrawer(
    theme: KeyboardThemeSpec,
    prefs: KeyboardPreferences,
    controller: KeyboardController
) {
    val scope = rememberCoroutineScope()
    val aiState by controller.uiState
        .map { it.aiRefine }
        .collectAsState(initial = AiRefineState())

    var inputText by remember { mutableStateOf("") }
    var originalLength by remember { mutableIntStateOf(0) }

    val toneChips = remember {
        listOf(
            AiRepository.ToneMode.TRANSLATE     to "🌐 Translate",
            AiRepository.ToneMode.FIX_GRAMMAR   to "✏️ Fix Grammar",
            AiRepository.ToneMode.FORMAL        to "👔 Formal",
            AiRepository.ToneMode.CASUAL        to "😊 Casual",
            AiRepository.ToneMode.POLITE        to "🙏 Polite",
            AiRepository.ToneMode.CLEAN_BANGLA  to "বাং Clean Bangla"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.boardBackground)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        Text(
            text = "${prefs.aiProvider.displayName.uppercase()} · AI TEXT REFINER",
            color = theme.secondaryText,
            fontSize = 11.sp,
            fontFamily = theme.keyFontFamily()
        )

        // Input field
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = inputText,
                onValueChange = { inputText = it },
                textStyle = TextStyle(color = theme.keyText, fontSize = 13.sp),
                cursorBrush = SolidColor(theme.accentColor),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(theme.keySurface)
                    .border(1.dp, theme.borderColor, RoundedCornerShape(8.dp))
                    .padding(9.dp),
                decorationBox = { inner ->
                    if (inputText.isEmpty()) Text(
                        "Type, paste, or the AI will read text before cursor…",
                        color = theme.secondaryText, fontSize = 12.sp
                    )
                    inner()
                },
                maxLines = 3
            )
        }

        // Tone chips (horizontal scroll)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            toneChips.forEach { (mode, label) ->
                val isLoading = aiState.isLoading
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(theme.modifierSurface)
                        .border(1.dp, theme.accentColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .clickable(enabled = !isLoading) {
                            val text = inputText.trim()
                            originalLength = text.length
                            controller.requestAiRefine(text, mode)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        color = if (isLoading) theme.secondaryText else theme.keyText,
                        fontSize = 11.sp,
                        fontFamily = theme.keyFontFamily()
                    )
                }
            }
        }

        // Loading
        if (aiState.isLoading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = theme.accentColor
                )
                Spacer(Modifier.width(8.dp))
                Text("Processing…", color = theme.secondaryText, fontSize = 12.sp)
            }
        }

        // Error
        aiState.errorMessage?.let { err ->
            Text(
                text = "⚠ $err",
                color = theme.accentColor,
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // AI Preview + Replace / Discard
        aiState.previewText?.let { preview ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(theme.modifierSurface)
                    .border(1.dp, theme.accentColor, RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(preview, color = theme.keyText, fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Replace button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(theme.actionSurface)
                            .clickable {
                                controller.commitAiRefineResult(originalLength)
                                inputText = ""
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "✓ Replace",
                            color = androidx.compose.ui.graphics.Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    // Discard button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(theme.modifierSurface)
                            .border(1.dp, theme.borderColor, RoundedCornerShape(8.dp))
                            .clickable { controller.discardAiRefineResult() }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "✗ Discard",
                            color = theme.secondaryText,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
