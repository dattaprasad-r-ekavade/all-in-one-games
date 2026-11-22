package com.datathecodie.witgrid.games

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.datathecodie.witgrid.R
import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.PlayBankLine
import com.datathecodie.witgrid.engine.core.PlayEngine
import java.io.InputStream

/**
 * App-facing wrapper for one Family B [PlayEngine], parallel to [GameModule] for grids.
 * Home, Play, Result, Continue and saves call these helpers until a dedicated play
 * controller exists for action-based boards.
 */
interface PlayGameModule<P : Any, S : Any, A : Any> {
    val id: String
    val engine: PlayEngine<P, S, A>

    /** Finished levels between two interstitial ads for [level] (plan 8.1). */
    fun levelsPerInterstitial(level: GameLevel<P>): Int

    /** All levels of [pack], in bank order; an intro is first in Easy when the game has one. */
    fun levelRefs(pack: Pack, bank: BankSource): List<LevelRef>

    fun firstLevelRef(pack: Pack, bank: BankSource): LevelRef

    fun nextLevelRef(current: LevelRef, bank: BankSource): LevelRef?

    fun dailyRef(dayIndex: Int, bank: BankSource): LevelRef

    fun loadLevel(ref: LevelRef, bank: BankSource): GameLevel<P>

    fun newState(level: GameLevel<P>): S = engine.initialState(level.puzzle)

    fun applyAction(state: S, action: A): S = engine.applyAction(state, action)

    fun applyActions(state: S, actions: List<A>): S =
        actions.fold(state) { currentState, action -> engine.applyAction(currentState, action) }

    fun isWon(level: GameLevel<P>, state: S): Boolean = engine.isWon(level.puzzle, state)

    fun isLost(level: GameLevel<P>, state: S): Boolean = engine.isLost(level.puzzle, state)

    fun hintAction(level: GameLevel<P>, state: S): A? = engine.hintAction(level.puzzle, state)

    fun applyContinue(state: S): S = engine.applyContinue(state)

    fun encodeState(state: S): ByteArray = engine.encodeState(state)

    fun decodeState(bytes: ByteArray): S = engine.decodeState(bytes)

    fun stateMatchesLevel(level: GameLevel<P>, state: S): Boolean

    @Composable
    fun Board(state: S, onAction: (A) -> Unit, onUndo: () -> Unit)
}

/**
 * Family B modules discovered at runtime. Append entries when a lane ships; Home must
 * not hard-code how many games exist.
 */
object PlayRegistry {
    val games: List<PlayGameModule<*, *, *>> = emptyList()

    private val gamesById = games.associateBy { it.id }

    fun find(gameId: String): PlayGameModule<*, *, *>? = gamesById[gameId]
}

/**
 * Wraps [engine] for the app. [puzzleOf] reads the puzzle embedded in a state, which the
 * engine interface does not expose. [interstitialRule] gives the interval for each level.
 */
