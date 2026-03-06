package com.example.mobile

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.mobile.ui.screens.RouletteBetChipStatic
import org.junit.Rule
import org.junit.Test

class RouletteBetChipTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysLabel() {
        composeTestRule.setContent {
            RouletteBetChipStatic(
                label = "17\n50",
                baseColor = Color(0xFFB71C1C)
            )
        }
        composeTestRule.onNodeWithText("17\n50").assertIsDisplayed()
    }

    @Test
    fun displaysWinnerChip() {
        composeTestRule.setContent {
            RouletteBetChipStatic(
                label = "Rouge\n100",
                baseColor = Color(0xFFB71C1C),
                isWinner = true
            )
        }
        composeTestRule.onNodeWithText("Rouge\n100").assertIsDisplayed()
    }

    @Test
    fun displaysLoserChip() {
        composeTestRule.setContent {
            RouletteBetChipStatic(
                label = "Noir\n50",
                baseColor = Color(0xFF212121),
                isWinner = false
            )
        }
        composeTestRule.onNodeWithText("Noir\n50").assertIsDisplayed()
    }

    @Test
    fun displaysMultiBetChip() {
        composeTestRule.setContent {
            RouletteBetChipStatic(
                label = "Cheval\n1-2\n25",
                baseColor = Color(0xFF37474F),
                isWinner = false
            )
        }
        composeTestRule.onNodeWithText("Cheval\n1-2\n25").assertIsDisplayed()
    }

    @Test
    fun displaysDozenChip() {
        composeTestRule.setContent {
            RouletteBetChipStatic(
                label = "1ere 12\n100",
                baseColor = Color(0xFF263238)
            )
        }
        composeTestRule.onNodeWithText("1ere 12\n100").assertIsDisplayed()
    }
}
