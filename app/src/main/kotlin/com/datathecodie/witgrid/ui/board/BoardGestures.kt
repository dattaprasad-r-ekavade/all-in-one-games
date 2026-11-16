package com.datathecodie.witgrid.ui.board

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalViewConfiguration
import com.datathecodie.witgrid.engine.core.Cell
import kotlinx.coroutines.withTimeoutOrNull

@Composable
internal fun Modifier.boardGestureInput(
    gridSize: Int,
    haptics: BoardHaptics,
    onCellTap: (Cell) -> Unit,
    onDragPaint: (List<Cell>) -> Unit,
    onDragPreview: (List<Cell>) -> Unit,
    onLongPress: (Cell) -> Unit,
    onUndo: () -> Unit,
): Modifier {
    val viewConfiguration = LocalViewConfiguration.current
    val latestCellTap = rememberUpdatedState(onCellTap)
    val latestDragPaint = rememberUpdatedState(onDragPaint)
    val latestDragPreview = rememberUpdatedState(onDragPreview)
    val latestLongPress = rememberUpdatedState(onLongPress)
    val latestUndo = rememberUpdatedState(onUndo)

    return pointerInput(gridSize, haptics, viewConfiguration) {
        awaitEachGesture {
            val firstDown = awaitFirstDown(requireUnconsumed = false)
            firstDown.consume()

            val boardWidth = size.width.toFloat()
            val boardHeight = size.height.toFloat()
            val startCell = BoardGeometry.cellAt(
                firstDown.position.x,
                firstDown.position.y,
                boardWidth,
                boardHeight,
                gridSize,
            )
            val downTimeMillis = firstDown.uptimeMillis
            var latestEventTimeMillis = downTimeMillis
            val initialPositions = mutableMapOf(firstDown.id to firstDown.position)
            val paintedCells = linkedSetOf<Cell>()
            var previousCell = startCell
            var hasMoved = false
            var isDragging = false
            var isMultiTouch = false
            var longPressHandled = false

            try {
                while (true) {
                    val canWaitForLongPress = startCell != null &&
                        !hasMoved && !isDragging && !isMultiTouch && !longPressHandled
                    val remainingLongPressMillis = viewConfiguration.longPressTimeoutMillis -
                        (latestEventTimeMillis - downTimeMillis)
                    val event = if (canWaitForLongPress && remainingLongPressMillis > 0L) {
                        withTimeoutOrNull(remainingLongPressMillis) { awaitPointerEvent() }
                    } else {
                        awaitPointerEvent()
                    }

                    if (event == null) {
                        longPressHandled = true
                        haptics.clear()
                        latestLongPress.value(requireNotNull(startCell))
                        continue
                    }

                    val activePointers = event.changes.filter { change -> change.pressed }
                    val eventTimeMillis = event.changes.maxOfOrNull { change -> change.uptimeMillis }
                        ?: latestEventTimeMillis
                    latestEventTimeMillis = eventTimeMillis

                    event.changes.forEach { change ->
                        val startPosition = initialPositions.getOrPut(change.id) { change.position }
                        if ((change.position - startPosition).getDistance() > viewConfiguration.touchSlop) {
                            hasMoved = true
                        }
                    }
                    if (activePointers.size >= TWO_POINTER_COUNT && !hasMoved) isMultiTouch = true

                    if (!isMultiTouch &&
                        !isDragging &&
                        !longPressHandled &&
                        startCell != null &&
                        !hasMoved &&
                        eventTimeMillis - downTimeMillis >= viewConfiguration.longPressTimeoutMillis
                    ) {
                        longPressHandled = true
                        haptics.clear()
                        latestLongPress.value(startCell)
                    }

                    val primaryPointer = event.changes.firstOrNull { change -> change.id == firstDown.id }
                    if (!isMultiTouch && !longPressHandled && hasMoved && startCell != null) {
                        val currentCell = primaryPointer?.let { change ->
                            BoardGeometry.cellAt(
                                change.position.x,
                                change.position.y,
                                boardWidth,
                                boardHeight,
                                gridSize,
                            )
                        }
                        if (currentCell == null) {
                            previousCell = null
                        } else {
                            isDragging = true
                            val segment = previousCell?.let { previous ->
                                BoardGeometry.cellsBetween(previous, currentCell, gridSize)
                            } ?: listOf(currentCell)
                            val newlyPainted = segment.filter { cell -> paintedCells.add(cell) }
                            if (newlyPainted.isNotEmpty()) {
                                newlyPainted.forEach { haptics.mark() }
                                latestDragPreview.value(paintedCells.toList())
                            }
                            previousCell = currentCell
                        }
                    }

                    if (activePointers.isEmpty()) {
                        val gestureDurationMillis = eventTimeMillis - downTimeMillis
                        when {
                            isMultiTouch -> {
                                if (!hasMoved &&
                                    gestureDurationMillis <= viewConfiguration.longPressTimeoutMillis
                                ) {
                                    haptics.undo()
                                    latestUndo.value()
                                }
                            }

                            longPressHandled -> Unit

                            isDragging && paintedCells.isNotEmpty() -> {
                                latestDragPaint.value(paintedCells.toList())
                            }

                            !hasMoved && startCell != null -> {
                                if (gestureDurationMillis >= viewConfiguration.longPressTimeoutMillis) {
                                    haptics.clear()
                                    latestLongPress.value(startCell)
                                } else {
                                    haptics.mark()
                                    latestCellTap.value(startCell)
                                }
                            }
                        }
                        event.changes.forEach(PointerInputChange::consume)
                        break
                    }

                    event.changes.forEach(PointerInputChange::consume)
                }
            } finally {
                latestDragPreview.value(emptyList())
            }
        }
    }
}

private const val TWO_POINTER_COUNT = 2
