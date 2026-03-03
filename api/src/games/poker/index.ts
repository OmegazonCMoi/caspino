import WebSocket, { WebSocketServer } from "ws"
import { PokerTable } from "./pokerTable.ts"
import type { ServerMessage } from "./utils/poker.model.ts"

const wss = new WebSocketServer({ port: 5900 })

const broadcast = (msg: ServerMessage) => {
  const data = JSON.stringify(msg)
  wss.clients.forEach((client) => {
    if (client.readyState === WebSocket.OPEN) {
      client.send(data)
    }
  })
}

const message = (ws: WebSocket, msg: ServerMessage) => {
  if (ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify(msg))
  }
}

const table = new PokerTable(broadcast, message)

console.log("Poker server started on port 5900")

wss.on("connection", (ws: WebSocket) => {
  ws.on("message", (raw) => {
    try {
      const msg = JSON.parse(raw.toString())

      switch (msg.type) {
        case "JOIN_TABLE":
          table.joinTable(ws, msg.payload.name, msg.payload.buyIn)
          break
        case "LEAVE_TABLE":
          table.leaveTable(ws)
          break
        case "PLAYER_ACTION":
          table.handleAction(ws, msg.payload.action, msg.payload.amount)
          break
        default:
          ws.send(
            JSON.stringify({
              type: "ERROR",
              payload: { message: `Unknown message type: ${msg.type}` },
            }),
          )
      }
    } catch (error: any) {
      ws.send(
        JSON.stringify({
          type: "ERROR",
          payload: { message: error.message },
        }),
      )
    }
  })

  ws.on("close", () => {
    table.handleDisconnect(ws)
  })
})
