package com.datathecodie.witgrid.bankgen

import com.datathecodie.witgrid.engine.arrows.ArrowsEngine
import com.datathecodie.witgrid.engine.arrows.ArrowsGenerator
import com.datathecodie.witgrid.engine.arrows.ArrowsRules

internal val defaultFamilyBValidators: List<PlayBankValidator<*>> = listOf(
    PlayEngineValidator(
        ArrowsEngine,
        { ArrowsGenerator.configFor(it.pack).difficulty },
    ) { ArrowsRules.isClearable(it.size, it.initialArrows) },
)

/**
 * Production registry of Family B bank validators.
 * Mutable hook so unit tests can substitute fake validators and restore [defaultFamilyBValidators].
 */
internal var familyBRegistry: () -> List<PlayBankValidator<*>> = { defaultFamilyBValidators }

internal fun discoverFamilyBBankGames(): List<BankGame> =
    familyBRegistry().map { PlayEngineBankGame(it) }