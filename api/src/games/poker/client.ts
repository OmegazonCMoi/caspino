// ! File for debug only

import WebSocket from "ws"
import type { Card, TableStatePayload } from "./utils/poker.model.ts"

await new Promise((resolve) => setTimeout(resolve, 2000))

const PLAYERS = [
  { name: "Alice", buyIn: 500 },
  { name: "Bob", buyIn: 500 },
  { name: "Charlie", buyIn: 500 },
]

const formatCard = (card: Card) => `${card.rank}${card.suit[0]}`

for (const { name, buyIn } of PLAYERS) {
  const ws = new WebSocket("ws://localhost:5900")

  ws.on("error", (error) => console.error(`[${name}] Error:`, error.message))

  ws.on("open", () => {
    console.log(`[${name}] Connected`)
    ws.send(JSON.stringify({ type: "JOIN_TABLE", payload: { name, buyIn } }))
  })

  ws.on("message", (raw) => {
    const msg = JSON.parse(raw.toString())

    if (msg.type === "TABLE_STATE") {
      const state: TableStatePayload = msg.payload

      if (state.yourCards.length > 0) {
        console.log(
          `[${name}] Cards: ${state.yourCards.map(formatCard).join(" ")} | ` +
            `Chips: ${state.yourChips} | Phase: ${state.phase} | ` +
            `Community: ${state.communityCards.map(formatCard).join(" ") || "-"}`,
        )
      }

      if (state.availableActions.length > 0) {
        // Simple strategy: CHECK if possible, else CALL, else FOLD
        let action: string
        let amount: number | undefined

        const actions = state.availableActions as string[]
        if (actions.includes("CHECK")) {
          action = "CHECK"
        } else if (actions.includes("CALL")) {
          action = "CALL"
        } else {
          action = "FOLD"
        }

        console.log(`[${name}] -> ${action}`)
        ws.send(
          JSON.stringify({
            type: "PLAYER_ACTION",
            payload: { action, amount },
          }),
        )
      }
    }

    if (msg.type === "HAND_RESULT") {
      console.log(`\n[${name}] === HAND RESULT ===`)
      console.log(
        `  Community: ${msg.payload.communityCards.map(formatCard).join(" ")}`,
      )
      for (const potWinner of msg.payload.potWinners) {
        for (const winner of potWinner.winners) {
          const handDesc = winner.hand ? ` (${winner.hand.handRank})` : ""
          console.log(
            `  Pot #${potWinner.potIndex}: ${winner.playerName} wins ${winner.amount}${handDesc}`,
          )
        }
      }
      for (const playerInfo of msg.payload.players) {
        if (playerInfo.cards) {
          console.log(
            `  ${playerInfo.name}: ${playerInfo.cards.map(formatCard).join(" ")}`,
          )
        }
      }
      console.log()
    }

    if (msg.type === "PLAYER_JOINED") {
      console.log(
        `[${name}] ${msg.payload.name} joined at seat ${msg.payload.seatIndex}`,
      )
    }

    if (msg.type === "PLAYER_LEFT") {
      console.log(`[${name}] ${msg.payload.name} left`)
    }

    if (msg.type === "ERROR") {
      console.error(`[${name}] Error: ${msg.payload.message}`)
    }
  })

  // Small delay between joins
  await new Promise((resolve) => setTimeout(resolve, 500))
}
