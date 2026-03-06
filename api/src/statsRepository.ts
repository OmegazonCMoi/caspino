import { sql } from "kysely"
import { db } from "./db/index.ts"

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
