package com.datathecodie.witgrid.ui.screens.play

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.engine.minisudoku.MiniSudokuState
import com.datathecodie.witgrid.ui.board.SudokuPlayActions
import com.datathecodie.witgrid.ui.components.ToolBarItem
import com.datathecodie.witgrid.ui.theme.WitgridTheme

@Composable
internal fun PlaySudokuDigits(state: MiniSudokuState, actions: SudokuPlayActions) {
    PlaySudokuDigits(state.puzzle.size, state.digits, actions)
}

@Composable
internal fun PlaySudokuDigits(size: Int, digits: List<Int?>, actions: SudokuPlayActions) {
    val colors = WitgridTheme.colors
    Column(Modifier.fillMaxWidth().testTag("play-digit-pad"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        (1..size).chunked(if (size <= 6) size else 3).forEach { rowDigits ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rowDigits.forEach { digit ->
                    val description = stringResource(
                        if (actions.notesMode) R.string.minisudoku_note_button_accessibility
                        else R.string.minisudoku_digit_button_accessibility,
                        digit,
                    )
                    Surface(
                        onClick = { actions.onDigit(digit) },
                        enabled = actions.canEdit,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            .semantics { contentDescription = description },
                        shape = RoundedCornerShape(14.dp),
                        color = colors.card,
                        contentColor = colors.accent,
                        border = BorderStroke(1.dp, colors.cardEdge),
                    ) {
                        Column(
                            Modifier.padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(digit.toString(), style = MaterialTheme.typography.titleLarge)
                            Text(
                                stringResource(
                                    R.string.play_digit_left,
                                    (size - digits.count { it == digit }).coerceAtLeast(0),
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.mutedInk,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PlaySudokuTools(actions: SudokuPlayActions) {
    Row(Modifier.fillMaxWidth()) {
        ToolBarItem(
            playIcon(PlayIcon.NOTES),
            stringResource(R.string.minisudoku_mode_notes),
            actions.onToggleNotes,
            Modifier.weight(1f),
            selected = actions.notesMode,
        )
        ToolBarItem(
            playIcon(PlayIcon.ERASE),
            stringResource(R.string.play_erase),
            actions.onErase,
            Modifier.weight(1f),
            enabled = actions.canEdit,
        )
    }
}
