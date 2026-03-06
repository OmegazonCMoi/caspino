package com.example.mobile

import com.example.mobile.ui.screens.BalanceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BalanceStateTest {

    @Before
    fun setUp() {
        BalanceState.setBalance(0)
        BalanceState.resetDailyClaim()
    }

    // ── setBalance ──

    @Test
    fun setBalance_setsCorrectValue() {
        BalanceState.setBalance(1000)
        assertEquals(1000, BalanceState.balance.intValue)
    }

    @Test
    fun setBalance_overwritesPreviousValue() {
        BalanceState.setBalance(500)
        BalanceState.setBalance(200)
        assertEquals(200, BalanceState.balance.intValue)
    }

    @Test
    fun setBalance_acceptsZero() {
        BalanceState.setBalance(0)
        assertEquals(0, BalanceState.balance.intValue)
    }

    @Test
    fun setBalance_acceptsNegativeValue() {
        BalanceState.setBalance(-50)
        assertEquals(-50, BalanceState.balance.intValue)
    }

    // ── addPinos ──

    @Test
    fun addPinos_increasesBalance() {
        BalanceState.setBalance(100)
        BalanceState.addPinos(50)
        assertEquals(150, BalanceState.balance.intValue)
    }

    @Test
    fun addPinos_decreasesBalanceWithNegativeAmount() {
        BalanceState.setBalance(100)
        BalanceState.addPinos(-30)
        assertEquals(70, BalanceState.balance.intValue)
    }

    @Test
    fun addPinos_multipleAdds_accumulate() {
        BalanceState.setBalance(0)
        BalanceState.addPinos(10)
        BalanceState.addPinos(20)
        BalanceState.addPinos(30)
        assertEquals(60, BalanceState.balance.intValue)
    }

    @Test
    fun addPinos_zero_doesNothing() {
        BalanceState.setBalance(100)
        BalanceState.addPinos(0)
        assertEquals(100, BalanceState.balance.intValue)
    }

    @Test
    fun addPinos_largeAmount() {
        BalanceState.setBalance(0)
        BalanceState.addPinos(1_000_000)
        assertEquals(1_000_000, BalanceState.balance.intValue)
    }

    // ── Daily claim ──

    @Test
    fun hasClaimedFreeToday_falseByDefault() {
        assertFalse(BalanceState.hasClaimedFreeToday())
    }

    @Test
    fun markFreeClaimedToday_setsClaimedTrue() {
        BalanceState.markFreeClaimedToday()
        assertTrue(BalanceState.hasClaimedFreeToday())
    }

    @Test
    fun resetDailyClaim_resetsClaimedToFalse() {
        BalanceState.markFreeClaimedToday()
        assertTrue(BalanceState.hasClaimedFreeToday())
        BalanceState.resetDailyClaim()
        assertFalse(BalanceState.hasClaimedFreeToday())
    }

    @Test
    fun markFreeClaimedToday_idempotent() {
        BalanceState.markFreeClaimedToday()
        BalanceState.markFreeClaimedToday()
        assertTrue(BalanceState.hasClaimedFreeToday())
    }

    @Test
    fun resetDailyClaim_idempotent() {
        BalanceState.resetDailyClaim()
        BalanceState.resetDailyClaim()
        assertFalse(BalanceState.hasClaimedFreeToday())
    }

    // ── Combined scenarios ──

    @Test
    fun fullScenario_setAddClaimReset() {
        BalanceState.setBalance(500)
        BalanceState.addPinos(100)
        assertEquals(600, BalanceState.balance.intValue)

        BalanceState.markFreeClaimedToday()
        assertTrue(BalanceState.hasClaimedFreeToday())

        BalanceState.addPinos(-200)
        assertEquals(400, BalanceState.balance.intValue)

        BalanceState.resetDailyClaim()
        assertFalse(BalanceState.hasClaimedFreeToday())
    }
}
