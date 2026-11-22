package com.datathecodie.witgrid.games.skyscrapers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersRules
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersState
import com.datathecodie.witgrid.ui.board.BoardCanvas
import com.datathecodie.witgrid.ui.board.LocalBoardViewportMode
import com.datathecodie.witgrid.ui.board.MIN_BOARD_CELL_SIZE
import com.datathecodie.witgrid.ui.board.PannableBoardViewport
import com.datathecodie.witgrid.ui.board.SudokuPlayActions
import com.datathecodie.witgrid.ui.theme.NumberStyles
import com.datathecodie.witgrid.ui.theme.Spacing
import com.datathecodie.witgrid.ui.theme.WitgridTheme

@Composable
fun SkyscrapersBoard(
    state: SkyscrapersState,
    onMoves: (List<Move>) -> Unit,
    onUndo: () -> Unit,
    options: SkyscrapersBoardOptions = LocalSkyscrapersBoardOptions.current,
    modifier: Modifier = Modifier,
    playLayout: (@Composable (board: @Composable () -> Unit, actions: SudokuPlayActions) -> Unit)? = null,
    layout: (@Composable (board: @Composable () -> Unit, controls: @Composable () -> Unit) -> Unit)? = null,
) {
    var selectedCellIndex by remember(state.puzzle.id) { mutableStateOf<Int?>(null) }
    var notesMode by remember(state.puzzle.id) { mutableStateOf(false) }
    val puzzle = state.puzzle
    val selectedCell = selectedCellIndex?.let(::Cell)
    val selectedCellIsEditable = selectedCellIndex?.let { index -> puzzle.givens[index] == null } == true
    val colors = WitgridTheme.colors
    val textMeasurer = rememberTextMeasurer()
    val digitStyle = NumberStyles.cell.copy(color = colors.accent)
    val givenStyle = NumberStyles.cell.copy(color = colors.ink)
    val noteStyle = NumberStyles.clue.copy(color = colors.mutedInk, fontSize = 11.sp)

    val completedTopClues = remember(state) {
        (0 until puzzle.size).map { col ->
            val colDown = (0 until puzzle.size).map { row -> state.digits[row * puzzle.size + col] }
            isClueSatisfied(colDown, puzzle.topClues[col], puzzle.size)
        }
    }
    val completedBottomClues = remember(state) {
        (0 until puzzle.size).map { col ->
            val colUp = (puzzle.size - 1 downTo 0).map { row -> state.digits[row * puzzle.size + col] }
            isClueSatisfied(colUp, puzzle.bottomClues[col], puzzle.size)
        }
    }
    val completedLeftClues = remember(state) {
        (0 until puzzle.size).map { row ->
            val rowRight = (0 until puzzle.size).map { col -> state.digits[row * puzzle.size + col] }
            isClueSatisfied(rowRight, puzzle.leftClues[row], puzzle.size)
        }
    }
    val completedRightClues = remember(state) {
        (0 until puzzle.size).map { row ->
            val rowLeft = (puzzle.size - 1 downTo 0).map { col -> state.digits[row * puzzle.size + col] }
            isClueSatisfied(rowLeft, puzzle.rightClues[row], puzzle.size)
        }
    }

    val boardContent: @Composable () -> Unit = {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val viewportMode = LocalBoardViewportMode.current
            val margin = if (viewportMode.embedded) 0.dp else Spacing.screenMargin
            val availableSide = if (maxHeight == Dp.Infinity) maxWidth else minOf(maxWidth, maxHeight)
            val totalCells = puzzle.size + 2
            val fitCellSize = ((availableSide - margin * 2) / totalCells).coerceAtLeast(1.dp)
            val cellSize = if (viewportMode.enlarged) {
                maxOf(fitCellSize, MIN_BOARD_CELL_SIZE)
            } else {
                fitCellSize
            }
            val clueBandSize = cellSize
            val boardSlotWidth = cellSize * puzzle.size + margin * 2
            val contentWidth = cellSize * totalCells + margin * 2
            val contentHeight = cellSize * totalCells
            val viewportWidth = minOf(maxWidth, contentWidth)
            val viewportHeight = if (maxHeight == Dp.Infinity) contentHeight else minOf(maxHeight, contentHeight)
            val canPan = viewportMode.enlarged &&
                (contentWidth > viewportWidth || contentHeight > viewportHeight)

            val cluesAndGrid: @Composable () -> Unit = {
                Column(modifier = Modifier.width(contentWidth).height(contentHeight)) {
                    // Top clues row
                    Row(modifier = Modifier.fillMaxWidth().height(clueBandSize)) {
                        Spacer(Modifier.width(clueBandSize))
                        Row(
                            modifier = Modifier
                                .width(boardSlotWidth)
                                .height(clueBandSize)
                                .padding(horizontal = margin),
                        ) {
                            (0 until puzzle.size).forEach { col ->
                                val clue = puzzle.topClues[col]
                                val isSatisfied = completedTopClues[col]
                                val isActive = selectedCellIndex != null && selectedCellIndex!! % puzzle.size == col
                                ClueLabel(
                                    label = clue?.toString().orEmpty(),
                                    cellSize = cellSize,
                                    color = if (isSatisfied) colors.mutedInk.copy(alpha = 0.5f) else colors.ink,
                                    modifier = Modifier.background(
                                        if (isActive) colors.accentSoft else Color.Transparent,
                                    ),
                                )
                            }
                        }
                        Spacer(Modifier.width(clueBandSize))
                    }

                    // Middle row (Left clues, Grid Canvas, Right clues)
                    Row(modifier = Modifier.fillMaxWidth().height(cellSize * puzzle.size)) {
                        Column(modifier = Modifier.width(clueBandSize).height(cellSize * puzzle.size)) {
                            (0 until puzzle.size).forEach { row ->
                                val clue = puzzle.leftClues[row]
                                val isSatisfied = completedLeftClues[row]
                                val isActive = selectedCellIndex != null && selectedCellIndex!! / puzzle.size == row
                                ClueLabel(
                                    label = clue?.toString().orEmpty(),
                                    cellSize = cellSize,
                                    color = if (isSatisfied) colors.mutedInk.copy(alpha = 0.5f) else colors.ink,
                                    modifier = Modifier.background(
                                        if (isActive) colors.accentSoft else Color.Transparent,
                                    ),
                                )
                            }
                        }

                        BoardCanvas(
                            gridSize = puzzle.size,
                            cellDescription = { cell ->
                                describeSkyscrapersCell(state, cell, selectedCellIndex == cell.index)
                            },
                            modifier = Modifier.width(boardSlotWidth),
                            solved = SkyscrapersRules.isSolved(puzzle, state),
                            reduceMotion = options.reduceMotion,
                            hapticsEnabled = options.hapticsEnabled,
                            dragPaintEnabled = false,
                            panningEnabled = canPan,
                            drawCell = { cell, bounds ->
                                val isGiven = puzzle.givens[cell.index] != null
                                drawRect(colors.card, bounds.topLeft, bounds.size)
                                val selected = selectedCellIndex
                                if (selected != null) {
                                    val row = cell.index / puzzle.size
                                    val column = cell.index % puzzle.size
                                    val selectedRow = selected / puzzle.size
                                    val selectedColumn = selected % puzzle.size
                                    if (row == selectedRow || column == selectedColumn) {
                                        drawRect(colors.accentSoft, bounds.topLeft, bounds.size)
                                    }
                                }
                                if (selectedCellIndex == cell.index) {
                                    drawRect(
                                        colors.accent.copy(alpha = SELECTION_FILL_ALPHA),
                                        bounds.topLeft,
                                        bounds.size,
                                    )
                                }

                                val digit = state.digits[cell.index]
                                if (digit != null) {
                                    val style = if (isGiven) givenStyle else digitStyle
                                    val text = textMeasurer.measure(AnnotatedString(digit.toString()), style = style)
                                    drawCenteredLabel(text, bounds, bounds.width, bounds.height)
                                } else {
                                    drawNotes(state.notes[cell.index], bounds, textMeasurer, noteStyle, puzzle.size)
                                }

                                if (selectedCellIndex == cell.index) {
                                    drawRect(
                                        color = colors.accent,
                                        topLeft = bounds.topLeft,
                                        size = bounds.size,
                                        style = Stroke(width = SELECTION_BORDER_WIDTH_DP.dp.toPx()),
                                    )
                                }
                            },
                            onCellTap = { cell -> selectedCellIndex = cell.index },
                            onDragPaint = {},
                            onLongPress = { cell ->
                                selectedCellIndex = cell.index
                                SkyscrapersBoardInput.clearMoves(state, cell).takeIf { it.isNotEmpty() }?.let(onMoves)
                            },
                            onUndo = onUndo,
                        )

                        Column(modifier = Modifier.width(clueBandSize).height(cellSize * puzzle.size)) {
                            (0 until puzzle.size).forEach { row ->
                                val clue = puzzle.rightClues[row]
                                val isSatisfied = completedRightClues[row]
                                val isActive = selectedCellIndex != null && selectedCellIndex!! / puzzle.size == row
                                ClueLabel(
                                    label = clue?.toString().orEmpty(),
                                    cellSize = cellSize,
                                    color = if (isSatisfied) colors.mutedInk.copy(alpha = 0.5f) else colors.ink,
                                    modifier = Modifier.background(
                                        if (isActive) colors.accentSoft else Color.Transparent,
                                    ),
                                )
                            }
                        }
                    }

                    // Bottom clues row
                    Row(modifier = Modifier.fillMaxWidth().height(clueBandSize)) {
                        Spacer(Modifier.width(clueBandSize))
                        Row(
                            modifier = Modifier
                                .width(boardSlotWidth)
                                .height(clueBandSize)
                                .padding(horizontal = margin),
                        ) {
                            (0 until puzzle.size).forEach { col ->
                                val clue = puzzle.bottomClues[col]
                                val isSatisfied = completedBottomClues[col]
                                val isActive = selectedCellIndex != null && selectedCellIndex!! % puzzle.size == col
                                ClueLabel(
                                    label = clue?.toString().orEmpty(),
                                    cellSize = cellSize,
                                    color = if (isSatisfied) colors.mutedInk.copy(alpha = 0.5f) else colors.ink,
                                    modifier = Modifier.background(
                                        if (isActive) colors.accentSoft else Color.Transparent,
                                    ),
                                )
                            }
                        }
                        Spacer(Modifier.width(clueBandSize))
                    }
                }
            }

            if (canPan) {
                PannableBoardViewport(
                    modifier = Modifier.width(viewportWidth).height(viewportHeight),
                    contentWidth = contentWidth,
                    contentHeight = contentHeight,
                    enabled = true,
                    content = cluesAndGrid,
                )
            } else {
                cluesAndGrid()
            }
        }
    }

    val controlsContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screenMargin),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = !notesMode,
                    onClick = { notesMode = false },
                    label = { Text(stringResource(R.string.minisudoku_mode_digits)) },
                )
                Spacer(Modifier.width(Spacing.xs))
                FilterChip(
                    selected = notesMode,
                    onClick = { notesMode = true },
                    label = { Text(stringResource(R.string.minisudoku_mode_notes)) },
                )
                Spacer(Modifier.width(Spacing.sm))
                TextButton(
                    enabled = selectedCellIsEditable,
                    onClick = {
                        selectedCell?.let { cell ->
                            SkyscrapersBoardInput.clearMoves(state, cell)
                                .takeIf { moves -> moves.isNotEmpty() }?.let(onMoves)
                        }
                    },
                ) {
                    Text(stringResource(R.string.minisudoku_clear_cell))
                }
            }

            Spacer(Modifier.height(Spacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                (1..puzzle.size).forEach { digit ->
                    val remaining = (puzzle.size - state.digits.count { it == digit }).coerceAtLeast(0)
                    val buttonDescription = stringResource(
                        if (notesMode) {
                            R.string.minisudoku_note_button_accessibility
                        } else {
                            R.string.minisudoku_digit_button_accessibility
                        },
                        digit,
                    )
                    Surface(
                        onClick = {
                            selectedCell?.let { cell ->
                                val moves = if (notesMode) {
                                    SkyscrapersBoardInput.noteMoves(state, cell, digit)
                                } else {
                                    SkyscrapersBoardInput.digitMoves(state, cell, digit, options.autoNotesEnabled)
                                }
                                moves.takeIf { it.isNotEmpty() }?.let(onMoves)
                            }
                        },
                        enabled = selectedCellIsEditable,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .semantics { contentDescription = buttonDescription },
                        shape = RoundedCornerShape(12.dp),
                        color = colors.card,
                        contentColor = colors.accent,
                        border = BorderStroke(1.dp, colors.cardEdge),
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = digit.toString(),
                                style = NumberStyles.cell.copy(fontSize = 18.sp),
                                maxLines = 1,
                                softWrap = false,
                            )
                            Text(
                                text = stringResource(R.string.play_digit_left, remaining),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = colors.mutedInk,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip,
                            )
                        }
                    }
                }
            }
        }
    }

    if (playLayout != null) {
        playLayout(boardContent, SudokuPlayActions(
            notesMode = notesMode,
            canEdit = selectedCellIsEditable,
            onToggleNotes = { notesMode = !notesMode },
            onErase = {
                selectedCell?.let { cell ->
                    SkyscrapersBoardInput.clearMoves(state, cell).takeIf { it.isNotEmpty() }?.let(onMoves)
                }
            },
            onDigit = { digit ->
                selectedCell?.let { cell ->
                    val moves = if (notesMode) {
                        SkyscrapersBoardInput.noteMoves(state, cell, digit)
                    } else {
                        SkyscrapersBoardInput.digitMoves(state, cell, digit, options.autoNotesEnabled)
                    }
                    moves.takeIf { it.isNotEmpty() }?.let(onMoves)
                }
            },
        ))
    } else if (layout == null) {
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            boardContent()
            Spacer(Modifier.height(Spacing.sm))
            controlsContent()
        }
    } else {
        layout(boardContent, controlsContent)
    }
}

