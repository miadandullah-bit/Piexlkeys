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
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.pixelkeys.ime.data.prefs.InputLanguage
import com.pixelkeys.ime.data.prefs.KeyboardPreferences
import com.pixelkeys.ime.data.prefs.OneHandedMode
import com.pixelkeys.ime.data.prefs.PreferencesRepository
import com.pixelkeys.ime.engine.VoiceTypingController
import com.pixelkeys.ime.theme.KeyboardThemeSpec
import kotlinx.coroutines.launch

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
    val emojis = remember {
        listOf(
            "😀", "😂", "🥹", "😍", "🤩", "😎", "🤔", "😴",
            "🔥", "✨", "❤️", "💯", "👍", "🙏", "🎉", "🚀",
            "🇧🇩", "🎹", "⚡", "🌙", "☕", "🎧", "💻", "📱"
        )
    }
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
            .height(210.dp)
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
                        color = if (selected) Color.White else theme.secondaryText,
                        fontSize = 12.sp,
                        fontFamily = theme.keyFontFamily()
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        when (tab) {
            0 -> LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                contentPadding = PaddingValues(4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(emojis) { emoji ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(theme.keySurface)
                            .clickable { onSelect(emoji) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 20.sp)
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
    onPaste: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
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
            Text(
                text = "Clear Unpinned",
                color = theme.accentColor,
                fontSize = 11.sp,
                modifier = Modifier.clickable {
                    scope.launch { clipboardDao.clearUnpinned() }
                }
            )
        }
        Spacer(Modifier.height(6.dp))
        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No saved clips yet", color = theme.secondaryText, fontSize = 13.sp)
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
                        color = if (selected) Color.White else theme.keyText,
                        fontSize = 11.sp,
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
            text = "GEMINI EN ↔ BN TRANSLATOR",
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
                            AiRepository.translateWithGemini(input, prefs.geminiApiKey)
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
