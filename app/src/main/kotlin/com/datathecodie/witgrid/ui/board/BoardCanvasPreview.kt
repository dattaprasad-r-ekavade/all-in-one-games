package com.datathecodie.witgrid.ui.board

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.ui.theme.WitgridTheme

@Preview(showBackground = true)
@Composable
fun BoardCanvasDemoPreview() {
    WitgridTheme {
        val gridSize = 4
        val colors = WitgridTheme.colors
        var markedCells by remember { mutableStateOf(emptySet<Cell>()) }
        val cellDescription: @Composable (Cell) -> String = { cell ->
            stringResource(
                R.string.board_cell_accessibility_label,
                cell.index / gridSize + 1,
                cell.index % gridSize + 1,
            )
        }

        BoardCanvas(
            gridSize = gridSize,
            cellDescription = cellDescription,
            modifier = Modifier.padding(vertical = 32.dp),
            solved = markedCells.size == gridSize * gridSize,
            reduceMotion = false,
            hapticsEnabled = false,
            drawCell = { cell, bounds ->
                if (cell in markedCells) {
                    drawCircle(
                        color = colors.accent,
                        radius = minOf(bounds.width, bounds.height) * 0.28f,
                        center = bounds.center,
                    )
                }
            },
            onCellTap = { cell ->
                markedCells = if (cell in markedCells) markedCells - cell else markedCells + cell
            },
            onDragPaint = { path -> markedCells = markedCells + path },
            onLongPress = { cell -> markedCells = markedCells - cell },
            onUndo = { markedCells = markedCells.toList().dropLast(1).toSet() },
        )
    }
}
