package com.example.mobile.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile.ui.theme.AccentBlue
import com.example.mobile.ui.theme.DarkBorder
import com.example.mobile.ui.theme.DarkSurface
import com.example.mobile.ui.theme.DarkTextPrimary
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun RouletteWheel(
    modifier: Modifier = Modifier,
    isConnected: Boolean = true,
    externalSpinCommand: SpinCommand? = null,
    onExternalSpinConsumed: () -> Unit = {},
    onSpinRequest: (Int?) -> Unit = {},
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
    var forcedNumberText by remember { mutableStateOf("") }
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

        onNumberSelected(targetNumber)

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
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val buttonLabel = when {
                !isConnected -> "Connexion..."
                isSpinning -> "Arrêt..."
                awaitingServer -> "En attente..."
                else -> "Lancer"
            }
            val buttonEnabled = isConnected && !isSpinning && !awaitingServer

            AppButton(
                text = buttonLabel,
                onClick = {
                    if (!isConnected || isSpinning || awaitingServer) return@AppButton
                    val forced = forcedNumberText.toIntOrNull()?.takeIf { it in 0..36 }
                    awaitingServer = true
                    onSpinRequest(forced)
                },
                variant = ButtonVariant.Primary,
                size = ButtonSize.Medium,
                enabled = buttonEnabled,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(12.dp))

            TextField(
                value = forcedNumberText,
                onValueChange = { input ->
                    forcedNumberText = input.filter { it.isDigit() }.take(2)
                },
                label = { Text("Forcer") },
                placeholder = { Text("0-36") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                keyboardActions = KeyboardActions(onDone = {}),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedIndicatorColor = AccentBlue,
                    unfocusedIndicatorColor = DarkBorder
                ),
                modifier = Modifier.width(100.dp),
                enabled = buttonEnabled
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(blockSize + 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(AccentBlue)
            )

            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(blockSpacing)
            ) {
                items(totalItems) { index ->
                    val number = numbers[index % numbers.size]
                    RouletteBlock(number = number, size = blockSize)
                }
            }
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
                color = DarkSurface.copy(alpha = 0.7f),
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

