package com.datathecodie.witgrid.engine.klondike

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KlondikeSolverAndGeneratorTest {
    private val engine = KlondikeEngine

    @Test
    fun seedOneGeneratesTenWinnableDeterministicLevelsPerPack() {
        val idPattern = Regex("^KL-[A-Z]+-[0-9a-f]{16}$")
        val averageMovesByPack = mutableMapOf<Pack, Double>()

        for (pack in Pack.entries) {
            val startNanos = System.nanoTime()
            val seqRng1 = SplitMix64(1L)
            val puzzles1 = List(10) { engine.generate(seqRng1, pack) }
            val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000

            val seqRng2 = SplitMix64(1L)
            val puzzles2 = List(10) { engine.generate(seqRng2, pack) }

            assertEquals(puzzles1, puzzles2, "Determinism failed for pack $pack")
            assertEquals(10, puzzles1.size)

            var totalMovesSum = 0
            for ((index, puzzle) in puzzles1.withIndex()) {
                assertEquals(pack, puzzle.pack)
                assertEquals(KlondikeGenerator.configFor(pack).difficulty, puzzle.difficulty)
                assertTrue(idPattern.matches(puzzle.bankId), "Bad ID ${puzzle.bankId}")
                assertEquals(puzzle, engine.decodePuzzle(engine.encodePuzzle(puzzle)))

                val initial = engine.initialState(puzzle)
                assertFalse(engine.isLost(puzzle, initial), "$pack #$index should not start lost")
                assertFalse(engine.isWon(puzzle, initial), "$pack #$index should not start won")

                val solution = KlondikeSolver.solve(initial)
                assertNotNull(solution, "$pack #$index should be winnable")
                totalMovesSum += solution!!.size

                val finalState = solution.fold(initial) { state, move -> engine.applyAction(state, move) }
                assertTrue(engine.isWon(puzzle, finalState), "$pack #$index solution did not reach win")
            }

            averageMovesByPack[pack] = totalMovesSum / 10.0
            println("Pack $pack: 10 puzzles generated (avg moves=${averageMovesByPack[pack]}) in ${elapsedMs}ms")
        }

        assertTrue(
            averageMovesByPack.getValue(Pack.EASY) < averageMovesByPack.getValue(Pack.HARD),
            "Easy should average fewer moves than Hard: $averageMovesByPack",
        )
        assertTrue(
            averageMovesByPack.getValue(Pack.MEDIUM) < averageMovesByPack.getValue(Pack.HARD),
            "Medium should average fewer moves than Hard: $averageMovesByPack",
        )
        assertTrue(
            averageMovesByPack.getValue(Pack.HARD) <= averageMovesByPack.getValue(Pack.EXPERT),
            "Hard should not exceed Expert average moves: $averageMovesByPack",
        )
    }

    @Test
    fun generatorConfigScalesWithPackDifficulty() {
        val easy = KlondikeGenerator.configFor(Pack.EASY)
        val medium = KlondikeGenerator.configFor(Pack.MEDIUM)
        val hard = KlondikeGenerator.configFor(Pack.HARD)
        val expert = KlondikeGenerator.configFor(Pack.EXPERT)

        assertEquals("d1", easy.difficulty)
        assertEquals("d2", medium.difficulty)
        assertEquals("d3", hard.difficulty)
        assertEquals("d4", expert.difficulty)

        assertTrue(easy.minNonDrawMoves <= medium.minNonDrawMoves)
        assertTrue(medium.minNonDrawMoves < hard.minNonDrawMoves)
        assertTrue(hard.minNonDrawMoves < expert.minNonDrawMoves)
        assertTrue(medium.minRecycleCount < hard.minRecycleCount)
    }

    @Test
    fun followHintsFromDealToWinOnEasyLevels() {
        val rng = SplitMix64(1L)
        val puzzles = List(10) { engine.generate(rng, Pack.EASY) }
        for ((index, puzzle) in puzzles.withIndex()) {
            assertFollowsHintsToWin(puzzle, "EASY #$index")
        }
    }

    @Test
    fun followHintsFromDealToWinOnMediumSample() {
        val rng = SplitMix64(1L)
        val puzzles = List(10) { engine.generate(rng, Pack.MEDIUM) }
        for (index in listOf(0, 8)) {
            assertFollowsHintsToWin(puzzles[index], "MEDIUM #$index")
        }
    }

    private fun assertFollowsHintsToWin(
        puzzle: KlondikePuzzle,
        label: String,
        maxSteps: Int = 260,
    ) {
        var state = engine.initialState(puzzle)
        var steps = 0
        while (!engine.isWon(puzzle, state) && steps < maxSteps) {
            assertFalse(
                engine.isLost(puzzle, state),
                "$label became lost while following hints at step $steps (seed=${puzzle.seed})",
            )
            val hint = engine.hintAction(puzzle, state)
            assertNotNull(hint, "$label hint returned null at step $steps (seed=${puzzle.seed})")
            state = engine.applyAction(state, hint!!)
            steps++
        }
        assertTrue(
            engine.isWon(puzzle, state),
            "$label did not win by following hints within $maxSteps steps (took $steps)",
        )
    }

    @Test
    fun fiftyTwoCardReversibleLoopWithoutWinningLineIsLostAfterPlay() {
        // Construct a full 52-card state where 9S can bounce back and forth between 10H and 10D,
        // and no winning line exists because Aces/low ranks are locked beneath higher face-up cards
        // with no empty columns and no playable stock/waste cards.
        val used = HashSet<Pair<Int, Suit>>()
        fun take(rank: Int, suit: Suit, faceUp: Boolean): Card {
            assertTrue(used.add(rank to suit), "Duplicate card $rank $suit")
            return Card(rank, suit, faceUp)
        }

        // Foundations: empty.
        // All 4 Kings sit on col 0..3 over face-down Aces/Twos (no empty columns can be formed).
        // 10H and 10D sit on top of col 0 and col 1, and 9S can bounce between them forever.
        val tableau = listOf(
            listOf(take(1, Suit.SPADES, false), take(13, Suit.SPADES, true), take(10, Suit.HEARTS, true), take(9, Suit.SPADES, true)),
            listOf(take(1, Suit.HEARTS, false), take(13, Suit.HEARTS, true), take(10, Suit.DIAMONDS, true)),
            listOf(take(1, Suit.CLUBS, false), take(13, Suit.CLUBS, true)),
            listOf(take(1, Suit.DIAMONDS, false), take(13, Suit.DIAMONDS, true)),
            listOf(take(2, Suit.SPADES, false), take(9, Suit.CLUBS, true)),
            listOf(take(2, Suit.HEARTS, false), take(9, Suit.HEARTS, true)),
            listOf(take(2, Suit.CLUBS, false), take(9, Suit.DIAMONDS, true)),
        )

        // Ensure remaining stock cards cannot be played on 10H, 10D, 13C, 13D, 9S, 9C, 9H, 9D or empty cols:
        // No 8s? Actually if 8H/8D are in stock, 8H can go onto 9S/9C!
        // Put 8H, 8D, 8C, 8S face-down beneath the Kings, and put 11..12 + 3..7 in stock?
        // Wait: 12S/12C can go onto 13D/13H! So put ALL 12s face-down too!
        // Wait: even simpler! Put 4..7 on foundations so we don't care? No, Aces are face down!
        // Just put 12s and 8s face-down under Kings/9s so stock ONLY has 2D, 3..7, 10S, 10C, 11s:
        // Wait: 11S/11C can go onto 12s if 12s are face-up, NOT if 12s are face-down!
        // Check: exposed tableau tops are: 9S, 10D, 13C, 13D, 9C, 9H, 9D.
        // Cards playable onto those exposed tops are: 8H, 8D, 9S/9C, 12S/12C, 12H/12D, 8S, 8C!
        // So bury ALL 8s and 12s face-down!
        used.clear()
        val lockedTableau = listOf(
            listOf(take(1, Suit.SPADES, false), take(8, Suit.HEARTS, false), take(13, Suit.SPADES, true), take(10, Suit.HEARTS, true), take(9, Suit.SPADES, true)),
            listOf(take(1, Suit.HEARTS, false), take(8, Suit.DIAMONDS, false), take(13, Suit.HEARTS, true), take(10, Suit.DIAMONDS, true)),
            listOf(take(1, Suit.CLUBS, false), take(8, Suit.CLUBS, false), take(12, Suit.CLUBS, false), take(13, Suit.CLUBS, true)),
            listOf(take(1, Suit.DIAMONDS, false), take(8, Suit.SPADES, false), take(12, Suit.DIAMONDS, false), take(13, Suit.DIAMONDS, true)),
            listOf(take(2, Suit.SPADES, false), take(12, Suit.SPADES, false), take(9, Suit.CLUBS, true)),
            listOf(take(2, Suit.HEARTS, false), take(12, Suit.HEARTS, false), take(9, Suit.HEARTS, true)),
            listOf(take(2, Suit.CLUBS, false), take(9, Suit.DIAMONDS, true)),
        )
        val remaining = KlondikeDeal.createStandardDeck()
            .filter { (it.rank to it.suit) !in used }
            .map { it.copy(faceUp = false) }
        assertEquals(52, lockedTableau.sumOf { it.size } + remaining.size)

        val puzzle = KlondikePuzzle("kl-loop-52", Pack.MEDIUM, "d2", seed = 77L)
        val start = KlondikeState(
            puzzle = puzzle,
            tableau = lockedTableau,
            foundations = Suit.entries.associateWith { emptyList() },
            stock = remaining,
            waste = emptyList(),
            moveCount = 0,
        )

        // Legal reversible moves exist (9S can move from col 0 onto 10D in col 1, and stock can be drawn).
        assertFalse(KlondikeRules.isDeadlocked(start))
        // Contract: moveCount == 0 is never lost.
        assertFalse(engine.isLost(puzzle, start))
        // No winning line exists, so hintAction must return null.
        assertNull(engine.hintAction(puzzle, start))

        // After one reversible move (moving 9S from col 0 onto 10D in col 1), isLost becomes true.
        val afterBounce = engine.applyAction(start, CardMove.TableauToTableau(0, 4, 1))
        assertFalse(KlondikeRules.isDeadlocked(afterBounce))
        assertTrue(engine.isLost(puzzle, afterBounce), "Reversible-only unwinnable 52-card state must be lost")
        assertNull(engine.hintAction(puzzle, afterBounce))

        // Continue restores pre-bounce state with one-action grace.
        val continued = engine.applyContinue(afterBounce)
        assertEquals(1, continued.extraUndos)
        assertFalse(engine.isLost(puzzle, continued))
    }

    @Test
    fun partialFixtureWithOnlyReversibleRunBounceIsLost() {
        val puzzle = KlondikePuzzle("kl-partial-loop", Pack.EASY, "d1", seed = 1L)
        val state = KlondikeState(
            puzzle = puzzle,
            tableau = listOf(
                listOf(Card(9, Suit.SPADES, true), Card(8, Suit.HEARTS, true)),
                listOf(Card(9, Suit.CLUBS, true)),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
            ),
            foundations = Suit.entries.associateWith { emptyList() },
            stock = emptyList(),
            waste = emptyList(),
            moveCount = 2,
        )
        assertFalse(KlondikeRules.isDeadlocked(state))
        assertTrue(engine.isLost(puzzle, state))
    }

    @Test
    fun fatalMistakeOnWinnableStateCanRecoverViaContinueAndHintToWin() {
        // Foundations: D=1..9, C=1..7, H=1..7, S=1..7 (30 cards).
        // Put 9C on col 4 (can bounce onto 10D? No, 10D is down; put 9S(up) on col 4 and 10H(up) on col 5
        //? No: we want `afterMistake` to still have a legal reversible move so `isDeadlocked == false`,
        // e.g. 9C bouncing between 10D and 10H, OR 11H in stock playable onto 12C?
        // Actually even simpler: keep 12C face-up on col 5 (`[8C(down), 8S(down), 12C(up)]`), and put 11H in stock!
        // Look: if 11H is in stock, wait — can 11H go onto 12C and flip anything? No! 12C is at index 2; wait if 11H goes onto 12C, 12C isn't freed!
        // Better: put 12C(down) and put `[10S(down), 12D(up)]`? No, put a reversible tableau bounce `[10H(up)]`? No, `10H` is needed down!
        // Put 9C on col 4 (`[8C(down), 8S(down), 12C(down), 9C(up)]`) and add `10D`? No, `10D` is needed under `11S`!
        // Put `9H` on col 5 (`[8H(down), 9S(down), 10S(down), 9H(up)]`) and `10H`?
        // Look! Just add one reversible run bounce on col 4 & col 5:
        // Foundations: D=1..9, C=1..6, H=1..6, S=1..6 (27 cards). Remaining: D=10..13 (4), C/H/S=7..13 (21) -> 25 cards!
        // Col 4: [7C(down), 7S(down), 8C(down), 8S(down), 12C(down), 9D? no, 9D is in F! Put 9H(up), 8C? no].
        // Actually keep `9C(up)` on Col 4 and `9H`... wait: if `10S(up)` is on Col 4 (`[7C(down), 8C(down), 12C(down), 10S(up)]`) and `10C(up)` is on Col 5 (`[7S(down), 8S(down), 9S(down), 10C(up)]`) and `9H`? No, `9H` can go on `10S` OR `10C`! Wait: then `9H` going from `10S` to `10C` flips `8C`? No, `9H` sits ON TOP OF `10S`, so Col 4 = `[..., 10S(up), 9H(up)]`, Col 5 = `[..., 10C(up)]`!
        // Wait: if `10S` and `10C` are both face-up, then `11D` (col 6) COULD hold `10S` or `10C`, freeing `12C`! So make Col 4 = `[..., 10C(up)]`? Still `10C` goes on `11D`!
        // Make Col 4 = `[..., 9S(up)]` and Col 5 = `[..., 9C(up)]`? Still `9S`/`9C` go on `10H`/`10D`? No, `10H` and `10D` are face-down!
        // Look! If Col 4 = `[..., 8H(down), 9C(up), 8D?]` -> use `7`s on foundations (`C,H,S = 1..7`, `D = 1..9`, wait: use `C,H,S = 1..6`, `D = 1..9`):
        // Put `7H` on `8S` (Col 4) and `8C` on Col 5!
        // - Col 4: `[7C(down), 7S(down), 12C(down), 8S(up), 7H(up)]` -> `7H` can bounce between `8S` and `8C` (`Col 5`)!
        // - Col 5: `[8H(down), 9S(down), 10S(down), 8C(up)]` -> `8S` and `8C` CANNOT move anywhere (`9D` is on foundation, `9H` and `9C` are face-down under `8S`/`8C`? Wait: put `9H(down), 9C(down)` under `13D`, `13H`!).
        // Check: can `7H` bounce between `8S` (Col 4) and `8C` (Col 5) forever without unlocking anything?
        // YES! Because moving `7H` off `8S` onto `8C` does NOT expose any face-down card (`8S` is still on `12C(down)`, and `7S` is face-down so `8S` cannot go to foundation, and `9H` / `9D` are not available)!
        // Let's verify all 25 remaining cards (D: 10..13 = 4; C,H,S: 7..13 = 21):
        // Col 0: [10D(down), 11S(up)] (2)
        // Col 1: [12H(down), 13C(up), 12D(up)] (3)
        // Col 2: [13S(down), 10C(down), 13D(up)] (3)
        // Col 3: [12S(down), 10H(down), 13H(up)] (3)
        // Col 4: [7C(down), 7S(down), 12C(down), 9C(down), 8S(up), 7H(up)] (6)
        // Col 5: [7H? no 7H is in Col 4! Put: 7D is in F! Put 8H(down), 9S(down), 10S(down), 9H(down), 8C(up)] (5)
        // Col 6: [11D(up)] (1)
        // Total tableau = 2 + 3 + 3 + 3 + 6 + 5 + 1 = 23 cards!
        // Remaining 2 cards: `11C` (in waste!) and `11H` (in stock!) -> 25 cards + 27 foundations = 52 cards!
        // Wait: once `10D` flips and `11D` goes to foundation, Col 0 and Col 6 become empty, so `13C`, `13D`, `13H`, `13S`, `12C`, `9C`, `9H`, `8H`, `7C`, `7S` ALL flip and win!
        // Wait: when `11C` (waste) moves onto `12D` (Col 1) instead of `11S`, `7H` STILL has a legal move (`TableauToTableau(4, 5, 5)`) so `isDeadlocked == false`, yet NO winning line remains!
        val checkFoundations = mapOf(
            Suit.CLUBS to (1..6).map { Card(it, Suit.CLUBS, true) },
            Suit.DIAMONDS to (1..9).map { Card(it, Suit.DIAMONDS, true) },
            Suit.HEARTS to (1..6).map { Card(it, Suit.HEARTS, true) },
            Suit.SPADES to (1..6).map { Card(it, Suit.SPADES, true) },
        )
        val checkTableau = listOf(
            listOf(Card(10, Suit.DIAMONDS, false), Card(11, Suit.SPADES, true)),
            listOf(Card(12, Suit.HEARTS, false), Card(13, Suit.CLUBS, true), Card(12, Suit.DIAMONDS, true)),
            listOf(Card(13, Suit.SPADES, false), Card(10, Suit.CLUBS, false), Card(13, Suit.DIAMONDS, true)),
            listOf(Card(12, Suit.SPADES, false), Card(10, Suit.HEARTS, false), Card(13, Suit.HEARTS, true)),
            listOf(
                Card(7, Suit.CLUBS, false),
                Card(7, Suit.SPADES, false),
                Card(12, Suit.CLUBS, false),
                Card(9, Suit.CLUBS, false),
                Card(8, Suit.SPADES, true),
                Card(7, Suit.HEARTS, true),
            ),
            listOf(
                Card(8, Suit.HEARTS, false),
                Card(9, Suit.SPADES, false),
                Card(10, Suit.SPADES, false),
                Card(9, Suit.HEARTS, false),
                Card(8, Suit.CLUBS, true),
            ),
            listOf(Card(11, Suit.DIAMONDS, true)),
        )
        val mistakeState = KlondikeState(
            puzzle = KlondikePuzzle("kl-mistake-52", Pack.MEDIUM, "d2", seed = 88L),
            tableau = checkTableau,
            foundations = checkFoundations,
            stock = listOf(Card(11, Suit.HEARTS, false)),
            waste = listOf(Card(11, Suit.CLUBS, true)),
            moveCount = 10,
        )

        assertEquals(52, KlondikeRules.totalCardCount(mistakeState))
        assertFalse(engine.isLost(mistakeState.puzzle, mistakeState))
        assertTrue(KlondikeSolver.isWinnable(mistakeState))

        // Fatal mistake: place 11C from waste onto 12D in col 1.
        val afterMistake = engine.applyAction(mistakeState, CardMove.WasteToTableau(1))
        assertFalse(KlondikeRules.isDeadlocked(afterMistake), "7H can still bounce between 8S and 8C, so not deadlocked")
        assertTrue(engine.isLost(mistakeState.puzzle, afterMistake), "No winning line remains, so must be lost")
        assertNull(engine.hintAction(mistakeState.puzzle, afterMistake))

        // Continue restores mistakeState with extraUndos == 1 grace.
        val continued = engine.applyContinue(afterMistake)
        assertFalse(engine.isLost(mistakeState.puzzle, continued))
        assertEquals(1, continued.extraUndos)
        assertEquals(mistakeState.tableau, continued.tableau)

        // Follow hints from continued all the way to win!
        var play = continued
        var steps = 0
        while (!engine.isWon(mistakeState.puzzle, play) && steps < 60) {
            val hint = engine.hintAction(mistakeState.puzzle, play)
            assertNotNull(hint, "Hint should recover winning line after Continue at step $steps")
            play = engine.applyAction(play, hint!!)
            if (steps == 0) {
                assertEquals(0, play.extraUndos, "One-action grace is consumed on the next move")
            }
            assertFalse(engine.isLost(mistakeState.puzzle, play))
            steps++
        }
        assertTrue(engine.isWon(mistakeState.puzzle, play))
    }

    @Test
    fun solverUsesFoundationToTableauRescueWhenRequired() {
        // Foundations: C=1..13, H=1..4, S=1..4, D=1..1 (22 cards).
        // Col 0: [2D(down), 3D(up)] -> 3D blocks 2D and needs 4S from Spades foundation onto 5H!
        // Col 1: [4D(down), 5H(up)]
        // Stack the rest cleanly in Col 2..6 so the chain from 13D down to 6S is already in place
        // (leaving no redundant stock branching):
        // Col 2: [13D, 12S, 11H, 10S, 9H, 8S, 7H, 6S] (8)
        // Col 3: [13H, 12C? no C is on F! Put 13H, 12S? no! Stack alternating:
        //   Col 2: [13D, 12S, 11D, 10S, 9D, 8S, 7D, 6S] (8) -> D/S alternating!
        //   Col 3: [13H] (1)
        //   Col 4: [13S, 12H, 11S, 10H, 9S, 8H, 7S, 6H, 5S] (9) -> S/H alternating!
        //   Col 5: [12D, 11H, 10D, 9H, 8D, 7H, 6D, 5D? no 6D is Red! Put 5D in Col 6!] (7)
        //   Col 6: [5D] (1) -> wait: 5D can go onto 6S (Col 2) immediately, which is fine,
        //   and 5H (Col 1) CANNOT go to foundation (4H on F, wait: 5H CAN go to foundation! Put 5H NOT playable to F? No, H=1..4 is on F! Wait: if 5H goes to F, then 4D flips without pulling 4S?! No, if 5H goes to F, 4D STILL needs 3D on F, and 3D is STILL on top of 2D! Wait: if 5H goes to F, Col 1 has 4D(up), NOT 5H, so 4S HAS NOWHERE TO GO! Make H=1..3 on F and put 4H face-down under 5H or in Col 6!)
        //   YES! Foundations H=1..3 (3 cards), S=1..4 (4 cards), C=1..13 (13 cards), D=1..1 (1 card) = 21 cards!
        //   Remaining 31 cards: 2D..13D (12), 4H..13H (10), 5S..13S (9).
        //   Put 4H(up)? No, if 4H is up it goes to F and makes 5H playable to F! Put 4H face-down under 5H in Col 1 (`[4H(down), 4D(down), 5H(up)]`)!
        //   And put 5D face-down under 4H? No, put 5D(up) on Col 6 (`[5D(up)]`)!
        //   Check:
        //   Col 0: [2D(down), 3D(up)] (2)
        //   Col 1: [4H(down), 4D(down), 5H(up)] (3)
        //   Col 2: [13D, 12S, 11D, 10S, 9D, 8S, 7D, 6S] (8, all faceUp)
        //   Col 3: [13H(up)] (1)
        //   Col 4: [13S, 12H, 11S, 10H, 9S, 8H, 7S, 6H, 5S] (9, all faceUp)
        //   Col 5: [12D, 11H, 10D, 9H, 8D, 7H, 6D] (7, all faceUp)
        //   Col 6: [5D(up)] (1) -> wait: 5D can go onto 6S, then Col 6 is EMPTY, then 13H moves? No, 13H moving leaves Col 3 EMPTY, doesn't help 3D! Still `3D` needs `4S` onto `5H` (or `5D`!).
        //   Total cards: 21 + 2 + 3 + 8 + 1 + 9 + 7 + 1 = 52!
        //   Trace: `4S` (from Spades foundation!) moves onto `5H` (or `5D`) -> `3D` moves onto `4S` -> flips `2D` -> `2D` to F -> `3D` to F -> `4S` back to F -> `4D` flips -> everything wins!
        //   AND check `isSafeToFoundation` for `4S` (S=4, H=3, D=1, C=13):
        //   `minOpp = min(H=3, D=1) = 1 < 3`, so `4S` is NOT safe and CAN be pulled by Tier 3!
        val rescueFoundations = mapOf(
            Suit.CLUBS to (1..13).map { Card(it, Suit.CLUBS, true) },
            Suit.HEARTS to (1..3).map { Card(it, Suit.HEARTS, true) },
            Suit.SPADES to (1..4).map { Card(it, Suit.SPADES, true) },
            Suit.DIAMONDS to listOf(Card(1, Suit.DIAMONDS, true)),
        )
        val rescueTableau = listOf(
            listOf(Card(2, Suit.DIAMONDS, false), Card(3, Suit.DIAMONDS, true)),
            listOf(Card(4, Suit.HEARTS, false), Card(4, Suit.DIAMONDS, false), Card(5, Suit.HEARTS, true)),
            listOf(
                Card(13, Suit.DIAMONDS, true),
                Card(12, Suit.SPADES, true),
                Card(11, Suit.DIAMONDS, true),
                Card(10, Suit.SPADES, true),
                Card(9, Suit.DIAMONDS, true),
                Card(8, Suit.SPADES, true),
                Card(7, Suit.DIAMONDS, true),
                Card(6, Suit.SPADES, true),
            ),
            listOf(Card(13, Suit.HEARTS, true)),
            listOf(
                Card(13, Suit.SPADES, true),
                Card(12, Suit.HEARTS, true),
                Card(11, Suit.SPADES, true),
                Card(10, Suit.HEARTS, true),
                Card(9, Suit.SPADES, true),
                Card(8, Suit.HEARTS, true),
                Card(7, Suit.SPADES, true),
                Card(6, Suit.HEARTS, true),
                Card(5, Suit.SPADES, true),
            ),
            listOf(
                Card(12, Suit.DIAMONDS, true),
                Card(11, Suit.HEARTS, true),
                Card(10, Suit.DIAMONDS, true),
                Card(9, Suit.HEARTS, true),
                Card(8, Suit.DIAMONDS, true),
                Card(7, Suit.HEARTS, true),
                Card(6, Suit.DIAMONDS, true),
            ),
            listOf(Card(5, Suit.DIAMONDS, true)),
        )
        assertEquals(52, rescueFoundations.values.sumOf { it.size } + rescueTableau.sumOf { it.size })

        val puzzle = KlondikePuzzle("kl-rescue-52", Pack.HARD, "d3", seed = 99L)
        val state = KlondikeState(
            puzzle = puzzle,
            tableau = rescueTableau,
            foundations = rescueFoundations,
            stock = emptyList(),
            waste = emptyList(),
            moveCount = 12,
        )

        assertFalse(engine.isLost(puzzle, state), "State is winnable via FoundationToTableau rescue")
        val solution = KlondikeSolver.solve(state)
        assertNotNull(solution, "Solver must find rescue line via FoundationToTableau")
        assertTrue(
            solution!!.any { it is CardMove.FoundationToTableau },
            "Solution must use FoundationToTableau: $solution",
        )
        val endState = solution.fold(state) { current, move -> engine.applyAction(current, move) }
        assertTrue(engine.isWon(puzzle, endState))
    }
}
