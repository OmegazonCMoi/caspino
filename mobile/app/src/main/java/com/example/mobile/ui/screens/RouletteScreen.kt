package com.example.mobile.ui.screens

import android.media.MediaPlayer
import android.content.Intent
import com.example.mobile.network.ApiClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.mobile.network.RouletteApi
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
import com.example.mobile.R
import com.example.mobile.ui.theme.AccentBlue
import com.example.mobile.ui.icons.AppIcons
import com.example.mobile.network.PunchlineApi
import kotlinx.coroutines.launch
import io.github.vinceglb.confettikit.core.Party
import io.github.vinceglb.confettikit.core.Position
import io.github.vinceglb.confettikit.core.emitter.Emitter
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import kotlin.time.Duration.Companion.seconds

// Chip denominations and their colors
internal val chipDenominations = listOf(10, 25, 50, 100, 500)

internal fun chipColor(value: Int): Color = when (value) {
    10 -> Color(0xFF1565C0)
    25 -> Color(0xFF2E7D32)
    50 -> Color(0xFFE65100)
    100 -> Color(0xFF212121)
    500 -> Color(0xFF6A1B9A)
    else -> Color.Gray
}

internal fun numberAt(row: Int, col: Int) = row * 3 + col + 1

internal const val EDGE_THRESHOLD = 0.28f

internal fun determineBetTarget(
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
        return listOf(numberAt(row - 1, col - 1), numberAt(row - 1, col), numberAt(row, col - 1), numberAt(row, col)).sorted()
    }
    if (nearTop && nearRight && row > 0 && col < 2) {
        return listOf(numberAt(row - 1, col), numberAt(row - 1, col + 1), numberAt(row, col), numberAt(row, col + 1)).sorted()
    }
    if (nearBottom && nearLeft && row < 11 && col > 0) {
        return listOf(numberAt(row, col - 1), numberAt(row, col), numberAt(row + 1, col - 1), numberAt(row + 1, col)).sorted()
    }
    if (nearBottom && nearRight && row < 11 && col < 2) {
        return listOf(numberAt(row, col), numberAt(row, col + 1), numberAt(row + 1, col), numberAt(row + 1, col + 1)).sorted()
    }

    // Horizontal split
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

    // Vertical split
    if (nearTop && !nearLeft && !nearRight && row > 0) {
        return listOf(numberAt(row - 1, col), numberAt(row, col)).sorted()
    }
    if (nearBottom && !nearLeft && !nearRight && row < 11) {
        return listOf(numberAt(row, col), numberAt(row + 1, col)).sorted()
    }

    return listOf(numberAt(row, col))
}

