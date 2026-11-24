package com.pixelkeys.ime

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelkeys.ime.ai.AiRepository
import com.pixelkeys.ime.data.local.CustomThemeEntity
import com.pixelkeys.ime.data.prefs.HapticStyle
import com.pixelkeys.ime.data.prefs.InputLanguage
import com.pixelkeys.ime.data.prefs.KeyboardPreferences
import com.pixelkeys.ime.data.prefs.OneHandedMode
import com.pixelkeys.ime.engine.VoiceTypingController
import com.pixelkeys.ime.ime.PlaygroundTargetAdapter
import com.pixelkeys.ime.keyboard.KeyboardController
import com.pixelkeys.ime.keyboard.PixelKeysBoard
import com.pixelkeys.ime.theme.BuiltInThemes
import com.pixelkeys.ime.theme.KeyboardThemeSpec
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as PixelKeysApp

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                PixelKeysStudioScreen(app)
            }
        }
    }
}

@Composable
fun PixelKeysStudioScreen(app: PixelKeysApp) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs by app.preferencesRepository.preferencesFlow.collectAsState(initial = KeyboardPreferences())
    val customThemes by app.database.customThemeDao().observeAll().collectAsState(initial = emptyList())
    val clipboardItems by app.database.clipboardDao().observeAll().collectAsState(initial = emptyList())

    val dynamicYou = BuiltInThemes.resolveDynamicYou(context)
    val activeTheme = when (prefs.activeThemeId) {
        "dynamic_you" -> dynamicYou
        else -> BuiltInThemes.allBuiltIns.firstOrNull { it.id == prefs.activeThemeId }
            ?: customThemes.firstOrNull { it.id == prefs.activeThemeId }?.toThemeSpec()
            ?: BuiltInThemes.NothingDark
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = Color(0xFF0A0A0C),
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF121215)) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Outlined.Keyboard, contentDescription = "Playground") },
                    label = { Text("Playground") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Outlined.Palette, contentDescription = "Themes") },
                    label = { Text("Themes") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Outlined.Tune, contentDescription = "Haptics") },
                    label = { Text("Haptics") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (selectedTab) {
                0 -> PlaygroundTab(app, prefs, activeTheme, clipboardItems)
                1 -> ThemesTab(app, prefs, dynamicYou, customThemes) { id ->
                    scope.launch { app.preferencesRepository.setThemeId(id) }
                }
                else -> HapticsAndLayoutTab(app, prefs)
            }
        }
    }
}

@Composable
private fun PlaygroundTab(
    app: PixelKeysApp,
    prefs: KeyboardPreferences,
    theme: KeyboardThemeSpec,
    clipboardItems: List<com.pixelkeys.ime.data.local.ClipboardItemEntity>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val textState = remember { mutableStateOf(TextFieldValue("")) }
    val adapter = remember { PlaygroundTargetAdapter(textState) }
    val controller = remember {
        KeyboardController(
            scope = scope,
            prefsRepo = app.preferencesRepository,
            hapticController = app.hapticController,
            target = adapter
        )
    }
    controller.updatePreferences(prefs)

    val voiceController = remember {
        VoiceTypingController(context) { spoken ->
            controller.commitTextDirectly(spoken)
        }
    }

    var showSetupBanner by remember { mutableStateOf(true) }
    val imeEnabled = remember { isImeEnabled(context) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PIXEL KEYS // STUDIO",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = prefs.activeLanguage.displayName,
                    color = theme.accentColor,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.clickable {
                        scope.launch { app.preferencesRepository.cycleLanguage(prefs.activeLanguage) }
                    }
                )
            }

            if (showSetupBanner && !imeEnabled) {
                ImeSetupCard(
                    onEnableClick = {
                        context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                    },
                    onSelectClick = {
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.showInputMethodPicker()
                    },
                    onDismiss = { showSetupBanner = false }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF141418))
                    .border(1.dp, Color(0xFF2B2B33), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                BasicTextField(
                    value = textState.value,
                    onValueChange = { textState.value = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                    cursorBrush = SolidColor(theme.accentColor),
                    modifier = Modifier.fillMaxSize(),
                    decorationBox = { inner ->
                        if (textState.value.text.isEmpty()) {
                            Text(
                                text = "Tap below to test EN, AVRO (e.g. 'ami banglay gan gai'), বাংলা, long-press keys for symbols, hold Backspace to delete, or hold Spacebar for cursor trackpad...",
                                color = Color(0xFF7C7C85),
                                fontSize = 14.sp
                            )
                        }
                        inner()
                    }
                )
            }
        }

        PixelKeysBoard(
            controller = controller,
            prefs = prefs,
            prefsRepo = app.preferencesRepository,
            theme = theme,
            clipboardItems = clipboardItems,
            clipboardDao = app.database.clipboardDao(),
            voiceController = voiceController
        )
    }
}

