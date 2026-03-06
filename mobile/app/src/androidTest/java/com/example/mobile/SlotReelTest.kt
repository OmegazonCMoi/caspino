package com.example.mobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.mobile.ui.screens.SlotReel
import org.junit.Rule
import org.junit.Test

class SlotReelTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun slotReel_rendersWithoutCrash_whenNotSpinning() {
        composeTestRule.setContent {
            SlotReel(
                iconResId = R.drawable.cherries,
                isSpinning = false
            )
        }
        composeTestRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun slotReel_rendersWithoutCrash_whenSpinning() {
        composeTestRule.setContent {
            SlotReel(
                iconResId = R.drawable.star,
                isSpinning = true
            )
        }
        composeTestRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun slotReel_rendersWithDifferentSymbols() {
        val symbols = listOf(
            R.drawable.cherries,
            R.drawable.lemon,
            R.drawable.tanguerine,
            R.drawable.grapes,
            R.drawable.bell,
            R.drawable.star,
            R.drawable.digit_seven
        )
        symbols.forEach { symbol ->
            composeTestRule.setContent {
                SlotReel(iconResId = symbol, isSpinning = false)
            }
            composeTestRule.onRoot().assertIsDisplayed()
        }
    }
}