class EnginePlayGameModule<P : Any, S : Any, A : Any>(
    override val engine: PlayEngine<P, S, A>,
    private val puzzleOf: (S) -> P,
    private val intro: GameIntro? = null,
    private val interstitialRule: (GameLevel<P>) -> Int,
) : PlayGameModule<P, S, A> {
    private class BankEntry(val raw: String, val bankLine: PlayBankLine)

    private val parsedBankFiles = mutableMapOf<String, List<BankEntry>>()

    override val id: String = engine.id

    override fun stateMatchesLevel(level: GameLevel<P>, state: S): Boolean =
        engine.encodePuzzle(puzzleOf(state)) == engine.encodePuzzle(level.puzzle)

    override fun levelsPerInterstitial(level: GameLevel<P>): Int =
        interstitialRule(level).also { require(it > 0) { "Interstitial interval must be positive" } }

    override fun levelRefs(pack: Pack, bank: BankSource): List<LevelRef> {
        val packRefs = bankEntries(bankAssetPath(pack), bank).map { entry ->
            LevelRef(id, entry.bankLine.id, pack)
        }
        if (pack != Pack.EASY || intro == null) return numberLevels(packRefs)

        val introEntry = introBankEntry(bank)
        require(packRefs.none { it.levelId == introEntry.bankLine.id }) {
            "Intro level ID ${introEntry.bankLine.id} is already used in the Easy bank"
        }
        val introRef = LevelRef(
            gameId = id,
            levelId = introEntry.bankLine.id,
            pack = Pack.EASY,
            introCoachLineResourceId = intro.coachLineResourceId,
        )
        return listOf(introRef) + numberLevels(packRefs)
    }

    override fun firstLevelRef(pack: Pack, bank: BankSource): LevelRef =
        levelRefs(pack, bank).firstOrNull()
            ?: throw IllegalStateException("Puzzle bank is empty for $id ${pack.name}")

    override fun nextLevelRef(current: LevelRef, bank: BankSource): LevelRef? {
        require(current.gameId == id) { "Level belongs to ${current.gameId}, not $id" }
        if (current.daily) return null
        val refs = levelRefs(current.pack, bank)
        val bankRefs = refs.filterNot { it.isIntro }
        if (current.isIntro) return LevelRef.forLevel(1L, bankRefs)
        val index = bankRefs.indexOfFirst { ref -> ref.levelId == current.levelId }
        require(index >= 0) { "Level ${current.levelId} is not in the ${current.pack} pack" }
        return LevelRef.forLevel(Math.addExact(current.levelNumber ?: (index + 1L), 1L), bankRefs)
    }

    override fun dailyRef(dayIndex: Int, bank: BankSource): LevelRef {
        require(dayIndex in 0..364) { "Daily index must be from 0 to 364" }
        val entry = bankEntries(dailyAssetPath(), bank).getOrNull(dayIndex)
            ?: throw NoSuchElementException("The daily bank has no entry for day $dayIndex")
        return LevelRef(id, entry.bankLine.id, entry.bankLine.pack, daily = true)
    }

    override fun loadLevel(ref: LevelRef, bank: BankSource): GameLevel<P> {
        require(ref.gameId == id) { "Level belongs to ${ref.gameId}, not $id" }
        val path = when {
            ref.daily -> dailyAssetPath()
            ref.isIntro -> {
                val definition = requireNotNull(intro) { "Game $id has no intro level" }
                require(ref.pack == Pack.EASY && ref.levelId == definition.levelId) {
                    "Level ${ref.levelId} is not the configured intro for $id"
                }
                require(ref.introCoachLineResourceId == definition.coachLineResourceId) {
                    "Intro coach line does not match the configured intro for $id"
                }
                introAssetPath()
            }
            else -> bankAssetPath(ref.pack)
        }
        val entry = bankEntries(path, bank).firstOrNull { it.bankLine.id == ref.levelId }
            ?: throw NoSuchElementException("Level ${ref.levelId} is not in the ${bankName(ref)}")
        if (ref.isIntro) {
            require(entry.bankLine.pack == Pack.EASY) { "Intro level ${ref.levelId} must use the Easy pack" }
        } else if (!ref.daily) {
            require(entry.bankLine.pack == ref.pack) { "Level ${ref.levelId} does not use the ${ref.pack} pack" }
        }
        val numberedRef = if (ref.daily) {
            ref
        } else if (ref.isIntro) {
            ref.copy(levelNumber = null, bankIndex = null)
        } else {
            val refs = levelRefs(ref.pack, bank)
            val bankRefs = refs.filterNot { it.isIntro }
            val index = bankRefs.indexOfFirst { it.levelId == ref.levelId }
            require(index >= 0) { "Level is not in the pack" }
            val numbered = LevelRef.forLevel(ref.levelNumber ?: (index + 1L), bankRefs)
            require(numbered.levelId == ref.levelId) { "Level number does not match bank puzzle" }
            numbered
        }
        return GameLevel(numberedRef, entry.bankLine.size, engine.decodePuzzle(entry.raw))
    }

    @Composable
    @Suppress("UNUSED_PARAMETER")
    override fun Board(state: S, onAction: (A) -> Unit, onUndo: () -> Unit) {
        Text(stringResource(R.string.board_coming_soon))
    }

    private fun numberLevels(refs: List<LevelRef>): List<LevelRef> =
        refs.mapIndexed { index, ref -> ref.copy(levelNumber = index + 1L, bankIndex = index) }

    private fun bankEntries(path: String, bank: BankSource): List<BankEntry> =
        synchronized(parsedBankFiles) {
            parsedBankFiles.getOrPut(path) { readBankFile(path, bank) }
        }

    private fun introBankEntry(bank: BankSource): BankEntry {
        val definition = requireNotNull(intro)
        val entries = bankEntries(introAssetPath(), bank)
        require(entries.size == 1) { "Intro bank for $id must contain exactly one puzzle" }
        val entry = entries.single()
        require(entry.bankLine.id == definition.levelId) {
            "Intro bank for $id must use level ID ${definition.levelId}"
        }
        require(entry.bankLine.pack == Pack.EASY) { "Intro bank for $id must use the Easy pack" }
        return entry
    }

    private fun readBankFile(path: String, bank: BankSource): List<BankEntry> =
        bank.open(path).bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.map { raw -> BankEntry(raw, PlayBankLine.decode(raw)) }.toList()
        }

    private fun bankName(ref: LevelRef): String =
        when {
            ref.daily -> "daily bank"
            ref.isIntro -> "intro bank"
            else -> "${ref.pack} pack"
        }

    private fun bankAssetPath(pack: Pack): String =
        "bank/$id-${pack.name.lowercase()}.txt.gz"

    private fun introAssetPath(): String = "bank/$id-intro.txt"

    private fun dailyAssetPath(): String = "bank/$id-daily.txt.gz"
}