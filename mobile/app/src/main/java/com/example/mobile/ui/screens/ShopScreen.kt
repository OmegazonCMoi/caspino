package com.example.mobile.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile.MainActivity
import com.example.mobile.R
import com.example.mobile.network.AlreadyClaimedException
import com.example.mobile.network.ApiClient
import com.example.mobile.network.AuthApi
import com.example.mobile.ui.components.AppBottomBar
import com.example.mobile.ui.components.AppButton
import com.example.mobile.ui.components.AppHeader
import com.example.mobile.ui.components.BalanceHeader
import com.example.mobile.ui.components.BottomBarItem
import com.example.mobile.ui.components.ButtonSize
import com.example.mobile.ui.components.ButtonVariant
import com.example.mobile.ui.icons.AppIcons
import com.example.mobile.ui.theme.DarkBackground
import com.example.mobile.ui.theme.DarkSurfaceVariant
import com.example.mobile.ui.theme.DarkTextPrimary
import com.example.mobile.ui.theme.DarkTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class PinosOffer(
    val label: String,
    val pinos: Int,
    val priceDisplay: String,
    val drawableResId: Int,
    val isFree: Boolean = false
)

@Composable
fun ShopScreen(
    onBackClick: () -> Unit
) {
    var balance by BalanceState.balance
    val coroutineScope = rememberCoroutineScope()
    var isClaiming by remember { mutableStateOf(false) }
    var isLoadingClaimStatus by remember { mutableStateOf(true) }
    var secondsUntilMidnight by remember { mutableLongStateOf(0L) }

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
            // On est déjà sur la boutique, pas de navigation supplémentaire
        }
    )

    LaunchedEffect(Unit) {
        AccountState.refreshFromServer()
        isLoadingClaimStatus = false
    }

    LaunchedEffect(BalanceState.hasClaimedFreeTodayState) {
        if (BalanceState.hasClaimedFreeToday()) {
            while (true) {
                val now = LocalDateTime.now()
                val midnight = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIDNIGHT)
                secondsUntilMidnight = Duration.between(now, midnight).seconds
                if (secondsUntilMidnight <= 0) {
                    BalanceState.resetDailyClaim()
                    break
                }
                delay(1000L)
            }
        }
    }

    val freeOffer = PinosOffer(
        label = "Starter",
        pinos = 500,
        priceDisplay = "Gratuit",
        drawableResId = R.drawable.little_coins,
        isFree = true
    )
    val paidOffers = listOf(
        PinosOffer("Bronze", 1_500, "0,99 €", R.drawable.medium_coins),
        PinosOffer("Silver", 5_000, "2,99 €", R.drawable.big_coins),
        PinosOffer("Gold", 15_000, "6,99 €", R.drawable.huge_coins),
        PinosOffer("Platinum", 50_000, "14,99 €", R.drawable.huge_coins)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Scaffold(
            topBar = {
                AppHeader(
                    title = "Acheter des pinos",
                    onBackClick = onBackClick
                )
            },
            bottomBar = {
                AppBottomBar(
                    items = bottomBarItems,
                    selectedIndex = 3
                )
            }
        ) { innerPadding ->
            if (isLoadingClaimStatus) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = DarkTextSecondary
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {
                    BalanceHeader(
                        amount = balance,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Text(
                        text = "La monnaie de l'app pour jouer à tous les jeux.",
                        fontSize = 14.sp,
                        color = DarkTextSecondary,
                        modifier = Modifier.padding(top = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    val hasClaimedFree = BalanceState.hasClaimedFreeToday()
                    if (hasClaimedFree) {
                        DailyBonusTimer(secondsUntilMidnight = secondsUntilMidnight)
                    } else {
                        PinosOfferCardFull(
                            offer = freeOffer,
                            enabled = !isClaiming,
                            onBuyClick = {
                                if (!BalanceState.hasClaimedFreeToday() && !isClaiming) {
                                    coroutineScope.launch {
                                        isClaiming = true
                                        val result = AuthApi.claimDailyBonus()
                                        result.onSuccess { newBalance ->
                                            BalanceState.balance.intValue = newBalance
                                            BalanceState.markFreeClaimedToday()
                                            ApiClient.saveBalance(newBalance)
                                        }.onFailure { error ->
                                            if (error is AlreadyClaimedException) {
                                                BalanceState.markFreeClaimedToday()
                                            }
                                        }
                                        isClaiming = false
                                    }
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    paidOffers.chunked(2).forEach { rowOffers ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowOffers.forEach { offer ->
                                PinosOfferCardHalf(
                                    offer = offer,
                                    onBuyClick = {
                                        // TODO: IAP
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowOffers.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun DailyBonusTimer(secondsUntilMidnight: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceVariant)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val hours = secondsUntilMidnight / 3600
        val minutes = (secondsUntilMidnight % 3600) / 60
        val seconds = secondsUntilMidnight % 60
        Text(
            text = "Bonus quotidien dans ${hours}h ${minutes}m ${seconds}s",
            fontSize = 14.sp,
            color = DarkTextSecondary
        )
    }
}

@Composable
private fun PinosOfferCardFull(
    offer: PinosOffer,
    enabled: Boolean,
    onBuyClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceVariant)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Image(
            painter = painterResource(id = offer.drawableResId),
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            contentScale = ContentScale.Fit
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
                .height(72.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = offer.label,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = DarkTextPrimary
            )
            Text(
                text = "${offer.pinos} pinos",
                fontSize = 15.sp,
                color = DarkTextSecondary
            )
        }
        AppButton(
            text = offer.priceDisplay,
            onClick = onBuyClick,
            variant = ButtonVariant.Primary,
            size = ButtonSize.Medium,
            enabled = enabled
        )
    }
}

@Composable
private fun PinosOfferCardHalf(
    offer: PinosOffer,
    onBuyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceVariant)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(
            painter = painterResource(id = offer.drawableResId),
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            contentScale = ContentScale.Fit
        )
        Text(
            text = offer.label,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = DarkTextPrimary
        )
        Text(
            text = "${offer.pinos} pinos",
            fontSize = 13.sp,
            color = DarkTextSecondary
        )
        AppButton(
            text = offer.priceDisplay,
            onClick = onBuyClick,
            variant = ButtonVariant.Primary,
            size = ButtonSize.Medium,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
