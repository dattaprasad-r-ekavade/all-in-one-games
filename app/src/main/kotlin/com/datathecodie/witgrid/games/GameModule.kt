package com.datathecodie.witgrid.games

import android.content.res.AssetManager
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.engine.core.BankLine
import com.datathecodie.witgrid.engine.core.Cell
import com.datathecodie.witgrid.engine.core.GameEngine
import com.datathecodie.witgrid.engine.core.HintStep
import com.datathecodie.witgrid.engine.core.Move
import com.datathecodie.witgrid.engine.core.Pack
import java.util.zip.GZIPInputStream

data class LevelRef(
    val gameId: String,
    val levelId: String,
    val pack: Pack,
) {
    init {
        require(gameId.isNotBlank()) { "Game ID must not be blank" }
        require(levelId.isNotBlank()) { "Level ID must not be blank" }
    }
}

data class GameLevel<P : Any>(
    val ref: LevelRef,
    val size: Int,
    val puzzle: P,
)

interface GameModule<P : Any, S : Any> {
    val id: String
    val engine: GameEngine<P, S>
    val levelsPerInterstitial: Int

    fun levelsPerInterstitial(level: GameLevel<P>): Int = levelsPerInterstitial

    fun firstLevelRef(pack: Pack, assets: AssetManager): LevelRef

    fun loadLevel(ref: LevelRef, assets: AssetManager): GameLevel<P>

    fun newState(level: GameLevel<P>): S = engine.initialState(level.puzzle)

    fun applyMove(state: S, move: Move): S = engine.applyMove(state, move)

    fun applyMoves(state: S, moves: List<Move>): S =
        moves.fold(state) { currentState, move -> engine.applyMove(currentState, move) }

    fun isSolved(level: GameLevel<P>, state: S): Boolean = engine.isSolved(level.puzzle, state)

    fun findMistakes(level: GameLevel<P>, state: S): List<Cell> =
        engine.mistakes(level.puzzle, state)

    fun nextHint(level: GameLevel<P>, state: S): HintStep? =
        engine.nextStep(level.puzzle, state)

    fun encodeState(state: S): ByteArray = engine.encodeState(state)

    fun decodeState(bytes: ByteArray): S = engine.decodeState(bytes)

    @Composable
    fun Board(state: S, onMoves: (List<Move>) -> Unit)
}

class EngineGameModule<P : Any, S : Any>(
    override val engine: GameEngine<P, S>,
    override val levelsPerInterstitial: Int,
) : GameModule<P, S> {
    init {
        require(levelsPerInterstitial > 0) { "Interstitial interval must be positive" }
    }

    override val id: String = engine.id

    override fun levelsPerInterstitial(level: GameLevel<P>): Int =
        if (id == "nonogram" && level.size == 5) 2 else levelsPerInterstitial

    override fun firstLevelRef(pack: Pack, assets: AssetManager): LevelRef {
        val firstLine = GZIPInputStream(assets.open(bankAssetPath(pack)))
            .bufferedReader(Charsets.UTF_8)
            .useLines { lines -> lines.firstOrNull() }
            ?: throw IllegalStateException("Puzzle bank is empty for $id ${pack.name}")
        val bankLine = BankLine.decode(firstLine)
        return LevelRef(id, bankLine.id, pack)
    }

    override fun loadLevel(ref: LevelRef, assets: AssetManager): GameLevel<P> {
        require(ref.gameId == id) { "Level belongs to ${ref.gameId}, not $id" }
        val matchingLine = GZIPInputStream(assets.open(bankAssetPath(ref.pack)))
            .bufferedReader(Charsets.UTF_8)
            .useLines { lines ->
                lines.firstOrNull { line -> BankLine.decode(line).id == ref.levelId }
            }
            ?: throw NoSuchElementException("Level ${ref.levelId} is not in the ${ref.pack} pack")
        val bankLine = BankLine.decode(matchingLine)
        return GameLevel(ref, bankLine.size, engine.decodePuzzle(matchingLine))
    }

    @Composable
    override fun Board(state: S, onMoves: (List<Move>) -> Unit) {
        Text(stringResource(R.string.board_coming_soon))
    }

    private fun bankAssetPath(pack: Pack): String =
        "bank/$id-${pack.name.lowercase()}.txt.gz"
}
