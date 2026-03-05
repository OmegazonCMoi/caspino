package com.example.mobile.ui.screens

import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile.MainActivity
import com.example.mobile.network.GameStatDto
import com.example.mobile.network.PlayerProfileDto
import com.example.mobile.network.PlayerPredictionDto
import com.example.mobile.network.PlayersAnalysisResponse
import com.example.mobile.network.StatsApi
import com.example.mobile.network.StatsPlatformResponse
import com.example.mobile.ui.components.AppBottomBar
import com.example.mobile.ui.components.AppHeader
import com.example.mobile.ui.components.BottomBarItem
import com.example.mobile.ui.icons.AppIcons
import com.example.mobile.ui.theme.AccentBlue
import com.example.mobile.ui.theme.AccentGreen
import com.example.mobile.ui.theme.AccentOrange
import com.example.mobile.ui.theme.AccentPurple
import com.example.mobile.ui.theme.AccentRed
import com.example.mobile.ui.theme.DarkBorder
import com.example.mobile.ui.theme.DarkSurface
import com.example.mobile.ui.theme.DarkSurfaceVariant
import com.example.mobile.ui.theme.DarkTextPrimary
import com.example.mobile.ui.theme.DarkTextSecondary
import com.example.mobile.ui.theme.DarkTextTertiary

private data class PlatformGameStat(
    val name: String,
    val sessions24h: Int,
    val uniquePlayers24h: Int,
    val betVolume24h: Int,
    val ggr24h: Int,
    val payoutRate: Int,
    val color: Color
)

private data class PeakHour(
    val hour: String,
    val sessions: Int,
    val ggr: Int
)

@Composable
fun StatsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val bottomBarItems = listOf(
        BottomBarItem(AppIcons.Home, AppIcons.HomeFilled) {
            context.startActivity(Intent(context, MainActivity::class.java))
        },
        BottomBarItem(AppIcons.Search, AppIcons.SearchFilled) { },
        BottomBarItem(AppIcons.Profile, AppIcons.ProfileFilled) {
            context.startActivity(Intent(context, com.example.mobile.AccountActivity::class.java))
        },
        BottomBarItem(AppIcons.Cart, AppIcons.CartFilled) {
            context.startActivity(Intent(context, com.example.mobile.ShopActivity::class.java))
        }
    )

    var stats by remember { mutableStateOf<StatsPlatformResponse?>(null) }
    var analysis by remember { mutableStateOf<PlayersAnalysisResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val result = StatsApi.fetchPlatformStats()
        result.onSuccess {
            stats = it
            isLoading = false
        }.onFailure {
            error = "Impossible de charger les statistiques."
            isLoading = false
        }

        StatsApi.fetchPlayersAnalysis().onSuccess { analysis = it }
    }

    Scaffold(
        topBar = { AppHeader(title = "Stats Caspino", onBackClick = onBackClick) },
        bottomBar = { AppBottomBar(items = bottomBarItems, selectedIndex = 1) }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Chargement des statistiques...", color = DarkTextSecondary)
            }
        } else if (error != null || stats == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(text = error ?: "Erreur inconnue", color = AccentRed)
            }
        } else {
            val data = stats!!

            val coloredGames = mapGamesWithColors(data.games)
            val totalSessions = coloredGames.sumOf { it.sessions24h }
            val totalPlayers = coloredGames.sumOf { it.uniquePlayers24h }.coerceAtLeast(1)
            val totalBetVolume = coloredGames.sumOf { it.betVolume24h }
            val totalGgr = coloredGames.sumOf { it.ggr24h }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard("Sessions 24h", totalSessions.toString(), Modifier.weight(1f))
                    KpiCard("Joueurs 24h", totalPlayers.toString(), Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard("Volume 24h", "${totalBetVolume / 1000}k", Modifier.weight(1f), AccentPurple)
                    KpiCard("GGR 24h", "${totalGgr / 1000}k", Modifier.weight(1f), AccentGreen)
                }

                SectionTitle("Tendance GGR (7 jours)")
                GgrLineChart(data.ggrTrend7d.map { it.ggr })

                SectionTitle("Répartition du trafic")
                if (totalSessions > 0) {
                    TrafficShareDonut(stats = coloredGames, totalSessions = totalSessions)
                }

                SectionTitle("Performance par jeu")
                GamePerformanceBars(coloredGames)

                SectionTitle("Heures fortes")
                PeakHoursTable(
                    data.peakHours.map {
                        PeakHour(
                            hour = it.label,
                            sessions = it.sessions,
                            ggr = it.ggr
                        )
                    }
                )

                if (analysis != null && analysis!!.profiles.isNotEmpty()) {
                    SectionTitle("Profilage joueurs (IA)")
                    PlayerProfilesTable(analysis!!.profiles)

                    SectionTitle("Prédictions (régression)")
                    PlayerPredictionsTable(analysis!!.predictions)
                }

                SectionTitle("Synthèse plateforme")
                SummaryPanel(
                    payoutRate = data.summary.payoutRate,
                    activeGames = data.summary.activeGames,
                    avgSessionPerPlayer = data.summary.avgSessionPerPlayer
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = DarkTextTertiary,
        letterSpacing = 1.2.sp
    )
}

