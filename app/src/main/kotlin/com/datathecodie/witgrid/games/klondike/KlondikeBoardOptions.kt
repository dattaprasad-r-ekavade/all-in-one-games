package com.datathecodie.witgrid.games.klondike

import androidx.compose.runtime.staticCompositionLocalOf
import com.datathecodie.witgrid.ui.theme.Colourway

/** Settings the play host can supply without coupling the Klondike board to storage. */
data class KlondikeBoardOptions(
    val hapticsEnabled: Boolean = true,
    val reduceMotion: Boolean = false,
)

val LocalKlondikeBoardOptions = staticCompositionLocalOf { KlondikeBoardOptions() }

/**
 * Hearts and diamonds stay on the rust colourway.
 * The player can pick another accent, and the suits must stay red.
 * Clubs and spades use the theme ink at draw time.
 */
internal fun klondikeRedSuitRgb(darkTheme: Boolean): Int =
    if (darkTheme) Colourway.RUST.darkAccent else Colourway.RUST.lightAccent

/** Card moves snap to the new pile. Reduce motion and full motion both skip travel animation. */
internal fun klondikeMoveDurationMillis(reduceMotion: Boolean): Int = if (reduceMotion) 0 else 0
