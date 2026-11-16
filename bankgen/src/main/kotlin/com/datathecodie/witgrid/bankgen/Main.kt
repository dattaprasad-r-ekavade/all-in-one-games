package com.datathecodie.witgrid.bankgen

import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val service = BankgenService(registeredBankGames(), defaultBankDirectory())
    val exitCode = BankgenCommandLine(service, System.out, System.err).run(args)
    if (exitCode != 0) exitProcess(exitCode)
}
