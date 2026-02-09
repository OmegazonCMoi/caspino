package com.example.mobile.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile.ui.theme.AccentBlue
import com.example.mobile.ui.theme.DarkTextPrimary
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun RouletteWheel(
    modifier: Modifier = Modifier,
    isConnected: Boolean = true,
    externalSpinCommand: SpinCommand? = null,
    onExternalSpinConsumed: () -> Unit = {},
    onNumberSelected: (Int) -> Unit = {}
) {
    val numbers = (0..36).toList()
    val blockSize = 80.dp
    val blockSpacing = 8.dp
    val itemWidthDp = blockSize + blockSpacing

    val density = LocalDensity.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val itemWidthPx = with(density) { itemWidthDp.toPx() }
    val screenWidthPx = with(density) { screenWidthDp.toPx() }

    val repeats = 300
    val totalItems = repeats * numbers.size
    val middleIndex = totalItems / 2

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = middleIndex)
    val scope = rememberCoroutineScope()

    var isSpinning by remember { mutableStateOf(false) }
    var pendingExternalSpin by remember { mutableStateOf<SpinCommand?>(null) }
    var awaitingServer by remember { mutableStateOf(false) }

    val scroller = remember { Animatable(0f) }

    LaunchedEffect(isConnected) {
        if (!isConnected) {
            awaitingServer = false
        }
    }

    fun triggerSpin(targetNumber: Int) {
        if (isSpinning) return
        val numberIndex = numbers.indexOf(targetNumber).takeIf { it >= 0 } ?: return
        val spinRounds = 15
        val targetIndex = middleIndex + (spinRounds * numbers.size) + numberIndex
        val targetCenterPx = targetIndex * itemWidthPx + (itemWidthPx / 2f)
        val targetScrollPx = targetCenterPx - (screenWidthPx / 2f)

        isSpinning = true
        scope.launch {
            scroller.snapTo(0f)
            scroller.animateTo(
                targetValue = targetScrollPx,
                animationSpec = tween(
                    durationMillis = 15000,
                    easing = CubicBezierEasing(0.08f, 0.78f, 0.22f, 1f)
                )
            )
            isSpinning = false
            // Signale le numéro gagnant une fois l'animation terminée
            onNumberSelected(targetNumber)
        }
    }

    LaunchedEffect(scroller.value) {
        val px = scroller.value.coerceAtLeast(0f)
        val index = (px / itemWidthPx).toInt().coerceIn(0, totalItems - 1)
        val offsetPx = px - (index * itemWidthPx)
        listState.scrollToItem(index = index, scrollOffset = offsetPx.roundToInt().coerceAtLeast(0))
    }

    LaunchedEffect(externalSpinCommand?.id) {
        pendingExternalSpin = externalSpinCommand
    }

    LaunchedEffect(pendingExternalSpin?.id, isSpinning) {
        val command = pendingExternalSpin ?: return@LaunchedEffect
        if (!isSpinning) {
            awaitingServer = false
            triggerSpin(command.number)
            onExternalSpinConsumed()
            pendingExternalSpin = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(blockSize)
        ) {
            // Rangée de numéros
            LazyRow(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(blockSpacing)
            ) {
                items(totalItems) { index ->
                    val number = numbers[index % numbers.size]
                    RouletteBlock(number = number, size = blockSize)
                }
            }

            // Barre bleue par-dessus, centrée
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(4.dp)
                    .height(blockSize)
                    .background(
                        color = AccentBlue,
                        shape = RoundedCornerShape(12.dp)
                    ),

            )
        }
    }
}

@Composable
private fun RouletteBlock(
    number: Int,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .background(
                color = when {
                    number == 0 -> Color(0xFF1B5E20)        // vert pour le 0
                    number % 2 == 0 -> Color(0xFFB71C1C)   // rouge pour les pairs
                    else -> Color(0xFF212121)             // noir pour les impairs
                },
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = DarkTextPrimary,
            textAlign = TextAlign.Center
        )
    }
}

data class SpinCommand(
    val id: Int,
    val number: Int
)

