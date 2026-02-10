// ! File for debug only

import WebSocket from "ws"

// To be sure api is started
await new Promise((resolve) => setTimeout(resolve, 2000))

const ws = new WebSocket("ws://localhost:5600")

ws.on("error", console.error)

ws.on("open", async () => {
  console.log("logged in")
})

ws.on("message", (raw) => {
  const msg = JSON.parse(raw.toString())

  if (msg.type === "PHASE_UPDATE" && msg.payload.phase === "BETTING") {
    return ws.send(
      JSON.stringify({
        type: "PLACE_BET",
        payload: [
          {
            choice: "red",
            amount: 100,
          },
          {
            numbers: [1, 2, 4, 5],
            amount: 100,
          },
          {
            dozen: "13-24",
            amount: 100,
          },
        ],
      }),
    )
  }

  if (msg.type === "BET_RESULT") {
    console.log(msg.payload)
  }
})
