package com.example.mobile.ui.screens

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile.MainActivity
import com.example.mobile.ui.components.AppButton
import com.example.mobile.ui.components.AppHeader
import com.example.mobile.ui.components.BalanceHeader
import com.example.mobile.ui.components.AppBottomBar
import com.example.mobile.ui.components.BottomBarItem
import com.example.mobile.ui.components.ButtonSize
import com.example.mobile.ui.components.ButtonVariant
import com.example.mobile.ui.components.CardVariant
import com.example.mobile.ui.components.CroupierPunchlineBanner
import com.example.mobile.ui.components.RouletteWheel
import com.example.mobile.ui.components.SpinCommand
import com.example.mobile.ui.components.ConfettiOverlay
import com.example.mobile.ui.theme.AccentBlue
import com.example.mobile.ui.icons.AppIcons
import com.example.mobile.network.PunchlineApi
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import io.github.vinceglb.confettikit.core.Party
import io.github.vinceglb.confettikit.core.Position
import io.github.vinceglb.confettikit.core.emitter.Emitter
import kotlin.time.Duration.Companion.seconds
import kotlin.random.Random

private val chipDenominations = listOf(10, 25, 50, 100, 500)

private fun chipColor(value: Int): Color = when (value) {
    10 -> Color(0xFF1565C0)
    25 -> Color(0xFF2E7D32)
    50 -> Color(0xFFE65100)
    100 -> Color(0xFF212121)
    500 -> Color(0xFF6A1B9A)
    else -> Color.Gray
}

private fun numberAt(row: Int, col: Int) = row * 3 + col + 1

private const val EDGE_THRESHOLD = 0.28f

/**
 * Determine bet target from tap position on the 12x3 grid.
 * Returns sorted list of numbers: size 1=straight, 2=split, 3=street, 4=corner.
 */
private fun determineBetTarget(
    tapX: Float,
    tapY: Float,
    cellWidthPx: Float,
    cellHeightPx: Float
): List<Int> {
    val col = (tapX / cellWidthPx).toInt().coerceIn(0, 2)
    val row = (tapY / cellHeightPx).toInt().coerceIn(0, 11)
    val xFrac = (tapX - col * cellWidthPx) / cellWidthPx
    val yFrac = (tapY - row * cellHeightPx) / cellHeightPx

    val nearLeft = xFrac < EDGE_THRESHOLD
    val nearRight = xFrac > (1 - EDGE_THRESHOLD)
    val nearTop = yFrac < EDGE_THRESHOLD
    val nearBottom = yFrac > (1 - EDGE_THRESHOLD)

    // Corner (carre) - 4 numbers
    if (nearTop && nearLeft && row > 0 && col > 0) {
        return listOf(
            numberAt(row - 1, col - 1), numberAt(row - 1, col),
            numberAt(row, col - 1), numberAt(row, col)
        ).sorted()
    }
    if (nearTop && nearRight && row > 0 && col < 2) {
        return listOf(
            numberAt(row - 1, col), numberAt(row - 1, col + 1),
            numberAt(row, col), numberAt(row, col + 1)
        ).sorted()
    }
    if (nearBottom && nearLeft && row < 11 && col > 0) {
        return listOf(
            numberAt(row, col - 1), numberAt(row, col),
            numberAt(row + 1, col - 1), numberAt(row + 1, col)
        ).sorted()
    }
    if (nearBottom && nearRight && row < 11 && col < 2) {
        return listOf(
            numberAt(row, col), numberAt(row, col + 1),
            numberAt(row + 1, col), numberAt(row + 1, col + 1)
        ).sorted()
    }

    // Horizontal split (cheval) - between left-right neighbours
    if (nearLeft && !nearTop && !nearBottom && col > 0) {
        return listOf(numberAt(row, col - 1), numberAt(row, col)).sorted()
    }
    if (nearRight && !nearTop && !nearBottom && col < 2) {
        return listOf(numberAt(row, col), numberAt(row, col + 1)).sorted()
    }

    // Street (transversale) - left edge of column 0
    if (nearLeft && !nearTop && !nearBottom && col == 0) {
        return listOf(numberAt(row, 0), numberAt(row, 1), numberAt(row, 2)).sorted()
    }

    // Vertical split (cheval) - between top-bottom neighbours
    if (nearTop && !nearLeft && !nearRight && row > 0) {
        return listOf(numberAt(row - 1, col), numberAt(row, col)).sorted()
    }
    if (nearBottom && !nearLeft && !nearRight && row < 11) {
        return listOf(numberAt(row, col), numberAt(row + 1, col)).sorted()
    }

    // Straight bet
    return listOf(numberAt(row, col))
}

