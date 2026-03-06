import { sql } from "kysely"
import { db } from "./db/index.ts"

export interface PlayerAnalysisDatasetRow {
  user_id: string
  username: string
  total_sessions: string
  total_wagered: string
  avg_bet: string
  active_days: string
  total_rounds: string
  total_won: string
  wins: string
  sessions_7d: string
  wagered_7d: string
  rounds_7d: string
  won_7d: string
  wins_7d: string
}

export interface WeeklyPlayerBetRow {
  user_id: string
  username: string
  week_start: Date
  sessions: string
  total_wagered: string
}

export interface WeeklyPlayerResultRow {
  user_id: string
  week_start: Date
  total_won: string
  rounds: string
  wins: string
}

export interface PlayerBetAnalysisRow {
  user_id: string
  username: string
  party_id: string
  amount: number
  created_at: Date
}

export interface PlayerResultAnalysisRow {
  user_id: string
  created_at: Date
  gain: number
}

export const getTotalPlayerWinnings24h = (since: Date) => {
  return db
    .selectFrom(
      sql<{ gain: number; created_at: Date }>`(
        SELECT gain, created_at FROM slot_results
        UNION ALL
        SELECT gain, created_at FROM roulette_results
        UNION ALL
        SELECT gain, created_at FROM blackjack_results
      )`.as("all_results"),
    )
    .where(sql.ref("all_results.created_at"), ">=", since)
    .select(sql<string>`COALESCE(SUM(${sql.ref("all_results.gain")}), 0)`.as("total_winnings"))
    .executeTakeFirstOrThrow()
}

export const getPlayerStats = (userId: string) => {
  return db
    .selectFrom("bets as b")
    .innerJoin("parties as p", "p.id", "b.party_id")
    .where("b.user_id", "=", userId)
    .groupBy("p.game_type")
    .select([
      "p.game_type",
      db.fn.countAll<string>().as("total_bets"),
      sql<string>`COALESCE(SUM(${sql.ref("b.amount")}), 0)`.as("total_wagered"),
    ])
    .execute()
}

export const getPlayerWinnings = (userId: string) => {
  return db
    .selectFrom(
      sql<{ gain: number; game_type: string }>`(
        SELECT sr.gain, p.game_type
        FROM slot_results sr
        JOIN parties p ON p.id = sr.party_id
        JOIN (SELECT DISTINCT party_id FROM bets WHERE user_id = ${userId}) ub ON ub.party_id = sr.party_id
        UNION ALL
        SELECT rr.gain, p.game_type
        FROM roulette_results rr
        JOIN parties p ON p.id = rr.party_id
        JOIN (SELECT DISTINCT party_id FROM bets WHERE user_id = ${userId}) ub ON ub.party_id = rr.party_id
        UNION ALL
        SELECT br.gain, p.game_type
        FROM blackjack_results br
        JOIN parties p ON p.id = br.party_id
        JOIN (SELECT DISTINCT party_id FROM bets WHERE user_id = ${userId}) ub ON ub.party_id = br.party_id
      )`.as("player_results"),
    )
    .groupBy("player_results.game_type")
    .select([
      "player_results.game_type",
      sql<string>`COUNT(*)`.as("total_rounds"),
      sql<string>`COALESCE(SUM(${sql.ref("player_results.gain")}), 0)`.as("total_won"),
      sql<string>`COUNT(CASE WHEN ${sql.ref("player_results.gain")} > 0 THEN 1 END)`.as("wins"),
    ])
    .execute()
}

export const getPlayerSessionCount = (userId: string) => {
  return db
    .selectFrom("bets as b")
    .where("b.user_id", "=", userId)
    .select(sql<string>`COUNT(DISTINCT ${sql.ref("b.party_id")})`.as("total_sessions"))
    .executeTakeFirstOrThrow()
}

