import WebSocket, { WebSocketServer } from "ws"
import type { IncomingMessage } from "http"
import { BlackjackGame } from "./blackjackGame.ts"
import { BlackjackAction } from "./utils/blackjack.model.ts"
import { authenticateWS } from "../../middleware/authenticateWS.ts"
import {
  getUserById,
  createParty,
  finishParty,
  checkBalanceAndPlaceBet,
} from "../../globalRepository.ts"
import { saveBlackjackResult } from "./blackjackRepository.ts"

const wss = new WebSocketServer({ host: "0.0.0.0", port: 5800 })

interface PlayerContext {
  userId: string
  partyId: string
  initialBet: number
}

const playerContexts = new Map<WebSocket, PlayerContext>()

const send = (ws: WebSocket, msg: any) => {
  ws.send(JSON.stringify(msg))
}

const message = async (ws: WebSocket, msg: any) => {
  if (msg.type === "BET_RESULT") {
    const ctx = playerContexts.get(ws)
    if (ctx) {
      try {
        const totalGains = msg.payload.gains
        const won = totalGains > 0

        await saveBlackjackResult(ctx.userId, ctx.partyId, won, totalGains)

        await finishParty(ctx.partyId)

        const updated = await getUserById(ctx.userId)
        msg.payload.balance = Number(updated?.balance ?? 0)
      } catch (e: any) {
        console.error("Blackjack DB error on resolve", e)
      }
    }
  }

  ws.send(JSON.stringify(msg))
}

const game = new BlackjackGame(message)

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
    }
  })

  ws.on("message", async (raw) => {
    const dbUser = await userPromise
    if (!dbUser) return

    const userId = dbUser.id
    const msg = JSON.parse(raw.toString())

    try {
      if (msg.type === "PLACE_BET") {
        const bet = msg.payload
        if (!bet || typeof bet !== "number" || bet <= 0) {
          return send(ws, {
            type: "ERROR",
            payload: { message: "Mise invalide" },
          })
        }

        const { id: partyId } = await createParty("blackjack")
        await checkBalanceAndPlaceBet(userId, partyId, bet, "blackjack_win", {})

        playerContexts.set(ws, { userId, partyId, initialBet: bet })

        game.startGame(bet, ws)
      } else if (msg.type === "DOUBLE_DOWN") {
        const ctx = playerContexts.get(ws)
        if (!ctx) throw new Error("No active session")

        await checkBalanceAndPlaceBet(userId, ctx.partyId, ctx.initialBet, "blackjack_win", { action: "double_down" })

        game.handleAction(BlackjackAction.DOUBLE_DOWN, ws)
      } else if (msg.type === "SPLIT") {
        const ctx = playerContexts.get(ws)
        if (!ctx) throw new Error("No active session")

        await checkBalanceAndPlaceBet(userId, ctx.partyId, ctx.initialBet, "blackjack_win", { action: "split" })

        game.handleAction(BlackjackAction.SPLIT, ws)
      } else if (msg.type === "INSURANCE") {
        const ctx = playerContexts.get(ws)
        if (!ctx) throw new Error("No active session")

        const insuranceAmount = Math.floor(ctx.initialBet / 2)
        await checkBalanceAndPlaceBet(userId, ctx.partyId, insuranceAmount, "blackjack_win", { action: "insurance" })

        game.handleAction(BlackjackAction.INSURANCE, ws)
      } else if (msg.type in BlackjackAction) {
        game.handleAction(msg.type as BlackjackAction, ws)
      }
    } catch (e: any) {
      console.error("Blackjack WS error", e)
      send(ws, {
        type: "ERROR",
        payload: { message: e.message ?? "Erreur serveur" },
      })
    }
  })

  ws.on("close", () => {
    game.removeSession(ws)
    playerContexts.delete(ws)
  })
})

console.log("Blackjack WebSocket server running on ws://0.0.0.0:5800")
