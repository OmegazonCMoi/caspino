import WebSocket from "ws"

const ws = new WebSocket("ws://localhost")

ws.on("error", console.error)

ws.on("open", async () => {
  setInterval(() => {
    ws.send(
      JSON.stringify([
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
      ])
    )
  }, 5000)
})

ws.on("message", function message(data) {
  console.log("received: %s", data)
})
