package com.example.mobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.mobile.network.BlackjackCard
import com.example.mobile.ui.screens.BlackjackCardDisplay
import org.junit.Rule
import org.junit.Test

class BlackjackCardDisplayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysQuestionMark_whenCardIsNull() {
        composeTestRule.setContent {
            BlackjackCardDisplay(card = null)
        }
        composeTestRule.onNodeWithText("?").assertIsDisplayed()
    }

    @Test
    fun displaysRank_whenCardProvided() {
        composeTestRule.setContent {
            BlackjackCardDisplay(card = BlackjackCard(suit = "hearts", rank = "A"))
        }
        composeTestRule.onNodeWithText("A").assertIsDisplayed()
    }

    @Test
    fun displaysHeartSymbol_forHeartsSuit() {
        composeTestRule.setContent {
            BlackjackCardDisplay(card = BlackjackCard(suit = "hearts", rank = "K"))
        }
        composeTestRule.onNodeWithText("K").assertIsDisplayed()
        composeTestRule.onNodeWithText("\u2665").assertIsDisplayed()
    }

    @Test
    fun displaysDiamondSymbol_forDiamondsSuit() {
        composeTestRule.setContent {
            BlackjackCardDisplay(card = BlackjackCard(suit = "diamonds", rank = "Q"))
        }
        composeTestRule.onNodeWithText("Q").assertIsDisplayed()
        composeTestRule.onNodeWithText("\u2666").assertIsDisplayed()
    }

    @Test
    fun displaysClubSymbol_forClubsSuit() {
        composeTestRule.setContent {
            BlackjackCardDisplay(card = BlackjackCard(suit = "clubs", rank = "J"))
        }
        composeTestRule.onNodeWithText("J").assertIsDisplayed()
        composeTestRule.onNodeWithText("\u2663").assertIsDisplayed()
    }

    @Test
    fun displaysSpadeSymbol_forSpadesSuit() {
        composeTestRule.setContent {
            BlackjackCardDisplay(card = BlackjackCard(suit = "spades", rank = "10"))
        }
        composeTestRule.onNodeWithText("10").assertIsDisplayed()
        composeTestRule.onNodeWithText("\u2660").assertIsDisplayed()
    }

    @Test
    fun displaysUnknownSuit_asQuestionMark() {
        composeTestRule.setContent {
            BlackjackCardDisplay(card = BlackjackCard(suit = "unknown", rank = "5"))
        }
        composeTestRule.onNodeWithText("5").assertIsDisplayed()
        composeTestRule.onNodeWithText("?").assertIsDisplayed()
    }

    @Test
    fun displaysNumericRank() {
        composeTestRule.setContent {
            BlackjackCardDisplay(card = BlackjackCard(suit = "spades", rank = "7"))
        }
        composeTestRule.onNodeWithText("7").assertIsDisplayed()
    }
}
