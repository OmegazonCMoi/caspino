import WebSocket from "ws"
import { calculateGains } from "./calculateRouletteGains.ts"
import { RoulettePhase, type Bet } from "./utils/roulette.model.ts"
import { playeEffect } from "../playeEffect.ts"

export class RouletteGame {
  private phase: RoulettePhase = RoulettePhase.BETTING
  private phaseEndsAt = 0
  private bets: Map<WebSocket, Bet[]> = new Map()

  constructor(
    private broadcast: (msg: any) => void,
    private message: (ws: WebSocket, msg: any) => Promise<void>,
  ) {}

  start() {
    this.setPhase(RoulettePhase.BETTING, 15_000)
  }

  private setPhase(phase: RoulettePhase, durationMs: number) {
    this.phase = phase
    this.phaseEndsAt = Date.now() + durationMs

    this.broadcast({
      type: "PHASE_UPDATE",
      payload: { phase, endsAt: this.phaseEndsAt },
    })

    console.log(`Current phase: ${phase}`)

    setTimeout(() => this.nextPhase(), durationMs)
  }

  private async nextPhase() {
    switch (this.phase) {
      case RoulettePhase.BETTING:
        this.setPhase(RoulettePhase.SPINNING, 3_000)
        break
      case RoulettePhase.SPINNING:
        await this.resolveGame()
        this.setPhase(RoulettePhase.RESULT, 15_000)
        break
      case RoulettePhase.RESULT:
        this.reset()
        this.setPhase(RoulettePhase.BETTING, 30_000)
        break
    }
  }

  placeBet(bets: Bet[], ws: WebSocket) {
    if (this.phase !== RoulettePhase.BETTING && this.phase !== RoulettePhase.SPINNING) {
      throw new Error("Betting closed")
    }

    this.bets.set(ws, bets)
  }

  private async resolveGame() {
    console.log(">>> resolveGame: bets count =", this.bets.size)
    const roulettteRandomResult = Math.floor(Math.random() * 37)

    // Broadcast winning number to ALL clients (so everyone sees the wheel spin)
    this.broadcast({
      type: "ROUND_RESULT",
      payload: { winningNumber: roulettteRandomResult },
    })

    // Send individual gains to each player who bet
    await Promise.all(
      Array.from(this.bets.entries()).map(async ([ws, playerBets]) => {
        const gains = await calculateGains(roulettteRandomResult, playerBets)
        const totalBet = playerBets.reduce((sum, singleBet) => sum + singleBet.amount, 0)
        playeEffect(gains, totalBet)
        await this.message(ws, {
          type: "BET_RESULT",
          payload: { gains, roulettteRandomResult },
        })
      }),
    )
  }

  getCurrentPhase() {
    return {
      type: "PHASE_UPDATE" as const,
      payload: { phase: this.phase, endsAt: this.phaseEndsAt },
    }
  }

  private reset() {
    this.bets = new Map()
  }
}
