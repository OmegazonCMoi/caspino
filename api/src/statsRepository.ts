import { sql } from "kysely"
import { db } from "./db/index.ts"

export const getPerGameStats = (since: Date) => {
  return sql<{
    game_type: string
    sessions_24h: string
    unique_players_24h: string
    bet_volume_24h: string | null
    total_payout_24h: string | null
  }>`
    SELECT
      p.game_type,
      COUNT(*) AS sessions_24h,
      COUNT(DISTINCT p.user_id) AS unique_players_24h,
      COALESCE(SUM(b.amount), 0) AS bet_volume_24h,
      COALESCE(
        SUM(
          COALESCE(sr.gain, 0)
          + COALESCE(rr.gain, 0)
          + COALESCE(br.gain, 0)
        ),
        0
      ) AS total_payout_24h
    FROM parties p
    LEFT JOIN bets b
      ON b.party_id = p.id
      AND b.created_at >= ${since}
    LEFT JOIN slot_results sr ON sr.party_id = p.id
    LEFT JOIN roulette_results rr ON rr.party_id = p.id
    LEFT JOIN blackjack_results br ON br.party_id = p.id
    WHERE p.created_at >= ${since}
    GROUP BY p.game_type
  `.execute(db)
}

export const getGgrTrend = (since: Date) => {
  return sql<{
    day: Date
    ggr: string | null
  }>`
    WITH bets_per_day AS (
      SELECT
        date_trunc('day', b.created_at) AS day,
        SUM(b.amount) AS bet_volume
      FROM bets b
      WHERE b.created_at >= ${since}
      GROUP BY day
    ),
    payouts_per_day AS (
      SELECT
        date_trunc('day', created_at) AS day,
        SUM(gain) AS total_payout
      FROM (
        SELECT created_at, gain FROM slot_results
        UNION ALL
        SELECT created_at, gain FROM roulette_results
        UNION ALL
        SELECT created_at, gain FROM blackjack_results
      ) r
      WHERE created_at >= ${since}
      GROUP BY day
    )
    SELECT
      d::date AS day,
      COALESCE(b.bet_volume, 0) - COALESCE(p.total_payout, 0) AS ggr
    FROM generate_series(${since}::date, now()::date, '1 day') d
    LEFT JOIN bets_per_day b ON b.day::date = d::date
    LEFT JOIN payouts_per_day p ON p.day::date = d::date
    ORDER BY day
  `.execute(db)
}

export const getPeakHours = (since: Date) => {
  return sql<{
    hour_bucket: Date
    sessions: string
    ggr: string | null
  }>`
    WITH aggregates AS (
      SELECT
        date_trunc('hour', p.created_at) AS hour_bucket,
        COUNT(*) AS sessions,
        COALESCE(SUM(b.amount), 0) AS bet_volume,
        COALESCE(
          SUM(
            COALESCE(sr.gain, 0)
            + COALESCE(rr.gain, 0)
            + COALESCE(br.gain, 0)
          ),
          0
        ) AS total_payout
      FROM parties p
      LEFT JOIN bets b
        ON b.party_id = p.id
        AND b.created_at >= ${since}
      LEFT JOIN slot_results sr ON sr.party_id = p.id
      LEFT JOIN roulette_results rr ON rr.party_id = p.id
      LEFT JOIN blackjack_results br ON br.party_id = p.id
      WHERE p.created_at >= ${since}
      GROUP BY hour_bucket
    )
    SELECT
      hour_bucket,
      sessions,
      (bet_volume - total_payout) AS ggr
    FROM aggregates
    ORDER BY sessions DESC
    LIMIT 5
  `.execute(db)
}
