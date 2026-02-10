import type WebSocket from "ws"
import { calculateGains } from "./calculateSlotsGains.ts"

export class SlotsGame {
  private readonly symbols = [
    { value: "1", weight: 30 },
    { value: "2", weight: 25 },
    { value: "3", weight: 20 },
    { value: "4", weight: 10 },
    { value: "5", weight: 5 },
    { value: "6", weight: 5 },
    { value: "7", weight: 5 },
  ]

  constructor(private message: (ws: WebSocket, msg: any) => void) {}

  spin(bet: number, ws: WebSocket) {
    const slotResult = this.generateResult()
    const gains = calculateGains(bet, slotResult, this.symbols)

    this.message(ws, { type: "BET_RESULT", payload: { slotResult, gains } })
  }

  private generateResult(reels: number = 3): string[] {
    const totalWeight = this.symbols.reduce((sum, sym) => sum + sym.weight, 0)

    const pickOne = (): string => {
      let random = Math.random() * totalWeight

      const picked = this.symbols.find((sym) => {
        random -= sym.weight
        return random < 0
      })

      return picked ? picked.value : "1"
    }

    return Array.from({ length: reels }, pickOne)
  }

  simulate(spins: number = 1_000_000, bet: number = 1): number {
    let totalWin = 0

    for (let i = 0; i < spins; i++) {
      const result = this.generateResult()
      totalWin += calculateGains(bet, result, this.symbols)
    }

    const rtp = (totalWin / (spins * bet)) * 100
    return rtp
  }
}
