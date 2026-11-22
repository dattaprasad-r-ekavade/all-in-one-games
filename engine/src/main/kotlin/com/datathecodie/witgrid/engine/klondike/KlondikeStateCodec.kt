package com.datathecodie.witgrid.engine.klondike

import com.datathecodie.witgrid.engine.core.VersionedStateCodec
import java.io.DataInputStream
import java.io.DataOutputStream

object KlondikeStateCodec {
    private const val MAGIC = 0x574B4C4F // "WKLO"

    fun encode(state: KlondikeState): ByteArray =
        VersionedStateCodec.encode(MAGIC) { output ->
            output.writeUTF(KlondikeCodec.encodePuzzle(state.puzzle))

            // Tableau
            output.writeInt(state.tableau.size)
            for (pile in state.tableau) {
                writeCardList(output, pile)
            }

            // Foundations
            output.writeInt(Suit.entries.size)
            for (suit in Suit.entries) {
                output.writeByte(suit.ordinal)
                writeCardList(output, state.foundations[suit] ?: emptyList())
            }

            // Stock and waste
            writeCardList(output, state.stock)
            writeCardList(output, state.waste)

            output.writeInt(state.moveCount)
            output.writeInt(state.extraUndos)
        }

    fun decode(bytes: ByteArray): KlondikeState =
        VersionedStateCodec.decode(bytes, MAGIC) { input ->
            val puzzle = KlondikeCodec.decodeLine(input.readUTF())

            val tableauSize = input.readInt()
            require(tableauSize == KlondikeRules.TABLEAU_COLUMNS) {
                "Expected ${KlondikeRules.TABLEAU_COLUMNS} tableau columns, got $tableauSize"
            }
            val tableau = ArrayList<List<Card>>(tableauSize)
            repeat(tableauSize) {
                tableau.add(readCardList(input))
            }

            val foundationsCount = input.readInt()
            val foundations = mutableMapOf<Suit, List<Card>>()
            repeat(foundationsCount) {
                val suitOrdinal = input.readByte().toInt()
                require(suitOrdinal in Suit.entries.indices) { "Invalid suit ordinal: $suitOrdinal" }
                val suit = Suit.entries[suitOrdinal]
                foundations[suit] = readCardList(input)
            }

            val stock = readCardList(input)
            val waste = readCardList(input)
            val moveCount = input.readInt()
            require(moveCount >= 0) { "Move count must not be negative: $moveCount" }
            val extraUndos = input.readInt()
            require(extraUndos >= 0) { "Extra undos must not be negative: $extraUndos" }

            KlondikeState(
                puzzle = puzzle,
                tableau = tableau,
                foundations = foundations,
                stock = stock,
                waste = waste,
                moveCount = moveCount,
                extraUndos = extraUndos,
            )
        }

    private fun writeCardList(output: DataOutputStream, cards: List<Card>) {
        output.writeInt(cards.size)
        for (card in cards) {
            output.writeByte(card.rank)
            output.writeByte(card.suit.ordinal)
            output.writeBoolean(card.faceUp)
        }
    }

    private fun readCardList(input: DataInputStream): List<Card> {
        val size = input.readInt()
        require(size in 0..KlondikeRules.TOTAL_CARDS) { "Card count out of range: $size" }
        val cards = ArrayList<Card>(size)
        repeat(size) {
            val rank = input.readByte().toInt()
            val suitOrdinal = input.readByte().toInt()
            require(suitOrdinal in Suit.entries.indices) { "Invalid suit ordinal: $suitOrdinal" }
            val faceUp = input.readBoolean()
            cards.add(Card(rank, Suit.entries[suitOrdinal], faceUp))
        }
        return cards
    }
}
