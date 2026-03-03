package com.example.mobile.ui.screens

import android.content.Intent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile.MainActivity
import com.example.mobile.ui.components.AppButton
import com.example.mobile.ui.components.AppCard
import com.example.mobile.ui.components.AppHeader
import com.example.mobile.ui.components.ButtonSize
import com.example.mobile.ui.components.ButtonVariant
import com.example.mobile.ui.components.CardVariant
import com.example.mobile.ui.theme.DarkSurface
import com.example.mobile.ui.theme.DarkTextPrimary
import com.example.mobile.ui.theme.DarkTextSecondary
import androidx.compose.ui.platform.LocalContext
import com.example.mobile.ui.components.AppBottomBar
import com.example.mobile.ui.components.BottomBarItem
import com.example.mobile.ui.icons.AppIcons
import kotlin.random.Random

@Composable
fun PokerScreen(
    onBackClick: () -> Unit
) {
    var playerCards by remember { mutableStateOf<List<Int>>(emptyList()) }
    var communityCards by remember { mutableStateOf<List<Int>>(emptyList()) }
    var gameStarted by remember { mutableStateOf(false) }
    var gamePhase by remember { mutableStateOf("") } // "preflop", "flop", "turn", "river"
    var pot by remember { mutableStateOf(0) }
    var playerBet by remember { mutableStateOf(0) }

    fun drawCard(): Int {
        return Random.nextInt(1, 14) // 1-13 (As à Roi)
    }

    fun dealCards() {
        playerCards = listOf(drawCard(), drawCard())
        communityCards = emptyList()
        gameStarted = true
        gamePhase = "preflop"
        pot = 100
        playerBet = 0
    }

    fun dealFlop() {
        if (gamePhase == "preflop") {
            communityCards = listOf(drawCard(), drawCard(), drawCard())
            gamePhase = "flop"
        }
    }

    fun dealTurn() {
        if (gamePhase == "flop") {
            communityCards = communityCards + drawCard()
            gamePhase = "turn"
        }
    }

    fun dealRiver() {
        if (gamePhase == "turn") {
            communityCards = communityCards + drawCard()
            gamePhase = "river"
        }
    }

    fun bet(amount: Int) {
        playerBet += amount
        pot += amount
    }

    fun fold() {
        gameStarted = false
        gamePhase = ""
        playerCards = emptyList()
        communityCards = emptyList()
        pot = 0
        playerBet = 0
    }

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

    Scaffold(
        topBar = {
            AppHeader(
                title = "Poker",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Pot
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                variant = CardVariant.Elevated
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Pot",
                        fontSize = 14.sp,
                        color = DarkTextSecondary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = "$pot",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkTextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }

            // Phase du jeu
            if (gameStarted) {
                Text(
                    text = when (gamePhase) {
                        "preflop" -> "Pré-flop"
                        "flop" -> "Flop"
                        "turn" -> "Turn"
                        "river" -> "River"
                        else -> ""
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkTextSecondary
                )
            }

            // Cartes communautaires
            if (gameStarted && communityCards.isNotEmpty()) {
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    variant = CardVariant.Outlined
                ) {
                    Text(
                        text = "Cartes communautaires",
                        fontSize = 14.sp,
                        color = DarkTextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        communityCards.forEach { card ->
                            PokerCardDisplay(card = card)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Cartes du joueur
            if (gameStarted) {
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    variant = CardVariant.Outlined
                ) {
                    Text(
                        text = "Vos cartes",
                        fontSize = 14.sp,
                        color = DarkTextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        playerCards.forEach { card ->
                            PokerCardDisplay(card = card)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Boutons d'action
            if (!gameStarted) {
                AppButton(
                    text = "Distribuer",
                    onClick = { dealCards() },
                    variant = ButtonVariant.Primary,
                    size = ButtonSize.Large,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (gamePhase) {
                            "preflop" -> {
                                AppButton(
                                    text = "Miser 10",
                                    onClick = { bet(10) },
                                    variant = ButtonVariant.Primary,
                                    size = ButtonSize.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                AppButton(
                                    text = "Miser 50",
                                    onClick = { bet(50) },
                                    variant = ButtonVariant.Primary,
                                    size = ButtonSize.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            "flop" -> {
                                AppButton(
                                    text = "Flop",
                                    onClick = { dealFlop() },
                                    variant = ButtonVariant.Secondary,
                                    size = ButtonSize.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            "turn" -> {
                                AppButton(
                                    text = "Turn",
                                    onClick = { dealTurn() },
                                    variant = ButtonVariant.Secondary,
                                    size = ButtonSize.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            "river" -> {
                                AppButton(
                                    text = "Nouvelle partie",
                                    onClick = { fold() },
                                    variant = ButtonVariant.Secondary,
                                    size = ButtonSize.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        AppButton(
                            text = "Se coucher",
                            onClick = { fold() },
                            variant = ButtonVariant.Destructive,
                            size = ButtonSize.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun PokerCardDisplay(
    card: Int,
    modifier: Modifier = Modifier
) {
    val cardValue = when (card) {
        1 -> "A"
        11 -> "J"
        12 -> "Q"
        13 -> "K"
        else -> card.toString()
    }

    Box(
        modifier = modifier
            .size(width = 50.dp, height = 70.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = cardValue,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = DarkTextPrimary
        )
    }
}
