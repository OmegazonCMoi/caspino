package com.example.mobile.ui.screens

import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile.MainActivity
import com.example.mobile.ui.components.AppButton
import com.example.mobile.ui.components.BetInput
import com.example.mobile.ui.components.AppHeader
import com.example.mobile.ui.components.BalanceHeader
import com.example.mobile.ui.components.AppBottomBar
import com.example.mobile.ui.components.BottomBarItem
import com.example.mobile.ui.components.ButtonSize
import com.example.mobile.ui.components.ButtonVariant
import com.example.mobile.ui.theme.AccentGreen
import com.example.mobile.ui.theme.AccentOrange
import com.example.mobile.ui.theme.AccentRed
import com.example.mobile.ui.theme.DarkSurface
import com.example.mobile.ui.theme.DarkTextPrimary
import com.example.mobile.ui.theme.DarkTextSecondary
import com.example.mobile.ui.theme.DarkTextTertiary
import com.example.mobile.ui.icons.AppIcons
import com.example.mobile.network.BlackjackApi
import com.example.mobile.network.BlackjackBetResult
import com.example.mobile.network.BlackjackCard
import com.example.mobile.network.BlackjackGameState
import com.example.mobile.network.BlackjackHand
import com.example.mobile.network.BlackjackHandResult
import com.example.mobile.network.BlackjackPlayerHand
import com.example.mobile.ui.components.ConfettiOverlay
import io.github.vinceglb.confettikit.core.Party
import io.github.vinceglb.confettikit.core.Position
import io.github.vinceglb.confettikit.core.emitter.Emitter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@Composable
fun BlackjackScreen(
    onBackClick: () -> Unit
) {
    var balance by BalanceState.balance
    var bet by remember { mutableStateOf(10) }

    var gameState by remember { mutableStateOf<BlackjackGameState?>(null) }
    var betResult by remember { mutableStateOf<BlackjackBetResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var confettiParties by remember { mutableStateOf<List<Party>>(emptyList()) }

    val isPlaying = gameState != null && betResult == null
    val isResolved = betResult != null

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

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

    DisposableEffect(Unit) {
        BlackjackApi.onGameState = { state ->
            coroutineScope.launch {
                gameState = state
                isLoading = false
                errorMessage = null
            }
        }
        BlackjackApi.onBetResult = { result ->
            coroutineScope.launch {
                delay(1500)
                betResult = result
                balance = result.balance
                isLoading = false
                errorMessage = null

                val totalBet = result.hands.sumOf { hand -> hand.bet }
                val netGain = result.gains - totalBet
                if (netGain > 0) {
                    val hasBlackjack = result.hands.any { hand -> hand.status == "BLACKJACK" }
                    val hasDouble = result.hands.any { hand -> hand.bet > totalBet / result.hands.size }
                    val isSplit = result.hands.size > 1
                    val winningHands = result.hands.count { hand -> hand.gains > hand.bet }
                    val losingHands = result.hands.count { hand -> hand.gains == 0 }

                    val isBigWin = hasBlackjack || hasDouble || (isSplit && winningHands == result.hands.size)
                    val isPartialWin = isSplit && winningHands > losingHands && losingHands > 0

                    val durationSec = if (isBigWin) 5.0 else 2.5
                    val particlesPerSec = if (isBigWin) 50 else 30

                    if (isBigWin || isPartialWin || !isSplit) {
                        confettiParties = listOf(
                            Party(
                                angle = -45, spread = 45,
                                position = Position.Relative(0.0, 0.26),
                                emitter = Emitter(duration = durationSec.seconds).perSecond(particlesPerSec),
                                fadeOutEnabled = true
                            ),
                            Party(
                                angle = -135, spread = 45,
                                position = Position.Relative(1.0, 0.26),
                                emitter = Emitter(duration = durationSec.seconds).perSecond(particlesPerSec),
                                fadeOutEnabled = true
                            )
                        )
                    }
                }
            }
        }
        BlackjackApi.onError = { message ->
            coroutineScope.launch {
                errorMessage = message
                isLoading = false
            }
        }

        onDispose {
            BlackjackApi.onGameState = null
            BlackjackApi.onBetResult = null
            BlackjackApi.onError = null
            BlackjackApi.disconnect()
        }
    }

    fun placeBet() {
        if (bet <= 0 || balance < bet || !AccountState.isLoggedIn) return
        isLoading = true
        errorMessage = null
        gameState = null
        betResult = null
        coroutineScope.launch {
            BlackjackApi.placeBet(bet)
        }
    }

    fun newRound() {
        gameState = null
        betResult = null
        errorMessage = null
        confettiParties = emptyList()
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
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BalanceHeader(
                    amount = balance,
                    modifier = Modifier
                        .padding(top = 40.dp)
                        .align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Error message
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        fontSize = 14.sp,
                        color = AccentRed,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Betting phase
                if (!isPlaying && !isResolved) {
                    BettingSection(
                        bet = bet,
                        balance = balance,
                        isLoading = isLoading,
                        isLoggedIn = AccountState.isLoggedIn,
                        onBetChange = { bet = it },
                        onPlay = { placeBet() }
                    )
                }

                // Game in progress
                if (isPlaying) {
                    val state = gameState!!

                    // Deck info
                    DeckIndicator(
                        remaining = state.deckRemaining,
                        total = state.deckTotal,
                        reshuffled = state.reshuffled
                    )

                    // Dealer section
                    DealerSection(
                        dealerUpCard = state.dealerUpCard,
                        dealerFullHand = null,
                        dealerValue = null
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Player hands
                    state.hands.forEachIndexed { handIndex, hand ->
                        val isActive = handIndex == state.activeHandIndex
                        PlayerHandSection(
                            hand = hand,
                            handIndex = handIndex,
                            totalHands = state.hands.size,
                            isActive = isActive
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Actions
                    val activeHand = state.hands.getOrNull(state.activeHandIndex)
                    if (activeHand != null && activeHand.status == "PLAYING") {
                        ActionButtons(
                            hand = activeHand,
                            canInsure = state.canInsure,
                            onHit = { BlackjackApi.hit() },
                            onStand = { BlackjackApi.stand() },
                            onDoubleDown = { BlackjackApi.doubleDown() },
                            onSplit = { BlackjackApi.split() },
                            onInsurance = { BlackjackApi.insurance() }
                        )
                    }
                }

                // Result phase
                if (isResolved) {
                    val result = betResult!!

                    DealerSection(
                        dealerUpCard = null,
                        dealerFullHand = result.dealerHand,
                        dealerValue = result.dealerValue
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Player hands with cards + results
                    result.hands.forEachIndexed { handIndex, handResult ->
                        val playerHand = result.playerHands.getOrNull(handIndex)
                        HandResultSection(
                            handResult = handResult,
                            playerHand = playerHand,
                            handIndex = handIndex,
                            totalHands = result.hands.size
                        )
                    }

                    // Total result
                    val totalBet = result.hands.sumOf { it.bet }
                    val netGain = result.gains - totalBet
                    val resultText = when {
                        netGain > 0 -> "+$netGain"
                        netGain < 0 -> "$netGain"
                        else -> "0"
                    }
                    val resultColor = when {
                        netGain > 0 -> AccentGreen
                        netGain < 0 -> AccentRed
                        else -> DarkTextSecondary
                    }
                    val resultLabel = when {
                        netGain > 0 -> "Gagné !"
                        netGain < 0 -> "Perdu"
                        else -> "Égalité"
                    }

                    if (result.insuranceGain > 0) {
                        Text(
                            text = "Assurance : +${result.insuranceGain}",
                            fontSize = 14.sp,
                            color = AccentGreen,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = resultLabel,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = resultColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = resultText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = resultColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    AppButton(
                        text = "Nouvelle partie",
                        onClick = { newRound() },
                        variant = ButtonVariant.Secondary,
                        size = ButtonSize.Medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            ConfettiOverlay(
                parties = confettiParties,
                onFinished = { confettiParties = emptyList() },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun DeckIndicator(
    remaining: Int,
    total: Int,
    reshuffled: Boolean
) {
    val percentage = if (total > 0) remaining.toFloat() / total else 0f
    val barColor = when {
        percentage < 0.3f -> AccentRed
        percentage < 0.5f -> AccentOrange
        else -> AccentGreen
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (reshuffled) "Pioche (remélangée)" else "Pioche",
                fontSize = 12.sp,
                color = if (reshuffled) AccentOrange else DarkTextTertiary
            )
            Text(
                text = "$remaining / $total",
                fontSize = 12.sp,
                color = DarkTextTertiary
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(DarkSurface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
            )
        }
    }
}

@Composable
private fun BettingSection(
    bet: Int,
    balance: Int,
    isLoading: Boolean,
    isLoggedIn: Boolean,
    onBetChange: (Int) -> Unit,
    onPlay: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BetInput(
            bet = bet,
            onBetChange = onBetChange,
            maxBet = balance,
            enabled = !isLoading
        )

        AppButton(
            text = "Jouer",
            onClick = onPlay,
            variant = ButtonVariant.Primary,
            size = ButtonSize.Medium,
            enabled = balance >= bet && isLoggedIn && !isLoading,
            loading = isLoading,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DealerSection(
    dealerUpCard: BlackjackCard?,
    dealerFullHand: List<BlackjackCard>?,
    dealerValue: Int?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Croupier",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = DarkTextPrimary
            )
            if (dealerValue != null) {
                Text(
                    text = "($dealerValue)",
                    fontSize = 14.sp,
                    color = DarkTextSecondary
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            if (dealerFullHand != null) {
                dealerFullHand.forEachIndexed { cardIndex, card ->
                    DealAnimatedCard(
                        card = card,
                        dealDelayMs = cardIndex * 150L,
                        key = "dealer_result_$cardIndex"
                    )
                }
            } else if (dealerUpCard != null) {
                DealAnimatedCard(
                    card = dealerUpCard,
                    dealDelayMs = 0L,
                    key = "dealer_up"
                )
                DealAnimatedCard(
                    card = null,
                    dealDelayMs = 200L,
                    key = "dealer_hidden"
                )
            }
        }
    }
}

@Composable
private fun PlayerHandSection(
    hand: BlackjackHand,
    handIndex: Int,
    totalHands: Int,
    isActive: Boolean
) {
    val borderColor = if (isActive) AccentOrange else Color.Transparent
    val label = if (totalHands > 1) "Main ${handIndex + 1}" else "Vous"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isActive && totalHands > 1) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isActive) AccentOrange else DarkTextPrimary
            )
            Text(
                text = "(${hand.value})",
                fontSize = 14.sp,
                color = DarkTextSecondary
            )
            if (hand.isDoubled) {
                Text(
                    text = "x2",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentOrange
                )
            }
            Text(
                text = "${hand.bet} P",
                fontSize = 12.sp,
                color = DarkTextTertiary
            )
            val statusText = when (hand.status) {
                "BUSTED" -> "Bust"
                "STOOD" -> "Stand"
                "BLACKJACK" -> "Blackjack!"
                else -> null
            }
            if (statusText != null) {
                val statusColor = when (hand.status) {
                    "BUSTED" -> AccentRed
                    "BLACKJACK" -> AccentGreen
                    else -> DarkTextSecondary
                }
                Text(
                    text = statusText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            hand.cards.forEachIndexed { cardIndex, card ->
                DealAnimatedCard(
                    card = card,
                    dealDelayMs = cardIndex * 200L,
                    key = "hand_${handIndex}_card_${card.suit}_${card.rank}"
                )
            }
        }
    }
}

@Composable
private fun HandResultSection(
    handResult: BlackjackHandResult,
    playerHand: BlackjackPlayerHand?,
    handIndex: Int,
    totalHands: Int
) {
    val label = if (totalHands > 1) "Main ${handIndex + 1}" else "Vous"
    val netGain = handResult.gains - handResult.bet
    val resultColor = when {
        netGain > 0 -> AccentGreen
        netGain < 0 -> AccentRed
        else -> DarkTextSecondary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkTextPrimary
                )
                Text(
                    text = "(${handResult.playerValue})",
                    fontSize = 12.sp,
                    color = DarkTextSecondary
                )
                if (playerHand?.isDoubled == true) {
                    Text(
                        text = "x2",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentOrange
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                val statusLabel = when (handResult.status) {
                    "BUSTED" -> "Bust"
                    "BLACKJACK" -> "Blackjack!"
                    else -> if (netGain > 0) "Gagné" else if (netGain < 0) "Perdu" else "Égalité"
                }
                Text(
                    text = statusLabel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = resultColor
                )
                Text(
                    text = if (netGain >= 0) "+${handResult.gains}" else "-${handResult.bet}",
                    fontSize = 12.sp,
                    color = resultColor
                )
            }
        }

        if (playerHand != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                playerHand.cards.forEachIndexed { cardIndex, card ->
                    DealAnimatedCard(
                        card = card,
                        dealDelayMs = cardIndex * 100L,
                        key = "result_hand_${handIndex}_${card.suit}_${card.rank}"
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButtons(
    hand: BlackjackHand,
    canInsure: Boolean,
    onHit: () -> Unit,
    onStand: () -> Unit,
    onDoubleDown: () -> Unit,
    onSplit: () -> Unit,
    onInsurance: () -> Unit
) {
    val canDoubleDown = hand.cards.size == 2
    val canSplit = hand.cards.size == 2 && canSplitHand(hand)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Primary actions
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            AppButton(
                text = "Tirer",
                onClick = onHit,
                variant = ButtonVariant.Primary,
                size = ButtonSize.Medium,
                modifier = Modifier.weight(1f)
            )
            AppButton(
                text = "Rester",
                onClick = onStand,
                variant = ButtonVariant.Outline,
                size = ButtonSize.Medium,
                modifier = Modifier.weight(1f)
            )
        }

        // Secondary actions
        if (canDoubleDown || canSplit || canInsure) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (canDoubleDown) {
                    AppButton(
                        text = "Doubler",
                        onClick = onDoubleDown,
                        variant = ButtonVariant.Outline,
                        size = ButtonSize.Small,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (canSplit) {
                    AppButton(
                        text = "Séparer",
                        onClick = onSplit,
                        variant = ButtonVariant.Outline,
                        size = ButtonSize.Small,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (canInsure) {
                    AppButton(
                        text = "Assurance",
                        onClick = onInsurance,
                        variant = ButtonVariant.Outline,
                        size = ButtonSize.Small,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private fun canSplitHand(hand: BlackjackHand): Boolean {
    if (hand.cards.size != 2) return false
    val firstCard = hand.cards[0]
    val secondCard = hand.cards[1]
    return cardNumericValue(firstCard) == cardNumericValue(secondCard)
}

private fun cardNumericValue(card: BlackjackCard): Int {
    return when (card.rank) {
        "A" -> 11
        "K", "Q", "J" -> 10
        else -> card.rank.toIntOrNull() ?: 0
    }
}

@Composable
private fun DealAnimatedCard(
    card: BlackjackCard?,
    dealDelayMs: Long,
    key: String,
    isSmall: Boolean = false
) {
    val scale = remember(key) { Animatable(0f) }
    val offsetY = remember(key) { Animatable(-80f) }
    val alpha = remember(key) { Animatable(0f) }

    LaunchedEffect(key) {
        delay(dealDelayMs)
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        launch {
            offsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = spring(stiffness = Spring.StiffnessLow)
            )
        }
    }

    BlackjackCardDisplay(
        card = card,
        isSmall = isSmall,
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
            }
            .offset { IntOffset(0, offsetY.value.toInt()) }
    )
}

@Composable
fun BlackjackCardDisplay(
    card: BlackjackCard?,
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
        if (card == null) {
            Text(
                text = "?",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
        } else {
            val suitSymbol = when (card.suit) {
                "hearts" -> "\u2665"
                "diamonds" -> "\u2666"
                "clubs" -> "\u2663"
                "spades" -> "\u2660"
                else -> "?"
            }
            val isRedSuit = card.suit == "hearts" || card.suit == "diamonds"
            val cardColor = if (isRedSuit) Color(0xFFE53935) else Color.Black

            val paddingH = if (isSmall) 10.dp else 14.dp
            val paddingV = if (isSmall) 8.dp else 12.dp
            val fontSize = 18.sp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = paddingH, vertical = paddingV),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = card.rank,
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