internal fun multiBetCenterPx(
    numbers: List<Int>,
    cellWidthPx: Float,
    cellHeightPx: Float
): Pair<Float, Float> {
    if (numbers.size == 3) {
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

@Composable
fun RouletteScreen(
    onBackClick: () -> Unit
) {
    var spinCommandId by remember { mutableIntStateOf(0) }
    var currentSpinCommand by remember { mutableStateOf<SpinCommand?>(null) }
    var wheelFinished by remember { mutableStateOf(false) }

    var balance by BalanceState.balance
    var winningNumber by remember { mutableStateOf<Int?>(null) }
    var confettiParties by remember { mutableStateOf<List<Party>>(emptyList()) }
    var punchline by remember { mutableStateOf<String?>(null) }
    var lastPayout by remember { mutableIntStateOf(0) }
    var betsSentThisRound by remember { mutableStateOf(false) }
    var totalBetSentThisRound by remember { mutableIntStateOf(0) }

    // Pending bet result to apply after wheel stops
    data class PendingBetResult(val gains: Int, val balance: Int)
    var pendingBetResult by remember { mutableStateOf<PendingBetResult?>(null) }

    // Server sync state
    var serverPhase by remember { mutableStateOf("BETTING") }
    var phaseEndsAt by remember { mutableLongStateOf(0L) }
    var timeLeftSec by remember { mutableIntStateOf(0) }
    var isConnected by remember { mutableStateOf(false) }

    // Chip-based betting state (local until sent to server)
    var numberBets by remember { mutableStateOf(mapOf<Int, Int>()) }
    var groupBets by remember { mutableStateOf(mapOf<String, Int>()) }
    var multiBets by remember { mutableStateOf(mapOf<String, Int>()) }
    var selectedChipValue by remember { mutableIntStateOf(10) }
    var isEraserMode by remember { mutableStateOf(false) }

    val totalBet = numberBets.values.sum() + groupBets.values.sum() + multiBets.values.sum()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        scope.launch {
            PunchlineApi.fetchPunchline("roulette")
                .onSuccess { punchline = it }
        }
    }

    val confettiMediaPlayer = remember {
        MediaPlayer.create(context, R.raw.confetti_sound)
    }

    val bottomBarItems = listOf(
        BottomBarItem(icon = AppIcons.Home, selectedIcon = AppIcons.HomeFilled) {
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

    // --- Bet helpers (local, deducted from displayed balance) ---
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

    fun handleGridTap(numbers: List<Int>) {
        if (isEraserMode) {
            if (numbers.size == 1) removeNumberBet(numbers[0]) else removeMultiBet(numbers)
        } else {
            if (numbers.size == 1) addNumberBet(numbers[0]) else addMultiBet(numbers)
        }
    }

    fun handleNumberTap(number: Int) {
        if (isEraserMode) removeNumberBet(number) else addNumberBet(number)
    }

    fun handleGroupTap(groupId: String) {
        if (isEraserMode) removeGroupBet(groupId) else addGroupBet(groupId)
    }

    /** Convert local bets to server JSON format and send via WebSocket */
    fun sendBetsToServer() {
        if (totalBet == 0 || betsSentThisRound) return

        val betsArray = JSONArray()

        // NumberBets → NumberBet { numbers: [n], amount }
        numberBets.forEach { (number, amount) ->
            betsArray.put(JSONObject().apply {
                put("numbers", JSONArray().apply { put(number) })
                put("amount", amount)
            })
        }

        // MultiBets → NumberBet { numbers: [...], amount }
        multiBets.forEach { (key, amount) ->
            val numbersArray = JSONArray()
            key.split("_").forEach { numbersArray.put(it.toInt()) }
            betsArray.put(JSONObject().apply {
                put("numbers", numbersArray)
                put("amount", amount)
            })
        }

        // GroupBets → TwoTimesBet or ThreeTimesBet
        groupBets.forEach { (groupId, amount) ->
            when (groupId) {
                "D_1ere 12" -> betsArray.put(JSONObject().apply { put("dozen", "1-12"); put("amount", amount) })
                "D_2eme 12" -> betsArray.put(JSONObject().apply { put("dozen", "13-24"); put("amount", amount) })
                "D_3eme 12" -> betsArray.put(JSONObject().apply { put("dozen", "25-36"); put("amount", amount) })
                "RED" -> betsArray.put(JSONObject().apply { put("choice", "red"); put("amount", amount) })
                "BLACK" -> betsArray.put(JSONObject().apply { put("choice", "black"); put("amount", amount) })
                "PAIR" -> betsArray.put(JSONObject().apply { put("choice", "even"); put("amount", amount) })
                "IMPAIR" -> betsArray.put(JSONObject().apply { put("choice", "odd"); put("amount", amount) })
                "LOW" -> betsArray.put(JSONObject().apply { put("choice", "1-18"); put("amount", amount) })
                "HIGH" -> betsArray.put(JSONObject().apply { put("choice", "19-36"); put("amount", amount) })
            }
        }

        RouletteApi.placeBets(betsArray)
        betsSentThisRound = true
        totalBetSentThisRound = totalBet
    }

    // --- WebSocket connection & event handling ---
    DisposableEffect(Unit) {
        RouletteApi.onPhaseUpdate = { update ->
            scope.launch {
                serverPhase = update.phase
                phaseEndsAt = if (update.durationMs > 0) {
                    System.currentTimeMillis() + update.durationMs
                } else {
                    update.endsAt
                }

                when (update.phase) {
                    "BETTING" -> {
                        // New round: reset local state
                        winningNumber = null
                        lastPayout = 0
                        betsSentThisRound = false
                        wheelFinished = false
                        pendingBetResult = null
                        numberBets = emptyMap()
                        groupBets = emptyMap()
                        multiBets = emptyMap()
                    }
                    "SPINNING", "RESULT" -> {
                        // If joining mid-round, trigger wheel spin with the winning number
                        val number = update.winningNumber
                        if (number != null && winningNumber == null) {
                            winningNumber = number
                            spinCommandId++
                            currentSpinCommand = SpinCommand(id = spinCommandId, number = number)
                        }
                    }
                }
            }
        }

        // Winning number broadcast to ALL clients → triggers wheel spin
        RouletteApi.onRoundResult = { number ->
            scope.launch {
                winningNumber = number
                spinCommandId++
                currentSpinCommand = SpinCommand(id = spinCommandId, number = number)
            }
        }

        // Individual gains for players who bet — store pending, apply after wheel stops
        RouletteApi.onBetResult = { result ->
            scope.launch {
                pendingBetResult = PendingBetResult(gains = result.gains, balance = result.balance)
            }
        }

        RouletteApi.onError = { _ ->
            scope.launch {
                if (totalBet > 0) {
                    BalanceState.addPinos(totalBet)
                    numberBets = emptyMap()
                    groupBets = emptyMap()
                    multiBets = emptyMap()
                    betsSentThisRound = false
                }
            }
        }

        scope.launch {
            isConnected = RouletteApi.connect()
        }

        onDispose {
            RouletteApi.onPhaseUpdate = null
            RouletteApi.onRoundResult = null
            RouletteApi.onBetResult = null
            RouletteApi.onError = null
            RouletteApi.disconnect()
            try { confettiMediaPlayer.release() } catch (_: Exception) { }
        }
    }

    // Countdown timer synced with server
    LaunchedEffect(phaseEndsAt) {
        while (true) {
            val remaining = ((phaseEndsAt - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
            timeLeftSec = remaining
            if (remaining <= 0) break
            delay(500L)
        }
    }

    // Auto-send bets when SPINNING starts (server says betting is closed)
    LaunchedEffect(serverPhase) {
        if (serverPhase == "SPINNING" && !betsSentThisRound && totalBet > 0) {
            sendBetsToServer()
        }
    }

    // Apply pending bet result only after the wheel animation finishes
    LaunchedEffect(wheelFinished, pendingBetResult) {
        val pending = pendingBetResult ?: return@LaunchedEffect
        if (!wheelFinished) return@LaunchedEffect

        lastPayout = pending.gains

        // Sync balance from server
        BalanceState.setBalance(pending.balance)
        ApiClient.saveBalance(pending.balance)

        // Confetti + sound only when gains >= 2x total bet
        if (pending.gains >= 2 * totalBetSentThisRound && totalBetSentThisRound > 0) {
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
            try { confettiMediaPlayer.seekTo(0); confettiMediaPlayer.start() } catch (_: Exception) { }
        }

        pendingBetResult = null
    }

    val isBetting = serverPhase == "BETTING"
    val isSpinning = serverPhase == "SPINNING"
    val isResult = serverPhase == "RESULT"

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
                        .then(if (isBetting || wheelFinished) Modifier.verticalScroll(scrollState) else Modifier)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
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

                    when {
                        isBetting -> {
                            // ============ BETTING PHASE ============
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Banner + countdown
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Faites vos jeux",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    if (timeLeftSec <= 10) {
                                        Text(
                                            text = "${timeLeftSec}s",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (timeLeftSec <= 5) Color(0xFFEF5350) else Color(0xFFFF9800)
                                        )
                                    }
                                }

                                if (!isConnected) {
                                    Text(
                                        text = "Connexion en cours...",
                                        fontSize = 12.sp,
                                        color = Color(0xFFFF9800)
                                    )
                                }

                                // Table + chip toolbar side by side
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Left: roulette table
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(0.dp)
                                    ) {
                                        // Zero cell
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(36.dp)
                                                .border(1.dp, Color(0xFF2E7D32), RoundedCornerShape(4.dp, 4.dp, 0.dp, 0.dp))
                                                .background(Color(0xFF1B5E20), RoundedCornerShape(4.dp, 4.dp, 0.dp, 0.dp))
                                                .clickable { handleNumberTap(0) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("0", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            val betOnZero = numberBets[0]
                                            if (betOnZero != null && betOnZero > 0) {
                                                BetBadge(amount = betOnZero)
                                            }
                                        }

                                        // Main 12x3 grid
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
                                                            handleGridTap(target)
                                                        }
                                                    )
                                                }
                                        ) {
                                            val cellWidthPx = constraints.maxWidth.toFloat() / 3f
                                            val cellHeightPx = constraints.maxHeight.toFloat() / 12f

                                            Column(modifier = Modifier.fillMaxSize()) {
                                                repeat(12) { rowIndex ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().height(cellHeightDp)
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
                                                                Text(number.toString(), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White)
                                                                val betAmount = numberBets[number]
                                                                if (betAmount != null && betAmount > 0) {
                                                                    BetBadge(amount = betAmount)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            // Multi-bet overlay badges
                                            multiBets.forEach { (key, amount) ->
                                                val numbers = key.split("_").map { it.toInt() }
                                                val (centerX, centerY) = multiBetCenterPx(numbers, cellWidthPx, cellHeightPx)
                                                val badgeOffsetX = with(density) { (centerX - badgeRadiusPx).toDp() }
                                                val badgeOffsetY = with(density) { (centerY - badgeRadiusPx).toDp() }
                                                Box(
                                                    modifier = Modifier
                                                        .offset(x = badgeOffsetX, y = badgeOffsetY)
                                                        .size(badgeSizeDp)
                                                        .background(Color(0xFFFF9800), CircleShape)
                                                        .border(1.5.dp, Color.White, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(amount.toString(), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                            }
                                        }

                                        // Dozens
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            listOf("D_1ere 12" to "1ere 12", "D_2eme 12" to "2eme 12", "D_3eme 12" to "3eme 12").forEach { (groupId, label) ->
                                                val betAmount = groupBets[groupId]
                                                Box(
                                                    modifier = Modifier.weight(1f).height(34.dp)
                                                        .border(0.5.dp, Color(0xFF37474F)).background(Color(0xFF263238))
                                                        .clickable { handleGroupTap(groupId) },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color.White)
                                                    if (betAmount != null && betAmount > 0) { BetBadge(amount = betAmount) }
                                                }
                                            }
                                        }

                                        // Special bets
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            listOf("LOW" to "1-18", "PAIR" to "Pair", "RED" to "Rouge", "BLACK" to "Noir", "IMPAIR" to "Impair", "HIGH" to "19-36").forEach { (groupId, label) ->
                                                val baseColor = when (groupId) { "RED" -> Color(0xFFB71C1C); "BLACK" -> Color(0xFF212121); else -> Color(0xFF263238) }
                                                val borderColor = when (groupId) { "RED" -> Color(0xFF8B0000); "BLACK" -> Color(0xFF444444); else -> Color(0xFF37474F) }
                                                val betAmount = groupBets[groupId]
                                                Box(
                                                    modifier = Modifier.weight(1f).height(34.dp)
                                                        .border(0.5.dp, borderColor).background(baseColor)
                                                        .clickable { handleGroupTap(groupId) },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(label, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = Color.White)
                                                    if (betAmount != null && betAmount > 0) { BetBadge(amount = betAmount) }
                                                }
                                            }
                                        }
                                    }

                                    // Right: chip toolbar
                                    Column(
                                        modifier = Modifier.padding(top = 36.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // Eraser
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .border(if (isEraserMode) 3.dp else 1.dp, if (isEraserMode) Color.White else Color(0xFF666666), CircleShape)
                                                .background(if (isEraserMode) Color(0xFFE53935) else Color(0xFF424242), CircleShape)
                                                .clickable { isEraserMode = !isEraserMode },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("X", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        chipDenominations.forEach { denomination ->
                                            val isSelected = !isEraserMode && selectedChipValue == denomination
                                            val color = chipColor(denomination)
                                            val chipSize = if (isSelected) 44.dp else 38.dp
                                            Box(
                                                modifier = Modifier
                                                    .size(chipSize)
                                                    .border(if (isSelected) 3.dp else 1.dp, if (isSelected) Color.White else color.copy(alpha = 0.5f), CircleShape)
                                                    .background(color, CircleShape)
                                                    .clickable { selectedChipValue = denomination; isEraserMode = false },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(denomination.toString(), fontSize = if (isSelected) 12.sp else 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }
                                }

                                // Total + buttons
                                if (totalBet > 0) {
                                    Text("Mise totale : $totalBet", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                }

                                if (totalBet > 0) {
                                    AppButton(
                                        text = "Effacer tout",
                                        onClick = { clearAllBets() },
                                        variant = ButtonVariant.Secondary,
                                        size = ButtonSize.Medium,
                                        enabled = true,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        isSpinning || isResult -> {
                            // ============ SPINNING + RESULT PHASE ============
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (!wheelFinished) "Les jeux sont faits" else "Resultat",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    if (isResult && timeLeftSec > 0) {
                                        Text(
                                            text = "${timeLeftSec}s",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF888888)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier.fillMaxWidth().height(wheelHeight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    RouletteWheel(
                                        isConnected = true,
                                        externalSpinCommand = currentSpinCommand,
                                        onExternalSpinConsumed = { currentSpinCommand = null },
                                        onNumberSelected = { wheelFinished = true }
                                    )
                                }

                                if (!wheelFinished) {
                                    Text(
                                        text = "La roue tourne...",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFB0B0B0)
                                    )
                                } else {
                                    // Show result once wheel stops
                                    if (winningNumber != null) {
                                        val winColor = when {
                                            winningNumber == 0 -> Color(0xFF1B5E20)
                                            isRouletteRed(winningNumber!!) -> Color(0xFFB71C1C)
                                            else -> Color(0xFF212121)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(72.dp)
                                                .background(winColor, CircleShape)
                                                .border(3.dp, Color.White, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(winningNumber.toString(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }

                                        if (!betsSentThisRound) {
                                            Text("Aucune mise", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color(0xFF888888))
                                        } else {
                                            val netResult = lastPayout - totalBetSentThisRound
                                            Text("Mise : $totalBetSentThisRound", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFFB0B0B0))
                                            Text("Gains : $lastPayout", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFFB0B0B0))
                                            when {
                                                netResult > 0 -> Text("+$netResult", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                                                netResult == 0 -> Text("0", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF888888))
                                                else -> Text("$netResult", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFEF5350))
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "En attente du resultat...",
                                            fontSize = 16.sp,
                                            color = Color(0xFFB0B0B0)
                                        )
                                    }

                                    // Bet summary
                                    if (betsSentThisRound && (numberBets.isNotEmpty() || multiBets.isNotEmpty() || groupBets.isNotEmpty())) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (numberBets.isNotEmpty()) {
                                                numberBets.entries.sortedBy { it.key }.chunked(6).forEach { rowEntries ->
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        rowEntries.forEach { (number, amount) ->
                                                            val baseColor = when { number == 0 -> Color(0xFF1B5E20); isRouletteRed(number) -> Color(0xFFB71C1C); else -> Color(0xFF212121) }
                                                            RouletteBetChipStatic(label = "$number\n$amount", baseColor = baseColor, isWinner = winningNumber == number, modifier = Modifier.weight(1f))
                                                        }
                                                        repeat(6 - rowEntries.size) { Spacer(modifier = Modifier.weight(1f)) }
                                                    }
                                                }
                                            }

                                            if (multiBets.isNotEmpty()) {
                                                multiBets.entries.chunked(4).forEach { rowEntries ->
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        rowEntries.forEach { (key, amount) ->
                                                            val numbers = key.split("_").map { it.toInt() }
                                                            val typeLabel = when (numbers.size) { 2 -> "Cheval"; 3 -> "Trans."; 4 -> "Carre"; else -> "" }
                                                            RouletteBetChipStatic(
                                                                label = "$typeLabel\n${numbers.joinToString("-")}\n$amount",
                                                                baseColor = Color(0xFF37474F),
                                                                isWinner = winningNumber != null && winningNumber in numbers,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                        }
                                                        repeat(4 - rowEntries.size) { Spacer(modifier = Modifier.weight(1f)) }
                                                    }
                                                }
                                            }

                                            if (groupBets.isNotEmpty()) {
                                                val groupLabels = mapOf(
                                                    "D_1ere 12" to "1ere 12", "D_2eme 12" to "2eme 12", "D_3eme 12" to "3eme 12",
                                                    "PAIR" to "Pair", "IMPAIR" to "Impair", "LOW" to "1-18", "HIGH" to "19-36", "RED" to "Rouge", "BLACK" to "Noir"
                                                )
                                                groupBets.entries.chunked(4).forEach { rowEntries ->
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        rowEntries.forEach { (groupId, amount) ->
                                                            val label = groupLabels[groupId] ?: groupId
                                                            val baseColor = when (groupId) { "RED" -> Color(0xFFB71C1C); "BLACK" -> Color(0xFF212121); else -> Color(0xFF263238) }
                                                            val isWinner = winningNumber != null && isWinningGroupForDisplay(groupId, winningNumber!!)
                                                            RouletteBetChipStatic(label = "$label\n$amount", baseColor = baseColor, isWinner = isWinner, modifier = Modifier.weight(1f))
                                                        }
                                                        repeat(4 - rowEntries.size) { Spacer(modifier = Modifier.weight(1f)) }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                }

                            }
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
            .size(22.dp)
            .background(Color(0xFFFF9800), CircleShape)
            .border(1.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(amount.toString(), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
    val borderColor = if (isWinner) AccentBlue
    else if (baseColor == Color(0xFF212121)) Color(0xFF444444)
    else darkenColor(baseColor, 0.75f)

    Box(
        modifier = modifier
            .height(44.dp)
            .border(2.dp, borderColor, shape)
            .background(backgroundColor, shape),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 10.sp, fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Medium, color = Color.White, textAlign = TextAlign.Center, lineHeight = 12.sp)
    }
}

// --- Utility functions ---

internal fun isRouletteRed(number: Int): Boolean {
    return number in setOf(1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36)
}

internal fun isWinningGroupForDisplay(groupId: String, number: Int): Boolean {
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

internal fun darkenColor(color: Color, factor: Float): Color {
    val clamped = factor.coerceIn(0f, 1f)
    return Color(
        red = (color.red * clamped).coerceIn(0f, 1f),
        green = (color.green * clamped).coerceIn(0f, 1f),
        blue = (color.blue * clamped).coerceIn(0f, 1f),
        alpha = color.alpha
    )
}
