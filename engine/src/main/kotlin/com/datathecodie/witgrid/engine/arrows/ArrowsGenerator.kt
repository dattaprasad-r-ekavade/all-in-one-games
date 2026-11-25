package com.datathecodie.witgrid.engine.arrows

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64

/**
 * Builds one clearable Arrows layout for a pack.
 *
 * Arrows are placed from the last slide to the first. Each new arrow needs a clear
 * ray, so it can leave while every earlier arrow is still on the board. Reversing
 * that placement order clears the board, and the layout cannot start deadlocked.
 */
object ArrowsGenerator {
    private const val ID_HEX_WIDTH = 16

    data class PackConfig(
        val size: Int,
        val minArrows: Int,
        val maxArrows: Int,
        val minChainDepth: Int,
        val difficulty: String,
    ) {
        init {
            require(size in ArrowsPuzzle.SUPPORTED_SIZES) { "Unsupported Arrows board size: $size" }
            require(minChainDepth in 2..size) { "Chain depth must fit the board: $minChainDepth" }
            require(minArrows >= minChainDepth + 1) {
                "Pack needs room for a second direction after the chain"
            }
            // An empty border cell can always point off the board, so placement can
            // always fill the border. The minimum count stays inside that guarantee.
            require(minArrows <= 4 * (size - 1)) {
                "Minimum arrows $minArrows exceed the border of a $size board"
            }
            require(maxArrows in minArrows..size * size) {
                "Arrow range $minArrows..$maxArrows does not fit a $size board"
            }
            require(difficulty.isNotBlank() && difficulty.none { it == '|' || it == '\n' || it == '\r' }) {
                "Invalid difficulty: $difficulty"
            }
        }
    }

    fun configFor(pack: Pack): PackConfig = when (pack) {
        // Difficulty tokens match the other Family B lane: one band per pack.
        // Inside the band, a larger board, more arrows, and a longer forced chain
        // are what make the pack harder.
        Pack.EASY -> PackConfig(
            size = 5,
            minArrows = 8,
            maxArrows = 16,
            minChainDepth = 2,
            difficulty = "d1",
        )
        Pack.MEDIUM -> PackConfig(
            size = 6,
            minArrows = 14,
            maxArrows = 24,
            minChainDepth = 3,
            difficulty = "d2",
        )
        Pack.HARD -> PackConfig(
            size = 8,
            minArrows = 22,
            maxArrows = 40,
            minChainDepth = 4,
            difficulty = "d3",
        )
        Pack.EXPERT -> PackConfig(
            size = 10,
            minArrows = 30,
            maxArrows = 60,
            minChainDepth = 5,
            difficulty = "d4",
        )
    }

    fun generate(rng: SplitMix64, pack: Pack): ArrowsPuzzle {
        val config = configFor(pack)
        require(config.size == ArrowsPuzzle.expectedGridSize(pack)) {
            "Pack $pack config size ${config.size} does not match the board rule"
        }
        val span = config.maxArrows - config.minArrows + 1
        val target = config.minArrows + rng.nextInt(span)
        val arrows = placeLayout(rng, config, target)
        val hex = rng.nextLong().toULong().toString(16).padStart(ID_HEX_WIDTH, '0')
        return ArrowsPuzzle(
            bankId = "AR-${pack.name}-$hex",
            size = config.size,
            pack = pack,
            difficulty = config.difficulty,
            initialArrows = arrows,
        )
    }

    /**
     * Longest line of arrows that must slide in order.
     * An arrow's depth is one more than the deepest arrow still standing on its ray.
     */
    internal fun chainDepth(size: Int, arrows: List<ArrowsDirection?>): Int {
        val memo = IntArray(arrows.size) { -1 }
        val visiting = BooleanArray(arrows.size)
        var best = 0
        for (index in arrows.indices) {
            if (arrows[index] == null) continue
            val depth = depthAt(size, arrows, index, memo, visiting)
            if (depth > best) best = depth
        }
        return best
    }

    private fun placeLayout(rng: SplitMix64, config: PackConfig, target: Int): List<ArrowsDirection?> {
        val size = config.size
        val arrows = MutableList<ArrowsDirection?>(size * size) { null }
        val spineDirection = ArrowsDirection.entries[rng.nextInt(ArrowsDirection.entries.size)]
        val line = rng.nextInt(size)
        for (step in 0 until config.minChainDepth) {
            val index = spineIndex(size, spineDirection, line, step, config.minChainDepth)
            require(rayIsClear(size, arrows, index, spineDirection)) {
                "Spine arrow at $index has no clear ray"
            }
            arrows[index] = spineDirection
        }

        var placed = config.minChainDepth
        var usedSecondDirection = false
        while (placed < target) {
            val available = legalPlacements(size, arrows)
            if (available.isEmpty()) {
                require(placed >= config.minArrows) {
                    "Placement stuck at $placed arrows, below the ${config.difficulty} minimum"
                }
                break
            }
            val options = if (!usedSecondDirection) {
                val other = available.filter { it.direction != spineDirection }
                require(other.isNotEmpty()) { "No cell can take a second arrow direction" }
                usedSecondDirection = true
                other
            } else {
                available
            }
            val choice = pickPlacement(rng, size, arrows, options)
            arrows[choice.index] = choice.direction
            placed++
        }

        val frozen = arrows.toList()
        require(placed >= config.minArrows) { "Layout has $placed arrows" }
        require(hasTwoDirections(frozen)) { "Layout uses only one arrow direction" }
        require(chainDepth(size, frozen) >= config.minChainDepth) {
            "Chain is shorter than ${config.minChainDepth}"
        }
        require(ArrowsRules.legalSlides(size, frozen).isNotEmpty()) {
            "Layout has no arrow that can slide"
        }
        require(ArrowsRules.isClearable(size, frozen)) { "Layout is deadlocked" }
        return frozen
    }

