package com.example.mobile.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.mobile.BlackjackActivity
import com.example.mobile.PokerActivity
import com.example.mobile.R
import com.example.mobile.RouletteActivity
import com.example.mobile.SlotMachineActivity
import com.example.mobile.SportsBettingActivity
import com.example.mobile.ui.components.AppBottomBar
import com.example.mobile.ui.components.BalanceHeader
import com.example.mobile.ui.components.BottomBarItem
import com.example.mobile.ui.icons.AppIcons
import com.example.mobile.ui.theme.DarkTextPrimary
import com.example.mobile.ui.theme.DarkTextSecondary

data class GameItem(
    val title: String,
    val iconResId: Int,
    val dateLastGame: String? = null
)

@Composable
fun HomeScreen() {

    var selectedTab by remember { mutableStateOf(0) }
    val context = LocalContext.current

    val games = listOf(
        GameItem("Blackjack", R.drawable.joker_card, "23/12/2025"),
        GameItem("Poker", R.drawable.heart_suit, "22/12/2025"),
        GameItem("Roulette", R.drawable.ferris_wheel, null),
        GameItem("Machine à sous", R.drawable.slot_machine, "20/12/2025"),
        GameItem("Paris sportifs", R.drawable.trophy, "21/12/2025")
    )

    fun navigateToGame(gameName: String) {
        val activityClass = when (gameName) {
            "Blackjack" -> BlackjackActivity::class.java
            "Poker" -> PokerActivity::class.java
            "Roulette" -> RouletteActivity::class.java
            "Machine à sous" -> SlotMachineActivity::class.java
            "Paris sportifs" -> SportsBettingActivity::class.java
            else -> return
        }
        context.startActivity(Intent(context, activityClass))
    }

    val bottomBarItems = listOf(
        BottomBarItem(AppIcons.Home, AppIcons.HomeFilled) { selectedTab = 0 },
        BottomBarItem(AppIcons.Search, AppIcons.SearchFilled) {
            context.startActivity(Intent(context, com.example.mobile.StatsActivity::class.java))
        },
        BottomBarItem(AppIcons.Profile, AppIcons.ProfileFilled) {
            context.startActivity(Intent(context, com.example.mobile.AccountActivity::class.java))
        },
        BottomBarItem(AppIcons.Cart, AppIcons.CartFilled) {
            context.startActivity(android.content.Intent(context, com.example.mobile.ShopActivity::class.java))
        }
    )

    Scaffold(
        bottomBar = {
            AppBottomBar(
                items = bottomBarItems,
                selectedIndex = selectedTab
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp)
        ) {

            // ===== SCORE HEADER (balance animée, partagée) =====
            BalanceHeader(
                amount = BalanceState.balance.intValue,
                modifier = Modifier.padding(top = 60.dp)
            )

            Spacer(Modifier.height(28.dp))

            // ===== ALL GAMES CARD =====
            AllGamesCard(
                onClick = { /* optionnel */ }
            )

            Spacer(Modifier.height(32.dp))

            // ===== SECTION LISTE =====
            Text(
                text = "All Games",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(Modifier.height(16.dp))

            games.forEach { game ->
                GameRow(
                    title = game.title,
                    iconResId = game.iconResId,
                    dateLastGame = game.dateLastGame,
                    onClick = { navigateToGame(game.title) }
                )
            }
        }
    }
}

/* ---------------------------------------------------------- */
/* ---------------------- COMPONENTS ------------------------ */
/* ---------------------------------------------------------- */

@Composable
fun AllGamesCard(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                    )
                )
            )
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.dice),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(Modifier.width(16.dp))

                Column {
                    Text(
                        "Random Game",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Jump into a random game",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun GameRow(
    title: String,
    iconResId: Int,
    dateLastGame: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Image(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(Modifier.width(12.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = DarkTextPrimary
            )
            Text(
                text = dateLastGame?.let { "Dernière partie: $it" } ?: "Jamais joué",
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = DarkTextSecondary
            )
        }
    }
}
