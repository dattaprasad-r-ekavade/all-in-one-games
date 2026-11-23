package com.datathecodie.witgrid.engine.coloursort

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64

object ColourSortGenerator {
    private const val MAX_ATTEMPTS = 500

    data class PackConfig(
        val tubeCount: Int,
        val colorCount: Int,
        val emptyTubeCount: Int,
        val difficulty: String,
        val minShuffleSteps: Int,
        val maxShuffleSteps: Int,
    )

    fun configFor(pack: Pack): PackConfig = when (pack) {
        Pack.EASY -> PackConfig(
            tubeCount = 4,
            colorCount = 3,
            emptyTubeCount = 1,
            difficulty = "d1",
            minShuffleSteps = 10,
            maxShuffleSteps = 25,
        )
        Pack.MEDIUM -> PackConfig(
            tubeCount = 6,
            colorCount = 4,
            emptyTubeCount = 2,
            difficulty = "d2",
            minShuffleSteps = 15,
            maxShuffleSteps = 35,
        )
        Pack.HARD -> PackConfig(
            tubeCount = 8,
            colorCount = 6,
            emptyTubeCount = 2,
            difficulty = "d3",
            minShuffleSteps = 20,
            maxShuffleSteps = 45,
        )
        Pack.EXPERT -> PackConfig(
            tubeCount = 10,
            colorCount = 8,
            emptyTubeCount = 2,
            difficulty = "d4",
            minShuffleSteps = 25,
            maxShuffleSteps = 55,
        )
    }

    fun generate(rng: SplitMix64, pack: Pack): ColourSortPuzzle {
        val config = configFor(pack)

        repeat(MAX_ATTEMPTS) {
            val candidateTubes = tryGenerateTubes(rng, config)
            if (candidateTubes != null) {
                val hex = rng.nextLong().toULong().toString(16).padStart(16, '0')
                val id = "CS-${pack.name}-$hex"
                return ColourSortPuzzle(
                    bankId = id,
                    pack = pack,
                    difficulty = config.difficulty,
                    initialTubes = candidateTubes,
                )
            }
        }

        throw IllegalStateException("Failed to generate a valid Colour Sort puzzle for pack $pack")
    }

    internal fun tryGenerateTubes(rng: SplitMix64, config: PackConfig): List<List<Int>>? {
        val tubes = ArrayList<MutableList<Int>>(config.tubeCount)
        for (color in 1..config.colorCount) {
            tubes.add(MutableList(ColourSortRules.TUBE_CAPACITY) { color })
        }
        repeat(config.emptyTubeCount) {
            tubes.add(mutableListOf())
        }

        val maxSteps = config.maxShuffleSteps + 30
        var lastFrom = -1
        var lastTo = -1

        for (step in 1..maxSteps) {
            val legalMoves = legalReverseMoves(tubes)
            if (legalMoves.isEmpty()) break

            val filtered = legalMoves.filterNot { it.from == lastTo && it.to == lastFrom }
            val candidateMoves = if (filtered.isNotEmpty()) filtered else legalMoves

            val currentEmpty = tubes.count { it.isEmpty() }
            val chosenMove = if (step >= config.minShuffleSteps && currentEmpty < config.emptyTubeCount) {
                // Bias towards moves that empty a tube
                val emptyingMoves = candidateMoves.filter { it.units == tubes[it.from].size }
                if (emptyingMoves.isNotEmpty()) {
                    emptyingMoves[rng.nextInt(emptyingMoves.size)]
                } else {
                    candidateMoves[rng.nextInt(candidateMoves.size)]
                }
            } else if (step >= config.minShuffleSteps && currentEmpty > config.emptyTubeCount) {
                // Bias towards moves that fill an empty tube
                val fillingMoves = candidateMoves.filter { tubes[it.to].isEmpty() }
                if (fillingMoves.isNotEmpty()) {
                    fillingMoves[rng.nextInt(fillingMoves.size)]
                } else {
                    candidateMoves[rng.nextInt(candidateMoves.size)]
                }
            } else {
                candidateMoves[rng.nextInt(candidateMoves.size)]
            }

            applyReverseMove(tubes, chosenMove)
            lastFrom = chosenMove.from
            lastTo = chosenMove.to

            if (step >= config.minShuffleSteps) {
                val emptyCount = tubes.count { it.isEmpty() }
                if (emptyCount == config.emptyTubeCount) {
                    val allFullOrEmpty = tubes.all { it.isEmpty() || it.size == ColourSortRules.TUBE_CAPACITY }
                    val noneSorted = tubes.none { it.isNotEmpty() && ColourSortRules.isTubeSorted(it) }
                    if (allFullOrEmpty && noneSorted) {
                        return tubes.map { it.toList() }
                    }
                }
            }
        }

        return null
    }

    internal data class ReverseMove(val from: Int, val to: Int, val units: Int)

    internal fun legalReverseMoves(tubes: List<List<Int>>): List<ReverseMove> {
        val moves = ArrayList<ReverseMove>()
        for (source in tubes.indices) {
            val sourceTube = tubes[source]
            if (sourceTube.isEmpty()) continue
            val maximumUnits = ColourSortRules.topRunLength(sourceTube)
            for (destination in tubes.indices) {
                if (source == destination) continue
                val destinationSpace = ColourSortRules.freeSpace(tubes[destination])
                if (destinationSpace == 0) continue
                val limit = minOf(maximumUnits, destinationSpace)
                for (units in 1..limit) {
                    val candidate = tubes.map { it.toMutableList() }
                    val colour = sourceTube.last()
                    repeat(units) {
                        candidate[source].removeAt(candidate[source].lastIndex)
                        candidate[destination].add(colour)
                    }
                    val candidateTubes = candidate.map { it.toList() }
                    if (ColourSortRules.isPourLegal(candidateTubes, from = destination, to = source)) {
                        val poured = ColourSortRules.applyPour(candidateTubes, from = destination, to = source)
                        if (poured == tubes) {
                            moves.add(ReverseMove(from = source, to = destination, units = units))
                        }
                    }
                }
            }
        }
        return moves
    }

    private fun applyReverseMove(tubes: ArrayList<MutableList<Int>>, move: ReverseMove) {
        val colour = tubes[move.from].last()
        repeat(move.units) {
            tubes[move.from].removeAt(tubes[move.from].lastIndex)
            tubes[move.to].add(colour)
        }
    }
}
