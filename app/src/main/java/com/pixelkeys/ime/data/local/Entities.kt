package com.pixelkeys.ime.data.local

import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pixelkeys.ime.theme.KeyTypographyStyle
import com.pixelkeys.ime.theme.KeyboardThemeSpec

@Entity(tableName = "custom_themes")
data class CustomThemeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val prompt: String,
    val boardBgArgb: Long,
    val keyBgArgb: Long,
    val modifierBgArgb: Long,
    val actionBgArgb: Long,
    val keyTextArgb: Long,
    val secondaryTextArgb: Long,
    val accentArgb: Long,
    val borderArgb: Long,
    val cornerRadiusDp: Float,
    val isHardwareBevel: Boolean,
    val typographyStyle: String,
    val isDark: Boolean,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toThemeSpec(): KeyboardThemeSpec = KeyboardThemeSpec(
        id = id,
        name = name,
        boardBackground = Color(boardBgArgb.toInt()),
        keySurface = Color(keyBgArgb.toInt()),
        modifierSurface = Color(modifierBgArgb.toInt()),
        actionSurface = Color(actionBgArgb.toInt()),
        keyText = Color(keyTextArgb.toInt()),
        secondaryText = Color(secondaryTextArgb.toInt()),
        accentColor = Color(accentArgb.toInt()),
        borderColor = Color(borderArgb.toInt()),
        keyCornerRadiusDp = cornerRadiusDp,
        isHardwareBevel = isHardwareBevel,
        typographyStyle = runCatching { KeyTypographyStyle.valueOf(typographyStyle) }
            .getOrDefault(KeyTypographyStyle.DOT_MATRIX),
        isDark = isDark
    )
}

@Entity(tableName = "clipboard_items")
data class ClipboardItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val isPinned: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_words")
data class UserWordEntity(
    @PrimaryKey val word: String,
    val language: String,
    val frequency: Int = 1,
    val lastUsedAt: Long = System.currentTimeMillis()
)
