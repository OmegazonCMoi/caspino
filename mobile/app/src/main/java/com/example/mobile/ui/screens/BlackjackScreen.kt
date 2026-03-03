package com.example.mobile.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import com.example.mobile.MainActivity
import com.example.mobile.R
import com.example.mobile.ui.components.AppButton
import com.example.mobile.ui.components.AppHeader
import com.example.mobile.ui.components.BalanceHeader
import com.example.mobile.ui.components.AppBottomBar
import com.example.mobile.ui.components.BottomBarItem
import com.example.mobile.ui.components.ButtonSize
import com.example.mobile.ui.components.ButtonVariant
import com.example.mobile.ui.theme.DarkTextPrimary
import com.example.mobile.ui.theme.DarkTextSecondary
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.mobile.ui.icons.AppIcons
import kotlin.random.Random

@Composable
fun BlackjackScreen(
    onBackClick: () -> Unit
) {
    var balance by BalanceState.balance
    var bet by remember { mutableStateOf(10) }

    var playerCards by remember { mutableStateOf<List<Int>>(emptyList()) }
    var dealerCards by remember { mutableStateOf<List<Int>>(emptyList()) }
    var gameStarted by remember { mutableStateOf(false) }
    var gameOver by remember { mutableStateOf(false) }
    var playerScore by remember { mutableStateOf(0) }
    var dealerScore by remember { mutableStateOf(0) }
    var gameResult by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    val bottomBarItems = listOf(
        BottomBarItem(
            icon = AppIcons.Home,
            selectedIcon = AppIcons.HomeFilled
        ) {
            context.startActivity(Intent(context, MainActivity::class.java))
        },
        BottomBarItem(AppIcons.Search, AppIcons.SearchFilled) {
            context.startActivity(Intent(context, com.example.mobile.StatsActivity::class.java))
        },
        BottomBarItem(AppIcons.Profile, AppIcons.ProfileFilled) {
            context.startActivity(Intent(context, com.example.mobile.AccountActivity::class.java))
        },
        BottomBarItem(AppIcons.Cart, AppIcons.CartFilled) {
            context.startActivity(Intent(context, com.example.mobile.ShopActivity::class.java))
        }
    )

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

    fun startNewRound() {
        if (gameStarted || gameOver) return
        if (bet <= 0 || balance < bet) return

        // Déduire la mise de départ
        balance -= bet

        // Réinitialiser l'état de la manche et distribuer automatiquement
        playerCards = emptyList()
        dealerCards = emptyList()
        playerScore = 0
        dealerScore = 0
        gameResult = null
        gameOver = false

        dealInitialCards()
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
                // Victoire du joueur (croupier dépasse 21 ou a un score plus faible)
                dealerScore > 21 || dealerScore < playerScore -> {
                    gameResult = "Vous avez gagné !"
                    // Mise gagnante : on rend la mise + le même montant
                    balance += bet * 2
                }
                // Égalité : on rembourse simplement la mise
                dealerScore == playerScore -> {
                    gameResult = "Égalité !"
                    balance += bet
                }
                // Défaite du joueur : la mise est déjà perdue
                else -> {
                    gameResult = "Vous avez perdu !"
                }
            }
        }
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = "Blackjack",
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            AppBottomBar(
                items = bottomBarItems,
                selectedIndex = 0
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Solde commune
                BalanceHeader(
                    amount = balance,
                    modifier = Modifier
                        .padding(top = 40.dp)
                        .align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Étape 1 : uniquement la mise (avant de voir les cartes)
                if (!gameStarted && !gameOver) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Mise : $bet",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkTextPrimary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppButton(
                                text = "-10",
                                onClick = { if (bet > 10) bet -= 10 },
                                variant = ButtonVariant.Outline,
                                size = ButtonSize.Medium,
                                enabled = bet > 10,
                                modifier = Modifier.weight(1f)
                            )
                            AppButton(
                                text = "+10",
                                onClick = { if (bet < balance) bet += 10 },
                                variant = ButtonVariant.Outline,
                                size = ButtonSize.Medium,
                                enabled = bet < balance,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        AppButton(
                            text = "Jouer",
                            onClick = { startNewRound() },
                            variant = ButtonVariant.Primary,
                            size = ButtonSize.Medium,
                            enabled = balance >= bet,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Étape 2 : cartes + actions, une fois la partie lancée
                if (gameStarted || gameOver) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Croupier
                    Text(
                        text = "Croupier",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DarkTextPrimary,
                        modifier = Modifier.align(Alignment.Start)
                    )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dealerCards.forEachIndexed { index, card ->
                        val hidden = index == 1 && !gameOver && gameStarted
                        AnimatedContent(
                            targetState = card to hidden,
                            transitionSpec = {
                                (slideInVertically { fullHeight -> -fullHeight } + fadeIn() togetherWith
                                        slideOutVertically { fullHeight -> fullHeight } + fadeOut())
                                    .using(SizeTransform(clip = false))
                            },
                            label = "dealer_card_$index"
                        ) { (value, isHidden) ->
                            CardDisplay(
                                card = value,
                                hidden = isHidden
                            )
                        }
                    }
                }

                    // Résultat du jeu (plus grand et plus marqué)
                    if (gameResult != null) {
                        Text(
                            text = gameResult!!,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DarkTextPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Zone joueur : cartes en bas à gauche, boutons à droite
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    // Cartes du joueur :
                    //  - 2 cartes principales plus grandes en bas
                    //  - toutes les cartes supplémentaires, plus petites, au-dessus
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        val mainCards = playerCards.take(2)
                        val extraCards = if (playerCards.size > 2) playerCards.drop(2) else emptyList()

                        // Ligne du haut : cartes supplémentaires (petites)
                        if (extraCards.isNotEmpty()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                extraCards.forEachIndexed { index, extra ->
                                    AnimatedContent(
                                        targetState = extra,
                                        transitionSpec = {
                                            (slideInVertically { fullHeight -> fullHeight } + fadeIn() togetherWith
                                                    slideOutVertically { fullHeight -> -fullHeight } + fadeOut())
                                                .using(SizeTransform(clip = false))
                                        },
                                        label = "player_card_extra_$index"
                                    ) { value ->
                                        CardDisplay(
                                            card = value,
                                            hidden = !gameStarted,
                                            isSmall = true
                                        )
                                    }
                                }
                            }
                        }

                        // Ligne du bas : deux grandes cartes principales
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AnimatedContent(
                                targetState = mainCards.getOrNull(0),
                                transitionSpec = {
                                    (slideInVertically { fullHeight -> fullHeight } + fadeIn() togetherWith
                                            slideOutVertically { fullHeight -> -fullHeight } + fadeOut())
                                        .using(SizeTransform(clip = false))
                                },
                                label = "player_card_0"
                            ) { value ->
                                CardDisplay(
                                    card = value ?: 0,
                                    hidden = !gameStarted || value == null
                                )
                            }
                            AnimatedContent(
                                targetState = mainCards.getOrNull(1),
                                transitionSpec = {
                                    (slideInVertically { fullHeight -> fullHeight } + fadeIn() togetherWith
                                            slideOutVertically { fullHeight -> -fullHeight } + fadeOut())
                                        .using(SizeTransform(clip = false))
                                },
                                label = "player_card_1"
                            ) { value ->
                                CardDisplay(
                                    card = value ?: 0,
                                    hidden = !gameStarted || value == null
                                )
                            }
                        }
                    }

                        // Boutons à droite (comme avant : seulement Tirer / Garder pendant la partie)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (gameStarted) {
                                AppButton(
                                    text = "Tirer",
                                    onClick = { hit() },
                                    variant = ButtonVariant.Primary,
                                    size = ButtonSize.Medium,
                                    enabled = !gameOver,
                                    modifier = Modifier.width(140.dp)
                                )
                                AppButton(
                                    text = "Garder",
                                    onClick = { stand() },
                                    variant = ButtonVariant.Outline,
                                    size = ButtonSize.Medium,
                                    enabled = !gameOver,
                                    modifier = Modifier.width(140.dp)
                                )
                            }
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
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun CardDisplay(
    card: Int,
    hidden: Boolean = false,
    modifier: Modifier = Modifier,
    isSmall: Boolean = false
) {
    Box(
        modifier = modifier
            .size(
                width = if (isSmall) 50.dp else 72.dp,
                height = if (isSmall) 72.dp else 100.dp
            )
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        if (hidden) {
            Text(
                text = "?",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
        } else {
            val cardValue = when (card) {
                1 -> "A"
                11 -> "J"
                12 -> "Q"
                13 -> "K"
                else -> card.toString()
            }

            // Choix pseudo-aléatoire mais stable d'un signe (suit) à partir de la valeur
            val suitSymbol = when (card % 4) {
                0 -> "♠"
                1 -> "♥"
                2 -> "♦"
                else -> "♣"
            }
            val isRedSuit = suitSymbol == "♥" || suitSymbol == "♦"
            val cardColor = if (isRedSuit) Color(0xFFE53935) else Color.Black

            val paddingH = if (isSmall) 10.dp else 14.dp
            val paddingV = if (isSmall) 8.dp else 12.dp
            val fontSize = if (isSmall) 18.sp else 18.sp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = paddingH, vertical = paddingV),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = cardValue,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Black,
                    color = cardColor
                )
                Text(
                    text = suitSymbol,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Black,
                    color = cardColor
                )
            }
        }
    }
}
