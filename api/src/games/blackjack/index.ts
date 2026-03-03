import WebSocket, { WebSocketServer } from "ws"
import { BlackjackGame } from "./blackjackGame.ts"
import { BlackjackAction } from "./utils/blackjack.model.ts"

const wss = new WebSocketServer({ host: "0.0.0.0", port: 5800 })

const message = (ws: WebSocket, msg: any) => {
  ws.send(JSON.stringify(msg))
}

const game = new BlackjackGame(message)

wss.on("connection", (ws: WebSocket) => {
  ws.on("message", (raw) => {
    const msg = JSON.parse(raw.toString())

    try {
      if (msg.type === "PLACE_BET") {
        game.startGame(msg.payload, ws)
      } else if (msg.type in BlackjackAction) {
        game.handleAction(msg.type as BlackjackAction, ws)
      }
    } catch (e: any) {
      ws.send(
        JSON.stringify({
          type: "ERROR",
          payload: { message: e.message },
        }),
      )
    }
  })

  ws.on("close", () => game.removeSession(ws))
})