internal fun isClueSatisfied(line: List<Int?>, clue: Int?, size: Int): Boolean {
    if (clue == null) return false
    val nonNull = line.filterNotNull()
    if (nonNull.toSet().size != nonNull.size) return false

    if (line.none { it == null }) {
        return nonNull.toSet().size == size && SkyscrapersRules.countVisible(nonNull) == clue
    }
    val maxIndex = line.indexOf(size)
    if (maxIndex != -1) {
        val prefix = line.subList(0, maxIndex + 1)
        if (prefix.none { it == null } && prefix.toSet().size == prefix.size) {
            return SkyscrapersRules.countVisible(prefix.filterNotNull()) == clue
        }
    }
    return false
}

@Composable
private fun describeSkyscrapersCell(state: SkyscrapersState, cell: Cell, selected: Boolean): String {
    val digit = state.digits[cell.index]?.toString() ?: stringResource(R.string.minisudoku_cell_empty)
    val noteDigits = (1..state.puzzle.size)
        .filter { value -> state.notes[cell.index] and (1 shl (value - 1)) != 0 }
        .joinToString(", ")
    val notes = if (noteDigits.isEmpty()) {
        stringResource(R.string.minisudoku_no_notes)
    } else {
        stringResource(R.string.minisudoku_cell_notes_accessibility, noteDigits)
    }
    val editability = stringResource(
        if (state.puzzle.givens[cell.index] == null) {
            R.string.minisudoku_cell_editable
        } else {
            R.string.minisudoku_cell_given
        },
    )
    val selection = stringResource(
        if (selected) R.string.minisudoku_cell_selected else R.string.minisudoku_cell_not_selected,
    )
    return stringResource(
        R.string.minisudoku_cell_accessibility_label,
        cell.index / state.puzzle.size + 1,
        cell.index % state.puzzle.size + 1,
        digit,
        notes,
        editability,
        selection,
    )
}

