import type { Bet, NumberBet } from "./categories.enum.js"

const isNumberBet = (bet: Bet) => {
  return typeof (bet as any)?.number !== "undefined"
}

const isTwoTimesBet = (bet: Bet) => {
  return typeof (bet as any)?.choice !== "undefined"
}

const isThreeTimesBet = (bet: Bet) => {
  return typeof (bet as any)?.dozen !== "undefined"
}

export const calculateGains = async (lastNumber: number, bets: Bet[]) => {
  let totalGains = 0

  await Promise.all(
    bets.map(async (bet) => {
      if (isNumberBet(bet)) {
        bet.numbers.map(numbers)
          totalGains += bet. === lastNumber ? bet.amount 36

      }

        if (isTwoTimesBet(bet)) {
    })
  )

  return totalGains
}