export const getPerGameStats = (since: Date) => {
  return db
    .with("bets_agg", (qb) =>
      qb
        .selectFrom("bets as b")
        .innerJoin("parties as p", "p.id", "b.party_id")
        .where("b.created_at", ">=", since)
        .groupBy("p.game_type")
        .select([
          "p.game_type",
          sql<string>`COUNT(DISTINCT ${sql.ref("b.party_id")})`.as("sessions_24h"),
          sql<string>`COUNT(DISTINCT ${sql.ref("b.user_id")})`.as("unique_players_24h"),
          sql<string>`COALESCE(SUM(${sql.ref("b.amount")}), 0)`.as("bet_volume_24h"),
        ]),
    )
    .with("payouts_agg", (qb) =>
      qb
        .selectFrom(
          sql<{ gain: number; game_type: string }>`(
            SELECT sr.gain, p.game_type
            FROM slot_results sr JOIN parties p ON p.id = sr.party_id
            WHERE sr.created_at >= ${since}
            UNION ALL
            SELECT rr.gain, p.game_type
            FROM roulette_results rr JOIN parties p ON p.id = rr.party_id
            WHERE rr.created_at >= ${since}
            UNION ALL
            SELECT br.gain, p.game_type
            FROM blackjack_results br JOIN parties p ON p.id = br.party_id
            WHERE br.created_at >= ${since}
          )`.as("all_payouts"),
        )
        .groupBy("all_payouts.game_type")
        .select([
          "all_payouts.game_type",
          sql<string>`COALESCE(SUM(${sql.ref("all_payouts.gain")}), 0)`.as("total_payout_24h"),
        ]),
    )
    .selectFrom("bets_agg as ba")
    .leftJoin("payouts_agg as pa", "pa.game_type", "ba.game_type")
    .select([
      "ba.game_type",
      "ba.sessions_24h",
      "ba.unique_players_24h",
      "ba.bet_volume_24h",
      sql<string>`COALESCE(${sql.ref("pa.total_payout_24h")}, 0)`.as("total_payout_24h"),
    ])
    .execute()
}

export const getGgrTrend = (since: Date) => {
  return db
    .with("bets_per_day", (qb) =>
      qb
        .selectFrom("bets as b")
        .where("b.created_at", ">=", since)
        .groupBy(sql`date_trunc('day', ${sql.ref("b.created_at")})`)
        .select([
          sql<Date>`date_trunc('day', ${sql.ref("b.created_at")})`.as("day"),
          sql<string>`SUM(${sql.ref("b.amount")})`.as("bet_volume"),
        ]),
    )
    .with("payouts_per_day", (qb) =>
      qb
        .selectFrom(
          sql<{ created_at: Date; gain: number }>`(
            SELECT created_at, gain FROM slot_results
            UNION ALL
            SELECT created_at, gain FROM roulette_results
            UNION ALL
            SELECT created_at, gain FROM blackjack_results
          )`.as("results"),
        )
        .where("results.created_at", ">=", since)
        .groupBy(sql`date_trunc('day', ${sql.ref("results.created_at")})`)
        .select([
          sql<Date>`date_trunc('day', ${sql.ref("results.created_at")})`.as(
            "day",
          ),
          sql<string>`SUM(${sql.ref("results.gain")})`.as("total_payout"),
        ]),
    )
    .selectFrom(
      sql<{ day: Date }>`generate_series(${since}::date, now()::date, '1 day'::interval)`.as(
        "day_series",
      ),
    )
    .leftJoin("bets_per_day as b", (join) =>
      join.on(
        sql`${sql.ref("b.day")}::date`,
        "=",
        sql`${sql.ref("day_series.day_series")}::date`,
      ),
    )
    .leftJoin("payouts_per_day as p", (join) =>
      join.on(
        sql`${sql.ref("p.day")}::date`,
        "=",
        sql`${sql.ref("day_series.day_series")}::date`,
      ),
    )
    .orderBy(sql`${sql.ref("day_series.day_series")}`)
    .select([
      sql<Date>`${sql.ref("day_series.day_series")}::date`.as("day"),
      sql<string>`COALESCE(${sql.ref("b.bet_volume")}, 0) - COALESCE(${sql.ref("p.total_payout")}, 0)`.as(
        "ggr",
      ),
    ])
    .execute()
}

