package com.example.mobile.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile.R
import com.example.mobile.ui.components.AppButton
import com.example.mobile.ui.components.AppCard
import com.example.mobile.ui.components.AppHeader
import com.example.mobile.ui.components.BalanceHeader
import com.example.mobile.ui.components.ButtonSize
import com.example.mobile.ui.components.ButtonVariant
import com.example.mobile.ui.theme.DarkSurfaceVariant
import com.example.mobile.ui.theme.DarkTextPrimary
import com.example.mobile.ui.theme.DarkTextSecondary
import android.media.MediaPlayer
import io.github.vinceglb.confettikit.compose.ConfettiKit
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
    var balance by remember { mutableStateOf(1000) }
    var bet by remember { mutableStateOf(10) }
    var lastWin by remember { mutableStateOf(0) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var spinTrigger by remember { mutableStateOf(0) }
    var confettiParties by remember { mutableStateOf<List<Party>>(emptyList()) }
    var confettiDurationMs by remember { mutableStateOf(0L) }

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
    val mediaPlayer = remember {
        MediaPlayer.create(context, R.raw.confetti_sound)
    }
    
    fun calculateWin(r1: Int, r2: Int, r3: Int, betAmount: Int): Int {
        return when {
            r1 == r2 && r2 == r3 -> {
                when (r1) {
                    6 -> betAmount * 100 // 7️⃣ 7️⃣ 7️⃣
                    5 -> betAmount * 50  // ⭐ ⭐ ⭐
                    4 -> betAmount * 25  // 🔔 🔔 🔔
                    else -> betAmount * 10
                }
            }
            r1 == r2 || r2 == r3 || r1 == r3 -> betAmount * 2
            else -> 0
        }
    }
    
    // Gérer l'animation du spin
    LaunchedEffect(spinTrigger) {
        if (spinTrigger == 0) return@LaunchedEffect
        
        isSpinning = true
        resultMessage = null
        
        // Animation des rouleaux
        val target1 = Random.nextInt(symbolDrawables.size)
        val target2 = Random.nextInt(symbolDrawables.size)
        val target3 = Random.nextInt(symbolDrawables.size)
        
        // Simuler plusieurs tours
        repeat(20) {
            reel1 = Random.nextInt(symbolDrawables.size)
            reel2 = Random.nextInt(symbolDrawables.size)
            reel3 = Random.nextInt(symbolDrawables.size)
            delay(50)
        }
        
        reel1 = target1
        reel2 = target2
        reel3 = target3
        
        // Calculer les gains
        val win = calculateWin(reel1, reel2, reel3, bet)
        balance += win
        lastWin = win

        if (win > 0) {
            resultMessage = "Vous avez gagné $win !"
            val isJackpot = (reel1 == reel2 && reel2 == reel3)
            val durationSec = if (isJackpot) 5.0 else 2.5
            confettiDurationMs = (durationSec * 1000).toLong()
            confettiParties = listOf(
                Party(
                    angle = 90,
                    spread = 45,
                    position = Position.Relative(0.0, 0.08),
                    emitter = Emitter(duration = durationSec.seconds).perSecond(30),
                    fadeOutEnabled = true
                ),
                Party(
                    angle = 90,
                    spread = 45,
                    position = Position.Relative(1.0, 0.08),
                    emitter = Emitter(duration = durationSec.seconds).perSecond(30),
                    fadeOutEnabled = true
                )
            )
            try {
                val volume = if (isJackpot) 1.0f else 0.5f
                mediaPlayer.setVolume(volume, volume)
                if (isJackpot) {
                    repeat(3) {
                        mediaPlayer.seekTo(0)
                        mediaPlayer.start()
                        val durationMs = mediaPlayer.duration.toLong().coerceIn(200, 2000)
                        delay(durationMs)
                    }
                } else {
                    mediaPlayer.seekTo(0)
                    mediaPlayer.start()
                }
            } catch (_: Exception) { }
        } else {
            resultMessage = "Essayez encore !"
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
        balance += win
        lastWin = win

        if (win > 0) {
            resultMessage = "Vous avez gagné $win !"
            val isJackpot = (reel1 == reel2 && reel2 == reel3)
            val durationSec = if (isJackpot) 5.0 else 2.5
            confettiDurationMs = (durationSec * 1000).toLong()
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
                mediaPlayer.setVolume(volume, volume)
                if (isJackpot) {
                    scope.launch {
                        repeat(3) {
                            mediaPlayer.seekTo(0)
                            mediaPlayer.start()
                            val durationMs = mediaPlayer.duration.toLong().coerceIn(200, 2000)
                            delay(durationMs)
                        }
                    }
                } else {
                    mediaPlayer.seekTo(0)
                    mediaPlayer.start()
                }
            } catch (_: Exception) { }
        } else {
            resultMessage = "Essayez encore !"
        }
    }

    fun spin() {
        if (isSpinning || balance < bet) return
        
        balance -= bet
        spinTrigger++
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = "Machine à sous",
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->

        LaunchedEffect(confettiParties) {
            if (confettiParties.isNotEmpty()) {
                delay(confettiDurationMs)
                confettiParties = emptyList()
            }
        }

        if (confettiParties.isNotEmpty()) {
            ConfettiKit(
                modifier = Modifier.fillMaxSize(),
                parties = confettiParties
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Solde (composant animé réutilisable)
            BalanceHeader(
                amount = balance,
                modifier = Modifier.padding(top = 60.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

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

            // Message de résultat
            if (resultMessage != null) {
                Text(
                    text = resultMessage!!,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (lastWin > 0) DarkTextPrimary else DarkTextSecondary,
                    textAlign = TextAlign.Center
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

            // Bouton de spin (désactivé pour les tests)
            /*
            AppButton(
                text = if (isSpinning) "En cours..." else "Jouer",
                onClick = { spin() },
                variant = ButtonVariant.Primary,
                size = ButtonSize.Large,
                enabled = !isSpinning && balance >= bet,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )
            */

            // Boutons de test : perdre, gagner avec 2, gagner avec 3
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppButton(
                    text = "Perdre",
                    onClick = {
                        // combinaison perdante : 3 symboles différents
                        applyForcedResult(0, 1, 2)
                    },
                    variant = ButtonVariant.Outline,
                    size = ButtonSize.Medium,
                    enabled = !isSpinning,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )
                AppButton(
                    text = "Gagner (2 symboles)",
                    onClick = {
                        // deux symboles identiques, un différent
                        applyForcedResult(0, 0, 1)
                    },
                    variant = ButtonVariant.Primary,
                    size = ButtonSize.Medium,
                    enabled = !isSpinning,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth()
                )
                AppButton(
                    text = "Jackpot (3 symboles)",
                    onClick = {
                        // 3 symboles identiques : jackpot (7 7 7)
                        applyForcedResult(6, 6, 6)
                    },
                    variant = ButtonVariant.Primary,
                    size = ButtonSize.Medium,
                    enabled = !isSpinning,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun SlotReel(
    iconResId: Int,
    isSpinning: Boolean,
    modifier: Modifier = Modifier
) {
    var rotation by remember { mutableStateOf(0f) }
    
    val animatedRotation by animateFloatAsState(
        targetValue = if (isSpinning) rotation + 360f else rotation,
        animationSpec = if (isSpinning) {
            tween(durationMillis = 1000, delayMillis = 0)
        } else {
            spring(dampingRatio = 0.6f, stiffness = 300f)
        },
        label = "reel_rotation"
    )

    LaunchedEffect(isSpinning) {
        if (isSpinning) {
            rotation += 360f
        }
    }

    Box(
        modifier = modifier
            .height(120.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(DarkSurfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            modifier = Modifier
                .size(42.dp)
                .rotate(animatedRotation),
            contentScale = ContentScale.Fit
        )
    }
}


