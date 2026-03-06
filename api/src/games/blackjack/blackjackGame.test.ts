import { describe, it, expect } from "vitest"
import { BlackjackGame } from "./blackjackGame.ts"
import type { Card } from "./utils/blackjack.model.ts"

const card = (rank: Card["rank"], suit: Card["suit"] = "hearts"): Card => ({
  rank,
  suit,
})

describe("BlackjackGame.handValue", () => {
  it("should sum numeric cards directly", () => {
    expect(BlackjackGame.handValue([card("2"), card("5"), card("3")])).toBe(10)
  })

  it("should count J, Q, K as 10", () => {
    expect(BlackjackGame.handValue([card("J"), card("Q"), card("K")])).toBe(30)
  })

  it("should count Ace as 11 when no bust", () => {
    expect(BlackjackGame.handValue([card("A"), card("5")])).toBe(16)
  })

  it("should count Ace as 1 when 11 would bust", () => {
    expect(BlackjackGame.handValue([card("A"), card("10"), card("5")])).toBe(16)
  })

  it("should handle two Aces as 12 (one 11, one 1)", () => {
    expect(BlackjackGame.handValue([card("A"), card("A")])).toBe(12)
  })

  it("should handle multiple Aces correctly", () => {
    expect(
      BlackjackGame.handValue([card("A"), card("A"), card("A")]),
    ).toBe(13)
  })
})

describe("BlackjackGame.isBlackjack", () => {
  it("should return true for Ace + face card", () => {
    expect(BlackjackGame.isBlackjack([card("A"), card("K")])).toBe(true)
  })

  it("should return true for Ace + 10", () => {
    expect(BlackjackGame.isBlackjack([card("A"), card("10")])).toBe(true)
  })

  it("should return false for 3 cards totaling 21", () => {
    expect(
      BlackjackGame.isBlackjack([card("7"), card("7"), card("7")]),
    ).toBe(false)
  })

  it("should return false for 2 cards not totaling 21", () => {
    expect(BlackjackGame.isBlackjack([card("10"), card("9")])).toBe(false)
  })
})
