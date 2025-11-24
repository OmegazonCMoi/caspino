import {
  redNumbers,
  type Bet,
  type NumberBet,
  type TwoTimesBet,
  type ThreeTimesBet,
  MULTIPLIERS,
} from "./utils/roulette.model.ts"

const isNumberBet = (bet: Bet): bet is NumberBet => {
  return Array.isArray((bet as NumberBet).numbers)
}

const isTwoTimesBet = (bet: Bet): bet is TwoTimesBet => {
  return typeof (bet as TwoTimesBet).choice === "string"
}

const isThreeTimesBet = (bet: Bet): bet is ThreeTimesBet => {
  return typeof (bet as ThreeTimesBet).dozen === "string"
}

export const calculateGains = async (lastNumber: number, bets: Bet[]) => {
  const gains = await Promise.all(
    bets.map(async (bet) => {
      if (isNumberBet(bet)) return calculateNumberBetGains(bet, lastNumber)
      if (isTwoTimesBet(bet)) return calculateTwoTimesBetGains(bet, lastNumber)
      if (isThreeTimesBet(bet))
        return calculateThreeTimesBetGains(bet, lastNumber)

      console.error("Unknown bet type:", bet)
      return 0
    })
  )

  return gains.reduce((acc, gain) => acc + gain, 0)
}

const isRed = (number: number): boolean => redNumbers.includes(number)
const isEven = (number: number): boolean => number % 2 === 0
const isInRange = (number: number, min: number, max: number): boolean =>
  number >= min && number <= max

const calculateNumberBetGains = (
  bet: NumberBet,
  lastNumber: number
): number => {
  if (bet.numbers.includes(lastNumber)) {
    const total = (bet.amount * MULTIPLIERS.STRAIGHT_UP) / bet.numbers.length
    return total
  }
  return 0
}

const calculateTwoTimesBetGains = (
  bet: TwoTimesBet,
  lastNumber: number
): number => {
  if (lastNumber === 0) return 0

  const isWinningBet = (() => {
    switch (bet.choice) {
      case "red":
        return isRed(lastNumber)
      case "black":
        return !isRed(lastNumber)
      case "even":
        return isEven(lastNumber)
      case "odd":
        return !isEven(lastNumber)
      case "1-18":
        return isInRange(lastNumber, 1, 18)
      case "19-36":
        return isInRange(lastNumber, 19, 36)

      default:
        console.error("Unknown two times bet:", bet)
        return 0
    }
  })()

  return isWinningBet ? bet.amount * MULTIPLIERS.EVEN_MONEY : 0
}

const calculateThreeTimesBetGains = (
  bet: ThreeTimesBet,
  lastNumber: number
): number => {
  if (lastNumber === 0) return 0

  const isWinningBet = (() => {
    switch (bet.dozen) {
      case "1-12":
        return isInRange(lastNumber, 1, 12)
      case "13-24":
        return isInRange(lastNumber, 13, 24)
      case "25-36":
        return isInRange(lastNumber, 25, 36)

      default:
        console.error("Unknown dozen bet:", bet)
        return 0
    }
  })()

  return isWinningBet ? bet.amount * MULTIPLIERS.DOZEN : 0
}
