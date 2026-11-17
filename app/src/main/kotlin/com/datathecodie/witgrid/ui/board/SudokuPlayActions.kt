package com.datathecodie.witgrid.ui.board

/** The game keeps selection and editing rules; the play screen places its controls. */
data class SudokuPlayActions(
    val notesMode: Boolean,
    val canEdit: Boolean,
    val onToggleNotes: () -> Unit,
    val onErase: () -> Unit,
    val onDigit: (Int) -> Unit,
)