@Composable
private fun ImeSetupCard(
    onEnableClick: () -> Unit,
    onSelectClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1B1B20))
            .border(1.dp, Color(0xFFD71921), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enable Pixel Keys System-Wide", color = Color.White, fontWeight = FontWeight.SemiBold)
            Text("Dismiss", color = Color(0xFF8E8E93), fontSize = 12.sp, modifier = Modifier.clickable(onClick = onDismiss))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onEnableClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD71921)),
                modifier = Modifier.weight(1f)
            ) {
                Text("1. Enable IME", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = onSelectClick,
                modifier = Modifier.weight(1f)
            ) {
                Text("2. Switch Input", fontSize = 12.sp, color = Color.White)
            }
        }
    }
}

@Composable
private fun ThemesTab(
    app: PixelKeysApp,
    prefs: KeyboardPreferences,
    dynamicYou: KeyboardThemeSpec,
    customThemes: List<CustomThemeEntity>,
    onSelectTheme: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var apiKeyDraft by remember(prefs.geminiApiKey) { mutableStateOf(prefs.geminiApiKey) }
    var aiPrompt by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }
    var apiKeySavedMsg by remember { mutableStateOf(false) }

    val allPresets = BuiltInThemes.allBuiltIns + dynamicYou

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "GEMINI AI THEME STUDIO",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Gemini API Key (free at aistudio.google.com/apikey)",
                color = Color(0xFF9E9EA7),
                fontSize = 11.sp
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = apiKeyDraft,
                    onValueChange = {
                        apiKeyDraft = it
                        apiKeySavedMsg = false
                    },
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp, fontFamily = FontFamily.Monospace),
                    cursorBrush = SolidColor(Color(0xFFD71921)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1A1A1F))
                        .border(1.dp, Color(0xFF33333C), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    decorationBox = { inner ->
                        if (apiKeyDraft.isEmpty()) Text("Paste AIza... key here", color = Color.Gray, fontSize = 12.sp)
                        inner()
                    }
                )
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            app.preferencesRepository.setGeminiApiKey(apiKeyDraft)
                            apiKeySavedMsg = true
                        }
                    }
                ) {
                    Text(if (apiKeySavedMsg) "Saved" else "Save Key", color = Color.White, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = aiPrompt,
                    onValueChange = { aiPrompt = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    cursorBrush = SolidColor(Color(0xFFD71921)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1A1A1F))
                        .border(1.dp, Color(0xFF33333C), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    decorationBox = { inner ->
                        if (aiPrompt.isEmpty()) Text("Describe a theme (e.g. Cyberpunk Tokyo neon amber)...", color = Color.Gray, fontSize = 13.sp)
                        inner()
                    }
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (aiPrompt.isNotBlank() && !isGenerating) {
                            isGenerating = true
                            aiError = null
                            scope.launch {
                                val effectiveKey = apiKeyDraft.trim().ifEmpty { prefs.geminiApiKey }
                                if (effectiveKey.isNotEmpty() && effectiveKey != prefs.geminiApiKey) {
                                    app.preferencesRepository.setGeminiApiKey(effectiveKey)
                                }
                                AiRepository.generateThemeWithGemini(aiPrompt, effectiveKey)
                                    .onSuccess { generated ->
                                        app.database.customThemeDao().upsert(generated)
                                        app.preferencesRepository.setThemeId(generated.id)
                                        aiPrompt = ""
                                    }
                                    .onFailure { err ->
                                        aiError = err.message
                                    }
                                isGenerating = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD71921))
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text("Generate")
                    }
                }
            }
            if (aiError != null) {
                Spacer(Modifier.height(6.dp))
                Text(aiError!!, color = Color(0xFFFF6B6B), fontSize = 12.sp)
            }
        }

        items(allPresets, key = { it.id }) { preset ->
            ThemePresetCard(
                spec = preset,
                selected = prefs.activeThemeId == preset.id,
                onClick = { onSelectTheme(preset.id) }
            )
        }

        if (customThemes.isNotEmpty()) {
            item {
                Text(
                    text = "GEMINI GENERATED THEMES",
                    color = Color(0xFF8E8E93),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            items(customThemes, key = { it.id }) { entity ->
                val spec = entity.toThemeSpec()
                ThemePresetCard(
                    spec = spec,
                    selected = prefs.activeThemeId == spec.id,
                    onClick = { onSelectTheme(spec.id) }
                )
            }
        }
    }
}

