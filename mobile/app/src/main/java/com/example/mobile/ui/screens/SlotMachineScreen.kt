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
import com.example.mobile.ui.components.ConfettiOverlay
import io.github.vinceglb.confettikit.core.Party
import io.github.vinceglb.confettikit.core.Position
import io.github.vinceglb.confettikit.core.emitter.Emitter
import kotlin.random.Random
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
    
    fun calculateWin(r1: Int, r2: Int, r3: Int, betAmount: Int): Int {
        return when {
            // 3 symboles identiques
            r1 == r2 && r2 == r3 -> {
                when (r1) {
                    6 -> betAmount * 100 // 7️⃣ 7️⃣ 7️⃣  (jackpot max)
                    5 -> betAmount * 50  // ⭐ ⭐ ⭐
                    4 -> betAmount * 25  // 🔔 🔔 🔔
                    3 -> betAmount * 15  // 🍇 🍇 🍇
                    2 -> betAmount * 12  // 🍊 🍊 🍊
                    1 -> betAmount * 8   // 🍋 🍋 🍋
                    0 -> betAmount * 5   // 🍒 🍒 🍒
                    else -> betAmount * 10
                }
            }
            // 2 symboles identiques (peu importe lesquels)
            r1 == r2 || r2 == r3 || r1 == r3 -> betAmount * 2
            else -> 0
        }
    }

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
    
    // Gérer l'animation du spin
    LaunchedEffect(spinTrigger) {
        if (spinTrigger == 0) return@LaunchedEffect

        isSpinning = true
        // Son de démarrage, suivi instantanément du son de spin en boucle
        try {
            spinStartMediaPlayer.seekTo(0)
            spinStartMediaPlayer.start()
        } catch (_: Exception) { }
        try {
            spinLoopMediaPlayer.seekTo(0)
            spinLoopMediaPlayer.start()
        } catch (_: Exception) { }
        // La mise est déjà déduite dans spin() au clic sur "Jouer"

        // Cible finale des rouleaux
        val target1 = Random.nextInt(symbolDrawables.size)
        val target2 = Random.nextInt(symbolDrawables.size)
        val target3 = Random.nextInt(symbolDrawables.size)

        // Durée totale et arrêt progressif de chaque colonne
        val stepDelay = 80L
        val stepsReel1 = 20   // ~1.6s
        val stepsReel2 = 35   // ~2.8s
        val stepsReel3 = 50   // ~4.0s

        for (i in 0 until stepsReel3) {
            // Colonne 1 : aléatoire puis valeur finale
            when {
                i < stepsReel1 - 1 -> reel1 = Random.nextInt(symbolDrawables.size)
                i == stepsReel1 - 1 -> {
                    reel1 = target1
                    // Son d'arrêt du premier rouleau
                    try {
                        spinStopMediaPlayer.seekTo(0)
                        spinStopMediaPlayer.start()
                    } catch (_: Exception) { }
                }
                // ensuite on ne touche plus à reel1
            }

            // Colonne 2
            when {
                i < stepsReel2 - 1 -> reel2 = Random.nextInt(symbolDrawables.size)
                i == stepsReel2 - 1 -> {
                    reel2 = target2
                    // Son d'arrêt du deuxième rouleau
                    try {
                        spinStopMediaPlayer.seekTo(0)
                        spinStopMediaPlayer.start()
                    } catch (_: Exception) { }
                }
            }

            // Colonne 3
            when {
                i < stepsReel3 - 1 -> reel3 = Random.nextInt(symbolDrawables.size)
                i == stepsReel3 - 1 -> {
                    reel3 = target3
                    // Son d'arrêt du troisième rouleau
                    try {
                        spinStopMediaPlayer.seekTo(0)
                        spinStopMediaPlayer.start()
                    } catch (_: Exception) { }
                }
            }

            delay(stepDelay)
        }

        // Arrête le son de spin quand les rouleaux se sont arrêtés
        try {
            if (spinLoopMediaPlayer.isPlaying) {
                spinLoopMediaPlayer.pause()
                spinLoopMediaPlayer.seekTo(0)
            }
        } catch (_: Exception) { }
        
        // Calculer les gains et créditer les pinos (au bon moment : une fois les rouleaux arrêtés)
        val win = calculateWin(reel1, reel2, reel3, bet)
        if (win > 0) {
            BalanceState.addPinos(win)
        }
        lastWin = win

        if (win > 0) {
            val isJackpot = (reel1 == reel2 && reel2 == reel3)
            val durationSec = if (isJackpot) 5.0 else 2.5
            // Même configuration de confettis que pour les boutons de test
            confettiParties = listOf(
                Party(
                    angle = -45,
                    spread = 45,
                    position = Position.Relative(0.0, 0.26),
                    emitter = Emitter(duration = durationSec.seconds).perSecond(30),
                    fadeOutEnabled = true
                ),
                Party(
                    angle = -135,
                    spread = 45,
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
        } else {
        }

        isSpinning = false
    }

    // Utilitaire pour appliquer un résultat forcé (tests)
    fun applyForcedResult(r1: Int, r2: Int, r3: Int) {
        // on ne manipule pas isSpinning ici pour garder ça simple pour les tests
        reel1 = r1
        reel2 = r2
        reel3 = r3

        val win = calculateWin(reel1, reel2, reel3, bet)
        if (win > 0) {
            BalanceState.addPinos(win)
        }
        lastWin = win

        if (win > 0) {
            val isJackpot = (reel1 == reel2 && reel2 == reel3)
            val durationSec = if (isJackpot) 5.0 else 2.5
            confettiParties = listOf(
                Party(
                    angle = -45,
                    spread = 45,
                    position = Position.Relative(0.0, 0.26),
                    emitter = Emitter(duration = durationSec.seconds).perSecond(30),
                    fadeOutEnabled = true
                ),
                Party(
                    angle = -135,
                    spread = 45,
                    position = Position.Relative(1.0, 0.26),
                    emitter = Emitter(duration = durationSec.seconds).perSecond(30),
                    fadeOutEnabled = true
                )
            )
            try {
                val volume = if (isJackpot) 1.0f else 0.5f
                winMediaPlayer.setVolume(volume, volume)
                if (isJackpot) {
                    scope.launch {
                        repeat(3) {
                            winMediaPlayer.seekTo(0)
                            winMediaPlayer.start()
                            val durationMs = winMediaPlayer.duration.toLong().coerceIn(200, 2000)
                            delay(durationMs)
                        }
                    }
                } else {
                    winMediaPlayer.seekTo(0)
                    winMediaPlayer.start()
                }
            } catch (_: Exception) { }
        } else {
        }
    }

    fun spin() {
        if (isSpinning || balance < bet) return
        
        balance -= bet
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

            Spacer(modifier = Modifier.weight(1f))

            // Mise (texte centré au-dessus des boutons)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mise : ",
                    fontSize = 16.sp,
                    color = DarkTextPrimary
                )
                Text(
                    text = "$bet",
                    fontSize = 16.sp,
                    color = DarkTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Contrôles de mise
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppButton(
                    text = "-10",
                    onClick = { if (bet > 10) bet -= 10 },
                    variant = ButtonVariant.Outline,
                    size = ButtonSize.Medium,
                    enabled = !isSpinning && bet > 10,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                AppButton(
                    text = "+10",
                    onClick = { if (bet < balance) bet += 10 },
                    variant = ButtonVariant.Outline,
                    size = ButtonSize.Medium,
                    enabled = !isSpinning && bet < balance,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
            }

            AppButton(
                text = if (isSpinning) "En cours..." else "Jouer",
                onClick = { spin() },
                variant = ButtonVariant.Primary,
                size = ButtonSize.Large,
                enabled = !isSpinning && balance >= bet,
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