@Composable
private fun KpiCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = DarkTextPrimary
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
        Spacer(Modifier.height(2.dp))
        Text(text = label, fontSize = 11.sp, color = DarkTextSecondary)
    }
}

@Composable
private fun GgrLineChart(data: List<Int>) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(1100, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .padding(14.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (data.size < 2) return@Canvas
            val min = data.min().toFloat()
            val max = data.max().toFloat()
            val range = (max - min).coerceAtLeast(1f)
            val visible = (data.size * progress.value).toInt().coerceAtLeast(2)
            val stepX = size.width / (data.size - 1).toFloat()

            repeat(4) { i ->
                val y = size.height * (i / 3f)
                drawLine(
                    color = DarkBorder,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 0.6f
                )
            }

            val path = Path()
            val fill = Path()
            for (i in 0 until visible) {
                val x = i * stepX
                val y = size.height - ((data[i] - min) / range) * size.height
                if (i == 0) {
                    path.moveTo(x, y)
                    fill.moveTo(x, size.height)
                    fill.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fill.lineTo(x, y)
                }
            }
            val lastX = (visible - 1) * stepX
            fill.lineTo(lastX, size.height)
            fill.close()

            drawPath(
                path = fill,
                brush = Brush.verticalGradient(
                    listOf(AccentGreen.copy(alpha = 0.25f), Color.Transparent)
                )
            )
            drawPath(
                path = path,
                color = AccentGreen,
                style = Stroke(width = 2.5f, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun TrafficShareDonut(stats: List<PlatformGameStat>, totalSessions: Int) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(900, easing = FastOutSlowInEasing))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(108.dp)) {
            val stroke = 12.dp.toPx()
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(stroke / 2f, stroke / 2f)
            var start = -90f

            stats.forEach { game ->
                val fraction = game.sessions24h.toFloat() / totalSessions.toFloat()
                val sweep = 360f * fraction * progress.value
                drawArc(
                    color = game.color,
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt)
                )
                start += 360f * fraction
            }
        }

        Spacer(Modifier.width(18.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            stats.forEach { game ->
                val share = (game.sessions24h * 100f / totalSessions).toInt()
                LegendRow(game.color, game.name, "$share%")
            }
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Text(text = label, fontSize = 12.sp, color = DarkTextSecondary)
        Spacer(Modifier.width(6.dp))
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DarkTextPrimary)
    }
}

@Composable
private fun GamePerformanceBars(stats: List<PlatformGameStat>) {
    val maxGgr = stats.maxOf { it.ggr24h }.toFloat().coerceAtLeast(1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        stats.forEach { game ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = game.name,
                        color = DarkTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "GGR ${game.ggr24h / 1000}k",
                        color = AccentGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(DarkSurfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((game.ggr24h / maxGgr).coerceIn(0f, 1f))
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(game.color)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${game.sessions24h} sessions",
                        fontSize = 11.sp,
                        color = DarkTextSecondary
                    )
                    Text(
                        text = "Payout ${game.payoutRate}%",
                        fontSize = 11.sp,
                        color = DarkTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun PeakHoursTable(rows: List<PeakHour>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurfaceVariant)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text("Créneau", modifier = Modifier.weight(1f), fontSize = 11.sp, color = DarkTextTertiary)
            Text("Sessions", modifier = Modifier.weight(1f), fontSize = 11.sp, color = DarkTextTertiary)
            Text("GGR", modifier = Modifier.weight(1f), fontSize = 11.sp, color = DarkTextTertiary)
        }

        rows.forEachIndexed { index, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(row.hour, modifier = Modifier.weight(1f), fontSize = 12.sp, color = DarkTextPrimary)
                Text(row.sessions.toString(), modifier = Modifier.weight(1f), fontSize = 12.sp, color = DarkTextSecondary)
                Text("${row.ggr / 1000}k", modifier = Modifier.weight(1f), fontSize = 12.sp, color = AccentGreen)
            }
            if (index != rows.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .height(0.5.dp)
                        .background(DarkBorder)
                )
            }
        }
    }
}

