package com.example.mobile.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import com.example.mobile.ui.theme.DarkTextPrimary
import kotlinx.coroutines.delay
import kotlin.math.abs
import java.text.NumberFormat
import java.util.Locale

/**
 * Affichage animé du solde / argent, réutilisable.
 *
 * Chaque chiffre a sa propre "colonne" et roule vers le haut / bas
 * avec une transition spring bouncy.
 */
@Composable
fun BalanceHeader(
    amount: Int,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 56.sp,
    fontWeight: FontWeight = FontWeight.Light
) {
    // Compteur intermédiaire qui évolue pas à pas vers [amount]
    var displayedAmount by remember { mutableStateOf(amount) }

    LaunchedEffect(amount) {
        val start = displayedAmount
        val end = amount
        if (start == end) return@LaunchedEffect

        val diff = end - start
        val absDiff = abs(diff)

        // Si l'écart est vraiment énorme, on évite une animation longue
        // pour préserver les perfs (on "snap" directement à la valeur).
        if (absDiff > 200) {
            displayedAmount = end
            return@LaunchedEffect
        }
        // On limite le nombre d'étapes et la durée totale pour éviter
        // que l'animation devienne trop folle sur les gros gains.
        val maxSteps = 15
        val maxDurationMs = 400

        val steps = minOf(maxSteps, absDiff)
        if (steps == 0) {
            displayedAmount = end
            return@LaunchedEffect
        }

        val stepSize = if (diff > 0) {
            maxOf(1, diff / steps)
        } else {
            minOf(-1, diff / steps)
        }

        val stepDelay = maxDurationMs / steps

        var current = start
        repeat(steps) {
            current += stepSize
            if ((diff > 0 && current > end) || (diff < 0 && current < end)) {
                current = end
            }
            displayedAmount = current
            if (current == end) return@LaunchedEffect
            delay(stepDelay.toLong())
        }

        displayedAmount = end
    }

    // Formatage avec séparateurs (ex: 1 234, 12 345)
    val formatter = NumberFormat.getInstance(Locale.getDefault())
    val formatted = formatter.format(displayedAmount.coerceAtLeast(0))

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        formatted.forEach { ch ->
            if (ch.isDigit()) {
                val digit = ch.digitToInt()

                AnimatedContent(
                    targetState = digit,
                    transitionSpec = {
                        if (targetState > initialState) {
                            // nombre qui augmente -> le nouveau chiffre arrive par le bas
                            slideInVertically { fullHeight -> fullHeight } + fadeIn() togetherWith
                                slideOutVertically { fullHeight -> -fullHeight } + fadeOut()
                        } else {
                            // nombre qui diminue -> le nouveau chiffre arrive par le haut
                            slideInVertically { fullHeight -> -fullHeight } + fadeIn() togetherWith
                                slideOutVertically { fullHeight -> fullHeight } + fadeOut()
                        }.using(
                            SizeTransform(clip = false, sizeAnimationSpec = { _, _ ->
                                spring(
                                    stiffness = Spring.StiffnessMediumLow,
                                    dampingRatio = Spring.DampingRatioMediumBouncy
                                )
                            })
                        )
                    },
                    label = "balance_digit"
                ) { value ->
                    Text(
                        text = value.toString(),
                        fontSize = fontSize,
                        fontWeight = fontWeight,
                        color = DarkTextPrimary
                    )
                }
            } else {
                // Séparateurs (espace, virgule, etc.) non animés
                Text(
                    text = ch.toString(),
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    color = DarkTextPrimary
                )
            }
        }
    }
}