export const getPeakHours = (since: Date) => {
  return db
    .with("bets_hourly", (qb) =>
      qb
        .selectFrom("bets as b")
        .innerJoin("parties as p", "p.id", "b.party_id")
        .where("b.created_at", ">=", since)
        .groupBy(sql`date_trunc('hour', ${sql.ref("p.created_at")})`)
        .select([
          sql<Date>`date_trunc('hour', ${sql.ref("p.created_at")})`.as("hour_bucket"),
          sql<string>`COUNT(DISTINCT ${sql.ref("b.party_id")})`.as("sessions"),
          sql<string>`COALESCE(SUM(${sql.ref("b.amount")}), 0)`.as("bet_volume"),
        ]),
    )
    .with("payouts_hourly", (qb) =>
      qb
        .selectFrom(
          sql<{ gain: number; created_at: Date }>`(
            SELECT sr.gain, p.created_at
            FROM slot_results sr JOIN parties p ON p.id = sr.party_id
            WHERE sr.created_at >= ${since}
            UNION ALL
            SELECT rr.gain, p.created_at
            FROM roulette_results rr JOIN parties p ON p.id = rr.party_id
            WHERE rr.created_at >= ${since}
            UNION ALL
            SELECT br.gain, p.created_at
            FROM blackjack_results br JOIN parties p ON p.id = br.party_id
            WHERE br.created_at >= ${since}
          )`.as("all_payouts"),
        )
        .groupBy(sql`date_trunc('hour', ${sql.ref("all_payouts.created_at")})`)
        .select([
          sql<Date>`date_trunc('hour', ${sql.ref("all_payouts.created_at")})`.as("hour_bucket"),
          sql<string>`COALESCE(SUM(${sql.ref("all_payouts.gain")}), 0)`.as("total_payout"),
        ]),
    )
    .selectFrom("bets_hourly as bh")
    .leftJoin("payouts_hourly as ph", "ph.hour_bucket", "bh.hour_bucket")
    .orderBy(sql`${sql.ref("bh.sessions")}`, "desc")
    .limit(5)
    .select([
      "bh.hour_bucket",
      "bh.sessions",
      sql<string>`COALESCE(${sql.ref("bh.bet_volume")}, 0) - COALESCE(${sql.ref("ph.total_payout")}, 0)`.as("ggr"),
    ])
    .execute()
}

