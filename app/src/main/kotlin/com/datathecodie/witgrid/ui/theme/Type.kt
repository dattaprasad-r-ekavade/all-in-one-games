package com.datathecodie.witgrid.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.datathecodie.witgrid.R

val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

private const val TABULAR_NUMERALS = "tnum"

private fun style(weight: FontWeight, size: Int, line: Int, tabular: Boolean = false) = TextStyle(
    fontFamily = InterFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontFeatureSettings = if (tabular) TABULAR_NUMERALS else null,
)

/** Number and clue styles. All use tabular numerals so digits do not shift. */
object NumberStyles {
    val clue = style(FontWeight.Medium, 14, 18, tabular = true)
    val cell = style(FontWeight.SemiBold, 20, 24, tabular = true)
    val timer = style(FontWeight.Medium, 16, 20, tabular = true)
    val display = style(FontWeight.SemiBold, 40, 48, tabular = true)
}

val WitgridTypography = Typography(
    displayLarge = style(FontWeight.SemiBold, 40, 48),
    headlineMedium = style(FontWeight.SemiBold, 28, 36),
    titleLarge = style(FontWeight.SemiBold, 22, 28),
    titleMedium = style(FontWeight.Medium, 16, 24),
    bodyLarge = style(FontWeight.Normal, 16, 24),
    bodyMedium = style(FontWeight.Normal, 14, 20),
    labelLarge = style(FontWeight.Medium, 14, 20),
    labelMedium = style(FontWeight.Medium, 14, 18),
    labelSmall = style(FontWeight.Medium, 14, 16),
)
