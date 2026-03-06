package com.example.mobile

import com.example.mobile.network.AuthResponse
import com.example.mobile.network.BlackjackBetResult
import com.example.mobile.network.BlackjackCard
import com.example.mobile.network.BlackjackGameState
import com.example.mobile.network.BlackjackHand
import com.example.mobile.network.BlackjackHandResult
import com.example.mobile.network.BlackjackPlayerHand
import com.example.mobile.network.MeResponse
import com.example.mobile.network.SlotSpinResponse
import com.example.mobile.ui.screens.GameItem
import com.example.mobile.ui.screens.Match
import com.example.mobile.ui.screens.PinosOffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DataModelsTest {

    // ── BlackjackCard ──

    @Test
    fun blackjackCard_equality() {
        val card1 = BlackjackCard("hearts", "A")
        val card2 = BlackjackCard("hearts", "A")
        assertEquals(card1, card2)
    }

    @Test
    fun blackjackCard_inequality_differentSuit() {
        val card1 = BlackjackCard("hearts", "A")
        val card2 = BlackjackCard("spades", "A")
        assertNotEquals(card1, card2)
    }

    @Test
    fun blackjackCard_inequality_differentRank() {
        val card1 = BlackjackCard("hearts", "A")
        val card2 = BlackjackCard("hearts", "K")
        assertNotEquals(card1, card2)
    }

    @Test
    fun blackjackCard_copy() {
        val card = BlackjackCard("hearts", "A")
        val copy = card.copy(rank = "K")
        assertEquals("K", copy.rank)
        assertEquals("hearts", copy.suit)
    }

    // ── BlackjackHand ──

    @Test
    fun blackjackHand_construction() {
        val hand = BlackjackHand(
            cards = listOf(BlackjackCard("hearts", "A"), BlackjackCard("spades", "K")),
            bet = 50,
            status = "BLACKJACK",
            isDoubled = false,
            value = 21
        )
        assertEquals(2, hand.cards.size)
        assertEquals(50, hand.bet)
        assertEquals("BLACKJACK", hand.status)
        assertFalse(hand.isDoubled)
        assertEquals(21, hand.value)
    }

    @Test
    fun blackjackHand_doubled() {
        val hand = BlackjackHand(
            cards = listOf(BlackjackCard("hearts", "5"), BlackjackCard("spades", "6")),
            bet = 100,
            status = "PLAYING",
            isDoubled = true,
            value = 11
        )
        assertTrue(hand.isDoubled)
        assertEquals(100, hand.bet)
    }

    // ── BlackjackHandResult ──

    @Test
    fun blackjackHandResult_winCalculation() {
        val result = BlackjackHandResult(bet = 50, gains = 100, playerValue = 20, status = "WON")
        val netGain = result.gains - result.bet
        assertEquals(50, netGain)
    }

    @Test
    fun blackjackHandResult_lossCalculation() {
        val result = BlackjackHandResult(bet = 50, gains = 0, playerValue = 22, status = "BUSTED")
        val netGain = result.gains - result.bet
        assertEquals(-50, netGain)
    }

    @Test
    fun blackjackHandResult_pushCalculation() {
        val result = BlackjackHandResult(bet = 50, gains = 50, playerValue = 20, status = "PUSH")
        val netGain = result.gains - result.bet
        assertEquals(0, netGain)
    }

    // ── BlackjackGameState ──

    @Test
    fun blackjackGameState_construction() {
        val state = BlackjackGameState(
            hands = listOf(
                BlackjackHand(
                    cards = listOf(BlackjackCard("hearts", "10"), BlackjackCard("spades", "5")),
                    bet = 10, status = "PLAYING", isDoubled = false, value = 15
                )
            ),
            activeHandIndex = 0,
            dealerUpCard = BlackjackCard("diamonds", "7"),
            phase = "PLAYING",
            insuranceBet = 0,
            canInsure = false,
            deckRemaining = 280,
            deckTotal = 312,
            reshuffled = false
        )
        assertEquals(1, state.hands.size)
        assertEquals(0, state.activeHandIndex)
        assertEquals("7", state.dealerUpCard.rank)
        assertFalse(state.canInsure)
        assertFalse(state.reshuffled)
    }

    // ── BlackjackBetResult ──

    @Test
    fun blackjackBetResult_totalGains() {
        val result = BlackjackBetResult(
            gains = 150,
            hands = listOf(
                BlackjackHandResult(bet = 50, gains = 100, playerValue = 20, status = "WON"),
                BlackjackHandResult(bet = 50, gains = 50, playerValue = 18, status = "PUSH")
            ),
            playerHands = emptyList(),
            dealerHand = listOf(BlackjackCard("hearts", "10"), BlackjackCard("spades", "7")),
            dealerValue = 17,
            insuranceGain = 0,
            balance = 650
        )
        assertEquals(150, result.gains)
        assertEquals(2, result.hands.size)
        assertEquals(650, result.balance)
    }

    // ── BlackjackPlayerHand ──

    @Test
    fun blackjackPlayerHand_construction() {
        val hand = BlackjackPlayerHand(
            cards = listOf(BlackjackCard("hearts", "A"), BlackjackCard("spades", "K")),
            bet = 50,
            isDoubled = false,
            value = 21
        )
        assertEquals(2, hand.cards.size)
        assertEquals(21, hand.value)
    }

    // ── AuthResponse ──

    @Test
    fun authResponse_construction() {
        val auth = AuthResponse(token = "jwt_token_123", username = "player1", balance = 500)
        assertEquals("jwt_token_123", auth.token)
        assertEquals("player1", auth.username)
        assertEquals(500, auth.balance)
    }

    // ── MeResponse ──

    @Test
    fun meResponse_construction() {
        val me = MeResponse(
            username = "player1",
            email = "player1@test.com",
            balance = 1000,
            createdAt = "2025-01-01",
            hasClaimedDailyBonus = true
        )
        assertEquals("player1", me.username)
        assertEquals("player1@test.com", me.email)
        assertEquals(1000, me.balance)
        assertTrue(me.hasClaimedDailyBonus)
    }

    @Test
    fun meResponse_notClaimedBonus() {
        val me = MeResponse(
            username = "player2",
            email = "p2@test.com",
            balance = 0,
            createdAt = "2025-06-15",
            hasClaimedDailyBonus = false
        )
        assertFalse(me.hasClaimedDailyBonus)
    }

    // ── SlotSpinResponse ──

    @Test
    fun slotSpinResponse_construction() {
        val response = SlotSpinResponse(result = listOf(1, 3, 5), gain = 0, balance = 490)
        assertEquals(listOf(1, 3, 5), response.result)
        assertEquals(0, response.gain)
        assertEquals(490, response.balance)
    }

    @Test
    fun slotSpinResponse_jackpot() {
        val response = SlotSpinResponse(result = listOf(7, 7, 7), gain = 5000, balance = 5490)
        assertTrue(response.result.all { it == 7 })
        assertEquals(5000, response.gain)
    }

    @Test
    fun slotSpinResponse_3reels() {
        val response = SlotSpinResponse(result = listOf(1, 2, 3), gain = 0, balance = 100)
        assertEquals(3, response.result.size)
    }

    // ── GameItem ──

    @Test
    fun gameItem_withDate() {
        val item = GameItem(title = "Blackjack", iconResId = 0, dateLastGame = "23/12/2025")
        assertEquals("Blackjack", item.title)
        assertEquals("23/12/2025", item.dateLastGame)
    }

    @Test
    fun gameItem_withoutDate() {
        val item = GameItem(title = "Roulette", iconResId = 0)
        assertNull(item.dateLastGame)
    }

    // ── Match ──

    @Test
    fun match_construction() {
        val match = Match(
            id = 1, team1 = "PSG", team2 = "OM",
            odds1 = 1.5, oddsDraw = 3.5, odds2 = 6.0,
            date = "Aujourd'hui 20:00"
        )
        assertEquals(1, match.id)
        assertEquals("PSG", match.team1)
        assertEquals("OM", match.team2)
        assertEquals(1.5, match.odds1, 0.001)
        assertEquals(3.5, match.oddsDraw, 0.001)
        assertEquals(6.0, match.odds2, 0.001)
    }

    @Test
    fun match_equality() {
        val m1 = Match(1, "PSG", "OM", 1.5, 3.5, 6.0, "date")
        val m2 = Match(1, "PSG", "OM", 1.5, 3.5, 6.0, "date")
        assertEquals(m1, m2)
    }

    // ── PinosOffer ──

    @Test
    fun pinosOffer_freeOffer() {
        val offer = PinosOffer(
            label = "Starter", pinos = 500,
            priceDisplay = "Gratuit", drawableResId = 0, isFree = true
        )
        assertTrue(offer.isFree)
        assertEquals(500, offer.pinos)
    }

    @Test
    fun pinosOffer_paidOffer() {
        val offer = PinosOffer(
            label = "Gold", pinos = 15_000,
            priceDisplay = "6,99 €", drawableResId = 0
        )
        assertFalse(offer.isFree)
        assertEquals(15_000, offer.pinos)
    }
}