@Composable
private fun ThemePresetCard(
    spec: KeyboardThemeSpec,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(spec.boardBackground)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) spec.accentColor else spec.borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(spec.name, color = spec.keyText, fontWeight = FontWeight.Bold, fontFamily = spec.keyFontFamily())
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(18.dp).clip(CircleShape).background(spec.keySurface).border(1.dp, spec.borderColor, CircleShape))
                Box(Modifier.size(18.dp).clip(CircleShape).background(spec.modifierSurface))
                Box(Modifier.size(18.dp).clip(CircleShape).background(spec.actionSurface))
            }
        }
        if (selected) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = "Selected", tint = spec.accentColor)
        }
    }
}

@Composable
private fun HapticsAndLayoutTab(
    app: PixelKeysApp,
    prefs: KeyboardPreferences
) {
    val scope = rememberCoroutineScope()
    val repo = app.preferencesRepository

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "HAPTICS & LAYOUT LAB",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        item {
            Text("Key Height Scale: ${(prefs.heightScale * 100).toInt()}%", color = Color.White)
            Slider(
                value = prefs.heightScale,
                onValueChange = { scope.launch { repo.setHeightScale(it) } },
                valueRange = 0.85f..1.45f
            )
        }

        item {
            Text("Key Label Size: ${prefs.keyFontSizeSp.toInt()}sp", color = Color.White)
            Slider(
                value = prefs.keyFontSizeSp,
                onValueChange = { scope.launch { repo.setKeyFontSizeSp(it) } },
                valueRange = 15f..24f
            )
        }

        item {
            Text("Vibration Duration: ${prefs.hapticStrengthMs} ms", color = Color.White)
            Slider(
                value = prefs.hapticStrengthMs.toFloat(),
                onValueChange = { scope.launch { repo.setHapticStrength(it.toInt()) } },
                valueRange = 0f..50f
            )
        }

        item {
            Text("Haptic Profile", color = Color.White)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HapticStyle.entries.forEach { style ->
                    val selected = prefs.hapticStyle == style
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) Color(0xFFD71921) else Color(0xFF1D1D22))
                            .clickable { scope.launch { repo.setHapticStyle(style) } }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(style.name, color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Text("Default Language", color = Color.White)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InputLanguage.entries.forEach { lang ->
                    val selected = prefs.activeLanguage == lang
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) Color(0xFFD71921) else Color(0xFF1D1D22))
                            .clickable { scope.launch { repo.setLanguage(lang) } }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(lang.shortLabel, color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            SettingToggleRow("Show Number Row", prefs.showNumberRow) {
                scope.launch { repo.setShowNumberRow(it) }
            }
            SettingToggleRow("Autocorrect (Close English Words Only)", prefs.autocorrectEnabled) {
                scope.launch { repo.setAutocorrectEnabled(it) }
            }
            SettingToggleRow("Key Click Sound", prefs.soundEnabled) {
                scope.launch { repo.setSoundEnabled(it) }
            }
        }

        item {
            Text("One-Handed Alignment", color = Color.White)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OneHandedMode.entries.forEach { mode ->
                    val selected = prefs.oneHandedMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) Color(0xFFD71921) else Color(0xFF1D1D22))
                            .clickable { scope.launch { repo.setOneHandedMode(mode) } }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(mode.name, color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 14.sp)
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

private fun isImeEnabled(context: Context): Boolean {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager ?: return false
    return imm.enabledInputMethodList.any { it.packageName == context.packageName }
}
