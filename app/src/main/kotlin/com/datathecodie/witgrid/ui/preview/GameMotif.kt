package com.datathecodie.witgrid.ui.preview

/** Distinct decorative marks used to identify a game before it is opened. */
enum class GameMotifKind {
    CROWNS, BINARY, NONOGRAM, MINI_SUDOKU, SUDOKU, SKYSCRAPERS, FUTOSHIKI,
    PATH, KILLER_SUDOKU, MOSAIC, TACTICS, COLOUR_SORT, ARROWS, KLONDIKE, UNKNOWN,
}

object GameMotif {
    private val kinds = mapOf(
        "crowns" to GameMotifKind.CROWNS,
        "binary" to GameMotifKind.BINARY,
        "nonogram" to GameMotifKind.NONOGRAM,
        "minisudoku" to GameMotifKind.MINI_SUDOKU,
        "sudoku" to GameMotifKind.SUDOKU,
        "skyscrapers" to GameMotifKind.SKYSCRAPERS,
        "futoshiki" to GameMotifKind.FUTOSHIKI,
        "path" to GameMotifKind.PATH,
        "killersudoku" to GameMotifKind.KILLER_SUDOKU,
        "mosaic" to GameMotifKind.MOSAIC,
        "tactics" to GameMotifKind.TACTICS,
        "coloursort" to GameMotifKind.COLOUR_SORT,
        "arrows" to GameMotifKind.ARROWS,
        "klondike" to GameMotifKind.KLONDIKE,
    )

    fun kindFor(gameId: String): GameMotifKind = kinds[gameId] ?: GameMotifKind.UNKNOWN
}
