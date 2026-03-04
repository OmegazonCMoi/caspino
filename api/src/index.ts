import express, { type Request, type Response } from "express"
import bodyParser from "body-parser"
import bcrypt from "bcrypt"
import crypto from "crypto"
import jwt from "jsonwebtoken"
import { sql } from "kysely"
import { db } from "./db/index.ts"
import "dotenv/config"
import "./games/roulette/index.ts"

const SALT_ROUNDS = 12

const JWT_SECRET = process.env.JWT_SECRET

if (!JWT_SECRET) {
  throw new Error("JWT Secret is missing !")
}

const app = express()
const PORT = 5500

app.use(bodyParser.json())

const authenticateJWT = (req: Request, res: Response, next: Function) => {
  const authHeader = req.headers.authorization

  if (!authHeader || !authHeader.startsWith("Bearer ")) {
    return res.sendStatus(401)
  }

  const token = authHeader.split(" ")[1]

  if (!token) {
    return res.sendStatus(401)
  }

  jwt.verify(token, JWT_SECRET, (err, user) => {
    if (err) return res.sendStatus(403)
    ;(req as any).user = user
    next()
  })
}

const hashPassword = (password: string): Promise<string> => {
  return bcrypt.hash(password, SALT_ROUNDS)
}

export const verifyPassword = (
  password: string,
  hash: string,
): Promise<boolean> => {
  return bcrypt.compare(password, hash)
}

app.post("/signup", async (req: Request, res: Response) => {
  const { username, password, email } = req.body

  const user = await db
    .selectFrom("users")
    .select(["username", "email"])
    .where((eb: any) =>
      eb.or([eb("email", "=", email), eb("username", "=", username)]),
    )
    .executeTakeFirst()

  if (user) {
    if (user.username)
      return res.status(409).json({ message: "Username already taken" })

    return res.status(409).json({ message: "Email address already taken" })
  }

  const hashedPassword = await hashPassword(password)

  await db
    .insertInto("users")
    .values({
      id: crypto.randomUUID(),
      username,
      password_hash: hashedPassword,
      email,
      is_active: true,
      last_login: new Date(),
      created_at: new Date(),
      balance: 0,
    })
    .execute()

  const token = jwt.sign({ username }, JWT_SECRET, {
    expiresIn: "7d",
  })

  res.status(200).json({ message: "Login successful", token, balance: 0 })
})

app.post("/login", async (req: Request, res: Response) => {
  const { username, password } = req.body

  const user = (await db
    .selectFrom("users")
    .select(["password_hash", "balance"])
    .where("username", "=", username)
    .executeTakeFirst()) ?? { password_hash: undefined, balance: 0 }

  if (
    !user.password_hash ||
    !(await verifyPassword(password, user.password_hash))
  ) {
    return res.status(401).json({ message: "Invalid credentials" })
  }

  const token = jwt.sign({ username }, JWT_SECRET, {
    expiresIn: "7d",
  })

  res.status(200).json({ message: "Login successful", token, balance: Number(user.balance) })
})

app.post("/logout", (req: Request, res: Response) => {
  res
    .status(200)
    .json({ message: "Logout successful. Delete your token on client." })
})

app.get("/me", authenticateJWT, async (req: Request, res: Response) => {
  const { username } = (req as any).user

  const user = await db
    .selectFrom("users")
    .select(["balance", "email", "created_at"])
    .where("username", "=", username)
    .executeTakeFirst()

  if (!user) return res.sendStatus(404)

  res.status(200).json({
    user: {
      username,
      email: user.email,
      balance: Number(user.balance),
      createdAt: user.created_at,
    },
  })
})

app.get("/health", (req: Request, res: Response) => {
  res.status(200).json({ status: "ok", service: "api-caspino" })
})

app.post("/bonus/daily", authenticateJWT, async (req: Request, res: Response) => {
  try {
    const { username } = (req as any).user
    const DAILY_AMOUNT = 500

    const user = await db
      .selectFrom("users")
      .select("id")
      .where("username", "=", username)
      .executeTakeFirst()

    if (!user) return res.status(404).json({ message: "User not found" })

    await db
      .insertInto("wallet_transactions")
      .values({
        id: crypto.randomUUID(),
        user_id: user.id,
        amount: DAILY_AMOUNT,
        reason: "daily_bonus",
        created_at: new Date(),
      })
      .execute()

    const updated = await db
      .selectFrom("users")
      .select("balance")
      .where("id", "=", user.id)
      .executeTakeFirst()

    res.status(200).json({
      message: "Bonus journalier récupéré",
      amount: DAILY_AMOUNT,
      balance: Number(updated?.balance ?? 0),
    })
  } catch (error: any) {
    if (error?.code === "23505") {
      return res.status(409).json({ message: "Bonus déjà récupéré aujourd'hui" })
    }
    console.error("Daily bonus error", error)
    res.status(500).json({ message: "Erreur serveur" })
  }
})

