package com.example.mobile.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile.MainActivity
import com.example.mobile.ui.components.AppButton
import com.example.mobile.ui.components.AppCard
import com.example.mobile.ui.components.AppHeader
import com.example.mobile.ui.components.BalanceHeader
import com.example.mobile.ui.components.AppBottomBar
import com.example.mobile.ui.components.BottomBarItem
import com.example.mobile.ui.components.ButtonSize
import com.example.mobile.ui.components.ButtonVariant
import com.example.mobile.ui.components.CardVariant
import com.example.mobile.ui.components.RouletteWheel
import com.example.mobile.ui.components.SpinCommand
import com.example.mobile.ui.components.ConfettiOverlay
import com.example.mobile.ui.theme.AccentBlue
import com.example.mobile.ui.theme.DarkBorder
import com.example.mobile.ui.theme.DarkSurface
import com.example.mobile.ui.theme.DarkTextPrimary
import com.example.mobile.ui.theme.DarkTextSecondary
import androidx.compose.ui.platform.LocalContext
import com.example.mobile.ui.icons.AppIcons
import io.github.vinceglb.confettikit.core.Party
import io.github.vinceglb.confettikit.core.Position
import io.github.vinceglb.confettikit.core.emitter.Emitter
import kotlin.time.Duration.Companion.seconds
import kotlin.random.Random

