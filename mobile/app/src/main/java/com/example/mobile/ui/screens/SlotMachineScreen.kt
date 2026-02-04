package com.example.mobile.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile.R
import com.example.mobile.ui.components.AppButton
import com.example.mobile.ui.components.AppCard
import com.example.mobile.ui.components.AppHeader
import com.example.mobile.ui.components.ButtonSize
import com.example.mobile.ui.components.ButtonVariant
import com.example.mobile.ui.components.CardVariant
import com.example.mobile.ui.theme.DarkSurface
import com.example.mobile.ui.theme.DarkSurfaceVariant
import com.example.mobile.ui.theme.DarkTextPrimary
import com.example.mobile.ui.theme.DarkTextSecondary
import kotlin.random.Random
import kotlinx.coroutines.delay

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
        } else {
            resultMessage = "Essayez encore !"
        }
        
        isSpinning = false
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Solde (même style que la homepage)
            Text(
                text = "$balance",
                fontSize = 56.sp,
                fontWeight = FontWeight.Light,
                color = DarkTextPrimary,
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

            // Bouton de spin
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