app.get("/stats/platform", async (req: Request, res: Response) => {
  try {
    const now = new Date()
    const since24h = new Date(now.getTime() - 24 * 60 * 60 * 1000)
    const since7d = new Date(now.getTime() - 6 * 24 * 60 * 60 * 1000)

    const perGame = await sql<{
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
        AND b.created_at >= ${since24h}
      LEFT JOIN slot_results sr ON sr.party_id = p.id
      LEFT JOIN roulette_results rr ON rr.party_id = p.id
      LEFT JOIN blackjack_results br ON br.party_id = p.id
      WHERE p.created_at >= ${since24h}
      GROUP BY p.game_type
    `.execute(db)

    const ggrTrend = await sql<{
      day: Date
      ggr: string | null
    }>`
      WITH bets_per_day AS (
        SELECT
          date_trunc('day', b.created_at) AS day,
          SUM(b.amount) AS bet_volume
        FROM bets b
        WHERE b.created_at >= ${since7d}
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
        WHERE created_at >= ${since7d}
        GROUP BY day
      )
      SELECT
        d::date AS day,
        COALESCE(b.bet_volume, 0) - COALESCE(p.total_payout, 0) AS ggr
      FROM generate_series(${since7d}::date, now()::date, '1 day') d
      LEFT JOIN bets_per_day b ON b.day::date = d::date
      LEFT JOIN payouts_per_day p ON p.day::date = d::date
      ORDER BY day
    `.execute(db)

    const peakHours = await sql<{
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
          AND b.created_at >= ${since24h}
        LEFT JOIN slot_results sr ON sr.party_id = p.id
        LEFT JOIN roulette_results rr ON rr.party_id = p.id
        LEFT JOIN blackjack_results br ON br.party_id = p.id
        WHERE p.created_at >= ${since24h}
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

    let totalSessions = 0
    let totalPlayers = 0
    let totalBetVolume = 0
    let totalPayout = 0

    const games = perGame.rows.map((row) => {
      const sessions24h = Number(row.sessions_24h)
      const uniquePlayers24h = Number(row.unique_players_24h)
      const betVolume24h = Number(row.bet_volume_24h ?? 0)
      const totalPayout24h = Number(row.total_payout_24h ?? 0)

      const ggr24h = betVolume24h - totalPayout24h
      const payoutRate =
        betVolume24h > 0
          ? Math.round((totalPayout24h * 100) / betVolume24h)
          : 0

      totalSessions += sessions24h
      totalPlayers += uniquePlayers24h
      totalBetVolume += betVolume24h
      totalPayout += totalPayout24h

      const name =
        row.game_type === "blackjack"
          ? "Blackjack"
          : row.game_type === "roulette"
            ? "Roulette"
            : "Machine à sous"

      return {
        gameType: row.game_type,
        name,
        sessions24h,
        uniquePlayers24h,
        betVolume24h,
        ggr24h,
        payoutRate,
      }
    })

    const ggrTrend7d = ggrTrend.rows.map((row) => ({
      day: row.day,
      ggr: Number(row.ggr ?? 0),
    }))

    const peakHoursResult = peakHours.rows.map((row) => {
      const date = new Date(row.hour_bucket)
      const hour = date.getHours()
      const label = `${hour}h-${(hour + 1) % 24}h`

      return {
        hour,
        label,
        sessions: Number(row.sessions),
        ggr: Number(row.ggr ?? 0),
      }
    })

    const ggrGlobal = totalBetVolume - totalPayout
    const payoutGlobal =
      totalBetVolume > 0 ? Math.round((totalPayout * 100) / totalBetVolume) : 0
    const avgSessionPerPlayer =
      totalPlayers > 0 ? totalSessions / totalPlayers : 0

    res.status(200).json({
      games,
      ggrTrend7d,
      peakHours: peakHoursResult,
      summary: {
        payoutRate: payoutGlobal,
        activeGames: games.length,
        avgSessionPerPlayer,
      },
    })
  } catch (error) {
    console.error("Error fetching platform stats", error)
    res.status(500).json({ message: "Failed to fetch platform stats" })
  }
})

app.listen(PORT, "0.0.0.0", () => {
  console.log(`HTTP server running on http://0.0.0.0:${PORT}`)
})
