package com.datathecodie.witgrid.ui.screens.play

import androidx.compose.ui.unit.dp
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.engine.core.Pack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlayScreenLayoutTest {

    @Test
    fun isLandscapeLayoutIdentifiesOrientation() {
        assertTrue(isLandscapeLayout(840.dp, 360.dp))
        assertTrue(isLandscapeLayout(1024.dp, 600.dp))
        assertFalse(isLandscapeLayout(360.dp, 780.dp))
        assertFalse(isLandscapeLayout(432.dp, 936.dp))
        assertFalse(isLandscapeLayout(600.dp, 600.dp))
        assertFalse(isLandscapeLayout(400.dp, 390.dp))
        assertTrue(isLandscapeLayout(1024.dp, 768.dp))
    }

    @Test
    fun isPlayHeaderCompactDistinguishesPhonePortraitFromLandscape() {
        assertTrue(isPlayHeaderCompact(360.dp, 1.0f))
        assertTrue(isPlayHeaderCompact(432.dp, 1.0f))
        assertFalse(isPlayHeaderCompact(480.dp, 1.0f))
        assertTrue(isPlayHeaderCompact(480.dp, 1.3f))
        assertFalse(isPlayHeaderCompact(840.dp, 1.3f))
        assertFalse(isPlayHeaderCompact(1024.dp, 1.0f))
    }

    @Test
    fun formatDurationFormatsMinutesAndSeconds() {
        assertEquals("0:00", formatDuration(0L))
        assertEquals("0:05", formatDuration(5_000L))
        assertEquals("1:05", formatDuration(65_000L))
        assertEquals("2:03", formatDuration(123_000L))
        assertEquals("61:05", formatDuration(3_665_000L))
    }

    @Test
    fun resourceMappingsResolveSupportedGamesAndPacks() {
        assertEquals(R.string.game_crowns, gameNameResource("crowns"))
        assertEquals(R.string.game_binary, gameNameResource("binary"))
        assertEquals(R.string.game_nonogram, gameNameResource("nonogram"))
        assertEquals(R.string.game_minisudoku, gameNameResource("minisudoku"))
        assertEquals(R.string.game_futoshiki, gameNameResource("futoshiki"))
        assertEquals(R.string.game_unknown, gameNameResource("unknown"))

        assertEquals(R.string.pack_easy, packNameResource(Pack.EASY))
        assertEquals(R.string.pack_medium, packNameResource(Pack.MEDIUM))
        assertEquals(R.string.pack_hard, packNameResource(Pack.HARD))
        assertEquals(R.string.pack_expert, packNameResource(Pack.EXPERT))

        assertEquals(R.string.play_rules_crowns, playRulesResource("crowns"))
        assertEquals(R.string.play_rules_binary, playRulesResource("binary"))
        assertEquals(R.string.play_rules_nonogram, playRulesResource("nonogram"))
        assertEquals(R.string.play_rules_minisudoku, playRulesResource("minisudoku"))
        assertEquals(R.string.play_rules_sudoku, playRulesResource("sudoku"))
        assertEquals(R.string.play_rules_skyscrapers, playRulesResource("skyscrapers"))
        assertEquals(R.string.play_rules_futoshiki, playRulesResource("futoshiki"))
        assertEquals(R.string.game_path, gameNameResource("path"))
        assertEquals(R.string.play_rules_path, playRulesResource("path"))
        assertEquals(R.string.game_arrows, gameNameResource("arrows"))
        assertEquals(R.string.play_rules_arrows, playRulesResource("arrows"))
        assertEquals(R.string.play_rules_minisudoku, playRulesResource("unknown"))
    }
}
