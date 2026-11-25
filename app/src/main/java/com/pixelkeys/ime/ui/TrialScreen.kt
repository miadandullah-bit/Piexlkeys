package com.pixelkeys.ime.ui

import android.content.ClipData
import android.content.ClipboardManager
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelkeys.ime.PixelKeysApp
import com.pixelkeys.ime.data.local.ClipboardItemEntity
import com.pixelkeys.ime.data.prefs.InputLanguage
import com.pixelkeys.ime.data.prefs.KeyboardPreferences
import com.pixelkeys.ime.engine.VoiceTypingController
import com.pixelkeys.ime.ime.PlaygroundTargetAdapter
import com.pixelkeys.ime.keyboard.KeyboardController
import com.pixelkeys.ime.keyboard.PixelKeysBoard
import com.pixelkeys.ime.theme.DotMatrixText
import com.pixelkeys.ime.theme.KeyTypographyStyle
import com.pixelkeys.ime.theme.KeyboardThemeSpec
import com.pixelkeys.ime.theme.contrastTextOn
import kotlinx.coroutines.launch

@Composable
fun TrialScreen(
    app: PixelKeysApp,
    prefs: KeyboardPreferences,
    theme: KeyboardThemeSpec,
    clipboardItems: List<ClipboardItemEntity>,
    imeEnabled: Boolean,
    imeSelected: Boolean,
    onOpenThemes: () -> Unit,
    onOpenSettings: () -> Unit
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

    var trialNotice by remember { mutableStateOf<String?>(null) }

    val currentText = textState.value.text
    val charCount = currentText.length
    val wordCount = remember(currentText) {
        currentText.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    DotMatrixText(
                        text = "PIXELKEYS",
                        color = StudioPalette.TextPrimary,
                        fontSize = 18.sp,
                        typographyStyle = KeyTypographyStyle.DOT_MATRIX,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Trial Sandbox · ${theme.name}",
                        color = StudioPalette.TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusPill(
                        label = when {
                            imeEnabled && imeSelected -> "IME Active"
                            imeEnabled -> "Select IME"
                            else -> "Enable IME"
                        },
                        dotColor = when {
                            imeEnabled && imeSelected -> StudioPalette.Success
                            imeEnabled -> StudioPalette.Warning
                            else -> StudioPalette.Danger
                        },
                        onClick = onOpenSettings
                    )
                    StatusPill(
                        label = prefs.activeLanguage.shortLabel,
                        dotColor = theme.accentColor,
                        onClick = {
                            scope.launch { app.preferencesRepository.cycleLanguage(prefs.activeLanguage) }
                        }
                    )
                }
            }

            if (!imeEnabled || !imeSelected) {
                StudioCard(borderColor = theme.accentColor) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (!imeEnabled) "Enable Pixelkeys in Android Settings" else "Switch active keyboard to Pixelkeys",
                                color = StudioPalette.TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Use the buttons below or test directly in this Trial box.",
                                color = StudioPalette.TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!imeEnabled) {
                            Button(
                                onClick = {
                                    context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = theme.accentColor,
                                    contentColor = contrastTextOn(theme.accentColor)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Enable Keyboard", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
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

            // Quick trial actions bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TrialChip("EN Sample") {
                    val sample = "Typing with Pixelkeys feels crisp."
                    textState.value = TextFieldValue(sample, TextRange(sample.length))
                    scope.launch { app.preferencesRepository.setLanguage(InputLanguage.EN_US) }
                }
                TrialChip("Avro Sample") {
                    scope.launch { app.preferencesRepository.setLanguage(InputLanguage.AVRO_BN) }
                    trialNotice = "Switched to Avro. Type 'ami banglay gan gai' below."
                }
                TrialChip("Copy Test Clip") {
                    val sample = "Copied from Pixelkeys Trial"
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    cm?.setPrimaryClip(ClipData.newPlainText("pixelkeys", sample))
                    app.clipboardSyncManager.syncNow(forceCapture = true)
                    trialNotice = "Copied to clipboard. Tap the clipboard icon on the keyboard bar."
                }
                TrialChip("Themes") { onOpenThemes() }
                if (currentText.isNotEmpty()) {
                    TrialChip("Clear") {
                        textState.value = TextFieldValue("")
                        controller.resetSession()
                    }
                }
            }

            if (trialNotice != null) {
                Text(
                    text = trialNotice!!,
                    color = theme.accentColor,
                    fontSize = 11.sp,
                    modifier = Modifier.clickable { trialNotice = null }
                )
            }

            // Live Trial Text Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(StudioPalette.Surface)
                    .border(1.dp, StudioPalette.Border, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "INPUT BUFFER",
                        color = StudioPalette.TextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "$wordCount words · $charCount chars",
                        color = StudioPalette.TextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    BasicTextField(
                        value = textState.value,
                        onValueChange = { textState.value = it },
                        textStyle = TextStyle(color = StudioPalette.TextPrimary, fontSize = 16.sp),
                        cursorBrush = SolidColor(theme.accentColor),
                        modifier = Modifier.fillMaxSize(),
                        decorationBox = { inner ->
                            if (currentText.isEmpty()) {
                                Text(
                                    text = "Tap keys below to test typing, Avro phonetic, clipboard paste, swipe, or spacebar trackpad.",
                                    color = StudioPalette.TextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                            inner()
                        }
                    )
                }
            }
        }

        PixelKeysBoard(
            controller = controller,
            prefs = prefs,
            prefsRepo = app.preferencesRepository,
            theme = theme,
            clipboardItems = clipboardItems,
            clipboardDao = app.database.clipboardDao(),
            clipboardSyncManager = app.clipboardSyncManager,
            voiceController = voiceController
        )
    }
}

@Composable
private fun TrialChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(StudioPalette.SurfaceElevated)
            .border(1.dp, StudioPalette.Border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = StudioPalette.TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
