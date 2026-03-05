import express, { type Request, type Response } from "express"
import bodyParser from "body-parser"
import bcrypt from "bcrypt"
import crypto from "crypto"
import jwt from "jsonwebtoken"
import OpenAI from "openai"
import "dotenv/config"
import {
  getUserByUsername,
  getUserById,
  getUserByUsernameOrEmail,
  createUser,
  getLastDailyBonus,
  insertDailyBonus,
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
  try {
    const { username, password, email } = req.body
    console.log(`[signup] Attempt for username="${username}" email="${email}"`)

    const existing = await getUserByUsernameOrEmail(username, email)

    if (existing) {
      if (existing.username === username) {
        return res.status(409).json({ message: "Username already taken" })
      }

      return res.status(409).json({ message: "Email address already taken" })
    }

    const hashedPassword = await hashPassword(password)
    const userId = crypto.randomUUID()

    await createUser(userId, username, hashedPassword, email)

    const token = jwt.sign({ username, userId }, JWT_SECRET, {
      expiresIn: "7d",
    })

    console.log(`[signup] Success for username="${username}"`)
    res.status(200).json({ message: "Login successful", token, balance: 0 })
  } catch (error) {
    console.error("Signup error:", error)
    res.status(500).json({ message: "Internal server error" })
  }
})

app.post("/login", async (req: Request, res: Response) => {
  try {
    const { username, password } = req.body
    console.log(`[login] Attempt for username="${username}"`)

    const user = await getUserByUsername(username)

    if (!user?.password || !(await verifyPassword(password, user.password))) {
      console.log(`[login] Invalid credentials for username="${username}"`)
      return res.status(401).json({ message: "Invalid credentials" })
    }

    const token = jwt.sign({ username, userId: user.id }, JWT_SECRET, {
      expiresIn: "7d",
    })

    res.status(200).json({
      message: "Login successful",
      token,
      balance: Number(user.balance),
    })
  } catch (error) {
    console.error("Login error:", error)
    res.status(500).json({ message: "Internal server error" })
  }
})

app.post("/logout", (req: Request, res: Response) => {
  console.log("[logout] Logout called")
  res
    .status(200)
    .json({ message: "Logout successful. Delete your token on client." })
})

app.get("/me", authenticateJWT, async (req: Request, res: Response) => {
  try {
    const { userId, username } = (req as any).user

    const user = userId
      ? await getUserById(userId)
      : await getUserByUsername(username)

    if (!user) return res.sendStatus(404)

    const dailyBonusClaim = await getLastDailyBonus(user.id)

    res.status(200).json({
      user: {
        username: user.username,
        email: user.email,
        balance: Number(user.balance),
        createdAt: user.created_at,
        hasClaimedDailyBonus: !!dailyBonusClaim,
      },
    })
  } catch (error) {
    console.error("Fetch me error:", error)
    res.status(500).json({ message: "Internal server error" })
  }
})

app.post(
  "/claim-daily-bonus",
  authenticateJWT,
  async (req: Request, res: Response) => {
    try {
      const { userId } = (req as any).user
      console.log(`[claim-daily-bonus] Attempt for userId="${userId}"`)

      const existingClaim = await getLastDailyBonus(userId)

      if (existingClaim) {
        console.log(`[claim-daily-bonus] Already claimed today for userId="${userId}"`)
        return res.status(409).json({ message: "Already claimed today" })
      }

      await insertDailyBonus(userId, 500)

      const user = await getUserById(userId)
      const balance = Number(user?.balance ?? 0)

      console.log(`[claim-daily-bonus] Success for userId="${userId}", balance=${balance}`)
      res.status(200).json({ balance })
    } catch (error) {
      console.error("Claim daily bonus error:", error)
      res.status(500).json({ message: "Internal server error" })
    }
  },
)

app.get("/health", (req: Request, res: Response) => {
  res.status(200).json({ status: "ok", service: "api-caspino" })
})

const bastetenClient = new OpenAI({
  apiKey: process.env.BASETEN_API_KEY,
  baseURL: "https://inference.baseten.co/v1",
})

app.get("/ai/punchline", async (req: Request, res: Response) => {
  const game = req.query.game as string | undefined

  if (!game || !["blackjack", "roulette"].includes(game)) {
    return res
      .status(400)
      .json({ message: "Query param 'game' must be 'blackjack' or 'roulette'" })
  }

  const systemPrompt =
    game === "blackjack"
      ? "Tu es un croupier de blackjack charismatique et drôle dans un casino en ligne appelé Caspino. Tu balances des punchlines courtes, percutantes et stylées sur le blackjack. Garde un ton fun, un peu provocateur mais toujours classe. Réponds UNIQUEMENT avec la punchline, sans guillemets, sans explication, une seule phrase."
      : "Tu es un croupier de roulette charismatique et drôle dans un casino en ligne appelé Caspino. Tu balances des punchlines courtes, percutantes et stylées sur la roulette. Garde un ton fun, un peu provocateur mais toujours classe. Réponds UNIQUEMENT avec la punchline, sans guillemets, sans explication, une seule phrase."

  try {
    const response = await bastetenClient.chat.completions.create({
      model: "openai/gpt-oss-120b",
      messages: [
        { role: "system", content: systemPrompt },
        { role: "user", content: "Balance une punchline de croupier !" },
      ],
      max_tokens: 100,
      temperature: 1.2,
    })

    const punchline =
      response.choices?.[0]?.message?.content?.trim() ?? "Faites vos jeux !"

    res.status(200).json({ punchline, game })
  } catch (error) {
    console.error("Baseten AI error:", error)
    res.status(200).json({
      punchline:
        game === "blackjack"
          ? "21, le chiffre magique... ou pas."
          : "Rien ne va plus, les jeux sont faits !",
      game,
    })
  }
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
        betVolume24h > 0 ? Math.round((totalPayout24h * 100) / betVolume24h) : 0

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
