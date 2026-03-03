// ! File for debug only

import WebSocket from "ws"
import type { Card } from "./utils/blackjack.model.ts"

// To be sure api is started
await new Promise((resolve) => setTimeout(resolve, 2000))

const ws = new WebSocket("ws://localhost:5800")

const handValue = (cards: Card[]): number => {
  let value = 0
  let aces = 0
  for (const card of cards) {
    if (card.rank === "A") {
      value += 11
      aces++
    } else if (["K", "Q", "J"].includes(card.rank)) {
      value += 10
    } else {
      value += parseInt(card.rank)
    }
  }
  while (value > 21 && aces > 0) {
    value -= 10
    aces--
  }
  return value
}

ws.on("error", console.error)

ws.on("open", () => {
  console.log("Connected to blackjack server")
  ws.send(JSON.stringify({ type: "PLACE_BET", payload: 100 }))
})

ws.on("message", (raw) => {
  const msg = JSON.parse(raw.toString())

  if (msg.type === "GAME_STATE") {
    const { hands, activeHandIndex } = msg.payload
    const activeHand = hands[activeHandIndex]
    const value = handValue(activeHand.cards)

    console.log(
      `Hand ${activeHandIndex + 1}: ${activeHand.cards.map((c: Card) => `${c.rank}${c.suit[0]}`).join(" ")} (${value})`,
    )
    console.log(`Dealer shows: ${msg.payload.dealerUpCard.rank}`)

    // Basic strategy: hit if < 17, stand otherwise
    if (value < 17) {
      console.log("-> HIT")
      ws.send(JSON.stringify({ type: "HIT" }))
    } else {
      console.log("-> STAND")
      ws.send(JSON.stringify({ type: "STAND" }))
    }
  }

  if (msg.type === "BET_RESULT") {
    console.log("\n--- Result ---")
    console.log(
      `Dealer: ${msg.payload.dealerHand.map((c: Card) => `${c.rank}${c.suit[0]}`).join(" ")} (${msg.payload.dealerValue})`,
    )
    for (const [i, hand] of msg.payload.hands.entries()) {
      console.log(
        `Hand ${i + 1}: value=${hand.playerValue} status=${hand.status} bet=${hand.bet} gains=${hand.gains}`,
      )
    }
    console.log(`Total gains: ${msg.payload.gains}`)
    ws.close()
  }

  if (msg.type === "ERROR") {
    console.error("Error:", msg.payload.message)
  }
})
