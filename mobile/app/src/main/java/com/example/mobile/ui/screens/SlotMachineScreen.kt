package com.example.mobile.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile.MainActivity
import com.example.mobile.R
import com.example.mobile.ui.components.AppButton
import com.example.mobile.ui.components.AppCard
import com.example.mobile.ui.components.BetInput
import com.example.mobile.ui.components.AppHeader
import com.example.mobile.ui.components.BalanceHeader
import com.example.mobile.ui.components.AppBottomBar
import com.example.mobile.ui.components.BottomBarItem
import com.example.mobile.ui.components.ButtonSize
import com.example.mobile.ui.components.ButtonVariant
import com.example.mobile.ui.icons.AppIcons
import com.example.mobile.ui.theme.DarkSurfaceVariant
import com.example.mobile.ui.theme.DarkTextPrimary
import com.example.mobile.ui.theme.DarkTextSecondary
import android.media.MediaPlayer
import com.example.mobile.network.SlotsApi
import com.example.mobile.network.SlotSpinResponse
import com.example.mobile.ui.components.ConfettiOverlay
import io.github.vinceglb.confettikit.core.Party
import io.github.vinceglb.confettikit.core.Position
import io.github.vinceglb.confettikit.core.emitter.Emitter
import kotlin.random.Random
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun SlotMachineScreen(
    onBackClick: () -> Unit
) {
    var reel1 by remember { mutableStateOf(0) }
    var reel2 by remember { mutableStateOf(0) }
    var reel3 by remember { mutableStateOf(0) }
    var isSpinning by remember { mutableStateOf(false) }
    var balance by BalanceState.balance
    var bet by remember { mutableStateOf(10) }
    var lastWin by remember { mutableStateOf(0) }
    var lastResultDelta by remember { mutableStateOf<Int?>(null) }
    var spinTrigger by remember { mutableStateOf(0) }
    var confettiParties by remember { mutableStateOf<List<Party>>(emptyList()) }

    val symbolDrawables = listOf(
        R.drawable.cherries,
        R.drawable.lemon,
        R.drawable.tanguerine,
        R.drawable.grapes,
        R.drawable.bell,
        R.drawable.star,
        R.drawable.digit_seven
    )
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val winMediaPlayer = remember {
        MediaPlayer.create(context, R.raw.confetti_sound)
    }
    val spinStartMediaPlayer = remember {
        MediaPlayer.create(context, R.raw.slotmachine_start)
    }
    val spinLoopMediaPlayer = remember {
        MediaPlayer.create(context, R.raw.slotmachine_spinning).apply {
            isLooping = true
        }
    }
    val spinStopMediaPlayer = remember {
        MediaPlayer.create(context, R.raw.slotmachine_stop)
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                winMediaPlayer.release()
            } catch (_: Exception) { }
            try {
                spinStartMediaPlayer.release()
            } catch (_: Exception) { }
            try {
                spinLoopMediaPlayer.release()
            } catch (_: Exception) { }
            try {
                spinStopMediaPlayer.release()
            } catch (_: Exception) { }
        }
    }
    
    var spinError by remember { mutableStateOf<String?>(null) }

    // Bottom bar items (navigation principale)
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
    
    // Appel API en parallèle de l'animation, résultat appliqué une fois les rouleaux arrêtés
    LaunchedEffect(spinTrigger) {
        if (spinTrigger == 0) return@LaunchedEffect

        isSpinning = true
        spinError = null
        lastResultDelta = null

        try { spinStartMediaPlayer.seekTo(0); spinStartMediaPlayer.start() } catch (_: Exception) { }
        try { spinLoopMediaPlayer.seekTo(0); spinLoopMediaPlayer.start() } catch (_: Exception) { }

        val apiCall = async { SlotsApi.spin(bet) }

        val stepDelay = 80L
        val stepsReel1 = 20
        val stepsReel2 = 35
        val stepsReel3 = 50

        // Animation aléatoire pendant que l'API répond
        for (animationStep in 0 until stepsReel1 - 1) {
            reel1 = Random.nextInt(symbolDrawables.size)
            reel2 = Random.nextInt(symbolDrawables.size)
            reel3 = Random.nextInt(symbolDrawables.size)
            delay(stepDelay)
        }

        val apiResult = apiCall.await()

        val serverResult = apiResult.getOrNull()
        // Symboles serveur : 1-7 → indices mobile 0-6
        val target1 = serverResult?.result?.getOrNull(0)?.minus(1)?.coerceIn(0, symbolDrawables.size - 1) ?: Random.nextInt(symbolDrawables.size)
        val target2 = serverResult?.result?.getOrNull(1)?.minus(1)?.coerceIn(0, symbolDrawables.size - 1) ?: Random.nextInt(symbolDrawables.size)
        val target3 = serverResult?.result?.getOrNull(2)?.minus(1)?.coerceIn(0, symbolDrawables.size - 1) ?: Random.nextInt(symbolDrawables.size)

        reel1 = target1
        try { spinStopMediaPlayer.seekTo(0); spinStopMediaPlayer.start() } catch (_: Exception) { }

        for (animationStep in stepsReel1 until stepsReel2 - 1) {
            reel2 = Random.nextInt(symbolDrawables.size)
            reel3 = Random.nextInt(symbolDrawables.size)
            delay(stepDelay)
        }

        reel2 = target2
        try { spinStopMediaPlayer.seekTo(0); spinStopMediaPlayer.start() } catch (_: Exception) { }

        for (animationStep in stepsReel2 until stepsReel3 - 1) {
            reel3 = Random.nextInt(symbolDrawables.size)
            delay(stepDelay)
        }

        reel3 = target3
        try { spinStopMediaPlayer.seekTo(0); spinStopMediaPlayer.start() } catch (_: Exception) { }

        try {
            if (spinLoopMediaPlayer.isPlaying) {
                spinLoopMediaPlayer.pause()
                spinLoopMediaPlayer.seekTo(0)
            }
        } catch (_: Exception) { }

        if (serverResult != null) {
            val win = serverResult.gain
            balance = serverResult.balance
            lastWin = win
            lastResultDelta = win

            if (win > 0) {
                val isJackpot = (target1 == target2 && target2 == target3)
                val durationSec = if (isJackpot) 5.0 else 2.5
                confettiParties = listOf(
                    Party(
                        angle = -45, spread = 45,
                        position = Position.Relative(0.0, 0.26),
                        emitter = Emitter(duration = durationSec.seconds).perSecond(30),
                        fadeOutEnabled = true
                    ),
                    Party(
                        angle = -135, spread = 45,
                        position = Position.Relative(1.0, 0.26),
                        emitter = Emitter(duration = durationSec.seconds).perSecond(30),
                        fadeOutEnabled = true
                    )
                )
                try {
                    val volume = if (isJackpot) 1.0f else 0.5f
                    winMediaPlayer.setVolume(volume, volume)
                    if (isJackpot) {
                        repeat(3) {
                            winMediaPlayer.seekTo(0)
                            winMediaPlayer.start()
                            val durationMs = winMediaPlayer.duration.toLong().coerceIn(200, 2000)
                            delay(durationMs)
                        }
                    } else {
                        winMediaPlayer.seekTo(0)
                        winMediaPlayer.start()
                    }
                } catch (_: Exception) { }
            }
        } else {
            spinError = apiResult.exceptionOrNull()?.message ?: "Erreur réseau"
            lastWin = 0
        }

        isSpinning = false
    }

    fun spin() {
        if (isSpinning || balance < bet) return
        spinTrigger++
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            topBar = {
                AppHeader(
                    title = "Machine à sous",
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
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
            // Solde (composant animé réutilisable)
            BalanceHeader(
                amount = balance,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Image de la machine à sous (si disponible)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .padding(bottom = 16.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.slot_machine),
                    contentDescription = "Machine à sous",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            // Rouleaux
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 60.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SlotReel(
                    iconResId = symbolDrawables[reel1],
                    isSpinning = isSpinning,
                    modifier = Modifier.weight(1f)
                )
                SlotReel(
                    iconResId = symbolDrawables[reel2],
                    isSpinning = isSpinning,
                    modifier = Modifier.weight(1f)
                )
                SlotReel(
                    iconResId = symbolDrawables[reel3],
                    isSpinning = isSpinning,
                    modifier = Modifier.weight(1f)
                )
            }

            // Résultat du dernier spin
            if (lastResultDelta != null && !isSpinning) {
                val gain = lastResultDelta!!
                val gainColor = when {
                    gain <= 0 -> Color(0xFFEF4444)
                    gain < bet -> Color(0xFFF59E0B)
                    else -> Color(0xFF22C55E)
                }
                Text(
                    text = "+$gain",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = gainColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            BetInput(
                bet = bet,
                onBetChange = { bet = it },
                maxBet = balance,
                enabled = !isSpinning
            )

            if (spinError != null) {
                Text(
                    text = spinError ?: "",
                    fontSize = 12.sp,
                    color = Color(0xFFEF4444),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (!AccountState.isLoggedIn) {
                Text(
                    text = "Connecte-toi pour jouer",
                    fontSize = 13.sp,
                    color = Color(0xFFEF4444),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            AppButton(
                text = if (isSpinning) "En cours..." else "Jouer",
                onClick = { spin() },
                variant = ButtonVariant.Primary,
                size = ButtonSize.Large,
                enabled = !isSpinning && balance >= bet && AccountState.isLoggedIn,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Confettis au tout premier plan, par-dessus le header, le contenu et la bottom bar
        ConfettiOverlay(
            parties = confettiParties,
            onFinished = { confettiParties = emptyList() },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun SlotReel(
    iconResId: Int,
    isSpinning: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(120.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(DarkSurfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = iconResId,
            transitionSpec = {
                val duration = if (isSpinning) 320 else 260
                (
                    slideInVertically(
                        animationSpec = tween(durationMillis = duration),
                        initialOffsetY = { fullHeight -> -fullHeight }
                    ) + fadeIn() togetherWith
                        slideOutVertically(
                            animationSpec = tween(durationMillis = duration),
                            targetOffsetY = { fullHeight -> fullHeight }
                        ) + fadeOut()
                    ).using(SizeTransform(clip = false))
            },
            label = "slot_reel_symbol"
        ) { target ->
            Image(
                painter = painterResource(id = target),
                contentDescription = null,
                modifier = Modifier.size(42.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}


