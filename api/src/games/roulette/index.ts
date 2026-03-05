import WebSocket, { WebSocketServer } from "ws"
import type { IncomingMessage } from "http"
import { RouletteGame } from "./rouletteGame.ts"
import { redNumbers, type Bet } from "./utils/roulette.model.ts"
import { authenticateWS } from "../../middleware/authenticateWS.ts"
import {
  getUserById,
  createParty,
  finishParty,
} from "../../globalRepository.ts"
import { saveRouletteResult, placeRouletteBets } from "./rouletteRepository.ts"

const wss = new WebSocketServer({ port: 5600 })
const clients = new Set<WebSocket>()

interface PlayerContext {
  userId: string
  partyId: string | null
  totalBet: number
}

const playerContexts = new Map<WebSocket, PlayerContext>()

const broadCast = (msg: any) => {
  const data = JSON.stringify(msg)
  clients.forEach((ws) => ws.send(data))
}

const send = (ws: WebSocket, msg: any) => {
  ws.send(JSON.stringify(msg))
}

const message = async (ws: WebSocket, msg: any) => {
  if (msg.type === "BET_RESULT") {
    const ctx = playerContexts.get(ws)
    if (ctx?.partyId) {
      try {
        const gains = msg.payload.gains
        const number = msg.payload.roulettteRandomResult
        const color =
          number === 0 ? "green" : redNumbers.includes(number) ? "red" : "black"

        await saveRouletteResult(ctx.userId, ctx.partyId, number, color, gains)

        await finishParty(ctx.partyId)

        const updated = await getUserById(ctx.userId)
        msg.payload.balance = Number(updated?.balance ?? 0)

        ctx.partyId = null
        ctx.totalBet = 0
      } catch (e: any) {
        console.error("Roulette DB error on resolve", e)
      }
    }
  }

  ws.send(JSON.stringify(msg))
}

const game = new RouletteGame(broadCast, message)

game.start()

wss.on("connection", (ws: WebSocket, req: IncomingMessage) => {
  const decoded = authenticateWS(req)
  if (!decoded) {
    ws.close(1008, "Invalid or missing token")
    return
  }

  const userPromise = getUserById(decoded.userId as string)

  userPromise.then((dbUser) => {
    if (!dbUser) {
      ws.close(1008, "User not found")
      return
    }
    clients.add(ws)
    playerContexts.set(ws, {
      userId: dbUser.id,
      partyId: null,
      totalBet: 0,
    })

    // Send current phase so mid-round joiners know where we are
    send(ws, game.getCurrentPhase())
  })

  ws.on("message", async (raw) => {
    const dbUser = await userPromise
    if (!dbUser) return

    const msg = JSON.parse(raw.toString())

    if (msg.type === "PLACE_BET") {
      try {
        const ctx = playerContexts.get(ws)
        if (!ctx) throw new Error("No session")

        const bets: Bet[] = msg.payload
        const totalBet = bets.reduce((sum, bet) => sum + bet.amount, 0)

        if (totalBet <= 0) {
          return send(ws, {
            type: "ERROR",
            payload: { message: "Mise invalide" },
          })
        }

        // Store bets in memory FIRST (sync) so resolveGame() can find them
        // even if the async DB operations below yield to the event loop
        game.placeBet(bets, ws)

        const { id: partyId } = await createParty("roulette")
        await placeRouletteBets(partyId, ctx.userId, bets)

        ctx.partyId = partyId
        ctx.totalBet = totalBet
      } catch (e: any) {
        console.error("Roulette WS error", e)
        send(ws, {
          type: "ERROR",
          payload: { message: e.message ?? "Erreur serveur" },
        })
      }
    }
  })

  ws.on("close", () => {
    clients.delete(ws)
    playerContexts.delete(ws)
  })
})
