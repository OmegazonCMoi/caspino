package com.example.mobile.ui.screens

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile.R
import com.example.mobile.ui.components.AppButton
import com.example.mobile.ui.components.AppHeader
import com.example.mobile.ui.components.BalanceHeader
import com.example.mobile.ui.components.ButtonSize
import com.example.mobile.ui.components.ButtonVariant
import com.example.mobile.ui.theme.DarkBackground
import com.example.mobile.ui.theme.DarkSurfaceVariant
import com.example.mobile.ui.theme.DarkTextPrimary
import com.example.mobile.ui.theme.DarkTextSecondary
import com.example.mobile.network.WalletApi
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()
    var balance by BalanceState.balance
    var claimingBonus by remember { mutableStateOf(false) }
    var claimError by remember { mutableStateOf<String?>(null) }
    var hasClaimedFreeToday by remember { mutableStateOf(false) }

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
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header pleine largeur, aligné comme sur les autres écrans
            AppHeader(
                title = "Acheter des pinos",
                onBackClick = onBackClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Contenu scrollable avec padding horizontal
            Column(
                modifier = Modifier
                    .fillMaxSize()
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

                // Offre gratuite : toute la largeur
                PinosOfferCardFull(
                    offer = freeOffer,
                    enabled = !hasClaimedFreeToday && !claimingBonus && AccountState.isLoggedIn,
                    onBuyClick = {
                        claimError = null
                        claimingBonus = true
                        scope.launch {
                            WalletApi.claimDailyBonus()
                                .onSuccess { resp ->
                                    BalanceState.balance.intValue = resp.balance
                                    hasClaimedFreeToday = true
                                }
                                .onFailure { e ->
                                    claimError = e.message
                                    if (e.message?.contains("déjà") == true) {
                                        hasClaimedFreeToday = true
                                    }
                                }
                            claimingBonus = false
                        }
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Offres payantes : 2 par 2
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
            text = if (enabled) offer.priceDisplay else "Déjà récupéré",
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
