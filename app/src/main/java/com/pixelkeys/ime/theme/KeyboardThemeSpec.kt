package com.pixelkeys.ime.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

enum class KeyTypographyStyle {
    DOT_MATRIX,
    HARDWARE_MONO,
    CLEAN_SANS
}

data class KeyboardThemeSpec(
    val id: String,
    val name: String,
    val boardBackground: Color,
    val keySurface: Color,
    val modifierSurface: Color,
    val actionSurface: Color,
    val keyText: Color,
    val secondaryText: Color,
    val accentColor: Color,
    val borderColor: Color,
    val keyCornerRadiusDp: Float,
    val keySpacingDp: Float = 5f,
    val isHardwareBevel: Boolean = false,
    val typographyStyle: KeyTypographyStyle = KeyTypographyStyle.DOT_MATRIX,
    val isDark: Boolean = true
) {
    fun keyFontFamily(): FontFamily = when (typographyStyle) {
        KeyTypographyStyle.DOT_MATRIX -> FontFamily.Monospace
        KeyTypographyStyle.HARDWARE_MONO -> FontFamily.Monospace
        KeyTypographyStyle.CLEAN_SANS -> FontFamily.SansSerif
    }
}

object BuiltInThemes {
    val NothingDark = KeyboardThemeSpec(
        id = "nothing_dark",
        name = "Nothing Dark",
        boardBackground = Color(0xFF000000),
        keySurface = Color(0xFF141416),
        modifierSurface = Color(0xFF1F1F23),
        actionSurface = Color(0xFFD71921),
        keyText = Color(0xFFFFFFFF),
        secondaryText = Color(0xFF8E8E93),
        accentColor = Color(0xFFD71921),
        borderColor = Color(0xFF2C2C30),
        keyCornerRadiusDp = 12f,
        isHardwareBevel = false,
        typographyStyle = KeyTypographyStyle.DOT_MATRIX,
        isDark = true
    )

    val NothingLight = KeyboardThemeSpec(
        id = "nothing_light",
        name = "Nothing Light",
        boardBackground = Color(0xFFF2F2F4),
        keySurface = Color(0xFFFFFFFF),
        modifierSurface = Color(0xFFE3E4E8),
        actionSurface = Color(0xFFD71921),
        keyText = Color(0xFF111113),
        secondaryText = Color(0xFF636366),
        accentColor = Color(0xFFD71921),
        borderColor = Color(0xFFD1D1D6),
        keyCornerRadiusDp = 12f,
        isHardwareBevel = false,
        typographyStyle = KeyTypographyStyle.DOT_MATRIX,
        isDark = false
    )

    val HardwareMinimal = KeyboardThemeSpec(
        id = "hardware_minimal",
        name = "Hardware Minimal",
        boardBackground = Color(0xFFE3E5E8),
        keySurface = Color(0xFFF7F8FA),
        modifierSurface = Color(0xFFD5D8DD),
        actionSurface = Color(0xFFE84A27),
        keyText = Color(0xFF1E2229),
        secondaryText = Color(0xFF5C6370),
        accentColor = Color(0xFFE84A27),
        borderColor = Color(0xFFBFC4CC),
        keyCornerRadiusDp = 6f,
        isHardwareBevel = true,
        typographyStyle = KeyTypographyStyle.HARDWARE_MONO,
        isDark = false
    )

    val allBuiltIns: List<KeyboardThemeSpec> = listOf(
        NothingDark,
        NothingLight,
        HardwareMinimal
    )

    @Composable
    fun resolveDynamicYou(context: Context): KeyboardThemeSpec {
        val dark = isSystemInDarkTheme()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val scheme = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            KeyboardThemeSpec(
                id = "dynamic_you",
                name = "Dynamic You",
                boardBackground = scheme.surfaceContainerLowest,
                keySurface = scheme.surfaceContainerHigh,
                modifierSurface = scheme.secondaryContainer,
                actionSurface = scheme.primary,
                keyText = scheme.onSurface,
                secondaryText = scheme.onSurfaceVariant,
                accentColor = scheme.primary,
                borderColor = scheme.outlineVariant,
                keyCornerRadiusDp = 10f,
                isHardwareBevel = false,
                typographyStyle = KeyTypographyStyle.CLEAN_SANS,
                isDark = dark
            )
        } else {
            if (dark) NothingDark.copy(id = "dynamic_you", name = "Dynamic You")
            else NothingLight.copy(id = "dynamic_you", name = "Dynamic You")
        }
    }
}
