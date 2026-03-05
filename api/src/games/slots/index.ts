import WebSocket, { WebSocketServer } from "ws"
import type { IncomingMessage } from "http"
import { SlotsGame } from "./slotsGame.ts"
import { calculateGains } from "./calculateSlotsGains.ts"
import { authenticateWS } from "../../middleware/authenticateWS.ts"
import {
  getUserById,
  createParty,
  finishParty,
  placeBet,
} from "../../globalRepository.ts"
import { saveSlotResult } from "./slotsRepository.ts"

const wss = new WebSocketServer({ host: "0.0.0.0", port: 5700 })

const send = (ws: WebSocket, msg: any) => {
  ws.send(JSON.stringify(msg))
}

const game = new SlotsGame(send)

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
    try {
      const dbUser = await userPromise
      if (!dbUser) return

      const userId = dbUser.id
      const msg = JSON.parse(raw.toString())

      if (msg.type === "PLACE_BET") {
        const bet = msg.payload
        if (!bet || typeof bet !== "number" || bet <= 0) {
          return send(ws, {
            type: "ERROR",
            payload: { message: "Mise invalide" },
          })
        }

        const user = await getUserById(userId)
        if (!user || Number(user.balance) < bet) {
          return send(ws, {
            type: "ERROR",
            payload: { message: "Solde insuffisant" },
          })
        }

        const { id: partyId } = await createParty("slot")
        await placeBet(partyId, userId, bet, "slot_spin", {})

        const slotResult = game.generateResult()
        const gains = calculateGains(bet, slotResult, [
          { value: "1", weight: 30 },
          { value: "2", weight: 25 },
          { value: "3", weight: 20 },
          { value: "4", weight: 10 },
          { value: "5", weight: 5 },
          { value: "6", weight: 5 },
          { value: "7", weight: 5 },
        ])

        await saveSlotResult(userId, partyId, slotResult, gains)

        await finishParty(partyId)

        const updated = await getUserById(userId)

        send(ws, {
          type: "BET_RESULT",
          payload: {
            slotResult: slotResult.map(Number),
            gains,
            balance: Number(updated?.balance ?? 0),
          },
        })
      }
    } catch (error: any) {
      console.error("Slots WS error", error)
      send(ws, {
        type: "ERROR",
        payload: { message: error.message ?? "Erreur serveur" },
      })
    }
  })
})

console.log("Slots WebSocket server running on ws://0.0.0.0:5700")
