package com.pixelkeys.ime.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Lightweight 5x7 dot-matrix bitmap renderer for ASCII characters and core IME glyphs.
 * Falls back cleanly to standard Compose Text for non-ASCII scripts (e.g. Bengali) or emojis.
 */
object DotMatrixFont {

    private fun g(vararg rows: String): IntArray = IntArray(7) { r ->
        var bits = 0
        val row = rows.getOrElse(r) { "....." }.padEnd(5, '.')
        for (c in 0 until 5) {
            if (row[c] == '#') {
                bits = bits or (1 shl (4 - c))
            }
        }
        bits
    }

    private val glyphs: Map<Char, IntArray> = buildMap {
        // Space
        put(' ', g(".....", ".....", ".....", ".....", ".....", ".....", "....."))

        // Digits 0-9
        put('0', g(".###.", "#...#", "#..##", "#.#.#", "##..#", "#...#", ".###."))
        put('1', g("..#..", ".##..", "..#..", "..#..", "..#..", "..#..", ".###."))
        put('2', g(".###.", "#...#", "....#", "..##.", ".#...", "#....", "#####"))
        put('3', g("####.", "....#", "....#", ".###.", "....#", "....#", "####."))
        put('4', g("...#.", "..##.", ".#.#.", "#..#.", "#####", "...#.", "...#."))
        put('5', g("#####", "#....", "####.", "....#", "....#", "#...#", ".###."))
        put('6', g(".###.", "#....", "#....", "####.", "#...#", "#...#", ".###."))
        put('7', g("#####", "....#", "...#.", "..#..", ".#...", ".#...", ".#..."))
        put('8', g(".###.", "#...#", "#...#", ".###.", "#...#", "#...#", ".###."))
        put('9', g(".###.", "#...#", "#...#", ".####", "....#", "....#", ".###."))

        // Uppercase A-Z
        put('A', g(".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"))
        put('B', g("####.", "#...#", "#...#", "####.", "#...#", "#...#", "####."))
        put('C', g(".###.", "#...#", "#....", "#....", "#....", "#...#", ".###."))
        put('D', g("####.", "#...#", "#...#", "#...#", "#...#", "#...#", "####."))
        put('E', g("#####", "#....", "#....", "####.", "#....", "#....", "#####"))
        put('F', g("#####", "#....", "#....", "####.", "#....", "#....", "#...."))
        put('G', g(".###.", "#...#", "#....", "#.###", "#...#", "#...#", ".###."))
        put('H', g("#...#", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"))
        put('I', g(".###.", "..#..", "..#..", "..#..", "..#..", "..#..", ".###."))
        put('J', g("..###", "...#.", "...#.", "...#.", "#..#.", "#..#.", ".##.."))
        put('K', g("#...#", "#..#.", "#.#..", "##...", "#.#..", "#..#.", "#...#"))
        put('L', g("#....", "#....", "#....", "#....", "#....", "#....", "#####"))
        put('M', g("#...#", "##.##", "#.#.#", "#...#", "#...#", "#...#", "#...#"))
        put('N', g("#...#", "##..#", "#.#.#", "#..##", "#...#", "#...#", "#...#"))
        put('O', g(".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."))
        put('P', g("####.", "#...#", "#...#", "####.", "#....", "#....", "#...."))
        put('Q', g(".###.", "#...#", "#...#", "#...#", "#.#.#", "#..#.", ".##.#"))
        put('R', g("####.", "#...#", "#...#", "####.", "#.#..", "#..#.", "#...#"))
        put('S', g(".####", "#....", "#....", ".###.", "....#", "....#", "####."))
        put('T', g("#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#.."))
        put('U', g("#...#", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."))
        put('V', g("#...#", "#...#", "#...#", "#...#", "#...#", ".#.#.", "..#.."))
        put('W', g("#...#", "#...#", "#...#", "#...#", "#.#.#", "##.##", "#...#"))
        put('X', g("#...#", "#...#", ".#.#.", "..#..", ".#.#.", "#...#", "#...#"))
        put('Y', g("#...#", "#...#", ".#.#.", "..#..", "..#..", "..#..", "..#.."))
        put('Z', g("#####", "....#", "...#.", "..#..", ".#...", "#....", "#####"))

        // Lowercase a-z
        put('a', g(".....", ".....", ".###.", "....#", ".####", "#...#", ".####"))
        put('b', g("#....", "#....", "####.", "#...#", "#...#", "#...#", "####."))
        put('c', g(".....", ".....", ".###.", "#...#", "#....", "#...#", ".###."))
        put('d', g("....#", "....#", ".####", "#...#", "#...#", "#...#", ".####"))
        put('e', g(".....", ".....", ".###.", "#...#", "#####", "#....", ".###."))
        put('f', g("..##.", ".#...", "####.", ".#...", ".#...", ".#...", ".#..."))
        put('g', g(".....", ".####", "#...#", "#...#", ".####", "....#", ".###."))
        put('h', g("#....", "#....", "####.", "#...#", "#...#", "#...#", "#...#"))
        put('i', g("..#..", ".....", ".##..", "..#..", "..#..", "..#..", ".###."))
        put('j', g("...#.", ".....", "..##.", "...#.", "...#.", "#..#.", ".##.."))
        put('k', g("#....", "#....", "#..#.", "#.#..", "##...", "#.#..", "#..#."))
        put('l', g(".##..", "..#..", "..#..", "..#..", "..#..", "..#..", ".###."))
        put('m', g(".....", ".....", "##.#.", "#.#.#", "#.#.#", "#...#", "#...#"))
        put('n', g(".....", ".....", "####.", "#...#", "#...#", "#...#", "#...#"))
        put('o', g(".....", ".....", ".###.", "#...#", "#...#", "#...#", ".###."))
        put('p', g(".....", "####.", "#...#", "#...#", "####.", "#....", "#...."))
        put('q', g(".....", ".####", "#...#", "#...#", ".####", "....#", "....#"))
        put('r', g(".....", ".....", "#.##.", "##...", "#....", "#....", "#...."))
        put('s', g(".....", ".....", ".####", "#....", ".###.", "....#", "####."))
        put('t', g(".#...", ".#...", "####.", ".#...", ".#...", ".#..#", "..##."))
        put('u', g(".....", ".....", "#...#", "#...#", "#...#", "#...#", ".####"))
        put('v', g(".....", ".....", "#...#", "#...#", "#...#", ".#.#.", "..#.."))
        put('w', g(".....", ".....", "#...#", "#...#", "#.#.#", "#.#.#", ".#.#."))
        put('x', g(".....", ".....", "#...#", ".#.#.", "..#..", ".#.#.", "#...#"))
        put('y', g(".....", "#...#", "#...#", ".####", "....#", "...#.", ".##.."))
        put('z', g(".....", ".....", "#####", "...#.", "..#..", ".#...", "#####"))

        // Punctuation & Symbols
        put('@', g(".###.", "#...#", "#.###", "#.#.#", "#.###", "#....", ".###."))
        put('#', g(".#.#.", ".#.#.", "#####", ".#.#.", "#####", ".#.#.", ".#.#."))
        put('$', g("..#..", ".####", "#.#..", ".###.", "..#.#", "####.", "..#.."))
        put('%', g("##..#", "##.#.", "..#..", "..#..", "..#..", ".#.##", "#..##"))
        put('&', g(".##..", "#..#.", "#.#..", ".#...", "#.#.#", "#..#.", ".##.#"))
        put('-', g(".....", ".....", ".....", "#####", ".....", ".....", "....."))
        put('+', g(".....", "..#..", "..#..", "#####", "..#..", "..#..", "....."))
        put('[', g(".###.", ".#...", ".#...", ".#...", ".#...", ".#...", ".###."))
        put(']', g(".###.", "...#.", "...#.", "...#.", "...#.", "...#.", ".###."))
        put('(', g("..#..", ".#...", "#....", "#....", "#....", ".#...", "..#.."))
        put(')', g("..#..", "...#.", "....#", "....#", "....#", "...#.", "..#.."))
        put('{', g("..##.", ".#...", ".#...", "#....", ".#...", ".#...", "..##."))
        put('}', g(".##..", "...#.", "...#.", "....#", "...#.", "...#.", ".##.."))
        put('=', g(".....", ".....", "#####", ".....", "#####", ".....", "....."))
        put('\\', g("#....", ".#...", "..#..", "..#..", "...#.", "....#", "....."))
        put('/', g("....#", "...#.", "..#..", "..#..", ".#...", "#....", "....."))
        put('<', g("...#.", "..#..", ".#...", "#....", ".#...", "..#..", "...#."))
        put('>', g(".#...", "..#..", "...#.", "....#", "...#.", "..#..", ".#..."))
        put('*', g(".....", "#.#.#", ".###.", "#####", ".###.", "#.#.#", "....."))
        put('"', g(".#.#.", ".#.#.", ".....", ".....", ".....", ".....", "....."))
        put('\'', g("..#..", "..#..", ".....", ".....", ".....", ".....", "....."))
        put(':', g(".....", "..#..", ".....", ".....", ".....", "..#..", "....."))
        put(';', g(".....", "..#..", ".....", ".....", "..#..", "..#..", ".#..."))
        put('!', g("..#..", "..#..", "..#..", "..#..", "..#..", ".....", "..#.."))
        put('?', g(".###.", "#...#", "....#", "..##.", "..#..", ".....", "..#.."))
        put('.', g(".....", ".....", ".....", ".....", ".....", "..#..", "..#.."))
        put(',', g(".....", ".....", ".....", ".....", "..#..", "..#..", ".#..."))
        put('_', g(".....", ".....", ".....", ".....", ".....", ".....", "#####"))
        put('~', g(".....", ".....", ".#...", "#.#.#", "...#.", ".....", "....."))
        put('`', g(".#...", "..#..", ".....", ".....", ".....", ".....", "....."))
        put('|', g("..#..", "..#..", "..#..", "..#..", "..#..", "..#..", "..#.."))
        put('^', g("..#..", ".#.#.", "#...#", ".....", ".....", ".....", "....."))

        // Special key icons
        put('⇧', g("..#..", ".#.#.", "#...#", "##.##", ".#.#.", ".#.#.", ".###."))
        put('⬆', g("..#..", ".###.", "#####", ".###.", ".###.", ".###.", ".###."))
        put('⌫', g(".####", "#...#", "#.#.#", "#..#.", "#.#.#", "#...#", ".####"))
        put('↵', g("....#", "....#", ".#..#", "#...#", "#####", ".#...", "..#.."))
        put('•', g(".....", ".....", "..#..", ".###.", "..#..", ".....", "....."))
        put('◀', g("...#.", "..##.", ".###.", "####.", ".###.", "..##.", "...#."))
        put('▶', g(".#...", ".##..", ".###.", "####.", ".###.", ".##..", ".#..."))
    }

    fun supports(text: String): Boolean {
        if (text.isEmpty()) return false
        return text.all { it in glyphs }
    }

    fun glyphFor(ch: Char): IntArray? = glyphs[ch]
}

@Composable
fun DotMatrixText(
    text: String,
    color: Color,
    fontSize: TextUnit,
    typographyStyle: KeyTypographyStyle,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Medium,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val useDotMatrix = typographyStyle == KeyTypographyStyle.DOT_MATRIX &&
        maxLines == 1 &&
        DotMatrixFont.supports(text)

    if (!useDotMatrix) {
        val fallbackFamily = when (typographyStyle) {
            KeyTypographyStyle.DOT_MATRIX -> FontFamily.Monospace
            KeyTypographyStyle.HARDWARE_MONO -> FontFamily.Monospace
            KeyTypographyStyle.CLEAN_SANS -> FontFamily.SansSerif
        }
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontFamily = fallbackFamily,
            maxLines = maxLines,
            overflow = overflow,
            modifier = modifier
        )
        return
    }

    val density = LocalDensity.current
    val glyphHeightDp = with(density) { fontSize.toDp() } * 0.88f
    val cellDp = glyphHeightDp / 7f
    val charWidthCells = 5
    val charGapCells = 1.4f
    val totalCellsWidth = if (text.isEmpty()) 0f else {
        text.length * charWidthCells + (text.length - 1) * charGapCells
    }
    val totalWidthDp = cellDp * totalCellsWidth

    val glyphArrays = remember(text) {
        text.map { DotMatrixFont.glyphFor(it) }
    }

    BoxWithConstraints(
        modifier = modifier.clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        val fittedWidthDp = if (maxWidth.value.isFinite() && maxWidth < totalWidthDp) {
            maxWidth
        } else {
            totalWidthDp.coerceAtLeast(1.dp)
        }
        val scale = if (totalWidthDp > 0.dp) (fittedWidthDp / totalWidthDp).coerceIn(0.4f, 1f) else 1f
        val fittedHeightDp = (glyphHeightDp * scale).coerceAtLeast(1.dp)

        Canvas(modifier = Modifier.size(width = fittedWidthDp, height = fittedHeightDp)) {
            val cellPx = size.height / 7f
            val radius = (cellPx * 0.42f).coerceAtLeast(0.8f)
            val charAdvancePx = (charWidthCells + charGapCells) * cellPx

            glyphArrays.forEachIndexed { charIndex, rows ->
                if (rows == null) return@forEachIndexed
                val baseX = charIndex * charAdvancePx
                for (r in 0 until 7) {
                    val rowBits = rows[r]
                    if (rowBits == 0) continue
                    val cy = r * cellPx + cellPx * 0.5f
                    for (c in 0 until 5) {
                        if ((rowBits and (1 shl (4 - c))) != 0) {
                            val cx = baseX + c * cellPx + cellPx * 0.5f
                            drawCircle(
                                color = color,
                                radius = radius,
                                center = Offset(cx, cy)
                            )
                        }
                    }
                }
            }
        }
    }
}
