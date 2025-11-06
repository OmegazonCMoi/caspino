import { WebSocketServer } from "ws"

const wss = new WebSocketServer({ port: 80 })

wss.on("connection", (ws) => {
  console.log("Connection established")
  ws.on("error", console.error)

  ws.on("message", (data) => {
    console.log("received: %s", data)
  })
})
