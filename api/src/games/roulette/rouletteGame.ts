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
    private message: (ws: WebSocket, msg: any) => void,
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

  private nextPhase() {
    switch (this.phase) {
      case RoulettePhase.BETTING:
        this.setPhase(RoulettePhase.SPINNING, 3_000)
        break
      case RoulettePhase.SPINNING:
        this.resolveGame()
        this.setPhase(RoulettePhase.RESULT, 10_000)
        break
      case RoulettePhase.RESULT:
        this.reset()
        this.setPhase(RoulettePhase.BETTING, 30_000)
        break
    }
  }

  placeBet(bets: Bet[], ws: WebSocket) {
    if (this.phase !== RoulettePhase.BETTING) {
      throw new Error("Betting closed")
    }

    this.bets.set(ws, bets)
  }

  private async resolveGame() {
    const roulettteRandomResult = Math.floor(Math.random() * 37)

    await Promise.all(
      Array.from(this.bets.entries()).map(async ([ws, bets]) => {
        const gains = await calculateGains(roulettteRandomResult, bets)
        playeEffect(gains, bet)
        this.message(ws, {
          type: "BET_RESULT",
          payload: { gains, roulettteRandomResult },
        })
      }),
    )
  }

  private reset() {
    this.bets = new Map()
  }
}
