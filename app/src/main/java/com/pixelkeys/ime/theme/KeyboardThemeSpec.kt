package com.pixelkeys.ime.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import com.pixelkeys.ime.keyboard.KeySpec
import com.pixelkeys.ime.keyboard.KeyType

fun contrastTextOn(background: Color): Color =
    if (background.luminance() > 0.42f) Color(0xFF141416) else Color.White

enum class KeyTypographyStyle {
    DOT_MATRIX,
    HARDWARE_MONO,
    CLEAN_SANS
}

data class KeyColorSwatch(
    val background: Color,
    val text: Color,
    val border: Color
)

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
    val isDark: Boolean = true,
    val spaceKeySurface: Color = keySurface,
    val spaceKeyText: Color = keyText,
    val suggestionDividerColor: Color = borderColor,
    val keyColorBlocks: List<KeyColorSwatch>? = null
) {
    fun keyFontFamily(): FontFamily = when (typographyStyle) {
        KeyTypographyStyle.DOT_MATRIX -> FontFamily.Monospace
        KeyTypographyStyle.HARDWARE_MONO -> FontFamily.Monospace
        KeyTypographyStyle.CLEAN_SANS -> FontFamily.SansSerif
    }

    /**
     * Resolves background, foreground text, and border for a key.
     * Supports uniform themes and color-blocked themes (such as Pixel Block).
     */
    fun resolveKeySwatch(rowIndex: Int, colIndex: Int, key: KeySpec, isShiftActive: Boolean): KeyColorSwatch {
        val blocks = keyColorBlocks
        if (blocks == null || blocks.isEmpty()) {
            return when (key.type) {
                KeyType.ACTION -> KeyColorSwatch(actionSurface, Color.White, actionSurface)
                KeyType.SHIFT -> KeyColorSwatch(
                    background = if (isShiftActive) accentColor.copy(alpha = 0.25f) else modifierSurface,
                    text = keyText,
                    border = borderColor
                )
                KeyType.BACKSPACE, KeyType.SYMBOLS, KeyType.EMOJI, KeyType.LANGUAGE ->
                    KeyColorSwatch(modifierSurface, keyText, borderColor)
                KeyType.SPACE -> KeyColorSwatch(spaceKeySurface, spaceKeyText, borderColor)
                KeyType.CHAR -> KeyColorSwatch(keySurface, keyText, borderColor)
            }
        }

        // Color-block pattern inspired by Mondrian / Nothing Bauhaus layout:
        // blocks indices: 0 = Yellow, 1 = Blue, 2 = Black, 3 = Cream, 4 = Red
        fun swatchAt(idx: Int): KeyColorSwatch = blocks[idx % blocks.size]

        return when (key.type) {
            KeyType.SPACE -> KeyColorSwatch(spaceKeySurface, spaceKeyText, Color(0xFF3A3A3F))
            KeyType.ACTION -> KeyColorSwatch(actionSurface, Color.White, actionSurface)
            KeyType.SYMBOLS -> swatchAt(4) // Crimson Red
            KeyType.SHIFT -> if (isShiftActive) {
                swatchAt(4)
            } else {
                swatchAt(0)
            }
            KeyType.BACKSPACE -> swatchAt(3) // Cream
            KeyType.EMOJI -> swatchAt(2) // Matte Black
            KeyType.LANGUAGE -> swatchAt(1) // Cobalt Blue
            KeyType.CHAR -> when (key.id) {
                "comma" -> swatchAt(3)
                "period" -> swatchAt(4)
                else -> {
                    val pattern = when (rowIndex % 4) {
                        0 -> intArrayOf(0, 1, 2, 3, 4, 1, 3, 4, 0, 2)
                        1 -> intArrayOf(3, 4, 3, 4, 0, 4, 1, 2, 2, 3)
                        2 -> intArrayOf(2, 0, 3, 4, 1, 2, 0, 4, 1, 3)
                        else -> intArrayOf(1, 3, 0, 4, 2, 3, 1, 0, 4, 2)
                    }
                    swatchAt(pattern[colIndex % pattern.size])
                }
            }
        }
    }
}

object BuiltInThemes {
    private val pixelBlockSwatches = listOf(
        // 0: Mustard Yellow
        KeyColorSwatch(
            background = Color(0xFFE3B736),
            text = Color(0xFF141416),
            border = Color(0xFFC99D22)
        ),
        // 1: Cobalt Blue
        KeyColorSwatch(
            background = Color(0xFF204A8E),
            text = Color(0xFFFFFFFF),
            border = Color(0xFF3362AE)
        ),
        // 2: Matte Black
        KeyColorSwatch(
            background = Color(0xFF141416),
            text = Color(0xFFFFFFFF),
            border = Color(0xFF2B2B30)
        ),
        // 3: Warm Cream
        KeyColorSwatch(
            background = Color(0xFFEFECE4),
            text = Color(0xFF141416),
            border = Color(0xFFD4CFC3)
        ),
        // 4: Crimson Red
        KeyColorSwatch(
            background = Color(0xFFC5362C),
            text = Color(0xFFFFFFFF),
            border = Color(0xFFDE4B40)
        )
    )

    val PixelBlock = KeyboardThemeSpec(
        id = "pixel_block",
        name = "Pixel Block",
        boardBackground = Color(0xFF000000),
        keySurface = Color(0xFF141416),
        modifierSurface = Color(0xFF232327),
        actionSurface = Color(0xFFC5362C),
        keyText = Color(0xFFFFFFFF),
        secondaryText = Color(0xFFC2C2CA),
        accentColor = Color(0xFFE3B736),
        borderColor = Color(0xFF2D2D32),
        keyCornerRadiusDp = 10f,
        isHardwareBevel = false,
        typographyStyle = KeyTypographyStyle.DOT_MATRIX,
        isDark = true,
        spaceKeySurface = Color(0xFF29292D),
        spaceKeyText = Color(0xFFEFECE4),
        suggestionDividerColor = Color(0xFFE3B736),
        keyColorBlocks = pixelBlockSwatches
    )

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
        PixelBlock,
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
