package com.datathecodie.witgrid.ui.board

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import com.datathecodie.witgrid.engine.core.Cell

@Composable
internal fun AccessibilityCells(
    gridSize: Int,
    cellDescription: State<@Composable (Cell) -> String>,
    onCellTap: State<(Cell) -> Unit>,
    onLongPress: State<(Cell) -> Unit>,
    onUndo: State<() -> Unit>,
    haptics: BoardHaptics,
    clearActionLabel: String,
    undoActionLabel: String,
    modifier: Modifier = Modifier,
) {
    Layout(
        content = {
            repeat(gridSize * gridSize) { index ->
                val cell = Cell(index)
                val description = cellDescription.value(cell)
                Spacer(
                    Modifier.semantics {
                        contentDescription = description
                        role = Role.Button
                        customActions = listOf(
                            CustomAccessibilityAction(clearActionLabel) {
                                haptics.clear()
                                onLongPress.value(cell)
                                true
                            },
                        )
                        onClick {
                            haptics.mark()
                            onCellTap.value(cell)
                            true
                        }
                    },
                )
            }
        },
        modifier = modifier.semantics {
            collectionInfo = CollectionInfo(rowCount = gridSize, columnCount = gridSize)
            customActions = listOf(
                CustomAccessibilityAction(undoActionLabel) {
                    haptics.undo()
                    onUndo.value()
                    true
                },
            )
        },
    ) { measurables, constraints ->
        val boardWidth = constraints.maxWidth
        val boardHeight = constraints.maxHeight
        val placeables = measurables.mapIndexed { index, measurable ->
            val row = index / gridSize
            val column = index % gridSize
            val left = column * boardWidth / gridSize
            val right = (column + 1) * boardWidth / gridSize
            val top = row * boardHeight / gridSize
            val bottom = (row + 1) * boardHeight / gridSize
            measurable.measure(Constraints.fixed(right - left, bottom - top))
        }
        layout(boardWidth, boardHeight) {
            placeables.forEachIndexed { index, placeable ->
                val row = index / gridSize
                val column = index % gridSize
                placeable.place(
                    x = column * boardWidth / gridSize,
                    y = row * boardHeight / gridSize,
                )
            }
        }
    }
}
