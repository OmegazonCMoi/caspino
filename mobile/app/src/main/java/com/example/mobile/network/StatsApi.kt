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
    val summary: SummaryDto,
    val totalPlayerWinnings24h: Int
)

data class PlayerGameStatDto(
    val gameType: String,
    val name: String,
    val totalBets: Int,
    val totalWagered: Int,
    val totalWon: Int,
    val winRate: Int
)

data class PlayerStatsResponse(
    val totalSessions: Int,
    val totalBets: Int,
    val totalWagered: Int,
    val totalWon: Int,
    val netResult: Int,
    val games: List<PlayerGameStatDto>
)

data class PlayerProfileDto(
    val username: String,
    val profile: String,
    val totalSessions: Int,
    val totalWagered: Int,
    val avgBet: Int,
    val winRate: Int
)

data class PlayerPredictionDto(
    val username: String,
    val predictedWinRate: Int,
    val estimatedSessionsNextWeek: Int,
    val predictedNetGainNextWeek: Int,
    val trend: String
)

data class PlayersAnalysisResponse(
    val profiles: List<PlayerProfileDto>,
    val predictions: List<PlayerPredictionDto>
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

                    val totalPlayerWinnings24h = json.optInt("totalPlayerWinnings24h", 0)

                    Result.success(
                        StatsPlatformResponse(
                            games = games,
                            ggrTrend7d = trend,
                            peakHours = peak,
                            summary = summary,
                            totalPlayerWinnings24h = totalPlayerWinnings24h
                        )
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun fetchPlayerStats(): Result<PlayerStatsResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val token = ApiClient.token
                    ?: return@withContext Result.failure(IllegalStateException("No token"))

                val request = Request.Builder()
                    .url("${ApiClient.BASE_URL}/stats/player")
                    .addHeader("Authorization", "Bearer $token")
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
                    val games = mutableListOf<PlayerGameStatDto>()
                    for (gameIndex in 0 until gamesJson.length()) {
                        val gameObj = gamesJson.getJSONObject(gameIndex)
                        games.add(
                            PlayerGameStatDto(
                                gameType = gameObj.optString("gameType"),
                                name = gameObj.optString("name"),
                                totalBets = gameObj.optInt("totalBets"),
                                totalWagered = gameObj.optInt("totalWagered"),
                                totalWon = gameObj.optInt("totalWon"),
                                winRate = gameObj.optInt("winRate")
                            )
                        )
                    }

                    Result.success(
                        PlayerStatsResponse(
                            totalSessions = json.optInt("totalSessions"),
                            totalBets = json.optInt("totalBets"),
                            totalWagered = json.optInt("totalWagered"),
                            totalWon = json.optInt("totalWon"),
                            netResult = json.optInt("netResult"),
                            games = games
                        )
                    )
                }
            } catch (exception: Exception) {
                Result.failure(exception)
            }
        }
    }

    suspend fun fetchPlayersAnalysis(): Result<PlayersAnalysisResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${ApiClient.BASE_URL}/stats/players-analysis")
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

                    val profilesJson = json.optJSONArray("profiles") ?: JSONArray()
                    val profiles = mutableListOf<PlayerProfileDto>()
                    for (i in 0 until profilesJson.length()) {
                        val p = profilesJson.getJSONObject(i)
                        profiles.add(
                            PlayerProfileDto(
                                username = p.optString("username"),
                                profile = p.optString("profile"),
                                totalSessions = p.optInt("totalSessions"),
                                totalWagered = p.optInt("totalWagered"),
                                avgBet = p.optInt("avgBet"),
                                winRate = p.optInt("winRate")
                            )
                        )
                    }

                    val predictionsJson = json.optJSONArray("predictions") ?: JSONArray()
                    val predictions = mutableListOf<PlayerPredictionDto>()
                    for (i in 0 until predictionsJson.length()) {
                        val p = predictionsJson.getJSONObject(i)
                        predictions.add(
                            PlayerPredictionDto(
                                username = p.optString("username"),
                                predictedWinRate = p.optInt("predictedWinRate"),
                                estimatedSessionsNextWeek = p.optInt("estimatedSessionsNextWeek"),
                                predictedNetGainNextWeek = p.optInt("predictedNetGainNextWeek"),
                                trend = p.optString("trend")
                            )
                        )
                    }

                    Result.success(
                        PlayersAnalysisResponse(
                            profiles = profiles,
                            predictions = predictions
                        )
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

