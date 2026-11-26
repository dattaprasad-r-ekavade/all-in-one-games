package com.datathecodie.witgrid.bankgen

import com.datathecodie.witgrid.engine.arrows.ArrowsEngine
import com.datathecodie.witgrid.engine.arrows.ArrowsGenerator
import com.datathecodie.witgrid.engine.arrows.ArrowsRules
import com.datathecodie.witgrid.engine.coloursort.ColourSortEngine
import com.datathecodie.witgrid.engine.coloursort.ColourSortGenerator
import com.datathecodie.witgrid.engine.coloursort.ColourSortSolver
import com.datathecodie.witgrid.engine.klondike.KlondikeDeal
import com.datathecodie.witgrid.engine.klondike.KlondikeEngine
import com.datathecodie.witgrid.engine.klondike.KlondikeGenerator
import com.datathecodie.witgrid.engine.klondike.KlondikeSolver

internal val defaultFamilyBValidators: List<PlayBankValidator<*>> = listOf(
    PlayEngineValidator(
        ArrowsEngine,
        { ArrowsGenerator.configFor(it.pack).difficulty },
    ) { ArrowsRules.isClearable(it.size, it.initialArrows) },
    PlayEngineValidator(
        ColourSortEngine,
        { ColourSortGenerator.configFor(it.pack).difficulty },
    ) { ColourSortSolver.isWinnable(it.initialTubes) },
    PlayEngineValidator(
        KlondikeEngine,
        { KlondikeGenerator.configFor(it.pack).difficulty },
    ) { KlondikeSolver.isWinnable(KlondikeDeal.deal(it)) },
)

/**
 * Production registry of Family B bank validators.
 * Mutable hook so unit tests can substitute fake validators and restore [defaultFamilyBValidators].
 */
internal var familyBRegistry: () -> List<PlayBankValidator<*>> = { defaultFamilyBValidators }

internal fun discoverFamilyBBankGames(): List<BankGame> =
    familyBRegistry().map { PlayEngineBankGame(it) }