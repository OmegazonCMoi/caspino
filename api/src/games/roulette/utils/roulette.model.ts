export const MULTIPLIERS = {
  STRAIGHT_UP: 36, // Plein (un numéro)
  EVEN_MONEY: 2, // Chances simples (rouge/noir, pair/impair, etc.)
  DOZEN: 3, // Douzaine
} as const

export const redNumbers = [
  1, 3, 5, 7, 9, 12, 14, 16, 18, 21, 23, 25, 27, 28, 30, 32, 34, 36,
]
export const blackNumbers = [
  2, 4, 6, 8, 10, 11, 13, 15, 17, 19, 20, 22, 24, 26, 29, 31, 33, 35,
]

export interface NumberBet {
  numbers: number[]
  amount: number
}

export interface TwoTimesBet {
  choice: "red" | "black" | "even" | "odd" | "1-18" | "19-36"
  amount: number
}

export interface ThreeTimesBet {
  dozen: "1-12" | "13-24" | "25-36"
  amount: number
}

export type Bet = NumberBet | TwoTimesBet | ThreeTimesBet
