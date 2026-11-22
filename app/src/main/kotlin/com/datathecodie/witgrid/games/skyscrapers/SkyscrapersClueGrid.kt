package com.datathecodie.witgrid.games.skyscrapers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersPuzzle
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersRules
import com.datathecodie.witgrid.engine.skyscrapers.SkyscrapersState
import com.datathecodie.witgrid.ui.board.BoardViewportMode
import com.datathecodie.witgrid.ui.board.LocalBoardViewportMode
import com.datathecodie.witgrid.ui.board.MIN_BOARD_CELL_SIZE
import com.datathecodie.witgrid.ui.board.PannableBoardViewport
import com.datathecodie.witgrid.ui.theme.NumberStyles
import com.datathecodie.witgrid.ui.theme.Spacing
import com.datathecodie.witgrid.ui.theme.WitgridTheme

@Composable
internal fun SkyscrapersClueGrid(
    state: SkyscrapersState,
    selectedCellIndex: Int?,
    grid: @Composable (canPan: Boolean) -> Unit,
) {
    val viewportMode = LocalBoardViewportMode.current
    val margin = if (viewportMode.embedded) 0.dp else Spacing.screenMargin
    val size = state.puzzle.size
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().padding(horizontal = margin),
        contentAlignment = Alignment.TopCenter,
    ) {
        val enlargedSide = with(density) { MIN_BOARD_CELL_SIZE.roundToPx() } * (size + 2)
        val availableSide = minOf(constraints.maxWidth, constraints.maxHeight)
            .let { if (it == Constraints.Infinity) enlargedSide else it }
        if (availableSide < size + 2 && !viewportMode.enlarged) return@BoxWithConstraints
        if (availableSide == 0) return@BoxWithConstraints
        val contentSide = if (viewportMode.enlarged) maxOf(availableSide, enlargedSide) else availableSide
        val contentSize = with(density) { contentSide.toDp() }
        val canPan = contentSide > availableSide

        val boardContent: @Composable () -> Unit = {
            Layout(
                modifier = Modifier.size(contentSize),
                content = {
                    // The outer viewport moves the grid and clues together.
                    CompositionLocalProvider(LocalBoardViewportMode provides BoardViewportMode(embedded = true)) {
                        grid(canPan)
                    }
                    val bandSize = with(density) { (contentSide / (size + 2)).toDp() }
                    ClueSide.entries.forEach { side ->
                        val values = side.clues(state.puzzle)
                        val satisfied = remember(state, side) {
                            values.mapIndexed { index, clue ->
                                val line = List(size) { depth -> state.digits[side.cellIndex(index, depth, size)] }
                                isClueSatisfied(line, clue, size)
                            }
                        }
                        val activeLine = selectedCellIndex?.let { cell ->
                            if (side.isColumn) cell % size else cell / size
                        }
                        values.forEachIndexed { index, clue ->
                            ClueLabel(
                                clue = clue,
                                accessibilityLabel = side.accessibilityLabel,
                                index = index,
                                bandSize = bandSize,
                                satisfied = satisfied[index],
                                active = activeLine == index,
                            )
                        }
                    }
                },
            ) { measurables, constraints ->
                val side = constraints.maxWidth
                val band = side / (size + 2)
                val gridSize = side - band * 2
                val measuredGrid = measurables.first().measure(Constraints.fixed(gridSize, gridSize))
                val clues = ClueSide.entries.flatMapIndexed { sideIndex, clueSide ->
                    List(size) { index ->
                        val bounds = clueBounds(clueSide, index, measuredGrid.width, band, size)
                        val clue = measurables[1 + sideIndex * size + index]
                            .measure(Constraints.fixed(bounds.width, bounds.height))
                        clue to bounds
                    }
                }
                layout(side, side) {
                    measuredGrid.place(band, band)
                    clues.forEach { (clue, bounds) -> clue.place(bounds.left, bounds.top) }
                }
            }
        }
        if (canPan) {
            PannableBoardViewport(
                contentWidth = contentSize,
                contentHeight = contentSize,
                enabled = true,
                content = boardContent,
            )
        } else {
            boardContent()
        }
    }
}

internal enum class ClueSide(val isColumn: Boolean, val accessibilityLabel: Int) {
    TOP(true, R.string.skyscrapers_clue_top),
    BOTTOM(true, R.string.skyscrapers_clue_bottom),
    LEFT(false, R.string.skyscrapers_clue_left),
    RIGHT(false, R.string.skyscrapers_clue_right);

    fun clues(puzzle: SkyscrapersPuzzle): List<Int?> = when (this) {
        TOP -> puzzle.topClues
        BOTTOM -> puzzle.bottomClues
        LEFT -> puzzle.leftClues
        RIGHT -> puzzle.rightClues
    }

    fun cellIndex(line: Int, depth: Int, size: Int): Int = when (this) {
        TOP -> depth * size + line
        BOTTOM -> (size - 1 - depth) * size + line
        LEFT -> line * size + depth
        RIGHT -> line * size + size - 1 - depth
    }
}

internal fun clueBounds(side: ClueSide, index: Int, gridSize: Int, band: Int, size: Int): IntRect {
    // Divide the measured grid before rounding so each band uses the canvas cell coordinates.
    val start = band + index * gridSize / size
    val end = band + (index + 1) * gridSize / size
    val farEdge = band + gridSize
    return when (side) {
        ClueSide.TOP -> IntRect(start, 0, end, band)
        ClueSide.BOTTOM -> IntRect(start, farEdge, end, farEdge + band)
        ClueSide.LEFT -> IntRect(0, start, band, end)
        ClueSide.RIGHT -> IntRect(farEdge, start, farEdge + band, end)
    }
}

internal fun isClueSatisfied(line: List<Int?>, clue: Int?, size: Int): Boolean {
    if (clue == null || line.size != size) return false
    val digits = line.filterNotNull()
    return digits.toSet().size == size && SkyscrapersRules.countVisible(digits) == clue
}

@Composable
private fun ClueLabel(
    clue: Int?,
    accessibilityLabel: Int,
    index: Int,
    bandSize: Dp,
    satisfied: Boolean,
    active: Boolean,
) {
    val colors = WitgridTheme.colors
    Box(
        modifier = Modifier.background(if (active) colors.accentSoft else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        if (clue != null) {
            val description = stringResource(accessibilityLabel, clue, index + 1)
            val maximumFontSize = with(LocalDensity.current) { (bandSize * 0.65f).toSp() }
            val fontSize = minOf(NumberStyles.clue.fontSize.value, maximumFontSize.value).sp
            Text(
                text = clue.toString(),
                modifier = Modifier.clearAndSetSemantics { contentDescription = description },
                style = NumberStyles.clue.copy(
                    color = if (satisfied) colors.mutedInk.copy(alpha = 0.5f) else colors.ink,
                    fontSize = fontSize,
                    lineHeight = fontSize * 1.1f,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                ),
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}
