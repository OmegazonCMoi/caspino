package com.example.mobile

import com.example.mobile.network.BlackjackCard
import com.example.mobile.network.BlackjackHand
import com.example.mobile.ui.screens.canSplitHand
import com.example.mobile.ui.screens.cardNumericValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlackjackLogicTest {

    // ── cardNumericValue ──

    @Test
    fun cardNumericValue_ace_returns11() {
        val card = BlackjackCard(suit = "spades", rank = "A")
        assertEquals(11, cardNumericValue(card))
    }

    @Test
    fun cardNumericValue_king_returns10() {
        val card = BlackjackCard(suit = "hearts", rank = "K")
        assertEquals(10, cardNumericValue(card))
    }

    @Test
    fun cardNumericValue_queen_returns10() {
        val card = BlackjackCard(suit = "diamonds", rank = "Q")
        assertEquals(10, cardNumericValue(card))
    }

    @Test
    fun cardNumericValue_jack_returns10() {
        val card = BlackjackCard(suit = "clubs", rank = "J")
        assertEquals(10, cardNumericValue(card))
    }

    @Test
    fun cardNumericValue_numberCards_returnTheirValue() {
        for (rank in 2..10) {
            val card = BlackjackCard(suit = "spades", rank = rank.toString())
            assertEquals("Rank $rank", rank, cardNumericValue(card))
        }
    }

    @Test
    fun cardNumericValue_invalidRank_returns0() {
        val card = BlackjackCard(suit = "spades", rank = "X")
        assertEquals(0, cardNumericValue(card))
    }

    // ── canSplitHand ──

    @Test
    fun canSplitHand_pairOfSameRank_returnsTrue() {
        val hand = BlackjackHand(
            cards = listOf(
                BlackjackCard("hearts", "8"),
                BlackjackCard("spades", "8")
            ),
            bet = 10,
            status = "PLAYING",
            isDoubled = false,
            value = 16
        )
        assertTrue(canSplitHand(hand))
    }

    @Test
    fun canSplitHand_pairOfFaceCards_returnsTrue() {
        val hand = BlackjackHand(
            cards = listOf(
                BlackjackCard("hearts", "K"),
                BlackjackCard("spades", "Q")
            ),
            bet = 10,
            status = "PLAYING",
            isDoubled = false,
            value = 20
        )
        assertTrue(canSplitHand(hand))
    }

    @Test
    fun canSplitHand_pairOfAces_returnsTrue() {
        val hand = BlackjackHand(
            cards = listOf(
                BlackjackCard("hearts", "A"),
                BlackjackCard("spades", "A")
            ),
            bet = 10,
            status = "PLAYING",
            isDoubled = false,
            value = 12
        )
        assertTrue(canSplitHand(hand))
    }

    @Test
    fun canSplitHand_tenAndKing_returnsTrue() {
        val hand = BlackjackHand(
            cards = listOf(
                BlackjackCard("hearts", "10"),
                BlackjackCard("spades", "K")
            ),
            bet = 10,
            status = "PLAYING",
            isDoubled = false,
            value = 20
        )
        assertTrue(canSplitHand(hand))
    }

    @Test
    fun canSplitHand_differentValues_returnsFalse() {
        val hand = BlackjackHand(
            cards = listOf(
                BlackjackCard("hearts", "8"),
                BlackjackCard("spades", "9")
            ),
            bet = 10,
            status = "PLAYING",
            isDoubled = false,
            value = 17
        )
        assertFalse(canSplitHand(hand))
    }

    @Test
    fun canSplitHand_threeCards_returnsFalse() {
        val hand = BlackjackHand(
            cards = listOf(
                BlackjackCard("hearts", "8"),
                BlackjackCard("spades", "8"),
                BlackjackCard("diamonds", "5")
            ),
            bet = 10,
            status = "PLAYING",
            isDoubled = false,
            value = 21
        )
        assertFalse(canSplitHand(hand))
    }

    @Test
    fun canSplitHand_oneCard_returnsFalse() {
        val hand = BlackjackHand(
            cards = listOf(
                BlackjackCard("hearts", "8")
            ),
            bet = 10,
            status = "PLAYING",
            isDoubled = false,
            value = 8
        )
        assertFalse(canSplitHand(hand))
    }

    @Test
    fun canSplitHand_emptyHand_returnsFalse() {
        val hand = BlackjackHand(
            cards = emptyList(),
            bet = 10,
            status = "PLAYING",
            isDoubled = false,
            value = 0
        )
        assertFalse(canSplitHand(hand))
    }

    @Test
    fun canSplitHand_aceAndTen_returnsFalse() {
        val hand = BlackjackHand(
            cards = listOf(
                BlackjackCard("hearts", "A"),
                BlackjackCard("spades", "10")
            ),
            bet = 10,
            status = "PLAYING",
            isDoubled = false,
            value = 21
        )
        assertFalse(canSplitHand(hand))
    }

    @Test
    fun canSplitHand_jackAndQueen_returnsTrue() {
        val hand = BlackjackHand(
            cards = listOf(
                BlackjackCard("hearts", "J"),
                BlackjackCard("spades", "Q")
            ),
            bet = 10,
            status = "PLAYING",
            isDoubled = false,
            value = 20
        )
        assertTrue(canSplitHand(hand))
    }
}
