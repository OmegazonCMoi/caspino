import { WebSocketServer } from "ws"
import { calculateGains } from "./games/roulette/calculateRouletteGains.usecase.ts"

const wss = new WebSocketServer({ port: 80 })

wss.on("connection", (ws) => {
  console.log("Connection established")
  ws.on("error", console.error)

  ws.on("message", async (data) => {
    const randomNumber = Math.floor(Math.random() * 37)
    console.log(randomNumber)
    const result = await calculateGains(
      randomNumber,
      JSON.parse(data.toString())
    )

    ws.send(result)
  })
})

// import express, { type Request, type Response } from "express"
// import { calculateGains } from "./games/roulette/calculateRouletteGains.usecase.ts"

// const app = express()
// const port = 80

// app.post("/", express.json(), async (req: Request, res: Response) => {
//   const body = req.body

//   const randomNumber = Math.floor(Math.random() * 37)
//   console.log(randomNumber)

//   const result = await calculateGains(randomNumber, body)

//   res.status(201).json(result)
// })

// app.listen(port, () => {
//   console.log(`Server is running on http://localhost:${port}`)
// })
