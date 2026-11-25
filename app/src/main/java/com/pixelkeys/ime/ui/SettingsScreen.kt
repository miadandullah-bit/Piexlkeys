package com.pixelkeys.ime.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelkeys.ime.PixelKeysApp
import com.pixelkeys.ime.ai.AiRepository
import com.pixelkeys.ime.data.prefs.AiProvider
import com.pixelkeys.ime.data.prefs.HapticStyle
import com.pixelkeys.ime.data.prefs.InputLanguage
import com.pixelkeys.ime.data.prefs.KeyboardPreferences
import com.pixelkeys.ime.data.prefs.OneHandedMode
import com.pixelkeys.ime.theme.DotMatrixText
import com.pixelkeys.ime.theme.KeyTypographyStyle
import com.pixelkeys.ime.theme.KeyboardThemeSpec
import com.pixelkeys.ime.theme.contrastTextOn
import kotlinx.coroutines.launch

enum class SettingsCategory(val label: String) {
    ALL("All"),
    LAYOUT("Layout"),
    TYPING("Typing"),
    HAPTICS("Haptics"),
    AI_SERVICE("AI Service"),
    CLIPBOARD("Clipboard")
}

@Composable
fun SettingsScreen(
    app: PixelKeysApp,
    prefs: KeyboardPreferences,
    theme: KeyboardThemeSpec,
    clipboardCount: Int,
    imeEnabled: Boolean,
    imeSelected: Boolean,
    selectedCategory: SettingsCategory,
    onCategoryChange: (SettingsCategory) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = app.preferencesRepository
    val accent = theme.accentColor

    var apiKeyDraft by remember(prefs.aiProvider, prefs.apiKeyFor()) {
        mutableStateOf(prefs.apiKeyFor())
    }
    var modelDraft by remember(prefs.aiProvider, prefs.modelFor()) {
        mutableStateOf(prefs.modelFor())
    }
    var baseUrlDraft by remember(prefs.customAiBaseUrl) {
        mutableStateOf(prefs.customAiBaseUrl)
    }
    var showApiKey by remember { mutableStateOf(false) }
    var aiStatusMsg by remember { mutableStateOf<String?>(null) }
    var aiStatusIsError by remember { mutableStateOf(false) }
    var testingAi by remember { mutableStateOf(false) }
    var clipStatusMsg by remember { mutableStateOf<String?>(null) }

    fun showSection(cat: SettingsCategory): Boolean =
        selectedCategory == SettingsCategory.ALL || selectedCategory == cat

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DotMatrixText(
                    text = "SETTINGS",
                    color = StudioPalette.TextPrimary,
                    fontSize = 18.sp,
                    typographyStyle = KeyTypographyStyle.DOT_MATRIX,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SettingsCategory.entries.forEach { category ->
                        val active = selectedCategory == category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) accent else StudioPalette.SurfaceElevated)
                                .border(
                                    1.dp,
                                    if (active) accent else StudioPalette.Border,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onCategoryChange(category) }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = category.label,
                                color = if (active) contrastTextOn(accent) else StudioPalette.TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        if (selectedCategory == SettingsCategory.ALL) {
            item {
                StudioCard {
                    StudioSectionHeader(
                        title = "System Keyboard Status",
                        subtitle = when {
                            imeEnabled && imeSelected -> "Pixelkeys is enabled and active"
                            imeEnabled -> "Enabled in system settings; select Pixelkeys as active"
                            else -> "Enable Pixelkeys in Android Input Settings to use in other apps"
                        },
                        accent = accent
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent,
                                contentColor = contrastTextOn(accent)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Input Settings", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = {
                                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                imm?.showInputMethodPicker()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Switch Keyboard", fontSize = 12.sp, color = StudioPalette.TextPrimary)
                        }
                    }
                }
            }
        }

        if (showSection(SettingsCategory.LAYOUT)) {
            item {
                StudioCard {
                    StudioSectionHeader(
                        title = "Keyboard Layout & Sizing",
                        subtitle = "Adjust proportions, keycaps, and one-handed alignment",
                        accent = accent
                    )

                    Text(
                        text = "Key Height: ${(prefs.heightScale * 100).toInt()}%",
                        color = StudioPalette.TextPrimary,
                        fontSize = 13.sp
                    )
                    Slider(
                        value = prefs.heightScale,
                        onValueChange = { scope.launch { repo.setHeightScale(it) } },
                        valueRange = 0.85f..1.45f,
                        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                    )

                    Text(
                        text = "Key Label Size: ${prefs.keyFontSizeSp.toInt()}sp",
                        color = StudioPalette.TextPrimary,
                        fontSize = 13.sp
                    )
                    Slider(
                        value = prefs.keyFontSizeSp,
                        onValueChange = { scope.launch { repo.setKeyFontSizeSp(it) } },
                        valueRange = 15f..24f,
                        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                    )

                    StudioToggleRow(
                        title = "Dedicated Number Row",
                        description = "Pin digits 1 to 0 above the QWERTY row",
                        checked = prefs.showNumberRow,
                        accent = accent
                    ) { scope.launch { repo.setShowNumberRow(it) } }

                    StudioToggleRow(
                        title = "Dedicated Emoji Key",
                        description = "Show quick emoji key on the bottom bar",
                        checked = prefs.showEmojiKey,
                        accent = accent
                    ) { scope.launch { repo.setShowEmojiKey(it) } }

                    Text(
                        text = "One-Handed Mode",
                        color = StudioPalette.TextPrimary,
                        fontSize = 13.sp
                    )
                    SegmentedOptionRow(
                        items = OneHandedMode.entries,
                        selected = prefs.oneHandedMode,
                        labelOf = { it.name },
                        accent = accent,
                        onSelect = { scope.launch { repo.setOneHandedMode(it) } }
                    )
                }
            }
        }

        if (showSection(SettingsCategory.TYPING)) {
            item {
                StudioCard {
                    StudioSectionHeader(
                        title = "Language & Typing",
                        subtitle = "Default keyboard layout, Avro phonetic, and predictions",
                        accent = accent
                    )

                    SegmentedOptionRow(
                        items = InputLanguage.entries,
                        selected = prefs.activeLanguage,
                        labelOf = { it.shortLabel },
                        accent = accent,
                        onSelect = { scope.launch { repo.setLanguage(it) } }
                    )

                    StudioToggleRow(
                        title = "Banglish-Safe Autocorrect",
                        description = "Fixes close English typos while keeping Banglish words untouched",
                        checked = prefs.autocorrectEnabled,
                        accent = accent
                    ) { scope.launch { repo.setAutocorrectEnabled(it) } }

                    StudioToggleRow(
                        title = "Swipe Typing",
                        description = "Decode continuous glide paths in English mode",
                        checked = prefs.swipeTypingEnabled,
                        accent = accent
                    ) { scope.launch { repo.setSwipeTypingEnabled(it) } }
                }
            }
        }

        if (showSection(SettingsCategory.HAPTICS)) {
            item {
                StudioCard {
                    StudioSectionHeader(
                        title = "Haptics & Key Audio",
                        subtitle = "Tune tactile feedback strength and click profile",
                        accent = accent
                    )

                    Text(
                        text = "Vibration Duration: ${prefs.hapticStrengthMs} ms",
                        color = StudioPalette.TextPrimary,
                        fontSize = 13.sp
                    )
                    Slider(
                        value = prefs.hapticStrengthMs.toFloat(),
                        onValueChange = { scope.launch { repo.setHapticStrength(it.toInt()) } },
                        valueRange = 0f..50f,
                        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                    )

                    Text(
                        text = "Haptic Profile",
                        color = StudioPalette.TextPrimary,
                        fontSize = 13.sp
                    )
                    SegmentedOptionRow(
                        items = HapticStyle.entries,
                        selected = prefs.hapticStyle,
                        labelOf = { it.name },
                        accent = accent,
                        onSelect = { scope.launch { repo.setHapticStyle(it) } }
                    )

                    StudioToggleRow(
                        title = "Key Click Sound",
                        description = "Play system click tone on key tap",
                        checked = prefs.soundEnabled,
                        accent = accent
                    ) { scope.launch { repo.setSoundEnabled(it) } }

                    OutlinedButton(
                        onClick = {
                            app.hapticController.performKeyPress(
                                durationMs = prefs.hapticStrengthMs,
                                style = prefs.hapticStyle,
                                soundEnabled = prefs.soundEnabled
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Test Key Tap", color = StudioPalette.TextPrimary, fontSize = 12.sp)
                    }
                }
            }
        }

        if (showSection(SettingsCategory.AI_SERVICE)) {
            item {
                StudioCard {
                    StudioSectionHeader(
                        title = "AI Service Provider",
                        subtitle = "Used for AI Theme Generator and EN <-> BN Translator",
                        accent = accent
                    )

                    // Multi-provider chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AiProvider.entries.forEach { provider ->
                            val selected = prefs.aiProvider == provider
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) accent else StudioPalette.SurfaceElevated)
                                    .border(
                                        1.dp,
                                        if (selected) accent else StudioPalette.Border,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        aiStatusMsg = null
                                        scope.launch { repo.setAiProvider(provider) }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = provider.displayName,
                                    color = if (selected) contrastTextOn(accent) else StudioPalette.TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Text(
                        text = "Key source: ${prefs.aiProvider.helpSource}",
                        color = StudioPalette.TextSecondary,
                        fontSize = 11.sp
                    )

                    // API Key input with visibility toggle
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BasicTextField(
                            value = apiKeyDraft,
                            onValueChange = {
                                apiKeyDraft = it
                                aiStatusMsg = null
                            },
                            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                            textStyle = TextStyle(
                                color = StudioPalette.TextPrimary,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            cursorBrush = SolidColor(accent),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(StudioPalette.SurfaceElevated)
                                .border(1.dp, StudioPalette.Border, RoundedCornerShape(10.dp))
                                .padding(11.dp),
                            decorationBox = { inner ->
                                if (apiKeyDraft.isEmpty()) {
                                    Text(
                                        text = "Paste ${prefs.aiProvider.keyHint} key",
                                        color = StudioPalette.TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                                inner()
                            }
                        )
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                imageVector = if (showApiKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (showApiKey) "Hide API key" else "Show API key",
                                tint = StudioPalette.TextSecondary
                            )
                        }
                    }

                    // Model preset chips
                    if (prefs.aiProvider.presetModels.isNotEmpty()) {
                        Text("Model Preset", color = StudioPalette.TextPrimary, fontSize = 12.sp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            prefs.aiProvider.presetModels.forEach { presetModel ->
                                val selected = modelDraft.trim() == presetModel
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) accent.copy(alpha = 0.22f) else StudioPalette.SurfaceElevated)
                                        .border(
                                            1.dp,
                                            if (selected) accent else StudioPalette.Border,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            modelDraft = presetModel
                                            scope.launch { repo.setProviderModel(prefs.aiProvider, presetModel) }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = presetModel,
                                        color = if (selected) accent else StudioPalette.TextSecondary,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    // Custom Model ID input
                    Text("Model ID", color = StudioPalette.TextPrimary, fontSize = 12.sp)
                    BasicTextField(
                        value = modelDraft,
                        onValueChange = {
                            modelDraft = it
                            aiStatusMsg = null
                        },
                        textStyle = TextStyle(
                            color = StudioPalette.TextPrimary,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        cursorBrush = SolidColor(accent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(StudioPalette.SurfaceElevated)
                            .border(1.dp, StudioPalette.Border, RoundedCornerShape(10.dp))
                            .padding(11.dp),
                        decorationBox = { inner ->
                            if (modelDraft.isEmpty()) {
                                Text(prefs.aiProvider.defaultModel, color = StudioPalette.TextSecondary, fontSize = 12.sp)
                            }
                            inner()
                        }
                    )

                    if (prefs.aiProvider == AiProvider.CUSTOM) {
                        Text("Base URL (OpenAI-compatible)", color = StudioPalette.TextPrimary, fontSize = 12.sp)
                        BasicTextField(
                            value = baseUrlDraft,
                            onValueChange = {
                                baseUrlDraft = it
                                aiStatusMsg = null
                            },
                            textStyle = TextStyle(
                                color = StudioPalette.TextPrimary,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            cursorBrush = SolidColor(accent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(StudioPalette.SurfaceElevated)
                                .border(1.dp, StudioPalette.Border, RoundedCornerShape(10.dp))
                                .padding(11.dp),
                            decorationBox = { inner ->
                                if (baseUrlDraft.isEmpty()) {
                                    Text("https://api.openai.com/v1", color = StudioPalette.TextSecondary, fontSize = 12.sp)
                                }
                                inner()
                            }
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    repo.setProviderApiKey(prefs.aiProvider, apiKeyDraft)
                                    repo.setProviderModel(prefs.aiProvider, modelDraft)
                                    if (prefs.aiProvider == AiProvider.CUSTOM) {
                                        repo.setCustomAiBaseUrl(baseUrlDraft)
                                    }
                                    aiStatusIsError = false
                                    aiStatusMsg = "Saved ${prefs.aiProvider.displayName} settings."
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save Config", color = StudioPalette.TextPrimary, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                if (!testingAi) {
                                    testingAi = true
                                    aiStatusMsg = null
                                    scope.launch {
                                        repo.setProviderApiKey(prefs.aiProvider, apiKeyDraft)
                                        repo.setProviderModel(prefs.aiProvider, modelDraft)
                                        if (prefs.aiProvider == AiProvider.CUSTOM) {
                                            repo.setCustomAiBaseUrl(baseUrlDraft)
                                        }
                                        val updated = when (prefs.aiProvider) {
                                            AiProvider.GEMINI -> prefs.copy(geminiApiKey = apiKeyDraft.trim(), geminiModel = modelDraft.trim())
                                            AiProvider.GROQ -> prefs.copy(groqApiKey = apiKeyDraft.trim(), groqModel = modelDraft.trim())
                                            AiProvider.OPENAI -> prefs.copy(openaiApiKey = apiKeyDraft.trim(), openaiModel = modelDraft.trim())
                                            AiProvider.OPENROUTER -> prefs.copy(openrouterApiKey = apiKeyDraft.trim(), openrouterModel = modelDraft.trim())
                                            AiProvider.CUSTOM -> prefs.copy(
                                                customAiApiKey = apiKeyDraft.trim(),
                                                customAiModel = modelDraft.trim(),
                                                customAiBaseUrl = baseUrlDraft.trim()
                                            )
                                        }
                                        AiRepository.testConnection(updated)
                                            .onSuccess { msg ->
                                                aiStatusIsError = false
                                                aiStatusMsg = msg
                                            }
                                            .onFailure { err ->
                                                aiStatusIsError = true
                                                aiStatusMsg = err.message ?: "Connection failed"
                                            }
                                        testingAi = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent,
                                contentColor = contrastTextOn(accent)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (testingAi) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = contrastTextOn(accent)
                                )
                            } else {
                                Text("Save & Test AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (aiStatusMsg != null) {
                        Text(
                            text = aiStatusMsg!!,
                            color = if (aiStatusIsError) Color(0xFFFF6B6B) else StudioPalette.Success,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        if (showSection(SettingsCategory.CLIPBOARD)) {
            item {
                StudioCard {
                    StudioSectionHeader(
                        title = "Clipboard History",
                        subtitle = "Saved clips in database: $clipboardCount",
                        accent = accent
                    )

                    StudioToggleRow(
                        title = "Auto-Capture System Clipboard",
                        description = "Record copied text whenever Pixelkeys or Trial opens",
                        checked = prefs.clipboardAutoCapture,
                        accent = accent
                    ) { scope.launch { repo.setClipboardAutoCapture(it) } }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                app.clipboardSyncManager.syncNow(forceCapture = true)
                                clipStatusMsg = "Synced system clipboard."
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Sync Now", color = StudioPalette.TextPrimary, fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    app.database.clipboardDao().clearUnpinned()
                                    clipStatusMsg = "Cleared unpinned clips."
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Clear Unpinned", color = Color(0xFFFF6B6B), fontSize = 12.sp)
                        }
                    }

                    if (clipStatusMsg != null) {
                        Text(clipStatusMsg!!, color = StudioPalette.TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
