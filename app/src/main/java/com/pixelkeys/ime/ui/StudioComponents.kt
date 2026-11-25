package com.pixelkeys.ime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelkeys.ime.keyboard.KeySpec
import com.pixelkeys.ime.keyboard.KeyType
import com.pixelkeys.ime.theme.DotMatrixText
import com.pixelkeys.ime.theme.KeyboardThemeSpec
import com.pixelkeys.ime.theme.contrastTextOn

object StudioPalette {
    val Background = Color(0xFF09090B)
    val Surface = Color(0xFF131317)
    val SurfaceElevated = Color(0xFF1B1B21)
    val Border = Color(0xFF282832)
    val TextPrimary = Color(0xFFF4F4F6)
    val TextSecondary = Color(0xFF9C9CA8)
    val AccentDefault = Color(0xFFE3B736)
    val Success = Color(0xFF34C759)
    val Warning = Color(0xFFE3B736)
    val Danger = Color(0xFFC5362C)
}

@Composable
fun StudioCard(
    modifier: Modifier = Modifier,
    borderColor: Color = StudioPalette.Border,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(StudioPalette.Surface)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
fun StudioSectionHeader(
    title: String,
    subtitle: String? = null,
    accent: Color = StudioPalette.AccentDefault
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title.uppercase(),
            color = accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                color = StudioPalette.TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun StatusPill(
    label: String,
    dotColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val base = modifier
        .clip(RoundedCornerShape(8.dp))
        .background(StudioPalette.SurfaceElevated)
        .border(1.dp, StudioPalette.Border, RoundedCornerShape(8.dp))
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
        .padding(horizontal = 10.dp, vertical = 6.dp)

    Row(
        modifier = base,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = label,
            color = StudioPalette.TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun StudioToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    accent: Color,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onToggle(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = StudioPalette.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(description, color = StudioPalette.TextSecondary, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = contrastTextOn(accent),
                checkedTrackColor = accent
            )
        )
    }
}

@Composable
fun <T> SegmentedOptionRow(
    items: List<T>,
    selected: T,
    labelOf: (T) -> String,
    accent: Color,
    onSelect: (T) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            val isSelected = item == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) accent else StudioPalette.SurfaceElevated)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) accent else StudioPalette.Border,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelect(item) }
                    .padding(vertical = 10.dp, horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = labelOf(item),
                    color = if (isSelected) contrastTextOn(accent) else StudioPalette.TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun ThemeMiniKeyboardPreview(
    spec: KeyboardThemeSpec,
    modifier: Modifier = Modifier
) {
    val sampleKeys = listOf(
        KeySpec("p1", "1", "1"),
        KeySpec("p2", "Q", "Q"),
        KeySpec("p3", "W", "W"),
        KeySpec("p4", "E", "E"),
        KeySpec("p5", "R", "R"),
        KeySpec("p6", "⇧", type = KeyType.SHIFT),
        KeySpec("p7", "space", type = KeyType.SPACE, weight = 1.8f),
        KeySpec("p8", "↵", type = KeyType.ACTION)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(spec.boardBackground)
            .border(1.dp, spec.borderColor, RoundedCornerShape(10.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            sampleKeys.forEachIndexed { idx, key ->
                val swatch = spec.resolveKeySwatch(rowIndex = idx % 2, colIndex = idx, key = key, isShiftActive = false)
                Box(
                    modifier = Modifier
                        .weight(key.weight)
                        .height(30.dp)
                        .clip(RoundedCornerShape((spec.keyCornerRadiusDp * 0.7f).dp))
                        .background(swatch.background)
                        .border(0.8.dp, swatch.border, RoundedCornerShape((spec.keyCornerRadiusDp * 0.7f).dp)),
                    contentAlignment = Alignment.Center
                ) {
                    DotMatrixText(
                        text = key.label,
                        color = swatch.text,
                        fontSize = if (key.type == KeyType.SPACE) 9.sp else 12.sp,
                        typographyStyle = spec.typographyStyle,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(Modifier.height(0.dp))
    }
}
