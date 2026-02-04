package com.example.mobile.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile.R
import com.example.mobile.ui.components.AppButton
import com.example.mobile.ui.components.AppCard
import com.example.mobile.ui.components.AppHeader
import com.example.mobile.ui.components.ButtonSize
import com.example.mobile.ui.components.ButtonVariant
import com.example.mobile.ui.components.CardVariant
import com.example.mobile.ui.theme.DarkSurface
import com.example.mobile.ui.theme.DarkTextPrimary
import com.example.mobile.ui.theme.DarkTextSecondary
import kotlin.random.Random

@Composable
fun BlackjackScreen(
    onBackClick: () -> Unit
) {
    var playerCards by remember { mutableStateOf<List<Int>>(emptyList()) }
    var dealerCards by remember { mutableStateOf<List<Int>>(emptyList()) }
    var gameStarted by remember { mutableStateOf(false) }
    var gameOver by remember { mutableStateOf(false) }
    var playerScore by remember { mutableStateOf(0) }
    var dealerScore by remember { mutableStateOf(0) }
    var gameResult by remember { mutableStateOf<String?>(null) }

    fun drawCard(): Int {
        return Random.nextInt(1, 14) // 1-13 (As à Roi)
    }

    fun calculateScore(cards: List<Int>): Int {
        var score = 0
        var aces = 0
        cards.forEach { card ->
            when (card) {
                1 -> {
                    aces++
                    score += 11
                }
                in 2..10 -> score += card
                else -> score += 10 // Valet, Dame, Roi
            }
        }
        // Ajuster les As si nécessaire
        while (score > 21 && aces > 0) {
            score -= 10
            aces--
        }
        return score
    }

    fun updateScores() {
        playerScore = calculateScore(playerCards)
        dealerScore = calculateScore(dealerCards)
    }

    fun dealInitialCards() {
        playerCards = listOf(drawCard(), drawCard())
        dealerCards = listOf(drawCard(), drawCard())
        gameStarted = true
        gameOver = false
        gameResult = null
        updateScores()
    }

    fun hit() {
        if (!gameOver && gameStarted) {
            playerCards = playerCards + drawCard()
            updateScores()
            if (playerScore > 21) {
                gameOver = true
                gameResult = "Vous avez perdu !"
            }
        }
    }

    fun stand() {
        if (!gameOver && gameStarted) {
            // Le croupier tire jusqu'à 17
            var newDealerCards = dealerCards.toList()
            while (calculateScore(newDealerCards) < 17) {
                newDealerCards = newDealerCards + drawCard()
            }
            dealerCards = newDealerCards
            updateScores()
            gameOver = true
            
            when {
                dealerScore > 21 -> gameResult = "Vous avez gagné !"
                dealerScore > playerScore -> gameResult = "Vous avez perdu !"
                dealerScore < playerScore -> gameResult = "Vous avez gagné !"
                else -> gameResult = "Égalité !"
            }
        }
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = "Blackjack",
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Score du croupier
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                variant = CardVariant.Outlined
            ) {
                Text(
                    text = "Croupier: ${if (gameStarted) dealerScore else "?"}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTextPrimary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                // Cartes du croupier
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dealerCards.forEachIndexed { index, card ->
                        CardDisplay(
                            card = card,
                            hidden = index == 1 && !gameOver && gameStarted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Cartes du joueur
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                variant = CardVariant.Outlined
            ) {
                Text(
                    text = "Vous: ${if (gameStarted) playerScore else "0"}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTextPrimary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    playerCards.forEach { card ->
                        CardDisplay(card = card, hidden = false)
                    }
                }
            }

            // Résultat du jeu
            if (gameResult != null) {
                Text(
                    text = gameResult!!,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTextPrimary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Boutons d'action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!gameStarted) {
                    AppButton(
                        text = "Distribuer",
                        onClick = { dealInitialCards() },
                        variant = ButtonVariant.Primary,
                        size = ButtonSize.Large,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    AppButton(
                        text = "Tirer",
                        onClick = { hit() },
                        variant = ButtonVariant.Primary,
                        size = ButtonSize.Large,
                        enabled = !gameOver,
                        modifier = Modifier.weight(1f)
                    )
                    AppButton(
                        text = "Rester",
                        onClick = { stand() },
                        variant = ButtonVariant.Outline,
                        size = ButtonSize.Large,
                        enabled = !gameOver,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (gameOver) {
                AppButton(
                    text = "Nouvelle partie",
                    onClick = {
                        playerCards = emptyList()
                        dealerCards = emptyList()
                        gameStarted = false
                        gameOver = false
                        gameResult = null
                        playerScore = 0
                        dealerScore = 0
                    },
                    variant = ButtonVariant.Secondary,
                    size = ButtonSize.Medium,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun CardDisplay(
    card: Int,
    hidden: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 60.dp, height = 84.dp)
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (hidden) {
                    Modifier
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (hidden) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
            ) {
                // Carte cachée - utiliser une image si disponible
                Text(
                    text = "?",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTextSecondary
                )
            }
        } else {
            val cardValue = when (card) {
                1 -> "A"
                11 -> "J"
                12 -> "Q"
                13 -> "K"
                else -> card.toString()
            }
            
            Text(
                text = cardValue,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DarkTextPrimary
            )
        }
    }
}
