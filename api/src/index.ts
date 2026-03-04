import express, { type Request, type Response } from "express"
import bodyParser from "body-parser"
import bcrypt from "bcrypt"
import crypto from "crypto"
import jwt from "jsonwebtoken"
import "dotenv/config"
import {
  getUserByUsername,
  getUserById,
  getUserByUsernameOrEmail,
  createUser,
} from "./globalRepository.ts"
import {
  getPerGameStats,
  getGgrTrend,
  getPeakHours,
} from "./statsRepository.ts"

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

  const existing = await getUserByUsernameOrEmail(username, email)

  if (existing) {
    if (existing.username === username)
      return res.status(409).json({ message: "Username already taken" })

    return res.status(409).json({ message: "Email address already taken" })
  }

  const hashedPassword = await hashPassword(password)
  const userId = crypto.randomUUID()

  await createUser(userId, username, hashedPassword, email)

  const token = jwt.sign({ username, userId }, JWT_SECRET, {
    expiresIn: "7d",
  })

  res.status(200).json({ message: "Login successful", token, balance: 0 })
})

app.post("/login", async (req: Request, res: Response) => {
  const { username, password } = req.body

  const user = await getUserByUsername(username)

  if (
    !user?.password ||
    !(await verifyPassword(password, user.password))
  ) {
    return res.status(401).json({ message: "Invalid credentials" })
  }

  const token = jwt.sign({ username, userId: user.id }, JWT_SECRET, {
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
  const { userId, username } = (req as any).user

  const user = userId
    ? await getUserById(userId)
    : await getUserByUsername(username)

  if (!user) return res.sendStatus(404)

  res.status(200).json({
    user: {
      username: user.username,
      email: user.email,
      balance: Number(user.balance),
      createdAt: user.created_at,
    },
  })
})

app.get("/health", (req: Request, res: Response) => {
  res.status(200).json({ status: "ok", service: "api-caspino" })
})

app.get("/stats/platform", async (req: Request, res: Response) => {
  try {
    const now = new Date()
    const since24h = new Date(now.getTime() - 24 * 60 * 60 * 1000)
    const since7d = new Date(now.getTime() - 6 * 24 * 60 * 60 * 1000)

    const perGame = await getPerGameStats(since24h)
    const ggrTrend = await getGgrTrend(since7d)
    const peakHours = await getPeakHours(since24h)

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
