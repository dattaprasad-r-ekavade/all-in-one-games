package com.datathecodie.witgrid.engine.klondike

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.engine.core.SplitMix64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KlondikeEngineTest {
    private val engine = KlondikeEngine

    private val testPuzzle = KlondikePuzzle(
        bankId = "kl-fixture-1",
        pack = Pack.MEDIUM,
        difficulty = "d1",
        seed = 42L,
    )

    @Test
    fun cardModelAndRanks() {
        val aceOfSpades = Card(1, Suit.SPADES, faceUp = true)
        assertEquals(Rank.ACE, aceOfSpades.rankEnum)
        assertTrue(aceOfSpades.isBlack)
        assertFalse(aceOfSpades.isRed)
        assertTrue(aceOfSpades.faceUp)

        val kingOfHearts = Card(Rank.KING, Suit.HEARTS)
        assertEquals(13, kingOfHearts.rank)
        assertTrue(kingOfHearts.isRed)
        assertFalse(kingOfHearts.faceUp)

        val flipped = kingOfHearts.withFaceUp(true)
        assertTrue(flipped.faceUp)
        assertEquals(kingOfHearts.withFaceUp(true), flipped)

        assertThrows(IllegalArgumentException::class.java) { Card(0, Suit.CLUBS) }
        assertThrows(IllegalArgumentException::class.java) { Card(14, Suit.DIAMONDS) }
    }

    @Test
    fun dealingIsDeterministicFromSeed() {
        val state1 = engine.initialState(testPuzzle)
        val state2 = engine.initialState(testPuzzle)
        assertEquals(state1, state2)

        val otherSeedPuzzle = testPuzzle.copy(seed = 999L)
        val stateOther = engine.initialState(otherSeedPuzzle)
        assertNotEquals(state1.stock, stateOther.stock)
    }

    @Test
    fun dealPreservesAll52CardsWithoutDuplicates() {
        val state = engine.initialState(testPuzzle)
        val allCards = ArrayList<Card>()
        state.tableau.forEach { allCards.addAll(it) }
        allCards.addAll(state.stock)
        allCards.addAll(state.waste)
        state.foundations.values.forEach { allCards.addAll(it) }

        assertEquals(52, allCards.size)
        val distinctKeys = allCards.map { it.rank to it.suit }.toSet()
        assertEquals(52, distinctKeys.size)
    }

    @Test
    fun dealTableauShapeAndFaceUpCounts() {
        val mediumState = engine.initialState(testPuzzle.copy(pack = Pack.MEDIUM))
        for (col in 0 until 7) {
            val pile = mediumState.tableau[col]
            assertEquals(col + 1, pile.size)
            // In medium, exactly 1 card is face up (the top card)
            assertEquals(1, pile.count { it.faceUp })
            assertTrue(pile.last().faceUp)
        }
        assertEquals(24, mediumState.stock.size)
        assertTrue(mediumState.waste.isEmpty())

        val easyState = engine.initialState(testPuzzle.copy(pack = Pack.EASY))
        assertEquals(1, easyState.tableau[0].count { it.faceUp })
        for (col in 1 until 7) {
            val pile = easyState.tableau[col]
            assertEquals(col + 1, pile.size)
            assertEquals(2, pile.count { it.faceUp })
            assertTrue(pile[pile.lastIndex].faceUp)
            assertTrue(pile[pile.lastIndex - 1].faceUp)
        }
    }

    @Test
    fun initialDealIsNotWonAndNotLost() {
        val state = engine.initialState(testPuzzle)
        assertFalse(engine.isWon(testPuzzle, state))
        assertFalse(engine.isLost(testPuzzle, state))
    }

    @Test
    fun drawStockAndRecycleWaste() {
        val initial = engine.initialState(testPuzzle)
        assertEquals(24, initial.stock.size)
        assertTrue(initial.waste.isEmpty())

        // Draw 1
        val afterDraw1 = engine.applyAction(initial, CardMove.DrawStock)
        assertEquals(23, afterDraw1.stock.size)
        assertEquals(1, afterDraw1.waste.size)
        assertTrue(afterDraw1.waste.last().faceUp)

        // Draw all remaining 23 cards
        var current = afterDraw1
        repeat(23) {
            current = engine.applyAction(current, CardMove.DrawStock)
        }
        assertEquals(0, current.stock.size)
        assertEquals(24, current.waste.size)

        // Tapping DrawStock when stock is empty recycles waste
        val recycled = engine.applyAction(current, CardMove.DrawStock)
        assertEquals(24, recycled.stock.size)
        assertEquals(0, recycled.waste.size)
        assertTrue(recycled.stock.all { !it.faceUp })
        // The first card drawn after recycle matches the first card drawn initially
        val afterRecycleDraw1 = engine.applyAction(recycled, CardMove.DrawStock)
        assertEquals(afterDraw1.waste.last(), afterRecycleDraw1.waste.last())

        // Direct RecycleWaste
        val current2 = afterDraw1.copy(stock = emptyList(), waste = listOf(Card(1, Suit.HEARTS, true)))
        val recycledDirect = engine.applyAction(current2, CardMove.RecycleWaste)
        assertEquals(1, recycledDirect.stock.size)
        assertTrue(recycledDirect.waste.isEmpty())

        // Cannot recycle when stock is non-empty
        assertThrows(IllegalArgumentException::class.java) {
            engine.applyAction(afterDraw1, CardMove.RecycleWaste)
        }
        // Cannot draw when both stock and waste are empty
        val emptyState = afterDraw1.copy(stock = emptyList(), waste = emptyList())
        assertThrows(IllegalArgumentException::class.java) {
            engine.applyAction(emptyState, CardMove.DrawStock)
        }
    }

    @Test
    fun tableauToFoundationLegalAndAutoTurnUp() {
        val customTableau = List(7) { col ->
            if (col == 0) {
                listOf(Card(5, Suit.SPADES, false), Card(1, Suit.HEARTS, true))
            } else {
                emptyList()
            }
        }
        val customState = KlondikeState(
            puzzle = testPuzzle,
            tableau = customTableau,
            foundations = Suit.entries.associateWith { emptyList() },
            stock = emptyList(),
            waste = emptyList(),
            moveCount = 1,
        )

        // Move Ace of Hearts from column 0 to foundation
        val afterAce = engine.applyAction(customState, CardMove.TableauToFoundation(0))
        assertEquals(1, afterAce.foundations[Suit.HEARTS]?.size)
        assertEquals(Card(1, Suit.HEARTS, true), afterAce.foundations[Suit.HEARTS]?.last())
        // Auto turn-up flipped the remaining card in column 0
        assertEquals(1, afterAce.tableau[0].size)
        assertTrue(afterAce.tableau[0].last().faceUp)
        assertEquals(5, afterAce.tableau[0].last().rank)

        // Illegal foundation moves
        assertThrows(IllegalArgumentException::class.java) {
            // Cannot place 5 on Ace
            engine.applyAction(afterAce, CardMove.TableauToFoundation(0))
        }
        assertThrows(IllegalArgumentException::class.java) {
            // Empty column
            engine.applyAction(afterAce, CardMove.TableauToFoundation(1))
        }
    }

    @Test
    fun wasteToFoundationAndTableau() {
        val stateWithWaste = KlondikeState(
            puzzle = testPuzzle,
            tableau = List(7) { emptyList() },
            foundations = Suit.entries.associateWith { emptyList() },
            stock = emptyList(),
            waste = listOf(Card(1, Suit.SPADES, true), Card(13, Suit.DIAMONDS, true)),
            moveCount = 1,
        )

        // Move King of Diamonds from waste to empty column 3
        val afterKing = engine.applyAction(stateWithWaste, CardMove.WasteToTableau(3))
        assertEquals(1, afterKing.tableau[3].size)
        assertEquals(Card(13, Suit.DIAMONDS, true), afterKing.tableau[3].last())
        assertEquals(1, afterKing.waste.size)

        // Move Ace of Spades from waste to foundation
        val afterAce = engine.applyAction(afterKing, CardMove.WasteToFoundation)
        assertEquals(1, afterAce.foundations[Suit.SPADES]?.size)
        assertTrue(afterAce.waste.isEmpty())

        // Non-King to empty column throws
        val stateNonKing = afterAce.copy(waste = listOf(Card(10, Suit.CLUBS, true)))
        assertThrows(IllegalArgumentException::class.java) {
            engine.applyAction(stateNonKing, CardMove.WasteToTableau(0))
        }
    }

    @Test
    fun foundationToTableau() {
        val state = KlondikeState(
            puzzle = testPuzzle,
            tableau = List(7) { col ->
                if (col == 0) listOf(Card(10, Suit.SPADES, true)) else emptyList()
            },
            foundations = mapOf(
                Suit.HEARTS to listOf(Card(1, Suit.HEARTS, true), Card(9, Suit.HEARTS, true)),
                Suit.CLUBS to emptyList(),
                Suit.DIAMONDS to emptyList(),
                Suit.SPADES to emptyList(),
            ),
            stock = emptyList(),
            waste = emptyList(),
            moveCount = 1,
        )

        // 9 of Hearts from foundation onto 10 of Spades in column 0
        val afterMove = engine.applyAction(state, CardMove.FoundationToTableau(Suit.HEARTS, 0))
        assertEquals(1, afterMove.foundations[Suit.HEARTS]?.size)
        assertEquals(2, afterMove.tableau[0].size)
        assertEquals(Card(9, Suit.HEARTS, true), afterMove.tableau[0].last())

        // Illegal: cannot place 1 of Hearts onto 9 of Hearts
        assertThrows(IllegalArgumentException::class.java) {
            engine.applyAction(afterMove, CardMove.FoundationToTableau(Suit.HEARTS, 0))
        }
    }

    @Test
    fun tableauToTableauSingleCardAndRun() {
        val state = KlondikeState(
            puzzle = testPuzzle,
            tableau = listOf(
                listOf(Card(8, Suit.CLUBS, false), Card(12, Suit.SPADES, true), Card(11, Suit.HEARTS, true)),
                listOf(Card(13, Suit.DIAMONDS, true)),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
            ),
            foundations = Suit.entries.associateWith { emptyList() },
            stock = emptyList(),
            waste = emptyList(),
            moveCount = 1,
        )

        // Move run starting with Queen of Spades (index 1 in col 0) onto King of Diamonds (col 1)
        val afterRun = engine.applyAction(state, CardMove.TableauToTableau(0, 1, 1))
        assertEquals(1, afterRun.tableau[0].size)
        assertTrue(afterRun.tableau[0].last().faceUp) // auto turned up
        assertEquals(3, afterRun.tableau[1].size)
        assertEquals(Card(11, Suit.HEARTS, true), afterRun.tableau[1].last())

        // Moving to same column throws
        assertThrows(IllegalArgumentException::class.java) {
            engine.applyAction(afterRun, CardMove.TableauToTableau(1, 2, 1))
        }

        // King to empty column 2
        val stateKing = KlondikeState(
            puzzle = testPuzzle,
            tableau = listOf(
                listOf(Card(13, Suit.CLUBS, true)),
                emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(),
            ),
            foundations = Suit.entries.associateWith { emptyList() },
            stock = emptyList(),
            waste = emptyList(),
            moveCount = 1,
        )
        val afterKing = engine.applyAction(stateKing, CardMove.TableauToTableau(0, 0, 2))
        assertTrue(afterKing.tableau[0].isEmpty())
        assertEquals(1, afterKing.tableau[2].size)
        assertEquals(13, afterKing.tableau[2].last().rank)
    }

    @Test
    fun isWonWhenAll52CardsOnFoundations() {
        val fullFoundations = Suit.entries.associateWith { suit ->
            (1..13).map { rank -> Card(rank, suit, true) }
        }
        val wonState = KlondikeState(
            puzzle = testPuzzle,
            tableau = List(7) { emptyList() },
            foundations = fullFoundations,
            stock = emptyList(),
            waste = emptyList(),
            moveCount = 100,
        )
        assertTrue(engine.isWon(testPuzzle, wonState))
        assertFalse(engine.isLost(testPuzzle, wonState))
    }

    @Test
    fun isLostWhenDeadlockedAfterPlay() {
        // No legal tableau/foundation moves and cards in stock/waste cannot be placed anywhere
        val stuckState = KlondikeState(
            puzzle = testPuzzle,
            tableau = listOf(
                listOf(Card(2, Suit.SPADES, true)),
                listOf(Card(2, Suit.CLUBS, true)),
                listOf(Card(2, Suit.HEARTS, true)),
                listOf(Card(2, Suit.DIAMONDS, true)),
                listOf(Card(4, Suit.SPADES, true)),
                listOf(Card(4, Suit.CLUBS, true)),
                listOf(Card(4, Suit.HEARTS, true)),
            ),
            foundations = Suit.entries.associateWith { emptyList() },
            stock = listOf(Card(10, Suit.SPADES, false)), // Cannot go to foundation (needs Ace) or tableau (needs 3 or King)
            waste = listOf(Card(10, Suit.HEARTS, true)),
            moveCount = 1,
        )
        assertTrue(engine.isLost(testPuzzle, stuckState))
        assertFalse(engine.isWon(testPuzzle, stuckState))
    }

    @Test
    fun isNotLostIfStockCardCanBePlayed() {
        val playableStockState = KlondikeState(
            puzzle = testPuzzle,
            tableau = listOf(
                listOf(Card(2, Suit.SPADES, true)),
                emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(),
            ),
            foundations = Suit.entries.associateWith { emptyList() },
            stock = listOf(Card(13, Suit.HEARTS, false)), // King can be played to empty column!
            waste = emptyList(),
            moveCount = 1,
        )
        assertFalse(engine.isLost(testPuzzle, playableStockState))
    }

    @Test
    fun continueRestoresPreviousStateAndClearsLoss() {
        val preMove = KlondikeState(
            puzzle = testPuzzle,
            tableau = List(7) { emptyList() },
            foundations = Suit.entries.associateWith { emptyList() },
            stock = listOf(Card(10, Suit.SPADES, false)),
            waste = emptyList(),
            moveCount = 5,
        )
        val lostState = KlondikeState(
            puzzle = testPuzzle,
            tableau = listOf(
                listOf(Card(2, Suit.SPADES, true)),
                listOf(Card(2, Suit.CLUBS, true)),
                listOf(Card(2, Suit.HEARTS, true)),
                listOf(Card(2, Suit.DIAMONDS, true)),
                listOf(Card(4, Suit.SPADES, true)),
                listOf(Card(4, Suit.CLUBS, true)),
                listOf(Card(4, Suit.HEARTS, true)),
            ),
            foundations = Suit.entries.associateWith { emptyList() },
            stock = emptyList(),
            waste = listOf(Card(10, Suit.SPADES, true)),
            moveCount = 6,
            previousState = preMove,
        )
        assertTrue(engine.isLost(testPuzzle, lostState))

        val continued = engine.applyContinue(lostState)
        assertFalse(engine.isLost(testPuzzle, continued))
        assertEquals(preMove.stock, continued.stock)
        assertEquals(preMove.tableau, continued.tableau)
        assertEquals(1, continued.extraUndos)

        // Next move consumes extraUndos
        val afterMove = engine.applyAction(continued, CardMove.DrawStock)
        assertEquals(0, afterMove.extraUndos)

        // Continue throws if not lost
        assertThrows(IllegalArgumentException::class.java) {
            engine.applyContinue(continued)
        }
    }

    @Test
    fun continueAfterSaveRestoresPositionBeforeLastAction() {
        val beforeLastAction = KlondikeState(
            puzzle = testPuzzle,
            tableau = listOf(
                listOf(Card(2, Suit.SPADES, true)),
                listOf(Card(2, Suit.CLUBS, true)),
                listOf(Card(2, Suit.HEARTS, true)),
                listOf(Card(2, Suit.DIAMONDS, true)),
                listOf(Card(4, Suit.SPADES, true)),
                listOf(Card(4, Suit.CLUBS, true)),
                listOf(Card(4, Suit.HEARTS, true)),
            ),
            foundations = Suit.entries.associateWith { emptyList() },
            stock = listOf(Card(10, Suit.SPADES, false)),
            waste = emptyList(),
        )
        val deadlocked = engine.applyAction(beforeLastAction, CardMove.DrawStock)
        assertTrue(engine.isLost(testPuzzle, deadlocked))

        val restored = engine.decodeState(engine.encodeState(deadlocked))
        assertTrue(engine.isLost(testPuzzle, restored))
        val continued = engine.applyContinue(restored)

        assertEquals(beforeLastAction.tableau, continued.tableau)
        assertEquals(beforeLastAction.stock, continued.stock)
        assertEquals(beforeLastAction.waste, continued.waste)
        assertEquals(1, continued.extraUndos)
    }

    @Test
    fun bankCodecRoundTrips() {
        val encoded = engine.encodePuzzle(testPuzzle)
        val decoded = engine.decodePuzzle(encoded)
        assertEquals(testPuzzle, decoded)

        val customLine = "kl-test|52|seed=12345|EASY|d1"
        val customPuzzle = engine.decodePuzzle(customLine)
        assertEquals(12345L, customPuzzle.seed)
        assertEquals(Pack.EASY, customPuzzle.pack)

        assertThrows(IllegalArgumentException::class.java) {
            engine.decodePuzzle("invalid|format")
        }
        assertThrows(IllegalArgumentException::class.java) {
            engine.decodePuzzle("kl-bad|52|not-a-number|EASY|d1")
        }
    }

    @Test
    fun stateCodecRoundTrips() {
        val start = engine.initialState(testPuzzle)
        val afterDraw = engine.applyAction(start, CardMove.DrawStock)
        val bytes = engine.encodeState(afterDraw)
        val restored = engine.decodeState(bytes)
        assertEquals(afterDraw, restored)

        assertThrows(IllegalArgumentException::class.java) {
            engine.decodeState(byteArrayOf(1, 2, 3))
        }
    }

    @Test
    fun hintAndGenerateWorkForWinnableDeals() {
        val puzzle = engine.generate(SplitMix64(1), Pack.EASY)
        val start = engine.initialState(puzzle)
        val hint = engine.hintAction(puzzle, start)
        assertTrue(hint != null)
    }
}
