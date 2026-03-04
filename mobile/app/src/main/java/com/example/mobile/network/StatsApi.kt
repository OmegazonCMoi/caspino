package com.example.mobile.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

data class GameStatDto(
    val gameType: String,
    val name: String,
    val sessions24h: Int,
    val uniquePlayers24h: Int,
    val betVolume24h: Int,
    val ggr24h: Int,
    val payoutRate: Int
)

data class GgrPointDto(
    val day: String,
    val ggr: Int
)

data class PeakHourDto(
    val label: String,
    val sessions: Int,
    val ggr: Int
)

data class SummaryDto(
    val payoutRate: Int,
    val activeGames: Int,
    val avgSessionPerPlayer: Float
)

data class StatsPlatformResponse(
    val games: List<GameStatDto>,
    val ggrTrend7d: List<GgrPointDto>,
    val peakHours: List<PeakHourDto>,
    val summary: SummaryDto
)

object StatsApi {

    suspend fun fetchPlatformStats(): Result<StatsPlatformResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${ApiClient.BASE_URL}/stats/platform")
                    .get()
                    .build()

                ApiClient.http.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException("HTTP ${response.code}")
                        )
                    }

                    val body = response.body?.string()
                        ?: return@withContext Result.failure(IllegalStateException("Empty body"))

                    val json = JSONObject(body)

                    val gamesJson = json.optJSONArray("games") ?: JSONArray()
                    val games = mutableListOf<GameStatDto>()
                    for (i in 0 until gamesJson.length()) {
                        val g = gamesJson.getJSONObject(i)
                        games.add(
                            GameStatDto(
                                gameType = g.optString("gameType"),
                                name = g.optString("name"),
                                sessions24h = g.optInt("sessions24h"),
                                uniquePlayers24h = g.optInt("uniquePlayers24h"),
                                betVolume24h = g.optInt("betVolume24h"),
                                ggr24h = g.optInt("ggr24h"),
                                payoutRate = g.optInt("payoutRate")
                            )
                        )
                    }

                    val trendJson = json.optJSONArray("ggrTrend7d") ?: JSONArray()
                    val trend = mutableListOf<GgrPointDto>()
                    for (i in 0 until trendJson.length()) {
                        val p = trendJson.getJSONObject(i)
                        trend.add(
                            GgrPointDto(
                                day = p.optString("day"),
                                ggr = p.optInt("ggr")
                            )
                        )
                    }

                    val peakJson = json.optJSONArray("peakHours") ?: JSONArray()
                    val peak = mutableListOf<PeakHourDto>()
                    for (i in 0 until peakJson.length()) {
                        val ph = peakJson.getJSONObject(i)
                        peak.add(
                            PeakHourDto(
                                label = ph.optString("label"),
                                sessions = ph.optInt("sessions"),
                                ggr = ph.optInt("ggr")
                            )
                        )
                    }

                    val summaryJson = json.getJSONObject("summary")
                    val summary = SummaryDto(
                        payoutRate = summaryJson.optInt("payoutRate"),
                        activeGames = summaryJson.optInt("activeGames"),
                        avgSessionPerPlayer = summaryJson.optDouble("avgSessionPerPlayer").toFloat()
                    )

                    Result.success(
                        StatsPlatformResponse(
                            games = games,
                            ggrTrend7d = trend,
                            peakHours = peak,
                            summary = summary
                        )
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

