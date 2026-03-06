import { describe, it, expect } from "vitest"
import { calculateBlackjackGains } from "./calculateBlackjackGains.ts"
import {
  HandStatus,
  SessionPhase,
  type BlackjackSession,
  type Card,
  type Hand,
} from "./utils/blackjack.model.ts"

const card = (rank: Card["rank"], suit: Card["suit"] = "hearts"): Card => ({
  rank,
  suit,
})

const makeSession = (
  overrides: Partial<BlackjackSession> & { hands: Hand[]; dealerHand: Card[] },
): BlackjackSession => ({
  activeHandIndex: 0,
  phase: SessionPhase.RESOLVED,
  insuranceBet: 0,
  ...overrides,
})

describe("calculateBlackjackGains", () => {
  it("should return 0 gains when player busts", () => {
    const session = makeSession({
      hands: [
        {
          cards: [card("10"), card("8"), card("5")],
          bet: 100,
          status: HandStatus.BUSTED,
          isDoubled: false,
        },
      ],
      dealerHand: [card("10"), card("7")],
    })

    const result = calculateBlackjackGains(session)
    expect(result.totalGains).toBe(0)
    expect(result.handResults[0]!.gains).toBe(0)
  })

  it("should pay 2.5x for player blackjack without dealer blackjack", () => {
    const session = makeSession({
      hands: [
        {
          cards: [card("A"), card("K")],
          bet: 100,
          status: HandStatus.BLACKJACK,
          isDoubled: false,
        },
      ],
      dealerHand: [card("10"), card("7")],
    })

    const result = calculateBlackjackGains(session)
    expect(result.totalGains).toBe(250)
  })

  it("should push when both player and dealer have blackjack", () => {
    const session = makeSession({
      hands: [
        {
          cards: [card("A"), card("K")],
          bet: 100,
          status: HandStatus.BLACKJACK,
          isDoubled: false,
        },
      ],
      dealerHand: [card("A"), card("Q")],
    })

    const result = calculateBlackjackGains(session)
    expect(result.totalGains).toBe(100)
  })

  it("should pay 2x when dealer busts", () => {
    const session = makeSession({
      hands: [
        {
          cards: [card("10"), card("8")],
          bet: 100,
          status: HandStatus.STOOD,
          isDoubled: false,
        },
      ],
      dealerHand: [card("10"), card("6"), card("10")],
    })

    const result = calculateBlackjackGains(session)
    expect(result.totalGains).toBe(200)
  })

  it("should pay 2x when player value > dealer value", () => {
    const session = makeSession({
      hands: [
        {
          cards: [card("10"), card("9")],
          bet: 100,
          status: HandStatus.STOOD,
          isDoubled: false,
        },
      ],
      dealerHand: [card("10"), card("7")],
    })

    const result = calculateBlackjackGains(session)
    expect(result.totalGains).toBe(200)
  })

  it("should push when player value = dealer value", () => {
    const session = makeSession({
      hands: [
        {
          cards: [card("10"), card("8")],
          bet: 100,
          status: HandStatus.STOOD,
          isDoubled: false,
        },
      ],
      dealerHand: [card("10"), card("8")],
    })

    const result = calculateBlackjackGains(session)
    expect(result.totalGains).toBe(100)
  })

  it("should return 0 when player value < dealer value", () => {
    const session = makeSession({
      hands: [
        {
          cards: [card("10"), card("6")],
          bet: 100,
          status: HandStatus.STOOD,
          isDoubled: false,
        },
      ],
      dealerHand: [card("10"), card("9")],
    })

    const result = calculateBlackjackGains(session)
    expect(result.totalGains).toBe(0)
  })

  it("should pay insurance 3x when dealer has blackjack", () => {
    const session = makeSession({
      hands: [
        {
          cards: [card("10"), card("8")],
          bet: 100,
          status: HandStatus.STOOD,
          isDoubled: false,
        },
      ],
      dealerHand: [card("A"), card("K")],
      insuranceBet: 50,
    })

    const result = calculateBlackjackGains(session)
    expect(result.insuranceGain).toBe(150)
  })

  it("should not pay insurance when dealer has no blackjack", () => {
    const session = makeSession({
      hands: [
        {
          cards: [card("10"), card("9")],
          bet: 100,
          status: HandStatus.STOOD,
          isDoubled: false,
        },
      ],
      dealerHand: [card("A"), card("6")],
      insuranceBet: 50,
    })

    const result = calculateBlackjackGains(session)
    expect(result.insuranceGain).toBe(0)
  })

  it("should calculate gains correctly for multiple hands (split)", () => {
    const session = makeSession({
      hands: [
        {
          cards: [card("10"), card("9")],
          bet: 100,
          status: HandStatus.STOOD,
          isDoubled: false,
        },
        {
          cards: [card("10"), card("5"), card("8")],
          bet: 100,
          status: HandStatus.BUSTED,
          isDoubled: false,
        },
      ],
      dealerHand: [card("10"), card("7")],
    })

    const result = calculateBlackjackGains(session)
    expect(result.handResults[0]!.gains).toBe(200)
    expect(result.handResults[1]!.gains).toBe(0)
    expect(result.totalGains).toBe(200)
  })
})
