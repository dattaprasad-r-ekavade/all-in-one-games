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
import java.io.InputStream
import java.util.zip.GZIPInputStream

/** Identifies one level. Daily levels come from the daily bank file, not from a pack. */
data class LevelRef(
    val gameId: String,
    val levelId: String,
    val pack: Pack,
    val daily: Boolean = false,
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

/** Opens a bank asset by path, such as `bank/crowns-easy.txt.gz`. Tests pass a fake. */
interface BankSource {
    fun open(path: String): InputStream
}

/** Reads bank assets from the app assets folder. */
class AssetBankSource(private val assets: AssetManager) : BankSource {
    override fun open(path: String): InputStream = assets.open(path)
}

interface GameModule<P : Any, S : Any> {
    val id: String
    val engine: GameEngine<P, S>

    /** Finished levels between two interstitial ads for [level] (plan 8.1). */
    fun levelsPerInterstitial(level: GameLevel<P>): Int

    /** All levels of [pack], in bank order. */
    fun levelRefs(pack: Pack, bank: BankSource): List<LevelRef>

    fun firstLevelRef(pack: Pack, bank: BankSource): LevelRef

    /** The level after [current] in its pack, or null after the last level. */
    fun nextLevelRef(current: LevelRef, bank: BankSource): LevelRef?

    /** The daily level for [dayIndex], from 0 to 364. */
    fun dailyRef(dayIndex: Int, bank: BankSource): LevelRef

    fun loadLevel(ref: LevelRef, bank: BankSource): GameLevel<P>

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

    /** True when the puzzle embedded in [state] is the puzzle of [level]. */
    fun stateMatchesLevel(level: GameLevel<P>, state: S): Boolean

    @Composable
    fun Board(state: S, onMoves: (List<Move>) -> Unit)
}

/**
 * Wraps [engine] for the app. [puzzleOf] reads the puzzle embedded in a state, which the
 * engine interface does not expose. [interstitialRule] gives the interval for each level, so a
 * game with size-based rules sets them where it is registered, not in shared code.
 */
class EngineGameModule<P : Any, S : Any>(
    override val engine: GameEngine<P, S>,
    private val puzzleOf: (S) -> P,
    private val interstitialRule: (GameLevel<P>) -> Int,
) : GameModule<P, S> {
    private class BankEntry(val raw: String, val bankLine: BankLine)

    /** Bank files are small. Each file is parsed once per module, not once per call. */
    private val parsedBankFiles = mutableMapOf<String, List<BankEntry>>()

    override val id: String = engine.id

    override fun stateMatchesLevel(level: GameLevel<P>, state: S): Boolean =
        engine.encodePuzzle(puzzleOf(state)) == engine.encodePuzzle(level.puzzle)

    override fun levelsPerInterstitial(level: GameLevel<P>): Int =
        interstitialRule(level).also { require(it > 0) { "Interstitial interval must be positive" } }

    override fun levelRefs(pack: Pack, bank: BankSource): List<LevelRef> =
        bankEntries(bankAssetPath(pack), bank).map { entry -> LevelRef(id, entry.bankLine.id, pack) }

    override fun firstLevelRef(pack: Pack, bank: BankSource): LevelRef =
        levelRefs(pack, bank).firstOrNull()
            ?: throw IllegalStateException("Puzzle bank is empty for $id ${pack.name}")

    override fun nextLevelRef(current: LevelRef, bank: BankSource): LevelRef? {
        require(current.gameId == id) { "Level belongs to ${current.gameId}, not $id" }
        // Daily levels have no order inside a pack, so they have no next level.
        if (current.daily) return null
        val refs = levelRefs(current.pack, bank)
        val index = refs.indexOfFirst { ref -> ref.levelId == current.levelId }
        require(index >= 0) { "Level ${current.levelId} is not in the ${current.pack} pack" }
        return refs.getOrNull(index + 1)
    }

    override fun dailyRef(dayIndex: Int, bank: BankSource): LevelRef {
        require(dayIndex in 0..364) { "Daily index must be from 0 to 364" }
        val entry = bankEntries(dailyAssetPath(), bank).getOrNull(dayIndex)
            ?: throw NoSuchElementException("The daily bank has no entry for day $dayIndex")
        return LevelRef(id, entry.bankLine.id, entry.bankLine.pack, daily = true)
    }

    override fun loadLevel(ref: LevelRef, bank: BankSource): GameLevel<P> {
        require(ref.gameId == id) { "Level belongs to ${ref.gameId}, not $id" }
        val path = if (ref.daily) dailyAssetPath() else bankAssetPath(ref.pack)
        val entry = bankEntries(path, bank).firstOrNull { it.bankLine.id == ref.levelId }
            ?: throw NoSuchElementException("Level ${ref.levelId} is not in the ${bankName(ref)}")
        return GameLevel(ref, entry.bankLine.size, engine.decodePuzzle(entry.raw))
    }

    @Composable
    override fun Board(state: S, onMoves: (List<Move>) -> Unit) {
        Text(stringResource(R.string.board_coming_soon))
    }

    private fun bankEntries(path: String, bank: BankSource): List<BankEntry> =
        synchronized(parsedBankFiles) {
            parsedBankFiles.getOrPut(path) { readBankFile(path, bank) }
        }

    private fun readBankFile(path: String, bank: BankSource): List<BankEntry> =
        bank.open(path).use { stream ->
            GZIPInputStream(stream).bufferedReader(Charsets.UTF_8).useLines { lines ->
                lines.map { raw -> BankEntry(raw, BankLine.decode(raw)) }.toList()
            }
        }

    private fun bankName(ref: LevelRef): String =
        if (ref.daily) "daily bank" else "${ref.pack} pack"

    private fun bankAssetPath(pack: Pack): String =
        "bank/$id-${pack.name.lowercase()}.txt.gz"

    private fun dailyAssetPath(): String = "bank/$id-daily.txt.gz"
}
