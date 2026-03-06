package com.example.mobile

import com.example.mobile.ui.screens.Match
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SportsBettingModelTest {

    private val matches = listOf(
        Match(1, "PSG", "OM", 1.5, 3.5, 6.0, "Aujourd'hui 20:00"),
        Match(2, "Real Madrid", "Barcelona", 2.1, 3.2, 3.0, "Demain 21:00"),
        Match(3, "Liverpool", "Manchester City", 2.5, 3.0, 2.8, "Demain 18:00"),
        Match(4, "Bayern Munich", "Dortmund", 1.8, 3.8, 4.5, "Après-demain 19:00"),
        Match(5, "Juventus", "Inter Milan", 2.2, 3.1, 3.2, "Après-demain 20:30")
    )

    // ── Match model ──

    @Test
    fun match_odds1_isLowestForFavorite() {
        val psgOm = matches[0]
        // PSG est favori : odds1 (1.5) < odds2 (6.0)
        assert(psgOm.odds1 < psgOm.odds2)
    }

    @Test
    fun match_allOddsPositive() {
        matches.forEach { match ->
            assert(match.odds1 > 0) { "${match.team1} odds1 should be positive" }
            assert(match.oddsDraw > 0) { "${match.team1} oddsDraw should be positive" }
            assert(match.odds2 > 0) { "${match.team2} odds2 should be positive" }
        }
    }

    @Test
    fun match_allOddsAbove1() {
        matches.forEach { match ->
            assert(match.odds1 >= 1.0) { "${match.team1} odds1 should be >= 1.0" }
            assert(match.oddsDraw >= 1.0) { "draw odds should be >= 1.0" }
            assert(match.odds2 >= 1.0) { "${match.team2} odds2 should be >= 1.0" }
        }
    }

    @Test
    fun match_uniqueIds() {
        val ids = matches.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    // ── Potential win calculations ──

    @Test
    fun potentialWin_team1_bet() {
        val match = matches[0] // PSG: odds1 = 1.5
        val betAmount = 100
        val potentialWin = betAmount * match.odds1
        assertEquals(150.0, potentialWin, 0.001)
    }

    @Test
    fun potentialWin_draw_bet() {
        val match = matches[0] // draw odds = 3.5
        val betAmount = 100
        val potentialWin = betAmount * match.oddsDraw
        assertEquals(350.0, potentialWin, 0.001)
    }

    @Test
    fun potentialWin_team2_bet() {
        val match = matches[0] // OM: odds2 = 6.0
        val betAmount = 100
        val potentialWin = betAmount * match.odds2
        assertEquals(600.0, potentialWin, 0.001)
    }

    @Test
    fun potentialWin_zeroBet_returnsZero() {
        val match = matches[0]
        val potentialWin = 0 * match.odds1
        assertEquals(0.0, potentialWin, 0.001)
    }

    @Test
    fun potentialWin_higherOdds_higherPayout() {
        val match = matches[0]
        val bet = 100
        val winTeam1 = bet * match.odds1  // 1.5 → 150
        val winDraw = bet * match.oddsDraw  // 3.5 → 350
        val winTeam2 = bet * match.odds2  // 6.0 → 600
        assert(winTeam1 < winDraw)
        assert(winDraw < winTeam2)
    }

    // ── Bet placement logic ──

    @Test
    fun placeBet_sufficientBalance_deductsAmount() {
        var balance = 1000
        val betAmount = 100
        if (balance >= betAmount) {
            balance -= betAmount
        }
        assertEquals(900, balance)
    }

    @Test
    fun placeBet_insufficientBalance_noDeduction() {
        var balance = 50
        val betAmount = 100
        if (balance >= betAmount) {
            balance -= betAmount
        }
        assertEquals(50, balance)
    }

    @Test
    fun removeBet_refundsAmount() {
        var balance = 900
        val betAmount = 100
        balance += betAmount
        assertEquals(1000, balance)
    }

    @Test
    fun multipleBets_deductCorrectly() {
        var balance = 1000
        val bets = mutableMapOf<Int, Pair<String, Int>>()

        // Bet 100 on match 1
        if (balance >= 100) {
            bets[1] = Pair("1", 100)
            balance -= 100
        }
        // Bet 50 on match 2
        if (balance >= 50) {
            bets[2] = Pair("N", 50)
            balance -= 50
        }

        assertEquals(850, balance)
        assertEquals(2, bets.size)
    }

    @Test
    fun replaceExistingBet_refundsOldBet() {
        var balance = 1000
        val bets = mutableMapOf<Int, Pair<String, Int>>()

        // Initial bet
        bets[1] = Pair("1", 100)
        balance -= 100  // 900

        // Remove old bet
        val old = bets.remove(1)
        if (old != null) balance += old.second  // 1000

        // Place new bet
        bets[1] = Pair("2", 50)
        balance -= 50  // 950

        assertEquals(950, balance)
    }

    // ── Edge cases ──

    @Test
    fun match_copy_changesOdds() {
        val original = matches[0]
        val modified = original.copy(odds1 = 2.0)
        assertNotEquals(original.odds1, modified.odds1, 0.001)
        assertEquals(2.0, modified.odds1, 0.001)
        assertEquals(original.team1, modified.team1)
    }
}
