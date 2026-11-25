package com.datathecodie.witgrid.engine.tactics.spec

import com.datathecodie.witgrid.engine.tactics.TacticsCodec
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TacticsCodecSpecTest {

    @Test
    fun bankLineRoundTripKeepsEveryField() {
        val puzzle = TacticsSpecFixtures.mateInTwoForcedDefence()

        val encoded = TacticsCodec.encode(puzzle)
        val decoded = TacticsCodec.decode(encoded)

        assertEquals(puzzle, decoded)
        assertEquals(puzzle.id, decoded.id)
        assertEquals(puzzle.mateIn, decoded.mateIn)
        assertEquals(puzzle.solutionLine, decoded.solutionLine)
        assertEquals(puzzle.pack, decoded.pack)
        assertEquals(puzzle.storedGrade, decoded.storedGrade)
    }

    @Test
    fun encodedPayloadEndsWithTheMateHorizon() {
        val puzzle = TacticsSpecFixtures.mateInTwoForcedDefence()

        val payload = TacticsCodec.encode(puzzle).split("|")[2]
        assertTrue(payload.endsWith(" 2"), "Payload must end with the mate horizon: $payload")
    }

    @Test
    fun promotionMoveRoundTripsWithTheQueenSuffix() {
        val puzzle = TacticsSpecFixtures.promotionMate()

        val encoded = TacticsCodec.encode(puzzle)
        assertTrue(encoded.contains("|a7a8q|"), "Promotion must carry the q suffix: $encoded")
        assertEquals(puzzle, TacticsCodec.decode(encoded))
    }

    @Test
    fun malformedBankLinesAreRejected() {
        val fen = TacticsSpecFixtures.MATE_IN_ONE_FEN
        // Board size is not 8.
        assertThrows(IllegalArgumentException::class.java) {
            TacticsCodec.decode("bad-size|6|$fen 1|d7g7|EASY|checkmate:1")
        }
        // Payload has too few tokens for a FEN and a mate horizon.
        assertThrows(IllegalArgumentException::class.java) {
            TacticsCodec.decode("short-payload|8|7k/3Q4/5K2/8/8/8/8/8 w -|d7g7|EASY|checkmate:1")
        }
        // The first FEN field has 1 rank, not 8.
        assertThrows(IllegalArgumentException::class.java) {
            TacticsCodec.decode("bad-fen|8|not-a-fen w - - 1|d7g7|EASY|checkmate:1")
        }
        // z9 is not a square.
        assertThrows(IllegalArgumentException::class.java) {
            TacticsCodec.decode("bad-uci|8|$fen 1|z9z9|EASY|checkmate:1")
        }
        // Mate horizon 1 needs one move, not three.
        assertThrows(IllegalArgumentException::class.java) {
            TacticsCodec.decode("bad-length|8|$fen 1|d7g7 f6f7 g7g8|EASY|checkmate:1")
        }
    }

    @Test
    fun illegalMoveInTheLineIsRejected() {
        // The White king on f6 cannot move two squares to f8.
        val fen = TacticsSpecFixtures.MATE_IN_ONE_FEN
        assertThrows(IllegalArgumentException::class.java) {
            TacticsCodec.decode("illegal-move|8|$fen 1|f6f8|EASY|checkmate:1")
        }
    }

    @Test
    fun missingPromotionSuffixIsRejected() {
        val encoded = TacticsCodec.encode(TacticsSpecFixtures.promotionMate())
        val withoutSuffix = encoded.replace("|a7a8q|", "|a7a8|")

        assertThrows(IllegalArgumentException::class.java) { TacticsCodec.decode(withoutSuffix) }
    }

    @Test
    fun lineThatDoesNotMateIsRejected() {
        // Qd7-d8 is a legal queen move, but Black can answer and there is no mate.
        val fen = TacticsSpecFixtures.MATE_IN_ONE_FEN
        assertThrows(IllegalArgumentException::class.java) {
            TacticsCodec.decode("no-mate|8|$fen 1|d7d8|EASY|checkmate:1")
        }
    }
}
