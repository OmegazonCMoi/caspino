import {
  getPlayerBetAnalysisRows,
  getPlayerResultAnalysisRows,
} from "./statsRepository.ts"

type PlayerProfile = "flambeur" | "prudent" | "malchanceux"
type Trend = "up" | "stable" | "down"

interface PlayerProfileResult {
  username: string
  profile: PlayerProfile
  totalSessions: number
  totalWagered: number
  avgBet: number
  winRate: number
}

interface PlayerPredictionResult {
  username: string
  predictedWinRate: number
  estimatedSessionsNextWeek: number
  predictedNetGainNextWeek: number
  trend: Trend
}

const ANALYSIS_WINDOW_DAYS = 35
const RECENT_WINDOW_DAYS = 7
const MAX_PLAYERS = 8
const REGRESSION_WEEKS = 5

const clamp = (value: number, min: number, max: number) => {
  return Math.min(Math.max(value, min), max)
}

const round = (value: number) => Math.round(Number.isFinite(value) ? value : 0)

const startOfUtcWeek = (date: Date) => {
  const weekStart = new Date(
    Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate()),
  )
  const dayOffset = (weekStart.getUTCDay() + 6) % 7
  weekStart.setUTCDate(weekStart.getUTCDate() - dayOffset)
  weekStart.setUTCHours(0, 0, 0, 0)
  return weekStart
}

const toWeekKey = (date: Date) => startOfUtcWeek(date).toISOString().slice(0, 10)

const buildWeekKeys = () => {
  const keys: string[] = []
  const currentWeek = startOfUtcWeek(new Date())

  for (let index = REGRESSION_WEEKS - 1; index >= 0; index -= 1) {
    const week = new Date(currentWeek)
    week.setUTCDate(week.getUTCDate() - index * 7)
    keys.push(toWeekKey(week))
  }

  return keys
}

const predictNextValue = (values: number[]) => {
  if (values.length === 0) {
    return { prediction: 0, slope: 0 }
  }

  if (values.length === 1) {
    return { prediction: values[0] ?? 0, slope: 0 }
  }

  const xMean = (values.length - 1) / 2
  const yMean = values.reduce((sum, value) => sum + value, 0) / values.length

  let numerator = 0
  let denominator = 0

  values.forEach((value, index) => {
    const xDelta = index - xMean
    const yDelta = value - yMean
    numerator += xDelta * yDelta
    denominator += xDelta * xDelta
  })

  const slope = denominator === 0 ? 0 : numerator / denominator
  const intercept = yMean - slope * xMean

  return {
    prediction: intercept + slope * values.length,
    slope,
  }
}

const getProfileFromMetrics = (
  totalSessions: number,
  totalWagered: number,
  avgBet: number,
  winRate: number,
  roi: number,
  activeDays: number,
  population: {
    avgSessions: number
    avgWagered: number
    avgBet: number
    avgWinRate: number
    avgRoi: number
    avgIntensity: number
  },
): PlayerProfile => {
  const sessionIntensity = totalSessions / Math.max(activeDays, 1)
  const highStake =
    avgBet >= population.avgBet * 1.35 ||
    totalWagered >= population.avgWagered * 1.4
  const highIntensity =
    totalSessions >= population.avgSessions * 1.2 ||
    sessionIntensity >= population.avgIntensity * 1.15
  const unlucky =
    winRate <= population.avgWinRate - 10 && roi <= population.avgRoi - 0.12

  if (unlucky) {
    return "malchanceux"
  }

  if (highStake && (highIntensity || roi < population.avgRoi + 0.05)) {
    return "flambeur"
  }

  return "prudent"
}

