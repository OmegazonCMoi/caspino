import { describe, it, expect } from "vitest"
import { calculateGains } from "./calculateSlotsGains.ts"

const symbols = [
  { value: "1", weight: 30 },
  { value: "2", weight: 25 },
  { value: "3", weight: 20 },
  { value: "4", weight: 10 },
  { value: "5", weight: 5 },
  { value: "6", weight: 5 },
  { value: "7", weight: 5 },
]

const totalWeight = symbols.reduce((sum, entry) => sum + entry.weight, 0)

describe("calculateGains - slots", () => {
  it("should pay bet * (1/probability) for 3 identical symbols", () => {
    const probability = 30 / totalWeight
    const gains = calculateGains(10, ["1", "1", "1"], symbols)
    expect(gains).toBe(Math.round(10 * (1 / probability)))
  })

  it("should pay bet * (1/probability) * 0.225 for 2 identical symbols", () => {
    const probability = 30 / totalWeight
    const gains = calculateGains(10, ["1", "1", "3"], symbols)
    expect(gains).toBe(Math.round(10 * (1 / probability) * 0.225))
  })

  it("should pay bet * 1 for a single 7", () => {
    const gains = calculateGains(10, ["7", "1", "3"], symbols)
    expect(gains).toBe(Math.round(10 * 1))
  })

  it("should return 0 when no symbols match", () => {
    const gains = calculateGains(10, ["1", "2", "3"], symbols)
    expect(gains).toBe(0)
  })

  it("should return 0 for empty result", () => {
    const gains = calculateGains(10, [], symbols)
    expect(gains).toBe(0)
  })

  it("should handle rare symbol with higher payout", () => {
    const probability = 5 / totalWeight
    const gains = calculateGains(100, ["5", "5", "5"], symbols)
    expect(gains).toBe(Math.round(100 * (1 / probability)))
  })
})