    private fun pickPlacement(
        rng: SplitMix64,
        size: Int,
        arrows: List<ArrowsDirection?>,
        options: List<Placement>,
    ): Placement {
        require(options.isNotEmpty()) { "No legal arrow placement" }
        val covered = cellsOnArrowPaths(size, arrows)
        var blockingCount = 0
        for (option in options) {
            if (covered[option.index]) blockingCount++
        }
        // Three rolls in four extend a chain when a cell on an existing ray is free.
        // The other roll stays uniform so more than one arrow can slide at the start.
        if (blockingCount > 0 && rng.nextInt(4) != 0) {
            val choiceNumber = rng.nextInt(blockingCount)
            var seen = 0
            for (option in options) {
                if (!covered[option.index]) continue
                if (seen == choiceNumber) return option
                seen++
            }
            error("Blocking placement $choiceNumber was not in the option list")
        }
        return options[rng.nextInt(options.size)]
    }

    private fun legalPlacements(size: Int, arrows: List<ArrowsDirection?>): List<Placement> {
        val placements = ArrayList<Placement>()
        for (index in arrows.indices) {
            if (arrows[index] != null) continue
            for (direction in ArrowsDirection.entries) {
                if (rayIsClear(size, arrows, index, direction)) {
                    placements.add(Placement(index, direction))
                }
            }
        }
        return placements
    }

    private fun cellsOnArrowPaths(size: Int, arrows: List<ArrowsDirection?>): BooleanArray {
        val covered = BooleanArray(arrows.size)
        for (index in arrows.indices) {
            val direction = arrows[index] ?: continue
            var row = index / size + direction.rowDelta
            var column = index % size + direction.columnDelta
            while (row in 0 until size && column in 0 until size) {
                covered[row * size + column] = true
                row += direction.rowDelta
                column += direction.columnDelta
            }
        }
        return covered
    }

    private fun rayIsClear(
        size: Int,
        arrows: List<ArrowsDirection?>,
        index: Int,
        direction: ArrowsDirection,
    ): Boolean {
        var row = index / size + direction.rowDelta
        var column = index % size + direction.columnDelta
        while (row in 0 until size && column in 0 until size) {
            if (arrows[row * size + column] != null) return false
            row += direction.rowDelta
            column += direction.columnDelta
        }
        return true
    }

    /** [stepFromTail] 0 is the arrow that slides last, furthest from the edge it points toward. */
    private fun spineIndex(
        size: Int,
        direction: ArrowsDirection,
        line: Int,
        stepFromTail: Int,
        length: Int,
    ): Int {
        val fromEdge = length - 1 - stepFromTail
        return when (direction) {
            ArrowsDirection.UP -> fromEdge * size + line
            ArrowsDirection.DOWN -> (size - 1 - fromEdge) * size + line
            ArrowsDirection.LEFT -> line * size + fromEdge
            ArrowsDirection.RIGHT -> line * size + (size - 1 - fromEdge)
        }
    }

    private fun hasTwoDirections(arrows: List<ArrowsDirection?>): Boolean {
        var seen: ArrowsDirection? = null
        for (arrow in arrows) {
            if (arrow == null) continue
            if (seen == null) {
                seen = arrow
            } else if (arrow != seen) {
                return true
            }
        }
        return false
    }

    private data class Placement(val index: Int, val direction: ArrowsDirection)

    private fun depthAt(
        size: Int,
        arrows: List<ArrowsDirection?>,
        index: Int,
        memo: IntArray,
        visiting: BooleanArray,
    ): Int {
        if (memo[index] >= 0) return memo[index]
        val direction = arrows[index] ?: return 0
        visiting[index] = true
        var maxChild = 0
        var row = index / size + direction.rowDelta
        var column = index % size + direction.columnDelta
        while (row in 0 until size && column in 0 until size) {
            val next = row * size + column
            if (arrows[next] != null && !visiting[next]) {
                val child = depthAt(size, arrows, next, memo, visiting)
                if (child > maxChild) maxChild = child
            }
            row += direction.rowDelta
            column += direction.columnDelta
        }
        visiting[index] = false
        val depth = maxChild + 1
        memo[index] = depth
        return depth
    }
}
