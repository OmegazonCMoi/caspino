package com.example.mobile.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun RouletteWheel(
    modifier: Modifier = Modifier,
    onNumberSelected: (Int) -> Unit = {}
) {
    val numbers = (0..36).toList()

    // Visual sizes
    val blockSize = 80.dp
    val blockSpacing = 8.dp
    val itemWidthDp = blockSize + blockSpacing

    val density = LocalDensity.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp

    val itemWidthPx = with(density) { itemWidthDp.toPx() }
    val screenWidthPx = with(density) { screenWidthDp.toPx() }
    val screenCenterPx = screenWidthPx / 2f

    // LARGE repeated list to allow long scrolls
    val repeats = 300
    val totalItems = repeats * numbers.size
    val middleIndex = totalItems / 2

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = middleIndex)
    val scope = rememberCoroutineScope()

    var isSpinning by remember { mutableStateOf(false) }
    var forcedNumberText by remember { mutableStateOf("") }

    // An actual pixel animator (independent from LazyList)
    val scroller = remember { Animatable(0f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {

        // --- CONTROLS ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppButton(
                text = if (isSpinning) "Arrêt..." else "Lancer",
                onClick = {
                    if (isSpinning) return@AppButton

                    val forced = forcedNumberText.toIntOrNull()?.takeIf { it in 0..36 }
                    val selected = forced ?: (0..36).random()
                    val targetIndexInNumbers = numbers.indexOf(selected)
                    if (targetIndexInNumbers == -1) return@AppButton

                    onNumberSelected(selected)
                    isSpinning = true

                    scope.launch {
                        // Reset scroller offset to 0
                        scroller.snapTo(0f)

                        // Compute target index
                        val spinRounds = 15
                        val cycleSize = numbers.size

                        val targetGlobalIndex =
                            middleIndex + (spinRounds * cycleSize) + targetIndexInNumbers

                        // Compute target pixel position
                        val targetCenterPx =
                            targetGlobalIndex * itemWidthPx + (itemWidthPx / 2f)

                        val targetScrollPx = targetCenterPx - screenCenterPx

                        // Animate over 15 seconds
                        scroller.animateTo(
                            targetValue = targetScrollPx,
                            animationSpec = tween(
                                durationMillis = 15000,
                                easing = CubicBezierEasing(0.08f, 0.78f, 0.22f, 1f)
                            )
                        )

                        isSpinning = false
                    }
                },
                variant = ButtonVariant.Primary,
                size = ButtonSize.Medium,
                enabled = !isSpinning,
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
                enabled = !isSpinning
            )
        }

        // --- ROULETTE ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(blockSize + 32.dp)
                .padding(vertical = 16.dp)
        ) {
            // Blue center indicator
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(AccentBlue)
            )

            // LazyRow that follows the Animatable scroll
            LaunchedEffect(scroller.value) {
                val px = scroller.value
                val index = (px / itemWidthPx).toInt()
                val offsetPx = px - (index * itemWidthPx)
                listState.scrollToItem(
                    index = index,
                    scrollOffset = offsetPx.roundToInt().coerceAtLeast(0)
                )
            }

            LazyRow(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                items(totalItems) { i ->
                    val number = numbers[i % numbers.size]
                    RouletteBlock(
                        number = number,
                        size = blockSize,
                        modifier = Modifier.padding(horizontal = blockSpacing / 2)
                    )
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