export const buildPlayersAnalysis = async () => {
  const now = new Date()
  const since = new Date(now.getTime() - ANALYSIS_WINDOW_DAYS * 24 * 60 * 60 * 1000)
  const recentSince = new Date(
    now.getTime() - RECENT_WINDOW_DAYS * 24 * 60 * 60 * 1000,
  )

  const [betRows, resultRows] = await Promise.all([
    getPlayerBetAnalysisRows(since),
    getPlayerResultAnalysisRows(since),
  ])

  if (betRows.length === 0) {
    return {
      profiles: [] as PlayerProfileResult[],
      predictions: [] as PlayerPredictionResult[],
    }
  }

  const userMap = new Map<
    string,
    {
      username: string
      sessionIds: Set<string>
      totalWagered: number
      betCount: number
      activeDays: Set<string>
      recentSessionIds: Set<string>
      recentWagered: number
      sessionsByWeek: Map<string, Set<string>>
      wageredByWeek: Map<string, number>
      totalRounds: number
      totalWon: number
      wins: number
      rounds7d: number
      won7d: number
      wins7d: number
      wonByWeek: Map<string, number>
      roundsByWeek: Map<string, number>
      winsByWeek: Map<string, number>
    }
  >()

  const getUserAccumulator = (userId: string, username: string) => {
    const existing = userMap.get(userId)
    if (existing) {
      return existing
    }

    const created = {
      username,
      sessionIds: new Set<string>(),
      totalWagered: 0,
      betCount: 0,
      activeDays: new Set<string>(),
      recentSessionIds: new Set<string>(),
      recentWagered: 0,
      sessionsByWeek: new Map<string, Set<string>>(),
      wageredByWeek: new Map<string, number>(),
      totalRounds: 0,
      totalWon: 0,
      wins: 0,
      rounds7d: 0,
      won7d: 0,
      wins7d: 0,
      wonByWeek: new Map<string, number>(),
      roundsByWeek: new Map<string, number>(),
      winsByWeek: new Map<string, number>(),
    }

    userMap.set(userId, created)
    return created
  }

  betRows.forEach((row) => {
    const user = getUserAccumulator(row.user_id, row.username)
    const createdAt = new Date(row.created_at)
    const dayKey = createdAt.toISOString().slice(0, 10)
    const weekKey = toWeekKey(createdAt)

    user.sessionIds.add(row.party_id)
    user.totalWagered += Number(row.amount)
    user.betCount += 1
    user.activeDays.add(dayKey)

    if (createdAt >= recentSince) {
      user.recentSessionIds.add(row.party_id)
      user.recentWagered += Number(row.amount)
    }

    const weeklySessions = user.sessionsByWeek.get(weekKey) ?? new Set<string>()
    weeklySessions.add(row.party_id)
    user.sessionsByWeek.set(weekKey, weeklySessions)
    user.wageredByWeek.set(
      weekKey,
      (user.wageredByWeek.get(weekKey) ?? 0) + Number(row.amount),
    )
  })

  resultRows.forEach((row) => {
    const user = userMap.get(row.user_id)

    if (!user) {
      return
    }

    const createdAt = new Date(row.created_at)
    const weekKey = toWeekKey(createdAt)
    const gain = Number(row.gain)

    user.totalRounds += 1
    user.totalWon += gain
    if (gain > 0) {
      user.wins += 1
    }

    if (createdAt >= recentSince) {
      user.rounds7d += 1
      user.won7d += gain
      if (gain > 0) {
        user.wins7d += 1
      }
    }

    user.wonByWeek.set(weekKey, (user.wonByWeek.get(weekKey) ?? 0) + gain)
    user.roundsByWeek.set(weekKey, (user.roundsByWeek.get(weekKey) ?? 0) + 1)
    if (gain > 0) {
      user.winsByWeek.set(weekKey, (user.winsByWeek.get(weekKey) ?? 0) + 1)
    }
  })

  const dataset = Array.from(userMap.entries()).map(([userId, user]) => {
    return {
      userId,
      username: user.username,
      totalSessions: user.sessionIds.size,
      totalWagered: user.totalWagered,
      avgBet: user.betCount > 0 ? user.totalWagered / user.betCount : 0,
      activeDays: user.activeDays.size,
      totalRounds: user.totalRounds,
      totalWon: user.totalWon,
      wins: user.wins,
      sessions7d: user.recentSessionIds.size,
      wagered7d: user.recentWagered,
      rounds7d: user.rounds7d,
      won7d: user.won7d,
      wins7d: user.wins7d,
      sessionsByWeek: user.sessionsByWeek,
      wageredByWeek: user.wageredByWeek,
      wonByWeek: user.wonByWeek,
      roundsByWeek: user.roundsByWeek,
      winsByWeek: user.winsByWeek,
    }
  })

  const population = dataset.reduce(
    (accumulator, row) => {
      const totalSessions = row.totalSessions
      const totalWagered = row.totalWagered
      const avgBet = row.avgBet
      const totalRounds = row.totalRounds
      const wins = row.wins
      const totalWon = row.totalWon
      const activeDays = row.activeDays
      const winRate = totalRounds > 0 ? (wins * 100) / totalRounds : 0
      const roi = totalWagered > 0 ? (totalWon - totalWagered) / totalWagered : 0

      accumulator.avgSessions += totalSessions
      accumulator.avgWagered += totalWagered
      accumulator.avgBet += avgBet
      accumulator.avgWinRate += winRate
      accumulator.avgRoi += roi
      accumulator.avgIntensity += totalSessions / Math.max(activeDays, 1)

      return accumulator
    },
    {
      avgSessions: 0,
      avgWagered: 0,
      avgBet: 0,
      avgWinRate: 0,
      avgRoi: 0,
      avgIntensity: 0,
    },
  )

  population.avgSessions /= dataset.length
  population.avgWagered /= dataset.length
  population.avgBet /= dataset.length
  population.avgWinRate /= dataset.length
  population.avgRoi /= dataset.length
  population.avgIntensity /= dataset.length

  const weekKeys = buildWeekKeys()

  const enriched = dataset.map((row) => {
    const totalSessions = row.totalSessions
    const totalWagered = row.totalWagered
    const avgBet = row.avgBet
    const activeDays = row.activeDays
    const totalRounds = row.totalRounds
    const wins = row.wins
    const totalWon = row.totalWon
    const winRate = totalRounds > 0 ? (wins * 100) / totalRounds : 0
    const roi = totalWagered > 0 ? (totalWon - totalWagered) / totalWagered : 0

    const profile = getProfileFromMetrics(
      totalSessions,
      totalWagered,
      avgBet,
      winRate,
      roi,
      activeDays,
      population,
    )

    const sessionsSeries = weekKeys.map(
      (key) => row.sessionsByWeek.get(key)?.size ?? 0,
    )
    const wageredSeries = weekKeys.map(
      (key) => row.wageredByWeek.get(key) ?? 0,
    )
    const wonSeries = weekKeys.map((key) => row.wonByWeek.get(key) ?? 0)
    const roundsSeries = weekKeys.map(
      (key) => row.roundsByWeek.get(key) ?? 0,
    )
    const winsSeries = weekKeys.map((key) => row.winsByWeek.get(key) ?? 0)
    const netGainSeries = wonSeries.map(
      (won, index) => won - (wageredSeries[index] ?? 0),
    )
    const winRateSeries = roundsSeries.map((rounds, index) =>
      rounds > 0 ? ((winsSeries[index] ?? 0) * 100) / rounds : 0,
    )

    const sessionPrediction = predictNextValue(sessionsSeries)
    const netGainPrediction = predictNextValue(netGainSeries)
    const winRatePrediction = predictNextValue(winRateSeries)

    const predictedNetGainNextWeek = round(netGainPrediction.prediction)
    const latestWeeklyNet = netGainSeries.at(-1) ?? 0
    const netDelta = predictedNetGainNextWeek - latestWeeklyNet

    let trend: Trend = "stable"
    if (netDelta >= 250) {
      trend = "up"
    } else if (netDelta <= -250) {
      trend = "down"
    }

    return {
      username: row.username,
      totalSessions,
      totalWagered: round(totalWagered),
      avgBet: round(avgBet),
      winRate: round(winRate),
      profile,
      predictedWinRate: clamp(round(winRatePrediction.prediction), 0, 100),
      estimatedSessionsNextWeek: Math.max(0, round(sessionPrediction.prediction)),
      predictedNetGainNextWeek,
      trend,
    }
  })

  const ranked = enriched
    .filter((player) => player.totalSessions > 0)
    .sort((left, right) => right.totalSessions - left.totalSessions)
    .slice(0, MAX_PLAYERS)

  return {
    profiles: ranked.map((player) => ({
      username: player.username,
      profile: player.profile,
      totalSessions: player.totalSessions,
      totalWagered: player.totalWagered,
      avgBet: player.avgBet,
      winRate: player.winRate,
    })),
    predictions: ranked.map((player) => ({
      username: player.username,
      predictedWinRate: player.predictedWinRate,
      estimatedSessionsNextWeek: player.estimatedSessionsNextWeek,
      predictedNetGainNextWeek: player.predictedNetGainNextWeek,
      trend: player.trend,
    })),
  }
}
