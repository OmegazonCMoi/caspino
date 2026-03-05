package com.example.mobile.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.mobile.ui.components.BetInput
import com.example.mobile.ui.components.AppHeader
import com.example.mobile.ui.components.ButtonSize
import com.example.mobile.ui.components.ButtonVariant
import com.example.mobile.ui.components.CardVariant
import com.example.mobile.ui.theme.AccentGreen
import com.example.mobile.ui.theme.DarkSurface
import com.example.mobile.ui.theme.DarkTextPrimary
import com.example.mobile.ui.theme.DarkTextSecondary
import androidx.compose.ui.platform.LocalContext
import com.example.mobile.ui.components.AppBottomBar
import com.example.mobile.ui.components.BottomBarItem
import com.example.mobile.ui.icons.AppIcons
import kotlin.random.Random

data class Match(
    val id: Int,
    val team1: String,
    val team2: String,
    val odds1: Double,
    val oddsDraw: Double,
    val odds2: Double,
    val date: String
)

@Composable
fun SportsBettingScreen(
    onBackClick: () -> Unit
) {
    var balance by BalanceState.balance
    var bet by remember { mutableStateOf(10) }
    var selectedBets by remember { mutableStateOf<Map<Int, Pair<String, Int>>>(emptyMap()) }

    val matches = remember {
        listOf(
            Match(1, "PSG", "OM", 1.5, 3.5, 6.0, "Aujourd'hui 20:00"),
            Match(2, "Real Madrid", "Barcelona", 2.1, 3.2, 3.0, "Demain 21:00"),
            Match(3, "Liverpool", "Manchester City", 2.5, 3.0, 2.8, "Demain 18:00"),
            Match(4, "Bayern Munich", "Dortmund", 1.8, 3.8, 4.5, "Après-demain 19:00"),
            Match(5, "Juventus", "Inter Milan", 2.2, 3.1, 3.2, "Après-demain 20:30")
        )
    }

    fun placeBet(matchId: Int, betType: String, amount: Int) {
        if (balance >= amount) {
            val currentBets = selectedBets.toMutableMap()
            currentBets[matchId] = Pair(betType, amount)
            selectedBets = currentBets
            balance -= amount
        }
    }

    fun removeBet(matchId: Int) {
        val currentBets = selectedBets.toMutableMap()
        val bet = currentBets.remove(matchId)
        if (bet != null) {
            balance += bet.second
        }
        selectedBets = currentBets
    }

    fun calculatePotentialWin(matchId: Int): Double {
        val bet = selectedBets[matchId] ?: return 0.0
        val match = matches.find { it.id == matchId } ?: return 0.0
        val odds = when (bet.first) {
            "1" -> match.odds1
            "N" -> match.oddsDraw
            "2" -> match.odds2
            else -> 0.0
        }
        return bet.second * odds
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
                title = "Paris sportifs",
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
        ) {
            // Solde
            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                variant = CardVariant.Elevated
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Solde",
                            fontSize = 12.sp,
                            color = DarkTextSecondary
                        )
                        Text(
                            text = "$balance",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkTextPrimary
                        )
                    }
                    if (selectedBets.isNotEmpty()) {
                        val totalPotential = selectedBets.keys.sumOf { calculatePotentialWin(it) }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Gain potentiel",
                                fontSize = 12.sp,
                                color = DarkTextSecondary
                            )
                            Text(
                                text = "${totalPotential.toInt()}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentGreen
                            )
                        }
                    }
                }
            }

            BetInput(
                bet = bet,
                onBetChange = { bet = it },
                maxBet = balance,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Liste des matchs
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(matches) { match ->
                    MatchCard(
                        match = match,
                        selectedBet = selectedBets[match.id],
                        betAmount = bet,
                        onBetClick = { betType, amount ->
                            placeBet(match.id, betType, amount)
                        },
                        onRemoveBet = { removeBet(match.id) },
                        balance = balance
                    )
                }
            }
        }
    }
}

@Composable
fun MatchCard(
    match: Match,
    selectedBet: Pair<String, Int>?,
    betAmount: Int,
    onBetClick: (String, Int) -> Unit,
    onRemoveBet: () -> Unit,
    balance: Int
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        variant = CardVariant.Outlined
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // En-tête du match
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = match.date,
                    fontSize = 12.sp,
                    color = DarkTextSecondary
                )
                if (selectedBet != null) {
                    Text(
                        text = "Mise: ${selectedBet.second}",
                        fontSize = 12.sp,
                        color = AccentGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Équipes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = match.team1,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkTextPrimary
                    )
                }
                Text(
                    text = "VS",
                    fontSize = 12.sp,
                    color = DarkTextSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = match.team2,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkTextPrimary,
                        textAlign = TextAlign.End
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Boutons de pari
            if (selectedBet == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BetButton(
                        text = "${match.team1}\n${match.odds1}",
                        onClick = { onBetClick("1", betAmount) },
                        enabled = balance >= betAmount && betAmount > 0,
                        modifier = Modifier.weight(1f)
                    )
                    BetButton(
                        text = "Nul\n${match.oddsDraw}",
                        onClick = { onBetClick("N", betAmount) },
                        enabled = balance >= betAmount && betAmount > 0,
                        modifier = Modifier.weight(1f)
                    )
                    BetButton(
                        text = "${match.team2}\n${match.odds2}",
                        onClick = { onBetClick("2", betAmount) },
                        enabled = balance >= betAmount && betAmount > 0,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                AppButton(
                    text = "Annuler le pari",
                    onClick = onRemoveBet,
                    variant = ButtonVariant.Destructive,
                    size = ButtonSize.Medium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun BetButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    AppButton(
        text = text,
        onClick = onClick,
        variant = ButtonVariant.Outline,
        size = ButtonSize.Small,
        enabled = enabled,
        modifier = modifier
    )
}
