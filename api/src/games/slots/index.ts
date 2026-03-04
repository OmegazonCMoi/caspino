import WebSocket, { WebSocketServer } from "ws"
import crypto from "crypto"
import jwt from "jsonwebtoken"
import { db } from "../../db/index.ts"
import { SlotsGame } from "./slotsGame.ts"
import { calculateGains } from "./calculateSlotsGains.ts"
import "dotenv/config"

const JWT_SECRET = process.env.JWT_SECRET
if (!JWT_SECRET) throw new Error("JWT_SECRET is missing!")

const wss = new WebSocketServer({ host: "0.0.0.0", port: 5700 })

const send = (ws: WebSocket, msg: any) => {
  ws.send(JSON.stringify(msg))
}

const game = new SlotsGame(send)

const authenticatedUsers = new Map<WebSocket, { username: string; userId: string }>()

wss.on("connection", (ws: WebSocket) => {
  ws.on("message", async (raw) => {
    try {
      const msg = JSON.parse(raw.toString())

      if (msg.type === "AUTH") {
        const token = msg.payload?.token
        if (!token) {
          return send(ws, { type: "ERROR", payload: { message: "Token manquant" } })
        }

        try {
          const decoded = jwt.verify(token, JWT_SECRET) as { username: string }

          const user = await db
            .selectFrom("users")
            .select(["id", "balance"])
            .where("username", "=", decoded.username)
            .executeTakeFirst()

          if (!user) {
            return send(ws, { type: "ERROR", payload: { message: "Utilisateur introuvable" } })
          }

          authenticatedUsers.set(ws, { username: decoded.username, userId: user.id })
          send(ws, {
            type: "AUTH_OK",
            payload: { username: decoded.username, balance: Number(user.balance) },
          })
        } catch {
          send(ws, { type: "ERROR", payload: { message: "Token invalide" } })
        }
        return
      }

      if (msg.type === "PLACE_BET") {
        const auth = authenticatedUsers.get(ws)
        if (!auth) {
          return send(ws, { type: "ERROR", payload: { message: "Non authentifié" } })
        }

        const bet = msg.payload
        if (!bet || typeof bet !== "number" || bet <= 0) {
          return send(ws, { type: "ERROR", payload: { message: "Mise invalide" } })
        }

        const user = await db
          .selectFrom("users")
          .select(["id", "balance"])
          .where("id", "=", auth.userId)
          .executeTakeFirst()

        if (!user || Number(user.balance) < bet) {
          return send(ws, { type: "ERROR", payload: { message: "Solde insuffisant" } })
        }

        const partyId = crypto.randomUUID()
        const betId = crypto.randomUUID()

        await db
          .insertInto("parties")
          .values({
            id: partyId,
            user_id: auth.userId,
            game_type: "slot",
            created_at: new Date(),
          })
          .execute()

        await db
          .insertInto("bets")
          .values({
            id: betId,
            party_id: partyId,
            user_id: auth.userId,
            amount: bet,
            kind: "slot_spin",
            selection: JSON.stringify({}),
            created_at: new Date(),
          })
          .execute()

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

        await db
          .insertInto("slot_results")
          .values({
            party_id: partyId,
            result: slotResult.join(""),
            gain: gains,
            created_at: new Date(),
          })
          .execute()

        await db
          .updateTable("parties")
          .set({ finished_at: new Date() })
          .where("id", "=", partyId)
          .execute()

        const updated = await db
          .selectFrom("users")
          .select("balance")
          .where("id", "=", auth.userId)
          .executeTakeFirst()

        send(ws, {
          type: "BET_RESULT",
          payload: {
            slotResult: slotResult.map(Number),
            gains,
            balance: Number(updated?.balance ?? 0),
          },
        })
        return
      }
    } catch (e: any) {
      console.error("Slots WS error", e)
      send(ws, { type: "ERROR", payload: { message: e.message ?? "Erreur serveur" } })
    }
  })

  ws.on("close", () => {
    authenticatedUsers.delete(ws)
  })
})

console.log("Slots WebSocket server running on ws://0.0.0.0:5700")