@Composable
fun RouletteScreen(
    onBackClick: () -> Unit
) {
    var spinCommandId by remember { mutableStateOf(0) }
    var currentSpinCommand by remember { mutableStateOf<SpinCommand?>(null) }

    // Pour harmoniser avec la SlotMachine, on affiche un solde et une mise simple
    var balance by BalanceState.balance
    var bet by remember { mutableStateOf(10) }
    var winningNumber by remember { mutableStateOf<Int?>(null) }
    var forcedNumberText by remember { mutableStateOf("") }
    var selectedNumbers by remember { mutableStateOf(setOf<Int>()) }
    var selectedGroups by remember { mutableStateOf(setOf<String>()) }
    var isBettingPhase by remember { mutableStateOf(true) }
    var confettiParties by remember { mutableStateOf<List<Party>>(emptyList()) }

    val context = LocalContext.current

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

    fun requestSpin(forcedNumber: Int? = null) {
        // Ici tu pourras plus tard brancher la logique de gains/pertes avec balance/bet
        val targetNumber = forcedNumber ?: Random.nextInt(0, 37)
        spinCommandId++
        currentSpinCommand = SpinCommand(
            id = spinCommandId,
            number = targetNumber
        )
    }

    // Quand on passe en phase "roue", on lance automatiquement un spin
    // (retour à la phase de mise géré manuellement par l'utilisateur).
    LaunchedEffect(isBettingPhase) {
        if (!isBettingPhase) {
            val forced = forcedNumberText.toIntOrNull()?.takeIf { it in 0..36 }
            requestSpin(forced)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            topBar = {
                AppHeader(
                    title = "Roulette",
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
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val wheelHeight = androidx.compose.ui.unit.min(260.dp, maxHeight * 0.45f)
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                // Solde (component animé réutilisable)
                BalanceHeader(
                    amount = balance,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(2.dp))

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

                        // Disposition : 0 sur sa propre ligne, puis 1–36 en grille 4x9
                        val zeroNumber = 0
                        val numbers = (1..36).toList()
                        val rows = numbers.chunked(9)

                        // Ligne dédiée pour le 0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val isSelected = selectedNumbers.contains(zeroNumber)
                            val baseColor = Color(0xFF1B5E20) // vert pour le 0

                            RouletteBetChip(
                                label = zeroNumber.toString(),
                                isSelected = isSelected,
                                baseColor = baseColor,
                                onClick = {
                                    selectedNumbers = if (isSelected) {
                                        selectedNumbers - zeroNumber
                                    } else {
                                        selectedNumbers + zeroNumber
                                    }

                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Grille 6x6 pour 1–36 (évite une dernière ligne avec seulement 2 cases)
                        rows.forEach { rowNumbers ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                rowNumbers.forEach { number ->
                                    val isSelected = selectedNumbers.contains(number)

                                    val baseColor = when {
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

                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

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

                                            // ici on ne met plus à jour de texte de résultat,
                                            // seulement les sélections de mises
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Bouton pour passer à l'étape roue
                        AppButton(
                            text = "Valider les mises",
                            onClick = {
                                if (selectedNumbers.isNotEmpty() || selectedGroups.isNotEmpty()) {
                                    val totalStake = bet * (selectedNumbers.size + selectedGroups.size).coerceAtLeast(1)
                                    if (balance >= totalStake) {
                                        BalanceState.addPinos(-totalStake)
                                        isBettingPhase = false
                                    }
                                }
                            },
                            variant = ButtonVariant.Primary,
                            size = ButtonSize.Medium,
                            enabled = (selectedNumbers.isNotEmpty() || selectedGroups.isNotEmpty()) && balance >= bet * (selectedNumbers.size + selectedGroups.size).coerceAtLeast(1),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    // === Étape 2 : roue + résultat ===
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(wheelHeight),
                            contentAlignment = Alignment.Center
                        ) {
                            RouletteWheel(
                                isConnected = true,
                                externalSpinCommand = currentSpinCommand,
                                onExternalSpinConsumed = {
                                    currentSpinCommand = null
                                },
                                onNumberSelected = { number ->
                                    winningNumber = number

                                    // Calcul des gains et crédit des pinos (appelé quand la roue a fini de tourner)
                                    val payout = calculateRoulettePayout(
                                        selectedNumbers = selectedNumbers,
                                        selectedGroups = selectedGroups,
                                        winningNumber = number,
                                        betPerSelection = bet
                                    )
                                    if (payout > 0) {
                                        BalanceState.addPinos(payout)
                                    }

                                    // Déclenche les confettis uniquement si au moins une mise est gagnante
                                    val hasWinningNumber = selectedNumbers.contains(number)
                                    val hasWinningGroup = selectedGroups.any { isWinningGroup(it, number) }

                                    if (hasWinningNumber || hasWinningGroup) {
                                        val durationSec = 3.0
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
                                    }
                                }
                            )
                        }

                        // Afficher les mises sélectionnées sous forme de jetons non cliquables
                        if (selectedNumbers.isNotEmpty() || selectedGroups.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Jetons pour les numéros choisis
                                if (selectedNumbers.isNotEmpty()) {
                                    val rows = selectedNumbers.sorted().chunked(8)
                                    rows.forEach { rowNumbers ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            rowNumbers.forEach { number ->
                                                val baseColor = when {
                                                    number == 0 -> Color(0xFF1B5E20) // vert
                                                    isRouletteRed(number) -> Color(0xFFB71C1C) // rouge
                                                    else -> Color(0xFF111111) // noir
                                                }
                                                val isWinner = winningNumber != null && winningNumber == number
                                                RouletteBetChipStatic(
                                                    label = number.toString(),
                                                    baseColor = baseColor,
                                                    isWinner = isWinner,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Jetons pour les groupes spéciaux sélectionnés
                                if (selectedGroups.isNotEmpty()) {
                                    // Douzaines
                                    val dozenLabels = listOf("1ère 12", "2ème 12", "3ème 12")
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        dozenLabels.forEach { label ->
                                            val id = "D_$label"
                                            if (selectedGroups.contains(id)) {
                                                val isWinner = winningNumber != null && isWinningGroup(id, winningNumber!!)
                                                RouletteBetChipStatic(
                                                    label = label,
                                                    baseColor = Color(0xFF263238),
                                                    isWinner = isWinner,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }

                                    // Pair / Impair / 1-18 / 19-36 / Rouge / Noir
                                    val specialBets = listOf(
                                        "PAIR" to "Pair",
                                        "IMPAIR" to "Impair",
                                        "LOW" to "1-18",
                                        "HIGH" to "19-36",
                                        "RED" to "Rouge",
                                        "BLACK" to "Noir"
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        specialBets.forEach { (id, label) ->
                                            if (selectedGroups.contains(id)) {
                                                val baseColor = when (id) {
                                                    "RED" -> Color(0xFFB71C1C)
                                                    "BLACK" -> Color(0xFF111111)
                                                    else -> Color(0xFF263238)
                                                }
                                                val isWinner = winningNumber != null && isWinningGroup(id, winningNumber!!)
                                                RouletteBetChipStatic(
                                                    label = label,
                                                    baseColor = baseColor,
                                                    isWinner = isWinner,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Bouton pour revenir manuellement à l'étape de mise
                        AppButton(
                            text = "Nouvelle mise",
                            onClick = {
                                isBettingPhase = true
                                // on garde les sélections et les mises
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

        // Overlay de confettis au premier plan
        ConfettiOverlay(
            parties = confettiParties,
            onFinished = { confettiParties = emptyList() },
            modifier = Modifier.fillMaxSize()
        )
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

// Calcule le gain total roulette (mise rendue + gains) pour le numéro gagnant
private fun calculateRoulettePayout(
    selectedNumbers: Set<Int>,
    selectedGroups: Set<String>,
    winningNumber: Int,
    betPerSelection: Int
): Int {
    var payout = 0
    if (winningNumber in selectedNumbers) {
        payout += 36 * betPerSelection // plein : 35:1 + mise
    }
    selectedGroups.forEach { groupId ->
        if (isWinningGroup(groupId, winningNumber)) {
            payout += if (groupId.startsWith("D_")) 3 * betPerSelection else 2 * betPerSelection // douzaine 2:1, autres 1:1
        }
    }
    return payout
}

// Vérifie si un groupe de mise est gagnant pour un numéro donné
private fun isWinningGroup(groupId: String, number: Int): Boolean {
    if (number !in 0..36) return false
    return when (groupId) {
        "PAIR" -> number != 0 && number % 2 == 0
        "IMPAIR" -> number % 2 == 1
        "LOW" -> number in 1..18
        "HIGH" -> number in 19..36
        "RED" -> isRouletteRed(number)
        "BLACK" -> number != 0 && !isRouletteRed(number)
        "D_1ère 12" -> number in 1..12
        "D_2ème 12" -> number in 13..24
        "D_3ème 12" -> number in 25..36
        else -> false
    }
}

@Composable
fun RouletteBetChip(
    label: String,
    isSelected: Boolean,
    baseColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)
    // Fond beaucoup plus marqué quand sélectionné, très atténué sinon
    val backgroundColor = if (isSelected) {
        // Cas particulier pour le noir : éclaircir un peu la case sélectionnée
        if (baseColor == Color(0xFF111111)) {
            Color(0xFF333333)
        } else {
            baseColor
        }
    } else {
        baseColor.copy(alpha = 0.25f)
    }
    // Bordure = même couleur que la case, légèrement plus foncée
    val darkFactor = if (isSelected) 0.8f else 0.6f
    val borderColor = if (baseColor == Color(0xFF111111)) {
        // cas particulier pour le noir : éclaircir un peu pour la rendre visible
        Color(0xFF444444)
    } else {
        darkenColor(baseColor, darkFactor)
    }

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

@Composable
fun RouletteBetChipStatic(
    label: String,
    baseColor: Color,
    isWinner: Boolean = false,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)
    val backgroundColor = if (isWinner) {
        baseColor.copy(alpha = 1.0f)
    } else {
        baseColor.copy(alpha = 0.9f)
    }
    val borderColor = if (isWinner) {
        AccentBlue
    } else if (baseColor == Color(0xFF111111)) {
        Color(0xFF444444)
    } else {
        darkenColor(baseColor, 0.75f)
    }

    Box(
        modifier = modifier
            .height(32.dp)
            .border(2.dp, borderColor, shape)
            .background(backgroundColor, shape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Medium,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

// Assombrit légèrement une couleur (même teinte, plus foncée)
private fun darkenColor(color: Color, factor: Float): Color {
    val f = factor.coerceIn(0f, 1f)
    return Color(
        red = (color.red * f).coerceIn(0f, 1f),
        green = (color.green * f).coerceIn(0f, 1f),
        blue = (color.blue * f).coerceIn(0f, 1f),
        alpha = color.alpha
    )
}

