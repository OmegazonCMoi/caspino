package com.example.mobile.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile.R
import com.example.mobile.ui.components.AppBottomBar
import com.example.mobile.ui.components.AppButton
import com.example.mobile.ui.components.BottomBarItem
import com.example.mobile.ui.components.ButtonSize
import com.example.mobile.ui.components.ButtonVariant
import com.example.mobile.ui.icons.AppIcons
import com.example.mobile.ui.theme.DarkTextPrimary
import com.example.mobile.ui.theme.MobileTheme
import android.content.Context
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.example.mobile.RouletteActivity

@Composable
fun HomeScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    val context = LocalContext.current
    
    val bottomBarItems = listOf(
        BottomBarItem(
            icon = AppIcons.Home,
            selectedIcon = AppIcons.HomeFilled,
            onClick = { selectedTab = 0 }
        ),
        BottomBarItem(
            icon = AppIcons.Search,
            selectedIcon = AppIcons.SearchFilled,
            onClick = { selectedTab = 1 }
        ),
        BottomBarItem(
            icon = AppIcons.Profile,
            selectedIcon = AppIcons.ProfileFilled,
            onClick = { selectedTab = 2 }
        ),
        BottomBarItem(
            icon = AppIcons.Settings,
            selectedIcon = AppIcons.SettingsFilled,
            onClick = { selectedTab = 3 }
        )
    )
    
    val games = listOf(
        "Blackjack",
        "Poker",
        "Roulette",
        "Machine à sous",
        "Paris sportifs"
    )
    
    fun navigateToRoulette() {
        val intent = Intent(context, RouletteActivity::class.java)
        context.startActivity(intent)
    }
    
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
                .padding(start = 24.dp, top = 48.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Emoji slot machine avec animation
            SlotMachineEmoji(
                modifier = Modifier
                    .size(100.dp)
                    .padding(bottom = 32.dp)
            )
            
            // Titre avec animation
            AnimatedText(
                text = "À quel voulez-vous jouer ?",
                modifier = Modifier.padding(bottom = 48.dp),
                delay = 200
            )
            
            // Liste de boutons avec animations décalées
            games.forEachIndexed { index, game ->
                AnimatedButton(
                    text = game,
                    onClick = { 
                        if (game == "Roulette") {
                            navigateToRoulette()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    delay = 400 + (index * 100)
                )
            }
        }
    }
}

@Composable
fun SlotMachineEmoji(modifier: Modifier = Modifier) {
    var scale by remember { mutableStateOf(0f) }
    var alpha by remember { mutableStateOf(0f) }
    
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 300f
        ),
        label = "emoji_scale"
    )
    
    val animatedAlpha by animateFloatAsState(
        targetValue = alpha,
        animationSpec = tween(600),
        label = "emoji_alpha"
    )
    
    LaunchedEffect(Unit) {
        scale = 1f
        alpha = 1f
    }
    
    Box(
        modifier = modifier
            .scale(animatedScale)
            .alpha(animatedAlpha),
        contentAlignment = Alignment.Center
    ) {
        // Utilisation de l'image de slot machine depuis le dossier emojis
        Image(
            painter = painterResource(id = R.drawable.slot_machine),
            contentDescription = "Slot Machine",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun AnimatedText(
    text: String,
    modifier: Modifier = Modifier,
    delay: Int = 0
) {
    var translateY by remember { mutableStateOf(50f) }
    var alpha by remember { mutableStateOf(0f) }
    
    val animatedTranslateY by animateFloatAsState(
        targetValue = translateY,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 200f
        ),
        label = "text_translate"
    )
    
    val animatedAlpha by animateFloatAsState(
        targetValue = alpha,
        animationSpec = tween(600),
        label = "text_alpha"
    )
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        translateY = 0f
        alpha = 1f
    }
    
    Text(
        text = text,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = DarkTextPrimary,
        textAlign = TextAlign.Center,
        modifier = modifier
            .alpha(animatedAlpha)
            .offset(y = animatedTranslateY.dp)
    )
}

@Composable
fun AnimatedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    delay: Int = 0
) {
    var translateX by remember { mutableStateOf(-100f) }
    var alpha by remember { mutableStateOf(0f) }
    var scale by remember { mutableStateOf(0.8f) }
    
    val animatedTranslateX by animateFloatAsState(
        targetValue = translateX,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 250f
        ),
        label = "button_translate"
    )
    
    val animatedAlpha by animateFloatAsState(
        targetValue = alpha,
        animationSpec = tween(600),
        label = "button_alpha"
    )
    
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = 300f
        ),
        label = "button_scale"
    )
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        translateX = 0f
        alpha = 1f
        scale = 1f
    }
    
    Box(
        modifier = modifier
            .alpha(animatedAlpha)
            .scale(animatedScale)
            .offset(x = animatedTranslateX.dp)
    ) {
        AppButton(
            text = text,
            onClick = onClick,
            variant = ButtonVariant.Primary,
            size = ButtonSize.Large,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

