import WebSocket, { WebSocketServer } from "ws"
import { RouletteGame } from "./rouletteGame.ts"

const wss = new WebSocketServer({ port: 5600 })
const clients = new Set<WebSocket>()

const broadCast = (msg: any) => {
  const data = JSON.stringify(msg)
  clients.forEach((ws) => ws.send(data))
}

const message = (ws: WebSocket, msg: any) => {
  ws.send(JSON.stringify(msg))
}

const game = new RouletteGame(broadCast, message)

game.start()

wss.on("connection", (ws: WebSocket) => {
  clients.add(ws)
  ws.send(
    JSON.stringify({
      type: "SYNC_STATE",
      payload: game.getSnapshot(),
    }),
  )

  ws.on("message", (raw) => {
    const msg = JSON.parse(raw.toString())

    if (msg.type === "SYNC_REQUEST") {
      ws.send(
        JSON.stringify({
          type: "SYNC_STATE",
          payload: game.getSnapshot(),
        }),
      )
      return
    }

    if (msg.type === "PLACE_BET") {
      try {
        game.placeBet(msg.payload, ws)
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

  ws.on("close", () => clients.delete(ws))
})