@Composable
private fun SummaryPanel(
    payoutRate: Int,
    activeGames: Int,
    avgSessionPerPlayer: Float
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SummaryRow("Jeux actifs", activeGames.toString(), AccentBlue)
        SummaryRow("RTP global estimé", "$payoutRate%", AccentGreen)
        SummaryRow("Sessions / joueur", String.format("%.1f", avgSessionPerPlayer), AccentOrange)
        SummaryRow("Scope", "Blackjack / Roulette / Slot", DarkTextSecondary)
        SummaryRow("Sources", "mock local (DB à brancher)", AccentRed)
    }
}

@Composable
private fun SummaryRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = DarkTextSecondary)
        Text(text = value, fontSize = 12.sp, color = valueColor, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PlayerProfilesTable(profiles: List<PlayerProfileDto>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurfaceVariant)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text("Joueur", modifier = Modifier.weight(1.2f), fontSize = 11.sp, color = DarkTextTertiary)
            Text("Profil", modifier = Modifier.weight(1f), fontSize = 11.sp, color = DarkTextTertiary)
            Text("Mise moy.", modifier = Modifier.weight(0.8f), fontSize = 11.sp, color = DarkTextTertiary)
            Text("Win%", modifier = Modifier.weight(0.6f), fontSize = 11.sp, color = DarkTextTertiary)
        }

        profiles.forEachIndexed { index, p ->
            val profileColor = when (p.profile) {
                "flambeur" -> AccentOrange
                "malchanceux" -> AccentRed
                else -> AccentGreen
            }
            val profileLabel = when (p.profile) {
                "flambeur" -> "Flambeur"
                "malchanceux" -> "Malchanceux"
                else -> "Prudent"
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    p.username,
                    modifier = Modifier.weight(1.2f),
                    fontSize = 12.sp,
                    color = DarkTextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    profileLabel,
                    modifier = Modifier.weight(1f),
                    fontSize = 12.sp,
                    color = profileColor,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${p.avgBet}",
                    modifier = Modifier.weight(0.8f),
                    fontSize = 12.sp,
                    color = DarkTextSecondary
                )
                Text(
                    "${p.winRate}%",
                    modifier = Modifier.weight(0.6f),
                    fontSize = 12.sp,
                    color = if (p.winRate >= 50) AccentGreen else AccentRed,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (index != profiles.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .height(0.5.dp)
                        .background(DarkBorder)
                )
            }
        }
    }
}

@Composable
private fun PlayerPredictionsTable(predictions: List<PlayerPredictionDto>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurfaceVariant)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text("Joueur", modifier = Modifier.weight(1.2f), fontSize = 11.sp, color = DarkTextTertiary)
            Text("Win% prédit", modifier = Modifier.weight(1f), fontSize = 11.sp, color = DarkTextTertiary)
            Text("Sessions est.", modifier = Modifier.weight(0.9f), fontSize = 11.sp, color = DarkTextTertiary)
            Text("Tendance", modifier = Modifier.weight(0.8f), fontSize = 11.sp, color = DarkTextTertiary)
        }

        predictions.forEachIndexed { index, p ->
            val trendSymbol = when (p.trend) {
                "up" -> "\u2197"
                "down" -> "\u2198"
                else -> "\u2192"
            }
            val trendColor = when (p.trend) {
                "up" -> AccentGreen
                "down" -> AccentRed
                else -> AccentOrange
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    p.username,
                    modifier = Modifier.weight(1.2f),
                    fontSize = 12.sp,
                    color = DarkTextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${p.predictedWinRate}%",
                    modifier = Modifier.weight(1f),
                    fontSize = 12.sp,
                    color = AccentBlue,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${p.estimatedSessionsNextWeek}/sem",
                    modifier = Modifier.weight(0.9f),
                    fontSize = 12.sp,
                    color = DarkTextSecondary
                )
                Text(
                    "$trendSymbol ${p.trend.replaceFirstChar { it.uppercase() }}",
                    modifier = Modifier.weight(0.8f),
                    fontSize = 12.sp,
                    color = trendColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (index != predictions.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .height(0.5.dp)
                        .background(DarkBorder)
                )
            }
        }
    }
}

private fun mapGamesWithColors(games: List<GameStatDto>): List<PlatformGameStat> {
    return games.map { game ->
        val color = when (game.gameType) {
            "blackjack" -> AccentBlue
            "roulette" -> AccentGreen
            "slot" -> AccentOrange
            else -> DarkTextPrimary
        }
        PlatformGameStat(
            name = game.name,
            sessions24h = game.sessions24h,
            uniquePlayers24h = game.uniquePlayers24h,
            betVolume24h = game.betVolume24h,
            ggr24h = game.ggr24h,
            payoutRate = game.payoutRate,
            color = color
        )
    }
}
