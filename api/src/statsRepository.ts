import { sql } from "kysely"
import { db } from "./db/index.ts"

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
        FROM slot_results sr JOIN parties p ON p.id = sr.party_id
        WHERE sr.user_id = ${userId}
        UNION ALL
        SELECT rr.gain, p.game_type
        FROM roulette_results rr JOIN parties p ON p.id = rr.party_id
        WHERE rr.user_id = ${userId}
        UNION ALL
        SELECT br.gain, p.game_type
        FROM blackjack_results br JOIN parties p ON p.id = br.party_id
        WHERE br.user_id = ${userId}
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
    .selectFrom("parties as p")
    .leftJoin("bets as b", (join) =>
      join.onRef("b.party_id", "=", "p.id").on("b.created_at", ">=", since),
    )
    .leftJoin("slot_results as sr", "sr.party_id", "p.id")
    .leftJoin("roulette_results as rr", "rr.party_id", "p.id")
    .leftJoin("blackjack_results as br", "br.party_id", "p.id")
    .where("p.created_at", ">=", since)
    .groupBy("p.game_type")
    .select([
      "p.game_type",
      db.fn.countAll().as("sessions_24h"),
      sql<string>`COUNT(DISTINCT ${sql.ref("b.user_id")})`.as(
        "unique_players_24h",
      ),
      sql<string>`COALESCE(SUM(${sql.ref("b.amount")}), 0)`.as(
        "bet_volume_24h",
      ),
      sql<string>`COALESCE(SUM(COALESCE(${sql.ref("sr.gain")}, 0) + COALESCE(${sql.ref("rr.gain")}, 0) + COALESCE(${sql.ref("br.gain")}, 0)), 0)`.as(
        "total_payout_24h",
      ),
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
    .selectFrom("parties as p")
    .leftJoin("bets as b", (join) =>
      join.onRef("b.party_id", "=", "p.id").on("b.created_at", ">=", since),
    )
    .leftJoin("slot_results as sr", "sr.party_id", "p.id")
    .leftJoin("roulette_results as rr", "rr.party_id", "p.id")
    .leftJoin("blackjack_results as br", "br.party_id", "p.id")
    .where("p.created_at", ">=", since)
    .groupBy(sql`date_trunc('hour', ${sql.ref("p.created_at")})`)
    .orderBy(sql`sessions`, "desc")
    .limit(5)
    .select([
      sql<Date>`date_trunc('hour', ${sql.ref("p.created_at")})`.as(
        "hour_bucket",
      ),
      db.fn.countAll<string>().as("sessions"),
      sql<string>`COALESCE(SUM(${sql.ref("b.amount")}), 0) - COALESCE(SUM(COALESCE(${sql.ref("sr.gain")}, 0) + COALESCE(${sql.ref("rr.gain")}, 0) + COALESCE(${sql.ref("br.gain")}, 0)), 0)`.as(
        "ggr",
      ),
    ])
    .execute()
}
