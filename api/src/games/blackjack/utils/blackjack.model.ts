export type Suit = "hearts" | "diamonds" | "clubs" | "spades"
export type Rank =
  | "2"
  | "3"
  | "4"
  | "5"
  | "6"
  | "7"
  | "8"
  | "9"
  | "10"
  | "J"
  | "Q"
  | "K"
  | "A"

export interface Card {
  suit: Suit
  rank: Rank
}

export enum HandStatus {
  PLAYING = "PLAYING",
  STOOD = "STOOD",
  BUSTED = "BUSTED",
  BLACKJACK = "BLACKJACK",
}

export interface Hand {
  cards: Card[]
  bet: number
  status: HandStatus
  isDoubled: boolean
}

export enum SessionPhase {
  WAITING = "WAITING",
  PLAYER_TURN = "PLAYER_TURN",
  DEALER_TURN = "DEALER_TURN",
  RESOLVED = "RESOLVED",
}

export interface BlackjackSession {
  hands: Hand[]
  activeHandIndex: number
  dealerHand: Card[]
  phase: SessionPhase
  insuranceBet: number
}

export enum BlackjackAction {
  HIT = "HIT",
  STAND = "STAND",
  DOUBLE_DOWN = "DOUBLE_DOWN",
  SPLIT = "SPLIT",
  INSURANCE = "INSURANCE",
}

export const MULTIPLIERS = {
  WIN: 2,
  BLACKJACK: 2.5,
  PUSH: 1,
  INSURANCE: 3,
  LOSS: 0,
} as const

export const SUITS: Suit[] = ["hearts", "diamonds", "clubs", "spades"]
export const RANKS: Rank[] = [
  "2",
  "3",
  "4",
  "5",
  "6",
  "7",
  "8",
  "9",
  "10",
  "J",
  "Q",
  "K",
  "A",
]

export const DECK_COUNT = 6
export const RESHUFFLE_THRESHOLD = 0.25

export interface HandResult {
  bet: number
  gains: number
  playerValue: number
  status: HandStatus
}

export interface BlackjackResult {
  totalGains: number
  handResults: HandResult[]
  insuranceGain: number
}
