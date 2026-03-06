package com.example.mobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.mobile.ui.screens.BetButton
import com.example.mobile.ui.screens.MatchCard
import com.example.mobile.ui.screens.Match
import org.junit.Rule
import org.junit.Test

class SportsBettingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleMatch = Match(
        id = 1,
        team1 = "PSG",
        team2 = "OM",
        odds1 = 1.5,
        oddsDraw = 3.5,
        odds2 = 6.0,
        date = "Aujourd'hui 20:00"
    )

    // ── BetButton ──

    @Test
    fun betButton_displaysText() {
        composeTestRule.setContent {
            BetButton(
                text = "PSG\n1.5",
                onClick = {},
                enabled = true
            )
        }
        composeTestRule.onNodeWithText("PSG\n1.5").assertIsDisplayed()
    }

    @Test
    fun betButton_disabledWhenNotEnabled() {
        composeTestRule.setContent {
            BetButton(
                text = "PSG\n1.5",
                onClick = {},
                enabled = false
            )
        }
        composeTestRule.onNodeWithText("PSG\n1.5").assertIsDisplayed()
    }

    // ── MatchCard ──

    @Test
    fun matchCard_displaysTeamNames() {
        composeTestRule.setContent {
            MatchCard(
                match = sampleMatch,
                selectedBet = null,
                betAmount = 10,
                onBetClick = { _, _ -> },
                onRemoveBet = {},
                balance = 1000
            )
        }
        composeTestRule.onNodeWithText("PSG").assertIsDisplayed()
        composeTestRule.onNodeWithText("OM").assertIsDisplayed()
    }

    @Test
    fun matchCard_displaysDate() {
        composeTestRule.setContent {
            MatchCard(
                match = sampleMatch,
                selectedBet = null,
                betAmount = 10,
                onBetClick = { _, _ -> },
                onRemoveBet = {},
                balance = 1000
            )
        }
        composeTestRule.onNodeWithText("Aujourd'hui 20:00").assertIsDisplayed()
    }

    @Test
    fun matchCard_displaysVS() {
        composeTestRule.setContent {
            MatchCard(
                match = sampleMatch,
                selectedBet = null,
                betAmount = 10,
                onBetClick = { _, _ -> },
                onRemoveBet = {},
                balance = 1000
            )
        }
        composeTestRule.onNodeWithText("VS").assertIsDisplayed()
    }

    @Test
    fun matchCard_displaysBetButtons_whenNoBetSelected() {
        composeTestRule.setContent {
            MatchCard(
                match = sampleMatch,
                selectedBet = null,
                betAmount = 10,
                onBetClick = { _, _ -> },
                onRemoveBet = {},
                balance = 1000
            )
        }
        composeTestRule.onNodeWithText("PSG\n${sampleMatch.odds1}").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nul\n${sampleMatch.oddsDraw}").assertIsDisplayed()
        composeTestRule.onNodeWithText("OM\n${sampleMatch.odds2}").assertIsDisplayed()
    }

    @Test
    fun matchCard_displaysCancelButton_whenBetSelected() {
        composeTestRule.setContent {
            MatchCard(
                match = sampleMatch,
                selectedBet = Pair("1", 100),
                betAmount = 10,
                onBetClick = { _, _ -> },
                onRemoveBet = {},
                balance = 1000
            )
        }
        composeTestRule.onNodeWithText("Annuler le pari").assertIsDisplayed()
    }

    @Test
    fun matchCard_displaysBetAmount_whenBetSelected() {
        composeTestRule.setContent {
            MatchCard(
                match = sampleMatch,
                selectedBet = Pair("1", 100),
                betAmount = 10,
                onBetClick = { _, _ -> },
                onRemoveBet = {},
                balance = 1000
            )
        }
        composeTestRule.onNodeWithText("Mise: 100").assertIsDisplayed()
    }

    @Test
    fun matchCard_differentMatch_displaysCorrectTeams() {
        val match = Match(2, "Real Madrid", "Barcelona", 2.1, 3.2, 3.0, "Demain 21:00")
        composeTestRule.setContent {
            MatchCard(
                match = match,
                selectedBet = null,
                betAmount = 50,
                onBetClick = { _, _ -> },
                onRemoveBet = {},
                balance = 500
            )
        }
        composeTestRule.onNodeWithText("Real Madrid").assertIsDisplayed()
        composeTestRule.onNodeWithText("Barcelona").assertIsDisplayed()
    }
}
