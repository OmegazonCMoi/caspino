import { describe, it, expect } from "vitest"
import { calculateGains } from "./calculateRouletteGains.ts"
import type {
  NumberBet,
  TwoTimesBet,
  ThreeTimesBet,
} from "./utils/roulette.model.ts"

describe("calculateGains - roulette", () => {
  describe("NumberBet", () => {
    it("should pay amount * 36 when single number wins", async () => {
      const bet: NumberBet = { numbers: [17], amount: 10 }
      const gains = await calculateGains(17, [bet])
      expect(gains).toBe(360)
    })

    it("should return 0 when single number loses", async () => {
      const bet: NumberBet = { numbers: [17], amount: 10 }
      const gains = await calculateGains(5, [bet])
      expect(gains).toBe(0)
    })

    it("should divide payout by number count for multi-number bet", async () => {
      const bet: NumberBet = { numbers: [1, 2, 3, 4], amount: 40 }
      const gains = await calculateGains(3, [bet])
      expect(gains).toBe((40 * 36) / 4)
    })

    it("should return 0 for multi-number bet when none match", async () => {
      const bet: NumberBet = { numbers: [1, 2, 3], amount: 30 }
      const gains = await calculateGains(10, [bet])
      expect(gains).toBe(0)
    })
  })

  describe("TwoTimesBet", () => {
    it("should pay amount * 2 for red when winning", async () => {
      const bet: TwoTimesBet = { choice: "red", amount: 10 }
      const gains = await calculateGains(1, [bet])
      expect(gains).toBe(20)
    })

    it("should return 0 for red when losing", async () => {
      const bet: TwoTimesBet = { choice: "red", amount: 10 }
      const gains = await calculateGains(2, [bet])
      expect(gains).toBe(0)
    })

    it("should pay amount * 2 for black when winning", async () => {
      const bet: TwoTimesBet = { choice: "black", amount: 10 }
      const gains = await calculateGains(2, [bet])
      expect(gains).toBe(20)
    })

    it("should pay amount * 2 for even when winning", async () => {
      const bet: TwoTimesBet = { choice: "even", amount: 10 }
      const gains = await calculateGains(4, [bet])
      expect(gains).toBe(20)
    })

    it("should return 0 for even when odd number", async () => {
      const bet: TwoTimesBet = { choice: "even", amount: 10 }
      const gains = await calculateGains(3, [bet])
      expect(gains).toBe(0)
    })

    it("should pay amount * 2 for odd when winning", async () => {
      const bet: TwoTimesBet = { choice: "odd", amount: 10 }
      const gains = await calculateGains(3, [bet])
      expect(gains).toBe(20)
    })

    it("should pay amount * 2 for 1-18 when winning", async () => {
      const bet: TwoTimesBet = { choice: "1-18", amount: 10 }
      const gains = await calculateGains(18, [bet])
      expect(gains).toBe(20)
    })

    it("should return 0 for 1-18 when number is 19+", async () => {
      const bet: TwoTimesBet = { choice: "1-18", amount: 10 }
      const gains = await calculateGains(19, [bet])
      expect(gains).toBe(0)
    })

    it("should pay amount * 2 for 19-36 when winning", async () => {
      const bet: TwoTimesBet = { choice: "19-36", amount: 10 }
      const gains = await calculateGains(25, [bet])
      expect(gains).toBe(20)
    })

    it("should always return 0 on zero", async () => {
      const bets: TwoTimesBet[] = [
        { choice: "red", amount: 10 },
        { choice: "black", amount: 10 },
        { choice: "even", amount: 10 },
        { choice: "odd", amount: 10 },
        { choice: "1-18", amount: 10 },
        { choice: "19-36", amount: 10 },
      ]
      for (const bet of bets) {
        const gains = await calculateGains(0, [bet])
        expect(gains).toBe(0)
      }
    })
  })

  describe("ThreeTimesBet", () => {
    it("should pay amount * 3 for 1-12 when winning", async () => {
      const bet: ThreeTimesBet = { dozen: "1-12", amount: 10 }
      const gains = await calculateGains(7, [bet])
      expect(gains).toBe(30)
    })

    it("should return 0 for 1-12 when losing", async () => {
      const bet: ThreeTimesBet = { dozen: "1-12", amount: 10 }
      const gains = await calculateGains(13, [bet])
      expect(gains).toBe(0)
    })

    it("should pay amount * 3 for 13-24 when winning", async () => {
      const bet: ThreeTimesBet = { dozen: "13-24", amount: 10 }
      const gains = await calculateGains(20, [bet])
      expect(gains).toBe(30)
    })

    it("should pay amount * 3 for 25-36 when winning", async () => {
      const bet: ThreeTimesBet = { dozen: "25-36", amount: 10 }
      const gains = await calculateGains(30, [bet])
      expect(gains).toBe(30)
    })

    it("should always return 0 on zero", async () => {
      const bet: ThreeTimesBet = { dozen: "1-12", amount: 10 }
      const gains = await calculateGains(0, [bet])
      expect(gains).toBe(0)
    })
  })

  describe("combined bets", () => {
    it("should sum gains from multiple bets", async () => {
      const numberBet: NumberBet = { numbers: [7], amount: 5 }
      const twoTimesBet: TwoTimesBet = { choice: "red", amount: 10 }
      const threeTimesBet: ThreeTimesBet = { dozen: "1-12", amount: 10 }

      const gains = await calculateGains(7, [numberBet, twoTimesBet, threeTimesBet])
      expect(gains).toBe(5 * 36 + 10 * 2 + 10 * 3)
    })

    it("should return 0 when all bets lose", async () => {
      const numberBet: NumberBet = { numbers: [7], amount: 5 }
      const twoTimesBet: TwoTimesBet = { choice: "red", amount: 10 }
      const threeTimesBet: ThreeTimesBet = { dozen: "1-12", amount: 10 }

      const gains = await calculateGains(20, [numberBet, twoTimesBet, threeTimesBet])
      expect(gains).toBe(0)
    })
  })
})
