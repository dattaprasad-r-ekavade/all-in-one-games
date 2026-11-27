package com.datathecodie.witgrid.nav

import com.datathecodie.witgrid.data.settings.AppSettings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlayBoardOptionsTest {
    @Test
    fun sudokuBoardOptionsFromSettings_mapsHapticsAndMotion() {
        val settings = AppSettings(hapticsEnabled = false, reduceMotion = true)
        val options = sudokuBoardOptionsFromSettings(settings)
        assertFalse(options.hapticsEnabled)
        assertTrue(options.reduceMotion)
        assertFalse(options.autoNotesEnabled) // Should use default
    }

    @Test
    fun sudokuBoardOptionsFromSettings_usesDefaultAutoNotes() {
        val settings = AppSettings(hapticsEnabled = true, reduceMotion = false)
        val options = sudokuBoardOptionsFromSettings(settings)
        assertTrue(options.hapticsEnabled)
        assertFalse(options.reduceMotion)
        assertFalse(options.autoNotesEnabled)
    }

    @Test
    fun killerSudokuBoardOptionsFromSettings_mapsHapticsAndMotion() {
        val settings = AppSettings(hapticsEnabled = true, reduceMotion = false)
        val options = killerSudokuBoardOptionsFromSettings(settings)
        assertTrue(options.hapticsEnabled)
        assertFalse(options.reduceMotion)
        assertFalse(options.autoNotesEnabled) // Should use default
    }

    @Test
    fun killerSudokuBoardOptionsFromSettings_disablesHaptics() {
        val settings = AppSettings(hapticsEnabled = false, reduceMotion = true)
        val options = killerSudokuBoardOptionsFromSettings(settings)
        assertFalse(options.hapticsEnabled)
        assertTrue(options.reduceMotion)
    }

    @Test
    fun futoshikiBoardOptionsFromSettings_mapsHapticsAndMotion() {
        val settings = AppSettings(hapticsEnabled = false, reduceMotion = false)
        val options = futoshikiBoardOptionsFromSettings(settings)
        assertFalse(options.hapticsEnabled)
        assertFalse(options.reduceMotion)
        assertFalse(options.autoNotesEnabled)
    }

    @Test
    fun futoshikiBoardOptionsFromSettings_enablesHapticsAndMotion() {
        val settings = AppSettings(hapticsEnabled = true, reduceMotion = true)
        val options = futoshikiBoardOptionsFromSettings(settings)
        assertTrue(options.hapticsEnabled)
        assertTrue(options.reduceMotion)
    }

    @Test
    fun skyscrapersBoardOptionsFromSettings_mapsHapticsAndMotion() {
        val settings = AppSettings(hapticsEnabled = true, reduceMotion = true)
        val options = skyscrapersBoardOptionsFromSettings(settings)
        assertTrue(options.hapticsEnabled)
        assertTrue(options.reduceMotion)
        assertFalse(options.autoNotesEnabled)
    }

    @Test
    fun skyscrapersBoardOptionsFromSettings_usesDefaults() {
        val settings = AppSettings(hapticsEnabled = false, reduceMotion = false)
        val options = skyscrapersBoardOptionsFromSettings(settings)
        assertFalse(options.hapticsEnabled)
        assertFalse(options.reduceMotion)
    }

    @Test
    fun mosaicBoardOptionsFromSettings_mapsHapticsAndMotion() {
        val settings = AppSettings(hapticsEnabled = false, reduceMotion = true)
        val options = mosaicBoardOptionsFromSettings(settings)
        assertFalse(options.hapticsEnabled)
        assertTrue(options.reduceMotion)
    }

    @Test
    fun mosaicBoardOptionsFromSettings_enablesEverything() {
        val settings = AppSettings(hapticsEnabled = true, reduceMotion = false)
        val options = mosaicBoardOptionsFromSettings(settings)
        assertTrue(options.hapticsEnabled)
        assertFalse(options.reduceMotion)
    }

    @Test
    fun pathBoardOptionsFromSettings_mapsHapticsAndMotion() {
        val settings = AppSettings(hapticsEnabled = true, reduceMotion = true)
        val options = pathBoardOptionsFromSettings(settings)
        assertTrue(options.hapticsEnabled)
        assertTrue(options.reduceMotion)
    }

    @Test
    fun pathBoardOptionsFromSettings_disablesHaptics() {
        val settings = AppSettings(hapticsEnabled = false, reduceMotion = false)
        val options = pathBoardOptionsFromSettings(settings)
        assertFalse(options.hapticsEnabled)
        assertFalse(options.reduceMotion)
    }

    @Test
    fun klondikeBoardOptionsFromSettings_mapsHapticsAndMotion() {
        val settings = AppSettings(hapticsEnabled = false, reduceMotion = false)
        val options = klondikeBoardOptionsFromSettings(settings)
        assertFalse(options.hapticsEnabled)
        assertFalse(options.reduceMotion)
    }

    @Test
    fun klondikeBoardOptionsFromSettings_enablesBoth() {
        val settings = AppSettings(hapticsEnabled = true, reduceMotion = true)
        val options = klondikeBoardOptionsFromSettings(settings)
        assertTrue(options.hapticsEnabled)
        assertTrue(options.reduceMotion)
    }

    @Test
    fun allMappings_consistentWithAppSettings() {
        val settingsHapticsEnabled = AppSettings(hapticsEnabled = true, reduceMotion = false)
        val settingsMotionReduced = AppSettings(hapticsEnabled = false, reduceMotion = true)
        val settingsBoth = AppSettings(hapticsEnabled = true, reduceMotion = true)
        val settingsNeither = AppSettings(hapticsEnabled = false, reduceMotion = false)

        assertEquals(settingsHapticsEnabled.hapticsEnabled, sudokuBoardOptionsFromSettings(settingsHapticsEnabled).hapticsEnabled)
        assertEquals(settingsMotionReduced.reduceMotion, sudokuBoardOptionsFromSettings(settingsMotionReduced).reduceMotion)
        assertEquals(settingsBoth.hapticsEnabled, killerSudokuBoardOptionsFromSettings(settingsBoth).hapticsEnabled)
        assertEquals(settingsBoth.reduceMotion, killerSudokuBoardOptionsFromSettings(settingsBoth).reduceMotion)
        assertEquals(settingsNeither.hapticsEnabled, futoshikiBoardOptionsFromSettings(settingsNeither).hapticsEnabled)
        assertEquals(settingsHapticsEnabled.hapticsEnabled, skyscrapersBoardOptionsFromSettings(settingsHapticsEnabled).hapticsEnabled)
        assertEquals(settingsMotionReduced.reduceMotion, mosaicBoardOptionsFromSettings(settingsMotionReduced).reduceMotion)
        assertEquals(settingsBoth.hapticsEnabled, pathBoardOptionsFromSettings(settingsBoth).hapticsEnabled)
        assertEquals(settingsNeither.reduceMotion, klondikeBoardOptionsFromSettings(settingsNeither).reduceMotion)
    }
}
