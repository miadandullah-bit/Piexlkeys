package com.pixelkeys.ime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelkeys.ime.PixelKeysApp
import com.pixelkeys.ime.ai.AiRepository
import com.pixelkeys.ime.data.local.CustomThemeEntity
import com.pixelkeys.ime.data.prefs.AiProvider
import com.pixelkeys.ime.data.prefs.KeyboardPreferences
import com.pixelkeys.ime.theme.BuiltInThemes
import com.pixelkeys.ime.theme.DotMatrixText
import com.pixelkeys.ime.theme.KeyTypographyStyle
import com.pixelkeys.ime.theme.KeyboardThemeSpec
import com.pixelkeys.ime.theme.contrastTextOn
import kotlinx.coroutines.launch

@Composable
fun ThemesScreen(
    app: PixelKeysApp,
    prefs: KeyboardPreferences,
    activeTheme: KeyboardThemeSpec,
    dynamicYou: KeyboardThemeSpec,
    customThemes: List<CustomThemeEntity>,
    onSelectTheme: (String) -> Unit,
    onOpenAiSettings: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var aiPrompt by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }

    val allPresets = BuiltInThemes.allBuiltIns + dynamicYou
    val activeKeyPresent = prefs.apiKeyFor().isNotBlank() || prefs.aiProvider == AiProvider.CUSTOM

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    DotMatrixText(
                        text = "THEMES",
                        color = StudioPalette.TextPrimary,
                        fontSize = 18.sp,
                        typographyStyle = KeyTypographyStyle.DOT_MATRIX,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Active: ${activeTheme.name}",
                        color = StudioPalette.TextSecondary,
                        fontSize = 12.sp
                    )
                }
                StatusPill(
                    label = prefs.aiProvider.displayName,
                    dotColor = if (activeKeyPresent) StudioPalette.Success else StudioPalette.Warning,
                    onClick = onOpenAiSettings
                )
            }
        }

        item {
            StudioCard {
                StudioSectionHeader(
                    title = "Active Theme Preview",
                    subtitle = "Style: ${activeTheme.typographyStyle.name.replace('_', ' ')}",
                    accent = activeTheme.accentColor
                )
                ThemeMiniKeyboardPreview(spec = activeTheme)
            }
        }

        item {
            StudioSectionHeader(
                title = "Built-In Themes",
                subtitle = "Tap any palette to apply across system apps and Trial",
                accent = activeTheme.accentColor
            )
        }

        items(allPresets, key = { it.id }) { preset ->
            ThemePresetCard(
                spec = preset,
                subtitle = when (preset.id) {
                    "pixel_block" -> "Bauhaus color blocks + dot-matrix keys"
                    "nothing_dark" -> "Matte pitch black + dot-matrix red accent"
                    "nothing_light" -> "Clean daylight surface + dot-matrix keys"
                    "hardware_minimal" -> "Beveled mechanical keycaps + mono labels"
                    else -> "Adapts to your Android wallpaper palette"
                },
                selected = prefs.activeThemeId == preset.id,
                onClick = { onSelectTheme(preset.id) }
            )
        }

        item {
            StudioCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StudioSectionHeader(
                        title = "AI Theme Generator",
                        subtitle = "${prefs.aiProvider.displayName} (${prefs.modelFor()})",
                        accent = activeTheme.accentColor
                    )
                    OutlinedButton(onClick = onOpenAiSettings) {
                        Text("Change AI", fontSize = 11.sp, color = StudioPalette.TextPrimary)
                    }
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Cyberpunk Tokyo amber and charcoal",
                        "Matcha paper ink green",
                        "Retro CRT phosphor green"
                    ).forEach { suggestion ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(StudioPalette.SurfaceElevated)
                                .border(1.dp, StudioPalette.Border, RoundedCornerShape(8.dp))
                                .clickable { aiPrompt = suggestion }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(suggestion, color = StudioPalette.TextSecondary, fontSize = 11.sp)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = aiPrompt,
                        onValueChange = {
                            aiPrompt = it
                            aiError = null
                        },
                        textStyle = TextStyle(color = StudioPalette.TextPrimary, fontSize = 14.sp),
                        cursorBrush = SolidColor(activeTheme.accentColor),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(StudioPalette.SurfaceElevated)
                            .border(1.dp, StudioPalette.Border, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        decorationBox = { inner ->
                            if (aiPrompt.isEmpty()) {
                                Text(
                                    text = "Describe palette, mood, or material...",
                                    color = StudioPalette.TextSecondary,
                                    fontSize = 13.sp
                                )
                            }
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
                                    AiRepository.generateTheme(aiPrompt, prefs)
                                        .onSuccess { generated ->
                                            app.database.customThemeDao().upsert(generated)
                                            app.preferencesRepository.setThemeId(generated.id)
                                            aiPrompt = ""
                                        }
                                        .onFailure { err ->
                                            aiError = err.message ?: "Failed to generate theme"
                                        }
                                    isGenerating = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = activeTheme.accentColor,
                            contentColor = contrastTextOn(activeTheme.accentColor)
                        )
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = contrastTextOn(activeTheme.accentColor)
                            )
                        } else {
                            Text("Generate", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (!activeKeyPresent) {
                    Text(
                        text = "No API key saved for ${prefs.aiProvider.displayName}. Tap Change AI to add a key or switch to Groq / Gemini / OpenAI.",
                        color = StudioPalette.Warning,
                        fontSize = 11.sp
                    )
                }

                if (aiError != null) {
                    Text(
                        text = aiError!!,
                        color = Color(0xFFFF6B6B),
                        fontSize = 12.sp
                    )
                }
            }
        }

        if (customThemes.isNotEmpty()) {
            item {
                StudioSectionHeader(
                    title = "Custom AI Themes (${customThemes.size})",
                    accent = activeTheme.accentColor
                )
            }
            items(customThemes, key = { it.id }) { entity ->
                val spec = entity.toThemeSpec()
                ThemePresetCard(
                    spec = spec,
                    subtitle = entity.prompt,
                    selected = prefs.activeThemeId == spec.id,
                    onClick = { onSelectTheme(spec.id) },
                    onDelete = {
                        scope.launch {
                            if (prefs.activeThemeId == spec.id) {
                                app.preferencesRepository.setThemeId(BuiltInThemes.PixelBlock.id)
                            }
                            app.database.customThemeDao().deleteById(entity.id)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ThemePresetCard(
    spec: KeyboardThemeSpec,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Column(
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
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = spec.name,
                    color = spec.keyText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = spec.keyFontFamily()
                )
                Text(
                    text = subtitle,
                    color = spec.secondaryText,
                    fontSize = 11.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete theme",
                            tint = spec.secondaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (selected) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = "Selected",
                        tint = spec.accentColor
                    )
                }
            }
        }

        // Swatch strip
        val swatches = spec.keyColorBlocks?.map { it.background } ?: listOf(
            spec.keySurface,
            spec.modifierSurface,
            spec.actionSurface,
            spec.accentColor
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            swatches.forEach { swatchColor ->
                Box(
                    Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(swatchColor)
                        .border(1.dp, spec.borderColor, CircleShape)
                )
            }
        }
    }
}
