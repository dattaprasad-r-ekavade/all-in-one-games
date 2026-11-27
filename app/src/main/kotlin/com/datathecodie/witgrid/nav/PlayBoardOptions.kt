package com.datathecodie.witgrid.nav

import com.datathecodie.witgrid.data.settings.AppSettings
import com.datathecodie.witgrid.games.futoshiki.FutoshikiBoardOptions
import com.datathecodie.witgrid.games.killersudoku.KillerSudokuBoardOptions
import com.datathecodie.witgrid.games.klondike.KlondikeBoardOptions
import com.datathecodie.witgrid.games.mosaic.MosaicBoardOptions
import com.datathecodie.witgrid.games.path.PathBoardOptions
import com.datathecodie.witgrid.games.skyscrapers.SkyscrapersBoardOptions
import com.datathecodie.witgrid.games.sudoku.SudokuBoardOptions

/** Map player settings to SudokuBoardOptions for the play route. */
internal fun sudokuBoardOptionsFromSettings(settings: AppSettings): SudokuBoardOptions =
    SudokuBoardOptions(
        hapticsEnabled = settings.hapticsEnabled,
        reduceMotion = settings.reduceMotion,
    )

/** Map player settings to KillerSudokuBoardOptions for the play route. */
internal fun killerSudokuBoardOptionsFromSettings(settings: AppSettings): KillerSudokuBoardOptions =
    KillerSudokuBoardOptions(
        hapticsEnabled = settings.hapticsEnabled,
        reduceMotion = settings.reduceMotion,
    )

/** Map player settings to FutoshikiBoardOptions for the play route. */
internal fun futoshikiBoardOptionsFromSettings(settings: AppSettings): FutoshikiBoardOptions =
    FutoshikiBoardOptions(
        hapticsEnabled = settings.hapticsEnabled,
        reduceMotion = settings.reduceMotion,
    )

/** Map player settings to SkyscrapersBoardOptions for the play route. */
internal fun skyscrapersBoardOptionsFromSettings(settings: AppSettings): SkyscrapersBoardOptions =
    SkyscrapersBoardOptions(
        hapticsEnabled = settings.hapticsEnabled,
        reduceMotion = settings.reduceMotion,
    )

/** Map player settings to MosaicBoardOptions for the play route. */
internal fun mosaicBoardOptionsFromSettings(settings: AppSettings): MosaicBoardOptions =
    MosaicBoardOptions(
        hapticsEnabled = settings.hapticsEnabled,
        reduceMotion = settings.reduceMotion,
    )

/** Map player settings to PathBoardOptions for the play route. */
internal fun pathBoardOptionsFromSettings(settings: AppSettings): PathBoardOptions =
    PathBoardOptions(
        hapticsEnabled = settings.hapticsEnabled,
        reduceMotion = settings.reduceMotion,
    )

/** Map player settings to KlondikeBoardOptions for the play route. */
internal fun klondikeBoardOptionsFromSettings(settings: AppSettings): KlondikeBoardOptions =
    KlondikeBoardOptions(
        hapticsEnabled = settings.hapticsEnabled,
        reduceMotion = settings.reduceMotion,
    )
