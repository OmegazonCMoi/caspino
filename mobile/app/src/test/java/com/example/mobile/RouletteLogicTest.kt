package com.example.mobile

import androidx.compose.ui.graphics.Color
import com.example.mobile.ui.screens.EDGE_THRESHOLD
import com.example.mobile.ui.screens.chipColor
import com.example.mobile.ui.screens.chipDenominations
import com.example.mobile.ui.screens.darkenColor
import com.example.mobile.ui.screens.determineBetTarget
import com.example.mobile.ui.screens.isRouletteRed
import com.example.mobile.ui.screens.isWinningGroupForDisplay
import com.example.mobile.ui.screens.multiBetCenterPx
import com.example.mobile.ui.screens.numberAt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouletteLogicTest {

    // ── numberAt ──

    @Test
    fun numberAt_firstCell_returns1() {
        assertEquals(1, numberAt(0, 0))
    }

    @Test
    fun numberAt_lastCell_returns36() {
        assertEquals(36, numberAt(11, 2))
    }

    @Test
    fun numberAt_row0col2_returns3() {
        assertEquals(3, numberAt(0, 2))
    }

    @Test
    fun numberAt_row1col0_returns4() {
        assertEquals(4, numberAt(1, 0))
    }

    @Test
    fun numberAt_row5col1_returns17() {
        // row=5, col=1 → 5*3 + 1 + 1 = 17
        assertEquals(17, numberAt(5, 1))
    }

    // ── isRouletteRed ──

    @Test
    fun isRouletteRed_redNumbers() {
        val redNumbers = setOf(1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36)
        redNumbers.forEach { number ->
            assertTrue("$number should be red", isRouletteRed(number))
        }
    }

    @Test
    fun isRouletteRed_blackNumbers() {
        val blackNumbers = setOf(2, 4, 6, 8, 10, 11, 13, 15, 17, 20, 22, 24, 26, 28, 29, 31, 33, 35)
        blackNumbers.forEach { number ->
            assertFalse("$number should not be red", isRouletteRed(number))
        }
    }

    @Test
    fun isRouletteRed_zero_isNotRed() {
        assertFalse(isRouletteRed(0))
    }

    @Test
    fun isRouletteRed_totalRedCount_is18() {
        val count = (1..36).count { isRouletteRed(it) }
        assertEquals(18, count)
    }

    // ── isWinningGroupForDisplay ──

    @Test
    fun isWinningGroup_PAIR_evenNumberWins() {
        assertTrue(isWinningGroupForDisplay("PAIR", 2))
        assertTrue(isWinningGroupForDisplay("PAIR", 36))
    }

    @Test
    fun isWinningGroup_PAIR_oddNumberLoses() {
        assertFalse(isWinningGroupForDisplay("PAIR", 1))
        assertFalse(isWinningGroupForDisplay("PAIR", 35))
    }

    @Test
    fun isWinningGroup_PAIR_zeroLoses() {
        assertFalse(isWinningGroupForDisplay("PAIR", 0))
    }

    @Test
    fun isWinningGroup_IMPAIR_oddNumberWins() {
        assertTrue(isWinningGroupForDisplay("IMPAIR", 1))
        assertTrue(isWinningGroupForDisplay("IMPAIR", 35))
    }

    @Test
    fun isWinningGroup_IMPAIR_evenNumberLoses() {
        assertFalse(isWinningGroupForDisplay("IMPAIR", 2))
    }

    @Test
    fun isWinningGroup_LOW_1to18() {
        assertTrue(isWinningGroupForDisplay("LOW", 1))
        assertTrue(isWinningGroupForDisplay("LOW", 18))
        assertFalse(isWinningGroupForDisplay("LOW", 19))
        assertFalse(isWinningGroupForDisplay("LOW", 0))
    }

    @Test
    fun isWinningGroup_HIGH_19to36() {
        assertTrue(isWinningGroupForDisplay("HIGH", 19))
        assertTrue(isWinningGroupForDisplay("HIGH", 36))
        assertFalse(isWinningGroupForDisplay("HIGH", 18))
        assertFalse(isWinningGroupForDisplay("HIGH", 0))
    }

    @Test
    fun isWinningGroup_RED_matchesIsRouletteRed() {
        for (n in 0..36) {
            val expected = n != 0 && isRouletteRed(n)
            assertEquals("RED group for $n", expected, isWinningGroupForDisplay("RED", n))
        }
    }

    @Test
    fun isWinningGroup_BLACK_matchesNotRed() {
        for (n in 0..36) {
            val expected = n != 0 && !isRouletteRed(n)
            assertEquals("BLACK group for $n", expected, isWinningGroupForDisplay("BLACK", n))
        }
    }

    @Test
    fun isWinningGroup_dozen1_1to12() {
        for (n in 1..12) assertTrue(isWinningGroupForDisplay("D_1ere 12", n))
        for (n in 13..36) assertFalse(isWinningGroupForDisplay("D_1ere 12", n))
        assertFalse(isWinningGroupForDisplay("D_1ere 12", 0))
    }

    @Test
    fun isWinningGroup_dozen2_13to24() {
        for (n in 1..12) assertFalse(isWinningGroupForDisplay("D_2eme 12", n))
        for (n in 13..24) assertTrue(isWinningGroupForDisplay("D_2eme 12", n))
        for (n in 25..36) assertFalse(isWinningGroupForDisplay("D_2eme 12", n))
    }

    @Test
    fun isWinningGroup_dozen3_25to36() {
        for (n in 1..24) assertFalse(isWinningGroupForDisplay("D_3eme 12", n))
        for (n in 25..36) assertTrue(isWinningGroupForDisplay("D_3eme 12", n))
    }

    @Test
    fun isWinningGroup_unknownGroup_returnsFalse() {
        assertFalse(isWinningGroupForDisplay("UNKNOWN", 5))
    }

    @Test
    fun isWinningGroup_outOfRange_returnsFalse() {
        assertFalse(isWinningGroupForDisplay("RED", -1))
        assertFalse(isWinningGroupForDisplay("RED", 37))
    }

    // ── chipColor ──

    @Test
    fun chipColor_knownDenominations() {
        assertEquals(Color(0xFF1565C0), chipColor(10))
        assertEquals(Color(0xFF2E7D32), chipColor(25))
        assertEquals(Color(0xFFE65100), chipColor(50))
        assertEquals(Color(0xFF212121), chipColor(100))
        assertEquals(Color(0xFF6A1B9A), chipColor(500))
    }

    @Test
    fun chipColor_unknownDenomination_returnsGray() {
        assertEquals(Color.Gray, chipColor(1))
        assertEquals(Color.Gray, chipColor(999))
    }

    // ── chipDenominations ──

    @Test
    fun chipDenominations_contains5Values() {
        assertEquals(5, chipDenominations.size)
    }

    @Test
    fun chipDenominations_sortedAscending() {
        assertEquals(chipDenominations, chipDenominations.sorted())
    }

    // ── EDGE_THRESHOLD ──

    @Test
    fun edgeThreshold_isReasonableValue() {
        assertTrue(EDGE_THRESHOLD > 0f)
        assertTrue(EDGE_THRESHOLD < 0.5f)
    }

    // ── determineBetTarget ──

    @Test
    fun determineBetTarget_centerOfCell_returnsSingleNumber() {
        val cellW = 100f
        val cellH = 50f
        // Center of cell (row=0, col=0) → number 1
        val result = determineBetTarget(50f, 25f, cellW, cellH)
        assertEquals(listOf(1), result)
    }

    @Test
    fun determineBetTarget_centerOfCell_row0col1_returns2() {
        val cellW = 100f
        val cellH = 50f
        val result = determineBetTarget(150f, 25f, cellW, cellH)
        assertEquals(listOf(2), result)
    }

    @Test
    fun determineBetTarget_horizontalSplit_nearLeftEdge() {
        val cellW = 100f
        val cellH = 50f
        // Near left edge of cell (row=0, col=1), middle vertically → split between col 0 and col 1
        val tapX = cellW + cellW * 0.1f  // just inside col 1, near left edge
        val tapY = cellH * 0.5f  // middle of row 0
        val result = determineBetTarget(tapX, tapY, cellW, cellH)
        assertEquals(listOf(1, 2), result)
    }

    @Test
    fun determineBetTarget_verticalSplit_nearTopEdge() {
        val cellW = 100f
        val cellH = 50f
        // Near top edge of cell (row=1, col=0), middle horizontally → split between row 0 and row 1
        val tapX = cellW * 0.5f
        val tapY = cellH + cellH * 0.1f  // near top of row 1
        val result = determineBetTarget(tapX, tapY, cellW, cellH)
        assertEquals(listOf(1, 4), result)
    }

    @Test
    fun determineBetTarget_corner_fourNumbers() {
        val cellW = 100f
        val cellH = 50f
        // Near top-left corner of cell (row=1, col=1) → 4 numbers (carré)
        val tapX = cellW + cellW * 0.1f  // near left edge of col 1
        val tapY = cellH + cellH * 0.1f  // near top edge of row 1
        val result = determineBetTarget(tapX, tapY, cellW, cellH)
        assertEquals(listOf(1, 2, 4, 5), result)
    }

    // ── multiBetCenterPx ──

    @Test
    fun multiBetCenterPx_street_3numbers_returnsCorrectCenter() {
        val cellW = 100f
        val cellH = 50f
        val numbers = listOf(1, 2, 3)
        val (cx, cy) = multiBetCenterPx(numbers, cellW, cellH)
        assertEquals(0f, cx, 0.01f)
        assertEquals(25f, cy, 0.01f)
    }

    @Test
    fun multiBetCenterPx_split_2numbers_returnsAverage() {
        val cellW = 100f
        val cellH = 50f
        val numbers = listOf(1, 2)
        val (cx, cy) = multiBetCenterPx(numbers, cellW, cellH)
        // number 1 → (col=0, row=0) center = (50, 25)
        // number 2 → (col=1, row=0) center = (150, 25)
        assertEquals(100f, cx, 0.01f)
        assertEquals(25f, cy, 0.01f)
    }

    // ── darkenColor ──

    @Test
    fun darkenColor_factor1_returnsOriginal() {
        val original = Color(1f, 0.5f, 0.25f, 1f)
        val result = darkenColor(original, 1f)
        assertEquals(original.red, result.red, 0.001f)
        assertEquals(original.green, result.green, 0.001f)
        assertEquals(original.blue, result.blue, 0.001f)
    }

    @Test
    fun darkenColor_factor0_returnsBlack() {
        val original = Color(1f, 0.5f, 0.25f, 1f)
        val result = darkenColor(original, 0f)
        assertEquals(0f, result.red, 0.001f)
        assertEquals(0f, result.green, 0.001f)
        assertEquals(0f, result.blue, 0.001f)
    }

    @Test
    fun darkenColor_factorHalf_halvesRGB() {
        val original = Color(1f, 0.8f, 0.6f, 1f)
        val result = darkenColor(original, 0.5f)
        assertEquals(0.5f, result.red, 0.01f)
        assertEquals(0.4f, result.green, 0.01f)
        assertEquals(0.3f, result.blue, 0.01f)
    }

    @Test
    fun darkenColor_preservesAlpha() {
        val original = Color(1f, 1f, 1f, 0.5f)
        val result = darkenColor(original, 0.5f)
        assertEquals(0.5f, result.alpha, 0.01f)
    }

    @Test
    fun darkenColor_factorAbove1_clampedTo1() {
        val original = Color(0.5f, 0.5f, 0.5f, 1f)
        val result = darkenColor(original, 2f)
        assertEquals(0.5f, result.red, 0.01f)
    }

    @Test
    fun darkenColor_factorBelow0_clampedTo0() {
        val original = Color(0.5f, 0.5f, 0.5f, 1f)
        val result = darkenColor(original, -1f)
        assertEquals(0f, result.red, 0.001f)
    }
}