export const getPlayersAnalysisDataset = (since: Date, recentSince: Date) => {
  return db
    .with("bets_agg", (qb) =>
      qb
        .selectFrom("bets as b")
        .where("b.created_at", ">=", since)
        .groupBy("b.user_id")
        .select([
          "b.user_id",
          sql<string>`COUNT(DISTINCT ${sql.ref("b.party_id")})`.as(
            "total_sessions",
          ),
          sql<string>`COALESCE(SUM(${sql.ref("b.amount")}), 0)`.as(
            "total_wagered",
          ),
          sql<string>`COALESCE(AVG(${sql.ref("b.amount")}), 0)`.as("avg_bet"),
          sql<string>`COUNT(DISTINCT DATE(${sql.ref("b.created_at")}))`.as(
            "active_days",
          ),
          sql<string>`COUNT(DISTINCT CASE WHEN ${sql.ref("b.created_at")} >= ${recentSince} THEN ${sql.ref("b.party_id")} END)`.as(
            "sessions_7d",
          ),
          sql<string>`COALESCE(SUM(CASE WHEN ${sql.ref("b.created_at")} >= ${recentSince} THEN ${sql.ref("b.amount")} ELSE 0 END), 0)`.as(
            "wagered_7d",
          ),
        ]),
    )
    .with("results_agg", (qb) =>
      qb
        .selectFrom(
          sql<{ user_id: string; created_at: Date; gain: number }>`(
            SELECT user_id, created_at, gain FROM slot_results
            UNION ALL
            SELECT user_id, created_at, gain FROM roulette_results
            UNION ALL
            SELECT user_id, created_at, gain FROM blackjack_results
          )`.as("results"),
        )
        .where("results.created_at", ">=", since)
        .groupBy("results.user_id")
        .select([
          sql<string>`${sql.ref("results.user_id")}`.as("user_id"),
          sql<string>`COUNT(*)`.as("total_rounds"),
          sql<string>`COALESCE(SUM(${sql.ref("results.gain")}), 0)`.as(
            "total_won",
          ),
          sql<string>`COUNT(CASE WHEN ${sql.ref("results.gain")} > 0 THEN 1 END)`.as(
            "wins",
          ),
          sql<string>`COUNT(CASE WHEN ${sql.ref("results.created_at")} >= ${recentSince} THEN 1 END)`.as(
            "rounds_7d",
          ),
          sql<string>`COALESCE(SUM(CASE WHEN ${sql.ref("results.created_at")} >= ${recentSince} THEN ${sql.ref("results.gain")} ELSE 0 END), 0)`.as(
            "won_7d",
          ),
          sql<string>`COUNT(CASE WHEN ${sql.ref("results.created_at")} >= ${recentSince} AND ${sql.ref("results.gain")} > 0 THEN 1 END)`.as(
            "wins_7d",
          ),
        ]),
    )
    .selectFrom("users as u")
    .innerJoin("bets_agg as b", "b.user_id", "u.id")
    .leftJoin("results_agg as r", "r.user_id", "u.id")
    .select([
      sql<string>`${sql.ref("u.id")}`.as("user_id"),
      "u.username",
      sql<string>`COALESCE(${sql.ref("b.total_sessions")}, 0)`.as(
        "total_sessions",
      ),
      sql<string>`COALESCE(${sql.ref("b.total_wagered")}, 0)`.as(
        "total_wagered",
      ),
      sql<string>`COALESCE(${sql.ref("b.avg_bet")}, 0)`.as("avg_bet"),
      sql<string>`COALESCE(${sql.ref("b.active_days")}, 0)`.as("active_days"),
      sql<string>`COALESCE(${sql.ref("r.total_rounds")}, 0)`.as("total_rounds"),
      sql<string>`COALESCE(${sql.ref("r.total_won")}, 0)`.as("total_won"),
      sql<string>`COALESCE(${sql.ref("r.wins")}, 0)`.as("wins"),
      sql<string>`COALESCE(${sql.ref("b.sessions_7d")}, 0)`.as("sessions_7d"),
      sql<string>`COALESCE(${sql.ref("b.wagered_7d")}, 0)`.as("wagered_7d"),
      sql<string>`COALESCE(${sql.ref("r.rounds_7d")}, 0)`.as("rounds_7d"),
      sql<string>`COALESCE(${sql.ref("r.won_7d")}, 0)`.as("won_7d"),
      sql<string>`COALESCE(${sql.ref("r.wins_7d")}, 0)`.as("wins_7d"),
    ])
    .orderBy(sql`${sql.ref("b.total_sessions")}::int`, "desc")
    .execute() as Promise<PlayerAnalysisDatasetRow[]>
}

export const getWeeklyPlayerBetHistory = (since: Date) => {
  return db
    .selectFrom("bets as b")
    .innerJoin("users as u", "u.id", "b.user_id")
    .where("b.created_at", ">=", since)
    .groupBy([
      "b.user_id",
      "u.username",
      sql`date_trunc('week', ${sql.ref("b.created_at")})`,
    ])
    .select([
      "b.user_id",
      "u.username",
      sql<Date>`date_trunc('week', ${sql.ref("b.created_at")})`.as(
        "week_start",
      ),
      sql<string>`COUNT(DISTINCT ${sql.ref("b.party_id")})`.as("sessions"),
      sql<string>`COALESCE(SUM(${sql.ref("b.amount")}), 0)`.as(
        "total_wagered",
      ),
    ])
    .execute() as Promise<WeeklyPlayerBetRow[]>
}

