package com.datathecodie.witgrid.bankgen

/**
 * Optional hook for Family B bank games. Empty in production until engine/ exposes Play engines (E3).
 * Tests assign a supplier that returns fake validators.
 */
internal var familyBRegistry: () -> List<PlayBankValidator<*>> = { emptyList() }

internal fun discoverFamilyBBankGames(): List<BankGame> =
    familyBRegistry().map { PlayEngineBankGame(it) }