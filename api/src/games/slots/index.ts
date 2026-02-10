import WebSocket, { WebSocketServer } from "ws"
import { SlotsGame } from "./slotsGame.ts"

const wss = new WebSocketServer({ port: 5700 })

const message = (ws: WebSocket, msg: any) => {
  ws.send(JSON.stringify(msg))
}

const game = new SlotsGame(message)

wss.on("connection", (ws: WebSocket) => {
  ws.on("message", (raw) => {
    const msg = JSON.parse(raw.toString())

    if (msg.type === "PLACE_BET") {
      try {
        game.spin(msg.payload, ws)
      } catch (e: any) {
        ws.send(
          JSON.stringify({
            type: "ERROR",
            payload: { message: e.message },
          }),
        )
      }
    }
  })
})