/** Compute overlay badge center position in pixels for a multi-number bet. */
private fun multiBetCenterPx(
    numbers: List<Int>,
    cellWidthPx: Float,
    cellHeightPx: Float
): Pair<Float, Float> {
    if (numbers.size == 3) {
        // Street: left edge of the row
        val row = (numbers.min() - 1) / 3
        return Pair(0f, row * cellHeightPx + cellHeightPx / 2f)
    }
    var centerX = 0f
    var centerY = 0f
    numbers.forEach { number ->
        val row = (number - 1) / 3
        val col = (number - 1) % 3
        centerX += col * cellWidthPx + cellWidthPx / 2f
        centerY += row * cellHeightPx + cellHeightPx / 2f
    }
    return Pair(centerX / numbers.size, centerY / numbers.size)
}

private fun multiBetMultiplier(count: Int): Int = when (count) {
    2 -> 18   // split
    3 -> 12   // street
    4 -> 9    // corner
    else -> 0
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RouletteScreen(
    onBackClick: () -> Unit
) {
    var spinCommandId by remember { mutableIntStateOf(0) }
    var currentSpinCommand by remember { mutableStateOf<SpinCommand?>(null) }

    var balance by BalanceState.balance
    var winningNumber by remember { mutableStateOf<Int?>(null) }
    var forcedNumberText by remember { mutableStateOf("") }
    var isBettingPhase by remember { mutableStateOf(true) }
    var confettiParties by remember { mutableStateOf<List<Party>>(emptyList()) }
    var punchline by remember { mutableStateOf<String?>(null) }

    // Chip-based betting state
    var numberBets by remember { mutableStateOf(mapOf<Int, Int>()) }
    var groupBets by remember { mutableStateOf(mapOf<String, Int>()) }
    var multiBets by remember { mutableStateOf(mapOf<String, Int>()) }
    var selectedChipValue by remember { mutableIntStateOf(10) }

    val totalBet = numberBets.values.sum() + groupBets.values.sum() + multiBets.values.sum()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            PunchlineApi.fetchPunchline("roulette")
                .onSuccess { punchline = it }
        }
    }
    val density = LocalDensity.current

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

    // --- Bet helpers ---
    fun addNumberBet(number: Int) {
        if (balance >= selectedChipValue) {
            numberBets = numberBets + (number to ((numberBets[number] ?: 0) + selectedChipValue))
            BalanceState.addPinos(-selectedChipValue)
        }
    }

    fun removeNumberBet(number: Int) {
        val current = numberBets[number] ?: return
        val chip = chipDenominations.sortedDescending().firstOrNull { it <= current } ?: return
        val remaining = current - chip
        numberBets = if (remaining <= 0) numberBets - number else numberBets + (number to remaining)
        BalanceState.addPinos(chip)
    }

    fun addGroupBet(groupId: String) {
        if (balance >= selectedChipValue) {
            groupBets = groupBets + (groupId to ((groupBets[groupId] ?: 0) + selectedChipValue))
            BalanceState.addPinos(-selectedChipValue)
        }
    }

    fun removeGroupBet(groupId: String) {
        val current = groupBets[groupId] ?: return
        val chip = chipDenominations.sortedDescending().firstOrNull { it <= current } ?: return
        val remaining = current - chip
        groupBets = if (remaining <= 0) groupBets - groupId else groupBets + (groupId to remaining)
        BalanceState.addPinos(chip)
    }

    fun addMultiBet(numbers: List<Int>) {
        if (balance >= selectedChipValue) {
            val key = numbers.sorted().joinToString("_")
            multiBets = multiBets + (key to ((multiBets[key] ?: 0) + selectedChipValue))
            BalanceState.addPinos(-selectedChipValue)
        }
    }

    fun removeMultiBet(numbers: List<Int>) {
        val key = numbers.sorted().joinToString("_")
        val current = multiBets[key] ?: return
        val chip = chipDenominations.sortedDescending().firstOrNull { it <= current } ?: return
        val remaining = current - chip
        multiBets = if (remaining <= 0) multiBets - key else multiBets + (key to remaining)
        BalanceState.addPinos(chip)
    }

    fun clearAllBets() {
        BalanceState.addPinos(totalBet)
        numberBets = emptyMap()
        groupBets = emptyMap()
        multiBets = emptyMap()
    }

    fun handleGridTap(numbers: List<Int>, isRemove: Boolean) {
        if (numbers.size == 1) {
            if (isRemove) removeNumberBet(numbers[0]) else addNumberBet(numbers[0])
        } else {
            if (isRemove) removeMultiBet(numbers) else addMultiBet(numbers)
        }
    }

    fun requestSpin(forcedNumber: Int? = null) {
        val targetNumber = forcedNumber ?: Random.nextInt(0, 37)
        spinCommandId++
        currentSpinCommand = SpinCommand(id = spinCommandId, number = targetNumber)
    }

    LaunchedEffect(isBettingPhase) {
        if (!isBettingPhase) {
            val forced = forcedNumberText.toIntOrNull()?.takeIf { it in 0..36 }
            requestSpin(forced)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = { AppHeader(title = "Roulette", onBackClick = onBackClick) },
            bottomBar = { AppBottomBar(items = bottomBarItems, selectedIndex = 0) }
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

                if (punchline != null) {
                    CroupierPunchlineBanner(punchline!!)
                }

                Spacer(modifier = Modifier.height(2.dp))

                // === Étape 1 : mise (plein écran) ===
                if (isBettingPhase) {
                    Column(
                    BalanceHeader(
                        amount = balance,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    if (isBettingPhase) {
                        // ============ BETTING PHASE ============
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Placez vos mises",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )

                            Text(
                                text = "Tap: placer  /  Appui long: retirer",
                                fontSize = 11.sp,
                                color = Color(0xFF888888)
                            )

                            // Zero cell (separate)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .border(1.dp, Color(0xFF2E7D32), RoundedCornerShape(4.dp, 4.dp, 0.dp, 0.dp))
                                    .background(Color(0xFF1B5E20), RoundedCornerShape(4.dp, 4.dp, 0.dp, 0.dp))
                                    .combinedClickable(
                                        onClick = { addNumberBet(0) },
                                        onLongClick = { removeNumberBet(0) }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("0", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                val betOnZero = numberBets[0]
                                if (betOnZero != null && betOnZero > 0) {
                                    BetBadge(amount = betOnZero, modifier = Modifier.align(Alignment.TopEnd))
                                }
                            }

                            // Main 12x3 grid with touch detection
                            val cellHeightDp = 36.dp
                            val badgeSizeDp = 24.dp
                            val badgeRadiusPx = with(density) { 12.dp.toPx() }

                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(cellHeightDp * 12)
                                    .pointerInput(Unit) {
                                        val cellWidthPx = size.width / 3f
                                        val cellHeightPx = size.height / 12f
                                        detectTapGestures(
                                            onTap = { offset ->
                                                val target = determineBetTarget(
                                                    offset.x, offset.y, cellWidthPx, cellHeightPx
                                                )
                                                handleGridTap(target, isRemove = false)
                                            },
                                            onLongPress = { offset ->
                                                val target = determineBetTarget(
                                                    offset.x, offset.y, cellWidthPx, cellHeightPx
                                                )
                                                handleGridTap(target, isRemove = true)
                                            }
                                        )
                                    }
                            ) {
                                val gridWidthPx = constraints.maxWidth.toFloat()
                                val gridHeightPx = constraints.maxHeight.toFloat()
                                val cellWidthPx = gridWidthPx / 3f
                                val cellHeightPx = gridHeightPx / 12f

                                // Draw grid cells
                                Column(modifier = Modifier.fillMaxSize()) {
                                    repeat(12) { rowIndex ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(cellHeightDp)
                                        ) {
                                            repeat(3) { colIndex ->
                                                val number = rowIndex * 3 + colIndex + 1
                                                val isRed = isRouletteRed(number)
                                                val baseColor = if (isRed) Color(0xFFB71C1C) else Color(0xFF212121)
                                                val borderColor = if (isRed) Color(0xFF8B0000) else Color(0xFF444444)

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .border(0.5.dp, borderColor)
                                                        .background(baseColor),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = number.toString(),
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color.White
                                                    )
                                                    val betAmount = numberBets[number]
                                                    if (betAmount != null && betAmount > 0) {
                                                        BetBadge(
                                                            amount = betAmount,
                                                            modifier = Modifier.align(Alignment.TopEnd)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Multi-bet overlay badges
                                multiBets.forEach { (key, amount) ->
                                    val numbers = key.split("_").map { it.toInt() }
                                    val (centerX, centerY) = multiBetCenterPx(
                                        numbers, cellWidthPx, cellHeightPx
                                    )
                                    val offsetX = with(density) { (centerX - badgeRadiusPx).toDp() }
                                    val offsetY = with(density) { (centerY - badgeRadiusPx).toDp() }
                                    Box(
                                        modifier = Modifier
                                            .offset(x = offsetX, y = offsetY)
                                            .size(badgeSizeDp)
                                            .background(Color(0xFFFF9800), CircleShape)
                                            .border(1.5.dp, Color.White, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = amount.toString(),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            // Dozens
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                listOf(
                                    "D_1ere 12" to "1ere 12",
                                    "D_2eme 12" to "2eme 12",
                                    "D_3eme 12" to "3eme 12"
                                ).forEach { (groupId, label) ->
                                    val betAmount = groupBets[groupId]
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .border(1.dp, Color(0xFF37474F), RoundedCornerShape(4.dp))
                                            .background(Color(0xFF263238), RoundedCornerShape(4.dp))
                                            .combinedClickable(
                                                onClick = { addGroupBet(groupId) },
                                                onLongClick = { removeGroupBet(groupId) }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.White)
                                        if (betAmount != null && betAmount > 0) {
                                            BetBadge(amount = betAmount, modifier = Modifier.align(Alignment.TopEnd))
                                        }
                                    }
                                }
                            }

                            // Special bets
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                listOf(
                                    "LOW" to "1-18",
                                    "PAIR" to "Pair",
                                    "RED" to "Rouge",
                                    "BLACK" to "Noir",
                                    "IMPAIR" to "Impair",
                                    "HIGH" to "19-36"
                                ).forEach { (groupId, label) ->
                                    val baseColor = when (groupId) {
                                        "RED" -> Color(0xFFB71C1C)
                                        "BLACK" -> Color(0xFF212121)
                                        else -> Color(0xFF263238)
                                    }
                                    val borderColor = when (groupId) {
                                        "RED" -> Color(0xFF8B0000)
                                        "BLACK" -> Color(0xFF444444)
                                        else -> Color(0xFF37474F)
                                    }
                                    val betAmount = groupBets[groupId]

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                                            .background(baseColor, RoundedCornerShape(4.dp))
                                            .combinedClickable(
                                                onClick = { addGroupBet(groupId) },
                                                onLongClick = { removeGroupBet(groupId) }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color.White)
                                        if (betAmount != null && betAmount > 0) {
                                            BetBadge(amount = betAmount, modifier = Modifier.align(Alignment.TopEnd))
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Chip selector
                            Text(
                                text = "Jeton : $selectedChipValue",
                                fontSize = 13.sp,
                                color = Color(0xFFB0B0B0)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                            ) {
                                chipDenominations.forEach { denomination ->
                                    val isSelected = selectedChipValue == denomination
                                    val color = chipColor(denomination)
                                    val chipSize = if (isSelected) 48.dp else 40.dp
                                    val borderWidth = if (isSelected) 3.dp else 1.dp
                                    val borderCol = if (isSelected) Color.White else color.copy(alpha = 0.5f)

                                    Box(
                                        modifier = Modifier
                                            .size(chipSize)
                                            .border(borderWidth, borderCol, CircleShape)
                                            .background(color, CircleShape)
                                            .clickable { selectedChipValue = denomination },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = denomination.toString(),
                                            fontSize = if (isSelected) 13.sp else 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            if (totalBet > 0) {
                                Text(
                                    text = "Mise totale : $totalBet",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }

                            // Action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (totalBet > 0) {
                                    AppButton(
                                        text = "Effacer",
                                        onClick = { clearAllBets() },
                                        variant = ButtonVariant.Secondary,
                                        size = ButtonSize.Medium,
                                        enabled = true,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                AppButton(
                                    text = "Valider les mises",
                                    onClick = { if (totalBet > 0) isBettingPhase = false },
                                    variant = ButtonVariant.Primary,
                                    size = ButtonSize.Medium,
                                    enabled = totalBet > 0,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    } else {
                        // ============ WHEEL + RESULT PHASE ============
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
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
                                    onExternalSpinConsumed = { currentSpinCommand = null },
                                    onNumberSelected = { number ->
                                        winningNumber = number

                                        val payout = calculateRoulettePayout(
                                            numberBets = numberBets,
                                            groupBets = groupBets,
                                            multiBets = multiBets,
                                            winningNumber = number
                                        )
                                        if (payout > 0) {
                                            BalanceState.addPinos(payout)
                                        }

                                        val hasWin = numberBets.containsKey(number)
                                                || groupBets.keys.any { isWinningGroup(it, number) }
                                                || multiBets.keys.any { key ->
                                            key.split("_").map { it.toInt() }.contains(number)
                                        }

                                        if (hasWin) {
                                            val durationSec = 3.0
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
                                        }
                                    }
                                )
                            }

                            // Bet summary
                            if (numberBets.isNotEmpty() || multiBets.isNotEmpty() || groupBets.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Number bets
                                    if (numberBets.isNotEmpty()) {
                                        val sorted = numberBets.entries.sortedBy { it.key }
                                        sorted.chunked(6).forEach { rowEntries ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                rowEntries.forEach { (number, amount) ->
                                                    val baseColor = when {
                                                        number == 0 -> Color(0xFF1B5E20)
                                                        isRouletteRed(number) -> Color(0xFFB71C1C)
                                                        else -> Color(0xFF212121)
                                                    }
                                                    val isWinner = winningNumber == number
                                                    RouletteBetChipStatic(
                                                        label = "$number\n$amount",
                                                        baseColor = baseColor,
                                                        isWinner = isWinner,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                                repeat(6 - rowEntries.size) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }

                                    // Multi bets (split/street/corner)
                                    if (multiBets.isNotEmpty()) {
                                        multiBets.entries.chunked(4).forEach { rowEntries ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                rowEntries.forEach { (key, amount) ->
                                                    val numbers = key.split("_").map { it.toInt() }
                                                    val label = numbers.joinToString("-")
                                                    val isWinner = winningNumber != null && winningNumber in numbers
                                                    val typeLabel = when (numbers.size) {
                                                        2 -> "Cheval"
                                                        3 -> "Trans."
                                                        4 -> "Carre"
                                                        else -> ""
                                                    }
                                                    RouletteBetChipStatic(
                                                        label = "$typeLabel\n$label\n$amount",
                                                        baseColor = Color(0xFF37474F),
                                                        isWinner = isWinner,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                                repeat(4 - rowEntries.size) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }

                                    // Group bets
                                    if (groupBets.isNotEmpty()) {
                                        val groupLabels = mapOf(
                                            "D_1ere 12" to "1ere 12", "D_2eme 12" to "2eme 12",
                                            "D_3eme 12" to "3eme 12",
                                            "PAIR" to "Pair", "IMPAIR" to "Impair",
                                            "LOW" to "1-18", "HIGH" to "19-36",
                                            "RED" to "Rouge", "BLACK" to "Noir"
                                        )
                                        groupBets.entries.chunked(4).forEach { rowEntries ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                rowEntries.forEach { (groupId, amount) ->
                                                    val label = groupLabels[groupId] ?: groupId
                                                    val baseColor = when (groupId) {
                                                        "RED" -> Color(0xFFB71C1C)
                                                        "BLACK" -> Color(0xFF212121)
                                                        else -> Color(0xFF263238)
                                                    }
                                                    val isWinner = winningNumber != null && isWinningGroup(groupId, winningNumber!!)
                                                    RouletteBetChipStatic(
                                                        label = "$label\n$amount",
                                                        baseColor = baseColor,
                                                        isWinner = isWinner,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                                repeat(4 - rowEntries.size) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            AppButton(
                                text = "Nouvelle mise",
                                onClick = {
                                    isBettingPhase = true
                                    numberBets = emptyMap()
                                    groupBets = emptyMap()
                                    multiBets = emptyMap()
                                    winningNumber = null
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

        ConfettiOverlay(
            parties = confettiParties,
            onFinished = { confettiParties = emptyList() },
            modifier = Modifier.fillMaxSize()
        )
    }
}

// --- Composables ---

@Composable
private fun BetBadge(
    amount: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(2.dp)
            .background(Color(0xFFFF9800), CircleShape)
            .padding(horizontal = 4.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = amount.toString(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
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
    val backgroundColor = if (isWinner) baseColor else baseColor.copy(alpha = 0.9f)
    val borderColor = if (isWinner) {
        AccentBlue
    } else if (baseColor == Color(0xFF212121)) {
        Color(0xFF444444)
    } else {
        darkenColor(baseColor, 0.75f)
    }

    Box(
        modifier = modifier
            .height(44.dp)
            .border(2.dp, borderColor, shape)
            .background(backgroundColor, shape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Medium,
            color = Color.White,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp
        )
    }
}

// --- Utility functions ---

private fun isRouletteRed(number: Int): Boolean {
    return number in setOf(
        1, 3, 5, 7, 9,
        12, 14, 16, 18,
        19, 21, 23, 25, 27,
        30, 32, 34, 36
    )
}

private fun calculateRoulettePayout(
    numberBets: Map<Int, Int>,
    groupBets: Map<String, Int>,
    multiBets: Map<String, Int>,
    winningNumber: Int
): Int {
    var payout = 0

    // Straight bets: 36x
    numberBets[winningNumber]?.let { payout += 36 * it }

    // Group bets
    groupBets.forEach { (groupId, amount) ->
        if (isWinningGroup(groupId, winningNumber)) {
            payout += if (groupId.startsWith("D_")) 3 * amount else 2 * amount
        }
    }

    // Multi-number bets (split/street/corner)
    multiBets.forEach { (key, amount) ->
        val numbers = key.split("_").map { it.toInt() }
        if (winningNumber in numbers) {
            payout += multiBetMultiplier(numbers.size) * amount
        }
    }

    return payout
}

private fun isWinningGroup(groupId: String, number: Int): Boolean {
    if (number !in 0..36) return false
    return when (groupId) {
        "PAIR" -> number != 0 && number % 2 == 0
        "IMPAIR" -> number % 2 == 1
        "LOW" -> number in 1..18
        "HIGH" -> number in 19..36
        "RED" -> isRouletteRed(number)
        "BLACK" -> number != 0 && !isRouletteRed(number)
        "D_1ere 12" -> number in 1..12
        "D_2eme 12" -> number in 13..24
        "D_3eme 12" -> number in 25..36
        else -> false
    }
}

private fun darkenColor(color: Color, factor: Float): Color {
    val clamped = factor.coerceIn(0f, 1f)
    return Color(
        red = (color.red * clamped).coerceIn(0f, 1f),
        green = (color.green * clamped).coerceIn(0f, 1f),
        blue = (color.blue * clamped).coerceIn(0f, 1f),
        alpha = color.alpha
    )
}