export const getWeeklyPlayerResultHistory = async (since: Date) => {
  const slotRows = (await db
    .selectFrom("slot_results as r")
    .where("r.created_at", ">=", since)
    .groupBy([
      "r.user_id",
      sql`date_trunc('week', ${sql.ref("r.created_at")})`,
    ])
    .select([
      "r.user_id",
      sql<Date>`date_trunc('week', ${sql.ref("r.created_at")})`.as(
        "week_start",
      ),
      sql<string>`COALESCE(SUM(${sql.ref("r.gain")}), 0)`.as("total_won"),
      sql<string>`COUNT(*)`.as("rounds"),
      sql<string>`COUNT(CASE WHEN ${sql.ref("r.gain")} > 0 THEN 1 END)`.as(
        "wins",
      ),
    ])
    .execute()) as WeeklyPlayerResultRow[]

  const rouletteRows = (await db
    .selectFrom("roulette_results as r")
    .where("r.created_at", ">=", since)
    .groupBy([
      "r.user_id",
      sql`date_trunc('week', ${sql.ref("r.created_at")})`,
    ])
    .select([
      "r.user_id",
      sql<Date>`date_trunc('week', ${sql.ref("r.created_at")})`.as(
        "week_start",
      ),
      sql<string>`COALESCE(SUM(${sql.ref("r.gain")}), 0)`.as("total_won"),
      sql<string>`COUNT(*)`.as("rounds"),
      sql<string>`COUNT(CASE WHEN ${sql.ref("r.gain")} > 0 THEN 1 END)`.as(
        "wins",
      ),
    ])
    .execute()) as WeeklyPlayerResultRow[]

  const blackjackRows = (await db
    .selectFrom("blackjack_results as r")
    .where("r.created_at", ">=", since)
    .groupBy([
      "r.user_id",
      sql`date_trunc('week', ${sql.ref("r.created_at")})`,
    ])
    .select([
      "r.user_id",
      sql<Date>`date_trunc('week', ${sql.ref("r.created_at")})`.as(
        "week_start",
      ),
      sql<string>`COALESCE(SUM(${sql.ref("r.gain")}), 0)`.as("total_won"),
      sql<string>`COUNT(*)`.as("rounds"),
      sql<string>`COUNT(CASE WHEN ${sql.ref("r.gain")} > 0 THEN 1 END)`.as(
        "wins",
      ),
    ])
    .execute()) as WeeklyPlayerResultRow[]

  return [...slotRows, ...rouletteRows, ...blackjackRows]
}

export const getPlayerBetAnalysisRows = (since: Date) => {
  return db
    .selectFrom("bets as b")
    .innerJoin("users as u", "u.id", "b.user_id")
    .where("b.created_at", ">=", since)
    .select([
      "b.user_id",
      "u.username",
      "b.party_id",
      "b.amount",
      "b.created_at",
    ])
    .execute() as Promise<PlayerBetAnalysisRow[]>
}

export const getPlayerResultAnalysisRows = async (since: Date) => {
  const rows = await db
    .selectFrom(
      sql<{ user_id: string; created_at: Date; gain: number }>`(
        SELECT b.user_id, sr.created_at, sr.gain
        FROM slot_results sr
        JOIN bets b ON b.party_id = sr.party_id
        WHERE sr.created_at >= ${since}
        GROUP BY b.user_id, sr.created_at, sr.gain
        UNION ALL
        SELECT b.user_id, rr.created_at, rr.gain
        FROM roulette_results rr
        JOIN bets b ON b.party_id = rr.party_id
        WHERE rr.created_at >= ${since}
        GROUP BY b.user_id, rr.created_at, rr.gain
        UNION ALL
        SELECT b.user_id, br.created_at, br.gain
        FROM blackjack_results br
        JOIN bets b ON b.party_id = br.party_id
        WHERE br.created_at >= ${since}
        GROUP BY b.user_id, br.created_at, br.gain
      )`.as("all_results"),
    )
    .select([
      "all_results.user_id",
      "all_results.created_at",
      "all_results.gain",
    ])
    .execute()

  return rows as PlayerResultAnalysisRow[]
}
