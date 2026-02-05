package com.example.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile.ui.components.AppButton
import com.example.mobile.ui.components.AppCard
import com.example.mobile.ui.components.AppHeader
import com.example.mobile.ui.components.BalanceHeader
import com.example.mobile.ui.components.ButtonSize
import com.example.mobile.ui.components.ButtonVariant
import com.example.mobile.ui.components.CardVariant
import com.example.mobile.ui.components.RouletteWheel
import com.example.mobile.ui.components.SpinCommand
import com.example.mobile.ui.theme.AccentBlue
import com.example.mobile.ui.theme.DarkBorder
import com.example.mobile.ui.theme.DarkSurface
import com.example.mobile.ui.theme.DarkTextPrimary
import com.example.mobile.ui.theme.DarkTextSecondary
import kotlin.random.Random

@Composable
fun RouletteScreen(
    onBackClick: () -> Unit
) {
    var spinCommandId by remember { mutableStateOf(0) }
    var currentSpinCommand by remember { mutableStateOf<SpinCommand?>(null) }

    // Pour harmoniser avec la SlotMachine, on affiche un solde et une mise simple
    var balance by remember { mutableStateOf(1000) }
    var bet by remember { mutableStateOf(10) }
    var lastResult by remember { mutableStateOf<String?>(null) }
    var forcedNumberText by remember { mutableStateOf("") }
    var selectedNumbers by remember { mutableStateOf(setOf<Int>()) }
    var selectedGroups by remember { mutableStateOf(setOf<String>()) }
    var isBettingPhase by remember { mutableStateOf(true) }

    fun requestSpin(forcedNumber: Int? = null) {
        // Ici tu pourras plus tard brancher la logique de gains/pertes avec balance/bet
        val targetNumber = forcedNumber ?: Random.nextInt(0, 37)
        spinCommandId++
        currentSpinCommand = SpinCommand(
            id = spinCommandId,
            number = targetNumber
        )
        lastResult = "Résultat : $targetNumber"
    }

    // Quand on passe en phase "roue", on lance automatiquement un spin
    // (retour à la phase de mise géré manuellement par l'utilisateur).
    LaunchedEffect(isBettingPhase) {
        if (!isBettingPhase) {
            val forced = forcedNumberText.toIntOrNull()?.takeIf { it in 0..36 }
            requestSpin(forced)
        }
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = "Roulette",
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
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
                // Solde (component animé réutilisable)
                BalanceHeader(
                    amount = balance,
                    modifier = Modifier
                        .padding(top = 40.dp)
                        .align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // === Étape 1 : mise (plein écran) ===
                if (isBettingPhase) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Choisissez votre mise",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkTextPrimary
                        )

                        val numbers = (0..36).toList()
                        val rows = numbers.chunked(7)

                        rows.forEach { rowNumbers ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                rowNumbers.forEach { number ->
                                    val isSelected = selectedNumbers.contains(number)

                                    val baseColor = when {
                                        number == 0 -> Color(0xFF1B5E20) // vert
                                        isRouletteRed(number) -> Color(0xFFB71C1C) // rouge
                                        else -> Color(0xFF111111) // noir
                                    }

                                    RouletteBetChip(
                                        label = number.toString(),
                                        isSelected = isSelected,
                                        baseColor = baseColor,
                                        onClick = {
                                            selectedNumbers = if (isSelected) {
                                                selectedNumbers - number
                                            } else {
                                                selectedNumbers + number
                                            }

                                            // Mettre à jour le résumé des mises
                                            fun summaryText(): String {
                                                val numbersText = if (selectedNumbers.isEmpty()) {
                                                    null
                                                } else {
                                                    selectedNumbers.sorted().joinToString(", ")
                                                }
                                                val groupsText = if (selectedGroups.isEmpty()) {
                                                    null
                                                } else {
                                                    selectedGroups.joinToString(", ")
                                                }

                                                return when {
                                                    numbersText == null && groupsText == null ->
                                                        "Aucune mise sélectionnée"
                                                    numbersText != null && groupsText == null ->
                                                        "Mise sur $numbersText"
                                                    numbersText == null && groupsText != null ->
                                                        "Mise sur $groupsText"
                                                    else ->
                                                        "Mise sur $numbersText | $groupsText"
                                                }
                                            }

                                            lastResult = summaryText()
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Ligne de mises spéciales (douzaines, pair/impair, etc.)
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Douzaines
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("1ère 12", "2ème 12", "3ème 12").forEach { label ->
                                    val id = "D_$label"
                                    val isSelected = selectedGroups.contains(id)

                                    RouletteBetChip(
                                        label = label,
                                        isSelected = isSelected,
                                        baseColor = Color(0xFF263238),
                                        onClick = {
                                            selectedGroups = if (isSelected) {
                                                selectedGroups - id
                                            } else {
                                                selectedGroups + id
                                            }

                                            val numbersText = if (selectedNumbers.isEmpty()) {
                                                null
                                            } else {
                                                selectedNumbers.sorted().joinToString(", ")
                                            }
                                            val groupsText = if (selectedGroups.isEmpty()) {
                                                null
                                            } else {
                                                selectedGroups.joinToString(", ")
                                            }

                                            lastResult = when {
                                                numbersText == null && groupsText == null ->
                                                    "Aucune mise sélectionnée"
                                                numbersText != null && groupsText == null ->
                                                    "Mise sur $numbersText"
                                                numbersText == null && groupsText != null ->
                                                    "Mise sur $groupsText"
                                                else ->
                                                    "Mise sur $numbersText | $groupsText"
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // Pair / Impair / 1-18 / 19-36 / Rouge / Noir
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val specialBets = listOf(
                                    "PAIR" to "Pair",
                                    "IMPAIR" to "Impair",
                                    "LOW" to "1-18",
                                    "HIGH" to "19-36",
                                    "RED" to "Rouge",
                                    "BLACK" to "Noir"
                                )

                                specialBets.forEach { (id, label) ->
                                    val isSelected = selectedGroups.contains(id)
                                    val baseColor = when (id) {
                                        "RED" -> Color(0xFFB71C1C)
                                        "BLACK" -> Color(0xFF111111)
                                        else -> Color(0xFF263238)
                                    }

                                    RouletteBetChip(
                                        label = label,
                                        isSelected = isSelected,
                                        baseColor = baseColor,
                                        onClick = {
                                            selectedGroups = if (isSelected) {
                                                selectedGroups - id
                                            } else {
                                                selectedGroups + id
                                            }

                                            val numbersText = if (selectedNumbers.isEmpty()) {
                                                null
                                            } else {
                                                selectedNumbers.sorted().joinToString(", ")
                                            }
                                            val groupsText = if (selectedGroups.isEmpty()) {
                                                null
                                            } else {
                                                selectedGroups.joinToString(", ")
                                            }

                                            lastResult = when {
                                                numbersText == null && groupsText == null ->
                                                    "Aucune mise sélectionnée"
                                                numbersText != null && groupsText == null ->
                                                    "Mise sur $numbersText"
                                                numbersText == null && groupsText != null ->
                                                    "Mise sur $groupsText"
                                                else ->
                                                    "Mise sur $numbersText | $groupsText"
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Bouton pour passer à l'étape roue
                        AppButton(
                            text = "Valider les mises",
                            onClick = {
                                if (selectedNumbers.isNotEmpty()) {
                                    isBettingPhase = false
                                }
                            },
                            variant = ButtonVariant.Primary,
                            size = ButtonSize.Medium,
                            enabled = selectedNumbers.isNotEmpty(),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    // === Étape 2 : roue + résultat ===
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            RouletteWheel(
                                isConnected = true,
                                externalSpinCommand = currentSpinCommand,
                                onExternalSpinConsumed = {
                                    currentSpinCommand = null
                                }
                            )
                        }

                        // Afficher les mises sélectionnées
                        if (selectedNumbers.isNotEmpty()) {
                            Text(
                                text = "Mise sur ${selectedNumbers.sorted().joinToString(", ")}",
                                fontSize = 14.sp,
                                color = DarkTextSecondary
                            )
                        }

                        // Message de résultat
                        if (lastResult != null) {
                            Text(
                                text = lastResult!!,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = DarkTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = "La roue tourne...",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = DarkTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Bouton pour revenir manuellement à l'étape de mise
                        AppButton(
                            text = "Nouvelle mise",
                            onClick = {
                                isBettingPhase = true
                                // on garde les sélections et le dernier résultat pour l'instant
                            },
                            variant = ButtonVariant.Primary,
                            size = ButtonSize.Medium,
                            enabled = true,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

// Détermine si un numéro de roulette est rouge (roulette européenne)
private fun isRouletteRed(number: Int): Boolean {
    return number in setOf(
        1, 3, 5, 7, 9,
        12, 14, 16, 18,
        19, 21, 23, 25, 27,
        30, 32, 34, 36
    )
}

@Composable
private fun RouletteBetChip(
    label: String,
    isSelected: Boolean,
    baseColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)
    val backgroundColor = if (isSelected) {
        baseColor.copy(alpha = 0.95f)
    } else {
        baseColor.copy(alpha = 0.7f)
    }
    val borderColor = Color.White.copy(alpha = if (isSelected) 0.9f else 0.4f)

    Box(
        modifier = modifier
            .height(32.dp)
            .border(1.dp, borderColor, shape)
            .background(backgroundColor, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}