@Composable
private fun clueStyle(cellSize: Dp, color: Color): TextStyle {
    val maximumFontSize = with(LocalDensity.current) { (cellSize * CLUE_FONT_SIZE_FRACTION).toSp() }
    val fontSize = minOf(NumberStyles.clue.fontSize.value, maximumFontSize.value).sp
    return NumberStyles.clue.copy(
        color = color,
        fontSize = fontSize,
        lineHeight = fontSize * CLUE_LINE_HEIGHT_FRACTION,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )
}

@Composable
private fun ClueLabel(
    label: String,
    cellSize: Dp,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(cellSize),
        contentAlignment = Alignment.Center,
    ) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = clueStyle(cellSize, color),
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

private fun DrawScope.drawNotes(
    noteMask: Int,
    bounds: Rect,
    textMeasurer: TextMeasurer,
    style: TextStyle,
    size: Int,
) {
    val noteCols = if (size <= 4) 2 else 3
    val noteRows = if (size <= 4) 2 else 3
    for (digit in 1..size) {
        if (noteMask and (1 shl (digit - 1)) == 0) continue
        val text = textMeasurer.measure(AnnotatedString(digit.toString()), style = style)
        val column = (digit - 1) % noteCols
        val row = (digit - 1) / noteCols
        val slotWidth = bounds.width / noteCols
        val slotHeight = bounds.height / noteRows
        val center = Offset(
            x = bounds.left + (column + 0.5f) * slotWidth,
            y = bounds.top + (row + 0.5f) * slotHeight,
        )
        drawCenteredLabel(text, bounds, slotWidth, slotHeight, center)
    }
}

private fun DrawScope.drawCenteredLabel(
    text: TextLayoutResult,
    bounds: Rect,
    availableWidth: Float,
    availableHeight: Float,
    center: Offset = bounds.center,
) {
    val labelScale = minOf(
        1f,
        availableWidth * LABEL_SIZE_FRACTION / text.size.width.coerceAtLeast(1),
        availableHeight * LABEL_SIZE_FRACTION / text.size.height.coerceAtLeast(1),
    )
    scale(labelScale, pivot = center) {
        drawText(
            text,
            topLeft = Offset(
                x = center.x - text.size.width / 2f,
                y = center.y - text.size.height / 2f,
            ),
        )
    }
}

private const val SELECTION_BORDER_WIDTH_DP = 2
private const val SELECTION_FILL_ALPHA = 0.12f
private const val LABEL_SIZE_FRACTION = 0.78f
private const val CLUE_FONT_SIZE_FRACTION = 0.65f
private const val CLUE_LINE_HEIGHT_FRACTION = 1.1f
